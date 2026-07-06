package hazem.nurmontage.videoquran.utils

import hazem.nurmontage.videoquran.model.WordModel

class WordProcessor {

    fun reverseInGroupsOfFour(list: List<WordModel>): List<WordModel> {
        val result = mutableListOf<WordModel>()
        var i = 0
        while (i < list.size) {
            val end = i + 4
            val group = ArrayList(list.subList(i, minOf(end, list.size)))
            group.reverse()
            result.addAll(group)
            i = end
        }
        return result
    }

    fun findAndSelectPhrase(fullText: String, phrase: String): List<WordModel> {
        val words = fullText.trim().split("\\s+".toRegex())
        val phraseWords = phrase.trim().split("\\s+".toRegex())
        val result = mutableListOf<WordModel>()

        var matchStart = -1
        var i = 0
        search@ while (i <= words.size - phraseWords.size) {
            for (j in phraseWords.indices) {
                if (words[i + j] != phraseWords[j]) {
                    i++
                    continue@search
                }
            }
            matchStart = i
            break
        }

        for (k in words.indices) {
            val isSelected = matchStart != -1 && k >= matchStart && k < matchStart + phraseWords.size
            result.add(WordModel(words[k], isSelected))
        }

        return result
    }

    companion object {
        fun mapIndexAfterGroupReverse(index: Int, groupSize: Int, totalItems: Int): Int {
            val groupStart = (index / groupSize) * groupSize
            val offset = index % groupSize
            val actualGroupSize = minOf(groupSize, totalItems - groupStart)
            return groupStart + (actualGroupSize - 1 - offset)
        }
    }
}
