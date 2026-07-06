package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class GradientProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress: Int = 0
    private var maxProgress: Int = 100
    private var trackColor: Int = -1
    private var gradientColors: IntArray = intArrayOf(
        Color.parseColor("#a8ce46"),
        Color.parseColor("#D2DE49"),
        Color.parseColor("#F4D853")
    )
    private var cornerRadius: Float = 100.0f

    private val trackPaint: Paint = Paint().apply {
        color = trackColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val progressPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    private val trackRect: RectF = RectF()
    private val progressRect: RectF = RectF()
    private var progressShader: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        createProgressShader()
    }

    private fun createProgressShader() {
        if (width <= 0 || height <= 0) return
        val shader = LinearGradient(0f, 0f, width.toFloat(), 0f, gradientColors, null, Shader.TileMode.CLAMP)
        progressShader = shader
        progressPaint.shader = shader
    }

    fun setProgress(progress: Int) {
        this.progress = when {
            progress < 0 -> 0
            progress > maxProgress -> maxProgress
            else -> progress
        }
        invalidate()
    }

    fun getProgress(): Int = progress

    fun setMax(max: Int) {
        this.maxProgress = max
        invalidate()
    }

    fun getMax(): Int = maxProgress

    fun setTrackColor(color: Int) {
        trackColor = color
        trackPaint.color = color
        invalidate()
    }

    fun setGradientColors(colors: IntArray?) {
        if (colors == null || colors.isEmpty()) return
        gradientColors = colors
        createProgressShader()
        invalidate()
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        trackRect.set(0f, 0f, viewWidth, viewHeight)
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        progressRect.set(0f, 0f, viewWidth * (progress.toFloat() / maxProgress.toFloat()), viewHeight)
        canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
    }
}
