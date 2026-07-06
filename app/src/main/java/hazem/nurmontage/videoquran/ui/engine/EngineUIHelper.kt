package hazem.nurmontage.videoquran.ui.engine

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.*
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.*
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.utils.AspectRatioCalculator
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.fragment.AddQuranFragment
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.blurred.updateIpad
import hazem.nurmontage.videoquran.views.ButtonCustumFont
import hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar
import hazem.nurmontage.videoquran.views.TextCustumFont
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

// ==========================================================================
// EngineUIHelper.kt
// UI setup, dialogs, fragment management, image/video background handling
// for EngineActivity, extracted as extension functions.
// ==========================================================================

fun EngineActivity.iniTypeImg() {
    executor.execute(Runnable {
        var bitmap: Bitmap
        var cropTo16x9: Bitmap
        var cropToSquareWithRoundCorners: Bitmap? = null
        var bitmap2: Bitmap
        var rect: Rect
        var clrTrsl: Int
        try {
            blurredImageView.initCanvasDimension(blurredImageView.getW(), blurredImageView.getH(), mTemplate!!.geTypeResize())
            val height = blurredImageView.getH()
            try {
                bitmap = Glide.with(this as androidx.fragment.app.FragmentActivity).asBitmap()
                    .load(mTemplate!!.uri_bg)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .override(height, height)
                    .submit().get() as Bitmap
            } catch (unused: Exception) {
                mTemplate!!.color_ipad = -1
                bitmap = Glide.with(this as androidx.fragment.app.FragmentActivity).asBitmap()
                    .load(R.drawable.bg_19)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .override(height, height)
                    .submit().get() as Bitmap
            }
            blurredImageView.bitmapOriginal = setupOriginalBitmap(bitmap, height)
            cropTo16x9 = when (mTemplate!!.geTypeResize()) {
                ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())!!
                ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())!!
                else -> BitmapCropper.cropTo16x9(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())!!
            }
            blurredImageView.isGlass = mTemplate!!.isGlass
            blurredImageView.isVideo = false
            blurredImageView.updatePosCanvas(cropTo16x9)
            blurredImageView.updateIpad(cropTo16x9!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())

            if (mTemplate!!.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                val width = (blurredImageView.ipad_rect!!.width() * 0.6f).toInt()
                var round = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                var i3 = width + round
                if (i3 > blurredImageView.bitmapOriginal!!.width) {
                    round -= i3 - blurredImageView.bitmapOriginal!!.width
                    i3 = blurredImageView.bitmapOriginal!!.width
                }
                var i4 = width + round2
                if (i4 > blurredImageView.bitmapOriginal!!.height) {
                    round2 -= i4 - blurredImageView.bitmapOriginal!!.height
                    i4 = blurredImageView.bitmapOriginal!!.height
                }
                if (round < 0) round = 0
                if (round2 >= 0) {
                    // i2 = round2
                }
                val i2 = if (round2 >= 0) round2 else 0
                val rect2 = Rect(round, i2, i3, i4)
                blurredImageView.radius_square = width
                val width2 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                val height2 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                val cropToSquareWithRoundCorners2 = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect2, width, width2, height2)
                rect2.right = rect2.left + width2
                rect2.bottom = rect2.top + height2
                blurredImageView.rectSquare = rect2
                bitmap2 = cropToSquareWithRoundCorners2
                rect = rect2
            } else {
                if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal && mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal && mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal) {
                    val width3 = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                    val height3 = (cropTo16x9!!.height * 0.5355f).toInt()
                    var round3 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                    var round4 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                    var i5 = width3 + round3
                    if (i5 > blurredImageView.bitmapOriginal!!.width) {
                        round3 -= i5 - blurredImageView.bitmapOriginal!!.width
                        i5 = blurredImageView.bitmapOriginal!!.width
                    }
                    var i6 = height3 + round4
                    if (i6 > blurredImageView.bitmapOriginal!!.height) {
                        round4 -= i6 - blurredImageView.bitmapOriginal!!.height
                        i6 = blurredImageView.bitmapOriginal!!.height
                    }
                    if (round3 < 0) round3 = 0
                    if (round4 < 0) round4 = 0
                    val rect3 = Rect(round3, round4, i5, i6)
                    val width4 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                    val height4 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                    val cropToSquare = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect3, width4, height4)
                    blurredImageView.bitmapSquare = cropToSquare
                    blurredImageView.radius_square = 0
                    rect3.right = rect3.left + width4
                    rect3.bottom = rect3.top + height4
                    blurredImageView.rectSquare = rect3
                    bitmap2 = cropToSquare
                    rect = rect3
                }
                val width5 = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                val i7 = (width5 * 1.13f).toInt()
                val min = Math.min(width5, i7)
                var round5 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                var round6 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                var i8 = width5 + round5
                if (i8 > blurredImageView.bitmapOriginal!!.width) {
                    round5 -= i8 - blurredImageView.bitmapOriginal!!.width
                    i8 = blurredImageView.bitmapOriginal!!.width
                }
                var i9 = i7 + round6
                if (i9 > blurredImageView.bitmapOriginal!!.height) {
                    round6 -= i9 - blurredImageView.bitmapOriginal!!.height
                    i9 = blurredImageView.bitmapOriginal!!.height
                }
                if (round5 < 0) round5 = 0
                if (round6 < 0) round6 = 0
                val rect4 = Rect(round5, round6, i8, i9)
                if (mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                    val width6 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                    val height5 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                    val cropToSquare2 = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect4, width6, height5)
                    blurredImageView.bitmapSquare = cropToSquare2
                    blurredImageView.radius_square = 0
                    rect4.right = rect4.left + width6
                    rect4.bottom = rect4.top + height5
                    blurredImageView.rectSquare = rect4
                    cropToSquareWithRoundCorners = cropToSquare2
                } else {
                    val i10 = (min * 0.10800001f).toInt()
                    blurredImageView.radius_square = i10
                    val width7 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                    val height6 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                    cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect4, i10, width7, height6)
                    rect4.right = rect4.left + width7
                    rect4.bottom = rect4.top + height6
                    blurredImageView.rectSquare = rect4
                }
                bitmap2 = cropToSquareWithRoundCorners!!
                rect = rect4
            }
            if (mTemplate!!.gradient != null) {
                blurredImageView.setBitmap(UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap2, mTemplate!!.gradient!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
            } else {
                blurredImageView.setBitmap(UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap2, mTemplate!!.color_ipad, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
            }
            clrTrsl = if (mTemplate!!.ipad_type == IpadType.BLUE_TYPE.ordinal) {
                blurredImageView.paintLecture.color
            } else {
                if (blurredImageView.paintLecture.color == -1) androidx.core.view.InputDeviceCompat.SOURCE_ANY else Constants.COLOR_TRANSLATION
            }
            blurredImageView.clr_trsl = clrTrsl
            blurredImageView.clr_aya = blurredImageView.paintLecture.color
            addEntityFromTemplate()
        } catch (e: Exception) {
            android.util.Log.e("Tag : ", "init ${e.message}")
        }
    })
}

