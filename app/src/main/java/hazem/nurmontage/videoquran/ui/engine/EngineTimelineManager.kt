package hazem.nurmontage.videoquran.ui.engine

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.utils.TimeFormatter
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.utils.animator.SmoothTimelineAnimator
import hazem.nurmontage.videoquran.utils.video.SmoothVideoAnimator
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.fragment.EditEntityFragment
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import hazem.nurmontage.videoquran.fragment.EditTrslEntityFragment
import hazem.nurmontage.videoquran.fragment.audio_effect.*
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.File
import java.util.concurrent.TimeUnit

// ==========================================================================
// EngineTimelineManager.kt
// Timeline/seekbar/playback methods for EngineActivity, extracted as extension functions.
// ==========================================================================

fun EngineActivity.startTimelineAnimation() {
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
            if (blurredImageView != null) {
                updateTime(i.toLong())
                blurredImageView.progress = f
            }
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
                        mPlayer!!.start()
                    }
                } else if (mPlayer != null && mPlayer!!.isPlaying) {
                    mPlayer!!.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            updateStartViewTime(trackViewEntity.current_cursur_position)
            updateBtnCutState()
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
                    if (entityAudio_visible != null && entityAudio_visible!!.mediaPlayer != null && entityAudio_visible!!.mediaPlayer!!.isPlaying) {
                        entityAudio_visible!!.mediaPlayer!!.pause()
                    }
                    if (mPlayer != null && mPlayer!!.isPlaying) {
                        mPlayer!!.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                startCursur = 0
                current_position_time = 0
                if (btnPlayPause != null) {
                    btnPlayPause!!.setImageResource(hazem.nurmontage.videoquran.R.drawable.play_btn)
                }
                updateBtnToEnd()
                updateBtnToStart()
            }
        }
    })
    valueAnimator = smoothTimelineAnimator
    smoothTimelineAnimator.start()
    if (mTemplate!!.isVideoSquare) {
        start()
    }
}

