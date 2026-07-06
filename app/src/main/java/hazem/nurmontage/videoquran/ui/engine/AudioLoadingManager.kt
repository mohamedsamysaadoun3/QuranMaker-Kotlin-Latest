package hazem.nurmontage.videoquran.ui.engine

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.model.EntityMedia
import hazem.nurmontage.videoquran.model.RecitersModel
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.PCMWaveformExtractor
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * AudioLoadingManager
 *
 * Encapsulates all audio loading and preparation logic that was originally
 * in [EngineActivity] and [EngineAudioManager] as extension functions on `EngineActivity`.
 *
 * The original code mixed two distinct concerns:
 * 1. **Audio loading** — downloading, preparing, and placing audio on the
 *    timeline (reciters, templates, HTTP sources, video extraction).
 * 2. **Audio effects** — FFmpeg-based effect processing (volume, fade, echo, etc.).
 *
 * This class handles concern #1. Effect processing is in [AudioEffectProcessor].
 *
 * ## Dependencies
 * - [context] — Android context for resource / MediaPlayer access
 * - [template] — The current project template (set externally before calling load methods)
 * - [trackViewEntity] — The timeline track view (set externally before calling load methods)
 *
 * ## Callbacks
 * - [onAudioReady] — Fired when an EntityAudio has been created and placed on the timeline
 * - [onProgress] — Progress percentage (0–100) for loading UI
 * - [onError] — Error message when loading fails
 * - [onProgressShow] — Request to show the progress overlay
 * - [onProgressHide] — Request to hide the progress overlay
 * - [onUpdateTime] — Request to recalculate and refresh the timeline time display
 * - [onAddUriAudioToQuran] — Request to open the add-Quran-ayah fragment with an audio URI
 * - [onHideFragment] — Request to hide the current bottom sheet / fragment
 * - [onUpdateBtnToEnd] — Request to update navigation button state (scroll to end)
 * - [onUpdateBtnToStart] — Request to update navigation button state (scroll to start)
 * - [onUpdateViewTime] — Request to update the time label with specific max/cursor values
 */
