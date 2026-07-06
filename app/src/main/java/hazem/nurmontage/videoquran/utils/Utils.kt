package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Pair
import androidx.core.content.ContextCompat
import hazem.nurmontage.videoquran.constant.ResizeType
import kotlin.math.round

object Utils {

    private const val CHARACTER_TO_COUNT = ' '

    @JvmStatic
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun f2(value: Float): Float {
        return round(value * 100.0f) / 100.0f
    }

    @JvmStatic
    fun countSpace(limit: Int, text: String?): Int {
        if (text == null || limit <= 0) return 0
        val len = minOf(limit, text.length)
        var count = 0
        for (i in 0 until len) {
            if (text[i] == CHARACTER_TO_COUNT) count++
        }
        return count
    }

    @JvmStatic
    fun countSpace(text: String?): Int {
        if (text == null) return 0
        var count = 0
        for (i in text.indices) {
            if (text[i] == CHARACTER_TO_COUNT) count++
        }
        return count
    }

    @JvmStatic
    fun countIndex(startIndex: Int, spaceCount: Int, text: String?): Int {
        if (text == null || startIndex < 0) {
            return if (text == null) 0 else text.length
        }

        // First pass: count spaces to find the position
        var spacesFound = 0
        for (i in startIndex until text.length) {
            if (text[i] == CHARACTER_TO_COUNT) {
                spacesFound++
            }
            if (spacesFound > spaceCount) break
        }

        // Second pass: find the actual index
        var index = startIndex
        var count = 0
        while (index < text.length && count <= spaceCount) {
            if (text[index] == CHARACTER_TO_COUNT) {
                count++
            }
            index++
        }
        return index
    }

    @JvmStatic
    fun countIndex(spaceCount: Int, text: String): Int {
        var index = 0
        var count = 0

        // Preview pass
        for (i in text.indices) {
            if (text[i] == CHARACTER_TO_COUNT) count++
            if (count > spaceCount) break
        }

        // Actual index pass
        count = 0
        while (index < text.length && count < spaceCount) {
            if (text[index] == CHARACTER_TO_COUNT) {
                count++
            }
            index++
        }
        return index
    }

    @JvmStatic
    fun getDrawableByName(context: Context, name: String): Drawable? {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (id != 0) ContextCompat.getDrawable(context, id) else null
    }

    @JvmStatic
    fun getDimension(resizeType: ResizeType, size: Int): Pair<Int, Int> {
        val width: Int
        val height: Int

        if (resizeType.ordinal == ResizeType.SOCIAL_STORY.ordinal) {
            height = (size * ResizeType.VERTICAL.value).toInt()
            width = size
        } else if (resizeType.ordinal == ResizeType.YOUTUBE_THUMBNAIL.ordinal) {
            height = (size * ResizeType.YOUTUBE_THUMBNAIL.value).toInt()
            width = size
        } else {
            height = size
            width = size
        }

        return Pair(width, height)
    }

    @JvmStatic
    fun isProbablyLArabic(text: String): Boolean {
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            if (codePoint in 1536..1760) {
                return true
            }
            i += Character.charCount(codePoint)
        }
        return false
    }

    @JvmStatic
    fun indexOf(array: IntArray, value: Int): Int {
        for (i in array.indices) {
            if (array[i] == value) return i
        }
        return -1
    }
}
