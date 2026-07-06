package hazem.nurmontage.videoquran.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ProgressBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.ui.engine.EngineActivity
import hazem.nurmontage.videoquran.ui.home.WorkUserActivity
import hazem.nurmontage.videoquran.utils.AudioUploadHelper
import hazem.nurmontage.videoquran.utils.LocalPersistence
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Activity for handling incoming shared media from other apps.
 *
 * Originally: ShareWithMeActivity.java
 * Converted to: ShareWithMeActivity.kt — full logic match with JADX source
 *
 * Accepts SEND intent with the following MIME types:
 *   - image (all types) — Image files (backgrounds, overlays)
 *   - audio (all types) — Audio files (recitations, nasheeds)
 *   - video (all types) — Video files (extracts audio track via FFmpeg, then uses as background)
 *
 * Flow:
 *   1. Receives the shared content URI from the intent
 *   2. Determines the MIME type and routes to the appropriate handler
 *   3. Image: copies to permanent storage, launches Engine with img_bg extra
 *   4. Audio: processes via AudioUploadHelper, launches Engine with muri extra
 *   5. Video: extracts audio track via FFmpeg (try copy first, fallback to AAC re-encode)
 *      then launches Engine with the extracted audio path
 *   6. If no MIME type: launches WorkUserActivity (home screen)
 *
 * FFmpeg video processing has a two-stage fallback:
 *   - Stage 1: Try to copy the audio stream as-is (fast, lossless)
 *   - Stage 2: If copy fails, re-encode to AAC 192kbps (slower, universal)
 *   - Stage 3: If both fail, fall back to WorkUserActivity
 */
class ShareWithMeActivity : BaseActivity() {