fun EngineActivity.initTypeVideo() {
    val activity = this
    executor.execute {
        try {
            blurredImageView.initCanvasDimension(
                blurredImageView.getW(), blurredImageView.getH(), mTemplate!!.geTypeResize()
            )
            val height = blurredImageView.getH()

            // Copy the original video to local storage, then extract a frame for the background
            AudioUtils.copyToLocalAsync(
                this,
                mTemplate!!.uri_original_upload_video!!,
                mTemplate!!.folder_template!!,
                object : AudioUtils.Callback {
                    override fun onSuccess(localVideoPath: String) {
                        try {
                            mTemplate!!.uri_media_video = localVideoPath
                            val fileVideo = FileUtils.getFileVideo(mTemplate!!.folder_template!!)!!
                            val framePattern = File(fileVideo, "frame_%04d.jpg")
                            val firstFrame = File(fileVideo, "frame_0001.jpg")

                            endFrame = Math.min(Math.round(trackViewEntity.maxTime / 1000.0f), 4)
                            if (endFrame == 0) endFrame = 4

                            // Extract initial frames from video for background
                            id_ffmpeg.add(
                                FFmpegKit.executeWithArgumentsAsync(
                                    arrayOf(
                                        "-i", localVideoPath, "-ss", "0", "-t", "$endFrame",
                                        "-r", "25", "-vf",
                                        "scale=$height:$height:force_original_aspect_ratio=increase",
                                        "-q:v", "0", "-threads", "4", "-an", "-y",
                                        framePattern.absolutePath
                                    )
                                ) { _ ->
                                    try {
                                        mTemplate!!.frame_bg = firstFrame.absolutePath
                                        val bitmap = Glide.with(activity as androidx.fragment.app.FragmentActivity)
                                            .asBitmap()
                                            .load(mTemplate!!.frame_bg)
                                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                                            .skipMemoryCache(true)
                                            .override(height, height)
                                            .submit().get()

                                        blurredImageView.isGlass = mTemplate!!.isGlass
                                        blurredImageView.isVideo = true
                                        blurredImageView.bitmapOriginal = bitmap

                                        val cropTo16x9 = when (mTemplate!!.geTypeResize()) {
                                            ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                                                blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                                            )
                                            ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                                                blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                                            )
                                            else -> BitmapCropper.cropTo16x9(
                                                blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                                            )
                                        }

                                        blurredImageView.updatePosCanvas(cropTo16x9!!)
                                        blurredImageView.updateIpad(cropTo16x9, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())

                                        // Set blurred background based on ipad type
                                        val isLayeredType = mTemplate!!.ipad_type == IpadType.BLACK_LAYER.ordinal ||
                                                mTemplate!!.ipad_type == IpadType.GRADIENT.ordinal ||
                                                mTemplate!!.ipad_type == IpadType.MASK_BRUSH.ordinal ||
                                                mTemplate!!.ipad_type == IpadType.BLUE_TYPE.ordinal ||
                                                mTemplate!!.ipad_type == IpadType.CASSET_IMG.ordinal ||
                                                mTemplate!!.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal

                                        if (isLayeredType) {
                                            if (mTemplate!!.gradient != null) {
                                                blurredImageView.setBitmap(
                                                    UtilsBitmap.blur(activity, cropTo16x9!!, 20, 1),
                                                    null, mTemplate!!.gradient!!,
                                                    mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), null
                                                )
                                            } else {
                                                blurredImageView.setBitmap(
                                                    UtilsBitmap.blur(activity, cropTo16x9!!, 20, 1),
                                                    null, mTemplate!!.color_ipad,
                                                    mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), null
                                                )
                                            }
                                            val width8 = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                                            val height7 = (cropTo16x9!!.height * 0.5355f).toInt()
                                            var round7 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                                            var round8 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                                            var i11 = width8 + round7
                                            if (i11 > blurredImageView.bitmapOriginal!!.width) {
                                                round7 -= i11 - blurredImageView.bitmapOriginal!!.width
                                                i11 = blurredImageView.bitmapOriginal!!.width
                                            }
                                            var i12 = height7 + round8
                                            if (i12 > blurredImageView.bitmapOriginal!!.height) {
                                                round8 -= i12 - blurredImageView.bitmapOriginal!!.height
                                                i12 = blurredImageView.bitmapOriginal!!.height
                                            }
                                            if (round7 < 0) round7 = 0
                                            if (round8 < 0) round8 = 0
                                            val rect3 = android.graphics.Rect(round7, round8, i11, i12)
                                            if (mTemplate!!.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal) {
                                                blurredImageView.bitmapSquare = blurredImageView.bitmapBlured
                                            } else {
                                                blurredImageView.bitmapSquare = cropTo16x9
                                            }
                                            blurredImageView.radius_square = 0
                                            blurredImageView.rectSquare = rect3
                                        } else {
                                            // Standard ipad types — delegate to changeBitmap for the full flow
                                            changeBitmap(firstFrame.absolutePath)
                                        }

                                        val clrTrsl = if (mTemplate!!.ipad_type == IpadType.BLUE_TYPE.ordinal) {
                                            blurredImageView.paintLecture.color
                                        } else {
                                            if (blurredImageView.paintLecture.color == -1) androidx.core.view.InputDeviceCompat.SOURCE_ANY else Constants.COLOR_TRANSLATION
                                        }
                                        blurredImageView.clr_trsl = clrTrsl
                                        blurredImageView.clr_aya = blurredImageView.paintLecture.color
                                        addEntityFromTemplate()

                                        // Start background extraction for remaining frames
                                        id_ffmpeg.add(
                                            FFmpegKit.executeWithArgumentsAsync(
                                                arrayOf(
                                                    "-i", localVideoPath, "-ss", "$endFrame",
                                                    "-r", "25", "-vf",
                                                    "scale=$height:$height:force_original_aspect_ratio=increase",
                                                    "-start_number", "${endFrame * 25}",
                                                    "-q:v", "0", "-threads", "4", "-an", "-y",
                                                    framePattern.absolutePath
                                                )
                                            ) { _ ->
                                            }.sessionId
                                        )

                                        runOnUiThread {
                                            trackViewEntity.invalidate()
                                            updateTime()
                                            if (mTemplate!!.quranEntityList.isEmpty()) {
                                                blurredImageView.invalidate()
                                            }
                                            hideProgressFragment()
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        runOnUiThread { hideProgressFragment() }
                                    }
                                }.sessionId
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread { hideProgressFragment() }
                        }
                    }

                    override fun onError(exception: Exception) {
                        uri_bg = "android.resource://$packageName/drawable/${R.drawable.bg_1}"
                        mTemplate!!.name_drawable = "bg_1"
                        mTemplate!!.color_ipad = -1
                        mTemplate!!.isVideoSquare = false
                        iniTypeImg()
                    }
                })
        } catch (e: Exception) {
            // Fallback: if video init fails, treat as image
            uri_bg = "android.resource://$packageName/drawable/${R.drawable.bg_1}"
            mTemplate!!.name_drawable = "bg_1"
            mTemplate!!.color_ipad = -1
            mTemplate!!.isVideoSquare = false
            iniTypeImg()
            android.util.Log.e("Tag : ", "initTypeVideo fallback: ${e.message}")
        }
    }
}

fun EngineActivity.initResolution() {
    tv_resolution = findViewById(R.id.tv_resolution)
    layout_resolution = findViewById(R.id.layout_resolution)
    val linearLayout = findViewById<android.widget.LinearLayout>(R.id.btn_setup_fps)
    btn_setup_fps = linearLayout
    linearLayout.setOnClickListener {
        if (layout_resolution == null) return@setOnClickListener
        if (layout_resolution?.visibility != View.VISIBLE) {
            layout_resolution?.visibility = View.VISIBLE
        } else {
            layout_resolution?.visibility = View.GONE
        }
    }
    seekBar_fps = findViewById(R.id.seekbar_fps)
    when (mTemplate!!.fps) {
        15 -> seekBar_fps.setProgress(0)
        25 -> seekBar_fps.setProgress(1)
        30 -> seekBar_fps.setProgress(2)
        50 -> seekBar_fps.setProgress(3)
        else -> seekBar_fps.setProgress(4)
    }
    seekBar_fps.setOnProgressChangeListener(object : hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar.OnProgressChangeListener {
        override fun onProgressChanged(customDiscreteSeekBar: hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar, i: Int, str: String, z: Boolean) {}
        override fun onStartTrackingTouch(customDiscreteSeekBar: hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar) {}
        override fun onStopTrackingTouch(customDiscreteSeekBar: hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar) {
            if (mTemplate != null) {
                mTemplate!!.fps = seekBar_fps.getCurrentLabel().toInt()
            }
        }
    })
    tv_resolution.text = mTemplate!!.resolution
    seekBar_res = findViewById(R.id.seekbar_resolution)
    when (mTemplate!!.resolution) {
        "480p" -> seekBar_res.setProgress(0)
        "720p" -> seekBar_res.setProgress(1)
        "1080p" -> seekBar_res.setProgress(2)
        else -> seekBar_res.setProgress(3)
    }
    seekBar_res.setOnProgressChangeListener(object : hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar.OnProgressChangeListener {
        override fun onProgressChanged(customDiscreteSeekBar: hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar, i: Int, str: String, z: Boolean) {}
        override fun onStartTrackingTouch(customDiscreteSeekBar: hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar) {}
        override fun onStopTrackingTouch(customDiscreteSeekBar: hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar) {
            if (mTemplate != null) {
                mTemplate!!.resolution = seekBar_res.getCurrentLabel()
                val size = AspectRatioCalculator.getSize(mTemplate!!.geTypeResize(), mTemplate!!.resolution)
                tv_resolution.text = mTemplate!!.resolution
                mTemplate!!.setWidthAndHeight(size.first, size.second)
            }
        }
    })
}

