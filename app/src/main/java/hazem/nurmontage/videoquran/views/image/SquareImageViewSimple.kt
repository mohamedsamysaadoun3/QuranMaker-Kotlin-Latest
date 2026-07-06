package hazem.nurmontage.videoquran.views.image

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * Simple square ImageView that enforces equal width and height.
 *
 * Originally: SquareImageViewSimple.java
 * Converted to: SquareImageViewSimple.kt — idiomatic Kotlin
 *
 * Used by [ExploreAdabters] in the file explorer to display
 * folder thumbnails in a uniform square grid. Unlike [SquareImageView],
 * this variant has no selection overlay or number badge — it simply
 * enforces a square aspect ratio.
 *
 * @see SquareImageView
 */
class SquareImageViewSimple : AppCompatImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        setMeasuredDimension(width, width)
    }
}
