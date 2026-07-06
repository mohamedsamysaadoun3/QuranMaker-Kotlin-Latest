package hazem.nurmontage.videoquran.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import hazem.nurmontage.videoquran.views.TypefaceCache

/**
 * Custom TextView that applies the ReadexPro Bold font automatically.
 *
 * Originally: TextCustumFontBold.java
 * Converted to: TextCustumFontBold.kt — with shared [TypefaceCache] optimization
 *
 * Used for headings, emphasis text, and titles throughout the app.
 * Appears in XML layouts as:
 * ```xml
 * <hazem.nurmontage.videoquran.views.text.TextCustumFontBold ... />
 * ```
 *
 * @see TypefaceCache
 * @see TextCustumFont
 */
class TextCustumFontBold : AppCompatTextView {

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init() }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        TypefaceCache.get(resources.assets, FONT_PATH)?.let { setTypeface(it) }
    }

    companion object {
        private const val FONT_PATH = "fonts/ReadexPro_Bold.ttf"
    }
}
