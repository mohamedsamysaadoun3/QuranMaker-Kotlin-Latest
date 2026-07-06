package hazem.nurmontage.videoquran.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import hazem.nurmontage.videoquran.core.common.Constants
import java.io.File

object FileUtils {

    private const val TAG = "FileUtils"

    fun checkFileExists(path: String): Boolean = File(path).exists()

    fun getFile(context: Context): File? {
        val externalDir = context.getExternalFilesDir(null) ?: return null
        if (!externalDir.exists() && !externalDir.mkdirs()) {
            Log.e(TAG, "Failed to create external files directory")
            return null
        }
        val workDir = File(externalDir, "Work_${System.currentTimeMillis()}")
        if (workDir.exists() || workDir.mkdirs()) {
            return workDir
        }
        Log.e(TAG, "Failed to create work directory")
        return null
    }

    fun getFileVideo(basePath: String): File? {
        val baseDir = File(basePath)
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            Log.e(TAG, "Failed to create base directory: $basePath")
            return null
        }
        val frameDir = File(baseDir, Constants.VIDEO_FRAME_FOLDER)
        if (frameDir.exists() || frameDir.mkdirs()) {
            return frameDir
        }
        Log.e(TAG, "Failed to create video frame directory")
        return null
    }

    fun getFileFromUri(context: Context, uri: Uri): File {
        val filePath = when {
            DocumentsContract.isDocumentUri(context, uri) -> {
                resolveDocumentUri(context, uri)
            }
            uri.scheme?.equals("content", ignoreCase = true) == true -> {
                getDataColumn(context, uri, null, null)
            }
            uri.scheme?.equals("file", ignoreCase = true) == true -> {
                uri.path
            }
            else -> null
        }
        return File(filePath ?: throw Exception("Cannot resolve URI: $uri"))
    }

    private fun resolveDocumentUri(context: Context, uri: Uri): String? {
        return when {
            isExternalStorageDocument(uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                if (split[0].equals("primary", ignoreCase = true)) {
                    "${Environment.getExternalStorageDirectory()}/${split[1]}"
                } else null
            }
            isDownloadsDocument(uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    docId.toLongOrNull() ?: return null
                )
                getDataColumn(context, contentUri, null, null)
            }
            isMediaDocument(uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                val contentUri = when (split[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> return null
                }
                getDataColumn(context, contentUri, "_id=?", arrayOf(split[1]))
            }
            else -> null
        }
    }

    fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        try {
            context.contentResolver.query(
                uri, arrayOf("_data"),
                selection, selectionArgs, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow("_data"))
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }
}
