package hazem.nurmontage.videoquran.views

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Insets
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Scroller
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.CanvasUtils
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.constant.EntityAction
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.EntityView
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.Transition
import java.util.Locale
import java.util.Stack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

// Extension function imports for split modules
import hazem.nurmontage.videoquran.views.track.onDrawExt
import hazem.nurmontage.videoquran.views.track.mDrawExt
import hazem.nurmontage.videoquran.views.track.drawItemBtnExt
import hazem.nurmontage.videoquran.views.track.drawIconDrawableExt
import hazem.nurmontage.videoquran.views.track.drawBasmalaExt
import hazem.nurmontage.videoquran.views.track.drawAllEntitiesExt
import hazem.nurmontage.videoquran.views.track.drawMarkerExt
import hazem.nurmontage.videoquran.views.track.drawTimeBarExt
import hazem.nurmontage.videoquran.views.track.formatTimeLabelExt
import hazem.nurmontage.videoquran.views.track.formatTimeLabelArabicExt
import hazem.nurmontage.videoquran.views.track.onTouchExt
import hazem.nurmontage.videoquran.views.track.isPassExt
import hazem.nurmontage.videoquran.views.track.updateSelectionOnTapExt
import hazem.nurmontage.videoquran.views.track.handleItemInteractionExt
import hazem.nurmontage.videoquran.views.track.processQuranItemsSelectionExt
import hazem.nurmontage.videoquran.views.track.processTrslQuranItemsSelectionExt
import hazem.nurmontage.videoquran.views.track.processAudioItemsSelectionExt
import hazem.nurmontage.videoquran.views.track.deselectAllQuranItemsExt
import hazem.nurmontage.videoquran.views.track.deselectAllTrslQuranItemsExt
import hazem.nurmontage.videoquran.views.track.deselectAllAudioItemsExt
import hazem.nurmontage.videoquran.views.track.setupAnimationQuran
import hazem.nurmontage.videoquran.views.track.setupAnimationBismilah
import hazem.nurmontage.videoquran.views.track.translateFromNowExt
import hazem.nurmontage.videoquran.views.track.translateToRightExt
import hazem.nurmontage.videoquran.views.track.translateToRightNoParamExt
import hazem.nurmontage.videoquran.views.track.translateFromStartExt
import hazem.nurmontage.videoquran.views.track.translateUntilNowExt
import hazem.nurmontage.videoquran.views.track.translateToRightBismilahExt
import hazem.nurmontage.videoquran.views.track.translateEndNowExt
import hazem.nurmontage.videoquran.views.track.translateToStartExt
import hazem.nurmontage.videoquran.views.track.translateToEndExt
import hazem.nurmontage.videoquran.views.track.translateToStartEntityExt
import hazem.nurmontage.videoquran.views.track.translateToEndEntityExt
import hazem.nurmontage.videoquran.views.track.previewEntityExt
import hazem.nurmontage.videoquran.views.track.updateCursurToSelectEntityExt
import hazem.nurmontage.videoquran.views.track.addStackExt
import hazem.nurmontage.videoquran.views.track.selectEntityExt
import hazem.nurmontage.videoquran.views.track.stackSplitExt
import hazem.nurmontage.videoquran.views.track.splitAudioExt
import hazem.nurmontage.videoquran.views.track.deleteEntityExt
import hazem.nurmontage.videoquran.views.track.deleteEntityAllSelectExt
import hazem.nurmontage.videoquran.views.track.deleteMediaEntityExt
import hazem.nurmontage.videoquran.views.track.addAudioExt
import hazem.nurmontage.videoquran.views.track.addAudioSimpleExt
import hazem.nurmontage.videoquran.views.track.addQuranExt
import hazem.nurmontage.videoquran.views.track.addTrslQuranExt
import hazem.nurmontage.videoquran.views.track.addTrslQuranAtIndexExt
import hazem.nurmontage.videoquran.views.track.addQuranAtIndexExt
import hazem.nurmontage.videoquran.views.track.addQuran_splitExt
import hazem.nurmontage.videoquran.views.track.addQuran_splitTrslExt
import hazem.nurmontage.videoquran.views.track.calculMaxTimeExt
import hazem.nurmontage.videoquran.views.track.undoExt
import hazem.nurmontage.videoquran.views.track.redoExt

/**
 * Custom [FrameLayout] that serves as the main timeline track editor for the
 * Quran video maker. It displays audio, Quran verse, translation, and
 * Bismilah entities on a horizontal timeline with pinch-to-zoom, scroll/fling
 * gestures, entity selection, drag-to-move, and trim operations.
 *
 * Features:
 * - Horizontal timeline with time markers and playhead cursor
 * - Pinch-to-zoom scaling (0.09x – 8x)
 * - Scroll and fling gestures for timeline navigation
 * - Entity selection, multi-select, drag, and trim
 * - Undo/redo stacks for entity actions
 * - Auto-scroll during playback and trim operations
 * - Snap-to-edge and snap-to-entity alignment guides
 *
 * Converted from TrackEntityView.java (4,582 lines).
 */
@Suppress("DEPRECATION")
class TrackEntityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), View.OnTouchListener {

