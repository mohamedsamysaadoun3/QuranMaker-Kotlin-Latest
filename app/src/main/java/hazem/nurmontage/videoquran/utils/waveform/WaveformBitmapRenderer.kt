package hazem.nurmontage.videoquran.utils.waveform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF

class WaveformBitmapRenderer(
    amps: FloatArray?,
    bitmapWidth: Int,
    bitmapHeight: Int,
    waveColor: Int
) {
    private val amps: FloatArray? = amps
    private val bitmapWidth: Int = bitmapWidth
    private val bitmapHeight: Int = bitmapHeight
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = waveColor
        alpha = 100
    }
    private var waveformBitmap: Bitmap? = null

    init {
        generateBitmap()
    }

    fun drawOverlay(canvas: Canvas, rectF: RectF, f: Float, f2: Float, paint: Paint) {
    }

    private fun generateBitmap() {
        val amplitudes = amps ?: return
        if (amplitudes.isEmpty()) return

        waveformBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(waveformBitmap!!)

        val usableHeight = bitmapHeight * 0.85f

        var peak = 0f
        for (amp in amplitudes) {
            if (amp > peak) peak = amp
        }
        val normalizer = if (peak < 0.01f) 0.01f else peak

        for (x in 0 until bitmapWidth) {
            var index = (x.toFloat() / bitmapWidth * amplitudes.size).toInt()
            if (index >= amplitudes.size) {
                index = amplitudes.size - 1
            }
            val barHeight = (amplitudes[index] / normalizer) * usableHeight
            canvas.drawLine(x.toFloat(), bitmapHeight.toFloat(), x.toFloat(), bitmapHeight - barHeight, paint)
        }
    }

    fun draw(canvas: Canvas, rectF: RectF, scaleFactor: Float, offset: Float) {
        val bitmap = waveformBitmap ?: return
        val translateX = rectF.left - (offset * scaleFactor)
        val matrix = Matrix()
        matrix.postScale(scaleFactor, 1.0f)
        matrix.postTranslate(translateX, rectF.top)
        canvas.drawBitmap(bitmap, matrix, null)
    }

    fun setColor(color: Int) {
        paint.color = color
        generateBitmap()
    }

    fun getBitmap(): Bitmap? = waveformBitmap

    fun release() {
        waveformBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        waveformBitmap = null
    }
}
