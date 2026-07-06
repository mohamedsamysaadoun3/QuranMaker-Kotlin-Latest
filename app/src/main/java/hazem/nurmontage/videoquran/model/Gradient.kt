package hazem.nurmontage.videoquran.model

import java.io.Serializable

data class Gradient(
    val color: Int,
    val second: Int,
    val three: Int,
    var angle: Int = 81
) : Serializable
