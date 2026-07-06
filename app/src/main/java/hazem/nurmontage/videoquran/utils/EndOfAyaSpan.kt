package hazem.nurmontage.videoquran.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.text.style.ReplacementSpan

class EndOfAyaSpan(
    private val vectorDrawable: VectorDrawable?,
    private val fontNumber: Typeface?,
    private val number: String
) : ReplacementSpan() {

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        baseline: Int,
        bottom: Int,
        paint: Paint
    ) {
        val substring = text.substring(start, end)
        if (substring != " نص") {
            canvas.drawText(text, start, end, x, baseline.toFloat(), paint)
            return
        }

        val vd = vectorDrawable
        if (vd == null) {
            canvas.drawText(number, x, baseline.toFloat(), paint)
            return
        }

        val textWidth = paint.measureText(substring)
        val originalTypeface = paint.typeface
        val originalTextSize = paint.textSize

        if (fontNumber != null) {
            paint.typeface = fontNumber
        }
        paint.isFakeBoldText = true

        if (number.length > 2) {
            paint.textSize = paint.textSize * 0.8f
        } else {
            paint.textSize = paint.textSize * 0.7f
        }

        val textBounds = Rect()
        paint.getTextBounds(number, 0, number.length, textBounds)

        val spanRect = RectF(x, top.toFloat(), textWidth + x, bottom.toFloat())
        val halfWidth = spanRect.width() * 0.43f
        val halfHeight = spanRect.height() * 0.42f
        vd.setBounds(
            (spanRect.centerX() - halfWidth).toInt(),
            (spanRect.centerY() - halfHeight).toInt(),
            (spanRect.centerX() + halfWidth).toInt(),
            (spanRect.centerY() + halfHeight).toInt()
        )

        vd.setColorFilter(paint.color, PorterDuff.Mode.SRC_IN)
        vd.draw(canvas)

        if (number.length > 2) {
            paint.textSize = paint.textSize * 0.7f
            canvas.drawText(
                number,
                spanRect.centerX() - textBounds.width() * 0.4f,
                spanRect.centerY() + textBounds.height() * 0.35f,
                paint
            )
        } else {
            canvas.drawText(
                number,
                spanRect.centerX() - textBounds.width() * 0.54f,
                spanRect.centerY() + textBounds.height() * 0.4f,
                paint
            )
        }

        paint.typeface = originalTypeface
        paint.textSize = originalTextSize
        paint.isFakeBoldText = false
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        return Math.round(paint.measureText(text, start, end))
    }
}
