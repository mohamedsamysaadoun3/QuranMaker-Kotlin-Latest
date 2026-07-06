package hazem.nurmontage.videoquran.views.blurred

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.views.BlurredImageView
import kotlin.math.min

// ═══════════════════════════════════════════════════════════════════════════════
//  Extension functions for BlurredImageView that handle ipad_rect calculations
//  and all rectF positions (aya, progress, lecture, surahName).
//
//  Faithfully converted from BlurredImageView.java lines 968–2106.
//  Every pixel-calculation constant is preserved exactly as in the original.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Updates `ipad_rect` based on [IpadType] and [ResizeType] using the provided [bitmap].
 *
 * Also stores the passed values into `mResizetype`, `bitmapNotBlur`, and `mIpadType`.
 *
 * Original: BlurredImageView.java line 968
 */
fun BlurredImageView.updateIpad(bitmap: Bitmap, size: Int, width117: Int) {
    this.mResizetype = width117
    this.bitmapNotBlur = bitmap
    this.mIpadType = size

    // ── IPAD_CLASSIC ──────────────────────────────────────────────────────
    if (size == IpadType.IPAD_CLASSIC.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height = bitmap.height * 0.7601563f
            val height2 = this.btmY + ((bitmap.height - height) * 0.5f)
            val blurRadius = height * 0.56f
            val width = this.btmX + ((bitmap.width - blurRadius) * 0.5f)
            this.ipad_rect = RectF(width, height2, blurRadius + width, height + height2)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height3 = bitmap.height * 0.7601563f
            val height4 = (this.btmY + (bitmap.height * 0.5f)) - (height3 * 0.5f)
            val floatValue2 = height3 * 0.56f
            val width2 = this.btmX + ((bitmap.width * 0.5f) - (floatValue2 * 0.5f))
            this.ipad_rect = RectF(width2, height4, floatValue2 + width2, height3 + height4)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height5 = bitmap.height * 0.7601563f
            val height6 = (this.btmY + (bitmap.height * 0.5f)) - (height5 * 0.5f)
            val floatValue3 = height5 * 0.56f
            val width3 = this.btmX + ((bitmap.width * 0.5f) - (floatValue3 * 0.5f))
            this.ipad_rect = RectF(width3, height6, floatValue3 + width3, height5 + height6)
        }
    }

    // ── IPAD_NEOMORPHIC ───────────────────────────────────────────────────
    if (size == IpadType.IPAD_NEOMORPHIC.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height7 = bitmap.height * 0.7601563f
            val height8 = this.btmY + ((bitmap.height - height7) * 0.5f)
            val floatValue4 = height7 * 0.56f
            val width4 = this.btmX + ((bitmap.width - floatValue4) * 0.5f)
            this.ipad_rect = RectF(width4, height8, floatValue4 + width4, height7 + height8)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height9 = bitmap.height * 0.7601563f
            val height10 = (this.btmY + (bitmap.height * 0.5f)) - (height9 * 0.5f)
            val f5 = height9 * 0.56f
            val width5 = this.btmX + ((bitmap.width * 0.5f) - (f5 * 0.5f))
            this.ipad_rect = RectF(width5, height10, f5 + width5, height9 + height10)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height11 = bitmap.height * 0.7601563f
            val height12 = (this.btmY + (bitmap.height * 0.5f)) - (height11 * 0.5f)
            val f6 = height11 * 0.56f
            val width6 = this.btmX + ((bitmap.width * 0.5f) - (f6 * 0.5f))
            this.ipad_rect = RectF(width6, height12, f6 + width6, height11 + height12)
        }
    }

    // ── CASSET / CASSET_IMG / CASSET_IMG_BLUR ────────────────────────────
    if (size == IpadType.CASSET.ordinal || size == IpadType.CASSET_IMG.ordinal || size == IpadType.CASSET_IMG_BLUR.ordinal) {
        val minVal = min(bitmap.width.toFloat(), bitmap.height.toFloat())
        val f7 = minVal * 0.45f
        val height13 = (this.btmY + (bitmap.height * 0.5f)) - (f7 * 0.5f)
        val f8 = minVal * 0.8f
        val width7 = this.btmX + ((bitmap.width * 0.5f) - (f8 * 0.5f))
        this.ipad_rect = RectF(width7, height13, f8 + width7, f7 + height13)
    }

    // ── IPAD / IPAD_UNBLUR ───────────────────────────────────────────────
    if (size == IpadType.IPAD.ordinal || size == IpadType.IPAD_UNBLUR.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height14 = bitmap.height * 0.7601563f
            val height15 = this.btmY + ((bitmap.height - height14) * 0.5f)
            val f9 = height14 * 0.56f
            val width8 = this.btmX + ((bitmap.width - f9) * 0.5f)
            this.ipad_rect = RectF(width8, height15, f9 + width8, height14 + height15)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height16 = bitmap.height * 0.7601563f
            val height17 = (this.btmY + (bitmap.height * 0.5f)) - (height16 * 0.5f)
            val f10 = height16 * 0.56f
            val width9 = this.btmX + ((bitmap.width * 0.5f) - (f10 * 0.5f))
            this.ipad_rect = RectF(width9, height17, f10 + width9, height16 + height17)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height18 = bitmap.height * 0.7601563f
            val height19 = (this.btmY + (bitmap.height * 0.5f)) - (height18 * 0.5f)
            val f11 = 0.56f * height18
            val width10 = this.btmX + ((bitmap.width * 0.5f) - (f11 * 0.5f))
            this.ipad_rect = RectF(width10, height19, f11 + width10, height18 + height19)
        }
    }

    // ── BOTTOM_RECT ───────────────────────────────────────────────────────
    if (size == IpadType.BOTTOM_RECT.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height20 = bitmap.height * 0.2f
            val height21 = this.btmY + ((bitmap.height * 0.88f) - height20)
            val width11 = bitmap.width * 0.75f
            val width12 = this.btmX + ((bitmap.width - width11) * 0.5f)
            this.ipad_rect = RectF(width12, height21, width11 + width12, height20 + height21)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height22 = bitmap.height * 0.25f
            val height23 = this.btmY + ((bitmap.height * 0.88f) - height22)
            val width13 = bitmap.width * 0.7f
            val width14 = this.btmX + ((bitmap.width - width13) * 0.5f)
            this.ipad_rect = RectF(width14, height23, width13 + width14, height22 + height23)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height24 = bitmap.height * 0.25f
            val height25 = this.btmY + ((bitmap.height * 0.88f) - height24)
            val width15 = bitmap.width * 0.7f
            val width16 = this.btmX + ((bitmap.width - width15) * 0.5f)
            this.ipad_rect = RectF(width16, height25, width15 + width16, height24 + height25)
        }
    }

    // ── ROUND_RECT ────────────────────────────────────────────────────────
    if (size == IpadType.ROUND_RECT.ordinal) {
        val min2 = min(bitmap.width.toFloat(), bitmap.height.toFloat()) * 0.45f
        val f12 = min2 * 0.5f
        val height26 = (this.btmY + (bitmap.height * 0.5f)) - f12
        val width17 = this.btmX + ((bitmap.width * 0.5f) - f12)
        this.ipad_rect = RectF(width17, height26, width17 + min2, min2 + height26)
    }

    // ── RECT ──────────────────────────────────────────────────────────────
    if (size == IpadType.RECT.ordinal) {
        val min3 = min(bitmap.width.toFloat(), bitmap.height.toFloat())
        val f13 = 0.35f * min3
        val height27 = this.btmY + (bitmap.height * 0.3f)
        val f14 = min3 * 0.4f
        val width18 = this.btmX + ((bitmap.width * 0.85f) - f14)
        this.ipad_rect = RectF(width18, height27, f14 + width18, f13 + height27)
    }

    // ── BLACK_LAYER / BLUE_TYPE / HEART / BATTERY ─────────────────────────
    if (size == IpadType.BLACK_LAYER.ordinal || size == IpadType.BLUE_TYPE.ordinal || size == IpadType.HEART.ordinal || size == IpadType.BATTERY.ordinal) {
        val f15 = this.btmX
        this.ipad_rect = RectF(f15 - 2.0f, this.btmY, f15 + bitmap.width + 2.0f, this.btmY + bitmap.height)
    }

    // ── GRADIENT ──────────────────────────────────────────────────────────
    if (size == IpadType.GRADIENT.ordinal) {
        this.ipad_rect = RectF(
            this.btmX - 2.0f,
            this.btmY + (bitmap.height * 0.2f),
            this.btmX + bitmap.width + 2.0f,
            this.btmY + bitmap.height
        )
    }

    // ── MASK_BRUSH ────────────────────────────────────────────────────────
    if (size == IpadType.MASK_BRUSH.ordinal) {
        val f16 = this.btmX
        this.ipad_rect = RectF(f16, this.btmY, bitmap.width + f16, this.btmY + bitmap.height)
    }

    // ── BORDER (special: sets STROKE style and returns early) ─────────────
    if (size == IpadType.BORDER.ordinal) {
        val min4 = min(bitmap.width, bitmap.height)
        val height28 = bitmap.height * Common.pHBorder
        val width19 = bitmap.width * Common.PWBorder
        val f17 = this.btmY + height28
        val f18 = this.btmX
        this.ipad_rect = RectF(
            f18 + width19,
            f17,
            (f18 + bitmap.width) - width19,
            (this.btmY + bitmap.height) - height28
        )
        this.paintIpad.style = Paint.Style.STROKE
        this.paintIpad.strokeWidth = min4 * 0.013f
        return
    }
    this.paintIpad.style = Paint.Style.FILL
}

