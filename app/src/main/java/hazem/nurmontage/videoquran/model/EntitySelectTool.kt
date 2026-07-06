package hazem.nurmontage.videoquran.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.UtilsFileLast

class EntitySelectTool(canvasSize: Int, context: Context) {

    private val bitmapScale: Bitmap
    private val bitmapApplyAll: Bitmap
    private val rectFScale: RectF
    private val rectApplyAll: RectF
    private val paint: Paint
    private val offsetX: Float
    private val offsetY: Float
    private val offsetYApply: Float
    private val round: Float

    var isClick_apply: Boolean = false
        private set
    var isOnProgress: Boolean = false
        private set
    var isApply_Move: Boolean = false
        private set
    var isApply_Scale: Boolean = false
        private set
    var isApply_all: Boolean = false
        private set
    var isOnScale: Boolean = false
        private set

    init {
        val applyAllFont = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/خط الإبل.otf")
        val applyAllText = if (LocaleHelper.getLanguage(context) == "ar") "تطبيق على الكل" else "ApplyAll"

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        paint = p
        p.color = -0x6400B
        val f = canvasSize.toFloat()
        p.strokeWidth = 0.005f * f
        round = 0.02f * f

        val handleSize = (f * 0.047f).toInt()
        val handleSizeF = handleSize.toFloat()

        rectFScale = RectF(0f, 0f, handleSizeF, handleSizeF)
        rectApplyAll = RectF(0f, 0f, handleSize * 4f, rectFScale.height())

        offsetX = rectFScale.width() * 0.7f
        val halfStroke = paint.strokeWidth * 0.5f
        offsetY = halfStroke
        offsetYApply = halfStroke * 3.0f

        paint.style = Paint.Style.FILL
        val expandDrawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_expand)
        bitmapScale = Bitmap.createBitmap(handleSize, handleSize, Bitmap.Config.ARGB_8888)
        val scaleCanvas = Canvas(bitmapScale)
        scaleCanvas.drawCircle(
            scaleCanvas.width * 0.5f,
            scaleCanvas.height * 0.5f,
            handleSizeF * 0.5f,
            paint
        )
        val inset = (handleSizeF * 0.1f).toInt()
        val end = handleSize - inset
        expandDrawable?.setBounds(inset, inset, end, end)
        expandDrawable?.draw(scaleCanvas)

        bitmapApplyAll = Bitmap.createBitmap(
            rectApplyAll.width().toInt(),
            rectApplyAll.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        scaleCanvas.setBitmap(bitmapApplyAll)
        val cornerRadius = (rectApplyAll.height() * 0.2f).toInt().toFloat()
        scaleCanvas.drawRoundRect(rectApplyAll, cornerRadius, cornerRadius, paint)

        paint.style = Paint.Style.FILL
        paint.color = -0xDE4E4E
        paint.typeface = applyAllFont
        val maxTextWidth = rectApplyAll.width() * 0.8f
        val maxTextHeight = rectApplyAll.height() * 0.6f
        paint.textSize = 100f
        val textBounds = Rect()
        paint.getTextBounds(applyAllText, 0, applyAllText.length, textBounds)
        paint.textSize = Math.min(maxTextWidth / textBounds.width(), maxTextHeight / textBounds.height()) * 100f
        paint.getTextBounds(applyAllText, 0, applyAllText.length, textBounds)
        scaleCanvas.drawText(
            applyAllText,
            rectApplyAll.centerX() - textBounds.width() * 0.5f,
            rectApplyAll.centerY() - textBounds.exactCenterY(),
            paint
        )

        paint.color = -0x6400B
        paint.style = Paint.Style.STROKE
    }

    fun setClick_apply(click: Boolean) { isClick_apply = click }
    fun setOnProgress(onProgress: Boolean) { isOnProgress = onProgress }

    fun setApply_Move(move: Boolean) {
        isApply_Move = move
        if (move) setApply_Scale(false)
    }

    fun setApply_Scale(scale: Boolean) {
        isApply_Scale = scale
        if (scale) setApply_Move(false)
    }

    fun setApply_all(applyAll: Boolean) { isApply_all = applyAll }

    fun isApply(entityView: EntityView, x: Float, y: Float): Boolean {
        if (!isApply_all) return false
        rectApplyAll.left = entityView.rect.right - bitmapApplyAll.width
        rectApplyAll.right = entityView.rect.right
        rectApplyAll.top = entityView.rect.top - bitmapApplyAll.height - offsetYApply
        rectApplyAll.bottom = entityView.rect.top
        return rectApplyAll.contains(x, y)
    }

    fun isScale(entityView: EntityView, x: Float, y: Float): Boolean {
        if (entityView is TranslationQuranEntity) {
            rectFScale.top = entityView.rect.top - offsetY * 2.0f
            rectFScale.left = entityView.rect.left - offsetX
        } else {
            rectFScale.left = entityView.rect.left - offsetX * 2.0f
            rectFScale.top = entityView.rect.bottom - offsetY * 2.0f
        }
        rectFScale.right = rectFScale.left + bitmapScale.width * 1.5f
        rectFScale.bottom = rectFScale.top + bitmapScale.height * 1.5f
        val contains = rectFScale.contains(x, y)
        isOnScale = contains
        setApply_Scale(contains)
        return isOnScale
    }

    fun draw(canvas: Canvas, entityView: EntityView) {
        val rect = entityView.rect
        canvas.drawRoundRect(rect, round, round, paint)
        if (entityView is TranslationQuranEntity) {
            canvas.drawBitmap(bitmapScale, entityView.rect.left, entityView.rect.top - offsetY, null)
        } else {
            canvas.drawBitmap(bitmapScale, entityView.rect.left - offsetX, entityView.rect.bottom - offsetY, null)
        }
        if (isApply_all) {
            canvas.drawBitmap(
                bitmapApplyAll,
                entityView.rect.right - bitmapApplyAll.width,
                entityView.rect.top - bitmapApplyAll.height - offsetYApply,
                null
            )
        }
    }

    fun reset() {
        setApply_Move(false)
        setApply_Scale(false)
        setApply_all(false)
    }
}
