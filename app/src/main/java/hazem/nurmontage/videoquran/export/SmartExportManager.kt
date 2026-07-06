package hazem.nurmontage.videoquran.export

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.LogCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.StatisticsCallback

/**
 * Background video export manager with progress notifications.
 *
 * Orchestrates FFmpeg-based video export with:
 * - **Foreground notification** showing export progress (0-100%)
 * - **Statistics-based progress** from FFmpeg's time position
 * - **Cancellation support** via [cancel]
 * - **Thread-safe** state management with `volatile` flags
 * - **Main-thread callbacks** via [Handler] for UI updates
 *
 * The original Java implementation used reflection to invoke FFmpegKit methods
 * because JADX-decompiled code couldn't resolve the compile-time classpath.
 * This Kotlin version uses the **direct FFmpegKit API** since the dependency
 * is properly included in the project's Gradle configuration.
 *
 * Converted from SmartExportManager.java — logic preserved, reflection removed.
 */
class SmartExportManager(context: Context) {

    companion object {
        private const val TAG = "SmartExportManager"
        private const val CHANNEL_ID = "video_export_channel"
        private const val NOTIFICATION_ID = 1001
    }

    /**
     * Callback interface for export lifecycle events.
     *
     * All methods are invoked on the **main thread** via [Handler].
     */
    interface ExportCallback {
        /** Called periodically with the export progress percentage (0-99). */
        fun onExportProgress(percent: Int)

        /** Called when the export completes successfully. */
        fun onExportComplete(outputPath: String)

        /** Called when the export fails with an error message. */
        fun onExportError(error: String)

        /** Called when the export is cancelled by the user. */
        fun onExportCancelled()
    }

    private val context: Context = context.applicationContext
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isExporting = false

    @Volatile
    private var isCancelled = false

    // BUG-X22 fix: callback was not @Volatile. Set in startExport (caller thread),
    // read in FFmpeg callbacks (FFmpeg thread). Without @Volatile the write may
    // not be visible to the callback thread.
    @Volatile
    private var callback: ExportCallback? = null

    // BUG-X17 fix: track the current FFmpeg session ID so cancel() can target
    // it instead of calling the global FFmpegKit.cancel() (which kills every
    // session in the process, including unrelated audio decode / codec checks).
    @Volatile
    private var currentSessionId: Long = -1L

    // BUG-X05 fix: total expected duration (ms) — used by StatisticsCallback to
    // compute a meaningful progress percentage for videos longer than 10s.
    private var totalDurationMs: Long = 0L

    init {
        createNotificationChannel()
    }

