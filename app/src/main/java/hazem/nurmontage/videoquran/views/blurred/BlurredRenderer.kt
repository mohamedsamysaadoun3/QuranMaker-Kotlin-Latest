package hazem.nurmontage.videoquran.views.blurred

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.view.InputDeviceCompat
import androidx.core.view.ViewCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.views.BlurredImageView
import kotlin.math.cos

// ═══════════════════════════════════════════════════════════════════════════════
//  Extension functions for BlurredImageView that handle the main rendering
//  methods. Faithfully converted from BlurredImageView.java.
//
//  NOTE: The onDraw extension function cannot call super.onDraw(canvas).
//  The actual override in BlurredImageView should call super.onDraw(canvas)
//  first, then delegate to this extension function.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Draw watermark text "NurMontage" with optional remove button.
 *
 * Original: BlurredImageView.java lines 2139–2171
 */
fun BlurredImageView.drawWattermark(canvas: Canvas, isFlag: Boolean) {
    if (this.bitmapBlured == null || this.ipad_rect == null) {
        return
    }

    val textSize = this.bitmapBlured!!.width * 0.057f
    val blurRadius = 0.27f * textSize

    this.paintWattermark.textSize = textSize
    this.paintWattermark.isAntiAlias = true
    this.paintWattermark.color = -1 // White
    this.paintWattermark.alpha = 120

    val rect = Rect()
    val watermarkText = "NurMontage"
    this.paintWattermark.getTextBounds(watermarkText, 0, watermarkText.length, rect)
    val textWidth = rect.width().toFloat()
    val textHeight = rect.height().toFloat()

    val offset = 3.5f * blurRadius
    val textX = (this.bitmapBlured!!.width - textWidth) - offset
    val textY = canvas.height.toFloat() - offset

    canvas.drawText(watermarkText, textX, textY, this.paintWattermark)
    this.paintWattermark.clearShadowLayer()

    if (isFlag || this.isAnimWatermk) {
        return
    }

    this.mRectWattermark = RectF(
        textX - blurRadius,
        (textY - textHeight) - blurRadius,
        textX + textWidth + blurRadius,
        textY + blurRadius
    )

    val halfHeight = textHeight * 0.9f * 0.5f
    val removeBtnRect = RectF(
        this.mRectWattermark!!.right - halfHeight,
        this.mRectWattermark!!.top - halfHeight,
        this.mRectWattermark!!.right + halfHeight,
        this.mRectWattermark!!.top + halfHeight
    )

    val drawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_remove_wattermark)
    if (drawable != null) {
        drawable.setBounds(
            removeBtnRect.left.toInt(),
            removeBtnRect.top.toInt(),
            removeBtnRect.right.toInt(),
            removeBtnRect.bottom.toInt()
        )
        drawable.alpha = 180 // Angle.LEFT from konfetti library = 180
        drawable.draw(canvas)
    }

    this.mRectWattermark!!.union(removeBtnRect)
}

/**
 * Calculate the maximum text size that fits within the given width and height
 * constraints. Uses binary search (100 iterations) to find the optimal size.
 *
 * Original: BlurredImageView.java lines 2173–2196
 */
fun BlurredImageView.calculateTextSize(
    textValue: String?,
    paint: Paint,
    size: Int,
    counter: Int
): Float {
    var result = 0.0f
    if (textValue != null && textValue.isNotEmpty() && size > 0 && counter > 0) {
        paint.textSize = 1.0f
        val rect = Rect()
        paint.getTextBounds(textValue, 0, textValue.length, rect)
        var high = 1000.0f
        for (i in 0 until 100) {
            val mid = (result + high) / 2.0f
            paint.textSize = mid
            paint.getTextBounds(textValue, 0, textValue.length, rect)
            val w = rect.width().toFloat()
            val h = rect.height().toFloat()
            if (w > size || h > counter) {
                high = mid
            } else {
                result = mid
            }
        }
    }
    return result
}

/**
 * Main draw logic — renders the entire view including background bitmap,
 * iPad frame, progress bar, entities, bismilah, surah name, and watermark.
 *
 * Contains complex nested if/else for background bitmap drawing based on mIpadType.
 * The GRADIENT, MASK_BRUSH, BLACK_LAYER, and CASSET_IMG types additionally draw
 * bitmapNotBlur as their background layer.
 *
 * IMPORTANT: This is an extension function. The actual onDraw override in
 * BlurredImageView should call `super.onDraw(canvas)` first, then call this.
 *
 * Original: BlurredImageView.java lines 2204–2305
 */
