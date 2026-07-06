package hazem.nurmontage.videoquran.views.blurred

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.utils.ColorSchemeGenerator
import hazem.nurmontage.videoquran.utils.ColorUtils
import hazem.nurmontage.videoquran.utils.CreateGradient
import hazem.nurmontage.videoquran.views.BlurredImageView
import java.io.File
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════════
//  Extension functions for BlurredImageView that handle all IpadType-specific
//  rendering. Each method faithfully reproduces the original Java logic from
//  BlurredImageView.java lines 2764–3635.
//
//  DO NOT SIMPLIFY the rendering code. All BlurMaskFilter shadows, glass
//  effects, gradient overlays, border strokes, HSV manipulations, ShadowLayer
//  passes, cassette gear animations, clipPath shaping, RadialGradient,
//  PorterDuff compositing, etc. must be preserved exactly.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Draws a rectangle with BlurMaskFilter shadow, glass effect, gradient overlays,
 * and border strokes. When [isRounded] is true, draws a round-rect; otherwise
 * a plain rect.
 *
 * Original: BlurredImageView.java lines 2764–2840
 */
fun BlurredImageView.drawRectWithShadow(
    canvas: Canvas,
    rect: RectF,
    color: Int,
    shadowRadius: Int,
    offsetX: Int,
    offsetY: Int,
    isRounded: Boolean
) {
    if (isGlass()) {
        val cornerRadius = min(rect.width(), rect.height()) * 0.14f

        // ── Pass 1: BlurMaskFilter shadow ──
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            setColor(color)
            maskFilter = BlurMaskFilter(shadowRadius.toFloat(), BlurMaskFilter.Blur.OUTER)
            alpha = 80
        }
        val shadowPath = Path()
        if (isRounded) {
            shadowPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        } else {
            shadowPath.addRect(rect, Path.Direction.CW)
        }
        shadowPath.offset(offsetX.toFloat(), offsetY.toFloat())
        canvas.drawPath(shadowPath, shadowPaint)

        // ── Pass 2: Gradient fill (glass effect) ──
        val gradient = this.color_gradient
        val baseColor: Int
        if (gradient != null) {
            baseColor = gradient.color
            val argb = Color.argb(70, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            this.paintIpad.shader = CreateGradient.createLinearGradientWithAngle(
                this.ipad_rect!!,
                this.color_gradient!!.angle.toFloat(),
                intArrayOf(this.color_gradient!!.color, this.color_gradient!!.second, this.color_gradient!!.three),
                floatArrayOf(0.0f, 0.7f, 1.0f)
            )
            this.paintIpad.color = argb
        } else {
            baseColor = this.color_ipad
            this.paintIpad.color = Color.argb(60, Color.red(baseColor), Color.green(this.color_ipad), Color.blue(this.color_ipad))
        }
        this.paintIpad.style = Paint.Style.FILL
        if (isRounded) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, this.paintIpad)
        } else {
            canvas.drawRect(rect, this.paintIpad)
        }

        // ── Pass 3: Border stroke (lightened edge) ──
        this.paintIpad.style = Paint.Style.STROKE
        this.paintIpad.strokeWidth = rect.height() * 0.003f
        this.paintIpad.color = Color.argb(
            120,
            min(255, Color.red(baseColor) + 40),
            min(255, Color.green(baseColor) + 40),
            min(255, Color.blue(baseColor) + 40)
        )
        if (isRounded) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, this.paintIpad)
        } else {
            canvas.drawRect(rect, this.paintIpad)
        }

        // ── Pass 4: Glass reflection gradient overlay ──
        this.paintIpad.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(Color.argb(140, 255, 255, 255), Color.argb(10, 255, 255, 255)),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        this.paintIpad.style = Paint.Style.FILL
        if (isRounded) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, this.paintIpad)
        } else {
            canvas.drawRect(rect, this.paintIpad)
        }

        // ── Restore paintIpad state ──
        this.paintIpad.shader = if (this.color_gradient != null) this.linearGradient_classic else null
        this.paintIpad.color = this.color_ipad
        this.paintIpad.alpha = 190
        return
    }

    // ── Non-glass path: simple shadow + fill ──
    val shadowPaint2 = Paint().apply {
        isAntiAlias = true
        setColor(color)
        maskFilter = BlurMaskFilter(shadowRadius.toFloat(), BlurMaskFilter.Blur.OUTER)
        alpha = 80
    }
    if (isRounded) {
        val cornerRadius2 = min(rect.width(), rect.height()) * 0.14f
        val shadowPath2 = Path()
        shadowPath2.addRoundRect(rect, cornerRadius2, cornerRadius2, Path.Direction.CW)
        shadowPath2.offset(offsetX.toFloat(), offsetY.toFloat())
        canvas.drawPath(shadowPath2, shadowPaint2)
        canvas.drawRoundRect(rect, cornerRadius2, cornerRadius2, this.paintIpad)
        return
    }
    val shadowPath3 = Path()
    shadowPath3.addRect(rect, Path.Direction.CW)
    shadowPath3.offset(offsetX.toFloat(), offsetY.toFloat())
    canvas.drawPath(shadowPath3, shadowPaint2)
    canvas.drawRect(rect, this.paintIpad)
}

/**
 * Draws bottom rectangle with optional glass effect (gradient + border + reflection).
 *
 * Original: BlurredImageView.java lines 2842–2871
 */
fun BlurredImageView.drawRectBottom(canvas: Canvas, rect: RectF) {
    if (isGlass()) {
        min(rect.width(), rect.height()) // unused but preserved from original
        val gradient = this.color_gradient
        val baseColor: Int
        if (gradient != null) {
            baseColor = gradient.color
            val argb = Color.argb(70, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
            this.paintIpad.shader = CreateGradient.createLinearGradientWithAngle(
                this.ipad_rect!!,
                this.color_gradient!!.angle.toFloat(),
                intArrayOf(this.color_gradient!!.color, this.color_gradient!!.second, this.color_gradient!!.three),
                floatArrayOf(0.0f, 0.7f, 1.0f)
            )
            this.paintIpad.color = argb
        } else {
            baseColor = this.color_ipad
            this.paintIpad.color = Color.argb(60, Color.red(baseColor), Color.green(this.color_ipad), Color.blue(this.color_ipad))
        }
        this.paintIpad.style = Paint.Style.FILL
        canvas.drawRect(rect, this.paintIpad)

        // Border stroke
        this.paintIpad.style = Paint.Style.STROKE
        this.paintIpad.strokeWidth = rect.height() * 0.003f
        this.paintIpad.color = Color.argb(
            120,
            min(255, Color.red(baseColor) + 40),
            min(255, Color.green(baseColor) + 40),
            min(255, Color.blue(baseColor) + 40)
        )
        canvas.drawRect(rect, this.paintIpad)

        // Glass reflection gradient
        this.paintIpad.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(Color.argb(140, 255, 255, 255), Color.argb(10, 255, 255, 255)),
            floatArrayOf(0.0f, 1.0f),
            Shader.TileMode.CLAMP
        )
        this.paintIpad.style = Paint.Style.FILL
        canvas.drawRect(rect, this.paintIpad)

        // Restore state
        this.paintIpad.shader = if (this.color_gradient != null) this.linearGradient_classic else null
        this.paintIpad.color = this.color_ipad
        this.paintIpad.alpha = 190
        return
    }
    canvas.drawRect(rect, this.paintIpad)
}

