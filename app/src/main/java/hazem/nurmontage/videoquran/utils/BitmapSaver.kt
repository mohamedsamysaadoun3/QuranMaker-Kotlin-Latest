package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Saves bitmaps to the app's external files directory as JPEG files.
 *
 * The saved files are stored under [Context.getExternalFilesDir], which is
 * the app-private external storage. These files are automatically deleted
 * when the app is uninstalled.
 *
 * The JPEG quality is fixed at 90%, providing a good balance between
 * file size and visual quality for preview/thumbnail images.
 *
 * Usage:
 * ```kotlin
 * val saver = BitmapSaver(context)
 * val success = saver.saveBitmap(bitmap, "thumbnails/frame_001.jpg")
 * ```
 *
 * Converted from BitmapSaver.java — logic preserved with Kotlin `use{}` for safe stream cleanup.
 */
class BitmapSaver(private val context: Context) {

    companion object {
        /** JPEG compression quality (0-100). 90 = high quality. */
        private const val JPEG_QUALITY = 90
    }

    /**
     * Save a [Bitmap] to the app's external files directory.
     *
     * The file is written to `context.getExternalFilesDir(null)/<relativePath>`
     * as a JPEG with [JPEG_QUALITY] compression.
     *
     * The method handles all stream cleanup safely — if writing fails partway,
     * the output stream is closed properly and `false` is returned.
     *
     * @param bitmap       The bitmap to save (must not be recycled)
     * @param relativePath The path relative to the external files directory
     *                     (e.g. "thumbnails/frame_001.jpg")
     * @return `true` if the bitmap was saved successfully, `false` on any error
     *         (null external storage, I/O failure)
     */
    fun saveBitmap(bitmap: Bitmap, relativePath: String): Boolean {
        val externalDir = context.getExternalFilesDir(null) ?: return false

        try {
            val outputFile = File(externalDir.absolutePath + "/" + relativePath)
            FileOutputStream(outputFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                outputStream.flush()
            }
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
}
