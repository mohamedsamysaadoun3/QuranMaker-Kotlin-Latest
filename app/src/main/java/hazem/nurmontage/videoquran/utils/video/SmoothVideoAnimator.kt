package hazem.nurmontage.videoquran.utils.video

import android.view.Choreographer
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.File
import java.util.Locale

class SmoothVideoAnimator(
    private val trackViewEntity: TrackEntityView,
    private val mTemplate: Template,
    private val fps: Int,
    private val listener: FrameUpdateListener
) : Choreographer.FrameCallback {

    interface FrameUpdateListener {
        fun onFrameUpdate(framePath: String)
        fun onAnimationEnd()
    }

    private val frameIntervalNanos: Long = (1_000_000_000.0f / fps).toLong()
    private var lastFrameTimeNanos: Long = 0L
    private var currentFrameIndex: Int = 0
    private var maxFrameIndex: Int = 0
    private var mIsPlaying: Boolean = false

    fun start() {
        mIsPlaying = true
        currentFrameIndex = Math.max(
            1,
            Math.round((trackViewEntity.current_cursur_position / 1000.0f) * fps)
        )
        maxFrameIndex = mTemplate.duration_video_media * fps
        lastFrameTimeNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        mIsPlaying = false
        Choreographer.getInstance().removeFrameCallback(this)
        listener.onAnimationEnd()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!mIsPlaying || maxFrameIndex == 0) return

        if (lastFrameTimeNanos == 0L) {
            lastFrameTimeNanos = frameTimeNanos
        }

        if (frameTimeNanos - lastFrameTimeNanos >= frameIntervalNanos) {
            lastFrameTimeNanos = frameTimeNanos

            val framePath = File(
                mTemplate.folder_template + "/VideoFrame",
                buildFrameFilePath(currentFrameIndex)
            ).absolutePath

            listener.onFrameUpdate(framePath)

            val prev = currentFrameIndex
            currentFrameIndex = (prev % maxFrameIndex) + 1
        }

        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun buildFrameFilePath(index: Int): String {
        return String.format(Locale.US, "frame_%04d.jpg", index)
    }
}
