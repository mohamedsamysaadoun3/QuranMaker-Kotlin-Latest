package hazem.nurmontage.videoquran.views.blurred

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.util.Pair
import androidx.core.view.ViewCompat
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.SurahNameStyle
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.model.TimeModel
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.utils.ColorSchemeGenerator
import hazem.nurmontage.videoquran.utils.ColorUtils
import hazem.nurmontage.videoquran.utils.FontUtils
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.utils.UtilsFileLast
import hazem.nurmontage.videoquran.views.BlurredImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt

// ═══════════════════════════════════════════════════════════════════════════════
//  Extension functions for BlurredImageView that handle entity rendering,
//  transitions, text sizing, and export frame generation.
//
//  Faithfully converted from BlurredImageView.java.
//  DO NOT SIMPLIFY — the rendering code must be preserved exactly.
// ═══════════════════════════════════════════════════════════════════════════════

// ═══════════════════════════════════════════════════════════════════════════════
//  Transition Methods — frame-by-frame animation rendering
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Frame-by-frame slide-in from left animation.
 * Interpolates alpha and horizontal offset using AccelerateDecelerateInterpolator.
 * Each frame is saved as a PNG file.
 *
 * Original: BlurredImageView.java line 2624
 */
fun BlurredImageView.slideInToLeft(
    canvas: Canvas,
    bitmap: Bitmap,
    quranEntity: QuranEntity,
    file: File,
    size: Int,
    counter: Int
) {
    for (frame in 1 until counter) {
        val interpolated = accelerateDecelerateInterpolator(frame.toFloat() / counter)
        bitmap.eraseColor(0)
        quranEntity.singleDraw(canvas, (255.0f * interpolated).roundToInt(), 1.0f - interpolated)
        saveBitmap(bitmap, file, "quran_in_${size}_${frame}.png")
    }
}

/**
 * Frame-by-frame slide-in from right animation.
 * Interpolates alpha and horizontal offset (negative to positive).
 *
 * Original: BlurredImageView.java line 2633
 */
fun BlurredImageView.slideInToRight(
    canvas: Canvas,
    bitmap: Bitmap,
    quranEntity: QuranEntity,
    file: File,
    size: Int,
    counter: Int
) {
    for (frame in 1 until counter) {
        val interpolated = accelerateDecelerateInterpolator(frame.toFloat() / counter)
        bitmap.eraseColor(0)
        quranEntity.singleDraw(canvas, (255.0f * interpolated).roundToInt(), -1.0f + interpolated)
        saveBitmap(bitmap, file, "quran_in_${size}_${frame}.png")
    }
}

/**
 * Frame-by-frame slide-out to right animation.
 * Alpha fades out while entity slides to the right.
 *
 * Original: BlurredImageView.java line 2642
 */
fun BlurredImageView.slideOutToRight(
    canvas: Canvas,
    bitmap: Bitmap,
    quranEntity: QuranEntity,
    file: File,
    size: Int,
    counter: Int
) {
    for (frame in 1 until counter) {
        val interpolated = accelerateDecelerateInterpolator(frame.toFloat() / counter)
        val alpha = ((1.0f - interpolated) * 255.0f).roundToInt()
        bitmap.eraseColor(0)
        quranEntity.singleDraw(canvas, alpha, interpolated)
        saveBitmap(bitmap, file, "quran_out_${size}_${frame}.png")
    }
}

/**
 * Frame-by-frame slide-out to left animation.
 * Alpha fades out while entity slides to the left.
 *
 * Original: BlurredImageView.java line 2652
 */
fun BlurredImageView.slideOutToLeft(
    canvas: Canvas,
    bitmap: Bitmap,
    quranEntity: QuranEntity,
    file: File,
    size: Int,
    counter: Int
) {
    for (frame in 1 until counter) {
        val interpolated = accelerateDecelerateInterpolator(frame.toFloat() / counter)
        bitmap.eraseColor(0)
        quranEntity.singleDraw(canvas, ((1.0f - interpolated) * 255.0f).roundToInt(), interpolated * -1.0f)
        saveBitmap(bitmap, file, "quran_out_${size}_${frame}.png")
    }
}

/**
 * Frame-by-frame fade in animation.
 * Interpolates alpha from 0 to 255 using AccelerateDecelerateInterpolator.
 *
 * Original: BlurredImageView.java line 2661
 */
fun BlurredImageView.fadeIn(
    canvas: Canvas,
    bitmap: Bitmap,
    quranEntity: QuranEntity,
    file: File,
    size: Int,
    counter: Int
) {
    for (frame in 1 until counter) {
        val alpha = (accelerateDecelerateInterpolator(frame.toFloat() / counter) * 255.0f).roundToInt()
        bitmap.eraseColor(0)
        quranEntity.singleDraw(canvas, alpha)
        saveBitmap(bitmap, file, "quran_in_${size}_${frame}.png")
    }
}

/**
 * Frame-by-frame fade out animation.
 * Interpolates alpha from 255 to 0 using AccelerateDecelerateInterpolator.
 *
 * Original: BlurredImageView.java line 2670
 */