    private var progressBar: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_with_me)

        // Light status/navigation bar for white background
        setStatusBarColor(-1)
        setNavigationBarColor(-1)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        progressBar = findViewById(R.id.progress_horizontal)

        // Route the intent to the appropriate handler
        // Note: deleteTemplate is called inside handleIntent after null check,
        // matching the original Java flow (only called once, not in onCreate).
        handleIntent(intent)
    }

    /**
     * Routes the incoming intent based on its MIME type.
     * If no type is set, launches the home screen.
     */
    private fun handleIntent(intent: Intent?) {
        val type = intent?.type
        if (type == null) {
            // No MIME type — launch home screen
            startActivity(Intent(this, WorkUserActivity::class.java))
            finish()
            return
        }

        // Clean up any previous temporary template
        LocalPersistence.deleteTemplate(this, Constants.TEMPLATE_TMP)

        when {
            type.startsWith("image/") -> handleImg(intent)
            type.startsWith("audio/") -> handleAudio(intent)
            type.startsWith("video/") -> handleVideo(intent)
        }
    }

    // ═══════════════════════════════════════════════
    //  Image handling
    // ═══════════════════════════════════════════════

    /**
     * Handles a shared image by saving it to permanent storage,
     * then launching EngineActivity with the file path as "img_bg" extra.
     */
    private fun handleImg(intent: Intent) {
        val uri = getParcelable<Uri>(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (uri != null) {
            val engineIntent = Intent(this, EngineActivity::class.java)
            engineIntent.putExtra("img_bg", savePermanent(uri))
            startActivity(engineIntent)
            finish()
        }
    }

    /**
     * Copies an image from a content URI to a permanent file in internal storage.
     * The file is named with a timestamp to avoid collisions.
     *
     * @param uri The content URI of the image
     * @return The absolute path of the saved file, or null on failure
     */
    private fun savePermanent(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File(filesDir, "img_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream!!.read(buffer).also { bytesRead = it } > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ═══════════════════════════════════════════════
    //  Audio handling
    // ═══════════════════════════════════════════════

    /**
     * Handles a shared audio file by processing it via AudioUploadHelper,
     * then launching EngineActivity with the audio file path as "muri" extra.
     */
    private fun handleAudio(intent: Intent) {
        val uri = getParcelable<Uri>(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (uri != null) {
            val audioFile = AudioUploadHelper.processAudioUriForUpload(
                this, uri, "share_with_me.mp3"
            )
            val engineIntent = Intent(this, EngineActivity::class.java)
            engineIntent.data = uri
            engineIntent.putExtra("muri", audioFile?.absolutePath)
            startActivity(engineIntent)
            finish()
        }
    }

    // ═══════════════════════════════════════════════
    //  Video handling (FFmpeg audio extraction)
    // ═══════════════════════════════════════════════

    /**
     * Handles a shared video by extracting its audio track via FFmpeg,
     * then launching EngineActivity with the extracted audio path.
     */
    private fun handleVideo(intent: Intent) {
        val uri = getParcelable<Uri>(intent, Intent.EXTRA_STREAM, Uri::class.java)
        if (uri != null) {
            processVideo(uri)
        }
    }

    /**
     * Copies a video from a content URI to the cache directory.
     * FFmpeg requires a file path (cannot read from content:// URIs).
     *
     * @param uri The content URI of the video
     * @return The cached video file
     */
    private fun copyVideoToCache(uri: Uri): File {
        val file = File(cacheDir, "temp_video.mp4")
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream!!.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    /**
     * Extracts the audio track from a shared video file using FFmpeg.
     *
     * Two-stage extraction strategy:
     *   - Stage 1: Try to copy the audio stream as-is (fast, lossless)
     *   - Stage 2: If copy fails, re-encode to AAC 192kbps (slower, universal)
     *
     * @param uri The content URI of the video
     */
    private fun processVideo(uri: Uri) {
        progressBar?.visibility = View.VISIBLE

        val videoPath = copyVideoToCache(uri).absolutePath
        val audioOutputPath = "${getExternalFilesDir(null)}/share_with_me.m4a"

        // Stage 1: Try to copy audio stream without re-encoding (fast)
        FFmpegKit.executeAsync(
            "-y -i \"$videoPath\" -vn -map 0:a? -c:a copy \"$audioOutputPath\"",
            FFmpegSessionCompleteCallback { session ->
                if (ReturnCode.isSuccess(session.returnCode)) {
                    // Audio extraction succeeded — launch engine on main thread
                    runOnUiThread {
                        progressBar?.visibility = View.GONE
                        toEngine(Uri.parse(audioOutputPath), audioOutputPath)
                    }
                } else {
                    // Copy failed — try re-encoding to AAC (Stage 2)
                    FFmpegKit.executeAsync(
                        "-y -i \"$videoPath\" -vn -map 0:a? -c:a aac -b:a 192k \"$audioOutputPath\"",
                        FFmpegSessionCompleteCallback { retrySession ->
                            if (ReturnCode.isSuccess(retrySession.returnCode)) {
                                runOnUiThread {
                                    progressBar?.visibility = View.GONE
                                    toEngine(Uri.parse(audioOutputPath), audioOutputPath)
                                }
                            } else {
                                // Both extraction attempts failed — fall back to home
                                runOnUiThread {
                                    progressBar?.visibility = View.GONE
                                    startActivity(Intent(this, WorkUserActivity::class.java))
                                    finish()
                                }
                            }
                        }
                    )
                }
            }
        )
    }

    // ═══════════════════════════════════════════════
    //  Engine launcher
    // ═══════════════════════════════════════════════

    /**
     * Launches EngineActivity with the extracted audio URI and file path.
     *
     * @param uri The URI of the audio file
     * @param path The absolute path of the audio file
     */
    private fun toEngine(uri: Uri, path: String) {
        val intent = Intent(this, EngineActivity::class.java)
        intent.data = uri
        intent.putExtra("muri", path)
        startActivity(intent)
        finish()
    }

    // ═══════════════════════════════════════════════
    //  Utility: Parcelable getter with SDK compat
    // ═══════════════════════════════════════════════

    /**
     * Gets a Parcelable extra from an Intent with API level compatibility.
     * On API 33+ (Android 13), uses the type-safe method.
     * On older APIs, uses the deprecated but necessary method.
     *
     * @param intent The source intent
     * @param key The extra key
     * @param clazz The expected Parcelable class
     * @return The Parcelable value, or null if not found
     */
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun <T : Parcelable> getParcelable(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(key, clazz)
        } else {
            intent.getParcelableExtra(key) as? T
        }
    }

    // ═══════════════════════════════════════════════
    //  BaseActivity compat methods
    // ═══════════════════════════════════════════════

    // setStatusBarColor and setNavigationBarColor inherited from BaseActivity
}
