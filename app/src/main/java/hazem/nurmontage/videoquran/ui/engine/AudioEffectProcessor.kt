package hazem.nurmontage.videoquran.ui.engine

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import java.io.File

/**
 * AudioEffectProcessor
 *
 * Encapsulates FFmpeg-based audio effect processing that was originally
 * in [EngineActivity.kt] as direct methods.
 *
 * The original code mixed audio loading (downloading, preparing, placing on
 * the timeline) with audio effect processing (building FFmpeg filter chains,
 * running FFmpeg, updating EntityAudio state). This class isolates the
 * effect-processing concern.
 *
 * ## Effect pipeline
 *
 * Effects are applied via FFmpeg's `-af` (audio filter) flag. The filter
 * chain is built by [createCmd] and executed via [FfmpegCommandBuilder].
 * The supported effects in order of application:
 *
 * 1. `atrim=start=X:end=Y` — clip to time range
 * 2. `asetpts=N/SR/TB` — reset timestamps
 * 3. `afftdn=nf=-25` — noise removal (optional)
 * 4. `volume=X.XX` — volume adjustment
 * 5. `afade=t=in:st=0:d=X` — fade in (optional)
 * 6. `afade=t=out:st=X:d=Y` — fade out (optional)
 * 7. Voice enhancement command (optional)
 * 8. Reverb preset filter (optional)
 * 9. `aecho=1.0:outGain:delays:decays` — echo effect (optional)
 * 10. `atempo=X.XX` chain — speed adjustment (optional, cascaded for values outside [0.5, 2.0])
 *
 * **Note:** The actual filter-chain construction is already implemented in
 * [FfmpegCommandBuilder.buildAudioEffectsChain]. This class is responsible
 * for the _orchestration_ — running FFmpeg, handling the result, and
 * updating the [EntityAudio] state.
 *
 * ## Callbacks
 *
 * All Activity-specific operations are delegated via constructor lambdas
 * so this class has no direct reference to [EngineActivity]:
 *
 * - [onEffectApplied] — called after an effect is successfully applied to an entity
 * - [onError] — called when an error occurs during processing
 * - [onProgressShow] — called to show a progress indicator
 * - [onProgressHide] — called to hide the progress indicator
 * - [audioEntityCount] — returns the total number of audio entities on the timeline
 * - [entityAudioAt] — returns the next non-deleted entity at/after the given index
 * - [secondInScreen] — returns the pixels-per-second scale from the track view
 * - [updateWhenEffect] — repositions subsequent entities after a duration change
 * - [onAllComplete] — called when [applyEffectAll] finishes processing all entities
 * - [startPreview] — called to auto-start preview playback (used by [applyEffectPlayAuto])
 *
 * @param context          Android context for MediaPlayer creation and file operations
 * @param templateFolder   Directory path for FFmpeg output files
 * @param ffmpegBuilder    Optional [FfmpegCommandBuilder] for testability; defaults to the singleton
 * @param onEffectApplied  Called after an effect is successfully applied to an [EntityAudio]
 * @param onError          Called when an error occurs during effect processing
 * @param onProgressShow   Called to show a progress/loading indicator
 * @param onProgressHide   Called to hide the progress/loading indicator
 * @param audioEntityCount Returns the total number of audio entities on the timeline
 * @param entityAudioAt    Returns the next non-deleted entity at/after the given index (like TrackEntityView.getEntityAudioNotDeleted)
 * @param secondInScreen   Returns the pixels-per-second scale from the track view
 * @param updateWhenEffect Repositions subsequent entities after a duration change (like TrackEntityView.updateWhenEffect)
 * @param onAllComplete    Called when [applyEffectAll] finishes processing all entities
 * @param startPreview     Called to auto-start preview playback (used by [applyEffectPlayAuto])
 */
