package hazem.nurmontage.videoquran.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.view.ViewCompat

object ColorUtils {

    private fun mapHueToRange(
        value: Float,
        inMin: Float,
        inMax: Float,
        outMin: Float,
        outMax: Float
    ): Float {
        if (value < inMin) return outMin
        if (value > inMax) return outMax
        val range = inMax - inMin
        return if (range == 0f) (outMin + outMax) / 2f
        else ((value - inMin) * (outMax - outMin)) / range + outMin
    }

    @Suppress("KotlinConstantConditions")
    fun convertToEnergyColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        var hue = hsv[0]
        val saturation = hsv[1]
        val brightness = hsv[2]

        val adjustedSaturation: Float
        val adjustedBrightness: Float

        if (hue in 60f..300f) {
            when {
                hue in 60f..<170f -> {
                    hue = mapHueToRange(hue / 2f + 30f, 60f, 170f, 20f, 60f)
                }
                hue in 170f..<260f -> {
                    hue = ((hue - 180f) + 360f) % 360f
                    if (hue < 10f) hue += 10f
                    if (hue > 60f) hue = 60f
                }
                hue in 260f..300f -> {
                    hue = (hue + 60f) % 360f
                    if (hue in 270f..<300f) hue = 300f
                }
            }

            adjustedSaturation = if (saturation < 0.5f) {
                minOf(1f, saturation + 0.3f)
            } else {
                minOf(1f, saturation + 0.15f)
            }
            adjustedBrightness = if (brightness < 0.6f) {
                minOf(1f, brightness + 0.25f)
            } else {
                minOf(1f, brightness + 0.1f)
            }

            val originalHue = hsv[0]
            if (originalHue in 60f..300f && hue in 70f..290f) {
                hue = if (Math.random() < 0.5) 30f else 50f
            }
        } else {
            adjustedSaturation = minOf(1f, saturation + 0.1f)
            adjustedBrightness = minOf(1f, brightness + 0.05f)
        }

        hsv[0] = hue
        hsv[1] = adjustedSaturation.coerceIn(0.4f, 1f)
        hsv[2] = adjustedBrightness.coerceIn(0.5f, 1f)

        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    fun getSemiTransparentColorInt(color: Int, alphaPercent: Int): Int {
        return (color and ViewCompat.MEASURED_SIZE_MASK) or
                (Math.round(alphaPercent * 255 / 100f) shl 24)
    }

    fun toHex(color: Int): String {
        var hex = Integer.toHexString(color)
        while (hex.length < 8) {
            hex = "0$hex"
        }
        return "#${hex.uppercase()}"
    }

    fun isColorDark(color: Int): Boolean {
        val luminance = (Color.red(color) * 0.299 +
                Color.green(color) * 0.587 +
                Color.blue(color) * 0.114) / 255.0
        return (1.0 - luminance) >= 0.3
    }

    fun getAverageColor(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        var count = 0
        var totalRed = 0
        var totalGreen = 0
        var totalBlue = 0

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                totalRed += Color.red(pixel)
                totalGreen += Color.green(pixel)
                totalBlue += Color.blue(pixel)
                count++
                x += 20
            }
            y += 20
        }

        return if (count > 0) {
            Color.rgb(totalRed / count, totalGreen / count, totalBlue / count)
        } else {
            -7829368
        }
    }

    fun darkenColor(color: Int, factor: Float): Int {
        val multiplier = 1.0f - factor
        return Color.rgb(
            (Color.red(color) * multiplier).toInt(),
            (Color.green(color) * multiplier).toInt(),
            (Color.blue(color) * multiplier).toInt()
        )
    }

    fun lightenColor(color: Int, factor: Float): Int {
        return Color.rgb(
            (Color.red(color) + (255 - Color.red(color)) * factor).toInt(),
            (Color.green(color) + (255 - Color.green(color)) * factor).toInt(),
            (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        )
    }
}
