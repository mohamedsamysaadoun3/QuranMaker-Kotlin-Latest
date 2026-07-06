package hazem.nurmontage.videoquran.utils

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan

class CustomTypefaceSpan(
    private val typeface: Typeface
) : MetricAffectingSpan() {

    override fun updateDrawState(textPaint: TextPaint) {
        applyCustomTypeFace(textPaint, typeface)
    }

    override fun updateMeasureState(textPaint: TextPaint) {
        applyCustomTypeFace(textPaint, typeface)
    }

    private fun applyCustomTypeFace(paint: Paint, typeface: Typeface) {
        paint.typeface = typeface
    }
}