    companion object {
        private const val DEFAULT_SCALE = 0.5f
        private const val MAX_SCALE = 8.0f
        private const val MIN_SCALE = 0.09f
        private const val FACTOR_VITESSE = 180.0f
        internal const val CLR_DEFAULT_BG = -13421771   // 0x333333
        internal const val CLR_SELECT = -794718          // 0x3D3D32
        internal const val CLR_BTN_DEFAULT = -13421771
    }

    // ── Callback interface ──────────────────────────────────────────────

    interface ITrimLineCallback {
        fun enableRedo(enabled: Boolean)
        fun enableUndo(enabled: Boolean)
        fun fadeInAudio(delta: Float)
        fun fadeOutAudio(delta: Float)
        fun onAddStack(entityAction: EntityAction)
        fun onDelete(entityView: EntityView)
        fun onEmptySelect()
        fun onMove()
        fun onPlayVibration()
        fun onSeekPlayer(position: Float)
        fun onSelectEntity(entity: Entity, delta: Float)
        fun onSelectMultiple(count: Int)
        fun onUp()
        fun onUpdate()
        fun onUpdatePlayerAudio(entityAudio: EntityAudio)
        fun onUpdateTime()
        fun pause()
        fun progress(show: Boolean)
    }

    // ── Fields ────────────────────────────────────────────────────────

    internal var DETECT_LEFT_MOVE: Float = 0f
    internal var DETECT_RIGHT_MOVE: Float = 0f
    internal var SPEED: Float = 0f
    internal var TOLERANCE_X: Float = 0.95f

    internal var autoMoveRunnable: Runnable? = null
    internal var autoScrollHandler: Handler = Handler()
    internal var autoScrollRunnable: Runnable? = null

    var bismilahTimeline: EntityBismilahTimeline? = null
    internal var btn_redo: ImageButton? = null
    internal var btn_undo: ImageButton? = null
    internal var canvas_top_Y: Float = 0f
    internal var centerX: Float = 0f
    internal var clr_btn_audio: Int = CLR_BTN_DEFAULT
    internal var clr_btn_quran: Int = CLR_BTN_DEFAULT
    internal var clr_btn_trsl: Int = CLR_BTN_DEFAULT
    internal var countMove: Int = 0
    internal var currentEventX: Float = 0f
    @JvmField
    internal var currentPosition: Float = 0f
    internal var current_cursur_position: Int = 0
    internal var duration: Int = 0
    internal var dx: Float = 0f

    internal var entityList: Stack<Pair<Entity, EntityAction>> = Stack()
    var entityListAudio: MutableList<EntityAudio> = ArrayList()
    val entityListQuran: MutableList<EntityQuranTimeline> = ArrayList()
    val entityListTrslQuran: MutableList<EntityTrslTimeline> = ArrayList()

    internal var eventX: Float = 0f
    internal var eventY: Float = 0f
    var exclusionRects: MutableList<Rect> = ArrayList()

    internal var gestureDetector: GestureDetectorCompat? = null

    internal var iTrimLineCallback: ITrimLineCallback? = null
    var isArabic_lang: Boolean = false
    internal var isAutoMove: Boolean = false
    internal var isAutoScroll: Boolean = false
    internal var isCheckLine: Boolean = false
    internal var isCheckLineCursur: Boolean = false
    internal var isDetectChange: Boolean = false
    internal var isFling: Boolean = false
    internal var isMove: Boolean = false
    internal var isOnUp: Boolean = false
    internal var isPassScroll: Boolean = true
    var isPlaying: Boolean = false
    internal var isProgress: Boolean = false
    internal var isScaleListener: Boolean = false

    internal var lasX: Float = 0f
    internal var lastDifference: Long = 0L
    internal var lastTime: Long = 0L
    var mIsi3adaTimeline: EntityBismilahTimeline? = null
    internal var mScrollY: Float = 0f
    internal var m_pos_y_marker: Float = 0f
    internal var markerHeight: Float = 0f
    internal var maxBottom: Float = 0f
    var maxTime: Int = -1
        set(value) {
            field = value
            timeLineW = (value * getSecond_in_screen()) / 1000.0f
        }
    internal var max_trim: Float = 0f
    internal var objectAnimator: ObjectAnimator? = null
    internal var onThink: Boolean = true
    internal var p: Float = 0f
    internal var paddingCursur: Float = 0f

    internal var paintCursur: Paint? = null
    internal val paintItem: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal var paintLineCheck: Paint? = null
    internal var paintMaker: Paint? = null
    internal var paint_time: Paint? = null
    internal var pass: Boolean = false

    internal var pathItemAudio: Path? = null
    internal var pathItemQuran: Path? = null
    internal var pathItemTrslQuran: Path? = null
    internal var posY: Float = 0f
    internal var radius: Float = 0f

    internal var rectFItemQuran: RectF? = null
    internal var rectFItemTrslQuran: RectF? = null
    internal var rectItemAudio: RectF? = null
    internal var rectSquareAudio: RectF? = null
    internal var rectSquareQuran: RectF? = null
    internal var rectSquareTrslQuran: RectF? = null

    var scaleFactor: Float = DEFAULT_SCALE
        set(value) {
            field = value
            scrolled_with_zoom = value * currentPosition
        }

