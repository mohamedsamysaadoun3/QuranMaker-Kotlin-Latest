package hazem.nurmontage.videoquran.utils

import android.util.Pair
import java.util.Locale
import java.util.concurrent.TimeUnit

class TimeFormatter(totalDurationMs: Long = 0L) {

    companion object {
        fun timeToString(ms: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(ms)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) - TimeUnit.HOURS.toMinutes(hours)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toMinutes(
                TimeUnit.MILLISECONDS.toMinutes(ms)
            )
            val millis = TimeUnit.MILLISECONDS.toMillis(ms) - TimeUnit.SECONDS.toMillis(
                TimeUnit.MILLISECONDS.toSeconds(ms)
            )
            return "$hours:$minutes:$seconds.$millis"
        }
    }

    private var totalDurationMs: Long = totalDurationMs

    fun setTotalDurationMs(totalDurationMs: Long) {
        this.totalDurationMs = totalDurationMs
    }

    fun formatTime(currentMs: Long): Pair<String, String> {
        return Pair(formatMsToTime(currentMs), formatMsToTime(totalDurationMs - currentMs))
    }

    private fun formatMsToTime(ms: Long): String {
        var time = ms
        if (time < 0) time = 0
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.ENGLISH, "%d:%02d", minutes, seconds)
    }
}
