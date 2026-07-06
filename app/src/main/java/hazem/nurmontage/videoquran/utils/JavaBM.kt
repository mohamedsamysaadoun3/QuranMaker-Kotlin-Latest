package hazem.nurmontage.videoquran.utils

class JavaBM {

    private var mPattern: String? = null
    private var mText: String = ""
    private val skipTable: IntArray = IntArray(42)

    constructor() {
        this.mText = ""
    }

    constructor(text: String) {
        this.mText = text
    }

    fun setPattern(pattern: String) {
        mPattern = pattern
        buildSkipTable(pattern, skipTable)
    }

    fun getPattern(): String? = mPattern

    fun match(text: String): Int {
        val pattern = mPattern ?: return -1
        var i = 0
        val patternLen = pattern.length
        val textLen = text.length

        while (i <= textLen - patternLen) {
            var j = patternLen - 1
            var mismatchChar: Char = 1570.toChar()

            while (j >= 0) {
                val patternChar = pattern[j]
                val textChar = text[i + j]
                if (patternChar != textChar) {
                    mismatchChar = textChar
                    break
                }
                j--
                mismatchChar = textChar
            }

            var mappedChar = mismatchChar.code
            if (mappedChar < 1570 || mappedChar > 1610) {
                mappedChar = 1611
            }

            if (j < 0) {
                return i
            }

            i += Math.max(j - skipTable[mappedChar - 1570], 1)
        }

        return -1
    }

    private fun buildSkipTable(pattern: String, table: IntArray) {
        table.fill(-1)
        for (i in pattern.indices) {
            val c = pattern[i].code
            if (c < 1570 || c > 1610) {
                table[41] = i
            } else {
                table[c - 1570] = i
            }
        }
    }

    companion object {
        fun match(pattern: String, text: String): List<Int> {
            val result = mutableListOf<Int>()
            val textLen = text.length
            val patternLen = pattern.length
            val badCharShift = preprocessForBadCharacterShift(pattern)

            var patternIdx = patternLen - 1
            if (patternIdx >= textLen) return result

            var textIdx = 0
            while (true) {
                if (patternIdx >= 0) {
                    val textPos = textIdx + patternIdx
                    if (textPos >= textLen) break

                    val textChar = text[textPos]
                    val patternChar = pattern[patternIdx]

                    if (textChar != patternChar) {
                        val shift = badCharShift[textChar]
                        if (shift == null) {
                            textIdx = textPos + 1
                        } else {
                            var delta = textPos - (shift + textIdx)
                            if (delta <= 0) delta = 1
                            textIdx += delta
                        }
                    } else {
                        if (patternIdx == 0) {
                            result.add(textIdx)
                            textIdx++
                        }
                        patternIdx--
                    }
                }
            }

            return result
        }

        private fun preprocessForBadCharacterShift(pattern: String): Map<Char, Int> {
            val map = HashMap<Char, Int>()
            for (i in pattern.length - 1 downTo 0) {
                val c = pattern[i]
                if (!map.containsKey(c)) {
                    map[c] = i
                }
            }
            return map
        }
    }
}
