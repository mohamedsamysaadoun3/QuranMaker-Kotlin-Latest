package hazem.nurmontage.videoquran.utils.animator

import android.os.Handler

class TimelineAnimator(
    startTimeMs: Int,
    private val maxTimeMs: Int,
    private val listener: AnimatorListener
) {

    interface AnimatorListener {
        fun onUpdate(currentTimeMs: Int)
        fun onEnd()
    }

    private var startTimeMs: Int = startTimeMs
    private var currentTimeMs: Int = startTimeMs
    private var isRunning: Boolean = false
    private var lastFrameTime: Long = 0L
    private val handler = Handler()

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            val now = System.currentTimeMillis()
            val delta = (now - lastFrameTime).toInt()
            lastFrameTime = now
            currentTimeMs += delta

            if (currentTimeMs >= maxTimeMs) {
                currentTimeMs = maxTimeMs
                listener.onUpdate(currentTimeMs)
                listener.onEnd()
                isRunning = false
                return
            }

            listener.onUpdate(currentTimeMs)
            postFrame()
        }
    }

    fun isRunning(): Boolean = isRunning

    fun getCurrentTimeMs(): Int = currentTimeMs

    fun start() {
        isRunning = true
        lastFrameTime = System.currentTimeMillis()
        postFrame()
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(updateRunnable)
    }

    private fun postFrame() {
        handler.postDelayed(updateRunnable, 16L)
    }
}
