package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object UtilsFile {

    private const val TAG = "UtilsFile"

    @JvmStatic
    fun getPath(context: Context, uri: Uri?): File? {
        if (uri == null) return null
        if ("file" == uri.scheme) {
            return File(uri.path!!)
        }
        if ("content" == uri.scheme) {
            return copyContentUriToFile(context, uri)
        }
        return null
    }

    private fun copyContentUriToFile(context: Context, uri: Uri): File? {
        val file = File(context.cacheDir, "temp_file")
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(4096)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error copying content URI to file", e)
            return null
        }
    }

    /**
     * Get the file path from a content URI by querying the _data column.
     * Reconstructed from smali bytecode.
     */
    @JvmStatic
    fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        val column = "_data"
        val projection = arrayOf(column)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting data column", e)
            return null
        } finally {
            cursor?.close()
        }
    }
}
