package hazem.nurmontage.videoquran.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FontUtils {

    @JvmStatic
    fun copyFontToInternalStorage(context: Context, fontFileName: String) {
        val targetFile = File(context.filesDir, fontFileName)
        if (targetFile.exists()) return

        try {
            context.assets.open("fonts/arabic/$fontFileName").use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
