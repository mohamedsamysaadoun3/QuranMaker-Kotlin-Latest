package hazem.nurmontage.videoquran.utils

import android.graphics.Color

object ColorSchemeGenerator {

    data class Scheme(
        var screen1: Int = 0,
        var screen2: Int = 0,
        var body: Int = 0,
        var shadow: Int = 0,
        var label: Int = 0,
        var accent: Int = 0,
        var circle: Int = 0
    )

    fun generateScheme(baseColor: Int, hueOffset: Float): Scheme {
        val rotatedColor = rotateHue(baseColor, hueOffset)
        return Scheme(
            screen1 = baseColor,
            screen2 = lightenColor(rotatedColor, 0.15f),
            body = getComplementaryColor(rotatedColor),
            shadow = darkenColor(getComplementaryColor(rotatedColor), 0.25f),
            label = generateLabelColor(getComplementaryColor(rotatedColor)),
            accent = darkenColor(getComplementaryColor(rotatedColor), 0.15f)
        )
    }

    fun generateScheme(baseColor: Int): Scheme {
        val bodyColor = getComplementaryColor(baseColor)
        return Scheme(
            screen1 = baseColor,
            screen2 = lightenColor(baseColor, 0.15f),
            body = bodyColor,
            shadow = darkenColor(bodyColor, 0.25f),
            label = generateLabelColor(bodyColor),
            accent = darkenColor(getComplementaryColor(bodyColor), 0.15f)
        )
    }

    fun generateCircleColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + 180f) % 360f
        hsv[1] = minOf(0.4f, hsv[1])
        hsv[2] = 0.95f
        return Color.HSVToColor(hsv)
    }

    fun rotateHue(color: Int, degrees: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val newHue = (hsv[0] + degrees) % 360f
        hsv[0] = if (newHue < 0f) newHue + 360f else newHue
        return Color.HSVToColor(hsv)
    }

    fun lightenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = minOf(1.0f, hsv[2] + amount)
        return Color.HSVToColor(hsv)
    }

    fun darkenColor(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = maxOf(0.0f, hsv[2] - amount)
        return Color.HSVToColor(hsv)
    }

    fun getComplementaryColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + 180f) % 360f
        return Color.HSVToColor(hsv)
    }

    fun generateLabelColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val originalHue = hsv[0]
        hsv[1] = maxOf(0.0f, hsv[1] * 0.4f)
        var brightness = minOf(1.0f, hsv[2] + 0.25f)
        hsv[2] = brightness
        if (brightness < 0.75f) {
            hsv[2] = 0.85f
        }
        hsv[0] = originalHue
        return Color.HSVToColor(hsv)
    }

    fun generateAccentColor(color: Int): Int {
        val hsv = floatArrayOf(30f, 0.8f, 0.9f)
        Color.colorToHSV(color, hsv)
        return Color.HSVToColor(hsv)
    }
}
