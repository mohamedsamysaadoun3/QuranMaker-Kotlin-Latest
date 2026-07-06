package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import hazem.nurmontage.videoquran.utils.LocaleHelper
import java.util.Locale

/**
 * Custom progress bar that draws a square outline (rounded rectangle)
 * with a gradient-filled partial path showing progress.
 *
 * The outline path is constructed manually with arcs at each corner.
 * A [PathMeasure] is used to extract a segment of the outline path
 * proportional to the current progress, which is then drawn with a
 * gradient shader.
 *
 * Also displays a percentage label and a hint text in the center.
 *
 * Originally: SquareOutlineProgressBar.java (213 lines)
 */
class SquareOutlineProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cornerRadius: Float = 0f
    private var gradientColors: IntArray = intArrayOf(
        Color.parseColor("#a8ce46"),
        Color.parseColor("#D2DE49"),
        Color.parseColor("#F4D853")
    )
    private var maxProgress: Int = 100
    private val partialPath: Path = Path()
    private val path: Path = Path()
    private val pathMeasure: PathMeasure = PathMeasure()
    var progress: Int = 0
        set(value) {
            val clamped = Math.max(0, Math.min(value, maxProgress))
            if (field != clamped) {
                field = clamped
                invalidate()
            }
        }
    private val progressPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var progressShader: LinearGradient? = null
    private val rect: RectF = RectF()
    private var strHint: String
    private var strokeWidth: Float = 0f
    private val textPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -1
    }
    private var trackColor: Int = 587202559
    private val trackPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = trackColor
    }
    private var xH: Float = 0f
    private var xP: Float = 0f
    private var y_hint: Float = 0f
    private var y_progrees: Float = 0f

    init {
        val typeface = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
        textPaint.typeface = typeface
        strHint = if (LocaleHelper.getLanguage(context) == "ar") {
            "يرجى عدم قفل الشاشة أو التبديل إلى تطبيقات أخرى."
        } else {
            "Please don't lock the screen or switch to other apps."
        }
    }

    var max: Int
        get() = maxProgress
        set(value) {
            maxProgress = Math.max(1, value)
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension((size / 1.618034f).toInt(), size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader(w, h)

        val wF = w.toFloat()
        textPaint.textSize = 0.033f * wF
        val textRect = Rect()
        cornerRadius = 0.04f * wF
        val sw = wF * 0.0085f
        strokeWidth = sw
        trackPaint.strokeWidth = sw
        progressPaint.strokeWidth = strokeWidth

        textPaint.getTextBounds(strHint, 0, strHint.length, textRect)
        val halfStroke = strokeWidth / 2.0f
        rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke)
        y_progrees = rect.centerY() - textRect.height()
        y_hint = rect.centerY() + textRect.height()
        xH = rect.centerX() - (textRect.width() * 0.5f)
        textPaint.getTextBounds("100", 0, 3, textRect)
        xP = rect.centerX() - (textRect.width() * 0.5f)
    }

    private fun updateShader(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), gradientColors, null, Shader.TileMode.CLAMP)
        progressShader = shader
        progressPaint.shader = shader
    }



    fun setMaxProgress(max: Int) {
        maxProgress = Math.max(1, max)
        invalidate()
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        strokeWidth = width
        trackPaint.strokeWidth = width
        progressPaint.strokeWidth = width
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        val cr = cornerRadius

        canvas.drawRoundRect(rect, cr, cr, trackPaint)
        canvas.drawText(String.format(Locale.US, "%% %d", progress), xP, y_progrees, textPaint)
        canvas.drawText(strHint, xH, y_hint, textPaint)

        path.reset()
        partialPath.reset()

        val startArcX = left + cr
        path.moveTo(startArcX, top)
        path.lineTo(right - cr, top)

        val doubleCR = 2.0f * cr
        val rightArcLeft = right - doubleCR
        val topArcBottom = top + doubleCR
        path.arcTo(RectF(rightArcLeft, top, right, topArcBottom), -90.0f, 90.0f, false)
        path.lineTo(right, bottom - cr)

        val bottomArcTop = bottom - doubleCR
        path.arcTo(RectF(rightArcLeft, bottomArcTop, right, bottom), 0.0f, 90.0f, false)
        path.lineTo(left + cr, bottom)

        val leftArcRight = doubleCR + left
        path.arcTo(RectF(left, bottomArcTop, leftArcRight, bottom), 90.0f, 90.0f, false)
        path.lineTo(left, cr + top)
        path.arcTo(RectF(left, top, leftArcRight, topArcBottom), 180.0f, 90.0f, false)
        path.close()

        pathMeasure.setPath(path, false)
        pathMeasure.getSegment(0.0f, pathMeasure.length * (progress.toFloat() / maxProgress.toFloat()), partialPath, true)
        canvas.drawPath(partialPath, progressPaint)
    }
}
