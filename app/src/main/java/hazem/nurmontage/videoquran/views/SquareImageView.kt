package hazem.nurmontage.videoquran.views

import android.content.Context
import android.util.AttributeSet

/**
 * Alias class for [hazem.nurmontage.videoquran.views.image.SquareImageView].
 *
 * This class exists at the `views` package path because some layout XML files
 * reference `hazem.nurmontage.videoquran.views.SquareImageView` directly.
 * The canonical implementation lives in the `views.image` subpackage.
 *
 * @see hazem.nurmontage.videoquran.views.image.SquareImageView
 */
class SquareImageView : hazem.nurmontage.videoquran.views.image.SquareImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)
}
