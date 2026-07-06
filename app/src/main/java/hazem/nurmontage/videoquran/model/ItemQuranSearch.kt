package hazem.nurmontage.videoquran.model

import java.io.Serializable

data class ItemQuranSearch(
    val aya: String?,
    val surahName: String,
    private val _to: Int,
    val surahIndex: Int,
    val startSpannable: Int,
    val endSpannble: Int
) : Serializable {

    val to: Int get() = _to
}
