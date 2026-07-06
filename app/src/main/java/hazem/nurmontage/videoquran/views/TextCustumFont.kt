package hazem.nurmontage.videoquran.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Custom TextView that applies the ReadexPro Medium font automatically.
 *
 * Originally: TextCustumFont.java (preserved typo in original name)
 * Converted to: TextCustumFont.kt — with shared [TypefaceCache] optimization
 *
 * This is the primary custom text view used throughout the app's
 * adapters, fragments, and dialogs. It appears in XML layouts as:
 * ```xml
 * <hazem.nurmontage.videoquran.views.TextCustumFont
 *     android:id="@+id/tv_font"
 *     ... />
 * ```
 *
 * **Memory Fix:** The original Java loaded ReadexPro_Medium.ttf independently
 * for every view instance (6+ classes share this font). Now all share a
 * single cached Typeface via [TypefaceCache].
 *
 * @see TypefaceCache
 */
open class TextCustumFont : AppCompatTextView {

    constructor(context: Context) : super(context) { init() }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init() }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) { init() }

    private fun init() {
        val typeface = TypefaceCache.get(resources.assets, FONT_PATH)
        typeface?.let { setTypeface(it) }
    }

    companion object {
        private const val FONT_PATH = "fonts/ReadexPro_Medium.ttf"
    }
}
