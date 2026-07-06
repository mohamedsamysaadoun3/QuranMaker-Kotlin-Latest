package hazem.nurmontage.videoquran.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.max

object WaveformRendererPro {

    @JvmStatic
    fun drawWave(
        width: Int, height: Int, amplitudes: FloatArray,
        color: Int, barSpacing: Float, cornerRadius: Float, progress: Float
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.alpha = 100

        val w = width.toFloat()
        val h = height.toFloat()
        val visibleBars = (amplitudes.size * progress).toInt()
        if (visibleBars < 1) return bitmap

        val barCount = visibleBars.toFloat()
        val barWidth = w / barCount
        var effectiveBarWidth = barWidth - barSpacing
        if (effectiveBarWidth < 1.0f) effectiveBarWidth = 1.0f

        // Find max amplitude for normalization
        var maxAmp = 0.0f
        for (amp in amplitudes) {
            maxAmp = max(maxAmp, amp)
        }
        if (maxAmp < 0.01f) maxAmp = 0.01f

        var x = 0.0f
        for (i in 0 until visibleBars) {
            val ampIndex = (i / barCount * amplitudes.size).toInt()
            val barHeight = (amplitudes[ampIndex] / maxAmp) * h
            canvas.drawRoundRect(
                RectF(x, canvas.height - barHeight, x + effectiveBarWidth, canvas.height.toFloat()),
                cornerRadius, cornerRadius, paint
            )
            x += barWidth
        }

        return bitmap
    }

    @JvmStatic
    fun drawWaveInRect(
        canvas: Canvas, rect: RectF, amplitudes: FloatArray,
        color: Int, barSpacing: Float, cornerRadius: Float, progress: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.alpha = 100

        val rectWidth = rect.width()
        val rectHeight = rect.height() * 0.85f
        val visibleBars = (amplitudes.size * progress).toInt()
        if (visibleBars < 1) return

        val barCount = visibleBars.toFloat()
        val barWidth = rectWidth / barCount
        var effectiveBarWidth = barWidth - barSpacing
        if (effectiveBarWidth < 1.0f) effectiveBarWidth = 1.0f

        var maxAmp = 0.0f
        for (amp in amplitudes) {
            maxAmp = max(maxAmp, amp)
        }
        if (maxAmp < 0.01f) maxAmp = 0.01f

        var x = rect.left
        for (i in 0 until visibleBars) {
            val ampIndex = (i / barCount * amplitudes.size).toInt()
            val barHeight = (amplitudes[ampIndex] / maxAmp) * rectHeight
            canvas.drawRoundRect(
                RectF(x, rect.bottom - barHeight, x + effectiveBarWidth, rect.bottom),
                cornerRadius, cornerRadius, paint
            )
            x += barWidth
        }
    }

