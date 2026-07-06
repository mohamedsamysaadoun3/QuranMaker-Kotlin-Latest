package hazem.nurmontage.videoquran.multitouch

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.sqrt

abstract class TwoFingerGestureDetector(context: Context) : BaseGestureDetector(context) {

    protected var mPrevFingerDiffX: Float = 0f
    protected var mPrevFingerDiffY: Float = 0f
    protected var mCurrFingerDiffX: Float = 0f
    protected var mCurrFingerDiffY: Float = 0f
    private var mPrevLen: Float = -1f
    private var mCurrLen: Float = -1f
    private val mEdgeSlop: Float = ViewConfiguration.get(context).scaledEdgeSlop.toFloat()
    private var mRightSlopEdge: Float = 0f
    private var mBottomSlopEdge: Float = 0f

    override fun updateStateByEvent(event: MotionEvent) {
        super.updateStateByEvent(event)
        val prev = mPrevEvent!!
        mCurrLen = -1f
        mPrevLen = -1f
        val prevX0 = prev.getX(0)
        val prevY0 = prev.getY(0)
        val prevX1 = prev.getX(1)
        mPrevFingerDiffX = prevX1 - prevX0
        mPrevFingerDiffY = prev.getY(1) - prevY0
        val currX0 = event.getX(0)
        val currY0 = event.getY(0)
        val currX1 = event.getX(1)
        mCurrFingerDiffX = currX1 - currX0
        mCurrFingerDiffY = event.getY(1) - currY0
    }

    fun getCurrentSpan(): Float {
        if (mCurrLen == -1f) {
            val dx = mCurrFingerDiffX
            val dy = mCurrFingerDiffY
            mCurrLen = sqrt(dx * dx + dy * dy)
        }
        return mCurrLen
    }

    fun getPreviousSpan(): Float {
        if (mPrevLen == -1f) {
            val dx = mPrevFingerDiffX
            val dy = mPrevFingerDiffY
            mPrevLen = sqrt(dx * dx + dy * dy)
        }
        return mPrevLen
    }

    protected open fun isSloppyGesture(event: MotionEvent): Boolean {
        val displayMetrics = mContext.resources.displayMetrics
        mRightSlopEdge = displayMetrics.widthPixels - mEdgeSlop
        mBottomSlopEdge = displayMetrics.heightPixels - mEdgeSlop
        val edgeSlop = mEdgeSlop
        val rightEdge = mRightSlopEdge
        val bottomEdge = mBottomSlopEdge
        val rawX0 = event.rawX
        val rawY0 = event.rawY
        val rawX1 = getRawX(event, 1)
        val rawY1 = getRawY(event, 1)
        val pointer0Sloppy = rawX0 < edgeSlop || rawY0 < edgeSlop || rawX0 > rightEdge || rawY0 > bottomEdge
        val pointer1Sloppy = rawX1 < edgeSlop || rawY1 < edgeSlop || rawX1 > rightEdge || rawY1 > bottomEdge
        return (pointer0Sloppy && pointer1Sloppy) || pointer0Sloppy || pointer1Sloppy
    }

    companion object {
        @JvmStatic
        protected fun getRawX(event: MotionEvent, pointerIndex: Int): Float {
            val offset = event.x - event.rawX
            return if (pointerIndex < event.pointerCount) {
                event.getX(pointerIndex) + offset
            } else {
                0f
            }
        }

        @JvmStatic
        protected fun getRawY(event: MotionEvent, pointerIndex: Int): Float {
            val offset = event.y - event.rawY
            return if (pointerIndex < event.pointerCount) {
                event.getY(pointerIndex) + offset
            } else {
                0f
            }
        }
    }
}