    /**
     * Create the notification channel for export progress (Android O+).
     *
     * Uses [NotificationManager.IMPORTANCE_LOW] so the notification
     * doesn't make sound or pop up as a heads-up.
     */
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Video Export",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows video export progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start an asynchronous FFmpeg export with progress notifications.
     *
     * The method is **idempotent** — if an export is already in progress,
     * the call is silently ignored with a warning log.
     *
     * Flow:
     * 1. Shows a "Preparing export..." notification at 0%
     * 2. Executes the FFmpeg command asynchronously with three callbacks:
     *    - [ExecuteCallback] -> handles completion (success/failure/cancelled)
     *    - [StatisticsCallback] -> reports progress based on FFmpeg's time position
     *    - [LogCallback] -> suppresses FFmpeg log output
     * 3. On completion: shows 100% notification, auto-dismisses after 3 seconds
     * 4. On failure: cancels the notification and reports the error
     *
     * Progress calculation: `percent = min(99, timeMs * 100 / totalDurationMs)`.
     * Caller must supply the expected total duration so the percentage is
     * meaningful for videos longer than 10 seconds. The 99% cap ensures the
     * notification never shows 100% before the actual completion callback.
     *
     * @param ffmpegCommand     The FFmpeg command string to execute
     * @param outputPath        The expected output file path (reported on success)
     * @param totalDurationMs   Total expected output duration in milliseconds
     *                          (used for progress percentage). If 0, falls back
     *                          to the legacy 10-second assumption.
     * @param cb                Callback for export events
     */
    fun startExport(
        ffmpegCommand: String,
        outputPath: String,
        totalDurationMs: Long,
        cb: ExportCallback
    ) {
        if (isExporting) {
            Log.w(TAG, "Export already in progress")
            return
        }
        this.callback = cb
        this.isExporting = true
        this.isCancelled = false
        this.totalDurationMs = totalDurationMs
        // BUG-X17 fix: capture session ID so cancel() can target it instead of
        // calling the global FFmpegKit.cancel() which kills every other session
        // in the process (audio decode, codec check, etc.).
        currentSessionId = -1L
        showNotification(0, "Preparing export...")

        val session = FFmpegKit.executeAsync(
            ffmpegCommand,
            FFmpegSessionCompleteCallback { session ->
                // -- Export session completed --
                isExporting = false
                currentSessionId = -1L

                val returnCode = session.returnCode
                val success = ReturnCode.isSuccess(returnCode)

                mainHandler.post {
                    if (isCancelled) {
                        notificationManager.cancel(NOTIFICATION_ID)
                        callback?.onExportCancelled()
                        return@post
                    }
                    if (success) {
                        showNotification(100, "Export complete!")
                        callback?.onExportComplete(outputPath)
                        mainHandler.postDelayed({ notificationManager.cancel(NOTIFICATION_ID) }, 3000)
                    } else {
                        val error = "Export failed"
                        notificationManager.cancel(NOTIFICATION_ID)
                        callback?.onExportError(error)
                    }
                }
            },
            LogCallback { /* Suppress FFmpeg log output */ },
            StatisticsCallback { statistics ->
                // -- Progress from FFmpeg statistics --
                // BUG-X05 fix: was `time / 100f` which equals percent ONLY if
                // total duration is 10000ms. For a 60s export, notification hit
                // 99% at t=9.9s and stayed pinned for 50s. Now divide by the
                // actual total duration (falling back to 10s if caller passed 0).
                val time = statistics.time
                if (time > 0 && callback != null) {
                    val denom = if (totalDurationMs > 0) totalDurationMs else 10_000L
                    val percent = minOf(99, ((time * 100L) / denom).toInt())
                    mainHandler.post {
                        showNotification(percent, "Exporting... $percent%")
                        callback?.onExportProgress(percent)
                    }
                }
            }
        )
        currentSessionId = session.sessionId
    }

    /**
     * Cancel the current export operation.
     *
     * Sets the [isCancelled] flag and sends a cancel signal to FFmpegKit
     * scoped to the current session ID. The [ExportCallback.onExportCancelled]
     * will be invoked when the FFmpeg session completes after the cancellation
     * signal.
     *
     * If no export is in progress, the call is silently ignored.
     */
    fun cancel() {
        if (!isExporting) return
        isCancelled = true
        // BUG-X17 fix: was FFmpegKit.cancel() (global, kills all sessions in the
        // process). Cancel only the current session.
        val sid = currentSessionId
        if (sid > 0) {
            try { FFmpegKit.cancel(sid) } catch (_: Throwable) {}
        }
    }

    /**
     * Show or update the export progress notification.
     *
     * The notification is **ongoing** (non-dismissible) while the export
     * is in progress (percent < 100), and becomes dismissible once complete.
     *
     * @param percent Progress percentage (0-100)
     * @param text    Status text to display
     */
    private fun showNotification(percent: Int, text: String) {
        val builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder.setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentTitle("Video Export")
            .setContentText(text)
            .setProgress(100, percent, false)
            .setOngoing(percent < 100)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Check whether an export is currently in progress.
     *
     * @return `true` if an export operation is active
     */
    fun isExporting(): Boolean = isExporting
}