    @JvmStatic
    fun drawWaveProportional(
        canvas: Canvas, rect: RectF, amplitudes: FloatArray,
        color: Int, barSpacing: Float, cornerRadius: Float,
        scale: Float, scrollOffset: Float, barWidthBase: Float
    ) {
        if (amplitudes.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.alpha = 100

        val rectHeight = rect.height() * 0.85f
        val stepSize = (barWidthBase + barSpacing) * scale
        val startBin = (scrollOffset / stepSize).toInt()
        val safeStartBin = maxOf(0, startBin)
        val endBin = (rect.width() / stepSize).toInt() + 2 + safeStartBin
        val safeEndBin = minOf(endBin, amplitudes.size)

        var maxAmp = 0.0f
        for (amp in amplitudes) {
            if (amp > maxAmp) maxAmp = amp
        }
        if (maxAmp < 0.01f) maxAmp = 0.01f

        var x = rect.left - (scrollOffset % stepSize)
        val numBars = safeEndBin - safeStartBin
        for (i in 0 until numBars) {
            var ampIndex = ((i.toFloat() / numBars) * (amplitudes.size - safeStartBin)).toInt() + safeStartBin
            if (ampIndex >= amplitudes.size) ampIndex = amplitudes.size - 1

            val barHeight = (amplitudes[ampIndex] / maxAmp) * rectHeight
            canvas.drawRoundRect(
                RectF(x, rect.bottom - barHeight, barWidthBase * scale + x, rect.bottom),
                cornerRadius, cornerRadius, paint
            )
            x += stepSize
            if (x > rect.right) return
        }
    }

    @JvmStatic
    fun drawWaveInRect(
        canvas: Canvas, rect: RectF, amplitudes: FloatArray,
        color: Int, barSpacing: Float, cornerRadius: Float,
        scale: Float, barWidthBase: Float, scrollOffset: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.alpha = 100

        val rectHeight = rect.height() * 0.85f
        val scaledBarWidth = barWidthBase * scale
        val stepSize = (barWidthBase + barSpacing) * scale

        var startBin = (scrollOffset / stepSize).toInt()
        if (startBin < 0) startBin = 0

        val endBin = (rect.width() / stepSize).toInt() + 2 + startBin
        val safeEndBin = minOf(endBin, amplitudes.size)

        var maxAmp = 0.0f
        for (amp in amplitudes) {
            maxAmp = max(maxAmp, amp)
        }
        if (maxAmp < 0.01f) maxAmp = 0.01f

        var x = rect.left
        var i = startBin
        while (i < safeEndBin) {
            val barHeight = (amplitudes[i] / maxAmp) * rectHeight
            canvas.drawRoundRect(
                RectF(x, rect.bottom - barHeight, x + scaledBarWidth, rect.bottom),
                cornerRadius, cornerRadius, paint
            )
            x += stepSize
            i++
        }
    }

    @JvmStatic
    fun drawWaveInRect(
        canvas: Canvas, rect: RectF, amplitudes: FloatArray,
        color: Int, barSpacing: Float, cornerRadius: Float,
        progress: Float, offsetIndex: Int
    ) {
        if (amplitudes.isEmpty()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.alpha = 100

        val rectWidth = rect.width()
        val rectHeight = rect.height() * 0.85f
        val visibleBars = (amplitudes.size * progress).toInt()
        if (visibleBars < 1) return

        var startIdx = if (offsetIndex < 0) 0 else offsetIndex
        if (startIdx > amplitudes.size - visibleBars) {
            startIdx = amplitudes.size - visibleBars
        }

        val barWidth = rectWidth / visibleBars.toFloat()
        val effectiveBarWidth = max(1.0f, barWidth - barSpacing)

        var maxAmp = 0.0f
        for (i in startIdx until startIdx + visibleBars) {
            maxAmp = max(maxAmp, amplitudes[i])
        }
        if (maxAmp < 0.01f) maxAmp = 0.01f

        val barRect = RectF()
        var x = rect.left
        for (i in 0 until visibleBars) {
            val ampIndex = startIdx + i
            if (ampIndex >= amplitudes.size) return
            barRect.set(x, rect.bottom - (amplitudes[ampIndex] / maxAmp) * rectHeight, x + effectiveBarWidth, rect.bottom)
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, paint)
            x += barWidth
        }
    }

    @JvmStatic
    fun drawWaveformBottom(
        amplitudes: FloatArray, width: Int, height: Int,
        color: Int, gap: Int, cornerRadius: Float, barWidth: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color

        var maxAmp = 0.0f
        for (amp in amplitudes) {
            maxAmp = max(maxAmp, amp)
        }
        if (maxAmp < 0.01f) maxAmp = 0.01f

        var x = 0
        val bitmapHeight = bitmap.height
        for (amp in amplitudes) {
            val barHeight = ((amp / maxAmp) * bitmapHeight).toInt()
            canvas.drawRoundRect(
                RectF(x.toFloat(), (height - barHeight).toFloat(), (x + barWidth).toFloat(), height.toFloat()),
                cornerRadius, cornerRadius, paint
            )
            x += barWidth + gap
        }

        return bitmap
    }
}