class AudioLoadingManager(
    private val context: Context,
    private val onAudioReady: (EntityAudio) -> Unit,
    private val onProgress: (Int) -> Unit,
    private val onError: (String) -> Unit,
    private val onProgressShow: () -> Unit,
    private val onProgressHide: () -> Unit,
    private val onUpdateTime: () -> Unit,
    private val onAddUriAudioToQuran: (Uri, String?) -> Unit = { _, _ -> },
    private val onHideFragment: () -> Unit = {},
    private val onUpdateBtnToEnd: () -> Unit = {},
    private val onUpdateBtnToStart: () -> Unit = {},
    private val onUpdateViewTime: (Int, Int) -> Unit = { _, _ -> }
) {

    // ──────────────────────────────────────────────
    //  External dependencies (set by the host)
    // ──────────────────────────────────────────────

    /** The current project template. Must be set before calling load methods. */
    var template: Template? = null

    /** The timeline track view. Must be set before calling load methods. */
    var trackViewEntity: TrackEntityView? = null

    // ──────────────────────────────────────────────
    //  Internal state
    // ──────────────────────────────────────────────

    /** Active FFmpeg session IDs for cancellation tracking. */
    private val id_ffmpeg = mutableListOf<Long>()

    /** Currently held MediaPlayer reference (released on [release]). */
    private var mPlayer: MediaPlayer? = null

    /** Background executor for PCM / copy operations. */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Main-thread handler for posting UI updates. */
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Whether this manager has been released and should no longer operate. */
    private var isReleased = false

    /** Index into [EXTENSIONS] during recursive video-audio extraction. */
    private var startExtensionIndex: Int = 0

    companion object {
        private const val TAG = "AudioLoadingManager"

        /** File extensions to try when extracting audio from video. */
        private val EXTENSIONS = arrayOf(".mp3", ".ogg", ".acc", ".m4a", ".wav", ".mpeg")
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers — UI posting
    // ══════════════════════════════════════════════════════════════════

    private fun postOnUiThread(action: () -> Unit) {
        mainHandler.post(action)
    }

    private fun hideProgress() {
        postOnUiThread { onProgressHide() }
    }

    private fun hideFragment() {
        postOnUiThread { onHideFragment() }
    }

    private fun hideProgressAndFragment() {
        postOnUiThread {
            onProgressHide()
            onHideFragment()
        }
    }

    private fun updateTimeAndButtons() {
        postOnUiThread {
            onUpdateTime()
            onUpdateBtnToEnd()
            onUpdateBtnToStart()
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers — track dimensions
    // ══════════════════════════════════════════════════════════════════

    private fun trackWidth(): Int = trackViewEntity?.width_screen ?: 0

    private fun trackSecondInScreen(): Float = trackViewEntity?.second_in_screen ?: 0f

    private fun trackSecondInScreenNoScale(): Float = trackViewEntity?.second_in_screenNoScale ?: 0f

    private fun trackScaleFactor(): Float = trackViewEntity?.scaleFactor ?: 1f

    private fun lastAudioRight(): Float {
        val tv = trackViewEntity ?: return 0f
        val audio = tv.getAudio()
        return if (tv.entityListAudio.isEmpty() || audio == null) 0.0f
        else audio.rect.right / tv.scaleFactor
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers — MediaPlayer creation
    // ══════════════════════════════════════════════════════════════════

    /**
     * Create and prepare a [MediaPlayer] for the given [uri].
     * Sets [mPlayer] as a side effect.
     *
     * @return the prepared MediaPlayer, or null on failure
     */
    private fun createMediaPlayer(uri: Uri): MediaPlayer? {
        return try {
            MediaPlayer().also { mp ->
                mPlayer = mp
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
                    mp.setDataSource(uri.toString())
                } else {
                    mp.setDataSource(context, uri)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
            null
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers — FFmpeg
    // ══════════════════════════════════════════════════════════════════

    /** Build the FFmpeg PCM-extraction argument list for a given input file. */
    private fun buildPcmExtractionArgs(inputPath: String, outputFile: File): Array<String> {
        return arrayOf(
            "-i", inputPath,
            "-map", "0:a",
            "-ac", "1",
            "-ar", "44100",
            "-f", "s16le",
            outputFile.absolutePath,
            "-y"
        )
    }

    /** Compute the waveform sample count for the given pixel dimensions. */
    private fun waveformSampleCount(round: Int, round2: Int): Int {
        return round2 / ((round * 0.1f).toInt() + (round * 0.07f).toInt())
    }

    /** Build speed filter chain for FFmpeg atempo (range 0.5–2.0 per step). */
    internal fun buildSpeedFilters(speed: Float): List<String> {
        val filters = ArrayList<String>()
        var s = speed
        if (s < 0.5f) {
            while (s < 0.5f) {
                filters.add("atempo=0.5")
                s /= 0.5f
            }
            filters.add(String.format(Locale.US, "atempo=%.2f", s))
        } else if (s > 2.0f) {
            while (s > 2.0f) {
                filters.add("atempo=2.0")
                s /= 2.0f
            }
            filters.add(String.format(Locale.US, "atempo=%.2f", s))
        } else {
            filters.add(String.format(Locale.US, "atempo=%.2f", s))
        }
        return filters
    }

    /** Build the FFmpeg audio-filter chain from an [EffectAudio] for preview rendering. */
    private fun buildEffectFilterChain(effectAudio: EffectAudio, start: Float, end: Float): String {
        val filters = ArrayList<String>()
        filters.add("atrim=start=$start:end=$end")
        filters.add("asetpts=N/SR/TB")
        if (effectAudio.isRemoveNoice) {
            filters.add("afftdn=nf=-25")
        }
        filters.add(String.format(Locale.US, "volume=%.2f", effectAudio.volume))
        if (effectAudio.fade_in > 0) {
            filters.add("afade=t=in:st=0:d=${effectAudio.fade_in}")
        }
        if (effectAudio.fade_out > 0) {
            val fadeOut = effectAudio.fade_out
            filters.add("afade=t=out:st=${(end - start) - fadeOut}:d=$fadeOut")
        }
        if (effectAudio.isEnhance) {
            filters.add(Common.ENHANCE_CMD)
        }
        if (effectAudio.reverbPreset != null) {
            filters.add(effectAudio.reverbPreset!!)
        }
        if (effectAudio.decays > 0) {
            filters.add(
                String.format(
                    Locale.US, "aecho=%.2f:%.2f:%s:%s",
                    1.0f, effectAudio.outGain, effectAudio.delays_cmd, effectAudio.decays_cmd
                )
            )
        }
        if (effectAudio.speed != 1.0f) {
            filters.addAll(buildSpeedFilters(effectAudio.speed))
        }
        return TextUtils.join(",", filters)
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers — template entity chaining
    // ══════════════════════════════════════════════════════════════════

    /**
     * After processing template entity at [nextIndex], determine the next
     * entity's source type (video / reciters / HTTP) and start loading it.
     */
    private fun chainToNextTemplateEntity(nextIndex: Int) {
        val tmpl = template ?: return
        if (nextIndex >= tmpl.entityMediaList.size) {
            postOnUiThread {
                onUpdateTime()
                trackViewEntity?.invalidate()
                onProgressHide()
            }
            return
        }
        val entityMedia2 = tmpl.entityMediaList[nextIndex]
        if (entityMedia2.video_path != null) {
            val prevEntityMedia = tmpl.entityMediaList[nextIndex - 1]
            prevEntityMedia.video_path = AudioUtils.copyFromUri(
                context, Uri.parse(tmpl.uri_upload_extract_audio_video), tmpl.folder_template!!
            )
            if (tmpl.extension != null) {
                addAudioFromVideoWithExtension(tmpl.extension!!, prevEntityMedia.video_path!!, nextIndex)
            } else {
                startExtensionIndex = 0
                extractAudioFromVideoRecursive(prevEntityMedia.video_path!!, 0, true, nextIndex)
            }
        } else if (entityMedia2.paths_https != null) {
            addAudioRecitersTemplate(entityMedia2.paths_https!!, nextIndex, "")
        } else {
            addAudioTemplateHttp(Uri.parse(entityMedia2.uri), nextIndex, null)
        }
    }

    // ──────────────────────────────────────────────
    //  Direct audio addition
    // ──────────────────────────────────────────────

    /**
     * Add audio from a local or remote [Uri] to the timeline.
     *
     * **Origin:** `EngineAudioManager.addAudio(uri: Uri)`
     * Prepares a [MediaPlayer] asynchronously and calls [changeEntityAudio]
     * on success to create the [EntityAudio] on the track.
     *
     * @param uri The audio source URI (local file or HTTP)
     */
    fun addAudio(uri: Uri) {
        try {
            val mp = createMediaPlayer(uri) ?: return
            mp.setOnErrorListener { _, _, _ ->
                hideProgressAndFragment()
                true
            }
            mp.prepareAsync()
            mp.setOnPreparedListener { mediaPlayer ->
                if (mediaPlayer == null) return@setOnPreparedListener
                changeEntityAudio(mediaPlayer.duration, uri)
            }
        } catch (e: Exception) {
            hideProgressAndFragment()
            e.printStackTrace()
        }
    }

    /**
     * Add audio from a [Uri] with HTTP path list (for reciters/template).
     *
     * **Origin:** `EngineAudioManager.addAudio(uri, list, i, str)`
     * Same as [addAudio] but also stores the HTTP paths and PCM data path.
     *
     * @param uri        Audio source URI
     * @param httpPaths  List of HTTP URLs that make up this audio
     * @param index      Template entity media index (-1 for non-template)
     * @param pcmPath    Path to pre-extracted PCM data for waveform
     */
    fun addAudio(uri: Uri, httpPaths: List<String>, index: Int, pcmPath: String) {
        try {
            val mp = createMediaPlayer(uri) ?: return
            mp.setOnErrorListener { _, _, _ ->
                hideProgressAndFragment()
                true
            }
            mp.prepareAsync()
            mp.setOnPreparedListener { mediaPlayer ->
                if (mediaPlayer == null) return@setOnPreparedListener
                changeEntityAudio(mediaPlayer.duration, uri, httpPaths, index, pcmPath)
            }
        } catch (e: Exception) {
            hideProgressAndFragment()
            e.printStackTrace()
        }
    }

    /**
     * Add audio extracted from a video file.
     *
     * **Origin:** `EngineAudioManager.addAudioFromVideo(uri, str)`
     * Prepares MediaPlayer, then calls [changeEntityAudioFromVideo]
     * and [onUpdateTime] on the main thread.
     *
     * @param uri       Audio URI (typically extracted from video)
     * @param videoPath Path to the source video file
     */
    fun addAudioFromVideo(uri: Uri, videoPath: String) {
        try {
            val mp = createMediaPlayer(uri) ?: return
            mp.setOnErrorListener { _, _, _ ->
                hideProgressAndFragment()
                true
            }
            mp.prepareAsync()
            mp.setOnPreparedListener { mediaPlayer ->
                if (mediaPlayer == null) return@setOnPreparedListener
                changeEntityAudioFromVideo(mediaPlayer.duration, uri, videoPath)
                try {
                    postOnUiThread { onUpdateTime() }
                } catch (e: Exception) {
                    e.printStackTrace()
                    hideProgressAndFragment()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    // ──────────────────────────────────────────────
    //  Template audio loading
    // ──────────────────────────────────────────────

    /**
     * Add audio from a template's HTTP source (one entity at a time).
     *
     * **Origin:** `EngineAudioManager.addAudioTemplateHttp(uri, i, str)`
     * Copies the audio file locally, applies any preview effects if the
     * entity has them, creates the [EntityAudio], extracts waveform via
     * FFmpeg, then chains to the next template entity.
     *
     * @param uri       Audio source URI
     * @param index     Index into the template's entityMediaList
     * @param localPath Optional pre-copied local path (nullable)
     */
    fun addAudioTemplateHttp(uri: Uri?, index: Int, localPath: String?) {
        try {
            if (isReleased) return
            if (uri == null) {
                hideProgress()
                return
            }
            val tmpl = template ?: return
            if (tmpl.entityMediaList.isNotEmpty()) {
                updateProgress(index + 1, tmpl.entityMediaList.size)
            }

            // Determine the local file path for the audio
            val resolvedPath = when {
                localPath != null -> uri.path
                !uri.toString().contains("share_with_me") ->
                    AudioUtils.copyFromUri(context, uri, tmpl.folder_template!!)
                else -> uri.toString()
            }
            val ffmpegInputPath = resolvedPath ?: return
            val entityMedia = tmpl.entityMediaList[index]

            // If the template entity has preview effects, apply them before creating EntityAudio
            if (entityMedia.isApplyEffectInPreview) {
                val effectFile = File(tmpl.folder_template, "${System.currentTimeMillis()}_audio_echo.mp3")
                val effectAudio = entityMedia.effectAudio ?: return
                val start = effectAudio.start / 1000.0f
                val end = effectAudio.end / 1000.0f
                val filterChain = buildEffectFilterChain(effectAudio, start, end)

                id_ffmpeg.add(
                    FFmpegKit.executeWithArgumentsAsync(
                        arrayOf("-i", ffmpegInputPath, "-af", filterChain, "-y", effectFile.absolutePath),
                        object : FFmpegSessionCompleteCallback {
                            override fun apply(fFmpegSession: FFmpegSession) {
                                try {
                                    val preparedMp = createMediaPlayer(Uri.fromFile(effectFile)) ?: return
                                    preparedMp.prepareAsync()
                                    preparedMp.setOnPreparedListener { mp ->
                                        if (mp == null) return@setOnPreparedListener
                                        try {
                                            addEntityMediaHttp(
                                                entityMedia, effectAudio.duration, uri, mp,
                                                entityMedia.paths_https!!, index,
                                                ffmpegInputPath, localPath
                                            )
                                        } catch (unused: Exception) {
                                            hideProgress()
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    ).sessionId
                )
                return
            }

            // No preview effects — prepare MediaPlayer directly
            val mp = createMediaPlayer(uri) ?: return
            mp.prepareAsync()
            mp.setOnPreparedListener { mediaPlayer ->
                if (mediaPlayer == null) return@setOnPreparedListener
                try {
                    addEntityMediaHttp(
                        entityMedia, mediaPlayer.duration, uri, mediaPlayer,
                        entityMedia.paths_https!!, index, ffmpegInputPath, localPath
                    )
                } catch (unused: Exception) {
                    hideProgress()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgress()
        }
    }

    /**
     * Add audio from a template with local URI and metadata.
     *
     * **Origin:** `EngineAudioManager.addAudioTemplate(uri, list, i, str, str2, str3)`
     * Prepares MediaPlayer, then calls [addEntityMediaHttp] with the full
     * template metadata (paths, index, PCM path).
     *
     * @param uri        Audio source URI
     * @param httpPaths  List of HTTP URLs
     * @param index      Template entity media index
     * @param pcmPath    PCM data path for waveform
     * @param folderPath Template folder path for output
     * @param extension  File extension (nullable)
     */
    fun addAudioTemplate(
        uri: Uri, httpPaths: List<String>, index: Int,
        pcmPath: String, folderPath: String, extension: String?
    ) {
        try {
            val mp = createMediaPlayer(uri) ?: return
            mp.prepareAsync()
            mp.setOnPreparedListener { mediaPlayer ->
                val tmpl = template ?: return@setOnPreparedListener
                if (mediaPlayer != null && index < tmpl.entityMediaList.size) {
                    addEntityMediaHttp(
                        tmpl.entityMediaList[index], mediaPlayer.duration, uri,
                        mPlayer!!, httpPaths, index, folderPath, pcmPath, extension
                    )
                }
            }
        } catch (e: Exception) {
            hideProgressAndFragment()
            e.printStackTrace()
        }
    }

    // ──────────────────────────────────────────────
    //  Reciters loading
    // ──────────────────────────────────────────────

    /**
     * Download and concatenate audio files from reciters (background thread).
     *
     * **Origin:** `EngineAudioManager.addAudioReciters(list: List<RecitersModel>)`
     * Downloads each ayah MP3 from everyayah.com or Tarteel CDN, concatenates
     * them using FFmpeg `concat` demuxer, extracts PCM for waveform, and
     * calls [addAudio] with the result.
     *
     * @param list List of reciter models, each specifying surah/ayah identifiers
     */
    fun addAudioReciters(list: List<RecitersModel>) {
        val bgExecutor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        bgExecutor.execute {
            addAudioRecitersBackground(list, handler)
        }
    }

    /**
     * Add reciters audio one-by-one (sequential per-ayah approach).
     *
     * **Origin:** `EngineAudioManager.addAudioReciters(list: List<RecitersModel>, i: Int)`
     * Used as an alternative to the FFmpeg concat approach — loads each ayah
     * sequentially, creating an [EntityAudio] for each.
     *
     * @param list  Full list of reciter models
     * @param index Current index being processed (starts at 0)
     */
    fun addAudioReciters(list: List<RecitersModel>, index: Int) {
        try {
            if (isReleased) return
            updateProgress(index + 1, list.size)
            if (index >= list.size) {
                postOnUiThread {
                    onUpdateTime()
                    trackViewEntity?.translateToEnd()
                    onUpdateBtnToEnd()
                    onUpdateBtnToStart()
                    onProgressHide()
                    onHideFragment()
                }
                return
            }
            val recitersModel = list[index]
            val parse = if (recitersModel.isTarteel) {
                Uri.parse("https://audio-cdn.tarteel.ai/quran/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3")
            } else {
                Uri.parse("https://everyayah.com/data/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3")
            }
            val mp = createMediaPlayer(parse) ?: return
            mp.prepareAsync()
            mp.setOnPreparedListener { mediaPlayer ->
                if (mediaPlayer == null) {
                    hideProgress()
                } else {
                    changeEntityAudioReciters(mediaPlayer.duration, parse, mediaPlayer, list, index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgress()
        }
    }

    /**
     * Background implementation of reciters download + FFmpeg concat.
     *
     * **Origin:** `EngineAudioManager.addAudioRecitersBackground(list, handler)`
     * Downloads all ayah files, builds a concat text file, runs FFmpeg to
     * merge and extract PCM, then calls [addAudio] on the main thread.
     *
     * @param list    List of reciter models
     * @param handler Handler for posting results back to the main thread
     */
    @Suppress("UNCHECKED_CAST")
    fun addAudioRecitersBackground(list: List<RecitersModel>, handler: Handler) {
        val localPaths = ArrayList<String>()
        val httpPaths = ArrayList<String>()
        val concatBuilder = StringBuilder()
        try {
            val it = list.iterator()
            var i = 0
            while (it.hasNext()) {
                val recitersModel = it.next()
                try {
                    val url = if (recitersModel.isTarteel) {
                        "https://audio-cdn.tarteel.ai/quran/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3"
                    } else {
                        "https://everyayah.com/data/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3"
                    }
                    val downloadFile = AudioUtils.downloadFile(context, url, template!!.folder_template!!)
                    if (downloadFile != null) {
                        localPaths.add(downloadFile)
                        httpPaths.add(url)
                        concatBuilder.append("file '").append(downloadFile.replace("'", "\\'")).append("'\n")
                        i++
                        try {
                            handler.post { updateProgress(i, list.size) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // If no audio files were downloaded, skip FFmpeg and finish
            if (localPaths.isEmpty()) {
                handler.post {
                    onProgressHide()
                    onHideFragment()
                    onUpdateTime()
                    onUpdateBtnToEnd()
                    onUpdateBtnToStart()
                }
                return
            }
            val concatFile = File(template!!.folder_template, "concat_${System.currentTimeMillis()}.txt")
            FileOutputStream(concatFile).use { fos ->
                fos.write(concatBuilder.toString().toByteArray())
            }
            val outputMp3 = File(template!!.folder_template, "${System.currentTimeMillis()}_output.mp3")
            val outputPcm = File(template!!.folder_template, "${System.currentTimeMillis()}_output.pcm")

            val args = arrayListOf(
                "-f", "concat", "-safe", "0",
                "-i", concatFile.absolutePath,
                "-map", "0:a", "-c", "copy", outputMp3.absolutePath,
                "-map", "0:a", "-ac", "1", "-ar", "44100", "-f", "s16le", outputPcm.absolutePath,
                "-y"
            )
            val strArr = args.toTypedArray()
            handler.post {
                addAudioRecitersFfmpeg(strArr, outputMp3, httpPaths, outputPcm)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            handler.post {
                onProgressHide()
                onHideFragment()
            }
        }
    }

    /**
     * Run the FFmpeg concat command for reciters and add the result.
     *
     * **Origin:** `EngineAudioManager.addAudioRecitersFfmpeg(strArr, file, list, file2)`
     *
     * @param args      FFmpeg argument array
     * @param outputFile Concatenated MP3 output file
     * @param httpPaths HTTP URLs for the audio
     * @param pcmFile   PCM output file for waveform
     */
    fun addAudioRecitersFfmpeg(
        args: Array<String>, outputFile: File, httpPaths: List<String>, pcmFile: File
    ) {
        id_ffmpeg.add(
            FFmpegKit.executeWithArgumentsAsync(args) { fFmpegSession ->
                if (fFmpegSession.returnCode.isValueSuccess) {
                    addAudio(Uri.fromFile(outputFile), httpPaths, -1, pcmFile.absolutePath)
                } else {
                    Log.e(TAG, "FFmpeg concat failed: ${fFmpegSession.failStackTrace}")
                    hideProgressAndFragment()
                }
            }.sessionId
        )
    }

    /**
     * Add audio from a template with HTTP reciters paths.
     *
     * **Origin:** `EngineAudioManager.addAudioRecitersTemplate(list, index, pathVideo)`
     * Runs a background [Runnable] that downloads all ayah files, concatenates
     * with FFmpeg, and adds the result.
     *
     * @param httpPaths List of HTTP URLs for individual ayah files
     * @param index     Template entity media index
     * @param pathVideo Video path for audio extraction (empty string if none)
     */
    fun addAudioRecitersTemplate(httpPaths: List<String>, index: Int, pathVideo: String) {
        Executors.newSingleThreadExecutor().execute(
            addAudioRecitersTemplateRunnable(httpPaths, index, pathVideo)
        )
    }

    /**
     * Background runnable that downloads reciters, concatenates, and applies effects.
     *
     * **Origin:** `EngineAudioManager.addAudioRecitersTemplateRunnable(pathes, valIndex, valPathVideo)`
     *
     * @param paths      HTTP URL list for individual ayah files
     * @param valIndex   Template entity media index
     * @param valPathVideo Video path for audio extraction
     * @return A [Runnable] to execute on a background thread
     */
    internal fun addAudioRecitersTemplateRunnable(
        paths: List<String>, valIndex: Int, valPathVideo: String
    ): Runnable = Runnable {
        try {
            val concatBuilder = StringBuilder()
            val it = paths.iterator()
            var i = 0
            while (it.hasNext()) {
                val parse = Uri.parse(it.next())
                val uriStr = parse.toString()
                var downloadFile: String?
                if (!uriStr.startsWith("http://") && !uriStr.startsWith("https://")) {
                    downloadFile = AudioUtils.copyFromUri(context, parse, template!!.folder_template!!)
                    if (downloadFile == null) {
                        concatBuilder.append("file '").append(downloadFile!!.replace("'", "\\'")).append("'\n")
                        i++
                        updateProgress(i, paths.size)
                    }
                }
                downloadFile = AudioUtils.downloadFile(context, uriStr, template!!.folder_template!!)!!
                if (downloadFile == null) {
                    concatBuilder.append("file '").append(downloadFile!!.replace("'", "\\'")).append("'\n")
                    i++
                    updateProgress(i, paths.size)
                }
            }
            val concatFile = File(template!!.folder_template, "concat.txt")
            FileOutputStream(concatFile).use { fos ->
                fos.write(concatBuilder.toString().toByteArray())
            }
            val outputMp3 = File(template!!.folder_template, "${System.currentTimeMillis()}_output.mp3")
            val outputPcm = File(template!!.folder_template, "${System.currentTimeMillis()}_output.pcm")

            val args = arrayListOf(
                "-f", "concat", "-safe", "0",
                "-i", concatFile.absolutePath,
                "-map", "0:a", "-c", "copy", outputMp3.absolutePath,
                "-map", "0:a", "-ac", "1", "-ar", "44100", "-f", "s16le", outputPcm.absolutePath,
                "-y"
            )
            id_ffmpeg.add(
                FFmpegKit.executeWithArgumentsAsync(args.toTypedArray()) { fFmpegSession ->
                    if (fFmpegSession.returnCode.isValueSuccess) {
                        val tmpl = template ?: return@executeWithArgumentsAsync
                        if (valIndex in 0 until tmpl.entityMediaList.size) {
                            val entityMedia = tmpl.entityMediaList[valIndex]
                            if (entityMedia.isApplyEffectInPreview) {
                                val effectFile = File(
                                    tmpl.folder_template,
                                    "${System.currentTimeMillis()}_audio_echo.mp3"
                                )
                                val effectAudio = entityMedia.effectAudio ?: return@executeWithArgumentsAsync
                                val start = effectAudio.start / 1000.0f
                                val end = effectAudio.end / 1000.0f
                                val filterChain = buildEffectFilterChain(effectAudio, start, end)

                                id_ffmpeg.add(
                                    FFmpegKit.executeWithArgumentsAsync(
                                        arrayOf(
                                            "-i", outputMp3.absolutePath,
                                            "-af", filterChain,
                                            "-y", effectFile.absolutePath
                                        )
                                    ) { _ ->
                                        addAudioTemplate(
                                            Uri.fromFile(effectFile), paths, valIndex,
                                            outputMp3.absolutePath, outputPcm.absolutePath, valPathVideo
                                        )
                                    }.sessionId
                                )
                                return@executeWithArgumentsAsync
                            }
                        }
                        addAudioTemplate(
                            Uri.fromFile(outputMp3), paths, valIndex,
                            outputMp3.absolutePath, outputPcm.absolutePath, valPathVideo
                        )
                    }
                }.sessionId
            )
        } catch (e: Exception) {
            hideProgressAndFragment()
            e.printStackTrace()
        }
    }

    // ──────────────────────────────────────────────
    //  Entity media HTTP — create EntityAudio from template media
    // ──────────────────────────────────────────────

    /**
     * Create an [EntityAudio] from template media and extract waveform (8-param version).
     *
     * **Origin:** `EngineAudioManager.addEntitMediaHttp(entityMedia, i, uri, mediaPlayer, list, i2, str, str2?)`
     *
     * @param entityMedia  The template entity media descriptor
     * @param durationMs   Duration in milliseconds
     * @param uri          Audio source URI
     * @param mediaPlayer  Prepared MediaPlayer instance
     * @param httpPaths    List of HTTP URLs
     * @param nextIndex    Next template index to process
     * @param ffmpegPath   Path to the FFmpeg-ready audio file
     * @param pcmPath      PCM data path (nullable — if null, FFmpeg PCM extraction is run)
     */
    fun addEntityMediaHttp(
        entityMedia: EntityMedia, durationMs: Int, uri: Uri,
        mediaPlayer: MediaPlayer, httpPaths: List<String>,
        nextIndex: Int, ffmpegPath: String, pcmPath: String?
    ) {
        val tv = trackViewEntity ?: return
        val tmpl = template ?: return
        val round = Math.round(tv.width_screen * 0.077f)
        val round2 = Math.round(tv.second_in_screenNoScale * (durationMs / 1000.0f))

        val entityAudio: EntityAudio? = if (entityMedia.start != entityMedia.end) {
            val posX = if (tmpl.isNewCode) entityMedia.posX
                       else (entityMedia.posX / 1000.0f) * tv.second_in_screen
            val posY = if (tmpl.isNewCode) entityMedia.posY
                       else (entityMedia.posY / 1000.0f) * tv.second_in_screen
            EntityAudio(
                null, uri, posX, 0.0f, round.toFloat(), posY,
                entityMedia.max, tv.second_in_screenNoScale, durationMs,
                entityMedia.offset, entityMedia.offset_right, entityMedia.offset_left
            ).also { ea ->
                ea.setPathHttp(httpPaths)
                ea.mediaPlayer = mediaPlayer
                ea.videoPath = pcmPath
                ea.start = entityMedia.start
                ea.minDuration = entityMedia.start_original
                if (entityMedia.end != 0.0f) {
                    ea.end = entityMedia.end
                }
                ea.effectAudio = entityMedia.effectAudio ?: EffectAudio()
                ea.setFadeIn(entityMedia.duration_fade_in)
                ea.setFadeOut(entityMedia.duration_fade_out)
                tv.addAudio(ea)
            }
        } else null

        if (round2 <= 0 || round <= 0) {
            tv.invalidate()
            hideProgress()
            return
        }
        try {
            val pcmFile = File(tmpl.folder_template, "${System.currentTimeMillis()}_output.pcm")
            val args = buildPcmExtractionArgs(ffmpegPath, pcmFile)
            id_ffmpeg.add(
                FFmpegKit.executeWithArgumentsAsync(args, object : FFmpegSessionCompleteCallback {
                    override fun apply(fFmpegSession: FFmpegSession) {
                        if (fFmpegSession.returnCode.isValueSuccess) {
                            try {
                                entityAudio?.setAmps(
                                    PCMWaveformExtractor.extractWaveform(
                                        pcmFile.absolutePath, waveformSampleCount(round, round2)
                                    )
                                )
                                entityAudio?.setPathFfmpeg(ffmpegPath)
                                val i4 = nextIndex + 1
                                if (i4 >= tmpl.entityMediaList.size) {
                                    try {
                                        postOnUiThread {
                                            onUpdateTime()
                                            tv.invalidate()
                                            onProgressHide()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    val entityMedia2 = tmpl.entityMediaList[i4]
                                    if (entityMedia2.video_path != null) {
                                        entityMedia.video_path = AudioUtils.copyFromUri(
                                            context, Uri.parse(tmpl.uri_upload_extract_audio_video),
                                            tmpl.folder_template!!
                                        )
                                        if (tmpl.extension != null) {
                                            addAudioFromVideoWithExtension(tmpl.extension!!, entityMedia.video_path!!, i4)
                                        } else {
                                            startExtensionIndex = 0
                                            extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, i4)
                                        }
                                    } else if (entityMedia2.paths_https != null) {
                                        addAudioRecitersTemplate(entityMedia2.paths_https!!, i4, "")
                                    } else {
                                        addAudioTemplateHttp(Uri.parse(entityMedia2.uri), i4, null)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                postOnUiThread {
                                    tv.invalidate()
                                    onProgressHide()
                                }
                            }
                        }
                    }
                }).sessionId
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tv.invalidate()
    }

    /**
     * Create an [EntityAudio] from template media with explicit PCM path (9-param version).
     *
     * **Origin:** `EngineAudioManager.addEntitMediaHttp(entityMedia, i, uri, mediaPlayer, list, i2, str, str2, str3?)`
     *
     * @param entityMedia  The template entity media descriptor
     * @param durationMs   Duration in milliseconds
     * @param uri          Audio source URI
     * @param mediaPlayer  Prepared MediaPlayer instance
     * @param httpPaths    List of HTTP URLs
     * @param nextIndex    Next template index to process
     * @param ffmpegPath   Path to the FFmpeg-ready audio file
     * @param pcmPath      PCM data path (existing, no extraction needed)
     * @param videoPath    Video path for the source video
     */
    fun addEntityMediaHttp(
        entityMedia: EntityMedia, durationMs: Int, uri: Uri,
        mediaPlayer: MediaPlayer, httpPaths: List<String>,
        nextIndex: Int, ffmpegPath: String, pcmPath: String, videoPath: String?
    ) {
        val tv = trackViewEntity ?: return
        val tmpl = template ?: return
        val round = Math.round(tv.width_screen * 0.077f)
        val round2 = Math.round(tv.second_in_screenNoScale * (durationMs / 1000.0f))

        val entityAudioVal: EntityAudio? = if (entityMedia.start != entityMedia.end) {
            val posX = if (tmpl.isNewCode) entityMedia.posX
                       else (entityMedia.posX / 1000.0f) * tv.second_in_screen
            val posY = if (tmpl.isNewCode) entityMedia.posY
                       else (entityMedia.posY / 1000.0f) * tv.second_in_screen
            EntityAudio(
                null, uri, posX, 0.0f, round.toFloat(), posY,
                entityMedia.max, tv.second_in_screenNoScale, durationMs,
                entityMedia.offset, entityMedia.offset_right, entityMedia.offset_left
            ).also { ea ->
                ea.setPathHttp(httpPaths)
                ea.mediaPlayer = mediaPlayer
                ea.videoPath = videoPath
                ea.start = entityMedia.start
                ea.minDuration = entityMedia.start_original
                if (entityMedia.end != 0.0f) {
                    ea.end = entityMedia.end
                }
                ea.effectAudio = entityMedia.effectAudio ?: EffectAudio()
                ea.setFadeIn(entityMedia.duration_fade_in)
                ea.setFadeOut(entityMedia.duration_fade_out)
                tv.addAudio(ea)
            }
        } else null

        val entityAudio = entityAudioVal
        if (round2 <= 0 || round <= 0) {
            tv.invalidate()
            hideProgress()
        } else {
            executor.execute {
                try {
                    entityAudio?.setAmps(
                        PCMWaveformExtractor.extractWaveform(
                            pcmPath, waveformSampleCount(round, round2)
                        )
                    )
                    entityAudio?.setPathFfmpeg(ffmpegPath)
                    val i4 = nextIndex + 1
                    if (i4 >= tmpl.entityMediaList.size) {
                        try {
                            postOnUiThread {
                                onUpdateTime()
                                tv.invalidate()
                                onProgressHide()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val entityMedia2 = tmpl.entityMediaList[i4]
                        if (entityMedia2.video_path != null) {
                            entityMedia.video_path = AudioUtils.copyFromUri(
                                context, Uri.parse(tmpl.uri_upload_extract_audio_video),
                                tmpl.folder_template!!
                            )
                            if (tmpl.extension != null) {
                                addAudioFromVideoWithExtension(tmpl.extension!!, entityMedia.video_path!!, i4)
                            } else {
                                startExtensionIndex = 0
                                extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, i4)
                            }
                        } else if (entityMedia2.paths_https != null) {
                            addAudioRecitersTemplate(entityMedia2.paths_https!!, i4, "")
                        } else {
                            addAudioTemplateHttp(Uri.parse(entityMedia2.uri), i4, null)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    hideProgressAndFragment()
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Video audio extraction
    // ──────────────────────────────────────────────

    /**
     * Add audio extracted from a video with a file extension (e.g. ".mp4").
     *
     * **Origin:** `EngineAudioManager.addAudioFromVideoWithExtention(str, str2, i)`
     * Uses the extension to determine the extraction method and chains to
     * the next template entity.
     *
     * @param extension   Video file extension
     * @param videoPath   Path to the video file
     * @param nextIndex   Next template entity index
     */
    fun addAudioFromVideoWithExtension(extension: String, videoPath: String, nextIndex: Int) {
        try {
            val tmpl = template ?: return
            val file = File(File(tmpl.folder_template!!), "${System.currentTimeMillis()}_audio$extension")
            FFmpegKit.executeWithArgumentsAsync(
                arrayOf("-i", videoPath, "-vn", "-acodec", "copy", "-y", file.absolutePath)
            ) { fFmpegSession ->
                if (fFmpegSession.returnCode.isValueSuccess) {
                    addAudioTemplateHttp(Uri.fromFile(file), nextIndex, videoPath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Recursively extract audio from a video file chunk by chunk.
     *
     * **Origin:** `EngineAudioManager.extractAudioFromVideoRecursive(str, i, z, i2)`
     * Tries each extension in [EXTENSIONS] until one succeeds. If all fail,
     * falls back to [extractAudioFromVideo].
     *
     * @param videoPath  Path to the video file
     * @param extIndex   Current extension index to try
     * @param isFull     Whether this is for template (true) or standalone (false)
     * @param nextIndex  Next template entity index
     */
    fun extractAudioFromVideoRecursive(
        videoPath: String, extIndex: Int, isFull: Boolean, nextIndex: Int
    ) {
        if (isReleased) return

        if (extIndex < EXTENSIONS.size) {
            try {
                val tmpl = template ?: return
                val file = File(File(tmpl.folder_template!!), "${System.currentTimeMillis()}_audio${EXTENSIONS[extIndex]}")
                FFmpegKit.executeWithArgumentsAsync(
                    arrayOf("-i", videoPath, "-vn", "-acodec", "copy", "-y", file.absolutePath)
                ) { fFmpegSession ->
                    if (fFmpegSession.returnCode.isValueSuccess) {
                        tmpl.extension = EXTENSIONS[extIndex]
                        val fromFile = Uri.fromFile(file)
                        if (!isFull) {
                            hideFragment()
                            hideProgress()
                            onAddUriAudioToQuran(fromFile, videoPath)
                        } else {
                            addAudioTemplateHttp(fromFile, nextIndex, videoPath)
                        }
                        return@executeWithArgumentsAsync
                    }
                    startExtensionIndex++
                    extractAudioFromVideoRecursive(videoPath, startExtensionIndex, isFull, nextIndex)
                }
                return
            } catch (e: Exception) {
                e.printStackTrace()
                extractAudioFromVideo(videoPath, isFull)
                return
            }
        }
        extractAudioFromVideo(videoPath, isFull)
    }

    /**
     * Extract audio from a video file (fallback method).
     *
     * **Origin:** `EngineAudioManager.extractAudioFromVideo(str, z)`
     * Extracts the full audio track from a video using FFmpeg's `-vn -acodec copy`,
     * then calls [addAudioFromVideo] or [onAddUriAudioToQuran] with the result.
     *
     * @param videoPath Path to the video file
     * @param isFull    Whether to extract for template (true) or standalone (false)
     */
    fun extractAudioFromVideo(videoPath: String, isFull: Boolean) {
        try {
            val tmpl = template ?: return
            val file = File(File(tmpl.folder_template!!), "${System.currentTimeMillis()}_audio.mp3")
            FFmpegKit.executeWithArgumentsAsync(
                arrayOf("-i", videoPath, "-vn", "-acodec", "copy", "-y", file.absolutePath)
            ) { fFmpegSession ->
                if (fFmpegSession == null) {
                    hideProgressAndFragment()
                    return@executeWithArgumentsAsync
                }
                if (fFmpegSession.returnCode.isValueSuccess) {
                    val fromFile = Uri.fromFile(file)
                    tmpl.extension = ".mp3"
                    if (!isFull) {
                        onAddUriAudioToQuran(fromFile, videoPath)
                    } else {
                        addAudioTemplateHttp(fromFile, 0, videoPath)
                    }
                    return@executeWithArgumentsAsync
                }
                hideProgressAndFragment()
                onError("Video does not have a sound track")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    // ──────────────────────────────────────────────
    //  Entity audio manipulation
    // ──────────────────────────────────────────────

    /**
     * Create an [EntityAudio] on the timeline from a prepared audio source.
     *
     * **Origin:** `EngineAudioManager.changeEntityAudio(i, uri)`
     * Computes the position on the track, creates the EntityAudio,
     * then runs FFmpeg to extract PCM waveform data.
     *
     * @param durationMs Duration of the audio in milliseconds
     * @param uri        Audio source URI
     */
    fun changeEntityAudio(durationMs: Int, uri: Uri) {
        try {
            val tv = trackViewEntity ?: return
            val tmpl = template ?: return
            val scaleFactor = lastAudioRight()
            val round = Math.round(tv.width_screen * 0.077f)
            val round2 = Math.round(tv.second_in_screenNoScale * (durationMs / 1000.0f))
            val f = round2.toFloat()
            val entityAudio = EntityAudio(
                null, uri, scaleFactor, 0.0f, round.toFloat(),
                f + scaleFactor, f, tv.second_in_screenNoScale, durationMs
            )
            entityAudio.mediaPlayer = mPlayer
            entityAudio.effectAudio.end = entityAudio.end
            entityAudio.effectAudio.start = entityAudio.start
            entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
            tv.addAudio(entityAudio)
            if (round2 > 0 && round > 0) {
                val localPath = if (!uri.toString().contains("share_with_me")) {
                    AudioUtils.copyFromUri(context, uri, tmpl.folder_template!!)!!
                } else {
                    uri.toString()
                }
                val pcmFile = File(tmpl.folder_template, "${System.currentTimeMillis()}_output.pcm")
                val args = buildPcmExtractionArgs(localPath, pcmFile)
                val ffmpegInput = localPath
                id_ffmpeg.add(
                    FFmpegKit.executeWithArgumentsAsync(args, object : FFmpegSessionCompleteCallback {
                        override fun apply(fFmpegSession: FFmpegSession) {
                            if (fFmpegSession.returnCode.isValueSuccess) {
                                try {
                                    entityAudio.setAmps(
                                        PCMWaveformExtractor.extractWaveform(
                                            pcmFile.absolutePath, waveformSampleCount(round, round2)
                                        )
                                    )
                                    entityAudio.setPathFfmpeg(ffmpegInput)
                                    postOnUiThread {
                                        onUpdateTime()
                                        onUpdateBtnToEnd()
                                        onUpdateBtnToStart()
                                        onProgressHide()
                                        onHideFragment()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    hideProgressAndFragment()
                                }
                            } else {
                                hideProgressAndFragment()
                            }
                        }
                    }).sessionId
                )
                tv.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    /**
     * Create an [EntityAudio] on the timeline (template overload with HTTP paths).
     *
     * **Origin:** `EngineAudioManager.changeEntityAudio(i, uri, list, i2, str)`
     *
     * @param durationMs Duration in milliseconds
     * @param uri        Audio source URI
     * @param httpPaths  List of HTTP URLs
     * @param index      Template entity index
     * @param pcmPath    PCM data path
     */
    fun changeEntityAudio(
        durationMs: Int, uri: Uri, httpPaths: List<String>, index: Int, pcmPath: String
    ) {
        try {
            val tv = trackViewEntity ?: return
            val scaleFactor = lastAudioRight()
            val round = Math.round(tv.width_screen * 0.077f)
            val round2 = Math.round(tv.second_in_screenNoScale * (durationMs / 1000.0f))
            val f = round2.toFloat()
            val entityAudio = EntityAudio(
                null, uri, scaleFactor, 0.0f, round.toFloat(),
                f + scaleFactor, f, tv.second_in_screenNoScale, durationMs
            )
            entityAudio.mediaPlayer = mPlayer
            entityAudio.setPathHttp(httpPaths)
            entityAudio.effectAudio.end = entityAudio.end
            entityAudio.effectAudio.start = entityAudio.start
            entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
            tv.addAudio(entityAudio)
            if (round2 > 0 && round > 0) {
                executor.execute {
                    changeEntityAudioLambda(uri, round, round2, pcmPath, entityAudio, index)
                }
                tv.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    /**
     * Post-creation processing for template audio: copy local, extract waveform,
     * chain to next template entity or finalize.
     *
     * **Origin:** `EngineAudioManager.changeEntityAudioLambda(uri, i, i2, str, entityAudio, i3)`
     *
     * @param uri         Audio source URI
     * @param round       Track block height in pixels
     * @param round2      Track block width in pixels
     * @param pcmPath     Path to PCM data
     * @param entityAudio The entity audio to process
     * @param index       Template entity index (-1 for non-template)
     */
    private fun changeEntityAudioLambda(
        uri: Uri, round: Int, round2: Int, pcmPath: String,
        entityAudio: EntityAudio, index: Int
    ) {
        try {
            val tmpl = template ?: return
            val tv = trackViewEntity ?: return
            val copyFromUri = AudioUtils.copyFromUri(context, uri, tmpl.folder_template!!)!!
            val f = round.toFloat()
            entityAudio.setAmps(
                PCMWaveformExtractor.extractWaveform(
                    pcmPath, round2 / ((0.1f * f).toInt() + (f * 0.07f).toInt())
                )
            )
            entityAudio.setPathFfmpeg(copyFromUri)
            if (index != -1) {
                val i4 = index + 1
                if (i4 >= tmpl.entityMediaList.size) {
                    try {
                        postOnUiThread {
                            onUpdateTime()
                            onUpdateBtnToEnd()
                            onUpdateBtnToStart()
                            onProgressHide()
                            onHideFragment()
                        }
                        return
                    } catch (e: Exception) {
                        e.printStackTrace()
                        hideProgressAndFragment()
                        return
                    }
                }
                val entityMedia = tmpl.entityMediaList[index]
                val entityMedia2 = tmpl.entityMediaList[i4]
                if (entityMedia2.video_path != null) {
                    entityMedia.video_path = AudioUtils.copyFromUri(
                        context, Uri.parse(tmpl.uri_upload_extract_audio_video),
                        tmpl.folder_template!!
                    )
                    if (tmpl.extension != null) {
                        addAudioFromVideoWithExtension(tmpl.extension!!, entityMedia.video_path!!, i4)
                        return
                    } else {
                        startExtensionIndex = 0
                        extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, i4)
                        return
                    }
                }
                if (entityMedia2.paths_https != null) {
                    addAudioRecitersTemplate(entityMedia2.paths_https!!, i4, "")
                    return
                } else {
                    addAudioTemplateHttp(Uri.parse(entityMedia2.uri), i4, null)
                    return
                }
            }
            try {
                postOnUiThread {
                    tv.calculMaxTime()
                    onUpdateViewTime(tv.maxTime, tv.current_cursur_position)
                    tv.translateToEnd()
                    onUpdateTime()
                    onUpdateBtnToEnd()
                    onUpdateBtnToStart()
                    tv.invalidate()
                    onProgressHide()
                    onHideFragment()
                }
                return
            } catch (e: Exception) {
                e.printStackTrace()
                hideProgressAndFragment()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    /**
     * Create an [EntityAudio] from video audio extraction.
     *
     * **Origin:** `EngineAudioManager.changeEntityAudioFromVideo(i, uri, str)`
     *
     * @param durationMs Duration in milliseconds
     * @param uri        Audio source URI
     * @param videoPath  Path to the source video
     */
    fun changeEntityAudioFromVideo(durationMs: Int, uri: Uri, videoPath: String) {
        try {
            val tv = trackViewEntity ?: return
            val tmpl = template ?: return
            val scaleFactor = lastAudioRight()
            val round = Math.round(tv.width_screen * 0.077f)
            val round2 = Math.round(tv.second_in_screenNoScale * (durationMs / 1000.0f))
            val f = round2.toFloat()
            val entityAudio = EntityAudio(
                null, uri, scaleFactor, 0.0f, round.toFloat(),
                f + scaleFactor, f, tv.second_in_screenNoScale, durationMs
            )
            entityAudio.mediaPlayer = mPlayer
            entityAudio.effectAudio.end = entityAudio.end
            entityAudio.effectAudio.start = entityAudio.start
            entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
            tv.addAudio(entityAudio)
            if (round2 > 0 && round > 0) {
                val copyFromUri = AudioUtils.copyFromUri(context, uri, tmpl.folder_template!!)!!
                val pcmFile = File(tmpl.folder_template, "${System.currentTimeMillis()}_output.pcm")
                val args = buildPcmExtractionArgs(copyFromUri, pcmFile)
                id_ffmpeg.add(
                    FFmpegKit.executeWithArgumentsAsync(args, object : FFmpegSessionCompleteCallback {
                        override fun apply(fFmpegSession: FFmpegSession) {
                            if (fFmpegSession.returnCode.isValueSuccess) {
                                try {
                                    entityAudio.setAmps(
                                        PCMWaveformExtractor.extractWaveform(
                                            pcmFile.absolutePath, waveformSampleCount(round, round2)
                                        )
                                    )
                                    entityAudio.setPathFfmpeg(uri.path)
                                    entityAudio.videoPath = videoPath
                                    postOnUiThread {
                                        tv.invalidate()
                                        onProgressHide()
                                        onHideFragment()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    hideProgressAndFragment()
                                }
                            } else {
                                hideProgressAndFragment()
                            }
                        }
                    }).sessionId
                )
                tv.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    /**
     * Create an [EntityAudio] for a reciters ayah (sequential loading).
     *
     * **Origin:** `EngineAudioManager.changeEntityAudioReciters(i, uri, mediaPlayer, list, i2)`
     *
     * @param durationMs  Duration in milliseconds
     * @param uri         Audio source URI
     * @param mediaPlayer Prepared MediaPlayer for this ayah
     * @param list        Full reciters list (for chaining to next ayah)
     * @param index       Current index in the reciters list
     */
    fun changeEntityAudioReciters(
        durationMs: Int, uri: Uri, mediaPlayer: MediaPlayer,
        list: List<RecitersModel>, index: Int
    ) {
        try {
            val tv = trackViewEntity ?: return
            val tmpl = template ?: return
            val scaleFactor = lastAudioRight()
            val round = Math.round(tv.width_screen * 0.077f)
            val round2 = Math.round(tv.second_in_screenNoScale * (durationMs / 1000.0f))
            val f = round2.toFloat()
            val entityAudio = EntityAudio(
                null, uri, scaleFactor, 0.0f, round.toFloat(),
                f + scaleFactor, f, tv.second_in_screenNoScale, durationMs
            )
            entityAudio.effectAudio.end = entityAudio.end
            entityAudio.effectAudio.start = entityAudio.start
            entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
            entityAudio.mediaPlayer = mediaPlayer
            tv.addAudio(entityAudio)
            if (round2 > 0 && round > 0) {
                AudioUtils.copyToLocalAsync(
                    context, uri.toString(), tmpl.folder_template!!,
                    object : AudioUtils.Callback {
                        override fun onSuccess(localPath: String) {
                            try {
                                val waveFile = File(
                                    tmpl.folder_template,
                                    "${System.currentTimeMillis()}_audio_wave.png"
                                )
                                id_ffmpeg.add(
                                    FFmpegKit.executeWithArgumentsAsync(
                                        arrayOf(
                                            "-i", localPath,
                                            "-filter_complex",
                                            "aformat=channel_layouts=mono,showwavespic=s=${round}x${round2}:colors=#522123",
                                            "-frames:v", "1",
                                            "-y", waveFile.absolutePath
                                        ),
                                        object : FFmpegSessionCompleteCallback {
                                            override fun apply(fFmpegSession: FFmpegSession) {
                                                if (fFmpegSession.returnCode.isValueSuccess) {
                                                    try {
                                                        entityAudio.setPathFfmpeg(localPath)
                                                        postOnUiThread { tv.invalidate() }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        hideProgress()
                                                    }
                                                }
                                                addAudioReciters(list, index + 1)
                                            }
                                        }
                                    ).sessionId
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                hideProgress()
                            }
                        }

                        override fun onError(exc: Exception) {
                            exc.printStackTrace()
                            hideProgress()
                        }
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgress()
        }
    }

    /**
     * Duplicate an existing [EntityAudio] and insert it after the original.
     *
     * **Origin:** `EngineAudioManager.duplicateEntityAudio(i, entityAudio)`
     *
     * @param durationMs  Duration in milliseconds
     * @param entityAudio The audio entity to duplicate
     */
    fun duplicateEntityAudio(durationMs: Int, entityAudio: EntityAudio) {
        try {
            val tv = trackViewEntity ?: return
            val f = entityAudio.rect.right
            val entityAudio2 = EntityAudio(
                null, entityAudio.uri, f, entityAudio.rect.top, entityAudio.h,
                f + entityAudio.rect.width().toFloat(), entityAudio.max,
                entityAudio.secondInScreen, (durationMs / 1000.0f).toInt(),
                0.0f, 0.0f, 0.0f
            )
            entityAudio2.setAmps(entityAudio.amps)
            entityAudio2.setRenderer(entityAudio.getRenderer())
            entityAudio2.addPathHttp(entityAudio.pathsHttp)
            entityAudio2.mediaPlayer = entityAudio.mediaPlayer
            entityAudio2.rect.bottom = entityAudio.rect.bottom
            entityAudio2.setPathFfmpeg(entityAudio.getPathFfmpeg())
            entityAudio2.effectAudio = entityAudio.effectAudio
            entityAudio2.videoPath = entityAudio.videoPath
            entityAudio2.isApplyEffectInPreview = entityAudio.isApplyEffectInPreview
            entityAudio2.scaleFactor = entityAudio.scaleFactor
            entityAudio2.index = entityAudio.index + 1
            entityAudio2.setOffsetRight(entityAudio.getOffsetRight())
            entityAudio2.setOffsetLeft(entityAudio.getOffsetLeft())
            entityAudio2.offset = entityAudio.offset
            entityAudio2.end = Math.round(
                (Math.abs(Math.round((entityAudio.rect.right / tv.second_in_screen) * 1000.0f)) -
                 Math.abs(Math.round((entityAudio.rect.left / tv.second_in_screen) * 1000.0f))) +
                entityAudio.start
            ).toFloat()
            entityAudio2.start = entityAudio.start
            entityAudio2.minDuration = entityAudio.minDuration
            tv.addAudio(entityAudio2, entityAudio.index + 1)
            tv.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            hideProgressAndFragment()
        }
    }

    // ──────────────────────────────────────────────
    //  Progress and utilities
    // ──────────────────────────────────────────────

    /**
     * Update the loading progress UI.
     *
     * **Origin:** `EngineAudioManager.updateProgress(i, i2)`
     *
     * @param current Current item index
     * @param total   Total number of items
     */
    fun updateProgress(current: Int, total: Int) {
        val percent = if (total > 0) (current * 100) / total else 0
        onProgress(percent)
    }

    /**
     * Check network availability for HTTP audio sources.
     *
     * @return true if network is available, false otherwise
     */
    fun isNetworkAvailable(): Boolean = NetworkUtils.isNetworkAvailable(context)

    /**
     * Get the list of active FFmpeg session IDs (for external cancellation).
     */
    fun getFfmpegSessionIds(): List<Long> = id_ffmpeg.toList()

    /**
     * Cancel all active FFmpeg sessions tracked by this manager.
     */
    fun cancelFfmpegSessions() {
        id_ffmpeg.forEach { sessionId ->
            FFmpegKit.cancel(sessionId)
        }
        id_ffmpeg.clear()
    }

    /**
     * Release all resources held by this manager.
     *
     * **Origin:** Cleanup logic from `EngineActivity.onDestroy()`
     * Releases MediaPlayers, cancels FFmpeg sessions, shuts down executors.
     */
    fun release() {
        isReleased = true
        try {
            cancelFfmpegSessions()
        } catch (_: Exception) {
        }
        try {
            mPlayer?.let { mp ->
                if (mp.isPlaying) mp.pause()
                mp.release()
            }
            mPlayer = null
        } catch (_: Exception) {
        }
        try {
            executor.shutdownNow()
        } catch (_: Exception) {
        }
    }
}
