package hazem.nurmontage.videoquran.utils

import android.content.Context
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class QuranReader(private val context: Context) {

    fun getAyahText(surahNumber: Int, ayahNumber: Int): String {
        try {
            val reader = BufferedReader(
                InputStreamReader(
                    context.assets.open("quran/quran-simple.txt"),
                    StandardCharsets.UTF_8
                )
            )
            reader.use {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line!!.split("\\|".toRegex())
                    if (parts.size == 3) {
                        try {
                            val surah = parts[0].toInt()
                            val ayah = parts[1].toInt()
                            val text = parts[2]
                            if (surah == surahNumber && ayah == ayahNumber) {
                                return text
                            }
                        } catch (_: NumberFormatException) {
                        }
                    }
                }
            }
            return "Ayah not found"
        } catch (e: IOException) {
            e.printStackTrace()
            return "Error reading file: ${e.message}"
        }
    }

    fun getTranslationAyahText(
        translationFileName: String,
        surahNumber: Int,
        ayahNumber: Int
    ): String {
        try {
            BufferedReader(
                InputStreamReader(
                    context.assets.open("quran/$translationFileName"),
                    StandardCharsets.UTF_8
                )
            ).use { reader ->
                val prefix = "$surahNumber|$ayahNumber"
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith(prefix)) {
                        // BUG-A01 fix: line format is "surah|ayah|text".
                        // `prefix` is "surah|ayah" (length N); substring(N) would include
                        // the leading "|". Skip the separator.
                        return line!!.substring(prefix.length + 1)
                    }
                }
            }
            return "Aya Not Found !"
        } catch (e: IOException) {
            e.printStackTrace()
            return "Aya Not Found !"
        }
    }
}