fun EngineActivity.initViews() {
    initResolution()
    val imageButton = findViewById<android.widget.ImageButton>(R.id.btn_play_pause)
    btnPlayPause = imageButton
    imageButton.setOnClickListener {
        hideLayoutResolution()
        if (mIsPlaying) {
            mIsPlaying = false
            pauseTimelineAnimation()
            trackViewEntity.isPlaying = mIsPlaying
            blurredImageView.isPlaying = mIsPlaying
            trackViewEntity.invalidate()
            for (entityAudio in trackViewEntity.entityListAudio) {
                try {
                    if (entityAudio.visible() && entityAudio.mediaPlayer != null && entityAudio.mediaPlayer!!.isPlaying) {
                        entityAudio.mediaPlayer!!.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            btnPlayPause.setImageResource(R.drawable.play_btn)
            return@setOnClickListener
        }
        if (current_position_time == 0) {
            trackViewEntity.updateCursur(0)
        }
        trackViewEntity.calculMaxTime()
        btnPlayPause.setImageResource(R.drawable.pause_24px)
        updateBtnToEnd(); updateBtnToStart()
        current_position_time = System.currentTimeMillis().toInt()
        mIsPlaying = true
        trackViewEntity.isPlaying = true
        blurredImageView.isPlaying = true
        startTimelineAnimation()
    }
    val imageButton2 = findViewById<android.widget.ImageButton>(R.id.btn_to_end)
    btnToEnd = imageButton2
    imageButton2.setOnClickListener {
        if (trackViewEntity.current_cursur_position == trackViewEntity.maxTime) return@setOnClickListener
        blurredImageView.progress = 1.0f
        stop()
        startCursur = 0
        trackViewEntity.translateToEnd()
        updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
        updateBtnToEnd()
        updateBtnToStart()
    }
    val imageButton3 = findViewById<android.widget.ImageButton>(R.id.btn_to_start)
    btnToStart = imageButton3
    imageButton3.setOnClickListener {
        if (trackViewEntity.current_cursur_position == 0) return@setOnClickListener
        blurredImageView.progress = 0.0f
        stop()
        startCursur = 0
        trackViewEntity.translateToStart()
        updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
        updateBtnToStart()
        updateBtnToEnd()
    }
    updateBtnToStart()
    btnRedo = findViewById(R.id.btn_redo)
    btnUndo = findViewById(R.id.btn_undo)
    disableUndoBtn()
    disableRedoBtn()
    btnRedo.setOnClickListener(object : View.OnClickListener {
        override fun onClick(view: View) {
            stop()
            showProgressSimple()
            Thread(Runnable {
                runOnUiThread {
                    trackViewEntity.redo()
                    hideProgressFragment()
                }
            }).start()
        }
    })
    btnUndo.setOnClickListener(object : View.OnClickListener {
        override fun onClick(view: View) {
            stop()
            showProgressSimple()
            Thread(Runnable {
                runOnUiThread {
                    trackViewEntity.undo()
                    hideProgressFragment()
                }
            }).start()
        }
    })
    trackViewEntity.setRedoUndo(btnRedo, btnUndo)
    val blurredImgView = findViewById<hazem.nurmontage.videoquran.views.BlurredImageView>(R.id.view)
    blurredImageView = blurredImgView
    blurredImgView.setPro(true)
    blurredImageView.setiViewCallback(object : hazem.nurmontage.videoquran.views.BlurredImageView.IViewCallback {
        override fun onDrawFinish() {}
        override fun onSquare() {}
        override fun onEndMove() {
            if (blurredImageView.entity_select != null) {
                blurredImageView.applyAll(blurredImageView.entity_select!!.scaleFactor, blurredImageView.entity_select!!.rect, blurredImageView.entity_select!!.max_w, blurredImageView.entity_select!!.max_h)
            }
        }
        override fun onEndScale() {
            if (blurredImageView.entity_select != null) {
                blurredImageView.applyAll(blurredImageView.entity_select!!.scaleFactor, blurredImageView.entity_select!!.rect, blurredImageView.entity_select!!.max_w, blurredImageView.entity_select!!.max_h)
            }
        }
        override fun onSelect(entityView: hazem.nurmontage.videoquran.model.EntityView) {
            if (entityView is SurahNameEntity) {
                try {
                    if (EditS_NameFragment.instance != null) return
                    stop()
                    selectSurahName()
                    return
                } catch (unused: Exception) {
                    return
                }
            }
            if (entityView is QuranEntity) {
                trackViewEntity.selectEntity(entityView.entityQuran, true)
                iTrimLineCallback!!.onSelectEntity(entityView.entityQuran!!, 0.0f)
            } else if (entityView is BismilahEntity) {
                val bismilahTimeline = (entityView as BismilahEntity).bismilahTimeline
                trackViewEntity.selectEntity(bismilahTimeline, true)
                iTrimLineCallback!!.onSelectEntity(bismilahTimeline!!, 0.0f)
            } else if (entityView is TranslationQuranEntity) {
                trackViewEntity.selectEntity(entityView.entityTrslTimeline, true)
                iTrimLineCallback!!.onSelectEntity(entityView.entityTrslTimeline!!, 0.0f)
            }
        }
        override fun onEmtyClick() {
            iTrimLineCallback!!.onEmptySelect()
        }
        override fun onWattermark() {
            /* Watermark removed — all users are pro */
        }
    })
    // Billing removed — all users are pro; hide the pro button
    findViewById<View>(R.id.to_pro).visibility = View.GONE
    blurredImageView.post {
        if (mTemplate!!.isVideoSquare) {
            initTypeVideo()
        } else {
            iniTypeImg()
        }
    }
    val buttonCustumFont = findViewById<hazem.nurmontage.videoquran.views.ButtonCustumFont>(R.id.btn_export)
    btn_export = buttonCustumFont
    buttonCustumFont.text = mResources!!.getString(R.string.export)
    btn_export.setOnClickListener {
        isSaveTmpTemplate = false
        stop()
        if (Build.VERSION.SDK_INT >= 33) {
            save()
        } else if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0) {
            save()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.WRITE_EXTERNAL_STORAGE"), 1)
        }
    }
    val imageButton4 = findViewById<android.widget.ImageButton>(R.id.btn_cancel)
    btn_cancel = imageButton4
    imageButton4.setOnClickListener { showExitDialog() }
    tv_tittle_fragment = findViewById(R.id.tv_tittle_fragment)
    (findViewById<TextCustumFont>(R.id.tv_quran)).text = mResources!!.getString(R.string.quran)
    (findViewById<TextCustumFont>(R.id.tv_bg)).text = mResources!!.getString(R.string.bg)
    val textCustumFont = findViewById<TextCustumFont>(R.id.tv_ipad)
    textCustumFont.text = mResources!!.getString(R.string.ipad)
    findViewById<View>(R.id.btn_add_quran).setOnClickListener {
        stop()
        try {
            val beginTransaction = supportFragmentManager.beginTransaction()
            mCurrentFragment = AddQuranFragment.getInstance(iAddQuran, mResources)
            beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
            beginTransaction.commit()
            setupShowFragment(mResources!!.getString(R.string.quran))
        } catch (unused: Exception) {
        }
    }
    findViewById<View>(R.id.btn_bg).setOnClickListener {
        stop()
        try {
            val beginTransaction = supportFragmentManager.beginTransaction()
            mCurrentFragment = ChangeBgFragment.getInstance(iChangeBgCallback, mResources, mTemplate!!.name_drawable)
            beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
            beginTransaction.commitNow()
            setupShowFragment(mResources!!.getString(R.string.bg))
        } catch (unused: Exception) {
        }
    }
    btnIpod = findViewById(R.id.btn_ipad)
    textChangeResize = findViewById(R.id.tv_ratio)
    ivResize = findViewById(R.id.iv_ratio)
    ivIpod = findViewById(R.id.iv_ipod)
    btnChangeResize = findViewById(R.id.btn_change_aspect)
    // Billing removed — all features available to everyone
    btnChangeResize?.setOnClickListener {
        stop()
        try {
            val beginTransaction = supportFragmentManager.beginTransaction()
            mCurrentFragment = ResizeFragment.getInstance(iDimensionCallback, mResources, "16")
            beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
            beginTransaction.commit()
            setupShowFragment(null)
        } catch (unused: Exception) {
        }
    }
    btnIpod?.setOnClickListener {
        stop()
        try {
            val beginTransaction = supportFragmentManager.beginTransaction()
            mCurrentFragment = EditIpadFragment.getInstance(mResources, mTemplate!!.ipad_type, iIpadEditCallback, mTemplate!!.index_color, mTemplate!!.gradient != null, mTemplate!!.isGlass)
            beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
            beginTransaction.commit()
            setupShowFragment(mResources!!.getString(R.string.ipad))
        } catch (unused: Exception) {
        }
    }
    updateHitRatio(mTemplate!!.geTypeResize(), mTemplate!!.imgResize)
}

fun EngineActivity.setupShowFragment(str: String?) {
    findViewById<View>(R.id.layout_time).visibility = View.INVISIBLE
    findViewById<View>(R.id.layout_menu).visibility = View.INVISIBLE
    if (str != null) {
        tv_tittle_fragment?.text = str
        tv_tittle_fragment?.visibility = View.VISIBLE
        btnChangeResize?.let { it.visibility = View.INVISIBLE }
    }
    btn_cancel?.visibility = View.INVISIBLE
    btn_export?.visibility = View.INVISIBLE
    btn_setup_fps?.visibility = View.INVISIBLE
}

fun EngineActivity.setupHideFragment() {
    findViewById<View>(R.id.layout_time).visibility = View.VISIBLE
    findViewById<View>(R.id.layout_menu).visibility = View.VISIBLE
    tv_tittle_fragment?.visibility = View.GONE
    btnChangeResize?.let { it.visibility = View.VISIBLE }
    btn_cancel?.visibility = View.VISIBLE
    btn_export?.visibility = View.VISIBLE
    btn_setup_fps?.visibility = View.VISIBLE
}

fun EngineActivity.showEditAudioEntity(entityAudio: EntityAudio) {
    findViewById<View>(R.id.layout_menu).visibility = View.INVISIBLE
    val beginTransaction = supportFragmentManager.beginTransaction()
    mCurrentFragment = EditMediaFragment.getInstance(iEditMediaCallback, mResources, entityAudio, -trackViewEntity.currentPosition)
    beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
    beginTransaction.commit()
}

fun EngineActivity.showEditMultipleEntity(i: Int) {
    if (EditMultipleEntityFragment.instance != null) {
        EditMultipleEntityFragment.instance!!.setCount_select(i)
        return
    }
    findViewById<View>(R.id.layout_menu).visibility = View.INVISIBLE
    val beginTransaction = supportFragmentManager.beginTransaction()
    mCurrentFragment = EditMultipleEntityFragment.getInstance(iEditMultipleCallback, mResources, i)
    beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
    beginTransaction.commit()
}

fun EngineActivity.showEditEntity(entity: Entity) {
    val beginTransaction = supportFragmentManager.beginTransaction()
    mCurrentFragment = EditEntityFragment.getInstance(iEditEntityCallback, mResources, entity, -trackViewEntity.currentPosition)
    beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
    beginTransaction.commit()
}

fun EngineActivity.showEditTrslEntity(entity: Entity) {
    val beginTransaction = supportFragmentManager.beginTransaction()
    mCurrentFragment = EditTrslEntityFragment.getInstance(iEditTrstEntityCallback, mResources, entity, -trackViewEntity.currentPosition)
    beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
    beginTransaction.commit()
}

fun EngineActivity.showEditBismilahEntity(entity: Entity) {
    val beginTransaction = supportFragmentManager.beginTransaction()
    mCurrentFragment = EditBismilahEntityFragment.getInstance(iBismilahEntityCallback, mResources, entity, -trackViewEntity.currentPosition)
    beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
    beginTransaction.commit()
}

fun EngineActivity.hideFragment() {
    try {
        if (!isFinishing && !supportFragmentManager.isDestroyed) {
            val supportFragmentManager = supportFragmentManager
            val beginTransaction = supportFragmentManager.beginTransaction()
            val findFragmentById = supportFragmentManager.findFragmentById(R.id.m_container)
            if (findFragmentById != null) {
                beginTransaction.remove(findFragmentById)
            }
            beginTransaction.commit()
            setupHideFragment()
        }
    } catch (_: Exception) {
    }
    mCurrentFragment = null
}

fun EngineActivity.showProgress() {
    try {
        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)
        findViewById<View>(R.id.container_progress).visibility = 0
        if (isFinishing || supportFragmentManager.isDestroyed) {
            return
        }
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.replace(R.id.container_progress, ProgressViewFragment.getInstance())
        beginTransaction.commit()
    } catch (_: Exception) {
    }
}

fun EngineActivity.showProgressSimple() {
    try {
        findViewById<View>(R.id.container_progress).visibility = 0
        if (isFinishing || supportFragmentManager.isDestroyed) {
            return
        }
        val beginTransaction = supportFragmentManager.beginTransaction()
        beginTransaction.replace(R.id.container_progress, SimpleProgressViewFragment.getInstance())
        beginTransaction.commit()
    } catch (_: Exception) {
    }
}

fun EngineActivity.hideProgressFragment() {
    try {
        setStatusBarColor(-15658735)
        setNavigationBarColor(-14803426)
        if (!isFinishing && !supportFragmentManager.isDestroyed) {
            val supportFragmentManager = supportFragmentManager
            val beginTransaction = supportFragmentManager.beginTransaction()
            val findFragmentById = supportFragmentManager.findFragmentById(R.id.container_progress)
            if (findFragmentById != null) {
                beginTransaction.remove(findFragmentById)
            }
            beginTransaction.commit()
        }
        findViewById<View>(R.id.container_progress).visibility = 8
    } catch (_: Exception) {
    }
}

fun EngineActivity.hideLayoutResolution() {
    val linearLayout = layout_resolution
    if (linearLayout == null || linearLayout.visibility != 0) {
        return
    }
    layout_resolution!!.post {
        layout_resolution!!.visibility = 8
    }
}


fun EngineActivity.dialogCopyRight() {
    try {
        val dialog = Dialog(this)
        this.dialog = dialog
        dialog.setCancelable(false)
        this.dialog!!.requestWindowFeature(1)
        this.dialog!!.window!!.setLayout(-1, -2)
        this.dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
        val inflate = LayoutInflater.from(this).inflate(R.layout.layout_dialog_copyright, null)
        this.dialog!!.setContentView(inflate)
        val textCustumFontBold = inflate.findViewById<hazem.nurmontage.videoquran.views.TextCustumFontBold>(R.id.dialog_title)
        val textCustumFont = inflate.findViewById<TextCustumFont>(R.id.tv_msj)
        inflate.findViewById<View>(R.id.dialog_no).setOnClickListener {
            cancelDialog()
        }
        if (LocaleHelper.getLanguage(this) == "ar") {
            textCustumFontBold.text = "تنبيه حقوق الاستخدام ⚠️"
            textCustumFont.text = "بعض تسجيلات تلاوات القرّاء محمية بحقوق النشر، وهي مخصّصة للاستخدام الشخصي فقط.\n\nقد تسمح بعض المنصات باستخدام هذه الأصوات دون مشاكل، لكن ذلك لا يُعدّ تصريحًا بالنشر أو الاستخدام التجاري.\n\nللنشر الآمن، يُرجى اختيار قارئ مذكور على أنه مسموح بالنشر أو استخدام صوتك الخاص.\n\nالمستخدم مسؤول بالكامل عن الالتزام بسياسات حقوق النشر الخاصة بكل منصة."
        } else {
            textCustumFontBold.text = "⚠️ Copyright Notice"
            textCustumFont.text = "Some reciters' audio recordings are protected by copyright and are intended for personal use only.\n\nCertain platforms may allow these sounds without issues, but this does not constitute permission to publish or use them commercially.\n\nFor safe publishing, please select a reciter marked as allowed for publishing or use your own audio.\n\nThe user is solely responsible for complying with the copyright policies of each platform."
        }
        this.dialog!!.show()
        MyPreferences.putVuCopyRight(this)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}




fun EngineActivity.dialogNoInternet(uri: Uri) {
    try {
        val d = Dialog(this)
        dialogInternet = d
        d.setCancelable(false)
        dialogInternet!!.requestWindowFeature(1)
        dialogInternet!!.window!!.setLayout(-1, -2)
        dialogInternet!!.window!!.setBackgroundDrawable(ColorDrawable(0))
        val inflate = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null as ViewGroup?)
        dialogInternet!!.setContentView(inflate)
        inflate.findViewById<TextCustumFont>(R.id.dialog_title).text = mResources!!.getString(R.string.no_connection)
        inflate.findViewById<TextCustumFont>(R.id.dialog_message).text = mResources!!.getString(R.string.msj_connection_on)
        val btnNo = inflate.findViewById<ButtonCustumFont>(R.id.dialog_no)
        btnNo.text = mResources!!.getString(R.string.ignore)
        btnNo.setOnClickListener {
            cancelDialogInternet()
            hideProgressFragment()
        }
        val btnYes = inflate.findViewById<ButtonCustumFont>(R.id.dialog_yes)
        btnYes.text = mResources!!.getString(R.string.retry)
        btnYes.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                cancelDialogInternet()
                addAudioTemplateHttp(uri, 0, null)
            }
        }
        dialogInternet!!.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.dialogNoInternetList(list: List<String>) {
    try {
        val d = Dialog(this)
        dialogInternet = d
        d.setCancelable(false)
        dialogInternet!!.requestWindowFeature(1)
        dialogInternet!!.window!!.setLayout(-1, -2)
        dialogInternet!!.window!!.setBackgroundDrawable(ColorDrawable(0))
        val inflate = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null as ViewGroup?)
        dialogInternet!!.setContentView(inflate)
        inflate.findViewById<TextCustumFont>(R.id.dialog_title).text = mResources!!.getString(R.string.no_connection)
        inflate.findViewById<TextCustumFont>(R.id.dialog_message).text = mResources!!.getString(R.string.msj_connection_on)
        val btnNo = inflate.findViewById<ButtonCustumFont>(R.id.dialog_no)
        btnNo.text = mResources!!.getString(R.string.ignore)
        btnNo.setOnClickListener {
            runOnUiThread {
                trackViewEntity.invalidate()
                updateTime()
                if (mTemplate!!.quranEntityList.isEmpty()) {
                    blurredImageView.invalidate()
                }
                cancelDialogInternet()
                hideProgressFragment()
            }
        }
        val btnYes = inflate.findViewById<ButtonCustumFont>(R.id.dialog_yes)
        btnYes.text = mResources!!.getString(R.string.retry)
        btnYes.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                cancelDialogInternet()
                addAudioRecitersTemplate(list, 0, "")
            }
        }
        dialogInternet!!.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.dialogDeleteSelected() {
    try {
        val dialog = Dialog(this)
        this.dialog = dialog
        dialog.setCancelable(true)
        this.dialog!!.requestWindowFeature(1)
        this.dialog!!.window!!.setLayout(-1, -2)
        this.dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
        val inflate = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null)
        this.dialog!!.setContentView(inflate)
        inflate.findViewById<View>(R.id.dialog_title).visibility = 8
        (inflate.findViewById<View>(R.id.dialog_message) as TextCustumFont).text =
            mResources!!.getString(R.string.are_you_sure_to_delete_this_work)
        val buttonCustumFont = inflate.findViewById<ButtonCustumFont>(R.id.dialog_no)
        buttonCustumFont.text = mResources!!.getString(R.string.delete)
        buttonCustumFont.setTextColor(-1499549)
        buttonCustumFont.setOnClickListener {
            buttonCustumFont.isClickable = false
            showProgress()
            Thread {
                trackViewEntity.deleteEntityAllSelect()
                runOnUiThread {
                    trackViewEntity.invalidate()
                    updateTime()
                    hideProgressFragment()
                    iTrimLineCallback!!.onEmptySelect()
                }
            }.start()
            if (this.dialog != null) {
                this.dialog!!.dismiss()
            }
        }
        val buttonCustumFont2 = inflate.findViewById<ButtonCustumFont>(R.id.dialog_yes)
        buttonCustumFont2.text = mResources!!.getString(R.string.no)
        buttonCustumFont2.setOnClickListener {
            this.dialog!!.dismiss()
        }
        this.dialog!!.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.showExitDialog() {
    try {
        isSaveTmpTemplate = false
        pausePlayer()
        val d = Dialog(this)
        dialog = d
        d.setCancelable(true)
        dialog!!.requestWindowFeature(1)
        dialog!!.window!!.setLayout(-1, -2)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
        val inflate = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null as ViewGroup?)
        dialog!!.setContentView(inflate)
        inflate.findViewById<TextCustumFont>(R.id.dialog_title).text = mResources!!.getString(R.string.exit)
        inflate.findViewById<TextCustumFont>(R.id.dialog_message).text = mResources!!.getString(R.string.are_you_sure_want_to_leave_this_work)
        val btnNo = inflate.findViewById<ButtonCustumFont>(R.id.dialog_no)
        btnNo.text = mResources!!.getString(R.string.leave)
        btnNo.setOnClickListener {
            LocalPersistence.deleteTemplate(this, Constants.TEMPLATE_TMP)
            cancelDialog()
            startActivity(Intent(this, hazem.nurmontage.videoquran.ui.home.WorkUserActivity::class.java))
            finish()
        }
        val btnYes = inflate.findViewById<ButtonCustumFont>(R.id.dialog_yes)
        btnYes.text = mResources!!.getString(R.string.Continue)
        btnYes.setOnClickListener {
            cancelDialog()
        }
        dialog!!.show()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.cancelDialog() {
    try {
        val d = dialog
        if (d != null && d.isShowing) {
            d.dismiss()
        }
        dialog = null
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.cancelDialogInternet() {
    try {
        val d = dialogInternet
        if (d != null && d.isShowing) {
            d.dismiss()
        }
        dialogInternet = null
    } catch (e: Exception) {
        e.printStackTrace()
    }
}


fun EngineActivity.updateHitRatio(i: Int, str: String) {
    if (i == ResizeType.SOCIAL_STORY.ordinal) {
        textChangeResize!!.text = "9:16"
    } else if (i == ResizeType.SQUARE.ordinal) {
        textChangeResize!!.text = "1:1"
    } else {
        textChangeResize!!.text = "16:9"
    }
    ivResize!!.setImageResource(DrawableHelper.getIdResource(str))
}

fun EngineActivity.toCrop() {
    isSaveTmpTemplate = false
    isToCrop = true
    Common.bitmap = blurredImageView.bitmapOriginal
    Common.rect = blurredImageView.rectSquare!!
    if (blurredImageView.bitmapSquare != null) {
        Common.minSquareW = blurredImageView.bitmapSquare!!.width
        Common.minSquareH = blurredImageView.bitmapSquare!!.height
    }
    Common.radius = blurredImageView.getRadius_square()
    launchCropActivity!!.launch(Intent(this, hazem.nurmontage.videoquran.ui.editor.CropBitmapActivity::class.java))
}


fun EngineActivity.pickVideoFromGallery() {
    if (Build.VERSION.SDK_INT >= 34) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") != 0 &&
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") != 0
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.READ_MEDIA_IMAGES",
                    "android.permission.READ_MEDIA_VIDEO",
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                ),
                11
            )
            return
        }
    } else if (Build.VERSION.SDK_INT == 33) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") != 0 ||
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") != 0
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"),
                11
            )
            return
        }
    } else if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != 0) {
        ActivityCompat.requestPermissions(
            this, arrayOf("android.permission.READ_EXTERNAL_STORAGE"), 11
        )
        return
    }
    videoChooser()
}

