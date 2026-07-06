package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

class NeumorphicView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var accentColor: Int = 0
    private var baseColor: Int = 0
    private var darkShadowColor: Int = 0
    private var iconColor: Int = 0
    private var lightHighlightColor: Int = 0
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val rectF: RectF = RectF()
    private var textColor: Int = 0

    init {
        setBaseThemeColor(Color.rgb(200, 200, 200))
    }

    fun setBaseThemeColor(color: Int) {
        baseColor = color
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        darkShadowColor = Color.argb(150, Math.max(0, red - 50), Math.max(0, green - 50), Math.max(0, blue - 50))
        lightHighlightColor = Color.argb(200, Math.min(255, red + 50), Math.min(255, green + 50), Math.min(255, blue + 50))
        accentColor = Color.rgb(Math.max(0, red - 30), Math.max(0, green - 30), Math.max(0, blue - 30))
        val iconAndTextColor = Color.rgb(Math.max(0, red - 100), Math.max(0, green - 100), Math.max(0, blue - 100))
        textColor = iconAndTextColor
        iconColor = iconAndTextColor

        setBackgroundColor(baseColor)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val padding = dpToPx(30.0f).toInt()
        val innerWidth = width - padding * 2
        val paddingTop = padding.toFloat()

        // Main card rect
        drawNeumorphicRect(canvas, paddingTop, paddingTop, innerWidth.toFloat(), (height - padding).toFloat(), dpToPx(30.0f), baseColor, darkShadowColor, lightHighlightColor, dpToPx(10.0f), true)

        // Central circle
        val circleRadius = dpToPx(100.0f)
        val centerX = width / 2.0f
        val circleCenterY = paddingTop + dpToPx(100.0f)
        drawNeumorphicCircle(canvas, centerX, circleCenterY, circleRadius, baseColor, lightHighlightColor, darkShadowColor, dpToPx(10.0f), false)

        // Chapter name
        paint.color = textColor
        paint.textSize = dpToPx(30.0f)
        paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("الكَهْف", centerX, circleCenterY + paint.textSize / 3.0f, paint)

        // Surah transliteration
        paint.textSize = dpToPx(20.0f)
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val circleBottom = circleCenterY + circleRadius
        canvas.drawText("Al- Kahfi", width / 2.0f, circleBottom + dpToPx(40.0f), paint)

        // Reciter name
        paint.textSize = dpToPx(16.0f)
        canvas.drawText("Ustadz : Muzammil Hasballah", width / 2.0f, circleBottom + dpToPx(65.0f), paint)

        // Progress bar
        val barWidth = innerWidth - dpToPx(60.0f)
        val barHeight = dpToPx(20.0f)
        val barLeft = paddingTop + dpToPx(30.0f)
        val barTop = circleBottom + dpToPx(115.0f)
        val barRadius = dpToPx(10.0f)
        drawNeumorphicRect(canvas, barLeft, barTop, barWidth, barHeight, barRadius, baseColor, darkShadowColor, lightHighlightColor, dpToPx(5.0f), true)

        // Progress fill (60%)
        paint.color = accentColor
        rectF.set(barLeft, barTop, barLeft + barWidth * 0.6f, barTop + barHeight)
        canvas.drawRoundRect(rectF, barRadius, barRadius, paint)

        // Control buttons
        val btnSize = dpToPx(60.0f)
        val btnRadius = dpToPx(20.0f)
        val btnSmallSpacing = dpToPx(20.0f)
        val btnTop = barTop + dpToPx(70.0f)

        // Rewind button (left)
        val rewindLeft = ((width / 2.0f) - btnSize - btnSmallSpacing - dpToPx(40.0f))
        drawNeumorphicRect(canvas, rewindLeft, btnTop, btnSize, btnSize, btnRadius, baseColor, darkShadowColor, lightHighlightColor, dpToPx(8.0f), true)
        paint.color = iconColor
        canvas.drawRect(rewindLeft + dpToPx(20.0f), btnTop + dpToPx(20.0f), rewindLeft + dpToPx(25.0f), btnTop + dpToPx(40.0f), paint)
        canvas.drawPath(createTrianglePath(rewindLeft + dpToPx(25.0f), btnTop + dpToPx(30.0f), dpToPx(20.0f), true), paint)

        // Pause button (center)
        val pauseLeft = (width / 2.0f) - btnSize / 2.0f
        drawNeumorphicRect(canvas, pauseLeft, btnTop, btnSize, btnSize, btnRadius, baseColor, darkShadowColor, lightHighlightColor, dpToPx(8.0f), true)
        paint.color = iconColor
        canvas.drawRect(pauseLeft + dpToPx(20.0f), btnTop + dpToPx(20.0f), pauseLeft + dpToPx(30.0f), btnTop + dpToPx(40.0f), paint)
        canvas.drawRect(pauseLeft + dpToPx(35.0f), btnTop + dpToPx(20.0f), pauseLeft + dpToPx(45.0f), btnTop + dpToPx(40.0f), paint)

        // Fast-forward button (right)
        val ffLeft = (width / 2.0f) + btnSmallSpacing + dpToPx(40.0f)
        drawNeumorphicRect(canvas, ffLeft, btnTop, btnSize, btnSize, btnRadius, baseColor, darkShadowColor, lightHighlightColor, dpToPx(8.0f), true)
        paint.color = iconColor
        canvas.drawRect(ffLeft + dpToPx(40.0f), btnTop + dpToPx(20.0f), ffLeft + dpToPx(45.0f), btnTop + dpToPx(40.0f), paint)
        canvas.drawPath(createTrianglePath(ffLeft + dpToPx(35.0f), btnTop + dpToPx(30.0f), dpToPx(20.0f), false), paint)

        // "Created by" footer
        paint.textSize = dpToPx(12.0f)
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val footerText = "Created by : story_rilla"
        val footerTextWidth = paint.measureText(footerText)
        val footerHeight = (height - padding).toFloat() - dpToPx(20.0f)
        val footerRectWidth = footerTextWidth + dpToPx(40.0f)
        val footerRectHeight = dpToPx(40.0f)
        drawNeumorphicRect(canvas, (width / 2.0f) - footerRectWidth / 2.0f, (height - padding).toFloat() - footerRectHeight, footerRectWidth, footerRectHeight, dpToPx(15.0f), baseColor, darkShadowColor, lightHighlightColor, dpToPx(5.0f), true)
        paint.color = iconColor
        canvas.drawText(footerText, width / 2.0f, footerHeight + dpToPx(25.0f), paint)

        // Top-left back button
        val topBtnSize = dpToPx(40.0f)
        val topBtnMargin = dpToPx(20.0f)
        drawNeumorphicRect(canvas, topBtnMargin, topBtnMargin, topBtnSize, topBtnSize, dpToPx(15.0f), baseColor, darkShadowColor, lightHighlightColor, dpToPx(5.0f), true)
        paint.color = iconColor
        val backPath = Path().apply {
            moveTo(dpToPx(25.0f) + topBtnMargin, dpToPx(15.0f) + topBtnMargin)
            lineTo(dpToPx(15.0f) + topBtnMargin, dpToPx(25.0f) + topBtnMargin)
            lineTo(dpToPx(25.0f) + topBtnMargin, dpToPx(35.0f) + topBtnMargin)
            close()
        }
        canvas.drawPath(backPath, paint)

        // Top-right settings button
        drawNeumorphicRect(canvas, (width - topBtnMargin) - topBtnSize, topBtnMargin, topBtnSize, topBtnSize, dpToPx(15.0f), baseColor, darkShadowColor, lightHighlightColor, dpToPx(5.0f), true)
        paint.color = iconColor
        val settingsLeft = (width - topBtnMargin) - topBtnSize
        canvas.drawCircle(settingsLeft + dpToPx(20.0f), topBtnMargin + dpToPx(20.0f), dpToPx(5.0f), paint)
        canvas.drawLine(settingsLeft + dpToPx(20.0f), topBtnMargin + dpToPx(20.0f), settingsLeft + dpToPx(35.0f), topBtnMargin + dpToPx(5.0f), paint)
        canvas.drawLine(settingsLeft + dpToPx(20.0f), topBtnMargin + dpToPx(20.0f), settingsLeft + dpToPx(35.0f), topBtnMargin + dpToPx(35.0f), paint)

        // Second top button (share/info)
        val secondBtnLeft = topBtnMargin + topBtnSize + dpToPx(10.0f)
        drawNeumorphicRect(canvas, secondBtnLeft, topBtnMargin, topBtnSize, topBtnSize, dpToPx(15.0f), baseColor, darkShadowColor, lightHighlightColor, dpToPx(5.0f), true)
        paint.color = iconColor
        canvas.drawCircle(secondBtnLeft + dpToPx(15.0f) + dpToPx(10.0f), topBtnMargin + dpToPx(10.0f) + dpToPx(20.0f), dpToPx(5.0f), paint)
        canvas.drawLine(secondBtnLeft + dpToPx(20.0f) + dpToPx(10.0f), topBtnMargin + dpToPx(10.0f) + dpToPx(10.0f), secondBtnLeft + dpToPx(20.0f) + dpToPx(10.0f), topBtnMargin + dpToPx(10.0f) + dpToPx(20.0f), paint)
        canvas.drawLine(secondBtnLeft + dpToPx(20.0f) + dpToPx(10.0f), topBtnMargin + dpToPx(10.0f) + dpToPx(10.0f), secondBtnLeft + dpToPx(35.0f) + dpToPx(10.0f), topBtnMargin + dpToPx(10.0f) + dpToPx(10.0f), paint)
    }

    private fun drawNeumorphicRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        cornerRadius: Float,
        baseColor: Int,
        darkShadow: Int,
        lightHighlight: Int,
        offset: Float,
        isPressed: Boolean
    ) {
        val right = left + width
        val bottom = top + height

        // First shadow layer
        paint.color = if (isPressed) darkShadow else lightHighlight
        rectF.set(left + offset, top + offset, right + offset, bottom + offset)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

        // Second shadow layer
        paint.color = if (isPressed) lightHighlight else darkShadow
        rectF.set(left - offset, top - offset, right - offset, bottom - offset)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)

        // Base fill
        paint.color = baseColor
        rectF.set(left, top, right, bottom)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
    }

    private fun drawNeumorphicCircle(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        baseColor: Int,
        lightHighlight: Int,
        darkShadow: Int,
        offset: Float,
        isPressed: Boolean
    ) {
        paint.color = if (isPressed) lightHighlight else darkShadow
        canvas.drawCircle(cx + offset, cy + offset, radius, paint)

        paint.color = if (isPressed) darkShadow else lightHighlight
        canvas.drawCircle(cx - offset, cy - offset, radius, paint)

        paint.color = baseColor
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    private fun createTrianglePath(cx: Float, cy: Float, size: Float, pointingLeft: Boolean): Path {
        val path = Path()
        val half = size / 2.0f

        if (pointingLeft) {
            val tipX = cx + half
            path.moveTo(tipX, cy - half)
            path.lineTo(cx - half, cy)
            path.lineTo(tipX, cy + half)
        } else {
            val tipX = cx - half
            path.moveTo(tipX, cy - half)
            path.lineTo(cx + half, cy)
            path.lineTo(tipX, cy + half)
        }

        path.close()
        return path
    }
}
