package hazem.nurmontage.videoquran.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.media3.common.MimeTypes

object UtilsFileLast {

    private const val TAG = "UtilsFileLast"

    private fun extractNumericId(id: String): String {
        return id
    }

    @JvmStatic
    fun loadFontFromAsset(context: Context, fontPath: String): Typeface? {
        return try {
            Typeface.createFromAsset(context.assets, fontPath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getPath(context: Context?, uri: Uri?): String? {
        Log.d(TAG, "getPath called with URI: $uri")
        if (context == null || uri == null) {
            Log.e(TAG, "Context or URI is null")
            return null
        }

        if (DocumentsContract.isDocumentUri(context, uri)) {
            Log.d(TAG, "URI is a document URI")

            if (isExternalStorageDocument(uri)) {
                Log.d(TAG, "URI is an external storage document")
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                if ("primary".equals(split[0], ignoreCase = true)) {
                    val path = Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    Log.d(TAG, "External storage path (primary): $path")
                    return path
                }
                Log.d(TAG, "External storage path (non-primary): $docId")
                val treeUri = DocumentsContract.buildTreeDocumentUri(
                    "com.android.externalstorage.documents", docId
                )
                val pathFromTree = getPathFromTreeUri(context, treeUri, split[1])
                if (pathFromTree != null) return pathFromTree
                return null
            }

            if (isDownloadsDocument(uri)) {
                Log.d(TAG, "URI is a downloads document")
                val docId = DocumentsContract.getDocumentId(uri)
                val numericId = extractNumericId(docId)
                if (numericId == null) {
                    Log.e(TAG, "Could not extract numeric ID from downloads document ID: $docId")
                    return null
                }
                try {
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        numericId.toLong()
                    )
                    val path = getDataColumn(context, contentUri, null, null)
                    Log.d(TAG, "Downloads document path: $path")
                    return path
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Error parsing numeric ID from downloads document ID: $numericId", e)
                    return null
                }
            }

            if (isMediaDocument(uri)) {
                Log.d(TAG, "URI is a media document")
                val docId = DocumentsContract.getDocumentId(uri).split(":")
                val type = docId[0]
                val contentUri: Uri? = when (type) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    MimeTypes.BASE_TYPE_VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    MimeTypes.BASE_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> {
                        Log.w(TAG, "Unsupported media document type: $type")
                        return null
                    }
                }
                val path = getDataColumn(context, contentUri!!, "_id=?", arrayOf(docId[1]))
                Log.d(TAG, "Media document path: $path")
                return path
            }

            Log.w(TAG, "Unsupported document URI: $uri")
            return null
        }

        if ("content".equals(uri.scheme, ignoreCase = true)) {
            Log.d(TAG, "URI is a content URI")
            val path = getDataColumn(context, uri, null, null)
            Log.d(TAG, "Content URI path: $path")
            return path
        }

        if ("file".equals(uri.scheme, ignoreCase = true)) {
            Log.d(TAG, "URI is a file URI")
            val path = uri.path
            Log.d(TAG, "File URI path: $path")
            return path
        }

        Log.w(TAG, "Unsupported URI scheme: ${uri.scheme}")
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * Get the file path from a content URI by querying the _data column.
     * Reconstructed from smali bytecode.
     */
    private fun getDataColumn(
        context: Context, uri: Uri, selection: String?, selectionArgs: Array<String>?
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

    /**
     * Recursively search a tree URI for a file by display name.
     * Reconstructed from smali bytecode.
     */
    private fun getPathFromTreeUri(
        context: Context, treeUri: Uri, displayName: String
    ): String? {
        val columnDocumentId = "document_id"
        val columnDisplayName = "_display_name"
        val columnMimeType = "mime_type"
        val directoryMimeType = "vnd.android.document/directory"

        var cursor: Cursor? = null
        try {
            val treeId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUri(
                treeUri.toString(), treeId
            )

            cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(columnDocumentId, columnDisplayName, columnMimeType),
                null, null, null
            )

            while (cursor != null && cursor.moveToNext()) {
                val documentId = cursor.getString(cursor.getColumnIndexOrThrow(columnDocumentId))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(columnDisplayName))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(columnMimeType))

                if (name == displayName) {
                    if (directoryMimeType == mimeType) {
                        // Directory with matching name — recurse into it
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        val path = getPathFromTreeUri(context, childUri, displayName)
                        if (path != null) return path
                    } else {
                        // File with matching name — get its path
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        return getDataColumn(context, childUri, null, null)
                    }
                } else if (directoryMimeType == mimeType) {
                    // Directory with different name — recurse anyway
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    val path = getPathFromTreeUri(context, childUri, displayName)
                    if (path != null) return path
                }
            }

            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error in getPathFromTreeUri", e)
            return null
        } finally {
            cursor?.close()
        }
    }
}