fun BlurredImageView.fadeOut(
    canvas: Canvas,
    bitmap: Bitmap,
    quranEntity: QuranEntity,
    file: File,
    size: Int,
    counter: Int
) {
    for (frame in 1 until counter) {
        val alpha = (accelerateDecelerateInterpolator(1.0f - frame.toFloat() / counter) * 255.0f).roundToInt()
        bitmap.eraseColor(0)
        quranEntity.singleDraw(canvas, alpha)
        saveBitmap(bitmap, file, "quran_out_${size}_${frame}.png")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Export Frame Methods — per-entity bitmap creation for video export
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Per-entity bitmap creation for video export.
 * For each visible entity, creates a bitmap with setupCanvasDraw/restoreCanvas,
 * renders with singleDraw, and saves to file.
 *
 * Also handles bismilah and isti3adha entities.
 *
 * Original: BlurredImageView.java line 2679
 */
fun BlurredImageView.drawEntityBitmap(file: File, size: Int, index: Int) {
    updateSizeAyaSave(size, index)
    updateSizeTrslSave(size, index)
    updateBismilahEntity(size, index)

    // ── Quran entities ──
    var entityCounter = 0
    for (i in getQuranEntities().indices) {
        val quranEntity = getQuranEntities()[i]
        if (quranEntity.entityQuran?.visible() == true) {
            quranEntity.getPaintAya().alpha = 255
            quranEntity.getPaintTranslationAya().alpha = 255
            val transition = quranEntity.entityQuran?.getTransition()
            val indexF = index.toFloat()
            val sizeF = size.toFloat()
            val bmp = Bitmap.createBitmap(
                (quranEntity.copyRect!!.right * sizeF - quranEntity.copyRect!!.left * sizeF).toInt(),
                (quranEntity.copyRect!!.bottom * indexF - quranEntity.copyRect!!.top * indexF).toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bmp)
            quranEntity.setupCanvasDraw(canvas)
            quranEntity.singleDraw(canvas)
            quranEntity.entityQuran!!.setFile("quran_${entityCounter}.png")
            saveBitmap(bmp, file, quranEntity.entityQuran!!.getFile()!!)
            if (transition != null) {
                transition.fromW = bmp.width.toFloat()
            }
            entityCounter++
            quranEntity.restoreCanvas(canvas)
        }
    }

    // ── Translation entities ──
    var trslCounter = 0
    for (i in getTranslationEntities().indices) {
        val translationQuranEntity = getTranslationEntities()[i]
        if (translationQuranEntity.entityTrslTimeline?.visible() == true) {
            translationQuranEntity.getPaintAya().alpha = 255
            val transition2 = translationQuranEntity.entityTrslTimeline?.getTransition()
            val indexF2 = index.toFloat()
            val bmp2 = Bitmap.createBitmap(
                size,
                (translationQuranEntity.copyRect!!.bottom * indexF2 - translationQuranEntity.copyRect!!.top * indexF2).toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas2 = Canvas(bmp2)
            translationQuranEntity.setupCanvasDraw(canvas2)
            translationQuranEntity.singleDraw(canvas2)
            translationQuranEntity.entityTrslTimeline!!.setFile("trs_${trslCounter}.png")
            saveBitmap(bmp2, file, translationQuranEntity.entityTrslTimeline!!.getFile()!!)
            if (transition2 != null) {
                transition2.fromW = bmp2.width.toFloat()
            }
            trslCounter++
            translationQuranEntity.restoreCanvas(canvas2)
        }
    }

    // ── Bismilah entity ──
    val bismilah = this.bismilahEntity
    if (bismilah != null && bismilah.getBismilahTimeline()?.visible() == true) {
        bismilahEntity!!.getPaintAya().alpha = 255
        val transition3 = bismilahEntity!!.getBismilahTimeline()!!.getTransition()
        val indexF3 = index.toFloat()
        val sizeF3 = size.toFloat()
        val bmp3 = Bitmap.createBitmap(
            (bismilahEntity!!.copyRect!!.right * sizeF3 - bismilahEntity!!.copyRect!!.left * sizeF3).toInt(),
            (bismilahEntity!!.copyRect!!.bottom * indexF3 - bismilahEntity!!.copyRect!!.top * indexF3).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas3 = Canvas(bmp3)
        bismilahEntity!!.setupCanvasDraw(canvas3)
        bismilahEntity!!.singleDraw(canvas3)
        bismilahEntity!!.getBismilahTimeline()!!.setFile("bismilah.png")
        saveBitmap(bmp3, file, bismilahEntity!!.getBismilahTimeline()!!.getFile()!!)
        if (transition3 != null) {
            transition3.fromW = bmp3.width.toFloat()
        }
    }

    // ── Isti3adha entity ──
    val isti3adha = this.mIsti3adhaEntity
    if (isti3adha == null || isti3adha.getBismilahTimeline()?.visible() != true) {
        return
    }
    mIsti3adhaEntity!!.getPaintAya().alpha = 255
    val transition4 = mIsti3adhaEntity!!.getBismilahTimeline()!!.getTransition()
    val indexF4 = index.toFloat()
    val sizeF4 = size.toFloat()
    val bmp4 = Bitmap.createBitmap(
        (mIsti3adhaEntity!!.copyRect!!.right * sizeF4 - mIsti3adhaEntity!!.copyRect!!.left * sizeF4).toInt(),
        (mIsti3adhaEntity!!.copyRect!!.bottom * indexF4 - mIsti3adhaEntity!!.copyRect!!.top * indexF4).toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas4 = Canvas(bmp4)
    mIsti3adhaEntity!!.setupCanvasDraw(canvas4)
    mIsti3adhaEntity!!.singleDraw(canvas4)
    mIsti3adhaEntity!!.getBismilahTimeline()!!.setFile("mIstiada.png")
    saveBitmap(bmp4, file, mIsti3adhaEntity!!.getBismilahTimeline()!!.getFile()!!)
    if (transition4 != null) {
        transition4.fromW = bmp4.width.toFloat()
    }
}

/**
 * Save progress bar bitmap for export.
 * Draws background line, progress line, and circle cursor.
 *
 * Original: BlurredImageView.java line 2323
 */
fun BlurredImageView.saveProgressBitmap(file: File, blurRadius: Float) {
    val bmp = Bitmap.createBitmap(
        rectFProgress!!.width().toInt(),
        rectFProgress!!.height().toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    val height = canvas.height * 0.5f

    if (mIpadType == IpadType.BOTTOM_RECT.ordinal) {
        paintText.textSize = min(ipad_rect!!.width(), ipad_rect!!.height()) * 0.07f
    } else if (mIpadType == IpadType.BORDER.ordinal) {
        paintText.textSize = min(ipad_rect!!.width(), ipad_rect!!.height()) * 0.027f
    } else {
        paintText.textSize = ipad_rect!!.width() * 0.0388f
    }

    val textBounds = Rect()
    paintText.getTextBounds("0:60", 0, 4, textBounds)
    newLeft_txt = (rectFProgress!!.width() - textBounds.width()) * 0.964f
    txt_y = canvas.height * 0.76f

    linePaint.isAntiAlias = false
    linePaint.strokeWidth = linePaint.strokeWidth * 1.1f
    linePaint.color = paintLecture.color
    canvas.drawLine(0f, height, canvas.width.toFloat(), height, linePaint)
    saveBitmap(bmp, file, Constants.LINE_BG)

    bmp.eraseColor(0)
    linePaint.color = color_line_bg
    canvas.drawLine(0f, height, canvas.width.toFloat(), height, linePaint)
    linePaint.color = paintLecture.color
    linePaint.isAntiAlias = true
    canvas.drawCircle(blurRadius, height, blurRadius, linePaint)
    saveBitmap(bmp, file, Constants.LINE_PROGRESS)
}

/**
 * Cassette progress bitmap — draws drawable into bitmap for export.
 *
 * Original: BlurredImageView.java line 2351
 */
fun BlurredImageView.saveProgressCasetBitmap(file: File, size: Int, size145: Int, drawable: Drawable?) {
    val bmp = Bitmap.createBitmap(size, size145, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    drawable?.setBounds(0, 0, size, size145)
    drawable?.draw(canvas)
    saveBitmap(bmp, file, Constants.LINE_BG)
}

/**
 * Neumorphic progress bar bitmap for export.
 * Creates rounded track with background, temporary, and progress layers.
 *
 * Original: BlurredImageView.java line 2359
 */
fun BlurredImageView.saveProgressBitmapTypeIPAD_NEOMORPHIC(file: File, bitmap: Bitmap?) {
    val bmp = Bitmap.createBitmap(
        rectFProgress!!.width().toInt(),
        rectFProgress!!.height().toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    paintText.textSize = ipad_rect!!.width() * 0.0388f

    val textBounds = Rect()
    paintText.getTextBounds("0:60", 0, 4, textBounds)
    newLeft_txt = (rectFProgress!!.width() - textBounds.width()) * 0.964f
    txt_y = canvas.height * 0.76f

    val height = canvas.height * 0.5f
    val height2 = rectFProgress!!.height() * 0.18f
    val blurRadius = 0.5f * height2
    linePaint.strokeWidth = blurRadius
    linePaint.color = paintLecture.color
    val height3 = canvas.height.toFloat()
    val width = canvas.width * 0.024f

    canvas.save()
    canvas.clipRect(width, 0f, canvas.width.toFloat(), canvas.height.toFloat())
    val floatValue2 = height - blurRadius
    val floatValue3 = height + blurRadius
    canvas.drawRoundRect(0f, floatValue2, canvas.width.toFloat(), floatValue3, height3, height3, linePaint)
    canvas.restore()
    saveBitmap(bmp, file, Constants.LINE_BG)

    bmp.eraseColor(0)
    canvas.save()
    canvas.clipRect(0f, floatValue2, width, floatValue3)
    canvas.drawBitmap(
        bitmap!!,
        Rect(rectFProgress!!.left.toInt(), rectFProgress!!.top.toInt(), rectFProgress!!.right.toInt(), rectFProgress!!.bottom.toInt()),
        Rect(0, 0, bmp.width, bmp.height),
        null
    )
    canvas.drawRoundRect(0f, floatValue2, canvas.width.toFloat(), floatValue3, height3, height3, linePaint)
    canvas.restore()
    saveBitmap(bmp, file, Constants.LINE_BG_TMP)

    linePaint.strokeWidth = height2
    linePaint.color = color_line_bg
    bmp.eraseColor(0)
    canvas.drawRoundRect(0f, floatValue2, canvas.width.toFloat(), floatValue3, height3, height3, linePaint)
    saveBitmap(bmp, file, Constants.LINE_PROGRESS)
}

/**
 * Blue type progress bar bitmap for export.
 * Draws simple line progress with thicker stroke.
 *
 * Original: BlurredImageView.java line 2394
 */
fun BlurredImageView.saveProgressBitmapTypeBlue(file: File) {
    val bmp = Bitmap.createBitmap(
        rectFProgress!!.width().toInt(),
        rectFProgress!!.height().toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    val height = canvas.height * 0.5f
    val height2 = rectFProgress!!.height() * 0.18f
    linePaint.strokeWidth = 0.5f * height2
    linePaint.color = paintLecture.color
    canvas.drawLine(0f, height, canvas.width.toFloat(), height, linePaint)
    saveBitmap(bmp, file, Constants.LINE_BG)

    bmp.eraseColor(0)
    linePaint.strokeWidth = height2
    linePaint.color = color_line_bg
    canvas.drawLine(0f, height, canvas.width.toFloat(), height, linePaint)
    saveBitmap(bmp, file, Constants.LINE_PROGRESS)
}

/**
 * Heart shape progress bitmap for export.
 * Creates heart path, draws progress fill with PorterDuff CLEAR compositing,
 * then strokes the outline.
 *
 * Returns a Pair of (fillStartY, fillHeight) for video compositing.
 *
 * Original: BlurredImageView.java line 2410
 */
fun BlurredImageView.saveProgressBitmapTypeHeart(file: File, bitmap: Bitmap?): Pair<Float, Int> {
    val bmp = Bitmap.createBitmap(
        bitmap!!.width,
        rectFProgress!!.height().toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    val path = Path()
    val width = rectFProgress!!.width()
    val height = rectFProgress!!.height()
    val centerX = rectFProgress!!.centerX()
    val height2 = canvas.height * 0.5f
    val minDim = min(width, height) / 2.0f
    val bottomY = height2 + 0.6f * minDim

    path.moveTo(centerX, bottomY)
    val armWidth = minDim * 1.2f
    val armY = height2 + 0.1f * minDim
    val curveWidth = minDim * 0.8f
    val topCurveY = height2 - 0.9f * minDim
    path.cubicTo(centerX + armWidth, armY, centerX + curveWidth, topCurveY, centerX, height2 - 0.4f * minDim)
    path.cubicTo(centerX - curveWidth, topCurveY, centerX - armWidth, armY, centerX, bottomY)
    path.close()

    val pathBounds = RectF()
    path.computeBounds(pathBounds, true)
    val f6 = height2 - 0.536f * minDim
    val round = (pathBounds.bottom - f6).roundToInt()

    val alpha = paintIpad.alpha
    val linearGradient = linearGradient_classic
    if (getColor_gradient() != null && linearGradient != null) {
        paintIpad.shader = linearGradient
        paintIpad.color = getColor_gradient()!!.color
    } else {
        paintIpad.color = color_ipad
    }
    paintIpad.alpha = alpha

    canvas.drawColor(ViewCompat.MEASURED_STATE_MASK)
    paintIpad.style = Paint.Style.FILL
    paintIpad.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    canvas.drawPath(path, paintIpad)
    paintIpad.xfermode = null
    paintIpad.style = Paint.Style.STROKE
    paintIpad.strokeWidth = minDim * 0.02f
    canvas.drawPath(path, paintIpad)
    saveBitmap(bmp, file, Constants.LINE_PROGRESS)

    paintIpad.xfermode = null
    canvas.drawColor(0, PorterDuff.Mode.CLEAR)
    canvas.drawPaint(paintIpad)
    saveBitmap(bmp, file, Constants.LINE_BG)

    return Pair(f6, round)
}

/**
 * Battery shape progress bitmap for export.
 * Creates battery body path with cap and lightning bolt cutout.
 * Uses PorterDuff CLEAR compositing for the cutout.
 *
 * Returns a Pair of (startX, Point(width, height)) for video compositing.
 *
 * Original: BlurredImageView.java line 2457
 */
fun BlurredImageView.saveProgressBitmapTypeBattery(file: File, bitmap: Bitmap?): Pair<Float, Point> {
    val bmp = Bitmap.createBitmap(
        bitmap!!.width,
        rectFProgress!!.height().toInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bmp)
    val width = rectFProgress!!.width()
    val height = rectFProgress!!.height()
    val centerX = rectFProgress!!.centerX()
    val height2 = canvas.height * 0.5f
    val minDim = min(width, height)
    val batteryWidth = 0.8f * minDim
    val batteryHeight = 0.45f * minDim
    val halfW = batteryWidth / 2.0f
    val left = centerX - halfW
    val halfH = batteryHeight / 2.0f
    val right = halfW + centerX
    val capWidth = 0.07f * batteryWidth + right
    val capHalf = 0.25f * batteryHeight

    val path = Path()
    val batteryRect = RectF(left, height2 - halfH, right, halfH + height2)
    val cornerRadius = 0.05f * minDim
    path.addRoundRect(batteryRect, cornerRadius, cornerRadius, Path.Direction.CW)
    path.addRect(RectF(right, height2 - capHalf, capWidth, capHalf + height2), Path.Direction.CW)

    // Lightning bolt path
    val boltPath = Path()
    val boltWidth = batteryWidth * 0.3f
    val boltArm = 0.3f * boltWidth
    val boltHalf = batteryHeight * 0.5f * 0.5f
    boltPath.moveTo(centerX - boltArm, height2 - boltHalf)
    boltPath.lineTo(centerX, height2)
    val boltInner = boltWidth * 0.2f
    boltPath.lineTo(centerX - boltInner, height2)
    boltPath.lineTo(boltArm + centerX, boltHalf + height2)
    boltPath.lineTo(centerX, height2)
    boltPath.lineTo(centerX + boltInner, height2)
    boltPath.close()

    canvas.drawColor(ViewCompat.MEASURED_STATE_MASK)

    val alpha = paintIpad.alpha
    val linearGradient2 = linearGradient_classic
    if (getColor_gradient() != null && linearGradient2 != null) {
        paintIpad.shader = linearGradient2
        paintIpad.color = getColor_gradient()!!.color
    } else {
        paintIpad.color = color_ipad
    }
    paintIpad.alpha = alpha

    paintIpad.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    paintIpad.style = Paint.Style.FILL
    canvas.drawPath(path, paintIpad)
    paintIpad.xfermode = null
    paintIpad.style = Paint.Style.STROKE
    paintIpad.strokeWidth = minDim * 0.025f
    canvas.drawPath(path, paintIpad)

    paintIpad.xfermode = null
    paintIpad.style = Paint.Style.FILL
    val shader = paintIpad.shader
    paintIpad.shader = null
    paintIpad.color = ViewCompat.MEASURED_STATE_MASK
    canvas.drawPath(boltPath, paintIpad)
    paintIpad.shader = shader
    saveBitmap(bmp, file, Constants.LINE_PROGRESS)

    // Background bitmap
    val bmp2 = Bitmap.createBitmap(
        ((capWidth - left) + paintIpad.strokeWidth * 0.5f).roundToInt(),
        bmp.height,
        Bitmap.Config.ARGB_8888
    )
    canvas.setBitmap(bmp2)

    val linearGradient = linearGradient_classic
    if (getColor_gradient() != null && linearGradient != null) {
        paintIpad.shader = linearGradient
        paintIpad.color = getColor_gradient()!!.color
    } else {
        paintIpad.color = color_ipad
    }
    canvas.drawPaint(paintIpad)
    saveBitmap(bmp2, file, Constants.LINE_BG)

    return Pair(left, Point(bmp2.width, bmp2.height))
}

/**
 * Save bitmap to file as PNG.
 * If file is null, uses the app's external files directory.
 *
 * Original: BlurredImageView.java line 2544
 */
fun BlurredImageView.saveBitmap(bitmap: Bitmap, file: File?, textValue: String) {
    var targetDir = file
    if (targetDir == null) {
        targetDir = context.getExternalFilesDir(null)
    }
    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(File(targetDir, textValue))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.flush()
        fos.close()
    } catch (e: IOException) {
        e.printStackTrace()
        if (fos != null) {
            try {
                fos.close()
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Entity Management
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Set progress value and trigger invalidation.
 *
 * Original: BlurredImageView.java line 2592
 */
fun BlurredImageView.setProgress(blurRadius: Float) {
    progress = blurRadius
    postInvalidate()
}

/**
 * Draw all visible quran and translation entities to canvas.
 *
 * Original: BlurredImageView.java line 2597
 */
fun BlurredImageView.drawEntity(canvas: Canvas) {
    for (i in getQuranEntities().indices) {
        val quranEntity = getQuranEntities()[i]
        if (quranEntity.isVisible && quranEntity.entityQuran?.visible() == true) {
            quranEntity.draw(canvas)
        }
    }
    for (i in getTranslationEntities().indices) {
        val translationQuranEntity = getTranslationEntities()[i]
        if (translationQuranEntity.isVisible && translationQuranEntity.entityTrslTimeline?.visible() == true) {
            translationQuranEntity.draw(canvas)
        }
    }
}

/**
 * Get rendered bitmap for export with type-specific bitmap selection,
 * gradient recoloring, cassette paths, surah scaling.
 *
 * Selects bitmapNotBlur for certain types (BLACK_LAYER, BLUE_TYPE, GRADIENT,
 * MASK_BRUSH, CASSET_IMG, IPAD_UNBLUR), otherwise uses bitmapBlured.
 *
 * Original: BlurredImageView.java line 3927
 */
fun BlurredImageView.getBitmapDraw(isFlag: Boolean, file: File?): Bitmap {
    val bitmap: Bitmap?
    if (mIpadType == IpadType.BLACK_LAYER.ordinal ||
        mIpadType == IpadType.BLUE_TYPE.ordinal ||
        mIpadType == IpadType.GRADIENT.ordinal ||
        mIpadType == IpadType.MASK_BRUSH.ordinal ||
        mIpadType == IpadType.CASSET_IMG.ordinal ||
        mIpadType == IpadType.IPAD_UNBLUR.ordinal
    ) {
        bitmap = bitmapNotBlur
    } else {
        bitmap = bitmapBlured
    }

    if (getColor_gradient() != null) {
        setColorIpad(getColor_gradient()!!)
    }

    val canvas = Canvas(bitmap!!)

    if (mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
        if (getColor_gradient() != null) {
            paint.shader = linearGradient_classic
            canvas.drawPaint(paint)
            paint.shader = null
        } else {
            canvas.drawColor(color_bg_type_classic)
        }
    }

    if (mIpadType == IpadType.CASSET.ordinal) {
        drawCaset(canvas, false, file)
    } else if (mIpadType == IpadType.CASSET_IMG.ordinal) {
        drawCasetNoBg(canvas, false, file)
    } else if (mIpadType == IpadType.CASSET_IMG_BLUR.ordinal) {
        bitmapSquare = bitmapBlured
        drawCasetNoBg(canvas, false, file)
    } else {
        drawIpad(canvas, false)
    }

    // Billing removed — no watermark for any user

    if (surahNameEntity != null) {
        surahNameEntity!!.rect = RectF(
            surahNameEntity!!.copyRect!!.left * canvas.width,
            surahNameEntity!!.copyRect!!.top * canvas.height,
            surahNameEntity!!.copyRect!!.right * canvas.width,
            surahNameEntity!!.copyRect!!.bottom * canvas.height
        )
        val sne = surahNameEntity!!
        sne.scale(sne.factorScale, 1, 1)
        surahNameEntity!!.draw(canvas)
    }

    return bitmap
}

/**
 * Find last visible quran entity.
 * Searches backwards with visibility and factorSize == 1.0f check.
 * Falls back to the last entity if none found with factorSize == 1.0f.
 *
 * Original: BlurredImageView.java line 3970
 */
fun BlurredImageView.getLastAdd(): QuranEntity {
    for (i in getQuranEntities().indices.reversed()) {
        val quranEntity = getQuranEntities()[i]
        if (quranEntity.entityQuran?.visible() == true && quranEntity.factorSize == 1.0f) {
            return quranEntity
        }
    }
    return getQuranEntities().last()
}

/**
 * Find last visible translation entity.
 * Searches backwards with visibility and factorSize == 1.0f check.
 * Falls back to the last entity if none found with factorSize == 1.0f.
 *
 * Original: BlurredImageView.java line 3980
 */
fun BlurredImageView.getLastAddTrsl(): TranslationQuranEntity {
    for (i in getTranslationEntities().indices.reversed()) {
        val translationQuranEntity = getTranslationEntities()[i]
        if (translationQuranEntity.entityTrslTimeline?.visible() == true && translationQuranEntity.factorSize == 1.0f) {
            return translationQuranEntity
        }
    }
    return getTranslationEntities().last()
}

/**
 * Complex counting logic for quran entities.
 * Returns 1 if only one entity, 2 if more than one visible, or the actual count.
 *
 * Original: BlurredImageView.java line 3990
 */
fun BlurredImageView.countEntityQuran(): Int {
    if (getQuranEntities().size == 1) {
        return 1
    }
    var count = 0
    for (i in getQuranEntities().indices) {
        if (getQuranEntities()[i].entityQuran?.visible() == true) {
            count++
        }
        if (count > 1) {
            return 2
        }
    }
    return count
}

/**
 * Complex counting logic for translation entities.
 * Returns 1 if only one entity, 2 if more than one visible, or the actual count.
 *
 * Original: BlurredImageView.java line 4006
 */
fun BlurredImageView.countEntityTrsl(): Int {
    if (getTranslationEntities().size == 1) {
        return 1
    }
    var count = 0
    for (i in getTranslationEntities().indices) {
        if (getTranslationEntities()[i].entityTrslTimeline?.visible() == true) {
            count++
        }
        if (count > 1) {
            return 2
        }
    }
    return count
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Text Sizing Methods
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Save-variant of aya sizing with canvas dimensions.
 * Used during export to set up entity sizes at the export resolution.
 *
 * Original: BlurredImageView.java line 4022
 */
fun BlurredImageView.updateSizeAyaSave(size: Int, index: Int) {
    val list = getQuranEntities()
    if (list.isNullOrEmpty()) return

    for (i in getQuranEntities().indices.reversed()) {
        val quranEntity = getQuranEntities()[i]
        if (quranEntity.entityQuran?.visible() == true) {
            quranEntity.setIpad_type(mIpadType)
            quranEntity.setCopyRect()
            val sizeF = size.toFloat()
            val indexF = index.toFloat()
            val rectF = RectF(
                quranEntity.copyRect!!.left * sizeF,
                quranEntity.copyRect!!.top * indexF,
                quranEntity.copyRect!!.right * sizeF,
                quranEntity.copyRect!!.bottom * indexF
            )
            quranEntity.update(rectF, (rectF.width() * 0.85f).toInt(), (rectF.height() * 0.85f).toInt())
            quranEntity.setupScaleSave(quranEntity.factorSize, size)
            quranEntity.initPreset(quranEntity.getmPreset())
        }
    }
}

/**
 * Save-variant of translation sizing with canvas dimensions.
 * Used during export to set up entity sizes at the export resolution.
 *
 * Original: BlurredImageView.java line 4042
 */
fun BlurredImageView.updateSizeTrslSave(size: Int, index: Int) {
    val list = getTranslationEntities()
    if (list.isNullOrEmpty()) return

    for (i in getTranslationEntities().indices.reversed()) {
        val translationQuranEntity = getTranslationEntities()[i]
        if (translationQuranEntity.entityTrslTimeline?.visible() == true) {
            translationQuranEntity.setIpad_type(mIpadType)
            translationQuranEntity.setCopyRect()
            val sizeF = size.toFloat()
            val indexF = index.toFloat()
            val rectF = RectF(
                translationQuranEntity.copyRect!!.left * sizeF,
                translationQuranEntity.copyRect!!.top * indexF,
                translationQuranEntity.copyRect!!.right * sizeF,
                translationQuranEntity.copyRect!!.bottom * indexF
            )
            translationQuranEntity.update(rectF, (rectF.width() * 0.85f).toInt(), (rectF.height() * 0.85f).toInt())
            translationQuranEntity.setupScaleSave(translationQuranEntity.factorSize, size)
            translationQuranEntity.initPreset(translationQuranEntity.getmPreset())
        }
    }
}

/**
 * Full intelligent text sizing for quran entities.
 *
 * Finds the widest entity, calculates text sizes, handles single vs multiple entities,
 * applies preset, and handles translation width optimization.
 *
 * Original: BlurredImageView.java line 4062 (70+ lines with complex logic)
 */
fun BlurredImageView.updateSizeAya() {
    val list = getQuranEntities()
    if (list.isNullOrEmpty()) return

    var lastAdd = getLastAdd()
    var quranEntityWithTrsl: QuranEntity? = if (lastAdd.getTranslation() != null) lastAdd else null
    var translationWidth = quranEntityWithTrsl?.getTranslationWidth() ?: 0.0f
    var hasTranslation = translationWidth != 0.0f

    // ── Single entity path ──
    if (countEntityQuran() == 1) {
        val calculateTextSize = lastAdd.calculateTextSize()
        val blurRadius = if (lastAdd.getTxt()!!.length < 9) 0.7f else 0.95f
        val textSize = calculateTextSize * blurRadius
        lastAdd.setTextSize(textSize)
        lastAdd.setFcSize(textSize / getmCanvas_width())
        lastAdd.setupScale(lastAdd.factorSize, getmCanvas_width(), getmCanvas_height())
        if (hasTranslation) {
            val optimalSize = lastAdd.calculateOptimalTextSize(
                (lastAdd.rect.width() * 0.85f).toInt(),
                (lastAdd.rect.height() * 0.5f * 0.83f).toInt()
            )
            lastAdd.updateTranslation(optimalSize)
            lastAdd.factorSizeTrl = optimalSize / getmCanvas_width()
        }
        lastAdd.initPreset(lastAdd.getmPreset())
        return
    }

    // ── Multiple entities path: find the widest entity ──
    var width = if (lastAdd.factorSize == 1.0f) lastAdd.getWidth() else -1.0f
    for (i in getQuranEntities().indices.reversed()) {
        val quranEntity2 = getQuranEntities()[i]
        if (quranEntity2.entityQuran?.visible() == true && quranEntity2.rect.width() == rectFAya!!.width()) {
            val width2 = quranEntity2.getWidth()
            if (width2 > width || width == -1.0f) {
                width = width2
                lastAdd = quranEntity2
            }
            if (quranEntity2.getTranslation() != null && quranEntity2.getTranslationWidth() > translationWidth) {
                translationWidth = quranEntity2.getTranslationWidth()
                hasTranslation = true
                quranEntityWithTrsl = quranEntity2
            }
        }
    }

    val calculateTextSize2 = lastAdd.calculateTextSize()

    // ── Apply calculated text size to all visible entities ──
    for (i in getQuranEntities().indices.reversed()) {
        val quranEntity3 = getQuranEntities()[i]
        if (quranEntity3.entityQuran?.visible() == true && quranEntity3.rect.width() == rectFAya!!.width()) {
            if (quranEntity3.factorSize == 1.0f) {
                quranEntity3.setFcSize(calculateTextSize2 / getmCanvas_width())
            }
            quranEntity3.setupScale(quranEntity3.factorSize, getmCanvas_width(), getmCanvas_height())
            quranEntity3.initPresetAya(quranEntity3.getmPreset())
        }
    }

    // ── Handle translation sizing ──
    if (hasTranslation && quranEntityWithTrsl != null) {
        val optimalSize = quranEntityWithTrsl.calculateOptimalTextSize(
            (quranEntityWithTrsl.rect.width() * 0.85f).toInt(),
            (quranEntityWithTrsl.rect.height() * 0.5f * 0.83f).toInt()
        )
        for (i in getQuranEntities().indices) {
            val quranEntity4 = getQuranEntities()[i]
            if (quranEntity4.entityQuran?.visible() == true && quranEntity4.getTranslation() != null) {
                quranEntity4.updateTranslation(optimalSize)
                quranEntity4.factorSizeTrl = optimalSize / getmCanvas_width()
                quranEntity4.initPresetTrsl(quranEntity4.getmPreset())
            }
        }
    }
}

/**
 * Full intelligent text sizing for translation entities.
 * Handles single vs multiple entity logic with text size calculation.
 *
 * Original: BlurredImageView.java line 4133
 */
fun BlurredImageView.updateSizeAyaTrsl() {
    val list = getTranslationEntities()
    if (list.isNullOrEmpty()) return

    var lastAddTrsl = getLastAddTrsl()

    // ── Single entity path ──
    if (countEntityTrsl() == 1) {
        val calculateTextSize = lastAddTrsl.calculateTextSize()
        val blurRadius = if (lastAddTrsl.getTxt()!!.length < 9) 0.7f else 0.95f
        val textSize = calculateTextSize * blurRadius
        lastAddTrsl.setTextSize(textSize)
        lastAddTrsl.setFcSize(textSize / getmCanvas_width())
        lastAddTrsl.setupScale(lastAddTrsl.factorSize, getmCanvas_width(), getmCanvas_height())
        lastAddTrsl.initPreset(lastAddTrsl.getmPreset())
        return
    }

    // ── Multiple entities path: find widest ──
    var width = if (lastAddTrsl.factorSize == 1.0f) lastAddTrsl.getWidth() else -1.0f
    for (i in getTranslationEntities().indices.reversed()) {
        val translationQuranEntity = getTranslationEntities()[i]
        if (translationQuranEntity.entityTrslTimeline?.visible() == true) {
            val width2 = translationQuranEntity.getWidth()
            if (width2 > width || width == -1.0f) {
                lastAddTrsl = translationQuranEntity
                width = width2
            }
        }
    }

    val calculateTextSize2 = lastAddTrsl.calculateTextSize()

    // ── Apply calculated text size to all visible entities ──
    for (i in getTranslationEntities().indices.reversed()) {
        val translationQuranEntity2 = getTranslationEntities()[i]
        if (translationQuranEntity2.entityTrslTimeline?.visible() == true) {
            if (translationQuranEntity2.factorSize == 1.0f) {
                translationQuranEntity2.setFcSize(calculateTextSize2 / getmCanvas_width())
            }
            translationQuranEntity2.setupScale(translationQuranEntity2.factorSize, getmCanvas_width(), getmCanvas_height())
            translationQuranEntity2.initPresetAya(translationQuranEntity2.getmPreset())
        }
    }
}

/**
 * Resize variant of aya sizing.
 * Resets factors, updates rectFAya for all entities, then applies sizing.
 *
 * Original: BlurredImageView.java line 4180
 */
fun BlurredImageView.updateSizeAyaResize() {
    val list = getQuranEntities()
    if (list.isNullOrEmpty()) return

    var lastAdd = getLastAdd()
    lastAdd.setIpad_type(mIpadType)
    lastAdd.setCanvasWH(getmCanvas_width(), getmCanvas_height())
    lastAdd.setFactor_scale(1.0f)
    lastAdd.setFcSize(1.0f)
    lastAdd.factorSizeTrl = 1.0f

    val rectF = rectFAya!!
    lastAdd.update(rectF, (rectF.width() * 0.85f).toInt(), (rectFAya!!.height() * 0.85f).toInt())

    var quranEntityWithTrsl: QuranEntity? = if (lastAdd.getTranslation() != null) lastAdd else null
    var translationWidth = quranEntityWithTrsl?.getTranslationWidth() ?: 0.0f
    var hasTranslation = translationWidth != 0.0f
    var width = lastAdd.getWidth()

    // ── Single entity path ──
    if (countEntityQuran() == 1) {
        val calculateTextSize = lastAdd.calculateTextSize()
        val blurRadius = if (lastAdd.getTxt()!!.length < 9) 0.7f else 0.95f
        val textSize = calculateTextSize * blurRadius
        lastAdd.setTextSize(textSize)
        lastAdd.setFcSize(textSize / getmCanvas_width())
        lastAdd.setupScale(lastAdd.factorSize, getmCanvas_width(), getmCanvas_height())
        if (hasTranslation) {
            val optimalSize = lastAdd.calculateOptimalTextSize(
                (lastAdd.rect.width() * 0.85f).toInt(),
                (lastAdd.rect.height() * 0.5f * 0.83f).toInt()
            )
            lastAdd.updateTranslation(optimalSize)
            lastAdd.factorSizeTrl = optimalSize / getmCanvas_width()
        }
        lastAdd.initPreset(lastAdd.getmPreset())
        return
    }

    // ── Multiple entities: find widest ──
    for (i in getQuranEntities().indices.reversed()) {
        val quranEntity2 = getQuranEntities()[i]
        if (quranEntity2.entityQuran?.visible() == true) {
            quranEntity2.setCanvasWH(getmCanvas_width(), getmCanvas_height())
            quranEntity2.setIpad_type(mIpadType)
            quranEntity2.update(rectFAya!!, lastAdd.max_w, lastAdd.max_h)
            val width2 = quranEntity2.getWidth()
            if (width2 > width) {
                width = width2
                lastAdd = quranEntity2
            }
            if (quranEntity2.getTranslation() != null && quranEntity2.getTranslationWidth() > translationWidth) {
                translationWidth = quranEntity2.getTranslationWidth()
                hasTranslation = true
                quranEntityWithTrsl = quranEntity2
            }
        }
    }

    val calculateTextSize2 = lastAdd.calculateTextSize()

    // ── Apply calculated text size to all visible entities ──
    for (i in getQuranEntities().indices.reversed()) {
        val quranEntity3 = getQuranEntities()[i]
        if (quranEntity3.entityQuran?.visible() == true) {
            quranEntity3.setFactor_scale(1.0f)
            quranEntity3.factorSizeTrl = 1.0f
            quranEntity3.setFcSize(calculateTextSize2 / getmCanvas_width())
            quranEntity3.setupScale(quranEntity3.factorSize, getmCanvas_width(), getmCanvas_height())
            quranEntity3.initPresetAya(quranEntity3.getmPreset())
        }
    }

    // ── Handle translation sizing ──
    if (hasTranslation && quranEntityWithTrsl != null) {
        val optimalSize = quranEntityWithTrsl.calculateOptimalTextSize(
            (quranEntityWithTrsl.rect.width() * 0.85f).toInt(),
            (quranEntityWithTrsl.rect.height() * 0.5f * 0.83f).toInt()
        )
        for (i in getQuranEntities().indices) {
            val quranEntity4 = getQuranEntities()[i]
            if (quranEntity4.entityQuran?.visible() == true && quranEntity4.getTranslation() != null) {
                quranEntity4.updateTranslation(optimalSize)
                quranEntity4.factorSizeTrl = optimalSize / getmCanvas_width()
                quranEntity4.initPresetTrsl(quranEntity4.getmPreset())
            }
        }
    }
}

/**
 * Resize variant of translation sizing.
 * Resets factors, updates rectFAya for all translation entities, then applies sizing.
 *
 * Original: BlurredImageView.java line 4261
 */
fun BlurredImageView.updateSizeTrslAyaResize() {
    val list = getTranslationEntities()
    if (list.isNullOrEmpty()) return

    var lastAddTrsl = getLastAddTrsl()
    lastAddTrsl.setIpad_type(mIpadType)
    lastAddTrsl.setCanvasWH(getmCanvas_width(), getmCanvas_height())
    lastAddTrsl.setFactor_scale(1.0f)
    lastAddTrsl.setFcSize(1.0f)
    lastAddTrsl.factorSizeTrl = 1.0f

    val rectF = rectFAya!!
    lastAddTrsl.onResize(rectF, (rectF.width() * 0.85f).toInt(), (rectFAya!!.height() * 0.85f).toInt())
    var width = lastAddTrsl.getWidth()

    // ── Single entity path ──
    if (countEntityTrsl() == 1) {
        val calculateTextSize = lastAddTrsl.calculateTextSize()
        val blurRadius = if (lastAddTrsl.getTxt()!!.length < 9) 0.7f else 0.95f
        val textSize = calculateTextSize * blurRadius
        lastAddTrsl.setTextSize(textSize)
        lastAddTrsl.setFcSize(textSize / getmCanvas_width())
        lastAddTrsl.setupScale(lastAddTrsl.factorSize, getmCanvas_width(), getmCanvas_height())
        lastAddTrsl.initPreset(lastAddTrsl.getmPreset())
        return
    }

    // ── Multiple entities: find widest ──
    for (i in getTranslationEntities().indices.reversed()) {
        val translationQuranEntity = getTranslationEntities()[i]
        if (translationQuranEntity.entityTrslTimeline?.visible() == true) {
            translationQuranEntity.setCanvasWH(getmCanvas_width(), getmCanvas_height())
            translationQuranEntity.setIpad_type(mIpadType)
            translationQuranEntity.onResize(rectFAya!!, lastAddTrsl.max_w, lastAddTrsl.max_h)
            val width2 = translationQuranEntity.getWidth()
            if (width2 > width) {
                lastAddTrsl = translationQuranEntity
                width = width2
            }
        }
    }

    val calculateTextSize2 = lastAddTrsl.calculateTextSize()

    // ── Apply calculated text size to all visible entities ──
    for (i in getTranslationEntities().indices.reversed()) {
        val translationQuranEntity2 = getTranslationEntities()[i]
        if (translationQuranEntity2.entityTrslTimeline?.visible() == true) {
            translationQuranEntity2.setFactor_scale(1.0f)
            translationQuranEntity2.factorSizeTrl = 1.0f
            translationQuranEntity2.setFcSize(calculateTextSize2 / getmCanvas_width())
            translationQuranEntity2.setupScale(translationQuranEntity2.factorSize, getmCanvas_width(), getmCanvas_height())
            translationQuranEntity2.initPresetAya(translationQuranEntity2.getmPreset())
        }
    }
}

/**
 * Complex RTL/LTR alignment handling and rect repositioning for each IpadType.
 *
 * For neumorphic/cassette types, forces center alignment.
 * For non-Arabic text (LTR), aligns to the left with type-specific positioning.
 * For Arabic text (RTL), aligns to the right with type-specific positioning.
 *
 * The positioning logic varies significantly based on IpadType:
 * - IPAD/IPAD_UNBLUR/IPAD_CLASSIC: uses bitmapSquare center offset
 * - BOTTOM_RECT/BLACK_LAYER/BLUE_TYPE/GRADIENT/MASK_BRUSH/HEART/BATTERY:
 *   uses ipad_rect width with small margin
 * - Other types: uses ipad_rect with 7% margin
 *
 * Original: BlurredImageView.java line 4318 (75 lines!)
 */
fun BlurredImageView.updatePosSurahName() {
    if (surahNameEntity == null) return

    // ── Center alignment for neumorphic/cassette types ──
    if (mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal ||
        mIpadType == IpadType.CASSET.ordinal ||
        mIpadType == IpadType.CASSET_IMG.ordinal ||
        mIpadType == IpadType.CASSET_IMG_BLUR.ordinal
    ) {
        surahNameEntity!!.setAlignment(Layout.Alignment.ALIGN_CENTER)
    } else if (!Utils.isProbablyLArabic(surahNameEntity!!.reader)) {
        // ── LTR (non-Arabic): align left ──
        if (mIpadType == IpadType.IPAD.ordinal ||
            mIpadType == IpadType.IPAD_UNBLUR.ordinal ||
            mIpadType == IpadType.IPAD_CLASSIC.ordinal
        ) {
            val width5 = rectFSurahName!!.width()
            left_square = ipad_rect!!.centerX() - bitmapSquare!!.width * 0.5f
            if (mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
                rectFSurahName!!.left = left_square
            } else {
                rectFSurahName!!.left = ipad_rect!!.width() * 0.05f + left_square
            }
            rectFSurahName!!.right = rectFSurahName!!.left + width5
        } else {
            var width3 = 0f
            if (mIpadType == IpadType.BOTTOM_RECT.ordinal) {
                width3 = ipad_rect!!.width()
            } else if (mIpadType == IpadType.BLACK_LAYER.ordinal ||
                mIpadType == IpadType.BLUE_TYPE.ordinal ||
                mIpadType == IpadType.GRADIENT.ordinal ||
                mIpadType == IpadType.MASK_BRUSH.ordinal ||
                mIpadType == IpadType.HEART.ordinal ||
                mIpadType == IpadType.BATTERY.ordinal
            ) {
                width3 = ipad_rect!!.width()
            } else {
                val width4 = ipad_rect!!.width() * 0.07f
                val width6 = rectFSurahName!!.width()
                rectFSurahName!!.left = width4 + ipad_rect!!.left
                rectFSurahName!!.right = rectFSurahName!!.left + width6
            }
            val width4 = width3 * 0.015f
            val width62 = rectFSurahName!!.width()
            rectFSurahName!!.left = width4 + ipad_rect!!.left
            rectFSurahName!!.right = rectFSurahName!!.left + width62
        }
        surahNameEntity!!.setAlignment(Layout.Alignment.ALIGN_NORMAL)
    } else {
        // ── RTL (Arabic): align right ──
        if (mIpadType == IpadType.IPAD.ordinal ||
            mIpadType == IpadType.IPAD_UNBLUR.ordinal ||
            mIpadType == IpadType.IPAD_CLASSIC.ordinal
        ) {
            val width7 = rectFSurahName!!.width()
            left_square = ipad_rect!!.centerX() - bitmapSquare!!.width * 0.5f
            if (mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
                rectFSurahName!!.right = left_square + bitmapSquare!!.width
            } else {
                rectFSurahName!!.right = left_square + bitmapSquare!!.width - ipad_rect!!.width() * 0.05f
            }
            rectFSurahName!!.left = rectFSurahName!!.right - width7
        } else {
            var width = 0f
            if (mIpadType == IpadType.BOTTOM_RECT.ordinal) {
                width = ipad_rect!!.width()
            } else if (mIpadType == IpadType.BLACK_LAYER.ordinal ||
                mIpadType == IpadType.BLUE_TYPE.ordinal ||
                mIpadType == IpadType.GRADIENT.ordinal ||
                mIpadType == IpadType.MASK_BRUSH.ordinal ||
                mIpadType == IpadType.HEART.ordinal ||
                mIpadType == IpadType.BATTERY.ordinal
            ) {
                width = ipad_rect!!.width()
            } else {
                val width2 = ipad_rect!!.width() * 0.07f
                val width8 = rectFSurahName!!.width()
                rectFSurahName!!.right = ipad_rect!!.right - width2
                rectFSurahName!!.left = rectFSurahName!!.right - width8
            }
            val width2 = width * 0.015f
            val width82 = rectFSurahName!!.width()
            rectFSurahName!!.right = ipad_rect!!.right - width2
            rectFSurahName!!.left = rectFSurahName!!.right - width82
        }
        surahNameEntity!!.setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
    }

    surahNameEntity!!.ipad_type = mIpadType
    surahNameEntity!!.setFactor_scale(1.0f)
    surahNameEntity!!.update(rectFSurahName!!)
}

/**
 * No-arg version that resets bismilah entity and isti3adha entity.
 * Uses rectFAya and canvas dimensions from the view.
 *
 * Original: BlurredImageView.java line 4393
 */
fun BlurredImageView.updateBismilahEntity() {
    val bismilah = bismilahEntity
    if (bismilah != null && bismilah.getBismilahTimeline()?.visible() == true) {
        bismilahEntity!!.setCanvasWH(getmCanvas_width(), getmCanvas_height())
        bismilahEntity!!.setFactor_scale(1.0f)
        bismilahEntity!!.setFcSize(1.0f)
        val rectF = rectFAya!!
        bismilahEntity!!.update(rectF, (rectF.width() * 0.85f).toInt(), (rectFAya!!.height() * 0.85f).toInt())
        bismilahEntity!!.createStaticLayout()
        bismilahEntity!!.initPreset(bismilahEntity!!.getmPreset())
        bismilahEntity!!.setFcSize(bismilahEntity!!.getPaintAya().textSize / getmCanvas_width())
    }

    val isti3adha = mIsti3adhaEntity
    if (isti3adha == null || isti3adha.getBismilahTimeline()?.visible() != true) {
        return
    }

    mIsti3adhaEntity!!.setCanvasWH(getmCanvas_width(), getmCanvas_height())
    mIsti3adhaEntity!!.setFactor_scale(1.0f)
    mIsti3adhaEntity!!.setFcSize(1.0f)
    val rectF2 = rectFAya!!
    mIsti3adhaEntity!!.update(rectF2, (rectF2.width() * 0.85f).toInt(), (rectFAya!!.height() * 0.85f).toInt())
    mIsti3adhaEntity!!.createStaticLayout()
    mIsti3adhaEntity!!.initPreset(mIsti3adhaEntity!!.getmPreset())
    mIsti3adhaEntity!!.setFcSize(mIsti3adhaEntity!!.getPaintAya().textSize / getmCanvas_width())
}

/**
 * With canvas dimensions — used during export.
 * Sets copyRect, creates scaled rect, updates entity, and applies preset.
 *
 * Original: BlurredImageView.java line 4425
 */
fun BlurredImageView.updateBismilahEntity(size: Int, ayaNumber: Int) {
    val bismilah = bismilahEntity
    if (bismilah != null && bismilah.getBismilahTimeline()?.visible() == true) {
        bismilahEntity!!.setCopyRect()
        val sizeF = size.toFloat()
        val ayaNumberF = ayaNumber.toFloat()
        val rectF = RectF(
            bismilahEntity!!.copyRect!!.left * sizeF,
            bismilahEntity!!.copyRect!!.top * ayaNumberF,
            bismilahEntity!!.copyRect!!.right * sizeF,
            bismilahEntity!!.copyRect!!.bottom * ayaNumberF
        )
        bismilahEntity!!.update(rectF, (rectF.width() * 0.85f).toInt(), (rectF.height() * 0.85f).toInt())
        bismilahEntity!!.setupScaleSave(bismilahEntity!!.factorSize, size)
        bismilahEntity!!.initPreset(bismilahEntity!!.getmPreset())
    }

    val isti3adha = mIsti3adhaEntity
    if (isti3adha == null || isti3adha.getBismilahTimeline()?.visible() != true) {
        return
    }

    mIsti3adhaEntity!!.setCopyRect()
    val sizeF2 = size.toFloat()
    val ayaNumberF2 = ayaNumber.toFloat()
    val rectF2 = RectF(
        mIsti3adhaEntity!!.copyRect!!.left * sizeF2,
        mIsti3adhaEntity!!.copyRect!!.top * ayaNumberF2,
        mIsti3adhaEntity!!.copyRect!!.right * sizeF2,
        mIsti3adhaEntity!!.copyRect!!.bottom * ayaNumberF2
    )
    mIsti3adhaEntity!!.update(rectF2, (rectF2.width() * 0.85f).toInt(), (rectF2.height() * 0.85f).toInt())
    mIsti3adhaEntity!!.setupScaleSave(mIsti3adhaEntity!!.factorSize, size)
    mIsti3adhaEntity!!.initPreset(mIsti3adhaEntity!!.getmPreset())
}

/**
 * Full entity resize with surahNameEntity canvasWH.
 * Delegates to updateSizeAyaResize, updateSizeTrslAyaResize, and updateBismilahEntity.
 *
 * Original: BlurredImageView.java line 4453
 */
fun BlurredImageView.resizeEntity() {
    val sne = surahNameEntity
    if (sne != null) {
        sne.setCanvasWH(getmCanvas_width(), getmCanvas_height())
    }
    updateSizeAyaResize()
    updateSizeTrslAyaResize()
    updateBismilahEntity()
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Surah Name Entity Setup
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Complex surah name entity with alignment repositioning, font loading, color scheme handling.
 *
 * Determines alignment based on IpadType and language direction (RTL/LTR),
 * repositions rectFSurahName based on type-specific logic, loads fonts,
 * handles color scheme for cassette types, and creates or updates the SurahNameEntity.
 *
 * Original: BlurredImageView.java line 3669 (105 lines!)
 */
fun BlurredImageView.setSurahNameEntity(
    textValue: String,
    textValue2: String,
    rectF: RectF?,
    blurRadius: Float,
    name3: String,
    size: Int,
    surahNumber: Int,
    size3: Int,
    size4: Int,
    isFlag: Boolean,
    surahNumber242: Int
) {
    val surahName = textValue2.ifEmpty { " " }

    // ── Determine alignment based on IpadType and language ──
    val alignment: Layout.Alignment
    if (mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal ||
        mIpadType == IpadType.CASSET.ordinal ||
        mIpadType == IpadType.CASSET_IMG.ordinal ||
        mIpadType == IpadType.CASSET_IMG_BLUR.ordinal
    ) {
        alignment = Layout.Alignment.ALIGN_CENTER
    } else if (!Utils.isProbablyLArabic(surahName)) {
        alignment = Layout.Alignment.ALIGN_NORMAL

        // ── Reposition for LTR (non-ZAGHRAFAT) ──
        if (size3 != SurahNameStyle.ZAGHRAFAT.ordinal) {
            if (mIpadType == IpadType.IPAD.ordinal ||
                mIpadType == IpadType.IPAD_UNBLUR.ordinal ||
                mIpadType == IpadType.IPAD_CLASSIC.ordinal
            ) {
                val width5 = rectFSurahName!!.width()
                left_square = ipad_rect!!.centerX() - bitmapSquare!!.width * 0.5f
                if (mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
                    rectFSurahName!!.left = left_square
                } else {
                    rectFSurahName!!.left = ipad_rect!!.width() * 0.05f + left_square
                }
                rectFSurahName!!.right = rectFSurahName!!.left + width5
            } else {
                var width3 = 0f
                if (mIpadType == IpadType.BOTTOM_RECT.ordinal) {
                    width3 = ipad_rect!!.width()
                } else if (mIpadType == IpadType.BLACK_LAYER.ordinal ||
                    mIpadType == IpadType.BLUE_TYPE.ordinal ||
                    mIpadType == IpadType.GRADIENT.ordinal ||
                    mIpadType == IpadType.MASK_BRUSH.ordinal ||
                    mIpadType == IpadType.HEART.ordinal ||
                    mIpadType == IpadType.BATTERY.ordinal
                ) {
                    width3 = ipad_rect!!.width()
                } else {
                    val width4 = ipad_rect!!.width() * 0.07f
                    val width6 = rectFSurahName!!.width()
                    rectFSurahName!!.left = width4 + ipad_rect!!.left
                    rectFSurahName!!.right = rectFSurahName!!.left + width6
                }
                val width4 = width3 * 0.015f
                val width62 = rectFSurahName!!.width()
                rectFSurahName!!.left = width4 + ipad_rect!!.left
                rectFSurahName!!.right = rectFSurahName!!.left + width62
            }
        }
    } else {
        alignment = Layout.Alignment.ALIGN_OPPOSITE

        // ── Reposition for RTL (non-ZAGHRAFAT) ──
        if (size3 != SurahNameStyle.ZAGHRAFAT.ordinal) {
            if (mIpadType == IpadType.IPAD.ordinal ||
                mIpadType == IpadType.IPAD_UNBLUR.ordinal ||
                mIpadType == IpadType.IPAD_CLASSIC.ordinal
            ) {
                val width7 = rectFSurahName!!.width()
                left_square = ipad_rect!!.centerX() - bitmapSquare!!.width * 0.5f
                if (mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
                    rectFSurahName!!.right = left_square + bitmapSquare!!.width
                } else {
                    rectFSurahName!!.right = left_square + bitmapSquare!!.width - ipad_rect!!.width() * 0.05f
                }
                rectFSurahName!!.left = rectFSurahName!!.right - width7
            } else {
                var width = 0f
                if (mIpadType == IpadType.BOTTOM_RECT.ordinal) {
                    width = ipad_rect!!.width()
                } else if (mIpadType == IpadType.BLACK_LAYER.ordinal ||
                    mIpadType == IpadType.BLUE_TYPE.ordinal ||
                    mIpadType == IpadType.GRADIENT.ordinal ||
                    mIpadType == IpadType.MASK_BRUSH.ordinal ||
                    mIpadType == IpadType.HEART.ordinal ||
                    mIpadType == IpadType.BATTERY.ordinal
                ) {
                    width = ipad_rect!!.width()
                } else {
                    val width2 = ipad_rect!!.width() * 0.07f
                    val width8 = rectFSurahName!!.width()
                    rectFSurahName!!.right = ipad_rect!!.right - width2
                    rectFSurahName!!.left = rectFSurahName!!.right - width8
                }
                val width2 = width * 0.015f
                val width82 = rectFSurahName!!.width()
                rectFSurahName!!.right = ipad_rect!!.right - width2
                rectFSurahName!!.left = rectFSurahName!!.right - width82
            }
        }
    }

    // ── Create or update SurahNameEntity ──
    val existing = surahNameEntity
    if (existing == null) {
        // Load fonts
        val typeface = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/$name3")!!
        val styleTypeface = UtilsFileLast.loadFontFromAsset(context, "fonts/surah_name.otf")

        // Determine color
        val color: Int = if (size == 0) {
            val lectureColor = paintLecture.color
            if (getmIpadType() == IpadType.CASSET.ordinal ||
                getmIpadType() == IpadType.CASSET_IMG.ordinal ||
                mIpadType == IpadType.CASSET_IMG_BLUR.ordinal
            ) {
                if (ColorUtils.isColorDark(scheme!!.body)) -1 else ViewCompat.MEASURED_STATE_MASK
            } else {
                lectureColor
            }
        } else {
            size
        }

        if (rectF != null) {
            rectFSurahName = rectF
        }

        val entity = SurahNameEntity(
            alignment, textValue, surahName, rectFSurahName!!,
            typeface, color, blurRadius, name3,
            surahNumber, styleTypeface, size3, size4,
            mIpadType, isFlag, color
        )
        surahNameEntity = entity
        entity.setCanvasWH(getmCanvas_width(), getmCanvas_height())

        if (rectF != null) {
            entity.move()
        }
    } else {
        existing.index_surah = size4
        surahNameEntity!!.rect = rectFSurahName!!
        surahNameEntity!!.setNameAndReader(alignment, textValue, surahName)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  Bitmap Setup and Background Save
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Setup bitmaps for drawing — prepares frame interval, creates rects,
 * draws entity bitmaps, saves progress bitmaps per type, and saves background.
 *
 * Returns the background file path.
 *
 * Original: BlurredImageView.java line 3839
 */
fun BlurredImageView.setupBitmapDraw(bitmap: Bitmap, bitmap2: Bitmap, template: Template): String {
    frameInterval = 1000L / template.fps
    bitmapBlured = bitmap
    bitmapSquare = bitmap2

    val sne = surahNameEntity
    if (sne != null) {
        sne.setCopyRect()
    }
    createRect()

    val bgFileName = "bg_${System.currentTimeMillis()}.png"
    val file = File(template.folder_template!!)
    val bitmapDraw = getBitmapDraw(template.isVideoSquare, file)

    FontUtils.copyFontToInternalStorage(context, Constants.FONT_NUMBER)

    var strokeWidth = linePaint.strokeWidth * 4.2f
    var blurRadius = 0.0f
    if (template.ipad_type == IpadType.BLACK_LAYER.ordinal ||
        template.ipad_type == IpadType.BLUE_TYPE.ordinal ||
        template.ipad_type == IpadType.GRADIENT.ordinal ||
        template.ipad_type == IpadType.MASK_BRUSH.ordinal ||
        template.ipad_type == IpadType.HEART.ordinal ||
        mIpadType == IpadType.BATTERY.ordinal
    ) {
        strokeWidth = 0.0f
    }

    var size3 = 0
    var size4 = 0

    if (template.ipad_type == IpadType.BLUE_TYPE.ordinal) {
        saveProgressBitmapTypeBlue(file)
    } else if (template.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
        saveProgressBitmapTypeIPAD_NEOMORPHIC(file, bitmapDraw)
    } else if (template.ipad_type == IpadType.HEART.ordinal) {
        val result = saveProgressBitmapTypeHeart(file, bitmapDraw)
        blurRadius = result.first
        size3 = result.second
    } else if (mIpadType == IpadType.BATTERY.ordinal) {
        val result = saveProgressBitmapTypeBattery(file, bitmapDraw)
        blurRadius = result.first
        size4 = result.second.x
        size3 = result.second.y
    } else if (mIpadType == IpadType.CASSET.ordinal ||
        mIpadType == IpadType.CASSET_IMG.ordinal ||
        mIpadType == IpadType.CASSET_IMG_BLUR.ordinal
    ) {
        blurRadius = rectFProgress!!.left
        size3 = rectFProgress!!.top.toInt()
        size4 = rectFProgress!!.right.toInt()
    } else {
        saveProgressBitmap(file, strokeWidth)
    }

    drawEntityBitmap(file, bitmapDraw.width, bitmapDraw.height)
    saveBg(bgFileName, bitmapDraw, file)

    val timeModel = template.mTimeModel
    val round = (strokeWidth * 1.98f).roundToInt()
    val tm: TimeModel = if (timeModel != null) {
        timeModel.color = if (paintText.color != -1) "black" else "white"
        timeModel.posXRight = newLeft_txt
        timeModel.posY = txt_y
        timeModel.height_bitmap_progress = (rectFProgress!!.height() * 1.5f).toInt()
        timeModel.width_bitmap_progress = rectFProgress!!.width().toInt()
        timeModel.size = paintText.textSize * 0.96f
        timeModel.progress_offset = round
        timeModel
    } else {
        TimeModel(
            rectFProgress!!.width().toInt(),
            (rectFProgress!!.height() * 1.5f).toInt(),
            paintText.textSize * 0.96f,
            if (paintText.color != -1) "black" else "white",
            txt_y,
            newLeft_txt,
            round
        )
    }

    tm.startShape = blurRadius
    tm.widthShape = size4
    tm.heightShape = size3
    template.mTimeModel = tm

    return file.absolutePath + "/" + bgFileName
}

/**
 * Save background bitmap to file.
 *
 * Original: BlurredImageView.java line 3923
 */
fun BlurredImageView.saveBg(textValue: String, bitmap: Bitmap, file: File) {
    saveBitmap(bitmap, file, textValue)
}