fun EngineActivity.pickImageFromGallery() {
    if (Build.VERSION.SDK_INT >= 34) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") != 0 ||
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") != 0
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    "android.permission.READ_MEDIA_IMAGES",
                    "android.permission.READ_MEDIA_VIDEO",
                    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
                ),
                10
            )
            return
        }
    } else if (Build.VERSION.SDK_INT >= 33) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") != 0 ||
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") != 0
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO"),
                10
            )
            return
        }
    } else if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != 0) {
        ActivityCompat.requestPermissions(
            this, arrayOf("android.permission.READ_EXTERNAL_STORAGE"), 10
        )
        return
    }
    imageChooser()
}


fun EngineActivity.handleVideo(uri: Uri) {
    showProgress()
    id_ffmpeg.clear()
    executor.execute(handleVideoRunnable(uri))
}

fun EngineActivity.handleVideoRunnable(uri: Uri): Runnable = Runnable {
    try {
        val copyFromUri = AudioUtils.copyFromUri(this, uri, mTemplate!!.folder_template!!)!!
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(this, uri)
        mediaPlayer.setOnPreparedListener { mp ->
            if (mp == null) {
                return@setOnPreparedListener
            }
            val height = blurredImageView.getH()
            mTemplate!!.isVideoSquare = true
            blurredImageView.isVideo = true
            mTemplate!!.name_drawable = null
            mTemplate!!.uri_original_upload_video = uri.toString()
            mTemplate!!.uri_media_video = copyFromUri
            mTemplate!!.duration_video_media = mp.duration / 1000
            val fileVideo = FileUtils.getFileVideo(mTemplate!!.folder_template!!)!!
            val file = File(fileVideo, "frame_%04d.jpg")
            val file2 = File(fileVideo, "frame_0001.jpg")
            mTemplate!!.frame_bg = file2.absolutePath
            endFrame = Math.min(Math.round(trackViewEntity.maxTime / 1000.0f), 3)
            if (endFrame == 0) {
                endFrame = 3
            }
            val valCopyFromUri = copyFromUri
            id_ffmpeg.add(
                FFmpegKit.executeWithArgumentsAsync(
                    arrayOf(
                        "-i", valCopyFromUri, "-ss", "0", "-t", "$endFrame",
                        "-r", "25", "-vf",
                        "scale=$height:$height:force_original_aspect_ratio=increase",
                        "-q:v", "0", "-threads", "4", "-an", "-y",
                        file.absolutePath
                    )
                ) { _ ->
                    changeBitmap(file2.absolutePath)
                    runOnUiThread {
                        hideProgressFragment()
                    }
                    id_ffmpeg.add(
                        FFmpegKit.executeWithArgumentsAsync(
                            arrayOf(
                                "-i", valCopyFromUri, "-ss", "$endFrame",
                                "-r", "25", "-vf",
                                "scale=$height:$height:force_original_aspect_ratio=increase",
                                "-start_number", "${endFrame * 25}",
                                "-q:v", "0", "-threads", "4", "-an", "-y",
                                file.absolutePath
                            )
                        ) { _ ->
                        }.sessionId
                    )
                }.sessionId
            )
        }
        mediaPlayer.prepare()
    } catch (e: Exception) {
        e.printStackTrace()
        runOnUiThread {
            hideProgressFragment()
        }
    }
}

