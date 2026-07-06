package hazem.nurmontage.videoquran.views.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import hazem.nurmontage.videoquran.views.TypefaceCache

class AyaCircleBg : AppCompatTextView {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init() }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        TypefaceCache.get(resources.assets, FONT_PATH)?.let { setTypeface(it) }
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()
        val textPaint = paint
        val textWidth = textPaint.measureText(text)
        val fontMetrics = textPaint.fontMetrics

        val contentSize = maxOf(textWidth, fontMetrics.descent - fontMetrics.ascent)
        val radius = contentSize / 2f + 20f

        val centerX = width / 2f
        val centerY = height / 2f
        val halfTextWidth = textWidth / 2f

        bgPaint.shader = LinearGradient(
            centerX - halfTextWidth, centerY,
            centerX + halfTextWidth, centerY,
            intArrayOf(
                Color.parseColor("#B7833AB4"),
                Color.parseColor("#E1306C"),
                Color.parseColor("#BCF58529")
            ),
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(centerX, centerY, radius, bgPaint)
        super.onDraw(canvas)
    }

    companion object {
        private const val FONT_PATH = "fonts/arabic/محمدي.ttf"
    }
}
