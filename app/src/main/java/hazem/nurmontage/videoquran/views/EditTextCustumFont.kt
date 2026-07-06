package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class EditTextCustumFont : AppCompatEditText {

    private var typeface: Typeface? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        if (typeface == null) {
            val tf = Typeface.createFromAsset(resources.assets, "fonts/arabic/خط الإبل.otf")
            typeface = tf
            setTypeface(tf)
        }
    }
}
