package hazem.nurmontage.videoquran.views.text

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import hazem.nurmontage.videoquran.views.TypefaceCache

/**
 * Custom TextView with ReadexPro Medium font for Arabic-aware layouts.
 *
 * Originally: TextCustumFontAR.java
 * Converted to: TextCustumFontAR.kt — with shared [TypefaceCache] optimization
 *
 * Functionally identical to [TextCustumFont] (same font file), but kept
 * as a separate class for XML layout compatibility — existing layouts
 * reference this class by name. ReadexPro supports both Arabic and Latin
 * glyphs, making it suitable for bilingual content.
 *
 * @see TextCustumFont
 * @see TypefaceCache
 */
class TextCustumFontAR : AppCompatTextView {

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init() }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        TypefaceCache.get(resources.assets, FONT_PATH)?.let { setTypeface(it) }
    }

    companion object {
        private const val FONT_PATH = "fonts/ReadexPro_Medium.ttf"
    }
}
