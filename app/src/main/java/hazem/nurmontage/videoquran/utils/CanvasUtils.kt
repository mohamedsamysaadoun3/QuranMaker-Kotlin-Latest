package hazem.nurmontage.videoquran.utils

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.min

object CanvasUtils {

    @JvmStatic
    fun drawCustomRoundedRect(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        topRightRadius: Float,
        bottomRightRadius: Float
    ): Path {
        val path = Path()
        val halfWidth = (right - left) / 2.0f
        val halfHeight = (bottom - top) / 2.0f
        val topRight = min(topRightRadius, min(halfWidth, halfHeight))
        val bottomRight = min(bottomRightRadius, min(halfWidth, halfHeight))

        path.moveTo(left, top)
        path.lineTo(right - topRight, top)

        if (topRight > 0.0f) {
            val diameter = topRight * 2.0f
            path.arcTo(
                RectF(right - diameter, top, right, diameter + top),
                -90.0f, 90.0f, false
            )
        } else {
            path.lineTo(right, top)
        }

        path.lineTo(right, bottom - bottomRight)

        if (bottomRight > 0.0f) {
            val diameter = bottomRight * 2.0f
            path.arcTo(
                RectF(right - diameter, bottom - diameter, right, bottom),
                0.0f, 90.0f, false
            )
        } else {
            path.lineTo(right, bottom)
        }

        path.lineTo(left, bottom)
        path.close()
        return path
    }
}
