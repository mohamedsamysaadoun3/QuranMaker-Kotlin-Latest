package hazem.nurmontage.videoquran.core

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global crash handler that writes stack traces to a log file
 * so users can share error details after a crash.
 */
object CrashHandler {

    private const val CRASH_LOG_FILE = "crash_log.txt"

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(context, thread, throwable)
            } catch (_: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        // BUG-A04/A08 fix: getExternalFilesDir(null) may return null if external storage
        // is temporarily unavailable (e.g. SD card mounted over USB, early boot).
        // Fall back to internal storage (always available) so the crash is actually logged.
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, CRASH_LOG_FILE)
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        FileWriter(file, true).use { writer ->
            writer.appendLine("═══════════════════════════════════════")
            writer.appendLine("CRASH at $timestamp")
            writer.appendLine("Thread: ${thread.name}")
            writer.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            writer.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            writer.appendLine("App: ${getAppVersion(context)}")
            writer.appendLine("")
            writer.appendLine(getStackTraceString(throwable))
            writer.appendLine("")
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sb = StringBuilder()
        // BUG-A10 fix: cycle detection — some Throwable wrappers self-reference `cause`
        // (A↔B or t.cause == t), which would otherwise infinite-loop the crash handler
        // and prevent the default handler from running.
        val visited = java.util.IdentityHashMap<Throwable, Unit>()
        var t: Throwable? = throwable
        var depth = 0
        while (t != null && visited.put(t, Unit) == null && depth < 50) {
            sb.appendLine("${t::class.java.simpleName}: ${t.message}")
            t.stackTrace.take(15).forEach { frame ->
                sb.appendLine("    at $frame")
            }
            t = t.cause
            if (t != null) sb.appendLine("Caused by:")
            depth++
        }
        return sb.toString()
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pi.versionName} (${pi.longVersionCode})"
        } catch (_: Exception) {
            "unknown"
        }
    }

    /** Read the crash log contents, or null if no crashes recorded or read failed. */
    fun readCrashLog(context: Context): String? {
        // BUG-A07 fix: file.readText() throws IOException on read errors; declare null
        // as the failure signal rather than letting an exception escape.
        return try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, CRASH_LOG_FILE)
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    /** Delete the crash log file. */
    fun clearCrashLog(context: Context) {
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, CRASH_LOG_FILE)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }
}