fun BlurredImageView.onDrawExt(canvas: Canvas) {
    // BUG-V01 fix: use save-count pattern instead of save()/restore() inside try.
    // If any line between save() and restore() throws (NPE on rectFProgress!!,
    // selectTool!!, iViewCallback!!, entity_select!!), the catch swallowed it and
    // restore() was skipped — canvas save-count grew by 1 per dropped frame until
    // IllegalStateException (save count > 255) crashed the view.
    val saveCount = canvas.save()
    try {
        if (this.isNotDraw) {
            return
        }

        canvas.translate(this.mDrawingTranslationX, this.mDrawingTranslationY)
        canvas.clipRect(0, 0, this.mCanvas_width, this.mCanvas_height)
        canvas.drawColor(ViewCompat.MEASURED_STATE_MASK)

        val bitmap3 = this.bitmapBlured
        if (bitmap3 != null && !bitmap3.isRecycled) {

            // ═════════════════════════════════════════════════════════════════
            //  Phase 1: Type-specific background drawing
            //
            //  CONFIRMED by smali (BlurredImageView.smali lines 21307–21581):
            //  The original app draws type-specific backgrounds, then ALL types
            //  proceed to the SAME bitmapSquare check + drawIpad/drawProgress.
            //  The JADX decompiler split this into two exclusive branches,
            //  which was WRONG. The actual flow is unified.
            // ═════════════════════════════════════════════════════════════════
            if (this.mIpadType == IpadType.GRADIENT.ordinal ||
                this.mIpadType == IpadType.MASK_BRUSH.ordinal ||
                this.mIpadType == IpadType.BLACK_LAYER.ordinal ||
                this.mIpadType == IpadType.CASSET_IMG.ordinal
            ) {
                // GRADIENT/MASK_BRUSH/BLACK_LAYER/CASSET_IMG: draw bitmapNotBlur
                if (!this.isVideo) {
                    val bitmap = this.bitmapNotBlur
                    if (bitmap != null && !bitmap.isRecycled) {
                        canvas.drawBitmap(bitmap, this.btmX, this.btmY, this.paint)
                    }
                }
            } else if (this.mIpadType == IpadType.BLUE_TYPE.ordinal) {
                if (!this.isVideo) {
                    val bitmap2 = this.bitmapNotBlur
                    if (bitmap2 != null && !bitmap2.isRecycled) {
                        canvas.drawBitmap(this.bitmapNotBlur!!, this.btmX, this.btmY, this.grayscalePaint)
                    }
                }
            } else if (this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal) {
                if (!this.isVideo) {
                    canvas.drawBitmap(this.bitmapBlured!!, this.btmX, this.btmY, this.paint)
                }
            } else if (this.mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
                if (getColor_gradient() != null) {
                    this.paint.shader = this.linearGradient_classic
                    canvas.drawPaint(this.paint)
                    this.paint.shader = null
                } else {
                    canvas.drawColor(this.color_bg_type_classic)
                }
            } else if (this.mIpadType != IpadType.IPAD_NEOMORPHIC.ordinal &&
                this.mIpadType != IpadType.HEART.ordinal &&
                this.mIpadType != IpadType.BATTERY.ordinal &&
                this.mIpadType != IpadType.CASSET.ordinal
            ) {
                // All remaining types: draw blurred/unblurred background
                // IPAD_NEOMORPHIC, HEART, BATTERY, CASSET skip background drawing
                if (this.mIpadType == IpadType.IPAD_UNBLUR.ordinal) {
                    val nb = this.bitmapNotBlur
                    if (nb != null && !nb.isRecycled) {
                        canvas.drawBitmap(nb, this.btmX, this.btmY, this.paint)
                    }
                } else {
                    canvas.drawBitmap(this.bitmapBlured!!, this.btmX, this.btmY, this.paint)
                }
            }

            // ═════════════════════════════════════════════════════════════════
            //  Phase 2: bitmapSquare check → drawIpad or drawProgress
            //
            //  CONFIRMED by smali (BlurredImageView.smali lines 21586–21599):
            //  if-eqz v0 (bitmapSquare), :cond_a → if NOT null → drawIpad
            //  if null → drawProgress. This applies to ALL types, not just
            //  non-gradient types. The JADX decompiler incorrectly split this
            //  into two branches, causing GRADIENT/MASK_BRUSH/BLACK_LAYER/
            //  CASSET_IMG types to skip drawIpad entirely (BUG-3 root cause).
            // ═════════════════════════════════════════════════════════════════
            if (this.bitmapSquare != null) {
                this.drawIpad(canvas, true)
            } else {
                this.drawProgressExt(canvas)
            }

            // ═════════════════════════════════════════════════════════════════
            //  Phase 3: Overlays (same for all types)
            // ═════════════════════════════════════════════════════════════════
            this.drawLineHelper(canvas)
            this.drawBismilahExt(canvas)
            this.drawEntityExt(canvas)
            this.drawNameSurahExt(canvas)

            val entityView = this.entity_select
            if (entityView != null && this.selectTool != null && entityView.isVisible) {
                val entityView2 = this.entity_select
                if (entityView2 !is SurahNameEntity ||
                    entityView2 is BismilahEntity ||
                    (entityView2.entityQuran != null && this.entity_select!!.entityQuran!!.visible()) ||
                    (this.entity_select!!.entityTrslTimeline != null && this.entity_select!!.entityTrslTimeline!!.visible())
                ) {
                    this.selectTool!!.draw(canvas, this.entity_select!!)
                }
            }
        }

        // Billing removed — no watermark for any user

        // BUG-V02 fix: removed duplicate onDrawFinish() call from inside try.
        // `finally` block below already invokes it, so calling it here as well
        // caused the host to receive the callback TWICE per frame while playing,
        // which double-stepped playback state (e.g. frame counters, audio sync).
    } catch (e: Exception) {
        e.printStackTrace()
        if (!isPlaying() || this.iViewCallback == null) {
            return
        }
    } finally {
        // BUG-V01 fix: always restore the canvas to the saved count, even if
        // an exception was thrown mid-draw. Prevents save-count leak.
        try { canvas.restoreToCount(saveCount) } catch (_: Throwable) {}
        if (isPlaying() && this.iViewCallback != null) {
            this.iViewCallback!!.onDrawFinish()
        }
    }
}

