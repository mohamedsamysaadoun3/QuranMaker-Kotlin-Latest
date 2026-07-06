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

class NeumorphicRectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cornerRadius: Float = 40.0f
    private var shadowOffset: Float = 20.0f
    private var baseColor: Int = Color.parseColor("#398B89")
    private var lightShadowColor: Int = Color.parseColor("#57A9A7")
    private var darkShadowColor: Int = Color.parseColor("#1B6D6B")

    private val backgroundPaint: Paint
    private val lightShadowPaint: Paint
    private val darkShadowPaint: Paint
    private val rect: RectF = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            style = Paint.Style.FILL
        }

        lightShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            style = Paint.Style.FILL
            setShadowLayer(shadowOffset, -shadowOffset, -shadowOffset, lightShadowColor)
        }

        darkShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            style = Paint.Style.FILL
            setShadowLayer(shadowOffset, shadowOffset, shadowOffset, darkShadowColor)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val offset = shadowOffset
        rect.set(offset * 1.5f, offset * 1.5f, w - offset * 1.5f, h - offset * 1.5f)

        backgroundPaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            Color.argb(
                255,
                (Color.red(baseColor) * 1.1f).toInt(),
                (Color.green(baseColor) * 1.1f).toInt(),
                (Color.blue(baseColor) * 1.1f).toInt()
            ),
            Color.argb(
                255,
                (Color.red(baseColor) * 0.9f).toInt(),
                (Color.green(baseColor) * 0.9f).toInt(),
                (Color.blue(baseColor) * 0.9f).toInt()
            ),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, lightShadowPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, darkShadowPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
    }
}
