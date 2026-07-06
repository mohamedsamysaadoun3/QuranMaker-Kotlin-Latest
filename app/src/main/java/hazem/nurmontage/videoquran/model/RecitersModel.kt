package hazem.nurmontage.videoquran.model

class RecitersModel(
    identifer: String,
    surahIdx: Int,
    ayaNum: Int
) {
    var identifer: String = identifer
        private set
    val surah_index: String
    val number_aya: String
    var isTarteel: Boolean = false
        private set

    init {
        surah_index = when {
            surahIdx < 10  -> "00$surahIdx"
            surahIdx < 100 -> "0$surahIdx"
            else           -> "$surahIdx"
        }
        number_aya = when {
            ayaNum < 10  -> "00$ayaNum"
            ayaNum < 100 -> "0$ayaNum"
            else         -> "$ayaNum"
        }
        isTarteel = !identifer.contains("_")
    }
}