fun EngineActivity.changeBitmap(str: String) {
    executor.execute(Runnable {
        var cropTo16x9: Bitmap?
        var bitmap: Bitmap? = null
        var rect: Rect? = null
        try {
            val height = blurredImageView.getH()
            val bitmap2 = Glide.with(this as androidx.fragment.app.FragmentActivity)
                .asBitmap()
                .load(str)
                .override(height, height)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .submit().get()
            if (bitmap2 == null) {
                return@Runnable
            }
            blurredImageView.bitmapOriginal = bitmap2
            if (mTemplate!!.ipad_type == IpadType.RECT.ordinal ||
                mTemplate!!.ipad_type == IpadType.ROUND_RECT.ordinal
            ) {
                mTemplate!!.ipad_type = IpadType.IPAD.ordinal
            }
            cropTo16x9 = when (mTemplate!!.geTypeResize()) {
                ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                    blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                )
                ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                    blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                )
                else -> BitmapCropper.cropTo16x9(
                    blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                )
            }
            blurredImageView.updatePosCanvas(cropTo16x9)
            blurredImageView.updateIpad(cropTo16x9!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())
            // Full ipad-type-specific bitmapSquare computation (matches Java ref lines 7423-7543)
            if (mTemplate!!.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                mTemplate!!.ipad_type != IpadType.GRADIENT.ordinal &&
                mTemplate!!.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                mTemplate!!.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                mTemplate!!.ipad_type != IpadType.CASSET_IMG.ordinal
            ) {
                if (mTemplate!!.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal) {
                    blurredImageView.bitmapBlured = UtilsBitmap.blur(this, cropTo16x9!!, 20, 1)
                    blurredImageView.bitmapSquare = blurredImageView.bitmapBlured
                } else {
                    val min = Math.min(blurredImageView.bitmapOriginal!!.width, blurredImageView.bitmapOriginal!!.height)
                    val audioPosition = min.toFloat()
                    if (mTemplate!!.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                        val width = (blurredImageView.ipad_rect!!.width() * 0.6f).toInt()
                        var round = Math.round(mTemplate!!.x_square * audioPosition)
                        var round2 = Math.round(mTemplate!!.y_square * audioPosition)
                        var i6 = width + round
                        if (i6 > blurredImageView.bitmapOriginal!!.width) {
                            round -= i6 - blurredImageView.bitmapOriginal!!.width
                            i6 = blurredImageView.bitmapOriginal!!.width
                        }
                        var i7 = width + round2
                        if (i7 > blurredImageView.bitmapOriginal!!.height) {
                            round2 -= i7 - blurredImageView.bitmapOriginal!!.height
                            i7 = blurredImageView.bitmapOriginal!!.height
                        }
                        if (round < 0) round = 0
                        if (round2 < 0) round2 = 0
                        val rect2 = Rect(round, round2, i6, i7)
                        blurredImageView.setRadius_square(width)
                        val widthSq = (mTemplate!!.width_square * audioPosition).toInt()
                        val heightSq = (audioPosition * mTemplate!!.height_square).toInt()
                        bitmap = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect2, width, widthSq, heightSq)
                        rect2.right = rect2.left + widthSq
                        rect2.bottom = rect2.top + heightSq
                        blurredImageView.rectSquare = rect2
                        rect = rect2
                    } else if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal &&
                               mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                               mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal
                    ) {
                        // BOTTOM_RECT, BORDER, HEART, BATTERY, CASSET types
                        val width2 = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                        val height2 = (cropTo16x9!!.height * 0.5355f).toInt()
                        val floatVal = min.toFloat()
                        var round3 = Math.round(mTemplate!!.x_square * floatVal)
                        var round4 = Math.round(mTemplate!!.y_square * floatVal)
                        var totalDim = width2 + round3
                        if (totalDim > blurredImageView.bitmapOriginal!!.width) {
                            round3 -= totalDim - blurredImageView.bitmapOriginal!!.width
                            totalDim = blurredImageView.bitmapOriginal!!.width
                        }
                        var i5 = height2 + round4
                        if (i5 > blurredImageView.bitmapOriginal!!.height) {
                            round4 -= i5 - blurredImageView.bitmapOriginal!!.height
                            i5 = blurredImageView.bitmapOriginal!!.height
                        }
                        if (round3 < 0) round3 = 0
                        if (round4 < 0) round4 = 0
                        val rect3 = Rect(round3, round4, totalDim, i5)
                        val widthSq2 = (mTemplate!!.width_square * floatVal).toInt()
                        val heightSq2 = (floatVal * mTemplate!!.height_square).toInt()
                        bitmap = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect3, widthSq2, heightSq2)
                        blurredImageView.bitmapSquare = bitmap
                        blurredImageView.setRadius_square(0)
                        rect3.right = rect3.left + widthSq2
                        rect3.bottom = rect3.top + heightSq2
                        blurredImageView.rectSquare = rect3
                        rect = rect3
                    }
                    // IPAD / IPAD_UNBLUR / IPAD_CLASSIC computation
                    val width3 = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                    val i6 = (width3 * 1.13f).toInt()
                    val min2 = Math.min(width3, i6)
                    val floatVal3 = min.toFloat()
                    var round5 = Math.round(mTemplate!!.x_square * floatVal3)
                    var round6 = Math.round(mTemplate!!.y_square * floatVal3)
                    var value7 = width3 + round5
                    if (value7 > blurredImageView.bitmapOriginal!!.width) {
                        round5 -= value7 - blurredImageView.bitmapOriginal!!.width
                        value7 = blurredImageView.bitmapOriginal!!.width
                    }
                    var i8 = i6 + round6
                    if (i8 > blurredImageView.bitmapOriginal!!.height) {
                        round6 -= i8 - blurredImageView.bitmapOriginal!!.height
                        i8 = blurredImageView.bitmapOriginal!!.height
                    }
                    if (round5 < 0) round5 = 0
                    if (round6 < 0) round6 = 0
                    val rect4 = Rect(round5, round6, value7, i8)
                    val widthSq3 = (mTemplate!!.width_square * floatVal3).toInt()
                    val heightSq3 = (floatVal3 * mTemplate!!.height_square).toInt()
                    if (mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                        bitmap = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect4, widthSq3, heightSq3)
                        blurredImageView.bitmapSquare = bitmap
                        blurredImageView.setRadius_square(0)
                        rect4.right = rect4.left + widthSq3
                        rect4.bottom = rect4.top + heightSq3
                        blurredImageView.rectSquare = rect4
                    } else {
                        val i9 = (min2 * 0.10800001f).toInt()
                        blurredImageView.setRadius_square(i9)
                        bitmap = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect4, i9, widthSq3, heightSq3)
                        blurredImageView.bitmapSquare = bitmap
                        rect4.right = rect4.left + widthSq3
                        rect4.bottom = rect4.top + heightSq3
                        blurredImageView.rectSquare = rect4
                    }
                    rect = rect4
                    // Use setBitmap (NOT updateBitmap) to trigger createRect()
                    blurredImageView.setBitmap(
                        UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap, -1,
                        mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect
                    )
                }
                mTemplate!!.color_ipad = blurredImageView.colorIpad()
                runOnUiThread {
                    blurredImageView.invalidate()
                }
            }
            if (mTemplate!!.ipad_type == IpadType.GRADIENT.ordinal) {
                blurredImageView.setColorIpad(ViewCompat.MEASURED_STATE_MASK)
            }
            blurredImageView.bitmapSquare = cropTo16x9
            blurredImageView.bitmapBlured = UtilsBitmap.blur(this, cropTo16x9!!, 20, 1)
            mTemplate!!.color_ipad = blurredImageView.colorIpad()
            runOnUiThread {
                blurredImageView.invalidate()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    })
}

