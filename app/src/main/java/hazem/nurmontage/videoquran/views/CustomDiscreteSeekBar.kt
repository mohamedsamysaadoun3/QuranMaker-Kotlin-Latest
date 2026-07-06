package hazem.nurmontage.videoquran.views

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.LocaleHelper
class CustomDiscreteSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    interface OnProgressChangeListener {
        fun onProgressChanged(seekBar: CustomDiscreteSeekBar, index: Int, label: String, fromUser: Boolean)
        fun onStartTrackingTouch(seekBar: CustomDiscreteSeekBar)
        fun onStopTrackingTouch(seekBar: CustomDiscreteSeekBar)
    }

    private var mCurrentProgressIndex: Int = 0
    private var mGradientColors: IntArray
    private var mIsDragging: Boolean = false
    private var mIsRTL: Boolean
    private var mLabelTextSize: Float
    private var mLabels: MutableList<String>
    private var mListener: OnProgressChangeListener? = null
    private var mMaxProgressIndex: Int
    private var mPaddingBottom: Float
    private val mProgressPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val mTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -3355444
        textAlign = Paint.Align.CENTER
        typeface = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
    }
    private val mThumbPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -3355444
        style = Paint.Style.FILL
    }
    private var mThumbRadius: Float
    private var mThumbX: Float = 0f
    private val mTickPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -3355444
        style = Paint.Style.FILL
    }
    private var mTickPositionsX: FloatArray
    private var mTickRadius: Float
    private var mTrackHeight: Float
    private val mTrackPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = -3355444
        style = Paint.Style.FILL
    }
    private val mTrackRect: RectF = RectF()

    init {
        val isRTL = LocaleHelper.getLanguage(context) == "ar"
        mIsRTL = isRTL
        mGradientColors = if (isRTL) {
            intArrayOf(Color.parseColor("#fae065"), Color.parseColor("#cbd653"), Color.parseColor("#a8ce46"))
        } else {
            intArrayOf(Color.parseColor("#a8ce46"), Color.parseColor("#cbd653"), Color.parseColor("#fae065"))
        }
        mTrackHeight = dpToPx(1.2f)
        mThumbRadius = dpToPx(10.0f)
        mTickRadius = dpToPx(4.0f)
        mLabelTextSize = spToPx(10.5f)
        mPaddingBottom = dpToPx(8.0f)
        mTextPaint.textSize = mLabelTextSize

        var labelsArrayResId = 0
        if (attrs != null) {
            val ta: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomDiscreteSeekBar)
            try {
                labelsArrayResId = ta.getResourceId(R.styleable.CustomDiscreteSeekBar_labelsArray, 0)
            } finally {
                ta.recycle()
            }
        }

        mLabels = if (labelsArrayResId != 0) {
            ArrayList(context.resources.getStringArray(labelsArrayResId).toList())
        } else {
            ArrayList()
        }
        mMaxProgressIndex = mLabels.size - 1
        mCurrentProgressIndex = 0
        mTickPositionsX = FloatArray(mLabels.size)
    }

    fun setOnProgressChangeListener(listener: OnProgressChangeListener?) {
        mListener = listener
    }

    fun getmLabels(): List<String> = mLabels

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }

    fun setProgress(index: Int) {
        if (index < 0 || index > mMaxProgressIndex) return
        val changed = mCurrentProgressIndex != index
        mCurrentProgressIndex = index
        calculateThumbPositionForIndex()
        invalidate()
        if (changed) {
            mListener?.onProgressChanged(this, mCurrentProgressIndex, mLabels[mCurrentProgressIndex], false)
        }
    }

    fun getProgress(): Int = mCurrentProgressIndex

    fun getCurrentLabel(): String {
        val idx = mCurrentProgressIndex
        return if (idx in mLabels.indices) mLabels[idx] else ""
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var desiredWidth = dpToPx(200.0f).toInt()
        var desiredHeight = (mThumbRadius * 2.0f + mLabelTextSize + mPaddingBottom + dpToPx(8.0f)).toInt()

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode == View.MeasureSpec.EXACTLY) {
            desiredWidth = widthSize
        } else if (widthMode == View.MeasureSpec.AT_MOST) {
            desiredWidth = Math.min(desiredWidth, widthSize)
        }

        if (heightMode == View.MeasureSpec.EXACTLY) {
            desiredHeight = heightSize
        } else if (heightMode == View.MeasureSpec.AT_MOST) {
            desiredHeight = Math.min(desiredHeight, heightSize)
        }

        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val trackStart: Float
        val trackEnd: Float
        if (mIsRTL) {
            trackStart = (width - getPaddingEnd()) - mThumbRadius
            trackEnd = getPaddingStart() + mThumbRadius
        } else {
            trackStart = getPaddingStart() + mThumbRadius
            trackEnd = (width - getPaddingEnd()) - mThumbRadius
        }

        val left = Math.min(trackStart, trackEnd)
        val top = (paddingTop + mThumbRadius) - (mTrackHeight / 2.0f)
        val right = Math.max(trackStart, trackEnd)
        val bottom = paddingTop + mThumbRadius
        mTrackRect.set(left, top, right, (bottom - (mTrackHeight / 2.0f)) + mTrackHeight)

        mProgressPaint.shader = LinearGradient(
            mTrackRect.left, mTrackRect.centerY(),
            mTrackRect.right, mTrackRect.centerY(),
            mGradientColors, null, Shader.TileMode.CLAMP
        )

        if (mLabels.size > 1) {
            val step = Math.abs(trackEnd - trackStart) / (mLabels.size - 1)
            for (i in mLabels.indices) {
                mTickPositionsX[i] = if (mIsRTL) {
                    trackStart - (i * step)
                } else {
                    (i * step) + trackStart
                }
            }
        } else if (mLabels.size == 1) {
            mTickPositionsX[0] = mTrackRect.centerX()
        }

        calculateThumbPositionForIndex()
    }

    private fun calculateThumbPositionForIndex() {
        mThumbX = if (mMaxProgressIndex >= 0) {
            mTickPositionsX[mCurrentProgressIndex]
        } else {
            if (mIsRTL) mTrackRect.right else mTrackRect.left
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRoundRect(mTrackRect, 100.0f, 100.0f, mTrackPaint)

        if (mIsRTL) {
            canvas.drawRoundRect(mThumbX, mTrackRect.top, mTrackRect.right, mTrackRect.bottom, 100.0f, 100.0f, mProgressPaint)
        } else {
            canvas.drawRoundRect(mTrackRect.left, mTrackRect.top, mThumbX, mTrackRect.bottom, 100.0f, 100.0f, mProgressPaint)
        }

        for (i in mLabels.indices) {
            var labelX = mTickPositionsX[i]
            if (i == 0) {
                labelX += if (mIsRTL) -mThumbRadius * 0.7f else mThumbRadius * 0.7f
            }
            if (i == mLabels.size - 1) {
                labelX += if (mIsRTL) mThumbRadius else -mThumbRadius
            }
            val centerY = mTrackRect.centerY()
            val label = mLabels[i]
            val textBounds = Rect()
            mTextPaint.getTextBounds(label, 0, label.length, textBounds)
            canvas.drawText(label, labelX, centerY + mThumbRadius + mPaddingBottom + textBounds.height(), mTextPaint)
        }

        canvas.drawCircle(mThumbX, mTrackRect.centerY(), mThumbRadius, mThumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled || mMaxProgressIndex < 0) return false

        val x = event.x
        val trackLeft = mTrackRect.left
        val trackRight = mTrackRect.right

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isTouchNearThumbOrTrack(x, event.y)) return super.onTouchEvent(event)
                mIsDragging = true
                mListener?.onStartTrackingTouch(this)
                mThumbX = Math.max(trackLeft, Math.min(x, trackRight))
                invalidate()
                performClick()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mIsDragging) return super.onTouchEvent(event)
                mThumbX = Math.max(trackLeft, Math.min(x, trackRight))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!mIsDragging) return super.onTouchEvent(event)
                mIsDragging = false
                val oldIndex = mCurrentProgressIndex
                snapToNearestTickAndNotify(x)
                mListener?.let { listener ->
                    if (oldIndex != mCurrentProgressIndex) {
                        listener.onProgressChanged(this, mCurrentProgressIndex, mLabels[mCurrentProgressIndex], true)
                    }
                    listener.onStopTrackingTouch(this)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isTouchNearThumbOrTrack(x: Float, y: Float): Boolean {
        val touchPadding = dpToPx(20.0f)
        return y > (mTrackRect.centerY() - mThumbRadius) - touchPadding &&
                y < ((mTrackRect.centerY() + mThumbRadius) + mLabelTextSize) + mPaddingBottom + touchPadding &&
                x > (mTrackRect.left - mThumbRadius) - touchPadding &&
                x < (mTrackRect.right + mThumbRadius) + touchPadding
    }

    private fun snapToNearestTickAndNotify(x: Float) {
        var nearestIndex = 0
        var nearestDist = Float.MAX_VALUE
        for (i in mTickPositionsX.indices) {
            val dist = Math.abs(x - mTickPositionsX[i])
            if (dist < nearestDist) {
                nearestIndex = i
                nearestDist = dist
            }
        }
        mCurrentProgressIndex = nearestIndex
        calculateThumbPositionForIndex()
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
