package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper for processing shared/uploaded audio URIs into accessible local files.
 *
 * Originally: AudioUploadHelper.java (JADX failed to decompile — reconstructed from smali)
 * Converted to: AudioUploadHelper.kt — idiomatic Kotlin, full logic preserved
 *
 * Architecture:
 * - Takes a content URI (from sharing or file picker) and copies its audio data
 *   to a file in the app's external storage directory
 * - The output file name is specified by the caller (e.g., "share_with_me.mp3")
 * - Uses a 4KB buffer for efficient streaming copy
 * - Handles FileNotFoundException (permission issues) and IOException separately
 * - Properly closes InputStream in all code paths (try-finally / use block)
 * - Returns null on any failure, with appropriate error logging
 * - On failure, cleans up any partially-written file to prevent stale data
 *
 * This helper is used by [ShareWithMeActivity] when processing shared audio files,
 * and by other components that need to convert content URIs into file paths
 * (required by FFmpeg which cannot read from content:// URIs directly).
 */
object AudioUploadHelper {

    private const val TAG = "AudioUploadHelper"

    /**
     * Processes an audio content URI by copying its data to a local file.
     *
     * The method opens an InputStream from the content resolver, creates an
     * output file in the app's external files directory with the specified name,
     * and streams the data using a 4KB buffer.
     *
     * On success, returns the output [File] with the copied audio data.
     * On failure (file not found, I/O error, null stream), returns null and
     * cleans up any partially-written output file.
     *
     * @param context The application context for content resolution and file access
     * @param uri The content URI of the audio to process
     * @param fileName The desired output file name (e.g., "share_with_me.mp3")
     * @return The output [File] containing the audio data, or null on failure
     */
    fun processAudioUriForUpload(context: Context, uri: Uri, fileName: String): File? {
        var inputStream: java.io.InputStream? = null
        var outputFile: File? = null

        try {
            // Open input stream from the content URI
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open InputStream for URI: $uri")
                return null
            }

            // Create output file in app's external storage
            outputFile = File(context.getExternalFilesDir(null), fileName)

            // Stream copy with 4KB buffer
            val buffer = ByteArray(4096)
            FileOutputStream(outputFile).use { outputStream ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }

            Log.d(TAG, "Audio content copied to cache file: ${outputFile.absolutePath}")
            return outputFile

        } catch (e: FileNotFoundException) {
            Log.e(TAG, "File not found for URI (or permission issue): $uri", e)
            // Clean up partial file on error
            outputFile?.let { if (it.exists()) it.delete() }
            return null

        } catch (e: IOException) {
            Log.e(TAG, "IOException while processing URI: $uri", e)
            // Clean up partial file on error
            outputFile?.let { if (it.exists()) it.delete() }
            return null

        } finally {
            // Always close the input stream
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing InputStream", e)
            }
        }
    }
}
