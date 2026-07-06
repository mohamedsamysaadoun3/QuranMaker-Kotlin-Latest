package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MFileUtils {

    data class FileInfo(
        val name: String,
        val lastModified: Long,
        val formattedDate: String = formatDateShort(lastModified),
        val timedDate: String = if (lastModified > 0)
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastModified))
        else ""
    ) : Serializable

    fun getFileInfo(context: Context, path: String?): FileInfo? {
        if (path == null) return null

        val uri = Uri.parse(path)
        var name: String? = null
        var lastModified: Long = 0

        if ("content".equals(uri.scheme, ignoreCase = true)) {
            var cursor: android.database.Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    uri,
                    arrayOf("_display_name", "date_modified"),
                    null, null, null
                )
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    val dateIndex = cursor.getColumnIndex("date_modified")

                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                    if (dateIndex != -1) {
                        val dateValue = cursor.getLong(dateIndex)
                        if (dateValue > 0) {
                            lastModified = dateValue * 1000
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }

        if (name == null || lastModified == 0L) {
            try {
                val file = if ("file".equals(uri.scheme, ignoreCase = true)) {
                    File(uri.path ?: path)
                } else {
                    File(path)
                }
                if (file.exists()) {
                    if (name == null) name = file.name
                    if (lastModified == 0L) lastModified = file.lastModified()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (name == null) name = uri.lastPathSegment

        return FileInfo(name ?: "unknown", lastModified)
    }

    fun formatDateShort(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return SimpleDateFormat("MMM dd-yyyy", Locale.ENGLISH).format(Date(timestamp))
    }
}