/**
 * Updates `ipad_rect` using `bitmapBlured` instead of a passed bitmap.
 *
 * Original: BlurredImageView.java line 1112
 */
fun BlurredImageView.updateIpad() {
    val bm = this.bitmapBlured ?: return

    // ── IPAD_CLASSIC ──────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height = bm.height * 0.7601563f
            val height2 = (this.btmY + (bm.height * 0.5f)) - (height * 0.5f)
            val blurRadius = height * 0.56f
            val width = this.btmX + ((bm.width * 0.5f) - (blurRadius * 0.5f))
            this.ipad_rect = RectF(width, height2, blurRadius + width, height + height2)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height3 = bm.height * 0.7601563f
            val height4 = (this.btmY + (bm.height * 0.5f)) - (height3 * 0.5f)
            val floatValue2 = height3 * 0.56f
            val width2 = this.btmX + ((bm.width * 0.5f) - (floatValue2 * 0.5f))
            this.ipad_rect = RectF(width2, height4, floatValue2 + width2, height3 + height4)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height5 = bm.height * 0.7601563f
            val height6 = (this.btmY + (bm.height * 0.5f)) - (height5 * 0.5f)
            val floatValue3 = height5 * 0.56f
            val width3 = this.btmX + ((bm.width * 0.5f) - (floatValue3 * 0.5f))
            this.ipad_rect = RectF(width3, height6, floatValue3 + width3, height5 + height6)
        }
    }

    // ── IPAD_NEOMORPHIC ───────────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height7 = bm.height * 0.7601563f
            val height8 = (this.btmY + (bm.height * 0.5f)) - (height7 * 0.5f)
            val floatValue4 = height7 * 0.56f
            val width4 = this.btmX + ((bm.width * 0.5f) - (floatValue4 * 0.5f))
            this.ipad_rect = RectF(width4, height8, floatValue4 + width4, height7 + height8)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height9 = bm.height * 0.7601563f
            val height10 = (this.btmY + (bm.height * 0.5f)) - (height9 * 0.5f)
            val f5 = height9 * 0.56f
            val width5 = this.btmX + ((bm.width * 0.5f) - (f5 * 0.5f))
            this.ipad_rect = RectF(width5, height10, f5 + width5, height9 + height10)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height11 = bm.height * 0.7601563f
            val height12 = (this.btmY + (bm.height * 0.5f)) - (height11 * 0.5f)
            val f6 = height11 * 0.56f
            val width6 = this.btmX + ((bm.width * 0.5f) - (f6 * 0.5f))
            this.ipad_rect = RectF(width6, height12, f6 + width6, height11 + height12)
        }
    }

    // ── IPAD / IPAD_UNBLUR ───────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD.ordinal || this.mIpadType == IpadType.IPAD_UNBLUR.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height13 = bm.height * 0.7601563f
            val height14 = (this.btmY + (bm.height * 0.5f)) - (height13 * 0.5f)
            val f7 = height13 * 0.56f
            val width7 = this.btmX + ((bm.width * 0.5f) - (f7 * 0.5f))
            this.ipad_rect = RectF(width7, height14, f7 + width7, height13 + height14)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height15 = bm.height * 0.7601563f
            val height16 = (this.btmY + (bm.height * 0.5f)) - (height15 * 0.5f)
            val f8 = height15 * 0.56f
            val width8 = this.btmX + ((bm.width * 0.5f) - (f8 * 0.5f))
            this.ipad_rect = RectF(width8, height16, f8 + width8, height15 + height16)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height17 = bm.height * 0.7601563f
            val height18 = (this.btmY + (bm.height * 0.5f)) - (height17 * 0.5f)
            val f9 = 0.56f * height17
            val width9 = this.btmX + ((bm.width * 0.5f) - (f9 * 0.5f))
            this.ipad_rect = RectF(width9, height18, f9 + width9, height17 + height18)
        }
    }

    // ── BOTTOM_RECT ───────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val height19 = bm.height * 0.2f
            val height20 = this.btmY + ((bm.height * 0.88f) - height19)
            val width10 = bm.width * 0.75f
            val width11 = this.btmX + ((bm.width - width10) * 0.5f)
            this.ipad_rect = RectF(width11, height20, width10 + width11, height19 + height20)
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val height21 = bm.height * 0.25f
            val height22 = this.btmY + ((bm.height * 0.88f) - height21)
            val width12 = bm.width * 0.7f
            val width13 = this.btmX + ((bm.width - width12) * 0.5f)
            this.ipad_rect = RectF(width13, height22, width12 + width13, height21 + height22)
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val height23 = bm.height * 0.25f
            val height24 = this.btmY + ((bm.height * 0.88f) - height23)
            val width14 = bm.width * 0.7f
            val width15 = this.btmX + ((bm.width - width14) * 0.5f)
            this.ipad_rect = RectF(width15, height24, width14 + width15, height23 + height24)
        }
    }

    // ── ROUND_RECT ────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.ROUND_RECT.ordinal) {
        val minVal = min(bm.width.toFloat(), bm.height.toFloat()) * 0.45f
        val f10 = minVal * 0.5f
        val height25 = (this.btmY + (bm.height * 0.5f)) - f10
        val width16 = this.btmX + ((bm.width * 0.5f) - f10)
        this.ipad_rect = RectF(width16, height25, width16 + minVal, minVal + height25)
    }

    // ── CASSET / CASSET_IMG / CASSET_IMG_BLUR ────────────────────────────
    if (this.mIpadType == IpadType.CASSET.ordinal || this.mIpadType == IpadType.CASSET_IMG.ordinal || this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal) {
        val min2 = min(bm.width.toFloat(), bm.height.toFloat())
        val f11 = 0.45f * min2
        val height26 = (this.btmY + (bm.height * 0.5f)) - (f11 * 0.5f)
        val f12 = min2 * 0.8f
        val width17 = this.btmX + ((bm.width * 0.5f) - (0.5f * f12))
        this.ipad_rect = RectF(width17, height26, f12 + width17, f11 + height26)
    }

    // ── RECT ──────────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.RECT.ordinal) {
        val min3 = min(bm.width.toFloat(), bm.height.toFloat())
        val f13 = 0.35f * min3
        val height27 = this.btmY + (bm.height * 0.3f)
        val f14 = min3 * 0.4f
        val width18 = this.btmX + ((bm.width * 0.85f) - f14)
        this.ipad_rect = RectF(width18, height27, f14 + width18, f13 + height27)
    }

    // ── BLACK_LAYER / BLUE_TYPE / HEART / BATTERY ─────────────────────────
    if (this.mIpadType == IpadType.BLACK_LAYER.ordinal || this.mIpadType == IpadType.BLUE_TYPE.ordinal || this.mIpadType == IpadType.HEART.ordinal || this.mIpadType == IpadType.BATTERY.ordinal) {
        val f15 = this.btmX
        this.ipad_rect = RectF(f15 - 2.0f, this.btmY, f15 + bm.width + 2.0f, this.btmY + bm.height)
    }

    // ── GRADIENT ──────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.GRADIENT.ordinal) {
        this.ipad_rect = RectF(
            this.btmX - 2.0f,
            this.btmY + (bm.height * 0.2f),
            this.btmX + bm.width + 2.0f,
            this.btmY + bm.height
        )
    }

    // ── MASK_BRUSH ────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.MASK_BRUSH.ordinal) {
        val f16 = this.btmX
        this.ipad_rect = RectF(f16, this.btmY, bm.width + f16, this.btmY + bm.height)
    }

    // ── BORDER (special: sets STROKE style and returns early) ─────────────
    if (this.mIpadType == IpadType.BORDER.ordinal) {
        val min4 = min(bm.width, bm.height)
        val height28 = bm.height * Common.pHBorder
        val width19 = bm.width * Common.PWBorder
        val f17 = this.btmY + height28
        val f18 = this.btmX
        this.ipad_rect = RectF(
            f18 + width19,
            f17,
            (f18 + bm.width) - width19,
            (this.btmY + bm.height) - height28
        )
        this.paintIpad.style = Paint.Style.STROKE
        this.paintIpad.strokeWidth = min4 * 0.013f
        return
    }
    this.paintIpad.style = Paint.Style.FILL
}