fun EngineActivity.startTimelineAnimationPreview(entityAudio: EntityAudio) {
    val maxTime = trackViewEntity.maxTime
    val timeLineW = trackViewEntity.timeLineW
    timeFormatter = TimeFormatter(maxTime.toLong())
    val smoothTimelineAnimator = SmoothTimelineAnimator(startCursur, maxTime, object : SmoothTimelineAnimator.AnimatorListener {
        override fun onUpdate(i: Int) {
            if (!mIsPlaying || i == 0) return
            val f = i.toFloat() / maxTime
            if (blurredImageView != null) {
                updateTime(i.toLong())
                blurredImageView.progress = f
            }
            trackViewEntity.updateCursur(f * timeLineW)
            trackViewEntity.current_cursur_position = i
            try {
                if (entityAudio.mediaPlayer != null && !entityAudio.mediaPlayer!!.isPlaying) {
                    val abs = ((Math.abs(Math.round((trackViewEntity.currentPosition / trackViewEntity.second_in_screen) * 1000.0f)) - Math.abs(Math.round((entityAudio.rect.left / trackViewEntity.second_in_screen) * 1000.0f))) + entityAudio.start).toInt()
                    if (abs <= entityAudio.mediaPlayer!!.duration) {
                        entityAudio.mediaPlayer!!.seekTo(abs)
                    }
                    entityAudio.mediaPlayer!!.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            updateStartViewTime(trackViewEntity.current_cursur_position)
        }

        override fun onEnd() {
            if (mIsPlaying) {
                mIsPlaying = false
                trackViewEntity.isPlaying = mIsPlaying
                blurredImageView.isPlaying = mIsPlaying
                stop()
                try {
                    if (entityAudio.mediaPlayer != null && entityAudio.mediaPlayer!!.isPlaying) {
                        entityAudio.mediaPlayer!!.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                startCursur = trackViewEntity.current_cursur_position
            }
            VolumeFragment.instance?.updateButton()
            SpeedFragment.instance?.updateButton()
            FadeInOutFragment.instance?.updateButton()
            EchoEffectFragment.instance?.updateButton()
            EnhanceVoiceFragment.instance?.updateButton()
            RemoveNoiceFragment.instance?.updateButton()
        }
    })
    valueAnimator = smoothTimelineAnimator
    smoothTimelineAnimator.start()
    if (mTemplate!!.isVideoSquare) {
        start()
    }
}

fun EngineActivity.pauseTimelineAnimation() {
    valueAnimator?.stop()
}

fun EngineActivity.updateTime(j: Long) {
    val tf = timeFormatter
    if (tf == null) {
        timeFormatter = TimeFormatter(trackViewEntity.maxTime.toLong())
    } else {
        tf.setTotalDurationMs(trackViewEntity.maxTime.toLong())
    }
    val formatTime = timeFormatter!!.formatTime(j)
    blurredImageView.setCurrentTime(formatTime.first as String, formatTime.second as String)
}

fun EngineActivity.updateTime() {
    trackViewEntity.calculMaxTime()
    updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
    if (trackViewEntity.current_cursur_position <= trackViewEntity.maxTime) {
        updateTime(trackViewEntity.current_cursur_position.toLong())
        val trackEntityView = trackViewEntity
        trackEntityView.current_cursur_position = trackEntityView.current_cursur_position
        blurredImageView.progress = trackViewEntity.current_cursur_position.toFloat() / trackViewEntity.maxTime
    }
}

fun EngineActivity.updateTimeToEndAya() {
    trackViewEntity.calculMaxTime()
    trackViewEntity.translateToEnd()
    updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
    if (trackViewEntity.current_cursur_position <= trackViewEntity.maxTime) {
        updateTime(trackViewEntity.current_cursur_position.toLong())
        val trackEntityView = trackViewEntity
        trackEntityView.current_cursur_position = trackEntityView.current_cursur_position
        blurredImageView.progress = trackViewEntity.current_cursur_position.toFloat() / trackViewEntity.maxTime
    }
}

fun EngineActivity.updateEndViewTime(i: Int) {
    val j = i.toLong()
    val seconds = TimeUnit.MILLISECONDS.toSeconds(j) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(j))
    val str = if (seconds < 10) {
        "${TimeUnit.MILLISECONDS.toMinutes(j)}:0$seconds"
    } else {
        "${TimeUnit.MILLISECONDS.toMinutes(j)}:$seconds"
    }
    tv_endTime!!.text = "/$str"
}

fun EngineActivity.updateStartViewTime(i: Int) {
    val j = i.toLong()
    val seconds = TimeUnit.MILLISECONDS.toSeconds(j) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(j))
    val str = if (seconds < 10) {
        "${TimeUnit.MILLISECONDS.toMinutes(j)}:0$seconds"
    } else {
        "${TimeUnit.MILLISECONDS.toMinutes(j)}:$seconds"
    }
    tv_currentTime!!.text = str
}

fun EngineActivity.updateViewTime(i: Int, i2: Int) {
    val j = i2.toLong()
    val seconds = TimeUnit.MILLISECONDS.toSeconds(j) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(j))
    val str = if (seconds < 10) {
        "${TimeUnit.MILLISECONDS.toMinutes(j)}:0$seconds"
    } else {
        "${TimeUnit.MILLISECONDS.toMinutes(j)}:$seconds"
    }
    val j2 = i.toLong()
    val seconds2 = TimeUnit.MILLISECONDS.toSeconds(j2) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(j2))
    val str2 = if (seconds2 < 10) {
        "${TimeUnit.MILLISECONDS.toMinutes(j2)}:0$seconds2"
    } else {
        "${TimeUnit.MILLISECONDS.toMinutes(j2)}:$seconds2"
    }
    tv_currentTime!!.text = str
    tv_endTime!!.text = "/$str2"
}

fun EngineActivity.initTimeLineView() {
    tv_currentTime = findViewById(hazem.nurmontage.videoquran.R.id.tv_current_time)
    tv_endTime = findViewById(hazem.nurmontage.videoquran.R.id.tv_end_time)
    val trackEntityView = findViewById<hazem.nurmontage.videoquran.views.TrackEntityView>(hazem.nurmontage.videoquran.R.id.time_line_view)
    trackViewEntity = trackEntityView
    trackEntityView.setiTrimLineCallback(iTrimLineCallback)
    trackViewEntity.scaleFactor = mTemplate!!.scale_timeline
    trackViewEntity.post {
        val screenWidth = ScreenUtils.getScreenWidth(this)
        val audioPosition = screenWidth * 0.12f
        trackViewEntity.setSecond_in_screen(audioPosition)
        trackViewEntity.setSecond_in_screen(audioPosition, 0, screenWidth)
        trackViewEntity.maxTime = 0
        trackViewEntity.init(screenWidth, trackViewEntity.height)
        trackViewEntity.setPosCursur(mTemplate!!.currentCursur)
        startCursur = trackViewEntity.current_cursur_position
        updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
    }
}

