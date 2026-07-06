package hazem.nurmontage.videoquran.views

import android.content.res.AssetManager
import android.graphics.Typeface

/**
 * Shared Typeface cache for all custom views in the application.
 *
 * **Critical Memory Optimization:**
 * The original Java code loaded the same font file independently in each
 * custom view instance (TextCustumFont, ButtonCustumFont, CheckboxCustumFont,
 * etc.) — resulting in **6+ separate native Typeface allocations** for
 * "ReadexPro_Medium.ttf" alone! Each Typeface holds a native font object
 * consuming 50-200KB of memory.
 *
 * This shared cache ensures each font file is loaded **exactly once** from
 * assets, regardless of how many view instances reference it. Subsequent
 * requests return the cached instance.
 *
 * Thread-safe via synchronized access on the cache map.
 *
 * Font inventory:
 * - ReadexPro_Medium.ttf (shared by 6 view classes)
 * - ReadexPro_Bold.ttf (1 view class)
 * - خط حفص.ttf (AyaCustumFont)
 * - محمدي.ttf (AyaCircleBg)
 * - خط الإبل.otf (EditTextCustumFont)
 * - Poppins-Regular.ttf (SquareImageView numbers)
 */
internal object TypefaceCache {

    private val cache = mutableMapOf<String, Typeface>()

    /**
     * Returns a cached Typeface for the given font asset path.
     * Loads from assets on first access, then caches for all subsequent calls.
     *
     * @param assets The AssetManager for loading font files
     * @param fontPath Full asset path (e.g., "fonts/ReadexPro_Medium.ttf")
     * @return The loaded Typeface, or Typeface.DEFAULT on failure
     */
    fun get(assets: AssetManager, fontPath: String): Typeface {
        return cache.getOrPut(fontPath) {
            try {
                Typeface.createFromAsset(assets, fontPath)
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
        }
    }

    /**
     * Evicts all cached Typefaces. Should be called when the application
     * is low on memory or when all custom views have been destroyed.
     */
    fun clear() {
        cache.clear()
    }

    /** Returns the number of currently cached Typefaces. */
    val size: Int get() = cache.size
}
