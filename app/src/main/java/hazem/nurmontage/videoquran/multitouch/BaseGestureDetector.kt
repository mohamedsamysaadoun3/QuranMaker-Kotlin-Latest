package hazem.nurmontage.videoquran.multitouch

import android.content.Context
import android.view.MotionEvent

abstract class BaseGestureDetector(protected val mContext: Context) {

    companion object {
        const val PRESSURE_THRESHOLD = 0.67f
    }

    protected var mPrevEvent: MotionEvent? = null
    protected var mCurrEvent: MotionEvent? = null
    protected var mGestureInProgress: Boolean = false
    protected var mTimeDelta: Long = 0L
    protected var mCurrPressure: Float = 0f
    protected var mPrevPressure: Float = 0f

    protected abstract fun handleStartProgressEvent(action: Int, event: MotionEvent)

    protected abstract fun handleInProgressEvent(action: Int, event: MotionEvent)

    open fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            return false
        }
        val action = event.action and MotionEvent.ACTION_MASK
        if (!mGestureInProgress) {
            handleStartProgressEvent(action, event)
        } else {
            handleInProgressEvent(action, event)
        }
        return true
    }

    protected open fun updateStateByEvent(event: MotionEvent) {
        val prev = mPrevEvent
        val curr = mCurrEvent
        if (curr != null) {
            curr.recycle()
            mCurrEvent = null
        }
        mCurrEvent = MotionEvent.obtain(event)
        mTimeDelta = event.eventTime - prev!!.eventTime
        mCurrPressure = event.getPressure(event.actionIndex)
        mPrevPressure = prev.getPressure(prev.actionIndex)
    }

    protected open fun resetState() {
        mPrevEvent?.recycle()
        mPrevEvent = null
        mCurrEvent?.recycle()
        mCurrEvent = null
        mGestureInProgress = false
    }

    fun isInProgress(): Boolean = mGestureInProgress

    fun getTimeDelta(): Long = mTimeDelta

    fun getEventTime(): Long = mCurrEvent!!.eventTime
}
