package hazem.nurmontage.videoquran.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.max

object ArtistLightEffect {

    /**
     * Apply an artistic light effect to the given bitmap.
     * Adds color grading, radial light overlay, and vignette.
     *
     * @param bitmap Source bitmap
     * @param lightX X position of the light source (0 to width)
     * @param lightY Y position of the light source (0 to height)
     * @return New bitmap with the light effect applied
     */
    @JvmStatic
    fun apply(bitmap: Bitmap, lightX: Float, lightY: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw original bitmap
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Apply color matrix for warm tone grading
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.05f, 0.0f, 0.0f, 0.0f, -6.0f,
            0.0f, 1.02f, 0.0f, 0.0f, -4.0f,
            0.0f, 0.0f, 0.95f, 0.0f, 10.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        ))
        val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        colorPaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(result, 0f, 0f, colorPaint)

        val w = width.toFloat()
        val h = height.toFloat()

        // Overlay: Radial gradient light from the source point
        val saveLayer1 = canvas.saveLayer(0f, 0f, w, h, null)
        val radialGradient = RadialGradient(
            lightX, lightY,
            max(width, height) * 0.45f,
            intArrayOf(Color.parseColor("#8844FFAA"), Color.parseColor("#33226655"), 0),
            floatArrayOf(0.0f, 0.55f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        overlayPaint.shader = radialGradient
        overlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        canvas.drawRect(0f, 0f, w, h, overlayPaint)
        canvas.restoreToCount(saveLayer1)

        // Add: Focused light spot
        val saveLayer2 = canvas.saveLayer(0f, 0f, w, h, null)
        val spotGradient = RadialGradient(
            lightX, lightY,
            max(width, height) * 0.25f,
            intArrayOf(Color.parseColor("#5533FFAA"), 0),
            null,
            Shader.TileMode.CLAMP
        )
        val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        spotPaint.shader = spotGradient
        spotPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        canvas.drawRect(0f, 0f, w, h, spotPaint)
        canvas.restoreToCount(saveLayer2)

        // Vignette: Darken edges
        val saveLayer3 = canvas.saveLayer(0f, 0f, w, h, null)
        val vignetteGradient = RadialGradient(
            w / 2.0f, h / 2.0f,
            max(width, height).toFloat(),
            intArrayOf(0, Color.parseColor("#44000000")),
            floatArrayOf(0.6f, 1.0f),
            Shader.TileMode.CLAMP
        )
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        vignettePaint.shader = vignetteGradient
        vignettePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        canvas.drawRect(0f, 0f, w, h, vignettePaint)
        canvas.restoreToCount(saveLayer3)

        return result
    }
}
