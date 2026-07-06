package hazem.nurmontage.videoquran.utils

import android.graphics.Point
import hazem.nurmontage.videoquran.constant.ResizeType
import kotlin.math.round

object AspectRatioCalculator {

    private const val ASPECT_RATIO_WIDTH = 9.0f
    private const val ASPECT_RATIO_HEIGHT = 16.0f

    @JvmStatic
    fun calcuWattermark(width: Int): Int {
        return (width * 5.0 / 16.0).toInt()
    }

    @JvmStatic
    fun getSize(resizeTypeOrdinal: Int, quality: String): Pair<Int, Int> {
        if (resizeTypeOrdinal == ResizeType.SOCIAL_STORY.ordinal) {
            return when (quality) {
                "480p" -> Pair(480, 854)
                "720p" -> Pair(720, 1280)
                "1080p" -> Pair(1080, 1920)
                else -> Pair(1080, 1920)
            }
        }
        if (resizeTypeOrdinal == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            return when (quality) {
                "480p" -> Pair(854, 480)
                "720p" -> Pair(1280, 720)
                "1080p" -> Pair(1920, 1080)
                else -> Pair(1920, 1080)
            }
        }
        // Default: square
        return when (quality) {
            "480p" -> Pair(480, 480)
            "720p" -> Pair(720, 720)
            else -> Pair(1080, 1080)
        }
    }

    @JvmStatic
    fun calculateHeight(width: Int): Int {
        return round(width * ASPECT_RATIO_HEIGHT / ASPECT_RATIO_WIDTH).toInt()
    }

    @JvmStatic
    fun calculateHeight_Youtube(width: Int): Int {
        return round(width * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).toInt()
    }

    @JvmStatic
    fun calculateWidth(height: Int): Int {
        return round(height * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT).toInt()
    }

    private fun findGCD(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val temp = y
            y = x % y
            x = temp
        }
        return x
    }

    @JvmStatic
    fun calculateAspectRatio(width: Int, height: Int): Point {
        val gcd = findGCD(width, height)
        return Point(width / gcd, height / gcd)
    }
}