/**
 * Creates all rectF positions (rectFAya, rectFProgress, rectFLecture, rectFSurahName)
 * for each IpadType. Also calls `updatePosSurahName()` to adjust surah name position
 * based on language direction.
 *
 * This determines where ALL visual elements are positioned on screen.
 *
 * Original: BlurredImageView.java line 1273
 */
fun BlurredImageView.createRect() {
    this.updatePosSurahName()
    val ipad = this.ipad_rect ?: return
    val bmSq = this.bitmapSquare

    // ── IPAD_CLASSIC ──────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height = ipad.height() - bmSq.height
        val blurRadius = height * 0.03f
        val centerX = ipad.centerX() - (bmSq.width * 0.5f)
        val height2 = ipad.top + blurRadius + bmSq.height
        this.rectFSurahName = RectF()
        val width = bmSq.width + centerX
        val floatValue2 = height2 + (blurRadius * 1.5f)
        val floatValue3 = ((height * 0.2f) - blurRadius) + floatValue2
        this.rectFSurahName!!.set(width - (ipad.width() * 0.4f), floatValue2, width, floatValue3)
        this.rectFAya = RectF()
        val floatValue4 = ((height * 0.35f) - blurRadius) + floatValue3
        this.rectFAya!!.set(centerX, floatValue3, bmSq.width + centerX, floatValue4)
        this.rectFProgress = RectF()
        val f5 = (height * 0.15f) + floatValue4
        this.rectFProgress!!.set(centerX, floatValue4, this.rectFAya!!.right, f5)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(centerX, f5, this.rectFAya!!.right, (height * 0.25f) + f5)
    }

    // ── IPAD_NEOMORPHIC ───────────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal) {
        val height3 = ipad.height() * 0.6f
        if (bmSq == null) return  // can't create rects without square bitmap
        val f6 = height3 * 0.03f
        val width2 = ipad.top + (ipad.width() * 0.3f * 2.4f)
        this.rectFSurahName = RectF()
        val width3 = ipad.width() * 0.5f * 0.5f
        val f7 = width2 + (2.8f * f6)
        this.rectFSurahName!!.set(ipad.centerX() - width3, f7, ipad.centerX() + width3, (ipad.height() * 0.1f) + f7)
        val centerY = this.rectFSurahName!!.centerY() + (2.0f * f6)
        val rectF = RectF()
        this.rectFAya = rectF
        val f8 = ((height3 * 0.25f) - f6) + centerY
        rectF.set(ipad.left + f6, centerY, ipad.right - f6, f8)
        val f9 = f8 + (0.055f * height3)
        this.rectFProgress = RectF()
        val f10 = (height3 * 0.2f) + f9
        val width4 = bmSq.width * 0.65f
        this.rectFProgress!!.set(ipad.centerX() - width4, f9, ipad.centerX() + width4, f10)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        val width5 = bmSq.width * 0.1f
        this.rectFLecture!!.set(this.rectFProgress!!.left - width5, f10 + f6, this.rectFProgress!!.right + width5, ipad.bottom)
    }

    // ── IPAD / IPAD_UNBLUR ───────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD.ordinal || this.mIpadType == IpadType.IPAD_UNBLUR.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height4 = ipad.height() - bmSq.height
        val f11 = height4 * 0.03f
        val centerX2 = ipad.centerX() - (bmSq.width * 0.5f)
        val height5 = ipad.top + f11 + bmSq.height
        this.rectFSurahName = RectF()
        val width6 = bmSq.width + centerX2
        val f12 = height5 + (f11 * 1.5f)
        val f13 = ((height4 * 0.2f) - f11) + f12
        this.rectFSurahName!!.set(width6 - (ipad.width() * 0.4f), f12, width6, f13)
        this.rectFAya = RectF()
        val f14 = ((height4 * 0.35f) - f11) + f13
        this.rectFAya!!.set(centerX2, f13, bmSq.width + centerX2, f14)
        this.rectFProgress = RectF()
        val f15 = (height4 * 0.15f) + f14
        this.rectFProgress!!.set(centerX2, f14, this.rectFAya!!.right, f15)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(centerX2, f15, this.rectFAya!!.right, (height4 * 0.25f) + f15)
    }

    // ── ROUND_RECT ────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.ROUND_RECT.ordinal) {
        val width7 = ipad.width() * 0.07f
        val f16 = ipad.left + width7
        val f17 = ipad.top + width7
        this.rectFSurahName = RectF()
        val width8 = ipad.width() * 0.52f
        val height6 = ipad.height() * 0.25f
        val f18 = ipad.right - width7
        val f19 = height6 + f17
        this.rectFSurahName!!.set(f18 - width8, f17, f18, f19)
        this.rectFAya = RectF()
        val width9 = ipad.width() * 0.02f
        val f20 = ipad.left + width9
        val f21 = ipad.right - width9
        val height7 = (ipad.height() * 0.3f) + f19
        this.rectFAya!!.set(f20, f19, f21, height7)
        this.rectFProgress = RectF()
        val height8 = (ipad.height() * 0.168f) + height7
        this.rectFProgress!!.set(f16, height7, f18, height8)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f16, height8, f18, ipad.bottom - (width7 * 0.75f))
    }

    // ── RECT ──────────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.RECT.ordinal) {
        val width10 = ipad.width() * 0.05f
        val f22 = ipad.left + width10
        val f23 = ipad.top + width10
        this.rectFSurahName = RectF()
        val width11 = ipad.width() * 0.52f
        val height9 = ipad.height() * 0.25f
        val f24 = ipad.right - width10
        val f25 = height9 + f23
        this.rectFSurahName!!.set(f24 - width11, f23, f24, f25)
        this.rectFAya = RectF()
        val height10 = (ipad.height() * 0.3f) + f25
        this.rectFAya!!.set(f22, f25, f24, height10)
        this.rectFProgress = RectF()
        val height11 = (ipad.height() * 0.18f) + height10
        this.rectFProgress!!.set(f22, height10, this.rectFAya!!.right, height11)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f22, height11, this.rectFAya!!.right, ipad.bottom - width10)
    }

    // ── BOTTOM_RECT ───────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
        val width12 = ipad.width() * 0.005f
        val width13 = ipad.left + (ipad.width() * 0.025f)
        val f26 = ipad.top + width12
        this.rectFSurahName = RectF()
        val width14 = ipad.width() * 0.37f
        val height12 = ipad.height() * 0.2f
        val width15 = ipad.right - (ipad.width() * 0.015f)
        val f27 = height12 + f26
        this.rectFSurahName!!.set(width15 - width14, f26, width15, f27)
        val f28 = f27 + width12
        val minVal = min(ipad.height(), ipad.width())
        val rectF2 = RectF()
        this.rectFAya = rectF2
        val f29 = (minVal * 0.25f) + f28
        rectF2.set(width13, f28, width15, f29)
        this.rectFProgress = RectF()
        this.rectFProgress!!.set(width13, f29, this.rectFAya!!.right, (minVal * 0.3f) + f29)
        val f30 = this.rectFProgress!!.bottom - (width12 * 2.5f)
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(width13, f30, this.rectFAya!!.right, this.rectFAya!!.height() + f30)
        this.rectFAya!!.bottom = this.rectFProgress!!.top * 1.025f
    }

    // ── BLACK_LAYER ───────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BLACK_LAYER.ordinal) {
        val f31 = if (this.mResizetype == ResizeType.SQUARE.ordinal) 0.34f else 0.4f
        val width16 = ipad.width() * 0.014f
        val f32 = ipad.left + width16
        val f33 = ipad.top + width16
        this.rectFSurahName = RectF()
        val min2 = min(ipad.width(), ipad.height())
        val height13 = ipad.height() * 0.12f
        val f34 = ipad.right - width16
        this.rectFSurahName!!.set(f34 - (f31 * min2), f33, f34, height13 + f33)
        val centerY2 = ipad.centerY() * 0.8f
        this.rectFAya = RectF()
        val f35 = min2 * 0.15f
        this.rectFAya!!.set(ipad.left + f35, centerY2, ipad.right - f35, centerY2 + (min2 * 0.3f))
        val centerY3 = this.rectFAya!!.centerY()
        val rectF3 = RectF()
        this.rectFProgress = rectF3
        val f36 = (min2 * 0.168f) + centerY3
        rectF3.set(this.rectFAya!!.left, centerY3, this.rectFAya!!.right, f36)
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f32, f36, f34, ipad.bottom - (width16 * 0.75f))
    }

    // ── HEART / BATTERY ──────────────────────────────────────────────────
    if (this.mIpadType == IpadType.HEART.ordinal || this.mIpadType == IpadType.BATTERY.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val width17 = ipad.width() * 0.014f
            val f37 = ipad.left + width17
            val f38 = ipad.top + width17
            this.rectFSurahName = RectF()
            val min3 = min(ipad.width(), ipad.height())
            val height14 = ipad.height() * 0.09f
            val f39 = ipad.right - width17
            this.rectFSurahName!!.set(f39 - (min3 * 0.4f), f38, f39, height14 + f38)
            val centerY4 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f40 = min3 * 0.15f
            this.rectFAya!!.set(ipad.left + f40, centerY4, ipad.right - f40, (min3 * 0.3f) + centerY4)
            this.rectFProgress = RectF()
            val width18 = this.rectFAya!!.width()
            val centerY5 = ipad.centerY() - (this.rectFAya!!.width() * 0.5f)
            val f41 = width18 + centerY5
            this.rectFProgress!!.set(this.rectFAya!!.left, centerY5, this.rectFAya!!.right, f41)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f37, f41, f39, ipad.bottom - (width17 * 0.75f))
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val width19 = ipad.width() * 0.014f
            val f42 = ipad.left + width19
            val f43 = ipad.top + width19
            this.rectFSurahName = RectF()
            val min4 = min(ipad.width(), ipad.height())
            val height15 = ipad.height() * 0.15f
            val f44 = ipad.right - width19
            this.rectFSurahName!!.set(f44 - (min4 * 0.4f), f43, f44, height15 + f43)
            val centerY6 = ipad.centerY() * 0.34f
            this.rectFAya = RectF()
            val f45 = 0.55f * min4
            this.rectFAya!!.set(ipad.left + f45, centerY6, ipad.right - f45, (min4 * 0.3f) + centerY6)
            this.rectFProgress = RectF()
            val centerY7 = this.rectFAya!!.centerY() * 1.3f
            val f46 = ipad.bottom * 0.9f
            this.rectFProgress!!.set(this.rectFAya!!.left, centerY7, this.rectFAya!!.right, f46)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f42, f46, f44, ipad.bottom - (width19 * 0.75f))
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val width20 = ipad.width() * 0.014f
            val f47 = ipad.left + width20
            val f48 = ipad.top + width20
            this.rectFSurahName = RectF()
            val min5 = min(ipad.width(), ipad.height())
            val height16 = ipad.height() * 0.09f
            val f49 = ipad.right - width20
            this.rectFSurahName!!.set(f49 - (min5 * 0.34f), f48, f49, height16 + f48)
            val centerY8 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f50 = min5 * 0.25f
            this.rectFAya!!.set(ipad.left + f50, centerY8, ipad.right - f50, (min5 * 0.3f) + centerY8)
            this.rectFProgress = RectF()
            val centerY9 = this.rectFAya!!.centerY() * 1.3f
            val f51 = ipad.bottom * 0.9f
            this.rectFProgress!!.set(this.rectFAya!!.left, centerY9, this.rectFAya!!.right, f51)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f47, f51, f49, ipad.bottom - (width20 * 0.75f))
        }
    }

    // ── CASSET / CASSET_IMG / CASSET_IMG_BLUR ────────────────────────────
    if (this.mIpadType == IpadType.CASSET.ordinal || this.mIpadType == IpadType.CASSET_IMG.ordinal || this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal) {
        val width21 = ipad.width() * 0.012f
        val f52 = ipad.top + width21
        this.rectFSurahName = RectF()
        val min6 = min(ipad.width(), ipad.height()) * 0.45f
        val height17 = ipad.height() * 0.19f
        val f53 = ipad.right - width21
        this.rectFSurahName!!.set(f53 - min6, f52, f53, height17 + f52)
        val width22 = ipad.width() * 0.33f
        val height18 = ipad.height() * 0.2f
        val f54 = ipad.top + height18
        this.rectFAya = RectF(ipad.centerX() - width22, f54, ipad.centerX() + width22, height18 + f54)
        this.rectFProgress = RectF()
        this.rectFLecture = RectF()
    }

    // ── GRADIENT ──────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.GRADIENT.ordinal) {
        val width23 = ipad.width() * 0.014f
        val f55 = ipad.left + width23
        ipad.centerY() // unused, matching original
        this.rectFSurahName = RectF()
        val min7 = min(ipad.width(), ipad.height())
        val f56 = ipad.right - width23
        this.rectFAya = RectF()
        val f57 = min7 * 0.15f
        val f58 = ipad.left + f57
        val f59 = ipad.right - f57
        val f60 = min7 * 0.3f
        val f61 = ipad.bottom * 0.95f
        this.rectFAya!!.set(f58, f61 - f60, f59, f61)
        this.rectFSurahName!!.set(f56 - (min7 * 0.52f), this.rectFAya!!.centerY() - f60, f56, this.rectFAya!!.centerY())
        val centerY10 = this.rectFAya!!.centerY()
        val rectF4 = RectF()
        this.rectFProgress = rectF4
        val f62 = (min7 * 0.168f) + centerY10
        rectF4.set(this.rectFAya!!.left, centerY10, this.rectFAya!!.right, f62)
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f55, f62, f56, ipad.bottom - (width23 * 0.75f))
        this.rectFSurahName!!.bottom = this.rectFAya!!.top
    }

    // ── MASK_BRUSH ────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.MASK_BRUSH.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val width24 = ipad.width() * 0.014f
            val f63 = ipad.left + width24
            val f64 = ipad.top + width24
            this.rectFSurahName = RectF()
            val width25 = ipad.width()
            val height19 = ipad.height() * 0.09f
            val f65 = ipad.right
            this.rectFSurahName!!.set(f65 - (width25 * 0.4f), f64, f65, height19 + f64)
            val height20 = ipad.height() * 0.19f
            this.rectFAya = RectF()
            val f66 = width25 * 0.15f
            this.rectFAya!!.set(ipad.left + f66, height20, ipad.right - f66, (width25 * 0.3f) + height20)
            val centerY11 = this.rectFAya!!.centerY()
            val rectF5 = RectF()
            this.rectFProgress = rectF5
            val f67 = (width25 * 0.168f) + centerY11
            rectF5.set(this.rectFAya!!.left, centerY11, this.rectFAya!!.right, f67)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f63, f67, f65, ipad.bottom - (width24 * 0.75f))
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val width26 = ipad.width() * 0.014f
            val f68 = ipad.left + width26
            val f69 = ipad.top + width26
            this.rectFSurahName = RectF()
            val width27 = ipad.width()
            val height21 = ipad.height() * 0.09f
            val f70 = ipad.right
            this.rectFSurahName!!.set(f70 - (width27 * 0.25f), f69, f70, height21 + f69)
            val height22 = ipad.height() * 0.11f
            this.rectFAya = RectF()
            val f71 = width27 * 0.15f
            this.rectFAya!!.set(ipad.left + f71, height22, ipad.right - f71, (width27 * 0.3f) + height22)
            val centerY12 = this.rectFAya!!.centerY()
            val rectF6 = RectF()
            this.rectFProgress = rectF6
            val f72 = (width27 * 0.168f) + centerY12
            rectF6.set(this.rectFAya!!.left, centerY12, this.rectFAya!!.right, f72)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f68, f72, f70, ipad.bottom - (width26 * 0.75f))
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val width28 = ipad.width() * 0.014f
            val f73 = ipad.left + width28
            val f74 = ipad.top + width28
            this.rectFSurahName = RectF()
            val width29 = ipad.width()
            val height23 = ipad.height() * 0.09f
            val f75 = ipad.right
            this.rectFSurahName!!.set(f75 - (width29 * 0.25f), f74, f75, height23 + f74)
            val f76 = (-this.rectFSurahName!!.height()) * 0.5f
            this.rectFAya = RectF()
            val f77 = width29 * 0.3f
            this.rectFAya!!.set(ipad.left + f77, f76, ipad.right - f77, f77 + f76)
            val centerY13 = this.rectFAya!!.centerY()
            val rectF7 = RectF()
            this.rectFProgress = rectF7
            val f78 = (width29 * 0.168f) + centerY13
            rectF7.set(this.rectFAya!!.left, centerY13, this.rectFAya!!.right, f78)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f73, f78, f75, ipad.bottom - (width28 * 0.75f))
        }
    }

    // ── BLUE_TYPE ─────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BLUE_TYPE.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val width30 = ipad.width() * 0.014f
            val f79 = ipad.left + width30
            val f80 = ipad.top + width30
            this.rectFSurahName = RectF()
            val min8 = min(ipad.width(), ipad.height())
            val height24 = ipad.height() * 0.09f
            val f81 = ipad.right - width30
            this.rectFSurahName!!.set(f81 - (min8 * 0.4f), f80, f81, height24 + f80)
            val centerY14 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f82 = min8 * 0.15f
            this.rectFAya!!.set(ipad.left + f82, centerY14, ipad.right - f82, (min8 * 0.3f) + centerY14)
            val centerY15 = ipad.centerY() * 0.2f
            val rectF8 = RectF()
            this.rectFProgress = rectF8
            val f83 = (min8 * 0.168f) + centerY15
            rectF8.set(this.rectFAya!!.left, centerY15, this.rectFAya!!.right, f83)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f79, f83, f81, ipad.bottom - (width30 * 0.75f))
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val width31 = ipad.width() * 0.014f
            val f84 = ipad.left + width31
            val f85 = ipad.top + width31
            this.rectFSurahName = RectF()
            val min9 = min(ipad.width(), ipad.height())
            val f86 = min9 * 0.4f
            val height25 = ipad.height() * 0.15f
            val f87 = ipad.right - width31
            this.rectFSurahName!!.set(f87 - f86, f85, f87, height25 + f85)
            val centerY16 = ipad.centerY() * 0.34f
            this.rectFAya = RectF()
            this.rectFAya!!.set(ipad.left + f86, centerY16, ipad.right - f86, (min9 * 0.3f) + centerY16)
            val centerY17 = ipad.centerY() * 0.2f
            val rectF9 = RectF()
            this.rectFProgress = rectF9
            val f88 = (min9 * 0.168f) + centerY17
            rectF9.set(this.rectFAya!!.left, centerY17, this.rectFAya!!.right, f88)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f84, f88, f87, ipad.bottom - (width31 * 0.75f))
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val width32 = ipad.width() * 0.014f
            val f89 = ipad.left + width32
            val f90 = ipad.top + width32
            this.rectFSurahName = RectF()
            val min10 = min(ipad.width(), ipad.height())
            val height26 = ipad.height() * 0.09f
            val f91 = ipad.right - width32
            this.rectFSurahName!!.set(f91 - (0.34f * min10), f90, f91, height26 + f90)
            val centerY18 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f92 = min10 * 0.2f
            this.rectFAya!!.set(ipad.left + f92, centerY18, ipad.right - f92, (0.3f * min10) + centerY18)
            val centerY19 = ipad.centerY() * 0.2f
            val rectF10 = RectF()
            this.rectFProgress = rectF10
            val f93 = (min10 * 0.16f) + centerY19
            rectF10.set(this.rectFAya!!.left, centerY19, this.rectFAya!!.right, f93)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f89, f93, f91, ipad.bottom - (width32 * 0.75f))
        }
    }

    // ── BORDER ────────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BORDER.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height27 = ipad.height() - bmSq.height
        val f94 = height27 * 0.03f
        val width33 = ipad.width() * 0.8f
        val centerX3 = ipad.centerX() - (width33 * 0.5f)
        val height28 = (ipad.top + f94 + bmSq.height) * 0.5f
        this.rectFSurahName = RectF()
        val f95 = centerX3 + width33
        val f96 = height28 + (1.5f * f94)
        val f97 = ((height27 * 0.2f) - f94) + f96
        this.rectFSurahName!!.set(f95 - (ipad.width() * 0.4f), f96, f95, f97)
        this.rectFAya = RectF()
        val f98 = width33 * 0.7f
        val centerX4 = ipad.centerX() - (f98 * 0.5f)
        val f99 = ((0.35f * height27) - f94) + f97
        this.rectFAya!!.set(centerX4, f97, f98 + centerX4, f99)
        this.rectFProgress = RectF()
        val f100 = (0.22f * height27) + f99
        this.rectFProgress!!.set(centerX3, f99, this.rectFSurahName!!.right, f100)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(centerX3, f100, this.rectFSurahName!!.right, (height27 * 0.25f) + f100)
    } else {
        this.paintIpad.style = Paint.Style.FILL
    }

    // ── Line stroke width & cursor radius ─────────────────────────────────
    if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal || this.mIpadType == IpadType.ROUND_RECT.ordinal) {
        this.linePaint.strokeWidth = this.rectFProgress!!.height() * 0.02f
    } else {
        this.linePaint.strokeWidth = this.rectFProgress!!.height() * 0.03f
    }
    this.radius_cursur = this.linePaint.strokeWidth * 4.2f
}

