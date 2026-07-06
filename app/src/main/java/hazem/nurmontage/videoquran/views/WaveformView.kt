package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Interactive waveform visualization view.
 *
 * Renders an amplitude-based waveform with progress tracking. Bars before
 * the current progress position are drawn in white; bars after are drawn
 * in dark gray. Touch/drag interaction allows the user to seek by tapping
 * or sliding across the waveform.
 *
 * Ported from the original reverse-engineered Java implementation.
 */
class WaveformView : View {

    private var amplitudes: IntArray = intArrayOf(
        30, 40, 60, 80, 50, 90, 100, 70, 40, 60, 80, 50, 30, 50, 70, 90, 60, 40
    )

    private var listener: OnWaveformClickListener? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var progress: Float = 0f

    interface OnWaveformClickListener {
        fun onProgressChanged(progress: Float)
    }

    fun setOnWaveformClickListener(listener: OnWaveformClickListener) {
        this.listener = listener
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidate()
    }

    fun setAmplitudes(amplitudes: IntArray) {
        this.amplitudes = amplitudes
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN ||
            event.action == MotionEvent.ACTION_MOVE
        ) {
            var x = event.x / width
            x = x.coerceIn(0f, 1f)
            setProgress(x)
            listener?.onProgressChanged(x)
            return true
        }
        return super.onTouchEvent(event)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val barWidth = viewWidth / (amplitudes.size * 2)
        val barPlusGapWidth = barWidth * 2

        for (i in amplitudes.indices) {
            val barHeight = (amplitudes[i] / 100.0f) * viewHeight
            val x = i * barPlusGapWidth
            val y = (viewHeight - barHeight) / 2f

            val fraction = i.toFloat() / amplitudes.size
            if (progress > 0f && fraction < progress) {
                paint.color = -1           // white (0xFFFFFFFF) — played portion
            } else {
                paint.color = -12303292    // dark gray (0xFF444444) — unplayed portion
            }

            canvas.drawRoundRect(x, y, x + barWidth, y + barHeight, 5f, 5f, paint)
        }
    }
}