/**
 * Draws the square bitmap with a BlurMaskFilter inner shadow.
 *
 * Original: BlurredImageView.java lines 2881–2906
 */
fun BlurredImageView.drawBitmapWithShadow(canvas: Canvas) {
    val bitmap = this.bitmapSquare ?: return
    if (bitmap.isRecycled) return
    try {
        val blurMaskFilter = BlurMaskFilter(bitmap.width * 0.03f, BlurMaskFilter.Blur.INNER)
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = ViewCompat.MEASURED_STATE_MASK
            maskFilter = blurMaskFilter
        }
        this.left_square = this.ipad_rect!!.centerX() - (bitmap.width * 0.5f)
        this.top_square = this.ipad_rect!!.top + (this.bitmapBlured!!.height * 0.02f)

        val bitmap2 = this.bitmapSquare
        if (bitmap2 != null && !bitmap2.isRecycled) {
            canvas.drawBitmap(bitmap2, this.left_square, this.top_square, shadowPaint)
        }
        val bitmap3 = this.bitmapSquare
        if (bitmap3 == null || bitmap3.isRecycled) return
        canvas.drawBitmap(bitmap3, this.left_square, this.top_square, this.paint)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Draws bitmap with shadow for BOTTOM_RECT type (bitmap above the ipad rect).
 *
 * Original: BlurredImageView.java lines 2908–2924
 */
fun BlurredImageView.drawBitmapWithShadowTypeBottom(canvas: Canvas) {
    val bitmap = this.bitmapSquare ?: return
    if (bitmap.isRecycled) return
    try {
        this.left_square = this.ipad_rect!!.left
        this.top_square = this.ipad_rect!!.top - bitmap.height
        val bitmap2 = this.bitmapSquare
        if (bitmap2 == null || bitmap2.isRecycled) return
        canvas.drawBitmap(bitmap2, this.left_square, this.top_square, this.paint)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Draws bitmap with shadow for BOTTOM_RECT type — save/export variant
 * (uses null Paint for lossless drawing).
 *
 * Original: BlurredImageView.java lines 2926–2942
 */
fun BlurredImageView.drawBitmapWithShadowTypeBottomSave(canvas: Canvas) {
    val bitmap = this.bitmapSquare ?: return
    if (bitmap.isRecycled) return
    try {
        this.left_square = this.ipad_rect!!.left
        this.top_square = this.ipad_rect!!.top - bitmap.height
        val bitmap2 = this.bitmapSquare
        if (bitmap2 == null || bitmap2.isRecycled) return
        canvas.drawBitmap(bitmap2, this.left_square, this.top_square, null)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Complex neumorphic rendering with HSV color manipulation, ShadowLayer on
 * light/dark paints, gradient on backgroundPaint, three-pass draw (light shadow,
 * dark shadow, background), and circles for home button area.
 *
 * Original: BlurredImageView.java lines 2944–2996
 */
fun BlurredImageView.drawNeumorphicRect(canvas: Canvas, blurRadius: Float, isFlag: Boolean) {
    val baseColor: Int
    val lightenColor: Int
    val darkenColor: Int

    if (getColor_gradient() != null) {
        this.paint.shader = this.linearGradient_classic
        canvas.drawPaint(this.paint)
        this.paint.shader = null

        val hsv = FloatArray(3)
        Color.colorToHSV(getColor_gradient()!!.second, hsv)
        hsv[0] = (hsv[0] + getColor_gradient()!!.angle) % 360.0f
        hsv[1] = min(1.0f, hsv[1] * 1.2f)
        hsv[2] = min(1.0f, hsv[2] * 1.1f)
        baseColor = Color.HSVToColor(hsv)
        lightenColor = ColorUtils.lightenColor(baseColor, 0.4f)
        darkenColor = ColorUtils.darkenColor(baseColor, 0.4f)
    } else {
        canvas.drawColor(this.color_ipad)
        baseColor = this.color_ipad
        lightenColor = ColorUtils.lightenColor(baseColor, 0.4f)
        darkenColor = ColorUtils.darkenColor(this.color_ipad, 0.4f)
    }

    // Background paint with gradient
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = baseColor
        style = Paint.Style.FILL
    }
    this.backgroundPaint = bgPaint

    // Light shadow paint
    val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = baseColor
        style = Paint.Style.FILL
        setShadowLayer(5.0f, -5.0f, -5.0f, lightenColor)
    }
    this.lightShadowPaint = lightPaint

    // Dark shadow paint
    val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = baseColor
        style = Paint.Style.FILL
        setShadowLayer(5.0f, 5.0f, 5.0f, darkenColor)
    }
    this.darkShadowPaint = darkPaint

    // Gradient on background paint
    this.backgroundPaint.shader = LinearGradient(
        this.ipad_rect!!.left, this.ipad_rect!!.top,
        this.ipad_rect!!.right, this.ipad_rect!!.bottom,
        Color.argb(255, (Color.red(baseColor) * 1.1f).toInt(), (Color.green(baseColor) * 1.1f).toInt(), (Color.blue(baseColor) * 1.1f).toInt()),
        Color.argb(255, (Color.red(baseColor) * 0.9f).toInt(), (Color.green(baseColor) * 0.9f).toInt(), (Color.blue(baseColor) * 0.9f).toInt()),
        Shader.TileMode.CLAMP
    )

    // Three-pass neumorphic draw
    canvas.drawRoundRect(this.ipad_rect!!, blurRadius, blurRadius, this.lightShadowPaint)
    canvas.drawRoundRect(this.ipad_rect!!, blurRadius, blurRadius, this.darkShadowPaint)
    canvas.drawRoundRect(this.ipad_rect!!, blurRadius, blurRadius, this.backgroundPaint)

    // Home button circle area
    val centerX = this.ipad_rect!!.centerX()
    val circleWidth = this.ipad_rect!!.width() * 0.32f
    val circleY = this.ipad_rect!!.top + (1.35f * circleWidth)
    canvas.drawCircle(centerX, circleY, circleWidth, this.lightShadowPaint)
    canvas.drawCircle(centerX, circleY, circleWidth, this.darkShadowPaint)
    canvas.drawCircle(centerX, circleY, circleWidth, this.backgroundPaint)

    // Draw bitmap centered in the circle
    val halfBitmap = this.bitmapSquare!!.width * 0.5f
    this.left_square = centerX - halfBitmap
    this.top_square = circleY - halfBitmap
    canvas.drawBitmap(this.bitmapSquare!!, this.left_square, this.top_square, null)
}

/**
 * Detailed cassette tape drawing with body, trapezoid top/bottom, label strip,
 * two reels with rotating gears, spinning animation, and corner dots.
 * Uses ColorSchemeGenerator.Scheme colors.
 *
 * Original: BlurredImageView.java lines 2998–3114
 */
fun BlurredImageView.drawCaset(canvas: Canvas, isFlag: Boolean, file: File?) {
    val screen1 = this.scheme!!.screen1
    val screen2 = this.scheme!!.screen2
    val body = this.scheme!!.body
    val shadow = this.scheme!!.shadow
    val label = this.scheme!!.label
    val accent = this.scheme!!.accent

    val savedShader = this.paintIpad.shader
    this.paintIpad.shader = null

    val width = this.bitmapBlured!!.width
    val height = this.bitmapBlured!!.height

    // Background
    canvas.drawColor(screen1)

    // Bottom trapezoid area
    val bottomPath = Path()
    bottomPath.moveTo(this.ipad_rect!!.centerX(), this.ipad_rect!!.bottom * 0.8f)
    val w = width.toFloat()
    bottomPath.lineTo(w, this.ipad_rect!!.bottom * 1.065f)
    val h = height.toFloat()
    bottomPath.lineTo(w, h)
    bottomPath.lineTo(0.0f, h)
    bottomPath.lineTo(0.0f, this.ipad_rect!!.bottom * 1.065f)
    bottomPath.close()
    this.paintIpad.color = screen2
    canvas.drawPath(bottomPath, this.paintIpad)

    // Cassette body shadow
    val bodyRadius = this.ipad_rect!!.height() * 0.07f
    val shadowRect = RectF(
        this.ipad_rect!!.left - (this.ipad_rect!!.height() * 0.1f),
        this.ipad_rect!!.top,
        this.ipad_rect!!.right * 1.01f,
        this.ipad_rect!!.bottom
    )
    this.paintIpad.color = shadow
    canvas.drawRoundRect(shadowRect, bodyRadius, bodyRadius, this.paintIpad)

    // Cassette body
    this.paintIpad.color = body
    canvas.drawRoundRect(this.ipad_rect!!, bodyRadius, bodyRadius, this.paintIpad)

    // Top trapezoid
    val topTrapWidth = this.ipad_rect!!.width() * 0.24f
    val topTrapHeight = this.ipad_rect!!.height() * 0.14f
    val topTrapPath = Path()
    topTrapPath.moveTo(this.ipad_rect!!.centerX() - topTrapWidth, this.ipad_rect!!.top)
    topTrapPath.lineTo(this.ipad_rect!!.centerX() + topTrapWidth, this.ipad_rect!!.top)
    val topNarrow = 0.85f * topTrapWidth
    topTrapPath.lineTo(this.ipad_rect!!.centerX() + topNarrow, this.ipad_rect!!.top + topTrapHeight)
    topTrapPath.lineTo(this.ipad_rect!!.centerX() - topNarrow, this.ipad_rect!!.top + topTrapHeight)
    topTrapPath.close()
    this.paintIpad.color = label
    canvas.drawPath(topTrapPath, this.paintIpad)

    // Bottom trapezoid
    val bottomTrapPath = Path()
    bottomTrapPath.moveTo(this.ipad_rect!!.centerX() - topTrapWidth, this.ipad_rect!!.bottom)
    bottomTrapPath.lineTo(this.ipad_rect!!.centerX() + topTrapWidth, this.ipad_rect!!.bottom)
    bottomTrapPath.lineTo(this.ipad_rect!!.centerX() + topNarrow, this.ipad_rect!!.bottom - topTrapHeight)
    bottomTrapPath.lineTo(this.ipad_rect!!.centerX() - topNarrow, this.ipad_rect!!.bottom - topTrapHeight)
    bottomTrapPath.close()
    canvas.drawPath(bottomTrapPath, this.paintIpad)

    // Label strip (top)
    val labelWidth = this.ipad_rect!!.width() * 0.4f
    val labelHeight = this.ipad_rect!!.height() * 0.2f
    val labelTop = this.ipad_rect!!.top + labelHeight
    val labelRect = RectF(
        this.ipad_rect!!.centerX() - labelWidth, labelTop,
        this.ipad_rect!!.centerX() + labelWidth, labelHeight + labelTop
    )
    this.paintIpad.color = label
    canvas.drawRect(labelRect, this.paintIpad)

    // Label strip (bottom, between reels)
    val labelRect2 = RectF(
        this.ipad_rect!!.centerX() - labelWidth, labelRect.bottom * 1.01f,
        this.ipad_rect!!.centerX() + labelWidth, this.ipad_rect!!.bottom - (1.2f * topTrapHeight)
    )
    canvas.drawRect(labelRect2, this.paintIpad)

    // Reel circles
    val reelRadius = labelRect2.height() * 0.26f
    val reelCenterY = labelRect2.centerY()
    val reelSpacing = 2.0f * reelRadius
    val leftReelCenterX = labelRect2.centerX() - reelSpacing
    val rightReelCenterX = labelRect2.centerX() + reelSpacing

    this.paintIpad.color = ColorUtils.darkenColor(body, 0.8f)
    canvas.drawCircle(leftReelCenterX, reelCenterY, reelRadius, this.paintIpad)
    canvas.drawCircle(rightReelCenterX, reelCenterY, reelRadius, this.paintIpad)

    // Gear/cog drawable for reels
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_circle_caset)
    drawable!!.setTint(accent)
    val roundX = Math.round(leftReelCenterX)
    val roundY = reelCenterY.toInt()
    val gearSize = (reelRadius * 0.75f).toInt()
    val gearTop = roundY - gearSize
    val gearBottom = roundY + gearSize
    val gearRect = Rect(roundX - gearSize, gearTop, roundX + gearSize, gearBottom)

    val blurRadius: Float
    val floatValue2: Float

    if (isFlag) {
        // Animated spinning reels
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis()
        }
        blurRadius = topTrapHeight
        val rotation = ((System.currentTimeMillis() - this.startTime) / 1000.0f * 90.0f)

        // Left reel — clockwise
        canvas.save()
        canvas.rotate(rotation, roundX.toFloat(), roundY.toFloat())
        drawable.setBounds(gearRect)
        drawable.draw(canvas)
        canvas.restore()

        // Right reel — counter-clockwise
        val rightRoundX = rightReelCenterX.toInt()
        canvas.save()
        canvas.rotate(-rotation, rightRoundX.toFloat(), roundY.toFloat())
        drawable.setBounds(rightRoundX - gearSize, gearTop, rightRoundX + gearSize, gearBottom)
        drawable.draw(canvas)
        canvas.restore()

        floatValue2 = 0.5f
    } else {
        blurRadius = topTrapHeight
        floatValue2 = 0.5f
        this.rectFProgress!!.left = leftReelCenterX - (gearRect.width() * 0.5f)
        this.rectFProgress!!.top = gearRect.top.toFloat()
        this.rectFProgress!!.right = rightReelCenterX - (gearRect.width() * 0.5f)
        if (file != null) {
            saveProgressCasetBitmap(file, gearRect.width(), gearRect.height(), drawable)
        }
    }

    // Corner dots
    val dotRadius = blurRadius * 0.25f
    val dotOffset = blurRadius * floatValue2
    val dotTop = (this.ipad_rect!!.top + dotOffset).toInt()
    val dotBottom = this.ipad_rect!!.bottom - dotOffset
    val dotLeftX = leftReelCenterX * 1.02f
    val dotRightX = rightReelCenterX * 0.95f

    this.paintIpad.color = -1 // white
    canvas.drawCircle(dotLeftX, dotTop.toFloat(), dotRadius, this.paintIpad)
    canvas.drawCircle(dotRightX, dotTop.toFloat(), dotRadius, this.paintIpad)
    canvas.drawCircle(dotLeftX, dotBottom, dotRadius, this.paintIpad)
    canvas.drawCircle(dotRightX, dotBottom, dotRadius, this.paintIpad)

    // Restore shader
    this.paintIpad.shader = savedShader
}