fun EngineActivity.checkSplitEntity() {
    if (EditEntityFragment.instance == null || trackViewEntity.selectedEntity == null) {
        return
    }
    EditEntityFragment.instance!!.checkSplitEntity(
        trackViewEntity.selectedEntity!!, -trackViewEntity.getCurrentPosition()
    )
}

fun EngineActivity.checkSplitTrslEntity() {
    if (EditTrslEntityFragment.instance == null || trackViewEntity.selectedEntity == null) {
        return
    }
    EditTrslEntityFragment.instance!!.checkSplitEntity(
        trackViewEntity.selectedEntity!!, -trackViewEntity.getCurrentPosition()
    )
}

fun EngineActivity.checkSplitAudio() {
    if (EditMediaFragment.instance == null || trackViewEntity.selectedEntity !is EntityAudio) {
        return
    }
    val f = -trackViewEntity.getCurrentPosition()
    EditMediaFragment.instance!!.checkSplit(trackViewEntity.selectedEntity!! as EntityAudio, f)
}

fun EngineActivity.start() {
    if (mTemplate!!.ipad_type == IpadType.RECT.ordinal ||
        mTemplate!!.ipad_type == IpadType.ROUND_RECT.ordinal ||
        mTemplate!!.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal ||
        mTemplate!!.ipad_type == IpadType.CASSET.ordinal
    ) {
        return
    }
    isOnScroll = false
    val smoothVideoAnimator = SmoothVideoAnimator(
        trackViewEntity, mTemplate!!, 25,
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

fun EngineActivity.stop() {
    if (try { blurredImageView; true } catch (_: UninitializedPropertyAccessException) { false }) {
        blurredImageView.isDrawingSquareVideo = false
    }
    animator_frame_video?.stop()
}

fun EngineActivity.updateFrame() {
    val template = mTemplate
    if (template == null || !template.isVideoSquare ||
        mTemplate!!.ipad_type == IpadType.RECT.ordinal ||
        mTemplate!!.ipad_type == IpadType.ROUND_RECT.ordinal ||
        mTemplate!!.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal ||
        mTemplate!!.ipad_type == IpadType.CASSET.ordinal ||
        mIsPlaying
    ) {
        return
    }
    var max = Math.max(1, Math.round((trackViewEntity.current_cursur_position / 1000.0f) * 25.0f))
    val min = Math.min(
        mTemplate!!.duration_video_media * 25,
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
        File(mTemplate!!.folder_template + "/VideoFrame", str).absolutePath
    )
}

fun EngineActivity.processFrame(str: String) {
    var cropTo16x9: Bitmap?
    try {
        if (!(isOnScroll && mIsPlaying) && mIsPlaying) {
            val height = blurredImageView.getH()
            val bitmap = Glide.with(this as androidx.fragment.app.FragmentActivity)
                .asBitmap()
                .load(str)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .override(height, height)
                .submit().get()
            if (bitmap == null) {
                return
            }
            if (mTemplate!!.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                mTemplate!!.ipad_type != IpadType.GRADIENT.ordinal &&
                mTemplate!!.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                mTemplate!!.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                mTemplate!!.ipad_type != IpadType.CASSET_IMG.ordinal
            ) {
                if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal &&
                    mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                    mTemplate!!.ipad_type != IpadType.BOTTOM_RECT.ordinal &&
                    mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                    mTemplate!!.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal
                ) {
                    val width = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                    val i = (width * 1.13f).toInt()
                    val min = (Math.min(width, i) * 0.10800001f).toInt()
                    var round = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                    var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
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
                    val width2 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                    val height2 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
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
                runOnUiThread {
                    blurredImageView.bitmapSquare = cropTo16x9
                    if (!isOnScroll) {
                        blurredImageView.isDrawingSquareVideo = true
                    }
                    blurredImageView.invalidate()
                }
            }
            cropTo16x9 = when (mTemplate!!.geTypeResize()) {
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
            runOnUiThread {
                blurredImageView.bitmapSquare = cropTo16x9
                if (!isOnScroll) {
                    blurredImageView.isDrawingSquareVideo = true
                }
                blurredImageView.invalidate()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.updateSquareBitmap(str: String) {
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
                bitmap = Glide.with(this as androidx.fragment.app.FragmentActivity)
                    .asBitmap()
                    .load(str)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .override(height, height)
                    .submit().get()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    if (!isOnScroll) {
                        blurredImageView.isDrawingSquareVideo = true
                    }
                    blurredImageView.invalidate()
                }
                return@execute
            }
            if (bitmap == null) {
                return@execute
            }
            if (mTemplate!!.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                mTemplate!!.ipad_type != IpadType.GRADIENT.ordinal &&
                mTemplate!!.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                mTemplate!!.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                mTemplate!!.ipad_type != IpadType.CASSET_IMG.ordinal &&
                mTemplate!!.ipad_type != IpadType.CASSET_IMG_BLUR.ordinal
            ) {
                if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal &&
                    mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                    mTemplate!!.ipad_type != IpadType.BOTTOM_RECT.ordinal &&
                    mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                    mTemplate!!.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal
                ) {
                    val width = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                    val i = (width * 1.13f).toInt()
                    val min = (Math.min(width, i) * 0.10800001f).toInt()
                    var round = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                    var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
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
                    val width2 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                    val height2 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
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
                runOnUiThread {
                    if (!isOnScroll) {
                        blurredImageView.isDrawingSquareVideo = true
                    }
                    blurredImageView.invalidate()
                }
            }
            cropTo16x9 = when (mTemplate!!.geTypeResize()) {
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
            runOnUiThread {
                if (!isOnScroll) {
                    blurredImageView.isDrawingSquareVideo = true
                }
                blurredImageView.invalidate()
            }
        } finally {
            runOnUiThread {
                if (!isOnScroll) {
                    blurredImageView.isDrawingSquareVideo = true
                }
                blurredImageView.invalidate()
            }
        }
    }
}

fun EngineActivity.updateBtnToStart() {
    try {
        if (try { btnToStart; true } catch (_: UninitializedPropertyAccessException) { false }) {
            btnToStart.isEnabled = trackViewEntity.current_cursur_position > 0
            if (btnToStart.isEnabled) {
                btnToStart.setColorFilter(-1, android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                btnToStart.setColorFilter(-8355712, android.graphics.PorterDuff.Mode.SRC_IN)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.updateBtnToEnd() {
    try {
        if (try { btnToEnd; true } catch (_: UninitializedPropertyAccessException) { false }) {
            btnToEnd.isEnabled = trackViewEntity.current_cursur_position < trackViewEntity.maxTime
            if (btnToEnd.isEnabled) {
                btnToEnd.setColorFilter(-1, android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                btnToEnd.setColorFilter(-8355712, android.graphics.PorterDuff.Mode.SRC_IN)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.addUpdateAnim(
    entityBismilahTimeline: EntityBismilahTimeline?,
    entityBismilahTimeline2: hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
) {
    if (entityBismilahTimeline == null) {
        return
    }
    if (entityBismilahTimeline.getTransition() == null) {
        entityBismilahTimeline.setTransition(hazem.nurmontage.videoquran.model.Transition())
    }
    entityBismilahTimeline.getTransition()!!.isOut = entityBismilahTimeline2.getTransition()!!.isOut
    entityBismilahTimeline.getTransition()!!.type_out = entityBismilahTimeline2.getTransition()!!.type_out
    entityBismilahTimeline.getTransition()!!.duration_out = entityBismilahTimeline2.getTransition()!!.duration_out
    entityBismilahTimeline.getTransition()!!.isIn = entityBismilahTimeline2.getTransition()!!.isIn
    entityBismilahTimeline.getTransition()!!.type_in = entityBismilahTimeline2.getTransition()!!.type_in
    entityBismilahTimeline.getTransition()!!.duration_in = entityBismilahTimeline2.getTransition()!!.duration_in
}

fun EngineActivity.addUpdateAnim(
    entityBismilahTimeline: EntityBismilahTimeline?,
    entityQuranTimeline: hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
) {
    if (entityBismilahTimeline == null) {
        return
    }
    if (entityBismilahTimeline.getTransition() == null) {
        entityBismilahTimeline.setTransition(hazem.nurmontage.videoquran.model.Transition())
    }
    entityBismilahTimeline.getTransition()!!.isOut = entityQuranTimeline!!.getTransition()!!.isOut
    entityBismilahTimeline.getTransition()!!.type_out = entityQuranTimeline!!.getTransition()!!.type_out
    entityBismilahTimeline.getTransition()!!.duration_out = entityQuranTimeline!!.getTransition()!!.duration_out
    entityBismilahTimeline.getTransition()!!.isIn = entityQuranTimeline!!.getTransition()!!.isIn
    entityBismilahTimeline.getTransition()!!.type_in = entityQuranTimeline!!.getTransition()!!.type_in
    entityBismilahTimeline.getTransition()!!.duration_in = entityQuranTimeline!!.getTransition()!!.duration_in
}