class AudioEffectProcessor(
    private val context: Context,
    private val templateFolder: String,
    private val ffmpegBuilder: FfmpegCommandBuilder = FfmpegCommandBuilder,
    private val onEffectApplied: (EntityAudio) -> Unit,
    private val onError: (String) -> Unit,
    private val onProgressShow: () -> Unit,
    private val onProgressHide: () -> Unit,
    private val audioEntityCount: () -> Int,
    private val entityAudioAt: (Int) -> Pair<Int, EntityAudio>?,
    private val secondInScreen: () -> Float,
    private val updateWhenEffect: (EntityAudio) -> Unit,
    private val onAllComplete: () -> Unit = {},
    private val startPreview: () -> Unit = {}
) {

    /** FFmpeg session IDs for cleanup/cancellation. */
    private val ffmpegSessionIds = mutableListOf<Long>()

    /** Handler for posting callbacks to the main thread. */
    private val mainHandler = Handler(Looper.getMainLooper())

    // ──────────────────────────────────────────────
    //  Command building
    // ──────────────────────────────────────────────

    /**
     * Build the FFmpeg audio filter command string for the given effect settings.
     *
     * **Origin:** `EngineActivity.createCmd(effectAudio, f, f2)`
     * Delegates to [FfmpegCommandBuilder.buildAudioEffectsChain] which is the
     * already-refactored version of this logic.
     *
     * The filter chain is constructed in this exact order:
     * 1. Trim (`atrim`) + timestamp reset (`asetpts`)
     * 2. Noise removal (`afftdn`, if enabled)
     * 3. Volume adjustment
     * 4. Fade in (if `fade_in > 0`)
     * 5. Fade out (if `fade_out > 0`)
     * 6. Voice enhancement (if `isEnhance`)
     * 7. Reverb preset (if set)
     * 8. Echo effect (if `decays > 0`)
     * 9. Speed adjustment (`atempo` chain, if `speed != 1.0`)
     *
     * @param effectAudio  The audio effect configuration
     * @param startSec     Start time in seconds (typically `effectAudio.start / 1000.0f`)
     * @param endSec       End time in seconds (typically `effectAudio.end / 1000.0f`)
     * @return Comma-separated FFmpeg audio filter string
     */
    fun createCmd(effectAudio: EffectAudio, startSec: Float, endSec: Float): String {
        return ffmpegBuilder.buildAudioEffectsChain(effectAudio, startSec, endSec)
    }

    /**
     * Build cascaded `atempo` filters for speed values outside the [0.5, 2.0] range.
     *
     * **Origin:** `EngineActivity.buildSpeedFilters(f: Float)`
     * Delegates to [FfmpegCommandBuilder.buildSpeedFilters].
     *
     * FFmpeg's `atempo` filter only supports [0.5, 2.0]. For values outside
     * this range, multiple filters are chained:
     * - speed < 0.5 → chain `atempo=0.5` until remainder ∈ [0.5, 2.0]
     * - speed > 2.0 → chain `atempo=2.0` until remainder ∈ [0.5, 2.0]
     *
     * @param speed Desired playback speed multiplier
     * @return List of `atempo=X.XX` filter strings
     */
    fun buildSpeedFilters(speed: Float): List<String> {
        return ffmpegBuilder.buildSpeedFilters(speed)
    }

    // ──────────────────────────────────────────────
    //  Effect application
    // ──────────────────────────────────────────────

    /**
     * Apply an audio effect to **all** audio entities on the timeline, one by one.
     *
     * **Origin:** `EngineActivity.applyffectAll(effectAudio, i: Int)`
     * Iterates through the audio entities starting at index [startIndex],
     * running FFmpeg on each, creating a new MediaPlayer for the output,
     * and updating the EntityAudio's `pathFfmpegEffect` and `isApplyEffectInPreview`.
     *
     * This is a recursive operation — after processing one entity it calls
     * itself with the next index. Processing completes when the index exceeds
     * the entity list size, at which point it calls [onAllComplete] and
     * [onProgressHide].
     *
     * If an EntityAudio's duration changes after applying the effect (e.g. due
     * to speed adjustment), the entity's `right`, `duration`, `end`, `start`,
     * and `max` values are recalculated and [updateWhenEffect] is called.
     *
     * @param effectAudio The effect configuration to apply to all entities
     * @param startIndex  Index to start from (use 0 for the beginning)
     */
    fun applyEffectAll(effectAudio: EffectAudio, startIndex: Int) {
        if (startIndex >= audioEntityCount()) {
            mainHandler.post {
                onAllComplete()
                onProgressHide()
            }
            return
        }

        val entityPair = entityAudioAt(startIndex)
        if (entityPair == null) {
            mainHandler.post {
                onAllComplete()
                onProgressHide()
            }
            return
        }

        val entityAudio = entityPair.second
        val actualIndex = entityPair.first
        val cmd = createCmd(
            effectAudio,
            entityAudio.effectAudio.start / 1000.0f,
            entityAudio.effectAudio.end / 1000.0f
        )
        val file = File(templateFolder, "${System.currentTimeMillis()}_audio_echo.mp3")

        val args = ffmpegBuilder.buildAudioEffectArgs(
            entityAudio.getPathFfmpeg()!!,
            file.absolutePath,
            cmd
        )

        ffmpegSessionIds.add(
            FFmpegKit.executeWithArgumentsAsync(
                args,
                object : FFmpegSessionCompleteCallback {
                    override fun apply(fFmpegSession: FFmpegSession) {
                        if (fFmpegSession.returnCode.isValueSuccess) {
                            try {
                                val mediaPlayer = MediaPlayer()
                                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                                val uri = Uri.fromFile(file)
                                if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
                                    mediaPlayer.setDataSource(uri.toString())
                                } else {
                                    mediaPlayer.setDataSource(context, uri)
                                }
                                mediaPlayer.prepareAsync()
                                mediaPlayer.setOnPreparedListener { mp ->
                                    if (entityAudio.mediaPlayer != null &&
                                        mp.duration != entityAudio.mediaPlayer!!.duration
                                    ) {
                                        updateEntityDurationAfterEffect(
                                            entityAudio, mp.duration,
                                            secondInScreen(), updateWhenEffect
                                        )
                                    }
                                    entityAudio.mediaPlayer = mp
                                    applyEffectAll(effectAudio, actualIndex + 1)
                                }
                                entityAudio.pathFfmpegEffect = file.absolutePath
                                entityAudio.isApplyEffectInPreview = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                mainHandler.post {
                                    onAllComplete()
                                    onProgressHide()
                                }
                            }
                        }
                    }
                }
            ).sessionId
        )
    }

    /**
     * Apply an audio effect to a single [EntityAudio].
     *
     * **Origin:** `EngineActivity.applyffect(str, entityAudio)`
     * Runs FFmpeg with the given filter string on the entity's audio file,
     * creates a new MediaPlayer for the output, and updates the entity state.
     * If the duration changed (e.g. from speed adjustment), recalculates
     * the entity's timeline bounds.
     *
     * @param filterChain The FFmpeg audio filter string (from [createCmd] or custom)
     * @param entityAudio The audio entity to process
     */
    fun applyEffect(filterChain: String, entityAudio: EntityAudio) {
        onProgressShow()
        val file = File(templateFolder, "${System.currentTimeMillis()}_audio_echo.mp3")
        val uri = Uri.fromFile(file)

        val args = ffmpegBuilder.buildAudioEffectArgs(
            entityAudio.getPathFfmpeg()!!,
            file.absolutePath,
            filterChain
        )

        ffmpegSessionIds.add(
            FFmpegKit.executeWithArgumentsAsync(
                args,
                object : FFmpegSessionCompleteCallback {
                    override fun apply(fFmpegSession: FFmpegSession) {
                        if (fFmpegSession.returnCode.isValueSuccess) {
                            try {
                                val mediaPlayer = MediaPlayer()
                                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                                if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
                                    mediaPlayer.setDataSource(uri.toString())
                                } else {
                                    mediaPlayer.setDataSource(context, uri)
                                }
                                mediaPlayer.prepareAsync()
                                mediaPlayer.setOnPreparedListener { mp ->
                                    if (entityAudio.mediaPlayer != null &&
                                        mp.duration != entityAudio.mediaPlayer!!.duration
                                    ) {
                                        updateEntityDurationAfterEffect(
                                            entityAudio, mp.duration,
                                            secondInScreen(), updateWhenEffect
                                        )
                                        mainHandler.post {
                                            onEffectApplied(entityAudio)
                                            onProgressHide()
                                        }
                                    } else {
                                        mainHandler.post { onProgressHide() }
                                    }
                                    entityAudio.mediaPlayer = mp
                                }
                                entityAudio.pathFfmpegEffect = file.absolutePath
                                entityAudio.isApplyEffectInPreview = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                                mainHandler.post { onProgressHide() }
                            }
                        }
                    }
                }
            ).sessionId
        )
    }

    /**
     * Apply an audio effect and automatically start preview playback.
     *
     * **Origin:** `EngineActivity.applyffectPlayAuto(str, entityAudio)`
     * Similar to [applyEffect] but after FFmpeg completes and the new
     * MediaPlayer is prepared, it immediately triggers [startPreview]
     * so the user can hear the result without manually pressing play.
     *
     * This is typically used when the user adjusts an effect parameter
     * (e.g. volume, echo) and wants instant auditory feedback.
     *
     * @param filterChain The FFmpeg audio filter string
     * @param entityAudio The audio entity to process and preview
     */
    fun applyEffectPlayAuto(filterChain: String, entityAudio: EntityAudio) {
        onProgressShow()
        val file = File(templateFolder, "${System.currentTimeMillis()}_audio_echo.mp3")

        val args = ffmpegBuilder.buildAudioEffectArgs(
            entityAudio.getPathFfmpeg()!!,
            file.absolutePath,
            filterChain
        )

        ffmpegSessionIds.add(
            FFmpegKit.executeWithArgumentsAsync(args) { fFmpegSession ->
                if (fFmpegSession.returnCode.isValueSuccess) {
                    try {
                        val uri = Uri.fromFile(file)
                        val mediaPlayer = MediaPlayer()
                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
                            mediaPlayer.setDataSource(uri.toString())
                        } else {
                            mediaPlayer.setDataSource(context, uri)
                        }
                        mediaPlayer.prepareAsync()
                        mediaPlayer.setOnPreparedListener { mp ->
                            if (entityAudio.mediaPlayer != null &&
                                mp.duration != entityAudio.mediaPlayer!!.duration
                            ) {
                                updateEntityDurationAfterEffect(
                                    entityAudio, mp.duration,
                                    secondInScreen(), updateWhenEffect
                                )
                                mainHandler.post {
                                    onEffectApplied(entityAudio)
                                    entityAudio.mediaPlayer = mp
                                    startPreview()
                                    onProgressHide()
                                }
                            } else {
                                mainHandler.post {
                                    entityAudio.mediaPlayer = mp
                                    startPreview()
                                    onProgressHide()
                                }
                            }
                        }
                        entityAudio.setApplyEffectInPreview(true)
                        entityAudio.setPathFfmpegEffect(file.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        mainHandler.post { onProgressHide() }
                    }
                }
            }.sessionId
        )
    }

    // ──────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────

    /**
     * Update an EntityAudio's timeline bounds when its duration changes after an effect.
     *
     * **Origin:** Inlined in `applyffectAll`, `applyffect`, `applyffectPlayAuto`
     * When FFmpeg processing changes the audio duration (e.g. speed effect),
     * the entity's right edge, duration, start, end, and max values must be
     * recalculated. This also calls [updateWhenEffect] to reposition
     * subsequent entities on the timeline.
     *
     * @param entityAudio      The entity to update
     * @param newDurationMs    New duration in milliseconds from MediaPlayer
     * @param secInScreen      Pixels-per-second scale from the track view
     * @param doUpdateWhenEffect Callback to reposition subsequent entities (typically TrackEntityView.updateWhenEffect)
     */
    fun updateEntityDurationAfterEffect(
        entityAudio: EntityAudio,
        newDurationMs: Int,
        secInScreen: Float,
        doUpdateWhenEffect: (EntityAudio) -> Unit = updateWhenEffect
    ) {
        entityAudio.right = entityAudio.rect.left +
            Math.round(secInScreen * (newDurationMs / 1000.0f))
        entityAudio.duration = newDurationMs * 1000
        entityAudio.end = newDurationMs.toFloat()
        entityAudio.start = 0.0f
        entityAudio.max = (entityAudio.rect.right / entityAudio.scaleFactor) -
            ((entityAudio.rect.left / entityAudio.scaleFactor) - entityAudio.getOffsetLeft())
        doUpdateWhenEffect(entityAudio)
    }

    // ──────────────────────────────────────────────
    //  Session management
    // ──────────────────────────────────────────────

    /**
     * Cancel all running FFmpeg sessions started by this processor.
     */
    fun cancelAll() {
        synchronized(ffmpegSessionIds) {
            for (sessionId in ffmpegSessionIds) {
                FFmpegKit.cancel(sessionId)
            }
            ffmpegSessionIds.clear()
        }
    }

    /**
     * Get a snapshot of the currently tracked FFmpeg session IDs.
     */
    fun getSessionIds(): List<Long> {
        synchronized(ffmpegSessionIds) {
            return ffmpegSessionIds.toList()
        }
    }
}
