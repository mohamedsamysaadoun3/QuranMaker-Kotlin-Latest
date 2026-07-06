package hazem.nurmontage.videoquran.utils.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.pow
import kotlin.math.sqrt

object CinematicProcessor {

    fun applyCinematicEffect(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null
        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        applyColorGrade(canvas, copy, paint)
        applyVignette(canvas, copy)
        return copy
    }

    fun createGlassRect(bitmap: Bitmap, margin: Int): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1 }
        val rect = RectF(
            margin.toFloat(), margin.toFloat(),
            (bitmap.width - margin).toFloat(), (bitmap.height - margin).toFloat()
        )
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rect, 40f, 40f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        paint.xfermode = null
        return result
    }

    private fun applyColorGrade(canvas: Canvas, bitmap: Bitmap, paint: Paint) {
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1.1f, 0.1f, 0.0f, 0.0f, -10.0f,
                0.0f, 1.0f, 0.0f, 0.0f, -10.0f,
                0.0f, 0.1f, 1.2f, 0.0f, -10.0f,
                0.0f, 0.0f, 0.0f, 1.0f,   0.0f
            ))
        }
        val saturationMatrix = ColorMatrix().apply {
            setSaturation(0.85f)
        }
        colorMatrix.postConcat(saturationMatrix)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
    }

    private fun applyVignette(canvas: Canvas, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val halfW = width.toFloat()
        val halfH = height.toFloat()
        val radius = (sqrt(width.toDouble().pow(2.0) + height.toDouble().pow(2.0)) * 0.7).toFloat()
        val radialGradient = RadialGradient(
            halfW / 2.0f, halfH / 2.0f, radius,
            intArrayOf(0, 0, -1728053248),
            floatArrayOf(0.0f, 0.4f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply {
            isAntiAlias = true
            shader = radialGradient
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawRect(0f, 0f, halfW, halfH, paint)
    }
}