/**
 * Cassette without background — draws only the cassette body over existing content.
 * Draws the bitmapSquare first when available.
 *
 * Original: BlurredImageView.java lines 3116–3214
 */
fun BlurredImageView.drawCasetNoBg(canvas: Canvas, isFlag: Boolean, file: File?) {
    canvas.drawBitmap(this.bitmapSquare!!, 0.0f, 0.0f, null)

    val screen2 = this.scheme!!.screen2
    val body = this.scheme!!.body
    val shadow = this.scheme!!.shadow
    val label = this.scheme!!.label
    val accent = this.scheme!!.accent

    val savedShader = this.paintIpad.shader
    this.paintIpad.shader = null

    val bodyRadius = this.ipad_rect!!.height() * 0.07f

    // Shadow
    val shadowRect = RectF(
        this.ipad_rect!!.left - (this.ipad_rect!!.height() * 0.1f),
        this.ipad_rect!!.top,
        this.ipad_rect!!.right * 1.01f,
        this.ipad_rect!!.bottom
    )
    this.paintIpad.color = shadow
    canvas.drawRoundRect(shadowRect, bodyRadius, bodyRadius, this.paintIpad)

    // Body
    this.paintIpad.color = body
    canvas.drawRoundRect(this.ipad_rect!!, bodyRadius, bodyRadius, this.paintIpad)

    // Top trapezoid
    val topTrapWidth = this.ipad_rect!!.width() * 0.24f
    val topTrapHeight = this.ipad_rect!!.height() * 0.14f
    val topPath = Path()
    topPath.moveTo(this.ipad_rect!!.centerX() - topTrapWidth, this.ipad_rect!!.top)
    topPath.lineTo(this.ipad_rect!!.centerX() + topTrapWidth, this.ipad_rect!!.top)
    val topNarrow = 0.85f * topTrapWidth
    topPath.lineTo(this.ipad_rect!!.centerX() + topNarrow, this.ipad_rect!!.top + topTrapHeight)
    topPath.lineTo(this.ipad_rect!!.centerX() - topNarrow, this.ipad_rect!!.top + topTrapHeight)
    topPath.close()
    this.paintIpad.color = label
    canvas.drawPath(topPath, this.paintIpad)

    // Bottom trapezoid
    val bottomPath = Path()
    bottomPath.moveTo(this.ipad_rect!!.centerX() - topTrapWidth, this.ipad_rect!!.bottom)
    bottomPath.lineTo(this.ipad_rect!!.centerX() + topTrapWidth, this.ipad_rect!!.bottom)
    bottomPath.lineTo(this.ipad_rect!!.centerX() + topNarrow, this.ipad_rect!!.bottom - topTrapHeight)
    bottomPath.lineTo(this.ipad_rect!!.centerX() - topNarrow, this.ipad_rect!!.bottom - topTrapHeight)
    bottomPath.close()
    canvas.drawPath(bottomPath, this.paintIpad)

    // Label strips
    val labelWidth = this.ipad_rect!!.width() * 0.4f
    val labelHeight = this.ipad_rect!!.height() * 0.2f
    val labelTop = this.ipad_rect!!.top + labelHeight
    val labelRect = RectF(
        this.ipad_rect!!.centerX() - labelWidth, labelTop,
        this.ipad_rect!!.centerX() + labelWidth, labelHeight + labelTop
    )
    this.paintIpad.color = label
    canvas.drawRect(labelRect, this.paintIpad)

    val labelRect2 = RectF(
        this.ipad_rect!!.centerX() - labelWidth, labelRect.bottom * 1.01f,
        this.ipad_rect!!.centerX() + labelWidth, this.ipad_rect!!.bottom - (1.2f * topTrapHeight)
    )
    canvas.drawRect(labelRect2, this.paintIpad)

    // Reel circles
    val reelRadius = labelRect2.height() * 0.26f
    val reelCenterY = labelRect2.centerY()
    val reelSpacing = 2.0f * reelRadius
    val leftReelCenterX = labelRect2.centerX() - reelSpacing
    val rightReelCenterX = labelRect2.centerX() + reelSpacing

    this.paintIpad.color = ColorUtils.darkenColor(body, 0.8f)
    canvas.drawCircle(leftReelCenterX, reelCenterY, reelRadius, this.paintIpad)
    canvas.drawCircle(rightReelCenterX, reelCenterY, reelRadius, this.paintIpad)

    // Gear/cog drawable
    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_circle_caset)
    drawable!!.setTint(screen2)
    val roundX = Math.round(leftReelCenterX)
    val roundY = reelCenterY.toInt()
    val gearSize = (reelRadius * 0.75f).toInt()
    val gearTop = roundY - gearSize
    val gearBottom = roundY + gearSize
    val gearRect = Rect(roundX - gearSize, gearTop, roundX + gearSize, gearBottom)

    val blurRadius: Float

    if (isFlag) {
        // Animated spinning reels
        if (this.startTime < 0) {
            this.startTime = System.currentTimeMillis()
        }
        val rotation = ((System.currentTimeMillis() - this.startTime) / 1000.0f * 90.0f)

        // Left reel — clockwise
        canvas.save()
        canvas.rotate(rotation, roundX.toFloat(), roundY.toFloat())
        drawable.setBounds(gearRect)
        drawable.draw(canvas)
        canvas.restore()

        // Right reel — counter-clockwise
        val rightRoundX = rightReelCenterX.toInt()
        canvas.save()
        canvas.rotate(-rotation, rightRoundX.toFloat(), roundY.toFloat())
        drawable.setBounds(rightRoundX - gearSize, gearTop, rightRoundX + gearSize, gearBottom)
        drawable.draw(canvas)
        canvas.restore()

        blurRadius = 0.5f
    } else {
        blurRadius = 0.5f
        this.rectFProgress!!.left = leftReelCenterX - (gearRect.width() * 0.5f)
        this.rectFProgress!!.top = gearRect.top.toFloat()
        this.rectFProgress!!.right = rightReelCenterX - (gearRect.width() * 0.5f)
        if (file != null) {
            saveProgressCasetBitmap(file, gearRect.width(), gearRect.height(), drawable)
        }
    }

    // Corner dots
    val dotRadius = 0.25f * topTrapHeight
    val dotOffset = topTrapHeight * blurRadius
    val dotTop = (this.ipad_rect!!.top + dotOffset).toInt()
    val dotBottom = this.ipad_rect!!.bottom - dotOffset
    val dotLeftX = leftReelCenterX * 1.02f
    val dotRightX = rightReelCenterX * 0.95f

    this.paintIpad.color = -1 // white
    canvas.drawCircle(dotLeftX, dotTop.toFloat(), dotRadius, this.paintIpad)
    canvas.drawCircle(dotRightX, dotTop.toFloat(), dotRadius, this.paintIpad)
    canvas.drawCircle(dotLeftX, dotBottom, dotRadius, this.paintIpad)
    canvas.drawCircle(dotRightX, dotBottom, dotRadius, this.paintIpad)

    // Restore shader
    this.paintIpad.shader = savedShader
}

