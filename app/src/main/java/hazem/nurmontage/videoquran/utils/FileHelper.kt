package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.os.Environment
import java.io.File

class FileHelper(private val context: Context) {

    fun createVideoFolder(subFolder: String): File? {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), subFolder)
        if (!file.exists()) {
            if (file.mkdirs()) {
                println("Folder created successfully: ${file.absolutePath}")
            } else {
                System.err.println("Failed to create folder: ${file.absolutePath}")
                return null
            }
        }
        return file
    }

    fun createPublicVideoFolder(subFolder: String): File? {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), subFolder)
        if (!file.exists()) {
            if (file.mkdirs()) {
                println("Folder created successfully: ${file.absolutePath}")
            } else {
                System.err.println("Failed to create folder: ${file.absolutePath}")
                return null
            }
        }
        return file
    }
}
