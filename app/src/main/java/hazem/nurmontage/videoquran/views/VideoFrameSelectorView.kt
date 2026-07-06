package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.internal.view.SupportMenu

class VideoFrameSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var cornerRadius: Float = 10.0f
    private var cursorPaint: Paint = Paint()
    private var cursorX: Float = 0.0f
    private var frameBitmaps: MutableList<BitmapFrame> = ArrayList()
    private var frameCount: Int = 7
    private var frameHeight: Float = 0.0f
    private var framePaint: Paint = Paint()
    private var frameRect: RectF = RectF()
    private var frameSpacing: Float = 1.0f
    private var frameWidth: Float = 0.0f
    private var onFrameSelectedListener: OnFrameSelectedListener? = null
    private var selectedFrameIndex: Int = 0
    private var videoUri: Uri? = null

    interface OnFrameSelectedListener {
        fun onFrameSelected(value: Int, bitmap: Bitmap)
    }

    private fun init() {
        framePaint.color = -7829368
        cursorPaint.color = SupportMenu.CATEGORY_MASK
        cursorPaint.strokeWidth = 5.0f
        cursorPaint.style = Paint.Style.STROKE
    }

    fun setVideoUri(uri: Uri?) {
        this.videoUri = uri
        loadFrames()
        invalidate()
    }

    private fun loadFrames() {
        if (videoUri == null) {
            return
        }
        frameBitmaps.clear()
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            try {
                mediaMetadataRetriever.setDataSource(context, videoUri)
                val parseLong = java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(9)) / frameCount
                for (counter in 0 until frameCount) {
                    val durationMs = counter * parseLong * 1000
                    val frameAtTime = mediaMetadataRetriever.getFrameAtTime(durationMs, 2)
                    if (frameAtTime != null) {
                        frameBitmaps.add(BitmapFrame(frameAtTime, durationMs))
                    }
                }
                mediaMetadataRetriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
                mediaMetadataRetriever.release()
            }
        } catch (th: Throwable) {
            try {
                mediaMetadataRetriever.release()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
            throw th
        } catch (e3: Exception) {
            e3.printStackTrace()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val count = frameCount
        if (count > 0) {
            val f = (w * 1.0f) / count
            frameWidth = f
            frameHeight = f
            cursorX = f / 2.0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (frameBitmaps.isEmpty()) {
            val width = getWidth().toFloat()
            val height = getHeight().toFloat()
            val f = cornerRadius
            canvas.drawRoundRect(0.0f, 0.0f, width, height, f, f, framePaint)
            return
        }
        canvas.save()
        canvas.translate(0.0f, (getHeight() - frameHeight) * 0.5f)
        for (index in frameBitmaps.indices) {
            val widthRatio = frameWidth
            val f3 = index * (frameSpacing + widthRatio)
            frameRect.set(f3, 0.0f, widthRatio + f3, frameHeight)
            val f4 = cornerRadius
            canvas.drawRoundRect(frameRect, f4, f4, framePaint)
            val bitmap = frameBitmaps[index].getBitmap()
            if (bitmap != null) {
                canvas.drawBitmap(
                    bitmap,
                    Rect(0, 0, bitmap.width, bitmap.height),
                    frameRect,
                    null as Paint?
                )
            }
        }
        canvas.restore()
        val f5 = cursorX
        canvas.drawLine(f5, 0.0f, f5, getHeight().toFloat(), cursorPaint)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            val max = Math.max(0.0f, Math.min(motionEvent.x, getWidth().toFloat()))
            cursorX = max
            var count = (max / (frameWidth + frameSpacing)).toInt()
            selectedFrameIndex = count
            selectedFrameIndex = Math.max(0, Math.min(count, frameCount - 1))
            // Notify listener of frame selection (Java original set the listener but never
            // called it — this fix makes the preview actually work)
            val frame = getFrameBitmap()
            if (frame != null) {
                onFrameSelectedListener?.onFrameSelected(selectedFrameIndex, frame.getBitmap())
            }
            invalidate()
            return true
        }
        return super.onTouchEvent(motionEvent)
    }

    fun getFrameBitmap(): BitmapFrame? {
        val index = selectedFrameIndex
        if (index < 0 || index >= frameBitmaps.size) {
            return null
        }
        return frameBitmaps[index]
    }

    fun setOnFrameSelectedListener(onFrameSelectedListener: OnFrameSelectedListener?) {
        this.onFrameSelectedListener = onFrameSelectedListener
    }

    init {
        init()
    }

    inner class BitmapFrame(private val bitmap: Bitmap, private val time: Long) {
        fun getBitmap(): Bitmap = bitmap
        fun getTime(): Long = time
    }
}