/**
 * Draw progress bar with line drawing, circle cursor, and text labels.
 *
 * Draws:
 * 1. Background line (color_line_bg) across the progress rect
 * 2. Progress line (paintLecture color) from left to progress position
 * 3. Circle cursor at the progress position
 * 4. currentTime text at the left
 * 5. remainingTime text at the right
 *
 * Original: BlurredImageView.java lines 2307–2317
 */
fun BlurredImageView.drawProgressExt(canvas: Canvas) {
    val progressX = this.rectFProgress!!.left +
        ((this.rectFProgress!!.right - this.rectFProgress!!.left) * this.progress)

    // Background line
    this.linePaint.color = this.color_line_bg
    canvas.drawLine(
        this.rectFProgress!!.left,
        this.rectFProgress!!.centerY(),
        this.rectFProgress!!.right,
        this.rectFProgress!!.centerY(),
        this.linePaint
    )

    // Progress line
    this.linePaint.color = this.paintLecture.color
    canvas.drawLine(
        this.rectFProgress!!.left,
        this.rectFProgress!!.centerY(),
        progressX,
        this.rectFProgress!!.centerY(),
        this.linePaint
    )

    // Circle cursor
    canvas.drawCircle(progressX, this.rectFProgress!!.centerY(), this.radius_cursur, this.linePaint)

    // Time text
    val timeBounds = Rect()
    this.paintText.getTextBounds("0:60", 0, 4, timeBounds)
    canvas.drawText(this.currentTime, this.rectFProgress!!.left, this.rectFProgress!!.bottom, this.paintText)
    canvas.drawText(
        this.remainingTime,
        this.rectFProgress!!.right - timeBounds.width(),
        this.rectFProgress!!.bottom,
        this.paintText
    )
}

/**
 * Accelerate-decelerate interpolator using cosine function.
 *
 * Original: BlurredImageView.java lines 2319–2321
 */
fun BlurredImageView.accelerateDecelerateInterpolator(input: Float): Float {
    return ((cos((input + 1.0f) * Math.PI) / 2.0f) + 0.5f).toFloat()
}

/**
 * Draw aya rect overlay with semi-transparent color.
 *
 * Uses InputDeviceCompat.SOURCE_ANY color for the semi-transparent overlay.
 *
 * Original: BlurredImageView.java lines 3793–3796
 */
fun BlurredImageView.drawAyaExt(canvas: Canvas) {
    this.paintLecture.color = InputDeviceCompat.SOURCE_ANY
    canvas.drawRect(this.rectFAya!!, this.paintLecture)
}

/**
 * Draw lecture controls: pause/next/previous/favorite/repeat buttons
 * using drawable resources.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────┐
 * │  [favorite]  [previous]  [pause]  [next]  [repeat] │
 * └─────────────────────────────────────────────────────┘
 *
 * Original: BlurredImageView.java lines 3798–3832
 */
