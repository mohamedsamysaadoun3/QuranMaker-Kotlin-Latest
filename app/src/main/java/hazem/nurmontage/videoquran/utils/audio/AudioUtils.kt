package hazem.nurmontage.videoquran.utils.audio

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Utility for copying audio files from URIs or HTTP URLs to local storage.
 *
 * Supports two sources:
 * 1. **Content URI** → [copyFromUri] reads via ContentResolver and writes to a local file.
 * 2. **HTTP URL** → [downloadFile] downloads via [HttpURLConnection] and writes to a local file.
 *
 * All I/O operations run on a single-thread [ExecutorService] and deliver
 * results on the main thread via [Handler].
 *
 * Converted from AudioUtils.java — logic preserved with `use{}` for stream cleanup.
 */
object AudioUtils {

    private const val TAG = "AudioUtils"
    internal val executor: ExecutorService = Executors.newSingleThreadExecutor()
    internal val mainHandler = Handler(Looper.getMainLooper())

    /** Callback interface for async audio copy operations. */
    interface Callback {
        fun onSuccess(localPath: String)
        fun onError(exception: Exception)
    }

    /**
     * Asynchronously copy an audio file from [source] (URI or HTTP URL)
     * to the local directory at [destDir], delivering results via [callback].
     */
    fun copyToLocalAsync(context: Context, source: String, destDir: String, callback: Callback) {
        executor.execute {
            try {
                val localPath = if (source.startsWith("http")) {
                    downloadFile(context, source, destDir)
                } else {
                    copyFromUri(context, Uri.parse(source), destDir)
                }
                mainHandler.post {
                    if (localPath != null) {
                        callback.onSuccess(localPath)
                    } else {
                        callback.onError(Exception("Failed to process file"))
                    }
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onError(e) }
            }
        }
    }

    /**
     * Download a file from an HTTP URL to the local [destDir].
     *
     * The file is saved with a timestamped name: `audio_<millis>.mp3`.
     * Connection timeout: 15 seconds. Read timeout: 15 seconds.
     *
     * @return The absolute path of the downloaded file, or null on failure
     */
    internal fun downloadFile(context: Context, urlStr: String, destDir: String): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 15000
            }
            connection.connect()

            if (connection.responseCode != 200) {
                Log.e(TAG, "HTTP error: ${connection.responseCode}")
                return null
            }

            val dir = File(destDir)
            if (!dir.exists()) dir.mkdirs()

            val destFile = File(dir, "audio_${System.currentTimeMillis()}.mp3")

            connection.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Copy audio data from a content [uri] to the local [destDir].
     *
     * The file name is extracted from the ContentResolver cursor if available,
     * otherwise a timestamped name is used. A UUID suffix is appended to
     * prevent collisions.
     *
     * @return The absolute path of the copied file, or null on failure
     */
    fun copyFromUri(context: Context, uri: Uri, destDir: String): String? {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            val dir = File(destDir)
            if (!dir.exists()) dir.mkdirs()

            val fileName = getFileName(context, uri)
            val destName = if (fileName != null) addUniqueSuffix(fileName)
                           else "audio_${System.currentTimeMillis()}.mp3"

            val destFile = File(dir, destName)

            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "URI copy error", e)
            return null
        }
    }

    /**
     * Extract the display name from a content URI using ContentResolver.
     * Falls back to [Uri.getLastPathSegment] if the column is not available.
     */
    internal fun getFileName(context: Context, uri: Uri): String? {
        if ("content" == uri.scheme) {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex("_display_name")
                    if (columnIndex != -1) {
                        return cursor.getString(columnIndex)
                    }
                }
            }
        }
        return uri.lastPathSegment
    }

    /**
     * Append a UUID before the file extension to ensure uniqueness.
     * Example: `song.mp3` → `song_<uuid>.mp3`
     */
    internal fun addUniqueSuffix(fileName: String): String {
        val dotIndex = fileName.lastIndexOf(".")
        return if (dotIndex > 0) {
            fileName.substring(0, dotIndex) + "_" + UUID.randomUUID() + fileName.substring(dotIndex)
        } else {
            "${fileName}_${UUID.randomUUID()}"
        }
    }
}
