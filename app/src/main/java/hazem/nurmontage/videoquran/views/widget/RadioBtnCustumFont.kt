package hazem.nurmontage.videoquran.views.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton

class RadioBtnCustumFont : AppCompatRadioButton {

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
            val tf = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
            this.typeface = tf
            setTypeface(tf)
        }
    }
}
