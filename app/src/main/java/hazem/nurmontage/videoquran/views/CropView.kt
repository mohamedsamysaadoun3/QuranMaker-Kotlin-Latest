package hazem.nurmontage.videoquran.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.utils.UtilsBitmap
class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CropView"
        private const val HINT_ANIMATION_DURATION = 700L
        private const val HINT_ANIMATION_REPEATS = 3
        private const val HINT_SCALE_MAX_FACTOR = 1.0f
        private const val HINT_SCALE_MIN_FACTOR = 0.85f
    }

    // Callback interface
    interface ICropCallback {
        fun onSizeChange()
    }

    // ──────────────────────────────────────────────
    //  Fields
    // ──────────────────────────────────────────────

    private var bitmap: Bitmap? = null
    private val bitmapPaint: Paint
    private val cropPaint: Paint
    private var cropRect: RectF = RectF()
    private var hintAnimationPlayed: Boolean = false
    private var hintAnimator: ValueAnimator? = null
    private var iCropCallback: ICropCallback? = null

    private var initialHintRectCenterX: Float = 0f
    private var initialHintRectCenterY: Float = 0f
    private var initialHintRectHeight: Float = 0f
    private var initialHintRectWidth: Float = 0f

    private var isDragging: Boolean = false
    private var lastFocusX: Float = 0f
    private var lastFocusY: Float = 0f

    private var mCanvasHeight: Float = 0f
    private var mCanvasWidth: Float = 0f
    private var mDrawingX: Float = 0f
    private var mDrawingY: Float = 0f
    private var mHeight: Float = 0f
    private var mWidth: Float = 0f

    private var matrix: Matrix = Matrix()
    private var minH: Float = 0f
    private var minW: Float = 0f
    private var radius: Int = 0
    private var scale: Float = 0f
    private var scaleFactor: Float = 1.0f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var touchTolerance: Int = 10

    // ──────────────────────────────────────────────
    //  Public getters / setters
    // ──────────────────────────────────────────────

    fun setiCropCallback(callback: ICropCallback?) {
        this.iCropCallback = callback
    }

    fun setmDrawingX(cropScale: Float) {
        this.mDrawingX = cropScale
    }

    fun setmDrawingY(cropScale: Float) {
        this.mDrawingY = cropScale
    }

    fun getmDrawingX(): Float = mDrawingX

    fun getmDrawingY(): Float = mDrawingY
    fun getRectSquare(): Rect {
        return Rect(
            Math.round(cropRect.left / scale),
            Math.round(cropRect.top / scale),
            Math.round(cropRect.right / scale),
            Math.round(cropRect.bottom / scale)
        )
    }
    fun setBitmap(bmp: Bitmap, rect: Rect, radiusPx: Int, isFlag: Boolean) {
        this.bitmap = bmp
        this.radius = radiusPx
        this.mCanvasWidth = (width - paddingStart - paddingEnd).toFloat()
        this.mCanvasHeight = (height - paddingTop - paddingBottom).toFloat()

        val bmpWidth = bmp.width.toFloat()
        val bmpHeight = bmp.height.toFloat()
        val minScale = Math.min(mCanvasWidth / bmpWidth, mCanvasHeight / bmpHeight)
        this.scale = minScale

        val scaledWidth = bmpWidth * minScale
        this.mWidth = scaledWidth
        val scaledHeight = bmpHeight * minScale
        this.mHeight = scaledHeight

        this.mDrawingX = (mCanvasWidth - scaledWidth) * 0.5f
        this.mDrawingY = (mCanvasHeight - scaledHeight) * 0.5f

        val newMatrix = Matrix()
        this.matrix = newMatrix
        newMatrix.postScale(minScale, minScale)

        this.cropRect = RectF(
            rect.left * scale,
            rect.top * scale,
            rect.right * scale,
            rect.bottom * scale
        )
        this.minH = Common.minSquareH * scale
        this.minW = Common.minSquareW * scale

        invalidate()

        if (isFlag || width <= 0 || height <= 0) return

        this.initialHintRectWidth = cropRect.width()
        this.initialHintRectHeight = cropRect.height()
        this.initialHintRectCenterX = cropRect.centerX()
        this.initialHintRectCenterY = cropRect.centerY()
        startHintAnimation()
    }
    fun setBitmapLast(bmp: Bitmap, rect: Rect, radiusPx: Int, isFlag: Boolean) {
        this.bitmap = bmp
        this.cropRect = RectF(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat(), rect.bottom.toFloat())
        this.radius = radiusPx
        this.mCanvasWidth = (width - paddingStart - paddingEnd).toFloat()
        val viewHeight = (height - paddingTop - paddingBottom).toFloat()
        this.mCanvasHeight = viewHeight
        this.mDrawingY = (viewHeight - bmp.height) * 0.5f
        this.mWidth = mCanvasWidth
        this.mHeight = bmp.height.toFloat()

        val cropScale = mCanvasWidth / mWidth
        val m = Matrix()
        m.postScale(cropScale, cropScale)
        m.postTranslate(0f, mDrawingY)

        invalidate()

        if (isFlag || width <= 0 || height <= 0) return

        this.initialHintRectWidth = cropRect.width()
        this.initialHintRectHeight = cropRect.height()
        this.initialHintRectCenterX = cropRect.centerX()
        this.initialHintRectCenterY = cropRect.centerY()
        startHintAnimation()
    }
    fun getmY(): Float {
        if (bitmap == null) return 0.4f
        return Math.max(cropRect.top / mHeight, 0f)
    }
    fun getmX(): Float {
        if (bitmap == null) return 0.4f
        return Math.max(cropRect.left / mWidth, 0f)
    }
    fun getmW(): Float {
        if (bitmap == null) return 1.0f
        return cropRect.width() / mWidth
    }
    fun getmH(): Float {
        if (bitmap == null) return 1.0f
        return cropRect.height() / mHeight
    }
    fun getCropRect(): RectF = cropRect
    fun getCroppedBitmap(): Bitmap? {
        val bmp = bitmap ?: return null

        var left = Math.round(cropRect.left / scale)
        var top = Math.round(cropRect.top / scale)
        if (left < 0) left = 0
        if (top < 0) top = 0

        val right = Math.min(Math.round(cropRect.right / scale), bmp.width)
        val bottom = Math.min(Math.round(cropRect.bottom / scale), bmp.height)

        return UtilsBitmap.cropToSquareWithRoundCornersPlusScale(
            bmp,
            Rect(left, top, right, bottom),
            radius,
            Common.minSquareW,
            Common.minSquareH
        )
    }

    // ──────────────────────────────────────────────
    //  Init
    // ──────────────────────────────────────────────

    init {
        this.matrix = Matrix()
        bitmapPaint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        cropPaint = Paint().apply {
            color = -15605  // 0xFFFFC313 — gold/yellow color
            style = Paint.Style.STROKE
            strokeWidth = 5.0f
            isAntiAlias = true
        }
        cropRect = RectF()
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    // ──────────────────────────────────────────────
    //  Hint Animation
    // ──────────────────────────────────────────────

    private fun startHintAnimation() {
        hintAnimator?.let { if (it.isRunning) it.cancel() }

        hintAnimationPlayed = true
        val animator = ValueAnimator.ofFloat(1.0f, 1.8f)
        hintAnimator = animator
        animator.duration = HINT_ANIMATION_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.repeatCount = HINT_ANIMATION_REPEATS
        animator.repeatMode = ValueAnimator.REVERSE

        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Float
            val newWidth = initialHintRectWidth * animatedValue
            val newHeight = initialHintRectHeight * animatedValue
            val halfWidth = newWidth / 2.0f
            val halfHeight = newHeight / 2.0f
            cropRect.set(
                initialHintRectCenterX - halfWidth,
                initialHintRectCenterY - halfHeight,
                initialHintRectCenterX + halfWidth,
                initialHintRectCenterY + halfHeight
            )
            invalidate()
        }

        animator.start()
    }

    // ──────────────────────────────────────────────
    //  Drawing
    // ──────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        canvas.save()
        canvas.translate(mDrawingX, mDrawingY)
        canvas.clipRect(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, matrix, bitmapPaint)
        canvas.drawRoundRect(cropRect, radius.toFloat(), radius.toFloat(), cropPaint)
        canvas.restore()
    }

    // ──────────────────────────────────────────────
    //  Crop Rect Movement
    // ──────────────────────────────────────────────

    private fun moveCropRect(dx: Float, dy: Float) {
        var left = cropRect.left + dx
        var top = cropRect.top + dy
        var right = cropRect.right + dx
        var bottom = cropRect.bottom + dy

        // Horizontal bounds
        if (left < 0f) {
            right = cropRect.width()
            left = 0f
        }
        if (right > mWidth) {
            left = mWidth - cropRect.width()
            right = mWidth
        }

        // Vertical bounds
        if (top < 0f) {
            bottom = cropRect.height()
            top = 0f
        }
        if (bottom > mHeight) {
            top = mHeight - cropRect.height()
            bottom = mHeight
        }

        // Enforce minimum width
        if (right - left < minW) {
            if (dx > 0f) {
                right = left + minW
            } else {
                left = right - minW
            }
        }

        // Enforce minimum height
        if (bottom - top < minH) {
            if (dy > 0f) {
                bottom = top + minH
            } else {
                top = bottom - minH
            }
        }

        cropRect.set(left, top, right, bottom)
    }

    // ──────────────────────────────────────────────
    //  Scale Listener
    // ──────────────────────────────────────────────

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            lastFocusX = detector.focusX
            lastFocusY = detector.focusY
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // no-op
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            if (scaleFactor.isNaN() || scaleFactor.isInfinite()) {
                return false
            }

            this@CropView.scaleFactor *= scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY

            var width = cropRect.width() * scaleFactor
            var height = cropRect.height() * scaleFactor

            if (width < minW) width = minW
            if (height < minH) height = minH
            if (width > mWidth) width = cropRect.width()
            if (height > mHeight) height = cropRect.height()

            val dx = focusX - lastFocusX
            val dy = focusY - lastFocusY

            val centerX = cropRect.centerX()
            val centerY = cropRect.centerY()
            val halfW = width / 2.0f
            val halfH = height / 2.0f

            cropRect.set(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)
            moveCropRect(dx, dy)

            lastFocusX = focusX
            lastFocusY = focusY
            invalidate()
            return true
        }
    }

    // ──────────────────────────────────────────────
    //  Touch Handling
    // ──────────────────────────────────────────────

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // Cancel hint animation on any touch
        hintAnimator?.let { if (it.isRunning) it.cancel() }
        hintAnimationPlayed = true

        val scaleHandled = scaleGestureDetector.onTouchEvent(motionEvent)
        val actionMasked = motionEvent.actionMasked
        val x = motionEvent.x
        val y = motionEvent.y

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!scaleGestureDetector.isInProgress) {
                    isDragging = true
                    startX = x
                    startY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                if (motionEvent.actionIndex == 0) {
                    isDragging = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleGestureDetector.isInProgress && isDragging && motionEvent.pointerCount == 1) {
                    moveCropRect(x - startX, y - startY)
                    startX = x
                    startY = y
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isDragging = false
            }
        }

        if (scaleHandled) {
            invalidate()
            return true
        }
        if (isDragging || actionMasked == MotionEvent.ACTION_DOWN) {
            invalidate()
            return true
        }
        return super.onTouchEvent(motionEvent)
    }
}