@Throws(IOException::class)
fun EngineActivity.setupOriginalBitmap(uri: Uri): Bitmap {
    val height = blurredImageView.getH()
    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
    val min = height / Math.min(bitmap.width, bitmap.height).toFloat()
    return Bitmap.createScaledBitmap(
        bitmap, Math.round(bitmap.width * min), Math.round(bitmap.height * min), true
    )
}

fun EngineActivity.setupOriginalBitmap(bitmap: Bitmap, i: Int): Bitmap {
    val min = i / Math.min(bitmap.width, bitmap.height).toFloat()
    return Bitmap.createScaledBitmap(
        bitmap, Math.round(bitmap.width * min), Math.round(bitmap.height * min), true
    )
}

fun EngineActivity.handleImg(uri: Uri) {
    showProgress()
    executor.execute {
        var cropTo16x9: Bitmap?
        var bitmap: Bitmap? = null
        var rect: Rect? = null
        try {
            try {
                contentResolver.takePersistableUriPermission(uri, 1)
            } catch (_: Exception) {
            }
            try {
                try {
                    uri_bg = uri.toString()
                    mTemplate!!.name_drawable = null
                    mTemplate!!.uri_bg = uri_bg
                    mTemplate!!.isVideoSquare = false
                    blurredImageView.isVideo = false
                    blurredImageView.bitmapOriginal = setupOriginalBitmap(uri)
                    cropTo16x9 = when (mTemplate!!.geTypeResize()) {
                        ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                        )
                        ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                        )
                        else -> BitmapCropper.cropTo16x9(
                            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
                        )
                    }
                    blurredImageView.updatePosCanvas(cropTo16x9)
                    blurredImageView.updateIpad(cropTo16x9!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())
                    val min = Math.min(
                        blurredImageView.bitmapOriginal!!.width,
                        blurredImageView.bitmapOriginal!!.height
                    )
                    // Full ipad-type-specific bitmapSquare computation (matches Java ref lines 7735-7853)
                    if (mTemplate!!.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                        val width = (blurredImageView.ipad_rect!!.width() * 0.6f).toInt()
                        val audioPosition = min.toFloat()
                        var round = Math.round(mTemplate!!.x_square * audioPosition)
                        var round2 = Math.round(mTemplate!!.y_square * audioPosition)
                        var scaledDimension = width + round
                        if (scaledDimension > blurredImageView.bitmapOriginal!!.width) {
                            round -= scaledDimension - blurredImageView.bitmapOriginal!!.width
                            scaledDimension = blurredImageView.bitmapOriginal!!.width
                        }
                        var heightDimension = width + round2
                        if (heightDimension > blurredImageView.bitmapOriginal!!.height) {
                            round2 -= heightDimension - blurredImageView.bitmapOriginal!!.height
                            heightDimension = blurredImageView.bitmapOriginal!!.height
                        }
                        if (round < 0) round = 0
                        if (round2 < 0) round2 = 0
                        val rect2 = Rect(round, round2, scaledDimension, heightDimension)
                        blurredImageView.setRadius_square(width)
                        val widthSq = (mTemplate!!.width_square * audioPosition).toInt()
                        val heightSq = (audioPosition * mTemplate!!.height_square).toInt()
                        bitmap = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect2, width, widthSq, heightSq)
                        blurredImageView.bitmapSquare = bitmap
                        rect2.right = rect2.left + widthSq
                        rect2.bottom = rect2.top + heightSq
                        blurredImageView.rectSquare = rect2
                        rect = rect2
                    } else {
                        if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal &&
                            mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                            mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal
                        ) {
                            // BOTTOM_RECT type
                            if (mTemplate!!.ipad_type == IpadType.BOTTOM_RECT.ordinal) {
                                val width2 = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                                val height = (cropTo16x9!!.height * 0.5355f).toInt()
                                val floatVal = min.toFloat()
                                var round3 = Math.round(mTemplate!!.x_square * floatVal)
                                var round4 = Math.round(mTemplate!!.y_square * floatVal)
                                var totalDimension = width2 + round3
                                if (totalDimension > blurredImageView.bitmapOriginal!!.width) {
                                    round3 -= totalDimension - blurredImageView.bitmapOriginal!!.width
                                    totalDimension = blurredImageView.bitmapOriginal!!.width
                                }
                                var i5 = height + round4
                                if (i5 > blurredImageView.bitmapOriginal!!.height) {
                                    round4 -= i5 - blurredImageView.bitmapOriginal!!.height
                                    i5 = blurredImageView.bitmapOriginal!!.height
                                }
                                if (round3 < 0) round3 = 0
                                if (round4 < 0) round4 = 0
                                val rect3 = Rect(round3, round4, totalDimension, i5)
                                val widthSq2 = (mTemplate!!.width_square * floatVal).toInt()
                                val heightSq2 = (floatVal * mTemplate!!.height_square).toInt()
                                bitmap = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect3, widthSq2, heightSq2)
                                blurredImageView.bitmapSquare = bitmap
                                blurredImageView.setRadius_square(0)
                                rect3.right = rect3.left + widthSq2
                                rect3.bottom = rect3.top + heightSq2
                                blurredImageView.rectSquare = rect3
                                rect = rect3
                            } else {
                                bitmap = null
                                rect = null
                            }
                        }
                        // IPAD / IPAD_UNBLUR / IPAD_CLASSIC computation (always runs for those types)
                        val width3 = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                        val i6 = (width3 * 1.13f).toInt()
                        val min2 = Math.min(width3, i6)
                        val floatVal3 = min.toFloat()
                        var round5 = Math.round(mTemplate!!.x_square * floatVal3)
                        var round6 = Math.round(mTemplate!!.y_square * floatVal3)
                        var value7 = width3 + round5
                        if (value7 > blurredImageView.bitmapOriginal!!.width) {
                            round5 -= value7 - blurredImageView.bitmapOriginal!!.width
                            value7 = blurredImageView.bitmapOriginal!!.width
                        }
                        var i8 = i6 + round6
                        if (i8 > blurredImageView.bitmapOriginal!!.height) {
                            round6 -= i8 - blurredImageView.bitmapOriginal!!.height
                            i8 = blurredImageView.bitmapOriginal!!.height
                        }
                        if (round5 < 0) round5 = 0
                        if (round6 < 0) round6 = 0
                        val rect4 = Rect(round5, round6, value7, i8)
                        val widthSq3 = (mTemplate!!.width_square * floatVal3).toInt()
                        val heightSq3 = (floatVal3 * mTemplate!!.height_square).toInt()
                        if (mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                            bitmap = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect4, widthSq3, heightSq3)
                            blurredImageView.bitmapSquare = bitmap
                            blurredImageView.setRadius_square(0)
                            rect4.right = rect4.left + widthSq3
                            rect4.bottom = rect4.top + heightSq3
                            blurredImageView.rectSquare = rect4
                        } else {
                            val i9 = (min2 * 0.10800001f).toInt()
                            blurredImageView.setRadius_square(i9)
                            bitmap = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect4, i9, widthSq3, heightSq3)
                            blurredImageView.bitmapSquare = bitmap
                            rect4.right = rect4.left + widthSq3
                            rect4.bottom = rect4.top + heightSq3
                            blurredImageView.rectSquare = rect4
                        }
                        rect = rect4
                    }
                    // Type-specific setBitmap call (matches Java ref lines 7854-7864)
                    if (mTemplate!!.ipad_type == IpadType.GRADIENT.ordinal) {
                        val gradient = blurredImageView.color_gradient
                        if (gradient != null) {
                            blurredImageView.setBitmap(
                                UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap, gradient,
                                mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect
                            )
                        } else {
                            blurredImageView.setBitmap(
                                UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap, ViewCompat.MEASURED_STATE_MASK,
                                mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect
                            )
                        }
                    } else if (mTemplate!!.ipad_type == IpadType.BLUE_TYPE.ordinal) {
                        val gradient = blurredImageView.color_gradient
                        if (gradient != null) {
                            blurredImageView.setBitmap(
                                UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap, gradient,
                                mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect
                            )
                        } else {
                            blurredImageView.setBitmap(
                                UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap, blurredImageView.color_ipad,
                                mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect
                            )
                        }
                    } else {
                        blurredImageView.setBitmap(
                            UtilsBitmap.blur(this, cropTo16x9!!, 20, 1), bitmap, -1,
                            mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect
                        )
                    }
                    blurredImageView.invalidate()
                    runOnUiThread {
                        hideProgressFragment()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        hideProgressFragment()
                    }
                }
            } catch (_: Exception) {
            }
        } finally {
        }
    }
}


