package hazem.nurmontage.videoquran.model

import java.io.Serializable

class TimeModel : Serializable {

    var width_bitmap_progress: Int = 0
    var height_bitmap_progress: Int = 0
    var size: Float = 0f
    var color: String? = null
    var posY: Float = 0f
    var posXRight: Float = 0f
    var progress_offset: Int = 0
    var widthShape: Int = 0
    var heightShape: Int = 0
    var startShape: Float = 0f

    constructor(
        width_bitmap_progress: Int,
        height_bitmap_progress: Int,
        size: Float,
        color: String,
        posY: Float,
        posXRight: Float,
        progress_offset: Int
    ) {
        this.size = size
        this.color = color
        this.posY = posY
        this.posXRight = posXRight
        this.progress_offset = progress_offset
        this.width_bitmap_progress = width_bitmap_progress
        this.height_bitmap_progress = height_bitmap_progress
    }
}
