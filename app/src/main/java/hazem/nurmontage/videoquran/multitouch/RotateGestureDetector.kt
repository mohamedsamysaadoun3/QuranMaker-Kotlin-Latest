package hazem.nurmontage.videoquran.multitouch

import android.content.Context
import android.view.MotionEvent
import kotlin.math.atan2

class RotateGestureDetector(
    context: Context,
    private val mListener: OnRotateGestureListener
) : TwoFingerGestureDetector(context) {

    interface OnRotateGestureListener {
        fun onRotate(detector: RotateGestureDetector): Boolean
        fun onRotateBegin(detector: RotateGestureDetector): Boolean
        fun onRotateEnd(detector: RotateGestureDetector)
    }

    open class SimpleOnRotateGestureListener : OnRotateGestureListener {
        override fun onRotate(detector: RotateGestureDetector): Boolean = false
        override fun onRotateBegin(detector: RotateGestureDetector): Boolean = true
        override fun onRotateEnd(detector: RotateGestureDetector) {}
    }

    private var mSloppyGesture: Boolean = false

    override fun handleStartProgressEvent(action: Int, event: MotionEvent) {
        when (action) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                resetState()
                mPrevEvent = MotionEvent.obtain(event)
                mTimeDelta = 0L
                updateStateByEvent(event)
                mSloppyGesture = isSloppyGesture(event)
                if (!mSloppyGesture) {
                    mGestureInProgress = mListener.onRotateBegin(this)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (mSloppyGesture) {
                    mSloppyGesture = isSloppyGesture(event)
                    if (mSloppyGesture) return
                    mGestureInProgress = mListener.onRotateBegin(this)
                }
            }
        }
    }

    override fun handleInProgressEvent(action: Int, event: MotionEvent) {
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                updateStateByEvent(event)
                if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD && mListener.onRotate(this)) {
                    mPrevEvent?.recycle()
                    mPrevEvent = MotionEvent.obtain(event)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!mSloppyGesture) {
                    mListener.onRotateEnd(this)
                }
                resetState()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                updateStateByEvent(event)
                if (!mSloppyGesture) {
                    mListener.onRotateEnd(this)
                }
                resetState()
            }
        }
    }

    override fun resetState() {
        super.resetState()
        mSloppyGesture = false
    }

    fun getRotationDegreesDelta(): Float {
        val prevAngle = atan2(mPrevFingerDiffY, mPrevFingerDiffX)
        val currAngle = atan2(mCurrFingerDiffY, mCurrFingerDiffX)
        return (prevAngle - currAngle) * 180.0f / Math.PI.toFloat()
    }
}