// ==========================================================================
// addUriAudioToQuranFragment overloads
// ==========================================================================

fun EngineActivity.addUriAudioToQuranFragment(uri: Uri, textValue: String?) {
    try {
        val beginTransaction = supportFragmentManager.beginTransaction()
        mCurrentFragment = AddQuranFragment.getInstance(iAddQuran, mResources!!, uri, textValue ?: "", "-")
        beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
        beginTransaction.commit()
        runOnUiThread {
            setupShowFragment(mResources!!.getString(R.string.quran))
        }
    } catch (_: Exception) {
    }
}

// 3-param overload kept for compatibility; the main logic is in the 2-param version above
fun EngineActivity.addUriAudioToQuranFragment(uri: Uri, str: String, i: Int) {
    addUriAudioToQuranFragment(uri, str)
}

// ==========================================================================
// Activity Result Handlers
// ==========================================================================

@Suppress("unused")
fun EngineActivity.onCropActivityResult(activityResult: ActivityResult) {
    var cropTo16x9: Bitmap?
    if (activityResult.resultCode != -1 || activityResult.data == null ||
        Common.bitmap == null || Common.bitmap!!.isRecycled
    ) {
        return
    }
    Common.bitmap = Bitmap.createScaledBitmap(
        Common.bitmap!!, blurredImageView.getH(), blurredImageView.getH(), false
    )
    blurredImageView.bitmapOriginal = Common.bitmap
    cropTo16x9 = when (mTemplate!!.geTypeResize()) {
        ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
        )
        ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
        )
        else -> BitmapCropper.cropTo16x9(
            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
        )
    }
    blurredImageView.bitmapBlured = UtilsBitmap.blur(this, cropTo16x9!!, 20, 1)!!
    blurredImageView.invalidate()
}

@Suppress("unused")
fun EngineActivity.onCropDataActivityResult(activityResult: ActivityResult) {
    if (activityResult.resultCode == -1) {
        val data = activityResult.data ?: return
        mTemplate!!.x_square = data.getFloatExtra("x", 0.3f)
        mTemplate!!.y_square = data.getFloatExtra("y", 0.4f)
        mTemplate!!.width_square = data.getFloatExtra("w", 1.0f)
        mTemplate!!.height_square = data.getFloatExtra("h", 0.5f)
        blurredImageView.bitmapSquare = Common.bitmap
        blurredImageView.rectSquare = Common.rect
        blurredImageView.invalidate()
    }
    isToCrop = false
}

@Suppress("unused")
fun EngineActivity.onImgActivityResult(activityResult: ActivityResult) {
    val data: Intent?
    if (activityResult.resultCode != -1) return
    data = activityResult.data ?: return
    if (data.data == null) return
    handleImg(data.data!!)
}

