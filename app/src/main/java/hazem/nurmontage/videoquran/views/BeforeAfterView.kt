package hazem.nurmontage.videoquran.views

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.ScreenUtils
import java.util.concurrent.ExecutionException

class BeforeAfterView : View {

    private var afterImage: Bitmap? = null
    private var beforeImage: Bitmap? = null
    private val circlePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0000.toInt()
        style = Paint.Style.FILL
    }
    private var circleRadius: Float = 0f
    private var dividerX: Float = -1.0f
    private var hintAnimator: ValueAnimator? = null
    private val imagePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isShowTxt: Boolean = false
    private var isStartAnim: Boolean = false
    private val linePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFF0000.toInt()
    }
    private var textPaint: Paint? = null
    private var txt: String? = null
    private var x_text: Float = 0f
    private var y_text: Float = 0f

    constructor(context: Context) : super(context) {
        dividerX = -1.0f
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        dividerX = -1.0f
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        dividerX = -1.0f
        init(context)
    }

    private fun init(context: Context) {
        ScreenUtils.getScreenWidth(getActivity(context)!!)
        Thread {
            try {
                addTextPaint(beforeImage, afterImage, context)
                invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun setTxt(txt: String?) {
        this.txt = txt
    }

    fun showText(size: Int) {
        isShowTxt = true
        val paint = Paint()
        textPaint = paint
        paint.typeface = Typeface.createFromAsset(resources.assets, "fonts/arabic/فرشة.ttf")
        paint.textSize = calculateTextSize(txt!!, size, paint)
    }

    fun calculateTextSize(text: String, maxSize: Int, paint: Paint): Float {
        var size = 400.0f
        paint.textSize = size
        val rect = Rect()
        paint.getTextBounds(text, 0, text.length, rect)
        while (rect.width() > maxSize || rect.height() > maxSize) {
            size -= 1.0f
            paint.textSize = size
            paint.getTextBounds(text, 0, text.length, rect)
        }
        val half = maxSize / 2.0f
        x_text = half - (rect.width() / 2.0f)
        y_text = half + (rect.height() / 2.0f)
        return size
    }

    fun isShowTxt(): Boolean = isShowTxt

    fun addTextPaint(before: Bitmap?, after: Bitmap?, context: Context) {
        val bmp = before ?: return
        val bmp2 = after ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = bmp.width * 0.025f
        paint.typeface = Typeface.createFromAsset(resources.assets, "fonts/arabic/" + Common.FONT_ENGLISH_APP)
        val canvas = Canvas()
        val leftMargin = bmp.width * 0.04f
        val topMargin = bmp.height * 0.025f

        if (LocaleHelper.getLanguage(context) == "ar") {
            paint.color = -7829368
            canvas.setBitmap(bmp)
            canvas.drawText("قبل", leftMargin, topMargin, paint)
            canvas.setBitmap(bmp2)
            val rightX = (bmp.width - paint.measureText("بعد")) - leftMargin
            paint.color = -15605
            canvas.drawText("بعد", rightX, topMargin, paint)
        } else {
            paint.color = -7829368
            canvas.setBitmap(bmp)
            canvas.drawText("BEFORE", leftMargin, topMargin, paint)
            canvas.setBitmap(bmp2)
            val afterWidth = paint.measureText("AFTER")
            paint.color = -15605
            canvas.drawText("AFTER", (bmp.width - afterWidth) - leftMargin, topMargin, paint)
        }
    }

    private fun initHintAnimation(size: Int) {
        val existing = hintAnimator
        if (existing != null && existing.isRunning) return
        val start = dividerX
        val animator = ValueAnimator.ofFloat(start, (size * 0.065f) + start)
        hintAnimator = animator
        animator.duration = 700L
        animator.repeatMode = ValueAnimator.REVERSE
        animator.repeatCount = -1
        animator.addUpdateListener { animation ->
            dividerX = animation.animatedValue as Float
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
        val sizeF = size.toFloat()
        dividerX = sizeF / 2.0f
        val radius = sizeF * 0.05f
        circleRadius = radius
        linePaint.strokeWidth = radius * 0.1f
        initHintAnimation(size)
    }

    fun release() {
        afterImage?.let { if (!it.isRecycled) it.recycle() }
        beforeImage?.let { if (!it.isRecycled) it.recycle() }
    }

    fun setBeforeImage(bitmap: Bitmap?) {
        beforeImage?.let { if (!it.isRecycled) it.recycle() }
        beforeImage = bitmap
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ViewCompat.MEASURED_STATE_MASK)

        if (isShowTxt) {
            canvas.drawColor(-1)
            canvas.save()
            canvas.clipRect(dividerX, 0.0f, width.toFloat(), height.toFloat())
            canvas.drawText(txt!!, x_text, y_text, imagePaint)
            canvas.restore()
            val dx = dividerX
            canvas.drawLine(dx, 0.0f, dx, height.toFloat(), linePaint)
            canvas.drawCircle(dividerX, height / 2.0f, circleRadius, circlePaint)
            drawArrows(canvas, dividerX, height / 2.0f)
        } else {
            val before = beforeImage
            val after = afterImage
            if (before != null && after != null) {
                canvas.drawBitmap(before, 0.0f, 0.0f, imagePaint)
                canvas.save()
                canvas.clipRect(dividerX, 0.0f, width.toFloat(), height.toFloat())
                canvas.drawBitmap(after, 0.0f, 0.0f, imagePaint)
                canvas.restore()
                val dx = dividerX
                canvas.drawLine(dx, 0.0f, dx, height.toFloat(), linePaint)
                canvas.drawCircle(dividerX, height / 2.0f, circleRadius, circlePaint)
                drawArrows(canvas, dividerX, height / 2.0f)
            }
        }

        if (!isStartAnim) {
            hintAnimator?.start()
            isStartAnim = true
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isStartAnim && event.action == MotionEvent.ACTION_DOWN) {
            hintAnimator?.cancel()
        }
        if (event.action != MotionEvent.ACTION_MOVE) {
            return true
        }
        dividerX = event.x
        invalidate()
        return true
    }

    private fun drawArrows(canvas: Canvas, cx: Float, cy: Float) {
        val arrowSize = circleRadius / 3.0f
        val halfR = circleRadius / 2.0f

        val leftPath = Path().apply {
            moveTo(cx - halfR, cy)
            lineTo((cx - halfR) + arrowSize, cy - arrowSize)
            lineTo((cx - halfR) + arrowSize, cy + arrowSize)
            close()
        }

        val rightPath = Path().apply {
            moveTo(halfR + cx, cy)
            lineTo((halfR + cx) - arrowSize, cy - arrowSize)
            lineTo((cx + halfR) - arrowSize, cy + arrowSize)
            close()
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -1
            style = Paint.Style.FILL
        }
        canvas.drawPath(leftPath, paint)
        canvas.drawPath(rightPath, paint)
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

        @JvmStatic
        fun getActivity(context: Context): Activity? {
            return if (context is ContextWrapper && context is Activity) {
                context as Activity
            } else {
                null
            }
        }
    }
}