/**
 * Draws a gear/cog shape for cassette reels. The gear has [teethCount] teeth
 * alternating between outer radius [outerR] and inner radius [innerR],
 * centered at ([cx], [cy]).
 *
 * Original: BlurredImageView.java lines 3216–3242
 */
fun BlurredImageView.drawInnerGear(
    canvas: Canvas,
    cx: Float,
    cy: Float,
    outerR: Float,
    innerR: Float,
    teethCount: Int,
    paint: Paint
) {
    val path = Path()
    val counter = teethCount * 2
    val stepAngle = 2.0 * Math.PI / counter

    for (i in 0 until counter) {
        val angle = i * stepAngle
        val radius = if (i % 2 == 0) outerR else innerR
        val x = (cx + cos(angle) * radius).toFloat()
        val y = (cy + sin(angle) * radius).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    canvas.drawPath(path, paint)
}

/**
 * Progress bar for neumorphic type — draws a rounded track with
 * filled portion based on progress, plus time text labels.
 *
 * Original: BlurredImageView.java lines 3244–3262
 */
fun BlurredImageView.drawProgressNeumorphic(canvas: Canvas) {
    val savedStrokeWidth = this.linePaint.strokeWidth
    this.linePaint.strokeWidth = this.rectFProgress!!.height() * 0.18f
    val halfStroke = this.linePaint.strokeWidth * 0.5f
    val progressEnd = this.rectFProgress!!.left + (this.rectFProgress!!.width() * this.progress)

    // Background track
    this.linePaint.color = this.color_line_bg
    canvas.drawRoundRect(
        this.rectFProgress!!.left, this.rectFProgress!!.centerY() - halfStroke,
        this.rectFProgress!!.right, this.rectFProgress!!.centerY() + halfStroke,
        this.rectFProgress!!.height(), this.rectFProgress!!.height(),
        this.linePaint
    )

    // Filled progress
    this.linePaint.color = this.paintLecture.color
    this.linePaint.strokeWidth = this.linePaint.strokeWidth * 0.5f
    canvas.drawRoundRect(
        this.rectFProgress!!.left, this.rectFProgress!!.centerY() - halfStroke,
        progressEnd, this.rectFProgress!!.centerY() + halfStroke,
        this.rectFProgress!!.height(), this.rectFProgress!!.height(),
        this.linePaint
    )

    // Restore stroke width
    this.linePaint.strokeWidth = savedStrokeWidth

    // Time labels
    val textBounds = Rect()
    this.paintText.getTextBounds(this.currentTime, 0, this.currentTime.length, textBounds)
    canvas.drawText(this.currentTime, this.rectFProgress!!.left, this.rectFProgress!!.bottom, this.paintText)
    canvas.drawText(this.remainingTime, this.rectFProgress!!.right - textBounds.width(), this.rectFProgress!!.bottom, this.paintText)
}

/**
 * Lecture/playback controls for neumorphic type — draws three circular
 * neumorphic buttons (play/pause, forward, backward) with icon drawables.
 *
 * Original: BlurredImageView.java lines 3264–3296
 */
fun BlurredImageView.drawLectureNeumorphic(canvas: Canvas) {
    val buttonHeight = this.rectFLecture!!.height() * 0.3f
    val playRect = Rect(
        (this.rectFLecture!!.centerX() - buttonHeight).toInt(),
        (this.rectFLecture!!.centerY() - buttonHeight).toInt(),
        (this.rectFLecture!!.centerX() + buttonHeight).toInt(),
        (this.rectFLecture!!.centerY() + buttonHeight).toInt()
    )
    val padding = (playRect.width() * 0.15f).toInt()

    // Play/pause button — neumorphic three-pass
    canvas.drawCircle(playRect.centerX().toFloat(), playRect.centerY().toFloat(), playRect.height() * 0.5f, this.lightShadowPaint)
    canvas.drawCircle(playRect.centerX().toFloat(), playRect.centerY().toFloat(), playRect.height() * 0.5f, this.darkShadowPaint)
    canvas.drawCircle(playRect.centerX().toFloat(), playRect.centerY().toFloat(), playRect.height() * 0.5f, this.backgroundPaint)

    val pauseDrawable = ContextCompat.getDrawable(context, R.drawable.pause_24px)
    pauseDrawable!!.setTint(this.paintLecture.color)
    pauseDrawable.setBounds(playRect.left + padding, playRect.top + padding, playRect.right - padding, playRect.bottom - padding)
    pauseDrawable.draw(canvas)

    // Forward button
    val forwardSize = (playRect.height() * 0.4f).toInt()
    val forwardOffset = playRect.width() * 0.55f
    val forwardLeft = (playRect.right + forwardOffset).toInt()
    val forwardRect = Rect(forwardLeft, playRect.centerY() - forwardSize, forwardLeft + forwardSize, playRect.centerY() + forwardSize)
    val forwardPadding = (playRect.width() * 0.15f).toInt()

    canvas.drawCircle(forwardRect.centerX().toFloat(), forwardRect.centerY().toFloat(), forwardRect.height() * 0.5f, this.lightShadowPaint)
    canvas.drawCircle(forwardRect.centerX().toFloat(), forwardRect.centerY().toFloat(), forwardRect.height() * 0.5f, this.darkShadowPaint)
    canvas.drawCircle(forwardRect.centerX().toFloat(), forwardRect.centerY().toFloat(), forwardRect.height() * 0.5f, this.backgroundPaint)

    val forwardDrawable = ContextCompat.getDrawable(context, R.drawable.arrow_forward_ios_24px)
    forwardDrawable!!.setTint(this.paintLecture.color)
    forwardDrawable.setBounds(forwardRect.left, forwardRect.top + forwardPadding, forwardRect.right, forwardRect.bottom - forwardPadding)
    forwardDrawable.draw(canvas)

    // Backward button
    val backwardRight = (playRect.left - forwardOffset).toInt()
    val backwardRect = Rect(backwardRight - forwardSize, playRect.centerY() - forwardSize, backwardRight, playRect.centerY() + forwardSize)

    canvas.drawCircle(backwardRect.centerX().toFloat(), backwardRect.centerY().toFloat(), backwardRect.height() * 0.5f, this.lightShadowPaint)
    canvas.drawCircle(backwardRect.centerX().toFloat(), backwardRect.centerY().toFloat(), backwardRect.height() * 0.5f, this.darkShadowPaint)
    canvas.drawCircle(backwardRect.centerX().toFloat(), backwardRect.centerY().toFloat(), backwardRect.height() * 0.5f, this.backgroundPaint)

    val backwardDrawable = ContextCompat.getDrawable(context, R.drawable.arrow_back_ios_24px)
    backwardDrawable!!.setTint(this.paintLecture.color)
    backwardDrawable.setBounds(backwardRect.left, backwardRect.top + forwardPadding, backwardRect.right, backwardRect.bottom - forwardPadding)
    backwardDrawable.draw(canvas)
}

/**
 * Mask brush type with PorterDuff DST_OUT compositing on an offscreen bitmap.
 * Creates a colored layer, then cuts out a brush-mask shape from it.
 *
 * Original: BlurredImageView.java lines 3462–3494
 */
fun BlurredImageView.drawMaskedBitmap(canvas: Canvas, isFlag: Boolean) {
    this.paintIpad.alpha = 255

    if (isFlag) {
        canvas.drawBitmap(this.bitmapSquare!!, 0.0f, 0.0f, this.paint)
    }

    // Create offscreen bitmap for compositing
    val offscreenWidth = (this.bitmapNotBlur!!.width * 1.1f).toInt()
    val offscreenHeight = (this.bitmapNotBlur!!.height * 1.1f).toInt()
    val offscreenBitmap = Bitmap.createBitmap(offscreenWidth, offscreenHeight, Bitmap.Config.ARGB_8888)
    val offscreenCanvas = Canvas(offscreenBitmap)

    // Fill with gradient or solid color
    if (getColor_gradient() != null) {
        this.paint.shader = this.linearGradient_classic
        offscreenCanvas.drawPaint(this.paint)
        this.paint.shader = null
    } else {
        offscreenCanvas.drawColor(this.paintIpad.color)
    }

    // Create mask bitmap
    val maskSize = (min(offscreenBitmap.width, offscreenBitmap.height) * 0.57f).toInt()
    val maskRect = Rect(0, 0, maskSize, maskSize)
    val maskBitmap = Bitmap.createBitmap(maskRect.width(), maskRect.height(), Bitmap.Config.ARGB_8888)
    offscreenCanvas.setBitmap(maskBitmap)

    // Draw brush mask shape
    val brushDrawable = ContextCompat.getDrawable(context, R.drawable.brush_mask_2)
    brushDrawable!!.setBounds(0, 0, maskRect.width(), maskRect.height())
    brushDrawable.draw(offscreenCanvas)

    // Composite: cut out the mask shape using DST_OUT
    val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
    }
    offscreenCanvas.setBitmap(offscreenBitmap)

    val maskX: Float
    val maskY: Float
    if (this.mResizetype == ResizeType.SOCIAL_STORY.ordinal) {
        maskX = (offscreenBitmap.width - maskBitmap.width) * 0.5f
        maskY = offscreenBitmap.height * 0.35f
    } else {
        maskX = (offscreenBitmap.width - maskBitmap.width) * 0.5f
        maskY = offscreenBitmap.height * 0.32f
    }
    offscreenCanvas.drawBitmap(maskBitmap, maskX, maskY, maskPaint)

    // Draw result to main canvas
    canvas.drawBitmap(offscreenBitmap, this.btmX, this.btmY, null)

    // Restore paintIpad state
    this.paintIpad.shader = null
    this.paintIpad.alpha = 190
}

