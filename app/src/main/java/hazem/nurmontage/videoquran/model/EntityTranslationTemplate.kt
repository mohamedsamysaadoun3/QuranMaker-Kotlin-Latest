package hazem.nurmontage.videoquran.model

import java.io.Serializable

data class EntityTranslationTemplate(
    var transition: Transition? = null,
    var start: Float = 0f,
    var end: Float = 0f,
    var btm_x: Float = 0f,
    var btm_y: Float = 0f,
    var left: Float = 0f,
    var right: Float = 0f,
    var aya: String = "",
    var name_font: String? = null,
    var number: Int = 0,
    var color: Int = -1,
    var preset: Int = 0
) : Serializable {

    var x: Float = 0f
    var y: Float = 0f
    var scale: Float = 1.0f
    var factor_size: Float = 1.0f
    var factor_sizeTrl: Float = 1.0f
    var height: Float = 0f
    var rectF: MRectF? = null
    var file: String? = null
    var file_in: String? = null
    var file_out: String? = null
    var clr_bg: Int = 0
    var isHaveBg: Boolean = false
}
