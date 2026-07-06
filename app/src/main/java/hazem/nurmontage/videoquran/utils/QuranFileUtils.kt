package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object QuranFileUtils {

    private const val TARGET = "\u0628\u0651\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650"
    private const val REPLACEMENT = "*"

    fun replacePhraseInFile(sourceFile: File, destFile: File) {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(FileInputStream(sourceFile), StandardCharsets.UTF_8)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append('\n')
            }
        }
        val replaced = sb.toString().replace(TARGET, REPLACEMENT)
        BufferedWriter(OutputStreamWriter(FileOutputStream(destFile), StandardCharsets.UTF_8)).use { writer ->
            writer.write(replaced)
        }
    }

    fun replacePhraseFromAssetsToFilesDir(context: Context, assetPath: String, destFileName: String) {
        val sb = StringBuilder()
        context.assets.open(assetPath).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
            }
        }
        val replaced = sb.toString().replace(TARGET, REPLACEMENT)
        BufferedWriter(
            OutputStreamWriter(
                FileOutputStream(File(context.filesDir, destFileName)),
                StandardCharsets.UTF_8
            )
        ).use { writer ->
            writer.write(replaced)
        }
    }

    fun counTPhraseFromAssetsToFilesDir(context: Context, assetPath: String) {
        val sb = StringBuilder()
        context.assets.open(assetPath).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.contains("\u0628\u0651\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645")) {
                        Log.e("mLine", line!!)
                    }
                    sb.append(line).append('\n')
                }
            }
        }
    }
}
