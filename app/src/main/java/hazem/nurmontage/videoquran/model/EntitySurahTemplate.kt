package hazem.nurmontage.videoquran.model

import java.io.Serializable

data class EntitySurahTemplate(
    var name: String = "",
    var reader: String = "",
    var left: Float = 0f,
    var top: Float = 0f,
    var rectF: MRectF? = null,
    var factor_scale: Float = 1.0f,
    var name_font: String = "خط الإبل.otf",
    var clr: Int = -1,
    var preset: Int = 0,
    var style: Int = 0,
    var index_surah: Int = 1,
    var isHaveBg: Boolean = false,
    var clrBg: Int = 0x00000000
) : Serializable {

    fun setPos(left: Float, top: Float) {
        this.left = left
        this.top = top
    }
}
