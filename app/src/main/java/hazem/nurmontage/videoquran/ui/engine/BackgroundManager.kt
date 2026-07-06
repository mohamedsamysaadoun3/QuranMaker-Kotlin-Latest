package hazem.nurmontage.videoquran.ui.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.core.view.ViewCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.FileUtils
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.blurred.updateIpad
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * BackgroundManager
 *
 * Encapsulates all background (image / video) handling for the engine screen.
 * Moved from EngineActivity.kt and EngineUIHelper.kt — this class is
 * self-contained with no direct Activity reference.
 *
 * Original methods:
 *   - iniTypeImg()           — init image bg from template
 *   - initTypeVideo()        — init video bg from template
 *   - handleVideo(uri)       — user picks video from gallery
 *   - handleVideoRunnable(uri) — bg thread for video extraction
 *   - changeBitmap(str)      — swap bg bitmap from frame path
 *   - updateSquareBitmap(str) — update square/ipad bitmap during video playback
 *   - setupOriginalBitmap(uri)  — scale bitmap from Uri
 *   - setupOriginalBitmap(bitmap, i) — scale bitmap to size
 *   - handleImg(uri)         — user picks image from gallery
 *   - onCropActivityResult / onCropDataActivityResult / onImgActivityResult
 *   - onVideoActivityResult / onChoiceBgResult / onCropResult / onImgResult / onVideoResult
 */
