package hazem.nurmontage.videoquran.utils.animator

import android.os.SystemClock
import android.view.Choreographer

class SmoothTimelineAnimator(
    startCursorMs: Int,
    private val maxTimeMs: Int,
    private val listener: AnimatorListener
) {

    interface AnimatorListener {
        fun onUpdate(currentTimeMs: Int)
        fun onEnd()
    }

    private var startCursorMs: Int = startCursorMs
    private var currentTimeMs: Int = 0
    private var startTimeMs: Long = 0L
    private var isRunning: Boolean = false

    private lateinit var frameCallback: Choreographer.FrameCallback

    init {
        frameCallback = Choreographer.FrameCallback {
            if (isRunning) {
                val elapsed = (SystemClock.uptimeMillis() - startTimeMs).toInt()
                currentTimeMs = startCursorMs + elapsed

                if (currentTimeMs >= maxTimeMs) {
                    listener.onUpdate(maxTimeMs)
                    listener.onEnd()
                    isRunning = false
                } else {
                    listener.onUpdate(currentTimeMs)
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            }
        }
    }

    fun isRunning(): Boolean = isRunning

    fun getCurrentTimeMs(): Int = currentTimeMs

    fun start() {
        isRunning = true
        startTimeMs = SystemClock.uptimeMillis()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stop() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
