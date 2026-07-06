package hazem.nurmontage.videoquran.utils

object RemoveTashkeel {

    private const val SPECIFIC_POINT_CHAR_CODE = '.'

    private val TASHKEEL_SET: Set<Char> = setOf(
        1611.toChar(), 1612.toChar(), 1613.toChar(),
        1614.toChar(), 1615.toChar(), 1616.toChar(),
        1617.toChar(), 1618.toChar(), 1619.toChar(),
        1620.toChar(), 1621.toChar(), 1648.toChar(),
        1600.toChar()
    )

    val arabicVOriginal: List<String> = listOf(
        "ؘ", "ؙ", "ؚ", "ؐ", "ؐؑ", "ؒ", "ؓ", "ؔ", "ؕ", "ؖ", "ؗ",
        "ؗ", "ﹰﹰ", "ﹲ", "ﹴ", "ﹸ", "ﹼ", "ﹾ", "ٍ", "ً", "ُ", "ِ",
        "َ", "ّ", "ٓ", "ٔ", "ْ", "ِ", "َّ", "َ", "َْ", "َ", "ً", "ٌ",
        "َ", "ُ", "ٍ", "َ", "ْ", "ِ", "ُ", "ّ", "ً"
    )

    fun isTashkeel(c: Char): Boolean = TASHKEEL_SET.contains(c)

    fun removeTashkeel(text: String?): String? {
        if (text == null) return null
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (!isTashkeel(c)) {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun countTashkeel(text: String?): Int {
        if (text == null) return 0
        var count = 0
        for (c in text) {
            if (isTashkeel(c)) count++
        }
        return count
    }

    fun removeTashkeelAndPoint(text: String?): String? {
        if (text == null) return null
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (!isTashkeel(c) && c != SPECIFIC_POINT_CHAR_CODE) {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun removeChar(text: String?): String? {
        if (text == null) return null
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (isTashkeel(c)) {
                sb.append(c)
            } else {
                sb.append(' ')
            }
        }
        return sb.toString()
    }
}
