package hazem.nurmontage.videoquran.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.media3.common.C
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.utils.AppUtils
import java.util.concurrent.ExecutionException

class EyeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var background: Bitmap? = null
    private var centerX: Float = 0f
    private var centerY: Float = 0f
    private var eye: Bitmap? = null
    private var eyeCenterY: Float = 0f
    private var eyeHeight: Float = 0f
    private var eyeProgress: Float = 0.0f
    private val eyeRect: RectF = RectF()
    private var eyeWidth: Float = 0f
    private var lidBottomY: Float = 0f
    private var lidTopY: Float = 0f
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun createEyePath(cx: Float, cy: Float, width: Float, height: Float): Path {
        val path = Path()
        val halfW = width / 2.0f
        val left = cx - halfW
        path.moveTo(left, cy)
        val halfH = height / 2.0f
        path.quadTo(cx, cy - halfH, halfW + cx, cy)
        path.quadTo(cx, halfH + cy, left, cy)
        path.close()
        return path
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        eye = bmp
        bmp.eraseColor(-16711936) // green
    }

    fun setBackground(bitmap: Bitmap?) {
        this.background = bitmap
        invalidate()
    }

    fun setEyeProgress(progress: Float) {
        val bg = this.background ?: return
        this.eyeProgress = progress
        this.eyeWidth = bg.width * 0.8f
        this.eyeHeight = bg.height * 0.6f * progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bitmap = this.background ?: return

        val drawPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var halfFactor = 2.0f
        var halfHeight = getHeight() / 2.0f
        var halfWidth = getWidth() / 2.0f
        val eyeW = getWidth() * 0.6f
        val eyeH = getHeight() * 0.6f * eyeProgress
        var useZ2 = false

        canvas.drawBitmap(bitmap, 0.0f, 0.0f, drawPaint)

        val eyePath = createEyePath(halfWidth, halfHeight, eyeW, eyeH)

        var i = 0
        while (i <= 60) {
            val x = (getWidth() * i) / 60f
            val halfEyeW = eyeW / halfFactor
            val offset = x - halfWidth

            if (Math.abs(offset) > halfEyeW) {
                // Outside the eye shape, no distortion needed
            } else {
                val parabolic = (eyeH / halfFactor) * (1.0f - (offset * offset) / (halfEyeW * halfEyeW))
                val shiftedCenter = halfHeight + parabolic
                val nextI = i + 1

                val srcTop = Rect(
                    (bitmap.width * i) / 60, 0,
                    (bitmap.width * nextI) / 60, bitmap.height / 2
                )
                val srcBottom = Rect(
                    (bitmap.width * i) / 60, bitmap.height / 2,
                    (nextI * bitmap.width) / 60, bitmap.height
                )

                val dstTop = RectF(x, 0.0f, (getWidth() / 60f) + x, halfHeight - parabolic)
                val dstBottom = RectF(x, shiftedCenter, (getWidth() / 60f) + x, getHeight().toFloat())

                canvas.drawBitmap(bitmap, srcTop, dstTop, drawPaint)
                canvas.drawBitmap(bitmap, srcBottom, dstBottom, drawPaint)
            }
            i++
            halfFactor = 2.0f
        }

        // Clear the eye shape from canvas
        val saveLayer = canvas.saveLayer(0.0f, 0.0f, getWidth().toFloat(), getHeight().toFloat(), null)
        drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        canvas.drawPath(eyePath, drawPaint)
        drawPaint.xfermode = null
        canvas.restoreToCount(saveLayer)
    }

    companion object {
        @Throws(ExecutionException::class, InterruptedException::class)
        @JvmStatic
        fun get(context: Context, width: Int, height: Int, drawableRes: Int): Bitmap {
            val futureTarget: FutureTarget<Bitmap> = Glide.with(context)
                .asBitmap()
                .load(drawableRes)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .signature(ObjectKey(AppUtils.getAppVersionName(context)))
                .override(width, height)
                .centerInside()
                .submit()
            val bitmap = futureTarget.get().copy(Bitmap.Config.ARGB_8888, true)
            Glide.with(context).clear(futureTarget)
            return bitmap
        }
    }

    fun openEye() {
        val animator = ValueAnimator.ofFloat(0.0f, 1.0f)
        animator.duration = C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS
        animator.addUpdateListener { animation ->
            eyeProgress = animation.animatedValue as Float
            invalidate()
        }
        animator.repeatCount = 5
        animator.start()
    }
}