    internal var scaleGestureDetector: ScaleGestureDetector? = null
    internal var scrolled_with_zoom: Float = 0f
    internal var scroller: Scroller? = null
    @JvmField
    var second_in_screen: Float = 0f
    var selectedEntity: Entity? = null
    internal var signeX: Float = -1f
    internal var signeY: Float = -1f
    internal var startXLine: Float = 0f
    internal var start_y_draw: Float = 0f
    internal var target: Float = 0f
    internal var timeLineW: Float = 0f
    internal var time_start: Long = 0L
    internal var undoEntityList: Stack<Pair<Entity, EntityAction>> = Stack()
    internal var w_time_item: Float = 0f
    internal var width_screen: Int = 0
    var entityY: Float = 0f

    // ── Gesture listener (extracted from duplicated constructors) ─────

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(motionEvent: MotionEvent): Boolean {
            pauseScroll()
            val pointF = PointF(motionEvent.x, motionEvent.y)
            isPassScroll = true
            if (selectedEntity != null) {
                val contains = selectedEntity!!.contains(pointF)
                isPassScroll = !contains && selectedEntity!!.trimType == -1
                selectedEntity!!.isSelect = true
                if (!isPassScroll && iTrimLineCallback != null) {
                    if (selectedEntity!!.trimType == 0) {
                        selectedEntity!!.setCurrentRect()
                        selectedEntity!!.setOnTapTime(
                            round(selectedEntity!!.rect.left / getSecond_in_screen()) * 1000,
                            selectedEntity!!.rect.left
                        )
                        iTrimLineCallback!!.onPlayVibration()
                    } else if (selectedEntity!!.trimType == 1) {
                        selectedEntity!!.setCurrentRect()
                        selectedEntity!!.setOnTapTime(
                            round(selectedEntity!!.rect.right / getSecond_in_screen()) * 1000,
                            selectedEntity!!.rect.right
                        )
                        iTrimLineCallback!!.onPlayVibration()
                    } else if (contains) {
                        selectedEntity!!.setCurrentRect()
                        iTrimLineCallback!!.onSelectEntity(selectedEntity!!, 0.0f)
                    }
                }
            }
            return true
        }

        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            if (!isPlaying) {
                if (handleItemInteraction(
                        motionEvent.x + paddingLeft + (centerX - radius * 0.5f) + scrolled_with_zoom,
                        motionEvent.y
                    )) {
                    return true
                }
            } else if (clr_btn_quran != CLR_BTN_DEFAULT || clr_btn_audio != CLR_BTN_DEFAULT || clr_btn_trsl != CLR_BTN_DEFAULT) {
                clr_btn_trsl = CLR_BTN_DEFAULT
                clr_btn_quran = CLR_BTN_DEFAULT
                clr_btn_audio = CLR_BTN_DEFAULT
            }
            if (isPassScroll) {
                updateSelectionOnTap(motionEvent)
            }
            return true
        }

        override fun onScroll(motionEvent: MotionEvent?, motionEvent2: MotionEvent, f: Float, f2: Float): Boolean {
            if (isProgress || !isPassScroll || (selectedEntity != null && selectedEntity!!.trimType != -1)) {
                return super.onScroll(motionEvent, motionEvent2, f, f2)
            }
            if (!isScaleListener && motionEvent2.eventTime - motionEvent!!.eventTime >= 107 && isPass(motionEvent2)) {
                if (isPlaying) {
                    isPlaying = false
                }
                if (eventX == 0.0f) {
                    eventX = motionEvent2.rawX
                    eventY = motionEvent2.rawY
                    return true
                }
                val rawX = motionEvent2.rawX - eventX
                currentPosition += rawX / scaleFactor
                if (currentPosition > 0.0f) {
                    currentPosition = 0.0f
                }
                scrolled_with_zoom = currentPosition * scaleFactor
                if (iTrimLineCallback != null) {
                    iTrimLineCallback!!.onSeekPlayer(scrolled_with_zoom)
                }
                eventX = motionEvent2.rawX
                eventY = motionEvent2.rawY
                invalidate()
            }
            return true
        }

        override fun onFling(motionEvent: MotionEvent?, motionEvent2: MotionEvent, f: Float, f2: Float): Boolean {
            if (isProgress) return true
            if (isPlaying) isPlaying = false
            if (motionEvent2.eventTime - motionEvent!!.eventTime > 107) return true
            if (eventX == 0.0f) {
                eventX = motionEvent!!.rawX
                eventY = motionEvent!!.rawY
            }
            val abs = abs(motionEvent2.rawX - eventX)
            val abs2 = abs(motionEvent2.rawY - eventY)
            eventX = motionEvent2.rawX
            eventY = motionEvent2.rawY
            var velocityX = f
            if (if (motionEvent2.rawX > motionEvent!!.rawX) velocityX < 0.0f else velocityX > 0.0f) {
                velocityX *= -1.0f
            }
            if (abs2 > abs * 1.2f) {
                target = f2
                flingY()
            } else {
                scroller?.fling(
                    currentPosition.toInt(), 0, velocityX.toInt(), 0,
                    (-timeLineW).toInt(), 0, 0, 0
                )
                invalidate()
            }
            return true
        }
    }

    // ── Init ────────────────────────────────────────────────────────

    init {
        maxTime = -1
        TOLERANCE_X = 0.95f
        entityListAudio = ArrayList()
        lastTime = 0L
        lastDifference = 0L
        setWillNotDraw(false)
        initAutoScroll()
        setOnTouchListener(this)
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetectorCompat(context, gestureListener)
        scroller = Scroller(context)
    }

    // ── ScaleListener inner class ──────────────────────────────────────

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = max(MIN_SCALE, min(scaleFactor * detector.scaleFactor, MAX_SCALE))
            scrolled_with_zoom = scaleFactor * currentPosition
            invalidate()
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaleListener = true
            if (iTrimLineCallback != null) {
                iTrimLineCallback!!.pause()
            }
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
        }
    }

    // ── Auto-scroll initialization ──────────────────────────────────

    private fun initAutoScroll() {
        autoScrollHandler = Handler()
        lateinit var scrollRunnable: Runnable
        scrollRunnable = Runnable {
            if (isAutoScroll) {
                var currentTimeMillis = (System.currentTimeMillis() - time_start).toFloat() / FACTOR_VITESSE
                if (SPEED < 0.0f) currentTimeMillis *= -1.0f
                val f = currentTimeMillis + SPEED
                if (selectedEntity == null) return@Runnable
                if (selectedEntity!!.trimType == 1) {
                    val rect = selectedEntity!!.rect
                    val f2 = rect.right + f
                    rect.right = f2
                    if (rect.right - selectedEntity!!.rect.left <= max_trim) {
                        selectedEntity!!.rect.right = selectedEntity!!.rect.left + max_trim
                        selectedEntity!!.setLastRight(selectedEntity!!.rect.right)
                        invalidate()
                        autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        return@Runnable
                    }
                    if (selectedEntity is EntityQuranTimeline) {
                        val eqt = selectedEntity as EntityQuranTimeline
                        if (eqt.index + 1 < entityListQuran.size) {
                            val next = getPreviewOrNextEntityQuran(entityListQuran, eqt.index + 1, true)
                            if (next != null && f2 > next.rect.left) {
                                selectedEntity!!.rect.right = next.rect.left
                                selectedEntity!!.setLastRight(selectedEntity!!.rect.right)
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                isAutoScroll = false
                                invalidate()
                                return@Runnable
                            }
                        }
                    }
                    if (selectedEntity is EntityTrslTimeline) {
                        val etl = selectedEntity as EntityTrslTimeline
                        if (etl.index + 1 < entityListTrslQuran.size) {
                            val next = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index + 1, true)
                            if (next != null && f2 > next.rect.left) {
                                selectedEntity!!.rect.right = next.rect.left
                                selectedEntity!!.setLastRight(selectedEntity!!.rect.right)
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                isAutoScroll = false
                                invalidate()
                                return@Runnable
                            }
                        }
                    }
                    selectedEntity!!.rect.right = f2
                    selectedEntity!!.setLastRight(selectedEntity!!.rect.right)
                } else if (selectedEntity!!.trimType == 0) {
                    val rect2 = selectedEntity!!.rect
                    val f3 = rect2.left + f
                    rect2.left = f3
                    if (f3 < 0.0f) {
                        selectedEntity!!.rect.left = 0.0f
                        selectedEntity!!.setLastLeft(selectedEntity!!.rect.left)
                        selectedEntity!!.updateStartTrim()
                        isAutoScroll = false
                        autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        invalidate()
                        return@Runnable
                    }
                    if (selectedEntity!!.rect.right - f3 <= max_trim) {
                        val f4 = selectedEntity!!.rect.right - max_trim
                        isAutoScroll = false
                        selectedEntity!!.rect.left = f4
                        selectedEntity!!.setLastLeft(selectedEntity!!.rect.left)
                        selectedEntity!!.updateStartTrim()
                        autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                        invalidate()
                        return@Runnable
                    }
                    if (selectedEntity is EntityQuranTimeline) {
                        val eqt2 = selectedEntity as EntityQuranTimeline
                        if (eqt2.index > 0) {
                            val prev = getPreviewOrNextEntityQuran(entityListQuran, eqt2.index - 1, false)
                            if (prev != null && f3 <= prev.rect.right) {
                                selectedEntity!!.rect.left = prev.rect.right
                                selectedEntity!!.setLastLeft(selectedEntity!!.rect.left)
                                selectedEntity!!.updateStartTrim()
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                isAutoScroll = false
                                invalidate()
                                return@Runnable
                            }
                        }
                    }
                    if (selectedEntity is EntityTrslTimeline) {
                        val etl2 = selectedEntity as EntityTrslTimeline
                        if (etl2.index > 0) {
                            val prev = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl2.index - 1, false)
                            if (prev != null && f3 <= prev.rect.right) {
                                selectedEntity!!.rect.left = prev.rect.right
                                selectedEntity!!.setLastLeft(selectedEntity!!.rect.left)
                                selectedEntity!!.updateStartTrim()
                                autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                                isAutoScroll = false
                                invalidate()
                                return@Runnable
                            }
                        }
                    }
                    selectedEntity!!.rect.left = f3
                    selectedEntity!!.setLastLeft(selectedEntity!!.rect.left)
                    selectedEntity!!.updateStartTrim()
                }
                currentPosition -= f / scaleFactor
                if (currentPosition > 0.0f) {
                    currentPosition = 0.0f
                    scrolled_with_zoom = currentPosition * scaleFactor
                    isAutoScroll = false
                    autoScrollHandler.removeCallbacks(autoScrollRunnable!!)
                    invalidate()
                    return@Runnable
                }
                scrolled_with_zoom = currentPosition * scaleFactor
                invalidate()
                autoScrollHandler.postDelayed(scrollRunnable, 100L)
            }
        }
        autoScrollRunnable = scrollRunnable

        lateinit var moveRunnable: Runnable
        moveRunnable = Runnable {
            if (isAutoMove) {
                var currentTimeMillis = (System.currentTimeMillis() - time_start).toFloat() / FACTOR_VITESSE
                if (SPEED < 0.0f) currentTimeMillis *= -1.0f
                val f = currentTimeMillis + SPEED
                val width = selectedEntity!!.rect.width()
                var f2 = selectedEntity!!.rect.left + f
                if (f2 < 0.0f) f2 = 0.0f
                val f3 = f2 + width
                if (selectedEntity is EntityQuranTimeline) {
                    val eqt = selectedEntity as EntityQuranTimeline
                    if (eqt.index > 0) {
                        val prev = getPreviewOrNextEntityQuran(entityListQuran, eqt.index - 1, false)
                        if (prev != null && f2 <= prev.rect.right) {
                            selectedEntity!!.setX(prev.rect.right)
                            selectedEntity!!.right = prev.rect.right + width
                            pass = false
                            invalidate()
                            isAutoMove = false
                            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                            return@Runnable
                        }
                    }
                    if (eqt.index + 1 < entityListQuran.size) {
                        val next = getPreviewOrNextEntityQuran(entityListQuran, eqt.index + 1, true)
                        if (next != null && f3 >= next.rect.left) {
                            selectedEntity!!.setX(next.rect.left - width)
                            selectedEntity!!.right = next.rect.left
                            pass = false
                            invalidate()
                            isAutoMove = false
                            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                            return@Runnable
                        }
                    }
                }
                if (selectedEntity is EntityTrslTimeline) {
                    val etl = selectedEntity as EntityTrslTimeline
                    if (etl.index > 0) {
                        val prev = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index - 1, false)
                        if (prev != null && f2 <= prev.rect.right) {
                            selectedEntity!!.setX(prev.rect.right)
                            selectedEntity!!.right = prev.rect.right + width
                            pass = false
                            invalidate()
                            isAutoMove = false
                            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                            return@Runnable
                        }
                    }
                    if (etl.index + 1 < entityListTrslQuran.size) {
                        val next = getPreviewOrNextEntityTrslQuran(entityListTrslQuran, etl.index + 1, true)
                        if (next != null && f3 >= next.rect.left) {
                            selectedEntity!!.setX(next.rect.left - width)
                            selectedEntity!!.right = next.rect.left
                            pass = false
                            invalidate()
                            isAutoMove = false
                            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                            return@Runnable
                        }
                    }
                }
                if (selectedEntity is EntityAudio) {
                    val ea = selectedEntity as EntityAudio
                    if (ea.index > 0) {
                        val prev = getPreviewOrNextEntityAudio(entityListAudio, ea.index - 1, false)
                        if (prev != null && f2 <= prev.rect.right) {
                            selectedEntity!!.setX(prev.rect.right)
                            selectedEntity!!.right = prev.rect.right + width
                            pass = false
                            invalidate()
                            isAutoMove = false
                            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                            return@Runnable
                        }
                    }
                    if (ea.index + 1 < entityListAudio.size) {
                        val next = getPreviewOrNextEntityAudio(entityListAudio, ea.index + 1, true)
                        if (next != null && f3 >= next.rect.left) {
                            selectedEntity!!.setX(next.rect.left - width)
                            selectedEntity!!.right = next.rect.left
                            pass = false
                            invalidate()
                            isAutoMove = false
                            autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                            return@Runnable
                        }
                    }
                }
                currentPosition -= f / scaleFactor
                if (currentPosition > 0.0f) {
                    currentPosition = 0.0f
                    scrolled_with_zoom = currentPosition * scaleFactor
                    isAutoMove = false
                    autoScrollHandler.removeCallbacks(autoMoveRunnable!!)
                    invalidate()
                    return@Runnable
                }
                scrolled_with_zoom = currentPosition * scaleFactor
                selectedEntity!!.rect.left = f2
                selectedEntity!!.rect.right = f3
                isMove = true
                invalidate()
                autoScrollHandler.postDelayed(moveRunnable, 100L)
            }
        }
        autoMoveRunnable = moveRunnable
    }

    // ── Public API ──────────────────────────────────────────────────

    internal fun setupFade(entityAudio: EntityAudio) {
        // Empty — fade is handled by the audio entity
    }

    fun getDefaultScale(): Float = DEFAULT_SCALE

    fun setmIsi3adaTimeline(entityBismilahTimeline: EntityBismilahTimeline?) {
        mIsi3adaTimeline = entityBismilahTimeline
    }

    fun getmIsi3adaTimeline(): EntityBismilahTimeline? = mIsi3adaTimeline

    fun getEntityAudioNotDeleted(i: Int): Pair<Int, EntityAudio>? {
        var idx = i
        while (idx < entityListAudio.size) {
            val entityAudio = entityListAudio[idx]
            if (entityAudio.visible()) {
                return Pair(idx, entityAudio)
            }
            idx++
        }
        return null
    }

    fun clearAudio() {
        if (entityListAudio.isEmpty()) return
        entityListAudio.clear()
        val stack = Stack<Pair<Entity, EntityAction>>()
        val it = entityList.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (next.first !is EntityAudio) {
                stack.push(next)
            }
        }
        entityList.clear()
        entityList = stack
    }

    fun init(i: Int, i2: Int) {
        if (i <= 0 || i2 <= 0) return
        val f = i.toFloat()
        SPEED = 0.04f * f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint_time = paint
        paint.color = -8355712
        paint_time!!.typeface = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
        radius = 0.006f * f
        paint_time!!.textSize = f * 0.023f
        val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
        paintMaker = paint2
        paint2.color = -1
        paintMaker!!.strokeWidth = radius * 0.5f
        markerHeight = radius * 3.0f
        m_pos_y_marker = paintMaker!!.strokeWidth * 4.0f
        paddingCursur = 4.0f * radius
        centerX = width_screen * 0.5f - radius * 0.5f
        DETECT_RIGHT_MOVE = 0.4f * centerX
        DETECT_LEFT_MOVE = centerX * 0.45f
        val paint3 = Paint(Paint.ANTI_ALIAS_FLAG)
        paintCursur = paint3
        paint3.strokeWidth = radius
        val strokeWidth = paintCursur!!.strokeWidth * 2.8f
        val paint4 = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLineCheck = paint4
        paint4.color = -16121
        paintLineCheck!!.strokeWidth = paintCursur!!.strokeWidth
        paintLineCheck!!.pathEffect = DashPathEffect(floatArrayOf(strokeWidth, strokeWidth), 0.0f)
        w_time_item = paint_time!!.measureText("999") * 0.5f
    }

    private fun drawIconDrawable(canvas: Canvas) {
        drawIconDrawableExt(canvas)
    }



    fun setSecond_in_screen(f: Float, i: Int, i2: Int) {
        second_in_screen = f
        duration = i
        width_screen = i2
        val f2 = 0.03f * f
        dx = f2
        TOLERANCE_X = f2
        max_trim = f * 0.2f
    }

    fun getTextSize(): Float {
        val paint = paint_time ?: return 1.0f
        return paint.textSize * 1.42f
    }

    override fun onLayout(z: Boolean, i: Int, i2: Int, i3: Int, i4: Int) {
        if (z) updateGestureExclusion()
    }

    private fun updateGestureExclusion() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                exclusionRects.clear()
                val systemGestureInsets = rootWindowInsets?.systemGestureInsets ?: return
                val rect = Rect(0, 0, systemGestureInsets.left, height)
                val rect2 = Rect(right - systemGestureInsets.right, 0, right, height)
                exclusionRects.add(rect)
                exclusionRects.add(rect2)
                setSystemGestureExclusionRects(exclusionRects)
            }
        } catch (_: Exception) {
        }
    }

    override fun onWindowSystemUiVisibilityChanged(i: Int) {
        super.onWindowSystemUiVisibilityChanged(i)
        updateGestureExclusion()
    }

    override fun onSizeChanged(i: Int, i2: Int, i3: Int, i4: Int) {
        super.onSizeChanged(i, i2, i3, i4)
        if (i2 < 1 || i < 1) return
        val f = i2.toFloat()
        maxBottom = 0.78f * f
        start_y_draw = 0.18f * f
        canvas_top_Y = 0.1f * f
        posY = 0.05f * f
        p = f * 0.026f
    }

    private fun drawItemBtn(canvas: Canvas) {
        drawItemBtnExt(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        onDrawExt(canvas)
        super.onDraw(canvas)
    }

    val second_in_screenNoScale: Float get() = second_in_screen

    fun getSecond_in_screen(): Float = second_in_screen * scaleFactor

    fun setSecond_in_screen(f: Float) {
        second_in_screen = f
        dx = 0.03f * f
        max_trim = f * 0.2f
    }

    private fun mDraw(canvas: Canvas) {
        mDrawExt(canvas)
    }

    private fun drawMarker(canvas: Canvas, f: Float, f2: Float) {
        drawMarkerExt(canvas, f, f2)
    }

    fun setiTrimLineCallback(callback: ITrimLineCallback?) {
        iTrimLineCallback = callback
    }

    private fun drawTimeBar(canvas: Canvas, start: Int, end: Int, secondInScreen: Float) {
        drawTimeBarExt(canvas, start, end, secondInScreen)
    }

    private fun formatTimeLabel(f: Float): String {
        return formatTimeLabelExt(f)
    }

    private fun formatTimeLabelArabic(f: Float): String {
        return formatTimeLabelArabicExt(f)
    }

    fun getCurrentPosition(): Float = scrolled_with_zoom



    fun isExist(entityBismilahTimeline: EntityBismilahTimeline?): Boolean {
        return entityBismilahTimeline != null && entityBismilahTimeline.visible()
    }

    private fun drawBasmala(canvas: Canvas, rectF: RectF): Float {
        return drawBasmalaExt(canvas, rectF)
    }

    private fun drawAllEntities(canvas: Canvas, start: Int, end: Int) {
        drawAllEntitiesExt(canvas, start, end)
    }

    private fun setupAnimation(quranEntity: QuranEntity) {
        setupAnimationQuran(quranEntity)
    }

    private fun setupAnimation(bismilahEntity: BismilahEntity) {
        setupAnimationBismilah(bismilahEntity)
    }

    fun translateFromNow() {
        translateFromNowExt()
    }

    fun translateToRight(isIsi3ada: Boolean) {
        translateToRightExt(isIsi3ada)
    }

    fun translateToRight() {
        translateToRightNoParamExt()
    }

    fun translateFromStart() {
        translateFromStartExt()
    }

    fun translateUntilNow() {
        translateUntilNowExt()
    }

    fun translateToRightBismilah(entityBismilahTimeline: EntityBismilahTimeline) {
        translateToRightBismilahExt(entityBismilahTimeline)
    }

    fun translateEndNow() {
        translateEndNowExt()
    }

    fun translateToStart() {
        translateToStartExt()
    }

    fun translateToEnd() {
        translateToEndExt()
    }

    fun translateToStart(entity: Entity?) {
        translateToStartEntityExt(entity)
    }

    fun translateToEnd(entity: Entity?) {
        translateToEndEntityExt(entity)
    }

    fun previewEntity(entity: Entity?) {
        previewEntityExt(entity)
    }

    fun updateCursurToSelectEntity() {
        updateCursurToSelectEntityExt()
    }

    fun addStack(entity: Entity, entityAction: EntityAction) {
        addStackExt(entity, entityAction)
    }

    fun selectEntity(entity: Entity?, invalidate: Boolean) {
        selectEntityExt(entity, invalidate)
    }

    fun stackSplit(entity: Entity) {
        stackSplitExt(entity)
    }

    fun splitAudio(entityAudio: EntityAudio, i: Int) {
        splitAudioExt(entityAudio, i)
    }

    fun deleteEntity(isTrsl: Boolean) {
        deleteEntityExt(isTrsl)
    }

    fun deleteEntityAllSelect() {
        deleteEntityAllSelectExt()
    }

    fun deleteMediaEntity() {
        deleteMediaEntityExt()
    }

    fun addAudio(entityAudio: EntityAudio, i: Int) {
        addAudioExt(entityAudio, i)
    }

    fun addAudio(entityAudio: EntityAudio) {
        addAudioSimpleExt(entityAudio)
    }

    fun addQuran(entityQuranTimeline: EntityQuranTimeline) {
        addQuranExt(entityQuranTimeline)
    }

    fun addTrslQuran(entityTrslTimeline: EntityTrslTimeline) {
        addTrslQuranExt(entityTrslTimeline)
    }

    fun addTrslQuran(entityTrslTimeline: EntityTrslTimeline, i: Int) {
        addTrslQuranAtIndexExt(entityTrslTimeline, i)
    }

    fun addQuran(entityQuranTimeline: EntityQuranTimeline, i: Int) {
        addQuranAtIndexExt(entityQuranTimeline, i)
    }

    fun addQuran_split(entityQuranTimeline: EntityQuranTimeline, i: Int) {
        addQuran_splitExt(entityQuranTimeline, i)
    }

    fun addQuran_split(entityTrslTimeline: EntityTrslTimeline, i: Int) {
        addQuran_splitTrslExt(entityTrslTimeline, i)
    }



    fun getXCursur(): Float = (-currentPosition) * scaleFactor



    fun setOnProgress(progress: Boolean) { isProgress = progress }

    fun isPass(motionEvent: MotionEvent): Boolean {
        return isPassExt(motionEvent)
    }

    internal fun updateMediaIndex() {
        for (i in entityListAudio.indices) entityListAudio[i].index = i
    }

    internal fun updateIndex() {
        for (i in entityListQuran.indices) {
            val eqt = entityListQuran[i]
            eqt.index = i
            eqt.quranEntity.index = i
        }
    }

    internal fun updateTrslIndex() {
        for (i in entityListTrslQuran.indices) {
            val etl = entityListTrslQuran[i]
            etl.index = i
            etl.quranEntity.index = i
        }
    }

    fun updateSelectionOnTap(motionEvent: MotionEvent) {
        updateSelectionOnTapExt(motionEvent)
    }

    fun finishScroll() {
        try {
            val sc = scroller
            if (sc != null && !sc.isFinished) sc.abortAnimation()
            scroller = null
        } catch (_: Exception) {
        }
    }

    override fun computeScroll() {
        val sc = scroller
        if (sc == null || isProgress || !sc.computeScrollOffset()) return
        if (currentPosition != 0.0f || sc.currX > 0) {
            val currX = sc.currX.toFloat()
            currentPosition = currX
            if (currX > 0.0f) currentPosition = 0.0f
            val f = currentPosition * scaleFactor
            scrolled_with_zoom = f
            if (!isPlaying) {
                iTrimLineCallback?.onSeekPlayer(f)
            }
            invalidate()
        }
    }

    fun pauseScroll() {
        val sc = scroller
        if (sc == null || sc.isFinished) return
        sc.abortAnimation()
    }

    fun updateWhenEffect(entityAudio: EntityAudio) {
        if (entityAudio.index + 1 >= entityListAudio.size) return
        val next = getPreviewOrNextEntityAudio(entityListAudio, entityAudio.index + 1, true) ?: return
        if (entityAudio.rect.right <= next.rect.left) return
        val width = next.rect.width() + entityAudio.rect.right
        val f = entityAudio.rect.right - next.rect.left
        next.setCurrentRect()
        next.setX(entityAudio.rect.right)
        next.right = width
        for (index in entityAudio.index + 2 until entityListAudio.size) {
            val ea = entityListAudio[index]
            if (ea.visible()) {
                val f2 = ea.rect.left + f
                val width2 = ea.rect.width() + f2
                ea.setCurrentRect()
                ea.setX(f2)
                ea.right = width2
            }
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        return onTouchExt(view, motionEvent)
    }

    fun flingY() {
        val ofFloat = ObjectAnimator.ofFloat(this, "FlingY", target, 0.0f)
        objectAnimator = ofFloat
        ofFloat.duration = 1000L
        objectAnimator!!.start()
    }

    fun setFlingY(f: Float) {
        target = f
        if (f <= 0.0f) {
            if (entityY + mScrollY >= height) {
                val f2 = mScrollY + target / 100.0f
                mScrollY = f2
                if (entityY + f2 < height) mScrollY = (height - entityY).toFloat()
                invalidate()
            }
            return
        }
        if (mScrollY < 0.0f) {
            val f4 = mScrollY + f / 100.0f
            mScrollY = f4
            if (f4 > 0.0f) mScrollY = 0.0f
            invalidate()
        }
    }

    fun getPreviewOrNextEntityAudio(list: List<EntityAudio>, i: Int, next: Boolean): EntityAudio? {
        if (next) {
            var idx = i
            while (idx < list.size) {
                if (list[idx].visible()) return list[idx]
                idx++
            }
            return null
        }
        var idx = i
        while (idx >= 0 && idx < list.size) {
            if (list[idx].visible()) return list[idx]
            idx--
        }
        return null
    }

    fun getPreviewOrNextEntityQuran(list: List<EntityQuranTimeline>, i: Int, next: Boolean): EntityQuranTimeline? {
        if (next) {
            var idx = i
            while (idx < list.size) {
                if (list[idx].visible()) return list[idx]
                idx++
            }
            return null
        }
        var idx = i
        while (idx >= 0 && idx < list.size) {
            if (list[idx].visible()) return list[idx]
            idx--
        }
        return null
    }

    fun getPreviewOrNextEntityTrslQuran(list: List<EntityTrslTimeline>, i: Int, next: Boolean): EntityTrslTimeline? {
        if (next) {
            var idx = i
            while (idx < list.size) {
                if (list[idx].visible()) return list[idx]
                idx++
            }
            return null
        }
        var idx = i
        while (idx >= 0 && idx < list.size) {
            if (list[idx].visible()) return list[idx]
            idx--
        }
        return null
    }

    fun getAudio(): EntityAudio? {
        for (size in entityListAudio.size - 1 downTo 0) {
            val ea = entityListAudio[size]
            if (ea.visible()) return ea
        }
        return null
    }

    fun getLastAyaQuran(): EntityQuranTimeline? {
        if (entityListQuran.isEmpty()) return null
        return entityListQuran[entityListQuran.size - 1]
    }

    fun getQuran(): EntityQuranTimeline? {
        for (size in entityListQuran.size - 1 downTo 0) {
            val eqt = entityListQuran[size]
            if (eqt.visible()) return eqt
        }
        return null
    }

    fun getTrslQuran(): EntityTrslTimeline? {
        for (size in entityListTrslQuran.size - 1 downTo 0) {
            val etl = entityListTrslQuran[size]
            if (etl.visible()) return etl
        }
        return null
    }

    fun calculMaxTime() {
        calculMaxTimeExt()
    }

    fun update_current_cursur_position(i: Int) { current_cursur_position = i }





    fun unselectEntity() {
        val entity = selectedEntity
        if (entity != null) {
            entity.isSelect = false
            selectedEntity = null
        }
    }



    fun updateCursur(i: Int) {
        current_cursur_position = i
        val f = (-i * second_in_screen) / 1000.0f
        currentPosition = f
        scrolled_with_zoom = f * scaleFactor
        invalidate()
    }

    fun setPosCursur(i: Int) {
        current_cursur_position = i
        val f = (-i * second_in_screen) / 1000.0f
        currentPosition = f
        scrolled_with_zoom = f * scaleFactor
        invalidate()
    }

    fun updateCursur(f: Float) {
        val f2 = -f
        currentPosition = f2
        scrolled_with_zoom = f2 * scaleFactor
        invalidate()
    }

    fun setRedoUndo(imageButton: ImageButton, imageButton2: ImageButton) {
        btn_redo = imageButton
        btn_undo = imageButton2
    }

    fun undo() {
        undoExt()
    }

    fun redo() {
        redoExt()
    }

    fun handleItemInteraction(f: Float, f2: Float): Boolean {
        return handleItemInteractionExt(f, f2)
    }

    private fun processQuranItemsSelection(): Int {
        return processQuranItemsSelectionExt()
    }

    private fun processTrslQuranItemsSelection(): Int {
        return processTrslQuranItemsSelectionExt()
    }

    private fun processAudioItemsSelection(): Int {
        return processAudioItemsSelectionExt()
    }

    private fun deselectAllQuranItems(): Boolean {
        return deselectAllQuranItemsExt()
    }

    private fun deselectAllTrslQuranItems(): Boolean {
        return deselectAllTrslQuranItemsExt()
    }

    private fun deselectAllAudioItems(): Boolean {
        return deselectAllAudioItemsExt()
    }

    // ── Missing public API methods for Java interop ──────────────────

    fun getDuration(): Int = duration

    fun setDuration(value: Int) { duration = value }

    fun setCurrent_cursur_position(value: Int) { current_cursur_position = value }

    fun getTimeLineW(): Float = timeLineW
}