/**
 * Gradient overlay — draws a linear gradient from transparent to the
 * gradient colors (or solid color) over the ipad rect.
 *
 * Original: BlurredImageView.java lines 3496–3508
 */
fun BlurredImageView.drawGradientLayer(canvas: Canvas, isFlag: Boolean) {
    if (isFlag) {
        canvas.drawBitmap(this.bitmapSquare!!, 0.0f, 0.0f, null)
    }
    this.paintIpad.alpha = 255
    if (getColor_gradient() != null) {
        this.paintIpad.shader = LinearGradient(
            0.0f, this.ipad_rect!!.top, 0.0f, this.ipad_rect!!.bottom,
            intArrayOf(0, getColor_gradient()!!.color, getColor_gradient()!!.second, getColor_gradient()!!.three),
            floatArrayOf(0.0f, 0.87f, 0.93f, 1.0f),
            Shader.TileMode.CLAMP
        )
    } else {
        this.paintIpad.shader = LinearGradient(
            0.0f, this.ipad_rect!!.top, 0.0f, this.ipad_rect!!.bottom,
            intArrayOf(0, this.paintIpad.color),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(this.ipad_rect!!, this.paintIpad)
    this.paintIpad.shader = null
}

/**
 * Heart shape with shaped progress fill. Uses clipPath to restrict
 * the fill rectangle to the heart shape, then strokes the outline.
 *
 * Original: BlurredImageView.java lines 3510–3541
 */
fun BlurredImageView.drawHeartType(canvas: Canvas, isFlag: Boolean) {
    if (!isFlag) return

    val savedStrokeWidth = this.linePaint.strokeWidth

    // Build heart path
    val path = Path()
    val width = this.rectFProgress!!.width()
    val height = this.rectFProgress!!.height()
    val centerX = this.rectFProgress!!.centerX()
    val centerY = this.rectFProgress!!.centerY()
    val halfMin = min(width, height) / 2.0f
    val bottomY = 0.6f * halfMin + centerY

    path.moveTo(centerX, bottomY)
    val armWidth = 1.2f * halfMin
    val armY = 0.1f * halfMin + centerY
    val curveWidth = halfMin * 0.8f
    val topCurveY = centerY - (0.9f * halfMin)
    path.cubicTo(centerX + armWidth, armY, centerX + curveWidth, topCurveY, centerX, centerY - (0.4f * halfMin))
    path.cubicTo(centerX - curveWidth, topCurveY, centerX - armWidth, armY, centerX, bottomY)
    path.close()

    // Compute bounds for the fill
    val pathBounds = RectF()
    path.computeBounds(pathBounds, true)

    // Clip to heart and fill progress from bottom up
    canvas.save()
    canvas.clipPath(path)
    val fillTop = pathBounds.bottom - (pathBounds.height() * 0.78f * this.progress)
    canvas.drawRect(pathBounds.left, fillTop, pathBounds.right, pathBounds.bottom, this.paintIpad)
    canvas.restore()

    // Stroke the heart outline
    this.paintIpad.style = Paint.Style.STROKE
    this.paintIpad.strokeWidth = 0.02f * halfMin
    canvas.drawPath(path, this.paintIpad)

    // Restore
    this.paintIpad.style = Paint.Style.FILL
    this.paintIpad.strokeWidth = savedStrokeWidth
}

/**
 * Battery shape with shaped progress fill. Uses clipPath with
 * battery outline and lightning bolt cutout for progress indication.
 *
 * Original: BlurredImageView.java lines 3543–3593
 */
fun BlurredImageView.drawBatteryType(canvas: Canvas, isFlag: Boolean) {
    if (!isFlag) return

    val savedStrokeWidth = this.linePaint.strokeWidth
    val width = this.rectFProgress!!.width()
    val height = this.rectFProgress!!.height()
    val centerX = this.rectFProgress!!.centerX()
    val centerY = this.rectFProgress!!.centerY()
    val minDim = min(width, height)
    val batteryWidth = 0.8f * minDim
    val batteryHeight = 0.45f * minDim
    val halfW = batteryWidth / 2.0f
    val left = centerX - halfW
    val halfH = batteryHeight / 2.0f
    val top = centerY - halfH
    val right = halfW + centerX
    val bottom = halfH + centerY
    val capWidth = 0.07f * batteryWidth + right
    val capHalf = 0.25f * batteryHeight

    // Battery body path (round-rect + cap)
    val batteryPath = Path()
    val batteryRect = RectF(left, top, right, bottom)
    val cornerRadius = minDim * 0.05f
    batteryPath.addRoundRect(batteryRect, cornerRadius, cornerRadius, Path.Direction.CW)
    batteryPath.addRect(RectF(right, centerY - capHalf, capWidth, capHalf + centerY), Path.Direction.CW)

    // Lightning bolt path
    val boltPath = Path()
    val boltArm1 = batteryWidth * 0.3f
    val boltArm2 = 0.3f * boltArm1
    val boltHalf = batteryHeight * 0.5f * 0.5f
    boltPath.moveTo(centerX - boltArm2, centerY - boltHalf)
    boltPath.lineTo(centerX, centerY)
    val boltTail = boltArm1 * 0.2f
    boltPath.lineTo(centerX - boltTail, centerY)
    boltPath.lineTo(boltArm2 + centerX, boltHalf + centerY)
    boltPath.lineTo(centerX, centerY)
    boltPath.lineTo(centerX + boltTail, centerY)
    boltPath.close()

    // Clip to battery shape, cut out lightning bolt, then fill progress
    canvas.save()
    canvas.clipPath(batteryPath)
    if (Build.VERSION.SDK_INT >= 26) {
        val diffPath = Path()
        diffPath.op(batteryPath, boltPath, Path.Op.DIFFERENCE)
        canvas.clipPath(diffPath)
    }
    val progressRight = (capWidth - left) * this.progress + left
    canvas.drawRect(RectF(left, top, progressRight, bottom), this.paintIpad)
    canvas.restore()

    // Stroke battery outline
    this.paintIpad.style = Paint.Style.STROKE
    this.paintIpad.strokeWidth = 0.025f * minDim
    canvas.drawPath(batteryPath, this.paintIpad)

    // Restore
    this.paintIpad.style = Paint.Style.FILL
    this.paintIpad.strokeWidth = savedStrokeWidth
}

/**
 * Blue type with RadialGradient. Creates a dark overlay with a
 * radial gradient emanating from above center.
 *
 * Original: BlurredImageView.java lines 3595–3624
 */
fun BlurredImageView.drawBlueType(canvas: Canvas, isFlag: Boolean) {
    this.paintIpad.color = ViewCompat.MEASURED_STATE_MASK

    if (isFlag) {
        canvas.drawBitmap(this.bitmapSquare!!, 0.0f, 0.0f, this.grayscalePaint)
    }

    val radialRadius = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 1.3f
    this.paintIpad.alpha = 0xF0 // 240 — was PsExtractor.VIDEO_STREAM_MASK
    val radialCenterY = -0.15f * radialRadius

    this.paintIpad.shader = RadialGradient(
        this.ipad_rect!!.centerX(), radialCenterY, radialRadius,
        intArrayOf(ViewCompat.MEASURED_STATE_MASK, -0x34000000, Int.MIN_VALUE, 0),
        floatArrayOf(0.2f, 0.5f, 0.7f, 1.0f),
        Shader.TileMode.CLAMP
    )

    if (isFlag) {
        canvas.drawCircle(this.ipad_rect!!.centerX(), radialCenterY, radialRadius, this.paintIpad)
        this.paintIpad.shader = null
        this.paintIpad.alpha = 190

        // Progress line
        val savedStrokeWidth = this.linePaint.strokeWidth
        this.linePaint.strokeWidth = this.rectFProgress!!.height() * 0.18f
        val progressEnd = this.rectFProgress!!.left + (this.rectFProgress!!.width() * this.progress)

        this.linePaint.color = this.color_line_bg
        canvas.drawLine(
            this.rectFProgress!!.left, this.rectFProgress!!.centerY(),
            this.rectFProgress!!.right, this.rectFProgress!!.centerY(),
            this.linePaint
        )
        this.linePaint.color = this.paintLecture.color
        this.linePaint.strokeWidth = this.linePaint.strokeWidth * 0.5f
        canvas.drawLine(
            this.rectFProgress!!.left, this.rectFProgress!!.centerY(),
            progressEnd, this.rectFProgress!!.centerY(),
            this.linePaint
        )
        this.linePaint.strokeWidth = savedStrokeWidth
        return
    }

    // Non-playing state
    canvas.drawBitmap(this.bitmapNotBlur!!, 0.0f, 0.0f, this.grayscalePaint)
    canvas.drawCircle(this.ipad_rect!!.centerX(), radialCenterY, radialRadius, this.paintIpad)
    this.paintIpad.shader = null
    this.paintIpad.alpha = 190
}

/**
 * Semi-transparent black overlay — draws the bitmapSquare (if not empty)
 * then a dark rect with alpha 204 (80% opacity).
 *
 * Original: BlurredImageView.java lines 3626–3633
 */
fun BlurredImageView.drawBlackLayer(canvas: Canvas, isFlag: Boolean, isEmpty: Boolean) {
    this.paintIpad.alpha = 204
    if (isEmpty) {
        canvas.drawBitmap(this.bitmapSquare!!, 0.0f, 0.0f, null)
    }
    canvas.drawRect(this.ipad_rect!!, this.paintIpad)
    this.paintIpad.alpha = 190
}

/**
 * Main iPad drawing dispatcher — routes to the appropriate drawing method
 * based on the current IpadType. All rendering uses premium-quality paths
 * (billing removed — all features available to everyone).
 *
 * Original: BlurredImageView.java lines 3298–3460
 */
fun BlurredImageView.drawIpad(canvas: Canvas, isFlag: Boolean) {
    when {
        this.mIpadType == IpadType.IPAD_CLASSIC.ordinal -> {
            canvas.drawRect(this.ipad_rect!!, this.paintIpad)
            // BEFORE: Premium path set coordinates but never drew the bitmap (thin-line bug)
            // WHY_CHANGED: Reference calls drawBitmapWithShadow() here, not coordinate-only path
            // FIXED_BY: Call drawBitmapWithShadow(canvas) to actually draw the iPad album art
            // REF: BlurredImageView.java line 3315
            // VISUAL_IMPACT: iPad frame now shows album art instead of empty rectangle
            drawBitmapWithShadow(canvas)
            drawLectureExt(canvas)
            if (isFlag) {
                drawProgressExt(canvas)
            }
        }
        this.mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal -> {
            drawNeumorphicRect(canvas, this.ipad_rect!!.width() * 0.12f, false)
            drawLectureNeumorphic(canvas)
            if (isFlag) {
                drawProgressNeumorphic(canvas)
            }
        }
        this.mIpadType == IpadType.CASSET.ordinal -> {
            drawCaset(canvas, isFlag, null)
        }
        this.mIpadType == IpadType.CASSET_IMG.ordinal || this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal -> {
            drawCasetNoBg(canvas, isFlag, null)
        }
        this.mIpadType == IpadType.IPAD.ordinal || this.mIpadType == IpadType.IPAD_UNBLUR.ordinal -> {
            val shadowRad = (min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.03f).toInt()
            drawRectWithShadow(canvas, this.ipad_rect!!, ViewCompat.MEASURED_STATE_MASK, if (shadowRad <= 0) 1 else shadowRad, 0, 0, true)
            // BEFORE: Premium path set coordinates but never drew the bitmap (thin-line bug)
            // WHY_CHANGED: Reference calls drawBitmapWithShadow() here for onDraw rendering
            // FIXED_BY: Call drawBitmapWithShadow(canvas) to actually draw the iPad album art
            // REF: BlurredImageView.java line 3325
            // VISUAL_IMPACT: iPad frame now shows album art instead of empty rectangle
            drawBitmapWithShadow(canvas)
            drawLectureExt(canvas)
            if (isFlag) {
                drawProgressExt(canvas)
            }
        }
        this.mIpadType == IpadType.BOTTOM_RECT.ordinal -> {
            drawRectBottom(canvas, this.ipad_rect!!)
            // BEFORE: Used Save variant (premium export path) instead of display path
            // WHY_CHANGED: Reference's 2-param drawIpad uses drawBitmapWithShadowTypeBottom for display
            // FIXED_BY: Use drawBitmapWithShadowTypeBottom for on-screen rendering
            // REF: BlurredImageView.java line 3335
            // VISUAL_IMPACT: Bottom-rect iPad renders correctly during editing
            drawBitmapWithShadowTypeBottom(canvas)
            drawLectureExt(canvas)
            if (isFlag) {
                drawProgressExt(canvas)
            }
        }
        this.mIpadType == IpadType.ROUND_RECT.ordinal -> {
            val shadowRad2 = (this.ipad_rect!!.width() * 0.03f).toInt()
            drawRectWithShadow(canvas, this.ipad_rect!!, ViewCompat.MEASURED_STATE_MASK, if (shadowRad2 <= 0) 1 else shadowRad2, 0, 0, true)
            drawLectureExt(canvas)
            if (isFlag) {
                drawProgressExt(canvas)
            }
        }
        this.mIpadType == IpadType.RECT.ordinal || this.mIpadType == IpadType.BORDER.ordinal -> {
            val shadowRad3 = (this.ipad_rect!!.width() * 0.03f).toInt()
            drawRectWithShadow(canvas, this.ipad_rect!!, ViewCompat.MEASURED_STATE_MASK, if (shadowRad3 <= 0) 1 else shadowRad3, 0, 0, false)
            drawLectureExt(canvas)
            if (isFlag) {
                drawProgressExt(canvas)
            }
        }
        this.mIpadType == IpadType.BLACK_LAYER.ordinal -> {
            drawBlackLayer(canvas, isFlag, isVideo())
        }
        this.mIpadType == IpadType.BLUE_TYPE.ordinal -> {
            drawBlueType(canvas, isFlag)
        }
        this.mIpadType == IpadType.HEART.ordinal -> {
            drawHeartType(canvas, isFlag)
        }
        this.mIpadType == IpadType.BATTERY.ordinal -> {
            drawBatteryType(canvas, isFlag)
        }
        this.mIpadType == IpadType.GRADIENT.ordinal -> {
            drawGradientLayer(canvas, isVideo())
        }
        this.mIpadType == IpadType.MASK_BRUSH.ordinal -> {
            drawMaskedBitmap(canvas, this.isVideo)
        }
    }
}
