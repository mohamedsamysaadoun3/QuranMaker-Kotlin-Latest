package hazem.nurmontage.videoquran.views.image

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.common.Common

open class SquareImageView : AppCompatImageView {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintRect = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var isSelect: Boolean = false
    private var number: String? = null
    private var anInt: Int = 0
    private var cx: Float = 0f
    private var cy: Float = 0f
    private var r: Float = 0f
    private var x: Float = 0f
    private var y: Float = 0f
    private var drawableDone: Drawable? = null

    fun getAnInt(): Int = anInt

    fun setNumber(number: Int) {
        if (number == 0) return
        this.anInt = number
        this.number = "" + number
        this.cx = (width * 0.5f) - (textPaint.measureText(this.number) * 0.5f)
    }

    fun isMSelect(): Boolean = isSelect

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init() }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        textPaint.color = -1
        textPaint.typeface = Typeface.createFromAsset(resources.assets, "fonts/" + Common.FONT_ENGLISH_APP)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        setMeasuredDimension(width, width)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        paintRect.color = -1056964608
        val f = w.toFloat()
        paint.strokeWidth = 0.02f * f
        if (!isSelect) {
            paint.color = -8355712
            paint.style = Paint.Style.STROKE
        } else {
            paint.color = -12190534
            paint.style = Paint.Style.FILL
        }
        textPaint.textSize = 0.25f * f
        val xPosition = 0.1f * f
        this.r = xPosition
        this.x = f - 1.2f * xPosition
        this.y = xPosition + paint.strokeWidth
        if (number != null) {
            cx = (width * 0.5f) - (textPaint.measureText(number) * 0.5f)
        }
        cy = height * 0.5f
        val i5 = (f * 0.3f).toInt()
        val width = (width * 0.5f).toInt()
        val f3 = cy
        val f4 = i5.toFloat()
        val rect = Rect(width - i5, (f3 - f4).toInt(), width + i5, (f3 + f4).toInt())
        val drawable = ContextCompat.getDrawable(context, R.drawable.check_24px)
        drawableDone = drawable
        drawable?.bounds = rect
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (paint == null || !isSelect) {
            return
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintRect)
        drawableDone?.draw(canvas)
        if (number != null) {
            canvas.drawText(number!!, cx, cy, textPaint)
        }
    }

    fun onSelect(selected: Boolean) {
        isSelect = selected
        if (!selected) {
            paint.color = -8355712
            paint.style = Paint.Style.STROKE
        } else {
            paint.color = -12190534
            paint.style = Paint.Style.FILL
        }
        invalidate()
    }
}
