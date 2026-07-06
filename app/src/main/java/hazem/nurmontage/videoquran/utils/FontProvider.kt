package hazem.nurmontage.videoquran.utils

import android.content.res.Resources
import android.graphics.Typeface
import android.text.TextUtils
import hazem.nurmontage.videoquran.core.common.Constants
import java.util.TreeMap

class FontProvider(resources: Resources) {

    private var resources: Resources? = resources
    private var fontNameToTypefaceFileQuran: MutableMap<String, String>
    private var fontNamesQuran: MutableList<String>

    private val typefaceCache = object : android.util.LruCache<String, Typeface>(30) {
        override fun sizeOf(key: String, value: Typeface): Int = 1
    }

    var defaultFontName: String? = null

    init {
        fontNameToTypefaceFileQuran = mutableMapOf()
        loadQuranFont()
        fontNamesQuran = TreeMap(fontNameToTypefaceFileQuran).keys.toList().toMutableList()
    }

    private fun loadQuranFont() {
        fontNameToTypefaceFileQuran.apply {
            put("المجد", "المجد.ttf")
            put("جنة", "جنة.ttf")
            put("محمدي", "محمدي.ttf")
            put("خط الثلث مزخرف", "الثلث مزخرف.ttf")
            put("باك تايب أجراك", "باك تايب أجراك.ttf")
            put("باك تايب تحرير", "باك تايب تحرير.ttf")
            put("باك تايب نسخ", "باك تايب نسخ.ttf")
            put("خط نسخ عثماني", "خط نسخ عثماني.otf")
            put("عثماني", Constants.FONT_QURAN)
            put("خط القيروان", "خط القيروان.ttf")
            put("خط حفص", "خط حفص.ttf")
            put("خط ورش", "خط ورش.ttf")
            put("قالون", "قالون.ttf")
            put("مريم", "مريم.ttf")
            put("الأقصى", "الأقصى.ttf")
            put("أجنادين", "أجنادين.ttf")
            put("بيبو", "بيبو.ttf")
            put("بيسان لايت", "بيسان لايت.ttf")
            put("تبيان", "تبيان.ttf")
            put("تجمع كوفي", "تجمع كوفي.ttf")
            put("تريكا", "تريكا.ttf")
            put("خط تجمع المصممين", "خط تجمع المصممين.ttf")
            put("شمائل", "شمائل.ttf")
            put("عصومي", "عصومي.ttf")
            put("فرشة", "فرشة.ttf")
            put("فسيح", "فسيح.ttf")
            put("كوفي", "كوفي.ttf")
            put("مطرية", "مطرية.ttf")
            put("نمر", "نمر.ttf")
            put("هيفن", "هيفن.ttf")
            put("لفتا بلاك", "لفتا بلاك.otf")
            put("خط الإبل", "خط الإبل.otf")
        }
    }

    fun getFullName(displayName: String): String? = fontNameToTypefaceFileQuran[displayName]

    fun getTypeface(displayName: String?): Typeface {
        if (displayName == null || TextUtils.isEmpty(displayName)) {
            return Typeface.DEFAULT
        }
        typefaceCache.get(displayName)?.let { return it }
        return try {
            val fileName = fontNameToTypefaceFileQuran[displayName] ?: return Typeface.DEFAULT
            val typeface = Typeface.createFromAsset(
                resources?.assets ?: return Typeface.DEFAULT,
                "fonts/arabic/$fileName"
            )
            typefaceCache.put(displayName, typeface)
            typeface
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    fun getFontNamesQuran(): List<String> = fontNamesQuran

    fun getResources(): Resources? = resources

    fun clear() {
        fontNameToTypefaceFileQuran.clear()
        fontNameToTypefaceFileQuran = mutableMapOf()
        fontNamesQuran.clear()
        fontNamesQuran = mutableListOf()
        typefaceCache.evictAll()
        resources = null
    }
}
