package hazem.nurmontage.videoquran.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class EyeOpenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private val clearPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var currentRY: Float = 0.0f
    private val eyePath: Path = Path()
    private val eyeRect: RectF = RectF()
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var radiusX: Float = 0f
    private var radiusYFull: Float = 0f
    @Suppress("unused")
    private var wrapOffset: Float = 50.0f

    init {
        clearPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        invalidate()
    }

    fun startEyeOpenAnimation(duration: Long) {
        val animator = ValueAnimator.ofFloat(0.0f, radiusYFull)
        animator.duration = duration
        animator.addUpdateListener { animation ->
            currentRY = animation.animatedValue as Float
            invalidate()
        }
        animator.repeatCount = 5
        animator.start()
    }

    private fun map(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
        return outMin + ((value - inMin) / (inMax - inMin)) * (outMax - outMin)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val wF = w.toFloat()
        centerX = wF / 2.0f
        val hF = h.toFloat()
        centerY = hF / 2.0f
        radiusX = wF * 0.4f
        radiusYFull = hF * 0.2f
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        canvas.drawBitmap(bmp, 0.0f, 0.0f, paint)

        // Clear an oval area (the "eye opening")
        eyePath.reset()
        eyeRect.set(
            centerX - radiusX, centerY - currentRY,
            centerX + radiusX, centerY + currentRY
        )
        eyePath.addOval(eyeRect, Path.Direction.CW)
        canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG)
        canvas.drawPath(eyePath, clearPaint)
        canvas.restore()

        // Draw upper portion of bitmap, scaled to compress as eye opens
        val upperScale = map(currentRY, 0.0f, radiusYFull, 1.0f, 0.0f)
        canvas.save()
        canvas.clipRect(0.0f, 0.0f, width.toFloat(), centerY - currentRY)
        canvas.scale(1.0f, upperScale, centerX, centerY - currentRY)
        canvas.drawBitmap(bmp, 0.0f, 0.0f, paint)
        canvas.restore()

        // Draw lower portion of bitmap, scaled to compress as eye opens
        val lowerScale = map(currentRY, 0.0f, radiusYFull, 1.0f, 0.0f)
        canvas.save()
        canvas.clipRect(0.0f, centerY + currentRY, width.toFloat(), height.toFloat())
        canvas.scale(1.0f, lowerScale, centerX, centerY + currentRY)
        canvas.drawBitmap(bmp, 0.0f, 0.0f, paint)
        canvas.restore()
    }
}
