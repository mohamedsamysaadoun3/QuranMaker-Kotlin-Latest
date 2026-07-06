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
        val file = File(context.getExternalFilesDir(null), CRASH_LOG_FILE)
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
        var t: Throwable? = throwable
        while (t != null) {
            sb.appendLine("${t::class.java.simpleName}: ${t.message}")
            t.stackTrace.take(15).forEach { frame ->
                sb.appendLine("    at $frame")
            }
            t = t.cause
            if (t != null) sb.appendLine("Caused by:")
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

    /** Read the crash log contents, or null if no crashes recorded. */
    fun readCrashLog(context: Context): String? {
        val file = File(context.getExternalFilesDir(null), CRASH_LOG_FILE)
        return if (file.exists()) file.readText() else null
    }

    /** Delete the crash log file. */
    fun clearCrashLog(context: Context) {
        val file = File(context.getExternalFilesDir(null), CRASH_LOG_FILE)
        if (file.exists()) file.delete()
    }
}