/**
 * Creates all rectF positions without calling `updatePosSurahName()`.
 * Used for cases where surah name positioning adjustment is not needed.
 *
 * The rect calculations are identical to [createRect]; the only difference
 * is that `updatePosSurahName()` is NOT called at the beginning.
 *
 * Original: BlurredImageView.java line 1691
 */
fun BlurredImageView.createRectWithoutSurahName() {
    val ipad = this.ipad_rect ?: return
    val bmSq = this.bitmapSquare

    // ── IPAD_CLASSIC ──────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height = ipad.height() - bmSq.height
        val blurRadius = height * 0.03f
        val centerX = ipad.centerX() - (bmSq.width * 0.5f)
        val height2 = ipad.top + blurRadius + bmSq.height
        this.rectFSurahName = RectF()
        val width = bmSq.width + centerX
        val floatValue2 = height2 + (blurRadius * 1.5f)
        val floatValue3 = ((height * 0.2f) - blurRadius) + floatValue2
        this.rectFSurahName!!.set(width - (ipad.width() * 0.4f), floatValue2, width, floatValue3)
        this.rectFAya = RectF()
        val floatValue4 = ((height * 0.35f) - blurRadius) + floatValue3
        this.rectFAya!!.set(centerX, floatValue3, bmSq.width + centerX, floatValue4)
        this.rectFProgress = RectF()
        val f5 = (height * 0.15f) + floatValue4
        this.rectFProgress!!.set(centerX, floatValue4, this.rectFAya!!.right, f5)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(centerX, f5, this.rectFAya!!.right, (height * 0.25f) + f5)
    }

    // ── IPAD_NEOMORPHIC ───────────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height3 = ipad.height() * 0.6f
        val f6 = height3 * 0.03f
        val width2 = ipad.top + (ipad.width() * 0.3f * 2.4f)
        this.rectFSurahName = RectF()
        val width3 = ipad.width() * 0.5f * 0.5f
        val f7 = width2 + (2.8f * f6)
        this.rectFSurahName!!.set(ipad.centerX() - width3, f7, ipad.centerX() + width3, (ipad.height() * 0.1f) + f7)
        val centerY = this.rectFSurahName!!.centerY() + (2.0f * f6)
        val rectF = RectF()
        this.rectFAya = rectF
        val f8 = ((height3 * 0.25f) - f6) + centerY
        rectF.set(ipad.left + f6, centerY, ipad.right - f6, f8)
        val f9 = f8 + (0.055f * height3)
        this.rectFProgress = RectF()
        val f10 = (height3 * 0.2f) + f9
        val width4 = bmSq.width * 0.65f
        this.rectFProgress!!.set(ipad.centerX() - width4, f9, ipad.centerX() + width4, f10)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        val width5 = bmSq.width * 0.1f
        this.rectFLecture!!.set(this.rectFProgress!!.left - width5, f10 + f6, this.rectFProgress!!.right + width5, ipad.bottom)
    }

    // ── IPAD / IPAD_UNBLUR ───────────────────────────────────────────────
    if (this.mIpadType == IpadType.IPAD.ordinal || this.mIpadType == IpadType.IPAD_UNBLUR.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height4 = ipad.height() - bmSq.height
        val f11 = height4 * 0.03f
        val centerX2 = ipad.centerX() - (bmSq.width * 0.5f)
        val height5 = ipad.top + f11 + bmSq.height
        this.rectFSurahName = RectF()
        val width6 = bmSq.width + centerX2
        val f12 = height5 + (f11 * 1.5f)
        val f13 = ((height4 * 0.2f) - f11) + f12
        this.rectFSurahName!!.set(width6 - (ipad.width() * 0.4f), f12, width6, f13)
        this.rectFAya = RectF()
        val f14 = ((height4 * 0.35f) - f11) + f13
        this.rectFAya!!.set(centerX2, f13, bmSq.width + centerX2, f14)
        this.rectFProgress = RectF()
        val f15 = (height4 * 0.15f) + f14
        this.rectFProgress!!.set(centerX2, f14, this.rectFAya!!.right, f15)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(centerX2, f15, this.rectFAya!!.right, (height4 * 0.25f) + f15)
    }

    // ── ROUND_RECT ────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.ROUND_RECT.ordinal) {
        val width7 = ipad.width() * 0.07f
        val f16 = ipad.left + width7
        val f17 = ipad.top + width7
        this.rectFSurahName = RectF()
        val width8 = ipad.width() * 0.52f
        val height6 = ipad.height() * 0.25f
        val f18 = ipad.right - width7
        val f19 = height6 + f17
        this.rectFSurahName!!.set(f18 - width8, f17, f18, f19)
        this.rectFAya = RectF()
        val width9 = ipad.width() * 0.02f
        val f20 = ipad.left + width9
        val f21 = ipad.right - width9
        val height7 = (ipad.height() * 0.3f) + f19
        this.rectFAya!!.set(f20, f19, f21, height7)
        this.rectFProgress = RectF()
        val height8 = (ipad.height() * 0.168f) + height7
        this.rectFProgress!!.set(f16, height7, f18, height8)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f16, height8, f18, ipad.bottom - (width7 * 0.75f))
    }

    // ── RECT ──────────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.RECT.ordinal) {
        val width10 = ipad.width() * 0.05f
        val f22 = ipad.left + width10
        val f23 = ipad.top + width10
        this.rectFSurahName = RectF()
        val width11 = ipad.width() * 0.52f
        val height9 = ipad.height() * 0.25f
        val f24 = ipad.right - width10
        val f25 = height9 + f23
        this.rectFSurahName!!.set(f24 - width11, f23, f24, f25)
        this.rectFAya = RectF()
        val height10 = (ipad.height() * 0.3f) + f25
        this.rectFAya!!.set(f22, f25, f24, height10)
        this.rectFProgress = RectF()
        val height11 = (ipad.height() * 0.18f) + height10
        this.rectFProgress!!.set(f22, height10, this.rectFAya!!.right, height11)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f22, height11, this.rectFAya!!.right, ipad.bottom - width10)
    }

    // ── BOTTOM_RECT ───────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
        val width12 = ipad.width() * 0.005f
        val width13 = ipad.left + (ipad.width() * 0.025f)
        val f26 = ipad.top + width12
        this.rectFSurahName = RectF()
        val width14 = ipad.width() * 0.37f
        val height12 = ipad.height() * 0.2f
        val width15 = ipad.right - (ipad.width() * 0.015f)
        val f27 = height12 + f26
        this.rectFSurahName!!.set(width15 - width14, f26, width15, f27)
        val f28 = f27 + width12
        val minVal = min(ipad.height(), ipad.width())
        val rectF2 = RectF()
        this.rectFAya = rectF2
        val f29 = (minVal * 0.25f) + f28
        rectF2.set(width13, f28, width15, f29)
        this.rectFProgress = RectF()
        this.rectFProgress!!.set(width13, f29, this.rectFAya!!.right, (minVal * 0.3f) + f29)
        val f30 = this.rectFProgress!!.bottom - (width12 * 2.5f)
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(width13, f30, this.rectFAya!!.right, this.rectFAya!!.height() + f30)
        this.rectFAya!!.bottom = this.rectFProgress!!.top * 1.025f
    }

    // ── BLACK_LAYER ───────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BLACK_LAYER.ordinal) {
        val f31 = if (this.mResizetype == ResizeType.SQUARE.ordinal) 0.34f else 0.4f
        val width16 = ipad.width() * 0.014f
        val f32 = ipad.left + width16
        val f33 = ipad.top + width16
        this.rectFSurahName = RectF()
        val min2 = min(ipad.width(), ipad.height())
        val height13 = ipad.height() * 0.12f
        val f34 = ipad.right - width16
        this.rectFSurahName!!.set(f34 - (f31 * min2), f33, f34, height13 + f33)
        val centerY2 = ipad.centerY() * 0.8f
        this.rectFAya = RectF()
        val f35 = min2 * 0.15f
        this.rectFAya!!.set(ipad.left + f35, centerY2, ipad.right - f35, centerY2 + (min2 * 0.3f))
        val centerY3 = this.rectFAya!!.centerY()
        val rectF3 = RectF()
        this.rectFProgress = rectF3
        val f36 = (min2 * 0.168f) + centerY3
        rectF3.set(this.rectFAya!!.left, centerY3, this.rectFAya!!.right, f36)
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f32, f36, f34, ipad.bottom - (width16 * 0.75f))
    }

    // ── HEART / BATTERY ──────────────────────────────────────────────────
    if (this.mIpadType == IpadType.HEART.ordinal || this.mIpadType == IpadType.BATTERY.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val width17 = ipad.width() * 0.014f
            val f37 = ipad.left + width17
            val f38 = ipad.top + width17
            this.rectFSurahName = RectF()
            val min3 = min(ipad.width(), ipad.height())
            val height14 = ipad.height() * 0.09f
            val f39 = ipad.right - width17
            this.rectFSurahName!!.set(f39 - (min3 * 0.4f), f38, f39, height14 + f38)
            val centerY4 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f40 = min3 * 0.15f
            this.rectFAya!!.set(ipad.left + f40, centerY4, ipad.right - f40, (min3 * 0.3f) + centerY4)
            this.rectFProgress = RectF()
            val width18 = this.rectFAya!!.width()
            val centerY5 = ipad.centerY() - (this.rectFAya!!.width() * 0.5f)
            val f41 = width18 + centerY5
            this.rectFProgress!!.set(this.rectFAya!!.left, centerY5, this.rectFAya!!.right, f41)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f37, f41, f39, ipad.bottom - (width17 * 0.75f))
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val width19 = ipad.width() * 0.014f
            val f42 = ipad.left + width19
            val f43 = ipad.top + width19
            this.rectFSurahName = RectF()
            val min4 = min(ipad.width(), ipad.height())
            val height15 = ipad.height() * 0.15f
            val f44 = ipad.right - width19
            this.rectFSurahName!!.set(f44 - (min4 * 0.4f), f43, f44, height15 + f43)
            val centerY6 = ipad.centerY() * 0.34f
            this.rectFAya = RectF()
            val f45 = 0.55f * min4
            this.rectFAya!!.set(ipad.left + f45, centerY6, ipad.right - f45, (min4 * 0.3f) + centerY6)
            this.rectFProgress = RectF()
            val centerY7 = this.rectFAya!!.centerY() * 1.3f
            val f46 = ipad.bottom * 0.9f
            this.rectFProgress!!.set(this.rectFAya!!.left, centerY7, this.rectFAya!!.right, f46)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f42, f46, f44, ipad.bottom - (width19 * 0.75f))
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val width20 = ipad.width() * 0.014f
            val f47 = ipad.left + width20
            val f48 = ipad.top + width20
            this.rectFSurahName = RectF()
            val min5 = min(ipad.width(), ipad.height())
            val height16 = ipad.height() * 0.09f
            val f49 = ipad.right - width20
            this.rectFSurahName!!.set(f49 - (min5 * 0.34f), f48, f49, height16 + f48)
            val centerY8 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f50 = min5 * 0.25f
            this.rectFAya!!.set(ipad.left + f50, centerY8, ipad.right - f50, (min5 * 0.3f) + centerY8)
            this.rectFProgress = RectF()
            val centerY9 = this.rectFAya!!.centerY() * 1.3f
            val f51 = ipad.bottom * 0.9f
            this.rectFProgress!!.set(this.rectFAya!!.left, centerY9, this.rectFAya!!.right, f51)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f47, f51, f49, ipad.bottom - (width20 * 0.75f))
        }
    }

    // ── CASSET / CASSET_IMG / CASSET_IMG_BLUR ────────────────────────────
    if (this.mIpadType == IpadType.CASSET.ordinal || this.mIpadType == IpadType.CASSET_IMG.ordinal || this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal) {
        val width21 = ipad.width() * 0.012f
        val f52 = ipad.top + width21
        this.rectFSurahName = RectF()
        val min6 = min(ipad.width(), ipad.height()) * 0.45f
        val height17 = ipad.height() * 0.19f
        val f53 = ipad.right - width21
        this.rectFSurahName!!.set(f53 - min6, f52, f53, height17 + f52)
        val width22 = ipad.width() * 0.33f
        val height18 = ipad.height() * 0.2f
        val f54 = ipad.top + height18
        this.rectFAya = RectF(ipad.centerX() - width22, f54, ipad.centerX() + width22, height18 + f54)
        this.rectFProgress = RectF()
        this.rectFLecture = RectF()
    }

    // ── GRADIENT ──────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.GRADIENT.ordinal) {
        val width23 = ipad.width() * 0.014f
        val f55 = ipad.left + width23
        ipad.centerY() // unused, matching original
        this.rectFSurahName = RectF()
        val min7 = min(ipad.width(), ipad.height())
        val f56 = ipad.right - width23
        this.rectFAya = RectF()
        val f57 = min7 * 0.15f
        val f58 = ipad.left + f57
        val f59 = ipad.right - f57
        val f60 = min7 * 0.3f
        val f61 = ipad.bottom * 0.95f
        this.rectFAya!!.set(f58, f61 - f60, f59, f61)
        this.rectFSurahName!!.set(f56 - (min7 * 0.52f), this.rectFAya!!.centerY() - f60, f56, this.rectFAya!!.centerY())
        val centerY10 = this.rectFAya!!.centerY()
        val rectF4 = RectF()
        this.rectFProgress = rectF4
        val f62 = (min7 * 0.168f) + centerY10
        rectF4.set(this.rectFAya!!.left, centerY10, this.rectFAya!!.right, f62)
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(f55, f62, f56, ipad.bottom - (width23 * 0.75f))
        this.rectFSurahName!!.bottom = this.rectFAya!!.top
    }

    // ── MASK_BRUSH ────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.MASK_BRUSH.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val width24 = ipad.width() * 0.014f
            val f63 = ipad.left + width24
            val f64 = ipad.top + width24
            this.rectFSurahName = RectF()
            val width25 = ipad.width()
            val height19 = ipad.height() * 0.09f
            val f65 = ipad.right
            this.rectFSurahName!!.set(f65 - (width25 * 0.4f), f64, f65, height19 + f64)
            val height20 = ipad.height() * 0.19f
            this.rectFAya = RectF()
            val f66 = width25 * 0.15f
            this.rectFAya!!.set(ipad.left + f66, height20, ipad.right - f66, (width25 * 0.3f) + height20)
            val centerY11 = this.rectFAya!!.centerY()
            val rectF5 = RectF()
            this.rectFProgress = rectF5
            val f67 = (width25 * 0.168f) + centerY11
            rectF5.set(this.rectFAya!!.left, centerY11, this.rectFAya!!.right, f67)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f63, f67, f65, ipad.bottom - (width24 * 0.75f))
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val width26 = ipad.width() * 0.014f
            val f68 = ipad.left + width26
            val f69 = ipad.top + width26
            this.rectFSurahName = RectF()
            val width27 = ipad.width()
            val height21 = ipad.height() * 0.09f
            val f70 = ipad.right
            this.rectFSurahName!!.set(f70 - (width27 * 0.25f), f69, f70, height21 + f69)
            val height22 = ipad.height() * 0.11f
            this.rectFAya = RectF()
            val f71 = width27 * 0.15f
            this.rectFAya!!.set(ipad.left + f71, height22, ipad.right - f71, (width27 * 0.3f) + height22)
            val centerY12 = this.rectFAya!!.centerY()
            val rectF6 = RectF()
            this.rectFProgress = rectF6
            val f72 = (width27 * 0.168f) + centerY12
            rectF6.set(this.rectFAya!!.left, centerY12, this.rectFAya!!.right, f72)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f68, f72, f70, ipad.bottom - (width26 * 0.75f))
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val width28 = ipad.width() * 0.014f
            val f73 = ipad.left + width28
            val f74 = ipad.top + width28
            this.rectFSurahName = RectF()
            val width29 = ipad.width()
            val height23 = ipad.height() * 0.09f
            val f75 = ipad.right
            this.rectFSurahName!!.set(f75 - (width29 * 0.25f), f74, f75, height23 + f74)
            val f76 = (-this.rectFSurahName!!.height()) * 0.5f
            this.rectFAya = RectF()
            val f77 = width29 * 0.3f
            this.rectFAya!!.set(ipad.left + f77, f76, ipad.right - f77, f77 + f76)
            val centerY13 = this.rectFAya!!.centerY()
            val rectF7 = RectF()
            this.rectFProgress = rectF7
            val f78 = (width29 * 0.168f) + centerY13
            rectF7.set(this.rectFAya!!.left, centerY13, this.rectFAya!!.right, f78)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f73, f78, f75, ipad.bottom - (width28 * 0.75f))
        }
    }

    // ── BLUE_TYPE ─────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BLUE_TYPE.ordinal) {
        if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
            val width30 = ipad.width() * 0.014f
            val f79 = ipad.left + width30
            val f80 = ipad.top + width30
            this.rectFSurahName = RectF()
            val min8 = min(ipad.width(), ipad.height())
            val height24 = ipad.height() * 0.09f
            val f81 = ipad.right - width30
            this.rectFSurahName!!.set(f81 - (min8 * 0.4f), f80, f81, height24 + f80)
            val centerY14 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f82 = min8 * 0.15f
            this.rectFAya!!.set(ipad.left + f82, centerY14, ipad.right - f82, (min8 * 0.3f) + centerY14)
            val centerY15 = ipad.centerY() * 0.2f
            val rectF8 = RectF()
            this.rectFProgress = rectF8
            val f83 = (min8 * 0.168f) + centerY15
            rectF8.set(this.rectFAya!!.left, centerY15, this.rectFAya!!.right, f83)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f79, f83, f81, ipad.bottom - (width30 * 0.75f))
        }
        if (this.mResizetype == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            val width31 = ipad.width() * 0.014f
            val f84 = ipad.left + width31
            val f85 = ipad.top + width31
            this.rectFSurahName = RectF()
            val min9 = min(ipad.width(), ipad.height())
            val f86 = min9 * 0.4f
            val height25 = ipad.height() * 0.15f
            val f87 = ipad.right - width31
            this.rectFSurahName!!.set(f87 - f86, f85, f87, height25 + f85)
            val centerY16 = ipad.centerY() * 0.34f
            this.rectFAya = RectF()
            this.rectFAya!!.set(ipad.left + f86, centerY16, ipad.right - f86, (min9 * 0.3f) + centerY16)
            val centerY17 = ipad.centerY() * 0.2f
            val rectF9 = RectF()
            this.rectFProgress = rectF9
            val f88 = (min9 * 0.168f) + centerY17
            rectF9.set(this.rectFAya!!.left, centerY17, this.rectFAya!!.right, f88)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f84, f88, f87, ipad.bottom - (width31 * 0.75f))
        }
        if (this.mResizetype == ResizeType.SQUARE.ordinal) {
            val width32 = ipad.width() * 0.014f
            val f89 = ipad.left + width32
            val f90 = ipad.top + width32
            this.rectFSurahName = RectF()
            val min10 = min(ipad.width(), ipad.height())
            val height26 = ipad.height() * 0.09f
            val f91 = ipad.right - width32
            this.rectFSurahName!!.set(f91 - (0.34f * min10), f90, f91, height26 + f90)
            val centerY18 = ipad.centerY() * 0.3f
            this.rectFAya = RectF()
            val f92 = min10 * 0.2f
            this.rectFAya!!.set(ipad.left + f92, centerY18, ipad.right - f92, (0.3f * min10) + centerY18)
            val centerY19 = ipad.centerY() * 0.2f
            val rectF10 = RectF()
            this.rectFProgress = rectF10
            val f93 = (min10 * 0.16f) + centerY19
            rectF10.set(this.rectFAya!!.left, centerY19, this.rectFAya!!.right, f93)
            this.rectFLecture = RectF()
            this.rectFLecture!!.set(f89, f93, f91, ipad.bottom - (width32 * 0.75f))
        }
    }

    // ── BORDER ────────────────────────────────────────────────────────────
    if (this.mIpadType == IpadType.BORDER.ordinal) {
        if (bmSq == null) return  // can't create rects without square bitmap
        val height27 = ipad.height() - bmSq.height
        val f94 = height27 * 0.03f
        val width33 = ipad.width() * 0.8f
        val centerX3 = ipad.centerX() - (width33 * 0.5f)
        val height28 = (ipad.top + f94 + bmSq.height) * 0.5f
        this.rectFSurahName = RectF()
        val f95 = centerX3 + width33
        val f96 = height28 + (1.5f * f94)
        val f97 = ((height27 * 0.2f) - f94) + f96
        this.rectFSurahName!!.set(f95 - (ipad.width() * 0.4f), f96, f95, f97)
        this.rectFAya = RectF()
        val f98 = width33 * 0.7f
        val centerX4 = ipad.centerX() - (f98 * 0.5f)
        val f99 = ((0.35f * height27) - f94) + f97
        this.rectFAya!!.set(centerX4, f97, f98 + centerX4, f99)
        this.rectFProgress = RectF()
        val f100 = (0.22f * height27) + f99
        this.rectFProgress!!.set(centerX3, f99, this.rectFSurahName!!.right, f100)
        this.rectFAya!!.bottom = this.rectFProgress!!.centerY()
        this.rectFLecture = RectF()
        this.rectFLecture!!.set(centerX3, f100, this.rectFSurahName!!.right, (height27 * 0.25f) + f100)
    } else {
        this.paintIpad.style = Paint.Style.FILL
    }

    // ── Line stroke width & cursor radius ─────────────────────────────────
    if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal || this.mIpadType == IpadType.ROUND_RECT.ordinal) {
        this.linePaint.strokeWidth = this.rectFProgress!!.height() * 0.02f
    } else {
        this.linePaint.strokeWidth = this.rectFProgress!!.height() * 0.03f
    }
    this.radius_cursur = this.linePaint.strokeWidth * 4.2f
}