@Suppress("unused")
fun EngineActivity.onVideoActivityResult(activityResult: ActivityResult) {
    val data: Intent?
    if (activityResult.resultCode != -1) return
    data = activityResult.data ?: return
    if (data.data == null) return
    val data2 = data.data!!
    try {
        contentResolver.takePersistableUriPermission(data2, 1)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    handleVideo(data2)
}

@Suppress("unused")
fun EngineActivity.onVideoExtractActivityResult(activityResult: ActivityResult) {
    val data: Intent?
    isToCrop = false
    if (activityResult.resultCode != -1) return
    data = activityResult.data ?: return
    if (data.data == null) return
    try {
        val data2 = data.data!!
        try {
            contentResolver.takePersistableUriPermission(data2, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        runOnUiThread {
            showProgress()
        }
        mTemplate!!.uri_upload_extract_audio_video = data2.toString()
        val copyFromUri = hazem.nurmontage.videoquran.utils.audio.AudioUtils.copyFromUri(this, data2, mTemplate!!.folder_template!!)!!
        start_extenstion = 0
        extractAudioFromVideoRecursive(copyFromUri, 0, false, 0)
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
}

/**
 * Result from ChoiceBgFromVideoActivity — sets the chosen background bitmap.
 * Maps to Java lambda$new$8 (launchChoiceBgActivity handler).
 * Same logic as onCropActivityResult: the bg bitmap is delivered via Common.bitmap.
 */
fun EngineActivity.onChoiceBgResult(activityResult: ActivityResult) {
    var cropTo16x9: Bitmap?
    if (activityResult.resultCode != -1 || activityResult.data == null ||
        Common.bitmap == null || Common.bitmap!!.isRecycled
    ) {
        return
    }
    Common.bitmap = Bitmap.createScaledBitmap(
        Common.bitmap!!, blurredImageView.getH(), blurredImageView.getH(), false
    )
    blurredImageView.bitmapOriginal = Common.bitmap
    cropTo16x9 = when (mTemplate!!.geTypeResize()) {
        ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
        )
        ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
        )
        else -> BitmapCropper.cropTo16x9(
            blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH()
        )
    }
    blurredImageView.bitmapBlured = UtilsBitmap.blur(this, cropTo16x9!!, 20, 1)!!
    blurredImageView.invalidate()
}

/**
 * Result from CropBitmapActivity — receives crop region coordinates.
 * Maps to Java lambda$new$9 (launchCropActivity handler).
 * Same logic as onCropDataActivityResult.
 */
fun EngineActivity.onCropResult(activityResult: ActivityResult) {
    if (activityResult.resultCode == -1) {
        val data = activityResult.data ?: return
        mTemplate!!.x_square = data.getFloatExtra("x", 0.3f)
        mTemplate!!.y_square = data.getFloatExtra("y", 0.4f)
        mTemplate!!.width_square = data.getFloatExtra("w", 1.0f)
        mTemplate!!.height_square = data.getFloatExtra("h", 0.5f)
        blurredImageView.bitmapSquare = Common.bitmap
        blurredImageView.rectSquare = Common.rect
        blurredImageView.invalidate()
    }
    isToCrop = false
}

/**
 * Result from GalleryPickerOneImage — user picked an image.
 * Maps to Java lambda$new$10 (launchImg handler).
 * Same logic as onImgActivityResult.
 */
fun EngineActivity.onImgResult(activityResult: ActivityResult) {
    val data: Intent?
    if (activityResult.resultCode != -1) return
    data = activityResult.data ?: return
    if (data.data == null) return
    handleImg(data.data!!)
}

/**
 * Result from GalleryPickerVideo — user picked a video for background.
 * Maps to Java lambda$new$11 (launchVideo handler).
 * Same logic as onVideoActivityResult.
 */
fun EngineActivity.onVideoResult(activityResult: ActivityResult) {
    val data: Intent?
    if (activityResult.resultCode != -1) return
    data = activityResult.data ?: return
    if (data.data == null) return
    val data2 = data.data!!
    try {
        contentResolver.takePersistableUriPermission(data2, 1)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    handleVideo(data2)
}

/**
 * Result from GalleryPickerVideo — user picked a video for audio extraction.
 * Maps to Java lambda$new$12 (launchVideoExtract handler).
 * Same logic as onVideoExtractActivityResult.
 */
fun EngineActivity.onVideoExtractResult(activityResult: ActivityResult) {
    val data: Intent?
    isToCrop = false
    if (activityResult.resultCode != -1) return
    data = activityResult.data ?: return
    if (data.data == null) return
    try {
        val data2 = data.data!!
        try {
            contentResolver.takePersistableUriPermission(data2, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        runOnUiThread {
            showProgress()
        }
        mTemplate!!.uri_upload_extract_audio_video = data2.toString()
        val copyFromUri = hazem.nurmontage.videoquran.utils.audio.AudioUtils.copyFromUri(this, data2, mTemplate!!.folder_template!!)!!
        start_extenstion = 0
        extractAudioFromVideoRecursive(copyFromUri, 0, false, 0)
    } catch (e2: Exception) {
        e2.printStackTrace()
    }
}

// ==========================================================================
// loadTemplate, initLauncher, checkUriShared — template loading helpers
// ==========================================================================

internal fun EngineActivity.loadTemplate() {
    var template = LocalPersistence.readObjectFromFile(this, Constants.TEMPLATE_TMP) as? Template
    mTemplate = template
    if (template == null && intent != null) {
        val templatePath = intent.getStringExtra(Constants.TEMPLATE)
        if (templatePath != null) {
            val template2 = LocalPersistence.readObjectFromFile(this, templatePath) as? Template
            mTemplate = template2
            if (template2 != null) {
                if (template2.name_drawable != null) {
                    uri_bg = "android.resource://$packageName/drawable/${template2.name_drawable!!}"
                } else {
                    uri_bg = template2.uri_bg
                }
                if (template2.width < 1 || template2.height < 1) {
                    template2.setWidthAndHeight(720, 1280)
                }
            }
        }
    }
    val template3 = mTemplate
    if (template3 == null) {
        mTemplate = Template()
        val imgBg = intent.getStringExtra("img_bg")
        uri_bg = imgBg
        if (imgBg != null) {
            mTemplate!!.uri_bg = imgBg
        } else {
            val randomEntry = DrawableHelper.getRandomDrawableEntry()
            val bgUri = "android.resource://$packageName/drawable/${randomEntry.key}"
            uri_bg = bgUri
            mTemplate!!.uri_bg = bgUri
            mTemplate!!.name_drawable = randomEntry.key
        }
        mTemplate!!.setWidthAndHeight(720, 1280)
    } else {
        if (template3.name_drawable != null) {
            uri_bg = "android.resource://$packageName/drawable/${template3.name_drawable!!}"
        } else {
            uri_bg = template3.uri_bg
        }
        if (template3.width < 1 || template3.height < 1) {
            template3.setWidthAndHeight(720, 1280)
        }
    }
    val file = FileUtils.getFile(applicationContext)
    if (file != null) {
        mTemplate!!.folder_template = file.absolutePath
    }
}

internal fun EngineActivity.initLauncher() {
    activityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: androidx.activity.result.ActivityResult ->
        if (activityResult.resultCode == -1) {
            val data = activityResult.data
            if (data != null && data.data != null) {
                val uri = data.data!!
                try {
                    contentResolver.takePersistableUriPermission(uri, 1)
                } catch (e: Exception) { e.printStackTrace() }
                addUriAudioToQuranFragment(uri, null)
            } else {
                android.widget.Toast.makeText(this, mResources?.getString(R.string.no_audio_select) ?: "No audio selected", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(this, mResources?.getString(R.string.audio_cancel) ?: "Audio cancelled", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun EngineActivity.checkUriShared() {
    val stringExtra = intent.getStringExtra("muri")
    if (stringExtra != null) {
        addUriAudioToQuranFragment(Uri.parse(stringExtra), null)
    }
}

// ==========================================================================
// Player helpers
// ==========================================================================

internal fun EngineActivity.pausePlayer() {
    try {
        hideLayoutResolution()
        if (mIsPlaying) {
            mIsPlaying = false
            pauseTimelineAnimation()
            trackViewEntity.isPlaying = mIsPlaying
            blurredImageView.isPlaying = mIsPlaying
            trackViewEntity.invalidate()
            for (entityAudio in trackViewEntity.entityListAudio) {
                try {
                    if (entityAudio.mediaPlayer != null && entityAudio.mediaPlayer!!.isPlaying) {
                        entityAudio.mediaPlayer!!.pause()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            try {
                btnPlayPause.setImageResource(R.drawable.play_btn)
            } catch (_: UninitializedPropertyAccessException) {}
            stop()
        }
        trackViewEntity.pauseScroll()
    } catch (_: Exception) {}
}

internal fun EngineActivity.releaseWakeLock() {
    try { window.clearFlags(0x00000400) } catch (_: Exception) {}
}

internal fun EngineActivity.clearFFmpeg() {
    for (id in id_ffmpeg) {
        FFmpegKit.cancel(id)
    }
}
