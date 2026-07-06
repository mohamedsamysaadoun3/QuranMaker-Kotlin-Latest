package hazem.nurmontage.videoquran.multitouch

import android.content.Context
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.atan2

class ShoveGestureDetector(
    context: Context,
    private val mListener: OnShoveGestureListener
) : TwoFingerGestureDetector(context) {

    interface OnShoveGestureListener {
        fun onShove(detector: ShoveGestureDetector): Boolean
        fun onShoveBegin(detector: ShoveGestureDetector): Boolean
        fun onShoveEnd(detector: ShoveGestureDetector)
    }

    open class SimpleOnShoveGestureListener : OnShoveGestureListener {
        override fun onShove(detector: ShoveGestureDetector): Boolean = false
        override fun onShoveBegin(detector: ShoveGestureDetector): Boolean = true
        override fun onShoveEnd(detector: ShoveGestureDetector) {}
    }

    private var mSloppyGesture: Boolean = false
    private var mPrevAverageY: Float = 0f
    private var mCurrAverageY: Float = 0f

    override fun handleStartProgressEvent(action: Int, event: MotionEvent) {
        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                resetState()
                mPrevEvent = MotionEvent.obtain(event)
                mTimeDelta = 0L
                updateStateByEvent(event)
                mSloppyGesture = isSloppyGesture(event)
                if (!mSloppyGesture) {
                    mGestureInProgress = mListener.onShoveBegin(this)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mSloppyGesture) {
                    mSloppyGesture = isSloppyGesture(event)
                    if (mSloppyGesture) return
                    mGestureInProgress = mListener.onShoveBegin(this)
                }
            }
        }
    }

    override fun handleInProgressEvent(action: Int, event: MotionEvent) {
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                updateStateByEvent(event)
                if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD
                    && abs(getShovePixelsDelta()) > 0.5f
                    && mListener.onShove(this)
                ) {
                    mPrevEvent?.recycle()
                    mPrevEvent = MotionEvent.obtain(event)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!mSloppyGesture) {
                    mListener.onShoveEnd(this)
                }
                resetState()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                updateStateByEvent(event)
                if (!mSloppyGesture) {
                    mListener.onShoveEnd(this)
                }
                resetState()
            }
        }
    }

    override fun updateStateByEvent(event: MotionEvent) {
        super.updateStateByEvent(event)
        val prev = mPrevEvent!!
        mPrevAverageY = (prev.getY(0) + prev.getY(1)) / 2f
        mCurrAverageY = (event.getY(0) + event.getY(1)) / 2f
    }

    override fun isSloppyGesture(event: MotionEvent): Boolean {
        if (super.isSloppyGesture(event)) return true
        val angle = abs(atan2(mCurrFingerDiffY.toDouble(), mCurrFingerDiffX.toDouble()))
        return (angle == 0.0 || angle >= 0.3499999940395355) &&
                (angle <= 2.7899999618530273 || angle >= Math.PI)
    }

    fun getShovePixelsDelta(): Float = mCurrAverageY - mPrevAverageY

    override fun resetState() {
        super.resetState()
        mSloppyGesture = false
        mPrevAverageY = 0f
        mCurrAverageY = 0f
    }
}
