package hazem.nurmontage.videoquran.core.common

import android.graphics.RectF

class StackEntity(
    val rectF: RectF,
    val offset: Float,
    val end: Float,
    val start: Float,
    val left: Float,
    val right: Float,
    val max: Float,
    val offset_right: Float,
    val offset_left: Float
) {
    var index_start_thumbnail: Int = 0
        protected set
    var index_end_thumbnail: Int = 0
        protected set
}