class BackgroundManager(
    private val context: Context,
    private val onProgressShow: () -> Unit,
    private val onProgressHide: () -> Unit,
    private val onInvalidate: () -> Unit,
    private val onBackgroundChanged: () -> Unit,
    private val onAddEntityFromTemplate: () -> Unit,
    private val onError: (String) -> Unit
) {
    // ──────────────────────────────────────────────
    //  External references — set by the host
    // ──────────────────────────────────────────────

    /** Shared template — must be set before calling any init/handle method. */
    var template: Template? = null

    /** Preview surface — must be set before calling any init/handle method. */
    var blurredImageView: BlurredImageView? = null

    // ──────────────────────────────────────────────
    //  Playback state — set externally by Activity/Controller
    // ──────────────────────────────────────────────

    /** Whether the timeline is currently being scrolled. */
    var isOnScroll: Boolean = false

    /** Whether video playback is active. */
    var isVideoPlaying: Boolean = false

    /** Maximum timeline time in ms (from TrackViewEntity). */
    var maxTimeMs: Int = 0

    // ──────────────────────────────────────────────
    //  Internal state
    // ──────────────────────────────────────────────

    /** FFmpeg session IDs for cancellation. */
    private val ffmpegSessionIds = mutableListOf<Long>()

    /** Background executor for off-main-thread work. */
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Background URI string. */
    var uriBg: String? = null

    /** End-frame counter for video extraction. */
    var endFrame: Int = 0

    // ═══════════════════════════════════════════════════════════════════
    //  Image background
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Initialize image background from the current template.
     *
     * Moved from EngineUIHelper.iniTypeImg() / EngineActivity.iniTypeImg().
     *
     * Loads the background bitmap via Glide, crops it to the correct aspect
     * ratio, sets up the iPad frame, and notifies [onBackgroundChanged].
     */
    fun iniTypeImg() {
        executor.execute {
            var bitmap: Bitmap
            var cropTo16x9: Bitmap
            var cropToSquareWithRoundCorners: Bitmap? = null
            var bitmap2: Bitmap
            var rect: Rect
            var clrTrsl: Int
            try {
                val tmpl = template ?: return@execute
                val biv = blurredImageView ?: return@execute
                biv.initCanvasDimension(biv.getW(), biv.getH(), tmpl.geTypeResize())
                val height = biv.getH()
                try {
                    bitmap = Glide.with(context).asBitmap()
                        .load(tmpl.uri_bg)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .override(height, height)
                        .submit().get()
                } catch (_: Exception) {
                    tmpl.color_ipad = -1
                    bitmap = Glide.with(context).asBitmap()
                        .load(R.drawable.bg_19)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .override(height, height)
                        .submit().get()
                }
                biv.bitmapOriginal = setupOriginalBitmap(bitmap, height)
                cropTo16x9 = when (tmpl.geTypeResize()) {
                    ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(biv.bitmapOriginal!!, biv.getW(), biv.getH())!!
                    ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(biv.bitmapOriginal!!, biv.getW(), biv.getH())!!
                    else -> BitmapCropper.cropTo16x9(biv.bitmapOriginal!!, biv.getW(), biv.getH())!!
                }
                biv.isGlass = tmpl.isGlass
                biv.isVideo = false
                biv.updatePosCanvas(cropTo16x9)
                biv.bitmapBlured = cropTo16x9
                biv.updateIpad(cropTo16x9, tmpl.ipad_type, tmpl.geTypeResize())

                if (tmpl.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                    val width = (biv.ipad_rect!!.width() * 0.6f).toInt()
                    var round = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                    var round2 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                    var i3 = width + round
                    if (i3 > biv.bitmapOriginal!!.width) {
                        round -= i3 - biv.bitmapOriginal!!.width
                        i3 = biv.bitmapOriginal!!.width
                    }
                    var i4 = width + round2
                    if (i4 > biv.bitmapOriginal!!.height) {
                        round2 -= i4 - biv.bitmapOriginal!!.height
                        i4 = biv.bitmapOriginal!!.height
                    }
                    if (round < 0) round = 0
                    val i2 = if (round2 >= 0) round2 else 0
                    val rect2 = Rect(round, i2, i3, i4)
                    biv.radius_square = width
                    val width2 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                    val height2 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                    val cropToSquareWithRoundCorners2 = UtilsBitmap.cropToSquareWithRoundCorners(
                        biv.bitmapOriginal!!, rect2, width, width2, height2
                    )
                    rect2.right = rect2.left + width2
                    rect2.bottom = rect2.top + height2
                    biv.rectSquare = rect2
                    bitmap2 = cropToSquareWithRoundCorners2
                    rect = rect2
                } else {
                    if (tmpl.ipad_type != IpadType.IPAD.ordinal &&
                        tmpl.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                        tmpl.ipad_type != IpadType.IPAD_CLASSIC.ordinal
                    ) {
                        val width3 = (biv.ipad_rect!!.width() * 1.0f).toInt()
                        val height3 = (cropTo16x9.height * 0.5355f).toInt()
                        var round3 = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                        var round4 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                        var i5 = width3 + round3
                        if (i5 > biv.bitmapOriginal!!.width) {
                            round3 -= i5 - biv.bitmapOriginal!!.width
                            i5 = biv.bitmapOriginal!!.width
                        }
                        var i6 = height3 + round4
                        if (i6 > biv.bitmapOriginal!!.height) {
                            round4 -= i6 - biv.bitmapOriginal!!.height
                            i6 = biv.bitmapOriginal!!.height
                        }
                        if (round3 < 0) round3 = 0
                        if (round4 < 0) round4 = 0
                        val rect3 = Rect(round3, round4, i5, i6)
                        val width4 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                        val height4 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                        val cropToSquare = UtilsBitmap.cropToSquare(
                            biv.bitmapOriginal!!, rect3, width4, height4
                        )
                        biv.bitmapSquare = cropToSquare
                        biv.radius_square = 0
                        rect3.right = rect3.left + width4
                        rect3.bottom = rect3.top + height4
                        biv.rectSquare = rect3
                        bitmap2 = cropToSquare
                        rect = rect3
                    } else {
                        val width5 = (biv.ipad_rect!!.width() * 0.87530595f).toInt()
                        val i7 = (width5 * 1.13f).toInt()
                        val min = Math.min(width5, i7)
                        var round5 = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                        var round6 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                        var i8 = width5 + round5
                        if (i8 > biv.bitmapOriginal!!.width) {
                            round5 -= i8 - biv.bitmapOriginal!!.width
                            i8 = biv.bitmapOriginal!!.width
                        }
                        var i9 = i7 + round6
                        if (i9 > biv.bitmapOriginal!!.height) {
                            round6 -= i9 - biv.bitmapOriginal!!.height
                            i9 = biv.bitmapOriginal!!.height
                        }
                        if (round5 < 0) round5 = 0
                        if (round6 < 0) round6 = 0
                        val rect4 = Rect(round5, round6, i8, i9)
                        if (tmpl.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                            val width6 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                            val height5 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                            val cropToSquare2 = UtilsBitmap.cropToSquare(
                                biv.bitmapOriginal!!, rect4, width6, height5
                            )
                            biv.bitmapSquare = cropToSquare2
                            biv.radius_square = 0
                            rect4.right = rect4.left + width6
                            rect4.bottom = rect4.top + height5
                            biv.rectSquare = rect4
                            cropToSquareWithRoundCorners = cropToSquare2
                        } else {
                            val i10 = (min * 0.10800001f).toInt()
                            biv.radius_square = i10
                            val width7 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                            val height6 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                            cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(
                                biv.bitmapOriginal!!, rect4, i10, width7, height6
                            )
                            rect4.right = rect4.left + width7
                            rect4.bottom = rect4.top + height6
                            biv.rectSquare = rect4
                        }
                        bitmap2 = cropToSquareWithRoundCorners!!
                        rect = rect4
                    }
                }
                if (tmpl.gradient != null) {
                    biv.setBitmap(
                        UtilsBitmap.blur(context, cropTo16x9, 20, 1),
                        bitmap2, tmpl.gradient!!, tmpl.ipad_type, tmpl.geTypeResize(), rect
                    )
                } else {
                    biv.setBitmap(
                        UtilsBitmap.blur(context, cropTo16x9, 20, 1),
                        bitmap2, tmpl.color_ipad, tmpl.ipad_type, tmpl.geTypeResize(), rect
                    )
                }
                clrTrsl = if (tmpl.ipad_type == IpadType.BLUE_TYPE.ordinal) {
                    biv.paintLecture.color
                } else {
                    if (biv.paintLecture.color == -1) ViewCompat.MEASURED_STATE_MASK else Constants.COLOR_TRANSLATION
                }
                biv.clr_trsl = clrTrsl
                biv.clr_aya = biv.paintLecture.color
                onAddEntityFromTemplate()
            } catch (e: Exception) {
                Log.e(TAG, "iniTypeImg failed: ${e.message}")
                onProgressHide()
            }
        }
    }

    /**
     * Handle a user-selected image URI as the new background.
     *
     * Moved from EngineUIHelper.handleImg() / EngineActivity.handleImg().
     * Full image background setup: loads, crops, blurs, updates ipad.
     */
    fun handleImg(uri: Uri) {
        onProgressShow()
        executor.execute {
            var cropTo16x9: Bitmap?
            var cropToSquareWithRoundCorners: Bitmap? = null
            var bitmap: Bitmap?
            var rect: Rect?
            try {
                val tmpl = template ?: return@execute
                val biv = blurredImageView ?: return@execute
                try {
                    try {
                        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {
                    }
                    try {
                        uriBg = uri.toString()
                        tmpl.name_drawable = null
                        tmpl.uri_bg = uriBg
                        var i = 0
                        tmpl.isVideoSquare = false
                        biv.isVideo = false
                        biv.bitmapOriginal = setupOriginalBitmap(uri)
                        cropTo16x9 = when (tmpl.geTypeResize()) {
                            ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                                biv.bitmapOriginal, biv.getW(), biv.getH()
                            )
                            ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                                biv.bitmapOriginal, biv.getW(), biv.getH()
                            )
                            else -> BitmapCropper.cropTo16x9(
                                biv.bitmapOriginal, biv.getW(), biv.getH()
                            )
                        }
                        biv.updatePosCanvas(cropTo16x9)
                        biv.bitmapBlured = cropTo16x9
                        biv.updateIpad(cropTo16x9!!, tmpl.ipad_type, tmpl.geTypeResize())
                        val min = Math.min(
                            biv.bitmapOriginal!!.width,
                            biv.bitmapOriginal!!.height
                        )
                        if (tmpl.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                            val width = (biv.ipad_rect!!.width() * 0.6f).toInt()
                            val f = min.toFloat()
                            var round = Math.round(tmpl.x_square * f)
                            var round2 = Math.round(tmpl.y_square * f)
                            var i2 = width + round
                            if (i2 > biv.bitmapOriginal!!.width) {
                                round -= i2 - biv.bitmapOriginal!!.width
                                i2 = biv.bitmapOriginal!!.width
                            }
                            var i3 = width + round2
                            if (i3 > biv.bitmapOriginal!!.height) {
                                round2 -= i3 - biv.bitmapOriginal!!.height
                                i3 = biv.bitmapOriginal!!.height
                            }
                            if (round < 0) {
                                round = 0
                            }
                            if (round2 >= 0) {
                                i = round2
                            }
                            val rect2 = Rect(round, i, i2, i3)
                            biv.radius_square = width
                            val widthSquare = (tmpl.width_square * f).toInt()
                            val heightSquare = (f * tmpl.height_square).toInt()
                            val cropToSquareWithRoundCorners2 = UtilsBitmap.cropToSquareWithRoundCorners(
                                biv.bitmapOriginal!!, rect2, width, widthSquare, heightSquare
                            )
                            biv.bitmapSquare = cropToSquareWithRoundCorners2
                            rect2.right = rect2.left + widthSquare
                            rect2.bottom = rect2.top + heightSquare
                            biv.rectSquare = rect2
                            bitmap = cropToSquareWithRoundCorners2
                            rect = rect2
                        } else {
                            if (tmpl.ipad_type != IpadType.IPAD.ordinal &&
                                tmpl.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                                tmpl.ipad_type != IpadType.IPAD_CLASSIC.ordinal
                            ) {
                                if (tmpl.ipad_type == IpadType.BOTTOM_RECT.ordinal) {
                                    val width2 = (biv.ipad_rect!!.width() * 1.0f).toInt()
                                    val height = (cropTo16x9!!.height * 0.5355f).toInt()
                                    val f2 = min.toFloat()
                                    var round3 = Math.round(tmpl.x_square * f2)
                                    var round4 = Math.round(tmpl.y_square * f2)
                                    var i4 = width2 + round3
                                    if (i4 > biv.bitmapOriginal!!.width) {
                                        round3 -= i4 - biv.bitmapOriginal!!.width
                                        i4 = biv.bitmapOriginal!!.width
                                    }
                                    var i5 = height + round4
                                    if (i5 > biv.bitmapOriginal!!.height) {
                                        round4 -= i5 - biv.bitmapOriginal!!.height
                                        i5 = biv.bitmapOriginal!!.height
                                    }
                                    if (round3 < 0) {
                                        round3 = 0
                                    }
                                    if (round4 < 0) {
                                        round4 = 0
                                    }
                                    val rect3 = Rect(round3, round4, i4, i5)
                                    val widthSquare2 = (tmpl.width_square * f2).toInt()
                                    val heightSquare2 = (f2 * tmpl.height_square).toInt()
                                    val cropToSquare = UtilsBitmap.cropToSquare(
                                        biv.bitmapOriginal!!, rect3, widthSquare2, heightSquare2
                                    )
                                    biv.bitmapSquare = cropToSquare
                                    biv.radius_square = 0
                                    rect3.right = rect3.left + widthSquare2
                                    rect3.bottom = rect3.top + heightSquare2
                                    biv.rectSquare = rect3
                                    bitmap = cropToSquare
                                    rect = rect3
                                } else {
                                    bitmap = null
                                    rect = null
                                }
                            } else {
                                val width3 = (biv.ipad_rect!!.width() * 0.87530595f).toInt()
                                val i6 = (width3 * 1.13f).toInt()
                                val min2 = Math.min(width3, i6)
                                val f3 = min.toFloat()
                                var round5 = Math.round(tmpl.x_square * f3)
                                var round6 = Math.round(tmpl.y_square * f3)
                                var i7 = width3 + round5
                                if (i7 > biv.bitmapOriginal!!.width) {
                                    round5 -= i7 - biv.bitmapOriginal!!.width
                                    i7 = biv.bitmapOriginal!!.width
                                }
                                var i8 = i6 + round6
                                if (i8 > biv.bitmapOriginal!!.height) {
                                    round6 -= i8 - biv.bitmapOriginal!!.height
                                    i8 = biv.bitmapOriginal!!.height
                                }
                                if (round5 < 0) {
                                    round5 = 0
                                }
                                if (round6 < 0) {
                                    round6 = 0
                                }
                                val rect4 = Rect(round5, round6, i7, i8)
                                cropToSquareWithRoundCorners = if (tmpl.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                                    val widthSquare3 = (tmpl.width_square * f3).toInt()
                                    val heightSquare3 = (f3 * tmpl.height_square).toInt()
                                    val cropToSquare2 = UtilsBitmap.cropToSquare(
                                        biv.bitmapOriginal!!, rect4, widthSquare3, heightSquare3
                                    )
                                    biv.bitmapSquare = cropToSquare2
                                    biv.radius_square = 0
                                    rect4.right = rect4.left + widthSquare3
                                    rect4.bottom = rect4.top + heightSquare3
                                    biv.rectSquare = rect4
                                    cropToSquare2
                                } else {
                                    val i9 = (min2 * 0.10800001f).toInt()
                                    biv.radius_square = i9
                                    val widthSquare4 = (tmpl.width_square * f3).toInt()
                                    val heightSquare4 = (f3 * tmpl.height_square).toInt()
                                    val result = UtilsBitmap.cropToSquareWithRoundCorners(
                                        biv.bitmapOriginal!!, rect4, i9, widthSquare4, heightSquare4
                                    )
                                    biv.bitmapSquare = result
                                    rect4.right = rect4.left + widthSquare4
                                    rect4.bottom = rect4.top + heightSquare4
                                    biv.rectSquare = rect4
                                    result
                                }
                                bitmap = cropToSquareWithRoundCorners
                                rect = rect4
                            }
                            when (tmpl.ipad_type) {
                                IpadType.GRADIENT.ordinal -> biv.setBitmap(
                                    UtilsBitmap.blur(context, cropTo16x9!!, 20, 1),
                                    bitmap, ViewCompat.MEASURED_STATE_MASK,
                                    tmpl.ipad_type, tmpl.geTypeResize(), rect
                                )
                                IpadType.BLUE_TYPE.ordinal -> {
                                    if (biv.color_gradient != null) {
                                        biv.setBitmap(
                                            UtilsBitmap.blur(context, cropTo16x9!!, 20, 1),
                                            bitmap, biv.color_gradient!!,
                                            tmpl.ipad_type, tmpl.geTypeResize(), rect
                                        )
                                    } else {
                                        biv.setBitmap(
                                            UtilsBitmap.blur(context, cropTo16x9!!, 20, 1),
                                            bitmap, biv.color_ipad,
                                            tmpl.ipad_type, tmpl.geTypeResize(), rect
                                        )
                                    }
                                }
                                else -> biv.setBitmap(
                                    UtilsBitmap.blur(context, cropTo16x9!!, 20, 1),
                                    bitmap, -1, tmpl.ipad_type, tmpl.geTypeResize(), rect
                                )
                            }
                            biv.invalidate()
                            onProgressHide()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onProgressHide()
                    }
                } catch (_: Exception) {
                }
            } finally {
                // no-op
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Video background
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Initialize video background from the current template.
     *
     * Moved from EngineActivity.initTypeVideo().
     *
     * Copies the video to local storage, extracts frames via FFmpeg, and sets
     * the first frame as the background bitmap.
     */
    fun initTypeVideo() {
        executor.execute {
            try {
                val tmpl = template ?: return@execute
                val biv = blurredImageView ?: return@execute
                biv.initCanvasDimension(biv.getW(), biv.getH(), tmpl.geTypeResize())
                val height = biv.getH()

                // Copy the original video to local storage, then extract a frame for the background
                AudioUtils.copyToLocalAsync(
                    context,
                    tmpl.uri_original_upload_video!!,
                    tmpl.folder_template!!,
                    object : AudioUtils.Callback {
                        override fun onSuccess(localVideoPath: String) {
                            try {
                                tmpl.uri_media_video = localVideoPath
                                val fileVideo = FileUtils.getFileVideo(tmpl.folder_template!!)!!
                                val framePattern = File(fileVideo, "frame_%04d.jpg")
                                val firstFrame = File(fileVideo, "frame_0001.jpg")

                                endFrame = Math.min(Math.round(maxTimeMs / 1000.0f), 4)
                                if (endFrame == 0) endFrame = 4

                                // Extract initial frames from video for background
                                ffmpegSessionIds.add(
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
                                            tmpl.frame_bg = firstFrame.absolutePath
                                            val bitmap = Glide.with(context)
                                                .asBitmap()
                                                .load(tmpl.frame_bg)
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .skipMemoryCache(true)
                                                .override(height, height)
                                                .submit().get()

                                            biv.isGlass = tmpl.isGlass
                                            biv.isVideo = true
                                            biv.bitmapOriginal = bitmap

                                            val cropTo16x9 = when (tmpl.geTypeResize()) {
                                                ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                                                    biv.bitmapOriginal, biv.getW(), biv.getH()
                                                )
                                                ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                                                    biv.bitmapOriginal, biv.getW(), biv.getH()
                                                )
                                                else -> BitmapCropper.cropTo16x9(
                                                    biv.bitmapOriginal, biv.getW(), biv.getH()
                                                )
                                            }

                                            biv.updatePosCanvas(cropTo16x9!!)
                                            biv.updateIpad(cropTo16x9, tmpl.ipad_type, tmpl.geTypeResize())

                                            // Set blurred background based on ipad type
                                            val isLayeredType = tmpl.ipad_type == IpadType.BLACK_LAYER.ordinal ||
                                                    tmpl.ipad_type == IpadType.GRADIENT.ordinal ||
                                                    tmpl.ipad_type == IpadType.MASK_BRUSH.ordinal ||
                                                    tmpl.ipad_type == IpadType.BLUE_TYPE.ordinal ||
                                                    tmpl.ipad_type == IpadType.CASSET_IMG.ordinal ||
                                                    tmpl.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal

                                            if (isLayeredType) {
                                                // These types don't need a square crop — set bitmap directly
                                                if (tmpl.gradient != null) {
                                                    biv.setBitmap(
                                                        UtilsBitmap.blur(context, cropTo16x9, 20, 1),
                                                        null, tmpl.gradient!!,
                                                        tmpl.ipad_type, tmpl.geTypeResize(), null
                                                    )
                                                } else {
                                                    biv.setBitmap(
                                                        UtilsBitmap.blur(context, cropTo16x9, 20, 1),
                                                        null, tmpl.color_ipad,
                                                        tmpl.ipad_type, tmpl.geTypeResize(), null
                                                    )
                                                }
                                                val width8 = (biv.ipad_rect!!.width() * 1.0f).toInt()
                                                val height7 = (cropTo16x9.height * 0.5355f).toInt()
                                                var round7 = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                                                var round8 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                                                var i11 = width8 + round7
                                                if (i11 > biv.bitmapOriginal!!.width) {
                                                    round7 -= i11 - biv.bitmapOriginal!!.width
                                                    i11 = biv.bitmapOriginal!!.width
                                                }
                                                var i12 = height7 + round8
                                                if (i12 > biv.bitmapOriginal!!.height) {
                                                    round8 -= i12 - biv.bitmapOriginal!!.height
                                                    i12 = biv.bitmapOriginal!!.height
                                                }
                                                if (round7 < 0) round7 = 0
                                                if (round8 < 0) round8 = 0
                                                val rect3 = Rect(round7, round8, i11, i12)
                                                if (tmpl.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal) {
                                                    biv.bitmapSquare = biv.bitmapBlured
                                                } else {
                                                    biv.bitmapSquare = cropTo16x9
                                                }
                                                biv.radius_square = 0
                                                biv.rectSquare = rect3
                                            } else {
                                                // Standard ipad types — delegate to changeBitmap for the full flow
                                                changeBitmap(firstFrame.absolutePath)
                                            }

                                            val clrTrsl = if (tmpl.ipad_type == IpadType.BLUE_TYPE.ordinal) {
                                                biv.paintLecture.color
                                            } else {
                                                if (biv.paintLecture.color == -1) ViewCompat.MEASURED_STATE_MASK else Constants.COLOR_TRANSLATION
                                            }
                                            biv.clr_trsl = clrTrsl
                                            biv.clr_aya = biv.paintLecture.color
                                            onAddEntityFromTemplate()

                                            // Start background extraction for remaining frames
                                            ffmpegSessionIds.add(
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

                                            onBackgroundChanged()
                                            onProgressHide()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            onProgressHide()
                                        }
                                    }.sessionId
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                onProgressHide()
                            }
                        }

                        override fun onError(exception: Exception) {
                            // Fallback on copy error — treat as image
                            uriBg = "android.resource://${context.packageName}/${R.drawable.bg_1}"
                            tmpl.name_drawable = "bg_1"
                            tmpl.color_ipad = -1
                            tmpl.isVideoSquare = false
                            iniTypeImg()
                        }
                    })
            } catch (e: Exception) {
                // Fallback: if video init fails, treat as image
                val tmpl = template ?: return@execute
                uriBg = "android.resource://${context.packageName}/${R.drawable.bg_1}"
                tmpl.name_drawable = "bg_1"
                tmpl.color_ipad = -1
                tmpl.isVideoSquare = false
                iniTypeImg()
                Log.e(TAG, "initTypeVideo fallback: ${e.message}")
            }
        }
    }

    /**
     * Handle a user-selected video URI as the new background.
     *
     * Moved from EngineUIHelper.handleVideo() / EngineActivity.handleVideo().
     */
    fun handleVideo(uri: Uri) {
        onProgressShow()
        ffmpegSessionIds.clear()
        executor.execute(handleVideoRunnable(uri))
    }

    /**
     * Build the Runnable that extracts frames from a video URI.
     *
     * Moved from EngineUIHelper.handleVideoRunnable() / EngineActivity.handleVideoRunnable().
     * Copies video, extracts frames via FFmpeg.
     */
    fun handleVideoRunnable(uri: Uri): Runnable = Runnable {
        try {
            val tmpl = template ?: return@Runnable
            val biv = blurredImageView ?: return@Runnable
            val copyFromUri = AudioUtils.copyFromUri(context, uri, tmpl.folder_template!!) ?: return@Runnable
            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.setOnPreparedListener { mp ->
                if (mp == null) return@setOnPreparedListener
                val height = biv.getH()

                tmpl.isVideoSquare = true
                biv.isVideo = true
                tmpl.name_drawable = null
                tmpl.uri_original_upload_video = uri.toString()
                tmpl.uri_media_video = copyFromUri
                tmpl.duration_video_media = mp.duration / 1000

                val fileVideo = FileUtils.getFileVideo(tmpl.folder_template!!)!!
                val file = File(fileVideo, "frame_%04d.jpg")
                val file2 = File(fileVideo, "frame_0001.jpg")
                tmpl.frame_bg = file2.absolutePath

                endFrame = Math.min(Math.round(maxTimeMs / 1000.0f), 3)
                if (endFrame == 0) endFrame = 3

                val valCopyFromUri = copyFromUri
                ffmpegSessionIds.add(
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
                        onProgressHide()
                        ffmpegSessionIds.add(
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
            onProgressHide()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Bitmap manipulation
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Swap the background bitmap from a frame file path.
     *
     * Moved from EngineUIHelper.changeBitmap() / EngineActivity.changeBitmap().
     * Loads frame, crops, blurs, updates ipad bitmap.
     */
    fun changeBitmap(framePath: String) {
        executor.execute {
            var cropTo16x9: Bitmap?
            var cropToSquareWithRoundCorners: Bitmap? = null
            var bitmap: Bitmap?
            var rect: Rect?
            try {
                val tmpl = template ?: return@execute
                val biv = blurredImageView ?: return@execute
                val height = biv.getH()
                val bitmap2 = Glide.with(context)
                    .asBitmap()
                    .load(framePath)
                    .override(height, height)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .submit().get()
                if (bitmap2 == null) {
                    return@execute
                }
                biv.bitmapOriginal = bitmap2
                if (tmpl.ipad_type == IpadType.RECT.ordinal ||
                    tmpl.ipad_type == IpadType.ROUND_RECT.ordinal
                ) {
                    tmpl.ipad_type = IpadType.IPAD.ordinal
                }
                cropTo16x9 = when (tmpl.geTypeResize()) {
                    ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                        biv.bitmapOriginal, biv.getW(), biv.getH()
                    )
                    ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                        biv.bitmapOriginal, biv.getW(), biv.getH()
                    )
                    else -> BitmapCropper.cropTo16x9(
                        biv.bitmapOriginal, biv.getW(), biv.getH()
                    )
                }
                biv.updatePosCanvas(cropTo16x9)
                biv.bitmapBlured = cropTo16x9
                biv.updateIpad(cropTo16x9!!, tmpl.ipad_type, tmpl.geTypeResize())
                if (tmpl.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                    tmpl.ipad_type != IpadType.GRADIENT.ordinal &&
                    tmpl.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                    tmpl.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                    tmpl.ipad_type != IpadType.CASSET_IMG.ordinal
                ) {
                    if (tmpl.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal) {
                        biv.bitmapBlured = UtilsBitmap.blur(context, cropTo16x9!!, 20, 1)
                        biv.bitmapSquare = biv.bitmapBlured
                    } else {
                        val min = Math.min(
                            biv.bitmapOriginal!!.width,
                            biv.bitmapOriginal!!.height
                        )
                        var i = 0
                        if (tmpl.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                            val width = (biv.ipad_rect!!.width() * 0.6f).toInt()
                            val f = min.toFloat()
                            var round = Math.round(tmpl.x_square * f)
                            var round2 = Math.round(tmpl.y_square * f)
                            var i2 = width + round
                            if (i2 > biv.bitmapOriginal!!.width) {
                                round -= i2 - biv.bitmapOriginal!!.width
                                i2 = biv.bitmapOriginal!!.width
                            }
                            var i3 = width + round2
                            if (i3 > biv.bitmapOriginal!!.height) {
                                round2 -= i3 - biv.bitmapOriginal!!.height
                                i3 = biv.bitmapOriginal!!.height
                            }
                            if (round < 0) {
                                round = 0
                            }
                            if (round2 >= 0) {
                                i = round2
                            }
                            val rect2 = Rect(round, i, i2, i3)
                            biv.radius_square = width
                            val widthSquare = (tmpl.width_square * f).toInt()
                            val heightSquare = (f * tmpl.height_square).toInt()
                            val cropToSquareWithRoundCorners2 = UtilsBitmap.cropToSquareWithRoundCorners(
                                biv.bitmapOriginal!!, rect2, width, widthSquare, heightSquare
                            )
                            rect2.right = rect2.left + widthSquare
                            rect2.bottom = rect2.top + heightSquare
                            biv.rectSquare = rect2
                            bitmap = cropToSquareWithRoundCorners2
                            rect = rect2
                        } else {
                            if (tmpl.ipad_type != IpadType.IPAD.ordinal &&
                                tmpl.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                                tmpl.ipad_type != IpadType.IPAD_CLASSIC.ordinal
                            ) {
                                val width2 = (biv.ipad_rect!!.width() * 1.0f).toInt()
                                val height = (cropTo16x9!!.height * 0.5355f).toInt()
                                val f2 = min.toFloat()
                                var round3 = Math.round(tmpl.x_square * f2)
                                var round4 = Math.round(tmpl.y_square * f2)
                                var i4 = width2 + round3
                                if (i4 > biv.bitmapOriginal!!.width) {
                                    round3 -= i4 - biv.bitmapOriginal!!.width
                                    i4 = biv.bitmapOriginal!!.width
                                }
                                var i5 = height + round4
                                if (i5 > biv.bitmapOriginal!!.height) {
                                    round4 -= i5 - biv.bitmapOriginal!!.height
                                    i5 = biv.bitmapOriginal!!.height
                                }
                                if (round3 < 0) {
                                    round3 = 0
                                }
                                if (round4 < 0) {
                                    round4 = 0
                                }
                                val rect3 = Rect(round3, round4, i4, i5)
                                val widthSquare2 = (tmpl.width_square * f2).toInt()
                                val heightSquare2 = (f2 * tmpl.height_square).toInt()
                                val cropToSquare = UtilsBitmap.cropToSquare(
                                    biv.bitmapOriginal!!, rect3, widthSquare2, heightSquare2
                                )
                                biv.bitmapSquare = cropToSquare
                                biv.radius_square = 0
                                rect3.right = rect3.left + widthSquare2
                                rect3.bottom = rect3.top + heightSquare2
                                biv.rectSquare = rect3
                                bitmap = cropToSquare
                                rect = rect3
                            } else {
                                val width3 = (biv.ipad_rect!!.width() * 0.87530595f).toInt()
                                val i6 = (width3 * 1.13f).toInt()
                                val min2 = Math.min(width3, i6)
                                val f3 = min.toFloat()
                                var round5 = Math.round(tmpl.x_square * f3)
                                var round6 = Math.round(tmpl.y_square * f3)
                                var i7 = width3 + round5
                                if (i7 > biv.bitmapOriginal!!.width) {
                                    round5 -= i7 - biv.bitmapOriginal!!.width
                                    i7 = biv.bitmapOriginal!!.width
                                }
                                var i8 = i6 + round6
                                if (i8 > biv.bitmapOriginal!!.height) {
                                    round6 -= i8 - biv.bitmapOriginal!!.height
                                    i8 = biv.bitmapOriginal!!.height
                                }
                                if (round5 < 0) {
                                    round5 = 0
                                }
                                if (round6 < 0) {
                                    round6 = 0
                                }
                                val rect4 = Rect(round5, round6, i7, i8)
                                cropToSquareWithRoundCorners = if (tmpl.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                                    val widthSquare3 = (tmpl.width_square * f3).toInt()
                                    val heightSquare3 = (f3 * tmpl.height_square).toInt()
                                    val cropToSquare2 = UtilsBitmap.cropToSquare(
                                        biv.bitmapOriginal!!, rect4, widthSquare3, heightSquare3
                                    )
                                    biv.bitmapSquare = cropToSquare2
                                    biv.radius_square = 0
                                    rect4.right = rect4.left + widthSquare3
                                    rect4.bottom = rect4.top + heightSquare3
                                    biv.rectSquare = rect4
                                    cropToSquare2
                                } else {
                                    val i9 = (min2 * 0.10800001f).toInt()
                                    biv.radius_square = i9
                                    val widthSquare4 = (tmpl.width_square * f3).toInt()
                                    val heightSquare4 = (f3 * tmpl.height_square).toInt()
                                    val result = UtilsBitmap.cropToSquareWithRoundCorners(
                                        biv.bitmapOriginal!!, rect4, i9, widthSquare4, heightSquare4
                                    )
                                    rect4.right = rect4.left + widthSquare4
                                    rect4.bottom = rect4.top + heightSquare4
                                    biv.rectSquare = rect4
                                    result
                                }
                                bitmap = cropToSquareWithRoundCorners
                                rect = rect4
                            }
                            biv.setBitmap(
                                UtilsBitmap.blur(context, cropTo16x9!!, 20, 1),
                                bitmap, -1, tmpl.ipad_type, tmpl.geTypeResize(), rect
                            )
                        }
                        tmpl.color_ipad = biv.colorIpad()
                        onInvalidate()
                    }
                    if (tmpl.ipad_type == IpadType.GRADIENT.ordinal) {
                        biv.setColorIpad(ViewCompat.MEASURED_STATE_MASK)
                    }
                    biv.bitmapSquare = cropTo16x9
                    biv.bitmapBlured = UtilsBitmap.blur(context, cropTo16x9!!, 20, 1)
                    tmpl.color_ipad = biv.colorIpad()
                    onInvalidate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Update the square/ipad bitmap during video playback.
     *
     * Moved from EngineActivity.updateSquareBitmap().
     * Reads [isOnScroll] and [isVideoPlaying] to decide whether to update.
     */
    fun updateSquareBitmap(framePath: String) {
        if (isOnScroll) {
            if (isVideoPlaying) {
                return
            }
        } else if (!isVideoPlaying) {
            return
        }
        executor.execute {
            var bitmap: Bitmap?
            var cropTo16x9: Bitmap?
            try {
                val tmpl = template ?: return@execute
                val biv = blurredImageView ?: return@execute
                try {
                    val height = biv.getH()
                    bitmap = Glide.with(context)
                        .asBitmap()
                        .load(framePath)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .override(height, height)
                        .submit().get()
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (!isOnScroll) {
                        biv.isDrawingSquareVideo = true
                    }
                    onInvalidate()
                    return@execute
                }
                if (bitmap == null) {
                    return@execute
                }
                if (tmpl.ipad_type != IpadType.BLACK_LAYER.ordinal &&
                    tmpl.ipad_type != IpadType.GRADIENT.ordinal &&
                    tmpl.ipad_type != IpadType.MASK_BRUSH.ordinal &&
                    tmpl.ipad_type != IpadType.BLUE_TYPE.ordinal &&
                    tmpl.ipad_type != IpadType.CASSET_IMG.ordinal &&
                    tmpl.ipad_type != IpadType.CASSET_IMG_BLUR.ordinal
                ) {
                    if (tmpl.ipad_type != IpadType.IPAD.ordinal &&
                        tmpl.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                        tmpl.ipad_type != IpadType.BOTTOM_RECT.ordinal &&
                        tmpl.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                        tmpl.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal
                    ) {
                        val width = (biv.ipad_rect!!.width() * 0.87530595f).toInt()
                        val i = (width * 1.13f).toInt()
                        val min = (Math.min(width, i) * 0.10800001f).toInt()
                        var round = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                        var round2 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                        var i2 = width + round
                        if (i2 > biv.bitmapOriginal!!.width) {
                            round -= i2 - biv.bitmapOriginal!!.width
                            i2 = biv.bitmapOriginal!!.width
                        }
                        var i3 = i + round2
                        if (i3 > biv.bitmapOriginal!!.height) {
                            round2 -= i3 - biv.bitmapOriginal!!.height
                            i3 = biv.bitmapOriginal!!.height
                        }
                        if (round < 0) {
                            round = 0
                        }
                        if (round2 < 0) {
                            round2 = 0
                        }
                        val rect = Rect(round, round2, i2, i3)
                        val width2 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                        val height2 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                        biv.setBitmapSquare(
                            UtilsBitmap.cropToSquareWithRoundCorners(bitmap, rect, min, width2, height2)
                        )
                        rect.right = rect.left + width2
                        rect.bottom = rect.top + height2
                        biv.rectSquare = rect
                    } else {
                        biv.setBitmapSquare(
                            UtilsBitmap.cropToSquareWithRoundCornersPlusScale(
                                bitmap, biv.rectSquare!!, biv.radius_square,
                                biv.bitmapSquare!!.width, biv.bitmapSquare!!.height
                            )
                        )
                    }
                    if (!isOnScroll) {
                        biv.isDrawingSquareVideo = true
                    }
                    onInvalidate()
                }
                cropTo16x9 = when (tmpl.geTypeResize()) {
                    ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                        bitmap, biv.getW(), biv.getH()
                    )
                    ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                        bitmap, biv.getW(), biv.getH()
                    )
                    else -> BitmapCropper.cropTo16x9(
                        bitmap, biv.getW(), biv.getH()
                    )
                }
                biv.bitmapSquare = cropTo16x9
                if (!isOnScroll) {
                    biv.isDrawingSquareVideo = true
                }
                onInvalidate()
            } finally {
                if (!isOnScroll) {
                    blurredImageView?.isDrawingSquareVideo = true
                }
                onInvalidate()
            }
        }
    }

    /**
     * Load and scale a bitmap from a content [uri] to fit the preview height.
     *
     * Moved from EngineUIHelper.setupOriginalBitmap(uri: Uri) / EngineActivity.setupOriginalBitmap(uri).
     */
    @Throws(IOException::class)
    fun setupOriginalBitmap(uri: Uri): Bitmap {
        val biv = blurredImageView ?: throw IllegalStateException("blurredImageView not set")
        val height = biv.getH()
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val min = height / Math.min(bitmap.width, bitmap.height).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap, Math.round(bitmap.width * min), Math.round(bitmap.height * min), true
        )
    }

    /**
     * Scale an existing [bitmap] so its shortest side equals [targetSize].
     *
     * Moved from EngineUIHelper.setupOriginalBitmap(bitmap, i) / EngineActivity.setupOriginalBitmap(bitmap, i).
     */
    fun setupOriginalBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
        val min = targetSize / Math.min(bitmap.width, bitmap.height).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap, Math.round(bitmap.width * min), Math.round(bitmap.height * min), true
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Activity result handlers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Handles crop activity result.
     * Moved from EngineActivity.onCropActivityResult().
     */
    fun onCropActivityResult(activityResult: ActivityResult) {
        var cropTo16x9: Bitmap?
        if (activityResult.resultCode != -1 || activityResult.data == null ||
            Common.bitmap == null || Common.bitmap!!.isRecycled
        ) {
            return
        }
        val biv = blurredImageView ?: return
        val tmpl = template ?: return
        Common.bitmap = Bitmap.createScaledBitmap(
            Common.bitmap!!, biv.getH(), biv.getH(), false
        )
        biv.bitmapOriginal = Common.bitmap
        cropTo16x9 = when (tmpl.geTypeResize()) {
            ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                biv.bitmapOriginal, biv.getW(), biv.getH()
            )
            ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                biv.bitmapOriginal, biv.getW(), biv.getH()
            )
            else -> BitmapCropper.cropTo16x9(
                biv.bitmapOriginal, biv.getW(), biv.getH()
            )
        }
        biv.bitmapBlured = UtilsBitmap.blur(context, cropTo16x9!!, 20, 1)
        onInvalidate()
    }

    /**
     * Handles crop data activity result.
     * Moved from EngineActivity.onCropDataActivityResult().
     */
    fun onCropDataActivityResult(activityResult: ActivityResult) {
        val tmpl = template ?: return
        val biv = blurredImageView ?: return
        if (activityResult.resultCode == -1) {
            val data = activityResult.data ?: return
            tmpl.x_square = data.getFloatExtra("x", 0.3f)
            tmpl.y_square = data.getFloatExtra("y", 0.4f)
            tmpl.width_square = data.getFloatExtra("w", 1.0f)
            tmpl.height_square = data.getFloatExtra("h", 0.5f)
            biv.bitmapSquare = Common.bitmap
            biv.rectSquare = Common.rect
            onInvalidate()
        }
    }

    /**
     * Handles image pick activity result.
     * Moved from EngineActivity.onImgActivityResult().
     */
    fun onImgActivityResult(activityResult: ActivityResult) {
        if (activityResult.resultCode != -1) return
        val data = activityResult.data ?: return
        val uri = data.data ?: return
        handleImg(uri)
    }

    /**
     * Handles video pick activity result.
     * Moved from EngineActivity.onVideoActivityResult().
     */
    fun onVideoActivityResult(activityResult: ActivityResult) {
        if (activityResult.resultCode != -1) return
        val data = activityResult.data ?: return
        val uri = data.data ?: return
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        handleVideo(uri)
    }

    /**
     * Handles bg-from-video (ChoiceBg) activity result.
     * Moved from EngineActivity.onChoiceBgResult().
     */
    fun onChoiceBgResult(activityResult: ActivityResult) {
        if (activityResult.resultCode != -1) return
        val data = activityResult.data ?: return
        if (Common.bitmap == null || Common.bitmap!!.isRecycled) return
        val biv = blurredImageView ?: return
        val tmpl = template ?: return
        try {
            Common.bitmap = Bitmap.createScaledBitmap(
                Common.bitmap!!, biv.height, biv.height, false
            )
            biv.bitmapOriginal = Common.bitmap
            val cropTo16x9 = when (tmpl.geTypeResize()) {
                ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(
                    biv.bitmapOriginal, biv.getW(), biv.getH()
                )
                ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(
                    biv.bitmapOriginal, biv.getW(), biv.getH()
                )
                else -> BitmapCropper.cropTo16x9(
                    biv.bitmapOriginal, biv.getW(), biv.getH()
                )
            }
            biv.bitmapBlured = UtilsBitmap.blur(context, cropTo16x9!!, 20, 1)
            onInvalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Handles crop result from activity launcher.
     * Moved from EngineActivity.onCropResult().
     */
    fun onCropResult(activityResult: ActivityResult) {
        val tmpl = template ?: return
        val biv = blurredImageView ?: return
        if (activityResult.resultCode == -1) {
            val data = activityResult.data
            if (data != null) {
                tmpl.x_square = data.getFloatExtra("x", 0.3f)
                tmpl.y_square = data.getFloatExtra("y", 0.4f)
                tmpl.width_square = data.getFloatExtra("w", 1.0f)
                tmpl.height_square = data.getFloatExtra("h", 0.5f)
                biv.bitmapSquare = Common.bitmap
                biv.rectSquare = Common.rect
                onInvalidate()
            }
        }
    }

    /**
     * Handles image result from activity launcher.
     * Moved from EngineActivity.onImgResult().
     */
    fun onImgResult(activityResult: ActivityResult) {
        val data = activityResult.data ?: return
        val uri = data.data ?: return
        if (activityResult.resultCode != -1) return
        handleImg(uri)
    }

    /**
     * Handles video result from activity launcher.
     * Moved from EngineActivity.onVideoResult().
     */
    fun onVideoResult(activityResult: ActivityResult) {
        val data = activityResult.data ?: return
        val uri = data.data ?: return
        if (activityResult.resultCode != -1) return
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        handleVideo(uri)
    }

    // ═══════════════════════════════════════════════════════════════════
    //  FFmpeg session management
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Cancel all running FFmpeg sessions.
     */
    fun cancelFFmpeg() {
        for (id in ffmpegSessionIds) {
            FFmpegKit.cancel(id)
        }
        ffmpegSessionIds.clear()
    }

    /**
     * Shut down the executor. Call when the host is being destroyed.
     */
    fun shutdown() {
        cancelFFmpeg()
        executor.shutdownNow()
    }

    companion object {
        private const val TAG = "BackgroundManager"
    }
}