fun BlurredImageView.drawLectureExt(canvas: Canvas) {
    val height = this.rectFLecture!!.height() * 0.4f

    // ── Pause button (center) ──
    val pauseRect = Rect(
        (this.rectFLecture!!.centerX() - height).toInt(),
        (this.rectFLecture!!.centerY() - height).toInt(),
        (this.rectFLecture!!.centerX() + height).toInt(),
        (this.rectFLecture!!.centerY() + height).toInt()
    )
    val pauseDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.pause_circle_24px)
    pauseDrawable?.setTint(this.paintLecture.color)
    pauseDrawable?.setBounds(pauseRect.left, pauseRect.top, pauseRect.right, pauseRect.bottom)
    pauseDrawable?.draw(canvas)

    // ── Next button (right of pause) ──
    val smallHeight = (pauseRect.height() * 0.3f).toInt()
    val smallWidth = (pauseRect.width() * 0.45f).toInt()
    val nextOffset = (pauseRect.width() * 0.29f).toInt()
    val nextLeft = pauseRect.right + nextOffset
    val nextRect = Rect(
        nextLeft,
        pauseRect.centerY() - smallHeight,
        nextLeft + smallWidth,
        pauseRect.centerY() + smallHeight
    )
    val nextDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.skip_next_24px)
    nextDrawable?.setTint(this.paintLecture.color)
    nextDrawable?.setBounds(nextRect.left, nextRect.top, nextRect.right, nextRect.bottom)
    nextDrawable?.draw(canvas)

    // ── Previous button (left of pause) ──
    val prevRight = pauseRect.left - nextOffset
    val prevRect = Rect(
        prevRight - smallWidth,
        pauseRect.centerY() - smallHeight,
        prevRight,
        pauseRect.centerY() + smallHeight
    )
    val prevDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.skip_previous_24px)
    prevDrawable?.setTint(this.paintLecture.color)
    prevDrawable?.setBounds(prevRect.left, prevRect.top, prevRect.right, prevRect.bottom)
    prevDrawable?.draw(canvas)

    // ── Favorite button (far left, at progress rect left) ──
    val favHalfWidth = (prevRect.width() * 0.5f).toInt()
    val favLeft = this.rectFProgress!!.left.toInt()
    val favRight = prevRect.width() + favLeft
    val favoriteDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.favorite_24px)
    favoriteDrawable?.setTint(this.paintLecture.color)
    favoriteDrawable?.setBounds(favLeft, prevRect.centerY() - favHalfWidth, favRight, prevRect.centerY() + favHalfWidth)
    favoriteDrawable?.draw(canvas)

    // ── Repeat button (far right, at progress rect right) ──
    val repeatLeft = this.rectFProgress!!.right.toInt() - prevRect.width()
    val repeatRight = this.rectFProgress!!.right.toInt()
    val repeatDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.repeat_24px)
    repeatDrawable?.setTint(this.paintLecture.color)
    repeatDrawable?.setBounds(repeatLeft, prevRect.centerY() - favHalfWidth, repeatRight, prevRect.centerY() + favHalfWidth)
    repeatDrawable?.draw(canvas)
}

/**
 * Draw bismilah entities (bismilah and isti3adha).
 * Only draws if the entity is visible and its timeline is visible.
 *
 * Original: BlurredImageView.java lines 3775–3783
 */
fun BlurredImageView.drawBismilahExt(canvas: Canvas) {
    val bismilah = this.bismilahEntity
    if (bismilah != null && bismilah.isVisible &&
        this.bismilahEntity!!.getBismilahTimeline()?.visible() == true
    ) {
        this.bismilahEntity!!.draw(canvas)
    }

    val isti3adha = this.mIsti3adhaEntity
    if (isti3adha != null && isti3adha.isVisible &&
        this.mIsti3adhaEntity!!.getBismilahTimeline()?.visible() == true
    ) {
        this.mIsti3adhaEntity!!.draw(canvas)
    }
}

/**
 * Draw surah name entity.
 *
 * Original: BlurredImageView.java lines 3786–3790
 */
fun BlurredImageView.drawNameSurahExt(canvas: Canvas) {
    val surahName = this.surahNameEntity
    if (surahName != null) {
        surahName.draw(canvas)
    }
}

/**
 * Draw all visible quran and translation entities.
 * Quran entities are drawn if isVisible AND their entityQuran timeline is visible.
 * Translation entities are drawn if isVisible AND their entityTrslTimeline is visible.
 *
 * Original: BlurredImageView.java lines 2597–2610
 */
fun BlurredImageView.drawEntityExt(canvas: Canvas) {
    val quranList = this.getQuranEntities()
    for (i in quranList.indices) {
        val quranEntity = quranList[i]
        if (quranEntity.isVisible && quranEntity.entityQuran?.visible() == true) {
            quranEntity.draw(canvas)
        }
    }

    val trslList = this.getTranslationEntities()
    for (i in trslList.indices) {
        val translationQuranEntity = trslList[i]
        if (translationQuranEntity.isVisible && translationQuranEntity.entityTrslTimeline?.visible() == true) {
            translationQuranEntity.draw(canvas)
        }
    }
}
