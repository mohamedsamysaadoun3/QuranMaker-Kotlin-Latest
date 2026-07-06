package hazem.nurmontage.videoquran.model

import hazem.nurmontage.videoquran.constant.ResizeType

data class ItemDimension(
    val name: String,
    val image: Int,
    val resizeType: ResizeType,
    val w: Int,
    val h: Int,
    val id: String
)
