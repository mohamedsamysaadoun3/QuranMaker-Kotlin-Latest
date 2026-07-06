package hazem.nurmontage.videoquran.multitouch

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent

class MoveGestureDetector(
    context: Context,
    private val mListener: OnMoveGestureListener
) : BaseGestureDetector(context) {

    interface OnMoveGestureListener {
        fun onMove(detector: MoveGestureDetector): Boolean
        fun onMoveBegin(detector: MoveGestureDetector): Boolean
        fun onMoveEnd(detector: MoveGestureDetector)
    }

    open class SimpleOnMoveGestureListener : OnMoveGestureListener {
        override fun onMove(detector: MoveGestureDetector): Boolean = false
        override fun onMoveBegin(detector: MoveGestureDetector): Boolean = true
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    private val mFocusExternal = PointF()
    private var mFocusDeltaExternal: PointF = FOCUS_DELTA_ZERO
    private var mCurrFocusInternal: PointF? = null
    private var mPrevFocusInternal: PointF? = null

    override fun handleStartProgressEvent(action: Int, event: MotionEvent) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                resetState()
                mPrevEvent = MotionEvent.obtain(event)
                mTimeDelta = 0L
                updateStateByEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                mGestureInProgress = mListener.onMoveBegin(this)
            }
        }
    }

    override fun handleInProgressEvent(action: Int, event: MotionEvent) {
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mListener.onMoveEnd(this)
                resetState()
            }
            MotionEvent.ACTION_MOVE -> {
                updateStateByEvent(event)
                if (mCurrPressure / mPrevPressure > PRESSURE_THRESHOLD && mListener.onMove(this)) {
                    mPrevEvent?.recycle()
                    mPrevEvent = MotionEvent.obtain(event)
                }
            }
        }
    }

    override fun updateStateByEvent(event: MotionEvent) {
        super.updateStateByEvent(event)
        val prev = mPrevEvent!!
        mCurrFocusInternal = determineFocalPoint(event)
        mPrevFocusInternal = determineFocalPoint(prev)
        mFocusDeltaExternal = if (prev.pointerCount != event.pointerCount) {
            FOCUS_DELTA_ZERO
        } else {
            PointF(
                mCurrFocusInternal!!.x - mPrevFocusInternal!!.x,
                mCurrFocusInternal!!.y - mPrevFocusInternal!!.y
            )
        }
        mFocusExternal.x += mFocusDeltaExternal.x
        mFocusExternal.y += mFocusDeltaExternal.y
    }

    private fun determineFocalPoint(event: MotionEvent): PointF {
        val count = event.pointerCount
        var sumX = 0f
        var sumY = 0f
        for (i in 0 until count) {
            sumX += event.getX(i)
            sumY += event.getY(i)
        }
        val n = count.toFloat()
        return PointF(sumX / n, sumY / n)
    }

    fun getFocusX(): Float = mFocusExternal.x
    fun getFocusY(): Float = mFocusExternal.y
    fun getFocusDelta(): PointF = mFocusDeltaExternal

    companion object {
        private val FOCUS_DELTA_ZERO = PointF()
    }
}
