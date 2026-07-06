package hazem.nurmontage.videoquran.ui.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaPlayer
import android.util.Log
import android.widget.ImageButton
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.TimeFormatter
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.utils.animator.SmoothTimelineAnimator
import hazem.nurmontage.videoquran.utils.video.SmoothVideoAnimator
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * VideoPlayerController
 *
 * Encapsulates video background playback, timeline animation, and audio sync
 * for the engine screen.  All logic was originally spread across EngineActivity
 * and EngineTimelineManager; this class consolidates it into a self-contained
 * controller that receives view references and Activity-scoped callbacks
 * rather than holding a direct reference to EngineActivity.
 *
 * Key responsibilities:
 *   - Frame-by-frame video background via [SmoothVideoAnimator]
 *   - Timeline cursor animation via [SmoothTimelineAnimator]
 *   - Audio MediaPlayer synchronisation during playback
 *   - Frame bitmap loading / cropping / square-bitmap updates
 */
class VideoPlayerController(
    private val blurredImageView: BlurredImageView,
    private val trackViewEntity: TrackEntityView,
    private val btnPlayPause: ImageButton,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val callbacks: Callbacks
) {

    // ──────────────────────────────────────────────
    //  Callbacks interface — Activity-scoped ops
    // ──────────────────────────────────────────────

    /**
     * Callbacks that require Activity context or reference Activity internals
     * that this controller must not hold directly.
     */
    interface Callbacks {
        /** Load a bitmap from the given path at the given size using Glide. */
        fun loadBitmap(path: String, size: Int): Bitmap?
        /** Post [runnable] on the Activity's UI thread. */
        fun postOnUiThread(runnable: Runnable)
        /** Called from pausePlayer() to hide the resolution layout overlay. */
        fun onHideLayoutResolution()
        /** Called from pausePlayer() to pause the track-view scroll. */
        fun onTrackScrollPause()
        /** Update the time display during playback. */
        fun onUpdateTime(currentMs: Long)
        /** Update the start-view time label. */
        fun onUpdateStartViewTime(positionMs: Int)
        /** Update the cut-button enabled/disabled state. */
        fun onUpdateBtnCutState()
        /** Update the "go to end" button state. */
        fun onUpdateBtnToEnd()
        /** Update the "go to start" button state. */
        fun onUpdateBtnToStart()
        /** Called when a preview animation ends (to refresh effect fragment buttons). */
        fun onPreviewAnimationEnd()
        /** Provide the current [Template] — may be null if not yet loaded. */
        fun getTemplate(): Template?
    }

    // ──────────────────────────────────────────────
    //  State — playback
    // ──────────────────────────────────────────────

    var mIsPlaying: Boolean = false
        private set

    var startCursur: Int = 0
    var current_position_time: Int = 0
    var endFrame: Int = 0

    // ──────────────────────────────────────────────
    //  State — animators
    // ──────────────────────────────────────────────

    private var animator_frame_video: SmoothVideoAnimator? = null
    private var valueAnimator: SmoothTimelineAnimator? = null

    // ──────────────────────────────────────────────
    //  State — audio sync
    // ──────────────────────────────────────────────

    private var mPlayer: MediaPlayer? = null
    private var entityAudio_visible: EntityAudio? = null
    private var entityAudio_player: EntityAudio? = null
    private var lastIndexVisible: Int = 0
    private var endTimeAudioVisible: Int = 0
    private var timeFormatter: TimeFormatter? = null

    // ──────────────────────────────────────────────
    //  State — frame processing
    // ──────────────────────────────────────────────

    var isOnScroll: Boolean = false
        private set

    private val frameLock = Any()
    private var pendingFramePath: String? = null
    private var isProcessingFrame: Boolean = false

    private val frameProcessorRunnable = Runnable {
        var path: String?
        while (true) {
            synchronized(frameLock) {
                if (pendingFramePath == null) {
                    isProcessingFrame = false
                    return@Runnable
                } else {
                    path = pendingFramePath
                    pendingFramePath = null
                }
            }
            processFrame(path!!)
        }
    }

    // ══════════════════════════════════════════════
    //  Video frame playback (SmoothVideoAnimator)
    // ══════════════════════════════════════════════

    /**
     * Start frame-by-frame video background playback.
     *
     * Moved from `EngineActivity.start()` (line ~7261).
     * Creates a [SmoothVideoAnimator] that reads pre-extracted frames from
     * the template's VideoFrame directory and fires per-frame callbacks.
     * Frames are queued and processed on [executor] to avoid blocking
     * the Choreographer thread.
     */
    fun start() {
        val template = callbacks.getTemplate() ?: return
        if (template.ipad_type == IpadType.RECT.ordinal ||
            template.ipad_type == IpadType.ROUND_RECT.ordinal ||
            template.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal ||
            template.ipad_type == IpadType.CASSET.ordinal
        ) {
            return
        }
        isOnScroll = false
        val smoothVideoAnimator = SmoothVideoAnimator(
            trackViewEntity, template, 25,
            object : SmoothVideoAnimator.FrameUpdateListener {
                override fun onAnimationEnd() {}

                override fun onFrameUpdate(str: String) {
                    synchronized(frameLock) {
                        pendingFramePath = str
                        if (!isProcessingFrame) {
                            isProcessingFrame = true
                            executor.execute(frameProcessorRunnable)
                        }
                    }
                }
            }
        )
        animator_frame_video = smoothVideoAnimator
        smoothVideoAnimator.start()
    }

    /**
     * Stop frame-by-frame video background playback.
     *
     * Moved from `EngineActivity.stop()` (line ~7290).
     * Resets the square-video drawing flag and stops the animator.
     */
    fun stop() {
        blurredImageView.isDrawingSquareVideo = false
        animator_frame_video?.stop()
    }

    // ══════════════════════════════════════════════
    //  Pause / resume
    // ══════════════════════════════════════════════

    /**
     * Pause all playback: timeline animation, video frames, audio MediaPlayers.
     *
     * Moved from `EngineActivity.pausePlayer()` (line ~352).
     * Updates UI state (play/pause button, track view) and stops the
     * video frame animator.
     */
    fun pausePlayer() {
        try {
            callbacks.onHideLayoutResolution()
            if (mIsPlaying) {
                mIsPlaying = false
                pauseTimelineAnimation()
                trackViewEntity.isPlaying = mIsPlaying
                blurredImageView.isPlaying = mIsPlaying
                trackViewEntity.invalidate()
                for (entityAudio in trackViewEntity.entityListAudio) {
                    try {
                        entityAudio.mediaPlayer?.let { mp ->
                            if (mp.isPlaying) {
                                mp.pause()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                btnPlayPause.setImageResource(R.drawable.play_btn)
                stop()
            }
            callbacks.onTrackScrollPause()
        } catch (_: Exception) {
        }
    }

    // ══════════════════════════════════════════════
    //  Timeline animation
    // ══════════════════════════════════════════════

    /**
     * Start full timeline playback with cursor animation and audio sync.
     *
     * Moved from `EngineActivity.startTimelineAnimation()` (line ~3847).
     * Creates a [SmoothTimelineAnimator] that advances the timeline cursor
     * from [startCursur] to the max time, synchronising audio MediaPlayers
     * so that visible audio entities start/seek at the correct position.
     */
    fun startTimelineAnimation() {
        val template = callbacks.getTemplate() ?: return
        entityAudio_visible = null
        entityAudio_player = null
        lastIndexVisible = 0
        val maxTime = trackViewEntity.maxTime
        val timeLineW = trackViewEntity.timeLineW
        timeFormatter = TimeFormatter(maxTime.toLong())
        val smoothTimelineAnimator = SmoothTimelineAnimator(startCursur, maxTime, object : SmoothTimelineAnimator.AnimatorListener {
            override fun onUpdate(i: Int) {
                if (!mIsPlaying || i == 0) return
                val f = i.toFloat() / maxTime
                updateTime(i.toLong())
                blurredImageView.progress = f
                trackViewEntity.updateCursur(f * timeLineW)
                trackViewEntity.current_cursur_position = i
                val abs = Math.abs(Math.round((trackViewEntity.currentPosition / trackViewEntity.second_in_screen) * 1000.0f))
                if (abs > endTimeAudioVisible) {
                    entityAudio_visible = null
                }
                if (entityAudio_visible == null) {
                    for (i2 in lastIndexVisible until trackViewEntity.entityListAudio.size) {
                        val ea = trackViewEntity.entityListAudio[i2]
                        if (ea.visible() && ea.isVisible) {
                            entityAudio_visible = ea
                            endTimeAudioVisible = Math.round((entityAudio_visible!!.rect.right / trackViewEntity.second_in_screen) * 1000.0f)
                            lastIndexVisible = i2
                            break
                        }
                    }
                }
                try {
                    if (entityAudio_visible != null) {
                        if (entityAudio_player !== entityAudio_visible && mPlayer != null && mPlayer!!.isPlaying) {
                            mPlayer!!.pause()
                        }
                        mPlayer = entityAudio_visible!!.mediaPlayer
                        if (mPlayer != null && !mPlayer!!.isPlaying) {
                            entityAudio_player = entityAudio_visible
                            val abs2 = ((abs - Math.abs(Math.round((entityAudio_visible!!.rect.left / trackViewEntity.second_in_screen) * 1000.0f))) + entityAudio_visible!!.start).toInt()
                            if (abs2 <= mPlayer!!.duration) {
                                mPlayer!!.seekTo(abs2)
                            }
                            Log.d(TAG, "mPlayer pos: ${mPlayer!!.currentPosition}")
                            mPlayer!!.start()
                            Log.d(TAG, "mPlayer playing: ${mPlayer!!.isPlaying}")
                        }
                    } else if (mPlayer != null && mPlayer!!.isPlaying) {
                        mPlayer!!.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                callbacks.onUpdateStartViewTime(trackViewEntity.current_cursur_position)
                callbacks.onUpdateBtnCutState()
            }

            override fun onEnd() {
                if (mIsPlaying) {
                    mIsPlaying = false
                    trackViewEntity.isPlaying = mIsPlaying
                    blurredImageView.isPlaying = mIsPlaying
                    stop()
                    trackViewEntity.current_cursur_position = trackViewEntity.maxTime
                    trackViewEntity.updateCursur(trackViewEntity.maxTime)
                    try {
                        if (entityAudio_visible != null) {
                            entityAudio_visible!!.mediaPlayer?.let { mp ->
                                if (mp.isPlaying) mp.pause()
                            }
                        }
                        mPlayer?.let { mp ->
                            if (mp.isPlaying) mp.pause()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    startCursur = 0
                    current_position_time = 0
                    btnPlayPause.setImageResource(R.drawable.play_btn)
                    callbacks.onUpdateBtnToEnd()
                    callbacks.onUpdateBtnToStart()
                }
            }
        })
        valueAnimator = smoothTimelineAnimator
        smoothTimelineAnimator.start()
        if (template.isVideoSquare) {
            start()
        }
    }

    /**
     * Preview animation for a single [EntityAudio].
     *
     * Moved from `EngineActivity.startTimelineAnimationPreview()` (line ~3940).
     * Similar to [startTimelineAnimation] but only plays the given audio
     * entity's MediaPlayer, without cycling through the full audio list.
     */
    fun startTimelineAnimationPreview(entityAudio: EntityAudio) {
        val template = callbacks.getTemplate() ?: return
        val maxTime = trackViewEntity.maxTime
        val timeLineW = trackViewEntity.timeLineW
        timeFormatter = TimeFormatter(maxTime.toLong())
        val smoothTimelineAnimator = SmoothTimelineAnimator(startCursur, maxTime, object : SmoothTimelineAnimator.AnimatorListener {
            override fun onUpdate(i: Int) {
                if (!mIsPlaying || i == 0) return
                val f = i.toFloat() / maxTime
                updateTime(i.toLong())
                blurredImageView.progress = f
                trackViewEntity.updateCursur(f * timeLineW)
                trackViewEntity.current_cursur_position = i
                try {
                    val mp = entityAudio.mediaPlayer
                    if (mp != null && !mp.isPlaying) {
                        val abs = ((Math.abs(Math.round((trackViewEntity.currentPosition / trackViewEntity.second_in_screen) * 1000.0f)) - Math.abs(Math.round((entityAudio.rect.left / trackViewEntity.second_in_screen) * 1000.0f))) + entityAudio.start).toInt()
                        if (abs <= mp.duration) {
                            mp.seekTo(abs)
                        }
                        mp.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                callbacks.onUpdateStartViewTime(trackViewEntity.current_cursur_position)
            }

            override fun onEnd() {
                if (mIsPlaying) {
                    mIsPlaying = false
                    trackViewEntity.isPlaying = mIsPlaying
                    blurredImageView.isPlaying = mIsPlaying
                    stop()
                    try {
                        entityAudio.mediaPlayer?.let { mp ->
                            if (mp.isPlaying) mp.pause()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    startCursur = trackViewEntity.current_cursur_position
                }
                callbacks.onPreviewAnimationEnd()
            }
        })
        valueAnimator = smoothTimelineAnimator
        smoothTimelineAnimator.start()
        if (template.isVideoSquare) {
            start()
        }
    }

    /**
     * Stop the [SmoothTimelineAnimator].
     *
     * Moved from `EngineActivity.pauseTimelineAnimation()` (line ~7425).
     */
    fun pauseTimelineAnimation() {
        valueAnimator?.stop()
    }

    // ══════════════════════════════════════════════
    //  Frame update / seek
    // ══════════════════════════════════════════════

    /**
     * Calculate the frame number from the current cursor position and
     * trigger a bitmap update for the square video region.
     *
     * Moved from `EngineActivity.updateFrame()` (line ~7297).
     * Only runs when the template has video-square mode and playback is
     * not active.  During scrolling, the frame is loaded directly; during
     * playback it is handled by [processFrame].
     */
    fun updateFrame() {
        val template = callbacks.getTemplate() ?: return
        if (!template.isVideoSquare ||
            template.ipad_type == IpadType.RECT.ordinal ||
            template.ipad_type == IpadType.ROUND_RECT.ordinal ||
            template.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal ||
            template.ipad_type == IpadType.CASSET.ordinal ||
            mIsPlaying
        ) {
            return
        }
        var max = Math.max(1, Math.round((trackViewEntity.current_cursur_position / 1000.0f) * 25.0f))
        val min = Math.min(
            template.duration_video_media * 25,
            trackViewEntity.duration * 25
        )
        if (max > min) {
            max = ((max - 1) % min) + 1
        }
        val str = when {
            max < 10 -> "frame_000$max.jpg"
            max < 100 -> "frame_00$max.jpg"
            max < 1000 -> "frame_0$max.jpg"
            else -> "frame_$max.jpg"
        }
        isOnScroll = true
        updateSquareBitmap(
            File(template.folder_template + "/VideoFrame", str).absolutePath
        )
    }

    // ══════════════════════════════════════════════
    //  Frame processing (background thread)
    // ══════════════════════════════════════════════

    /**
     * Load a frame image and crop/update the ipad square bitmap during
     * video playback.
     *
     * Moved from `EngineActivity.processFrame()` (line ~7328).
     * Called on the [executor] thread.  Loads the bitmap via Glide,
     * then applies ipad-type-specific cropping and posts the result
     * to the UI thread for display.
     */
    internal fun processFrame(str: String) {
        var cropTo16x9: Bitmap?
        try {
            if (!(isOnScroll && mIsPlaying) && mIsPlaying) {
                val height = blurredImageView.getH()
                val bitmap = callbacks.loadBitmap(str, height)
                if (bitmap == null) {
                    return
                }
                val template = callbacks.getTemplate() ?: return

                if (template.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                    template.ipad_type != IpadType.GRADIENT.ordinal &&
                    template.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                    template.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                    template.ipad_type != IpadType.CASSET_IMG.ordinal
                ) {
                    if (template.ipad_type != IpadType.IPAD.ordinal &&
                        template.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                        template.ipad_type != IpadType.BOTTOM_RECT.ordinal &&
                        template.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                        template.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal
                    ) {
                        val width = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                        val i = (width * 1.13f).toInt()
                        val min = (Math.min(width, i) * 0.10800001f).toInt()
                        var round = Math.round(blurredImageView.bitmapOriginal!!.width * template.x_square)
                        var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * template.y_square)
                        var i2 = width + round
                        if (i2 > blurredImageView.bitmapOriginal!!.width) {
                            round -= i2 - blurredImageView.bitmapOriginal!!.width
                            i2 = blurredImageView.bitmapOriginal!!.width
                        }
                        var i3 = i + round2
                        if (i3 > blurredImageView.bitmapOriginal!!.height) {
                            round2 -= i3 - blurredImageView.bitmapOriginal!!.height
                            i3 = blurredImageView.bitmapOriginal!!.height
                        }
                        if (round < 0) {
                            round = 0
                        }
                        if (round2 < 0) {
                            round2 = 0
                        }
                        val rect = Rect(round, round2, i2, i3)
                        val width2 = (blurredImageView.bitmapOriginal!!.width * template.width_square).toInt()
                        val height2 = (blurredImageView.bitmapOriginal!!.height * template.height_square).toInt()
                        cropTo16x9 = UtilsBitmap.cropToSquareWithRoundCorners(
                            bitmap, rect, min, width2, height2
                        )
                        rect.right = rect.left + width2
                        rect.bottom = rect.top + height2
                        blurredImageView.rectSquare = rect
                    } else {
                        cropTo16x9 = UtilsBitmap.cropToSquareWithRoundCornersPlusScale(
                            bitmap, blurredImageView.rectSquare!!, blurredImageView.getRadius_square(),
                            blurredImageView.bitmapSquare!!.width, blurredImageView.bitmapSquare!!.height
                        )
                    }
                    callbacks.postOnUiThread(Runnable {
                        blurredImageView.bitmapSquare = cropTo16x9
                        if (!isOnScroll) {
                            blurredImageView.isDrawingSquareVideo = true
                        }
                        blurredImageView.invalidate()
                    })
                }
                cropTo16x9 = when (template.geTypeResize()) {
                    ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                        bitmap, blurredImageView.getW(), blurredImageView.getH()
                    )
                    ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                        bitmap, blurredImageView.getW(), blurredImageView.getH()
                    )
                    else -> BitmapCropper.cropTo16x9(
                        bitmap, blurredImageView.getW(), blurredImageView.getH()
                    )
                }
                callbacks.postOnUiThread(Runnable {
                    blurredImageView.bitmapSquare = cropTo16x9
                    if (!isOnScroll) {
                        blurredImageView.isDrawingSquareVideo = true
                    }
                    blurredImageView.invalidate()
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ══════════════════════════════════════════════
    //  Square bitmap update (scroll + playback)
    // ══════════════════════════════════════════════

    /**
     * Update the square video bitmap from a frame image path.
     *
     * Moved from `EngineActivity.updateSquareBitmap()` (line ~6707).
     * Runs on [executor].  Handles two cases:
     * - **Scrolling** ([isOnScroll] = true): only updates if NOT currently playing
     * - **Playing** ([isOnScroll] = false): only updates if currently playing
     *
     * Loads the bitmap via Glide, applies ipad-type-specific cropping,
     * and posts the result to the UI thread.
     */
    internal fun updateSquareBitmap(str: String) {
        if (isOnScroll) {
            if (mIsPlaying) {
                return
            }
        } else if (!mIsPlaying) {
            return
        }
        executor.execute {
            var bitmap: Bitmap?
            var cropTo16x9: Bitmap?
            try {
                try {
                    val height = blurredImageView.getH()
                    bitmap = callbacks.loadBitmap(str, height)
                } catch (e: Exception) {
                    e.printStackTrace()
                    callbacks.postOnUiThread(Runnable {
                        if (!isOnScroll) {
                            blurredImageView.isDrawingSquareVideo = true
                        }
                        blurredImageView.invalidate()
                    })
                    return@execute
                }
                if (bitmap == null) {
                    return@execute
                }
                val template = callbacks.getTemplate() ?: return@execute

                if (template.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                    template.ipad_type != IpadType.GRADIENT.ordinal &&
                    template.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                    template.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                    template.ipad_type != IpadType.CASSET_IMG.ordinal &&
                    template.ipad_type != IpadType.CASSET_IMG_BLUR.ordinal
                ) {
                    if (template.ipad_type != IpadType.IPAD.ordinal &&
                        template.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                        template.ipad_type != IpadType.BOTTOM_RECT.ordinal &&
                        template.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                        template.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal
                    ) {
                        val width = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                        val i = (width * 1.13f).toInt()
                        val min = (Math.min(width, i) * 0.10800001f).toInt()
                        var round = Math.round(blurredImageView.bitmapOriginal!!.width * template.x_square)
                        var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * template.y_square)
                        var i2 = width + round
                        if (i2 > blurredImageView.bitmapOriginal!!.width) {
                            round -= i2 - blurredImageView.bitmapOriginal!!.width
                            i2 = blurredImageView.bitmapOriginal!!.width
                        }
                        var i3 = i + round2
                        if (i3 > blurredImageView.bitmapOriginal!!.height) {
                            round2 -= i3 - blurredImageView.bitmapOriginal!!.height
                            i3 = blurredImageView.bitmapOriginal!!.height
                        }
                        if (round < 0) {
                            round = 0
                        }
                        if (round2 < 0) {
                            round2 = 0
                        }
                        val rect = Rect(round, round2, i2, i3)
                        val width2 = (blurredImageView.bitmapOriginal!!.width * template.width_square).toInt()
                        val height2 = (blurredImageView.bitmapOriginal!!.height * template.height_square).toInt()
                        blurredImageView.setBitmapSquare(
                            UtilsBitmap.cropToSquareWithRoundCorners(bitmap, rect, min, width2, height2)
                        )
                        rect.right = rect.left + width2
                        rect.bottom = rect.top + height2
                        blurredImageView.rectSquare = rect
                    } else {
                        blurredImageView.setBitmapSquare(
                            UtilsBitmap.cropToSquareWithRoundCornersPlusScale(
                                bitmap, blurredImageView.rectSquare!!, blurredImageView.getRadius_square(),
                                blurredImageView.bitmapSquare!!.width, blurredImageView.bitmapSquare!!.height
                            )
                        )
                    }
                    callbacks.postOnUiThread(Runnable {
                        if (!isOnScroll) {
                            blurredImageView.isDrawingSquareVideo = true
                        }
                        blurredImageView.invalidate()
                    })
                }
                cropTo16x9 = when (template.geTypeResize()) {
                    ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                        bitmap, blurredImageView.getW(), blurredImageView.getH()
                    )
                    ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                        bitmap, blurredImageView.getW(), blurredImageView.getH()
                    )
                    else -> BitmapCropper.cropTo16x9(
                        bitmap, blurredImageView.getW(), blurredImageView.getH()
                    )
                }
                blurredImageView.bitmapSquare = cropTo16x9
                callbacks.postOnUiThread(Runnable {
                    if (!isOnScroll) {
                        blurredImageView.isDrawingSquareVideo = true
                    }
                    blurredImageView.invalidate()
                })
            } finally {
                callbacks.postOnUiThread(Runnable {
                    if (!isOnScroll) {
                        blurredImageView.isDrawingSquareVideo = true
                    }
                    blurredImageView.invalidate()
                })
            }
        }
    }

    // ══════════════════════════════════════════════
    //  Time display
    // ══════════════════════════════════════════════

    /**
     * Update the time formatter and pass the current time to the
     * Activity for display.
     *
     * Moved from `EngineActivity.updateTime()` (line ~4002).
     */
    private fun updateTime(j: Long) {
        val tf = timeFormatter
        if (tf == null) {
            timeFormatter = TimeFormatter(trackViewEntity.maxTime.toLong())
        } else {
            tf.setTotalDurationMs(trackViewEntity.maxTime.toLong())
        }
        val formatTime = timeFormatter!!.formatTime(j)
        blurredImageView.setCurrentTime(formatTime.first as String, formatTime.second as String)
        callbacks.onUpdateTime(j)
    }

    // ══════════════════════════════════════════════
    //  Public state setters
    // ══════════════════════════════════════════════

    /**
     * Set the playing state.  Exposed so that EngineActivity can
     * synchronise its own `mIsPlaying` flag when initiating playback
     * (e.g. before calling [startTimelineAnimation]).
     */
    fun setPlaying(playing: Boolean) {
        mIsPlaying = playing
    }

    /**
     * Set [isOnScroll] from external code (e.g. when the user
     * finishes scrolling the timeline).
     */
    fun setOnScroll(scroll: Boolean) {
        isOnScroll = scroll
    }

    // ══════════════════════════════════════════════
    //  Query
    // ══════════════════════════════════════════════

    fun isPlaying(): Boolean = mIsPlaying

    fun getAnimatorFrameVideo(): SmoothVideoAnimator? = animator_frame_video

    fun getValueAnimator(): SmoothTimelineAnimator? = valueAnimator

    // ══════════════════════════════════════════════
    //  Release
    // ══════════════════════════════════════════════

    /**
     * Release all player resources.  Call in onDestroy().
     *
     * Stops animators and clears frame processing state.
     */
    fun release() {
        try {
            animator_frame_video?.stop()
        } catch (_: Exception) {
        }
        try {
            valueAnimator?.stop()
        } catch (_: Exception) {
        }
        animator_frame_video = null
        valueAnimator = null
        mPlayer = null
        entityAudio_visible = null
        entityAudio_player = null
        mIsPlaying = false
        synchronized(frameLock) {
            pendingFramePath = null
            isProcessingFrame = false
        }
    }

    companion object {
        private const val TAG = "VideoPlayerController"
    }
}
