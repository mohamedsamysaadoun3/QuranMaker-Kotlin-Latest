package hazem.nurmontage.videoquran.utils

import android.graphics.LinearGradient
import android.graphics.RectF
import android.graphics.Shader

object CreateGradient {

    fun createLinearGradientWithAngle(
        rect: RectF,
        angle: Float,
        colors: IntArray,
        positions: FloatArray
    ): LinearGradient {
        val radians = Math.toRadians(angle.toDouble())
        val halfWidth = rect.width() / 2f
        val halfHeight = rect.height() / 2f
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val hypot = Math.hypot(halfWidth.toDouble(), halfHeight.toDouble()).toFloat()
        val cos = (Math.cos(radians) * hypot).toFloat()
        val sin = (Math.sin(radians) * hypot).toFloat()
        return LinearGradient(
            centerX - cos, centerY - sin,
            centerX + cos, centerY + sin,
            colors, positions,
            Shader.TileMode.CLAMP
        )
    }
}
