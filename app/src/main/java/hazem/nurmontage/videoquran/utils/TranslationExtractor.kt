package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import org.json.JSONObject

object TranslationExtractor {

    private const val TAG = "JSON_TO_TXT"

    @JvmStatic
    fun convertJsonToTxt(context: Context, assetFileName: String, outputFileName: String) {
        try {
            val reader = BufferedReader(
                InputStreamReader(context.assets.open(assetFileName), StandardCharsets.UTF_8)
            )
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()

            val jsonObject = JSONObject(sb.toString())
            val keysList = mutableListOf<String>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                keysList.add(keys.next())
            }

            // Sort by surah:ayah:verse order
            keysList.sortWith { a, b ->
                val partsA = a.split(":")
                val partsB = b.split(":")
                val surahA = partsA[0].toInt()
                val surahB = partsB[0].toInt()
                val ayahA = partsA[1].toInt()
                val ayahB = partsB[1].toInt()
                if (surahA != surahB) surahA - surahB
                else if (ayahA != ayahB) ayahA - ayahB
                else partsA[2].toInt() - partsB[2].toInt()
            }

            val surahAyahMap = LinkedHashMap<String, LinkedHashMap<Int, String>>()
            val maxVerseMap = HashMap<String, Int>()

            for (key in keysList) {
                val parts = key.split(":")
                val surah = parts[0]
                val ayah = parts[1]
                val verse = parts[2].toInt()
                val surahAyahKey = "$surah|$ayah"
                val text = jsonObject.getString(key)

                // Skip if text is just a number or (number)
                if (text.matches(Regex("\\(\\d+\\)")) || text.matches(Regex("\\d+"))) {
                    continue
                }

                surahAyahMap.getOrPut(surahAyahKey) { LinkedHashMap() }[verse] = text
                maxVerseMap[surahAyahKey] = maxOf(maxVerseMap.getOrDefault(surahAyahKey, 0), verse)
            }

            val outputDir = File(context.getExternalFilesDir(null), "QuranTranslations")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }

            val outputFile = File(outputDir, outputFileName)
            val writer = BufferedWriter(
                OutputStreamWriter(FileOutputStream(outputFile), StandardCharsets.UTF_8)
            )

            for ((surahAyahKey, verseMap) in surahAyahMap) {
                val maxVerse = maxVerseMap[surahAyahKey] ?: continue
                val verseList = mutableListOf<String>()
                for (i in 1..maxVerse) {
                    verseList.add(verseMap.getOrDefault(i, "*"))
                }
                writer.write(surahAyahKey + "," + verseList.joinToString(","))
                writer.newLine()
            }

            writer.close()
            Log.d(TAG, "Conversion completed. File saved: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error: ${e.message}")
        }
    }

    @JvmStatic
    fun extractTranslationsBySurahAndAyah(context: Context) {
        try {
            val translationsDir = File(context.getExternalFilesDir(null), "QuranTranslations")
            if (!translationsDir.exists()) {
                translationsDir.mkdirs()
            }

            // List all JSON translation files in assets
            val assetFiles = context.assets.list("translations") ?: return
            for (assetFile in assetFiles) {
                if (assetFile.endsWith(".json")) {
                    val outputName = assetFile.replace(".json", ".txt")
                    convertJsonToTxt(context, "translations/$assetFile", outputName)
                }
            }

            Log.d(TAG, "All translations extracted successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error extracting translations: ${e.message}")
        }
    }
}
