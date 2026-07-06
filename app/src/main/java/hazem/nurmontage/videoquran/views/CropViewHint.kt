package hazem.nurmontage.videoquran.views

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.FutureTarget
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.ScreenUtils
import java.util.concurrent.ExecutionException
class CropViewHint @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var arrowHeadSize: Int = 0
    private val arrowPaint: Paint = Paint().apply {
        color = 0xFFFF0000.toInt() // SupportMenu.CATEGORY_MASK
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private var bitmap: Bitmap? = null
    private val cropPaint: Paint = Paint().apply {
        color = -15605
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
        isAntiAlias = true
    }
    private var cropRect: RectF? = null
    private var endLineX: Float = 0f
    private var endLineX_arrow: Float = 0f
    private var endLineY: Float = 0f
    private var endLineY_arrow: Float = 0f
    private var endX: Float = 0f
    private var endY: Float = 0f
    private val imagePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ipadBitmap: Bitmap? = null
    private val linePaint: Paint = Paint().apply {
        color = 0xFFFF0000.toInt() // SupportMenu.CATEGORY_MASK
        strokeWidth = 5.0f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val mTextRect: Rect = Rect()
    private val mTittle: String
    private var radius: Float = 0f
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var x_text: Float = 0f
    private var y_bitmap: Float = 0f
    private var y_text: Float = 0f

    init {
        textPaint.color = -1 // white
        if (LocaleHelper.getLanguage(context) == "ar") {
            mTittle = "تحكم في شاشة الآيبود"
            textPaint.typeface = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
        } else {
            mTittle = "iPod screen selection"
            textPaint.typeface = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
        }

        val screenWidth = (ScreenUtils.getScreenWidth(getActivity(context)!!) * 0.52f).toInt()
        Thread {
            try {
                bitmap = get(context, screenWidth, screenWidth, R.drawable.bg_13)
                invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = View.MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)

        val sizeF = size.toFloat()
        linePaint.strokeWidth = 0.0085f * sizeF
        textPaint.textSize = sizeF * 0.045f
        textPaint.getTextBounds(mTittle, 0, mTittle.length, mTextRect)
        x_text = (size - mTextRect.width()) * 0.5f
        val textHeight = mTextRect.height() * 1.2f
        y_text = textHeight
        y_bitmap = textHeight + mTextRect.height()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ViewCompat.MEASURED_STATE_MASK)

        val bmp = bitmap
        if (bmp != null) {
            canvas.drawBitmap(bmp, 0.0f, y_bitmap, imagePaint)
            canvas.drawText(mTittle, x_text, y_text, textPaint)

            val ipad = ipadBitmap
            if (ipad != null) {
                if (cropRect == null) {
                    val cropLeft = bmp.width * 0.25f
                    val cropTop = (bmp.height * 0.08f) + y_bitmap
                    val cropWidth = bmp.width * 0.35f
                    val cropHeight = bmp.height * 0.43f
                    val rect = RectF(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)
                    cropRect = rect
                    radius = Math.min(rect.width(), rect.height()) * 0.10800001f
                    arrowHeadSize = (bmp.width * 0.1f).toInt()
                    endX = (width - ipad.width).toFloat()
                    endY = (y_bitmap + bmp.height).toFloat()
                    val height2 = ipad.height * 0.28f
                    val width2 = ipad.width * 0.3f
                    endLineY_arrow = endY + height2
                    endLineX_arrow = endX + width2
                    endLineY = endY * 0.98f + height2
                    endLineX = endX * 0.98f + width2
                }
                val cr = cropRect ?: return
                canvas.drawRoundRect(cr, radius, radius, cropPaint)
                canvas.drawBitmap(ipad, endX, endY, imagePaint)
                canvas.drawLine(cr.centerX(), cr.centerY(), endLineX, endLineY, linePaint)
                drawArrowHead(canvas, endLineX_arrow, endLineY_arrow, 0.0f, 0.0f)
            }
        }
    }

    private fun drawArrowHead(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        val angle = Math.atan2((y1 - y2).toDouble(), (x1 - x2).toDouble())
        val x1d = x1.toDouble()
        val y1d = y1.toDouble()
        val angleLeft = angle - 0.5235987755982988
        val leftX = (x1d - arrowHeadSize * Math.cos(angleLeft)).toFloat()
        val leftY = (y1d - arrowHeadSize * Math.sin(angleLeft)).toFloat()
        val angleRight = angle + 0.5235987755982988
        val rightX = (x1d - arrowHeadSize * Math.cos(angleRight)).toFloat()
        val rightY = (y1d - arrowHeadSize * Math.sin(angleRight)).toFloat()

        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(leftX, leftY)
            lineTo(rightX, rightY)
            close()
        }
        canvas.drawPath(path, arrowPaint)
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
