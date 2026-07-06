package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.text.Layout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.constant.SurahNameStyle
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.EntitySelectTool
import hazem.nurmontage.videoquran.model.EntityView
import hazem.nurmontage.videoquran.model.Gradient
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.multitouch.MoveGestureDetector
import hazem.nurmontage.videoquran.utils.AspectRatioCalculator
import hazem.nurmontage.videoquran.utils.ColorSchemeGenerator
import hazem.nurmontage.videoquran.utils.ColorUtils
import hazem.nurmontage.videoquran.utils.CreateGradient
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.utils.UtilsFileLast
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.views.blurred.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min


class BlurredImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), View.OnTouchListener {

    //  Companion object (static constants)

    companion object {
        private const val SNAP_FORCE = 0.2f
        private const val SNAP_THRESHOLD = 30.0f
    }

    //  Interface

    interface IViewCallback {
        fun onDrawFinish()
        fun onEmtyClick()
        fun onEndMove()
        fun onEndScale()
        fun onSelect(entityView: EntityView)
        fun onSquare()
        fun onWattermark()
    }

    //  Fields — all properties from the original Java class

    internal var backgroundPaint: Paint = Paint()
        internal set
    internal var bismilahEntity: BismilahEntity? = null
    internal var bitmapBlured: Bitmap? = null
    internal var bitmapNotBlur: Bitmap? = null
    internal var bitmapOriginal: Bitmap? = null
    internal var bitmapSquare: Bitmap? = null
    internal var btmX: Float = 0f
    internal var btmY: Float = 0f
    internal var clr_aya: Int = 0
    internal var clr_trsl: Int = 0
    internal var color_bg_type_classic: Int = 0
    internal var color_gradient: Gradient? = null
    internal var color_ipad: Int = -1
    internal var color_line_bg: Int = 0
    internal var currentTime: String = "0:00"
    internal var darkShadowPaint: Paint = Paint()
        internal set
    internal var entity_select: EntityView? = null
    internal var frameInterval: Long = 0L
    internal var gestureDetector: GestureDetectorCompat? = null
    internal var grayscalePaint: Paint = Paint()
    internal var iViewCallback: IViewCallback? = null
    internal var ipad_rect: RectF? = null
    internal var isAnimWatermk: Boolean = false
    internal var isDrawingSquareVideo: Boolean = false
    internal var isGlass: Boolean = false
    internal var isNotDraw: Boolean = false
    internal var isOnScale: Boolean = false
    internal var isPlaying: Boolean = false
    internal var isRemoveWattermark: Boolean = false
    internal var isSquare: Boolean = false
    internal var isVideo: Boolean = false
    internal var isWattermark: Boolean = false
    internal var left_square: Float = 0f
    internal var lightShadowPaint: Paint = Paint()
        internal set
    internal var linePaint: Paint = Paint()
    internal var linearGradient_classic: LinearGradient? = null
    internal var mCanvas_height: Int = 0
    internal var mCanvas_width: Int = 0
    internal var mDrawingTranslationX: Float = 0f
    internal var mDrawingTranslationY: Float = 0f
    internal var mIpadType: Int = IpadType.IPAD.ordinal
    internal var mIsti3adhaEntity: BismilahEntity? = null
    internal var mRectWattermark: RectF? = null
    internal var mResizetype: Int = 0
    internal var moveGestureDetector: MoveGestureDetector? = null
    internal var newLeft_txt: Float = 0f
    internal var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal var paintClear: Paint = Paint()
    internal var paintIpad: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal var paintLecture: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal var paintText: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    internal var paintWattermark: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal var prevDistance: Float = -1.0f
    internal var progress: Float = 0f
    internal val quranEntities: MutableList<QuranEntity> = ArrayList()
    internal var radius_cursur: Float = 0f
    internal var radius_square: Int = 0
    internal var rectFAya: RectF? = null
    internal var rectFLecture: RectF? = null
    internal var rectFProgress: RectF? = null
    internal var rectFSurahName: RectF? = null
    internal var rectSquare: Rect? = null
    internal var remainingTime: String = "0:15"
    internal var scaleGestureDetector: ScaleGestureDetector? = null
    internal var scheme: ColorSchemeGenerator.Scheme? = null
    internal var selectTool: EntitySelectTool? = null
    internal var showCenterLineX: Boolean = false
    internal var showCenterLineY: Boolean = false
    internal var startTime: Long = -1L
    internal var surahNameEntity: SurahNameEntity? = null
    internal var top_square: Float = 0f
    internal val translationEntities: MutableList<TranslationQuranEntity> = ArrayList()
    internal var txt_y: Float = 0f
    internal var wmAlpha: Float = 1.0f
    internal var wmScale: Float = 1.0f
    internal var wmTranslateY: Float = 0f

    //  Gesture listener — defined once, used by all constructors

    private val gestureListener: GestureDetector.SimpleOnGestureListener =
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                // Billing removed — watermark click guard removed; all users are pro
                if (entity_select != null && entity_select!!.isVisible && !isWattermark) {
                    if (selectTool!!.isApply(entity_select!!, e.x, e.y)) {
                        if (selectTool!!.isApply_Move) {
                            iViewCallback!!.onEndMove()
                        }
                        if (selectTool!!.isApply_Scale) {
                            iViewCallback!!.onEndScale()
                        }
                        selectTool!!.setClick_apply(true)
                        selectTool!!.reset()
                    } else {
                        selectTool!!.isScale(entity_select!!, e.x, e.y)
                    }
                    if (selectTool!!.isApply_Scale) {
                        selectTool!!.setOnProgress(true)
                        prevDistance = distanceToCenter(e.x, e.y)
                    }
                }
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (entity_select != null && selectTool!!.isClick_apply) {
                    selectTool!!.setClick_apply(false)
                    invalidate()
                    return true
                }
                if (!isWattermark) {
                    updateSelectionOnTap(e)
                }
                isOnScale = false
                if (iViewCallback != null) {
                    if (entity_select == null) {
                        if (isWattermark) {
                            iViewCallback!!.onWattermark()
                        } else if (isSquare) {
                            iViewCallback!!.onSquare()
                        } else {
                            iViewCallback!!.onEmtyClick()
                        }
                    } else if (selectTool != null && selectTool!!.isApply_Move &&
                        ((entity_select is QuranEntity) || (entity_select is TranslationQuranEntity)) &&
                        !selectTool!!.isApply_all
                    ) {
                        selectTool!!.setApply_all(true)
                        invalidate()
                    }
                    isWattermark = false
                    isSquare = false
                }
                return super.onSingleTapUp(e)
            }
        }

    //  init block — calls init()

    init {
        init()
    }

    //  init() — initializes touch listeners, paints, and detectors

    private fun init() {
        setOnTouchListener(this)
        moveGestureDetector = MoveGestureDetector(getContext(), MoveListener())
        scaleGestureDetector = ScaleGestureDetector(getContext(), ScaleListener())
        gestureDetector = GestureDetectorCompat(getContext(), gestureListener)
        grayscalePaint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0.0f)
        grayscalePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        val pw = Paint(Paint.ANTI_ALIAS_FLAG)
        paintWattermark = pw
        pw.color = ViewCompat.MEASURED_STATE_MASK
        paintWattermark.alpha = 25
        paintWattermark.typeface = UtilsFileLast.loadFontFromAsset(getContext(), "fonts/ReadexPro_Medium.ttf")
        paintWattermark.isFakeBoldText = true
        val lp = Paint()
        linePaint = lp
        lp.isAntiAlias = true
        paintLecture = Paint(Paint.ANTI_ALIAS_FLAG)
        paintIpad = Paint(Paint.ANTI_ALIAS_FLAG)
        paintText = TextPaint(Paint.ANTI_ALIAS_FLAG)
        val cp = Paint()
        paintClear = cp
        cp.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        paintText.typeface = UtilsFileLast.loadFontFromAsset(getContext(), "fonts/arabic/NotoNaskhArabic.ttf")
    }

    //  entity_select with custom setter logic

    fun setEntity_select(entityView: EntityView?) {
        if (this.entity_select != entityView) {
            selectTool?.reset()
        }
        this.entity_select = entityView
    }

    fun getEntity_select(): EntityView? = this.entity_select

    //  Simple getters and setters

    fun isRemoveWattermark(): Boolean = this.isRemoveWattermark
    fun setRemoveWattermark(z: Boolean) { this.isRemoveWattermark = z }
    fun setBitmapNotBlur(bitmap: Bitmap?) { this.bitmapNotBlur = bitmap }
    fun getBitmapNotBlur(): Bitmap? = this.bitmapNotBlur
    fun isVideo(): Boolean = this.isVideo
    fun setVideo(z: Boolean) { this.isVideo = z }
    fun setDrawingSquareVideo(z: Boolean) { this.isDrawingSquareVideo = z }
    fun isDrawingSquareVideo(): Boolean = this.isDrawingSquareVideo
    fun setPlaying(z: Boolean) { this.isPlaying = z }
    fun isPlaying(): Boolean = this.isPlaying
    fun setPro(z: Boolean) { /* Billing removed — always pro */ }
    fun isPro(): Boolean = true // Billing removed
    fun setBitmapOriginal(bitmap: Bitmap?) { this.bitmapOriginal = bitmap }
    fun getBitmapOriginal(): Bitmap? = this.bitmapOriginal
    fun setGlass(z: Boolean) { this.isGlass = z }
    fun isGlass(): Boolean = this.isGlass

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (this.selectTool == null) {
            this.selectTool = EntitySelectTool(w, context)
        }
    }

    fun getColor_ipad(): Int = this.color_ipad
    fun setColor_ipad(color: Int) { setColorIpad(color) }
    fun setResizetype(i: Int) { this.mResizetype = i }
    fun getmIpadType(): Int = this.mIpadType
    fun setIpad_rect(rectF: RectF?) { this.ipad_rect = rectF }
    fun setRectSquare(rect: Rect?) { this.rectSquare = rect }
    fun getBtmX(): Float = this.btmX
    fun getBtmY(): Float = this.btmY
    fun getmDrawingTranslationY(): Float = this.mDrawingTranslationY
    fun getmDrawingTranslationX(): Float = this.mDrawingTranslationX
    fun getmCanvas_height(): Int = this.mCanvas_height
    fun getmCanvas_width(): Int = this.mCanvas_width

    //  Canvas dimension initialization

    fun initCanvasDimension(i: Int, i2: Int, i3: Int) {
        if (i3 == ResizeType.SOCIAL_STORY.ordinal) {
            this.mCanvas_height = i2
            this.mCanvas_width = AspectRatioCalculator.calculateWidth(i2)
        } else if (i3 == ResizeType.SQUARE.ordinal) {
            val minVal = min(i, i2)
            this.mCanvas_width = minVal
            this.mCanvas_height = minVal
        } else {
            this.mCanvas_width = i
            this.mCanvas_height = AspectRatioCalculator.calculateHeight_Youtube(i)
        }
    }

    fun getW(): Int = (width - paddingStart) - paddingEnd
    fun getH(): Int = (height - paddingTop) - paddingBottom

    //  updatePosCanvas (both overloads)

    fun updatePosCanvas(bitmap: Bitmap?) {
        if (bitmap == null) return
        val w = (width - paddingStart - paddingEnd).toFloat()
        val h = (height - paddingTop - paddingBottom).toFloat()
        this.mDrawingTranslationX = (w - this.mCanvas_width) / 2.0f
        this.mDrawingTranslationY = (h - this.mCanvas_height) / 2.0f
        this.btmX = ((w - bitmap.width) / 2.0f) - this.mDrawingTranslationX
        this.btmY = ((h - bitmap.height) / 2.0f) - this.mDrawingTranslationY
    }

    fun updatePosCanvas(i: Int, i2: Int, bitmap: Bitmap?) {
        if (bitmap == null) return
        this.mDrawingTranslationX = (i - this.mCanvas_width) / 2.0f
        this.mDrawingTranslationY = (i2 - this.mCanvas_height) / 2.0f
        this.btmX = ((i - bitmap.width) / 2.0f) - this.mDrawingTranslationX
        this.btmY = ((i2 - bitmap.height) / 2.0f) - this.mDrawingTranslationY
    }

    fun getProgress(): Float = this.progress

    //  addEntity (all 4 overloads)

    fun addEntity(quranEntity: QuranEntity) {
        this.quranEntities.add(quranEntity)
        quranEntity.setIndex(this.quranEntities.size - 1)
    }

    fun addEntity(translationQuranEntity: TranslationQuranEntity) {
        this.translationEntities.add(translationQuranEntity)
        translationQuranEntity.setIndex(this.translationEntities.size - 1)
    }

    fun addEntity(quranEntity: QuranEntity, i: Int) {
        if (i < this.quranEntities.size) {
            this.quranEntities.add(i, quranEntity)
        } else {
            this.quranEntities.add(quranEntity)
        }
        quranEntity.setIndex(i)
    }

    fun addEntity(translationQuranEntity: TranslationQuranEntity, i: Int) {
        if (i < this.translationEntities.size) {
            this.translationEntities.add(i, translationQuranEntity)
        } else {
            this.translationEntities.add(translationQuranEntity)
        }
        translationQuranEntity.setIndex(i)
    }

    fun getQuranEntities(): List<QuranEntity> = this.quranEntities
    fun getPaintLecture(): Paint = this.paintLecture
    fun getBitmapSquare(): Bitmap? = this.bitmapSquare
    fun setClr_aya(i: Int) { this.clr_aya = i }
    fun setClr_trsl(i: Int) { this.clr_trsl = i }
    fun getClr_aya(): Int = this.clr_aya
    fun getClr_trsl(): Int = this.clr_trsl
    fun getColor_gradient(): Gradient? = this.color_gradient
    fun setColor_gradient(gradient: Gradient?) { this.color_gradient = gradient }
    fun colorIpad(): Int = this.color_ipad

    //  changeColorIpad

    fun changeColorIpad() {
        if (getColor_gradient() != null) {
            setColorIpad(getColor_gradient()!!)
        } else {
            setColorIpad(colorIpad())
        }
    }

    //  setColorIpad(int) — per-IpadType color logic

    fun setColorIpad(i: Int) {
        setColor_gradient(null)
        this.paintIpad.shader = null
        this.color_ipad = i
        if (this.mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
            this.color_bg_type_classic = ColorUtils.lightenColor(i, 0.4f)
            this.paintIpad.color = ColorUtils.darkenColor(i, 0.2f)
        } else {
            this.paintIpad.color = i
        }
        if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.color_line_bg = ColorUtils.darkenColor(i, 0.4f)
            this.paintLecture.color = i
        } else if (this.mIpadType == IpadType.BLUE_TYPE.ordinal) {
            this.paintLecture.color = ColorUtils.convertToEnergyColor(i)
            this.color_line_bg = ColorUtils.darkenColor(this.paintLecture.color, 0.7f)
        } else if (this.mIpadType == IpadType.CASSET.ordinal ||
            this.mIpadType == IpadType.CASSET_IMG.ordinal ||
            this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal
        ) {
            val generateScheme = ColorSchemeGenerator.generateScheme(i)
            this.scheme = generateScheme
            if (ColorUtils.isColorDark(generateScheme.label)) {
                this.paintLecture.color = -1
            } else {
                this.paintLecture.color = ViewCompat.MEASURED_STATE_MASK
            }
            this.color_line_bg = ColorUtils.darkenColor(this.paintLecture.color, 0.7f)
        } else {
            this.color_line_bg = ColorUtils.darkenColor(i, 0.4f)
            this.paintIpad.alpha = 190
            if (ColorUtils.isColorDark(this.paintIpad.color)) {
                this.paintLecture.color = -1
            } else {
                this.paintLecture.color = ViewCompat.MEASURED_STATE_MASK
            }
        }
        this.paintText.color = this.paintLecture.color
    }

    //  setColorIpad(Gradient) — gradient with per-IpadType color logic

    fun setColorIpad(gradient: Gradient) {
        setColor_gradient(gradient)
        val color = gradient.color
        if (this.mIpadType == IpadType.IPAD_CLASSIC.ordinal) {
            this.paintIpad.shader = null
            this.linearGradient_classic = CreateGradient.createLinearGradientWithAngle(
                this.ipad_rect!!,
                gradient.angle.toFloat(),
                intArrayOf(
                    ColorUtils.lightenColor(gradient.color, 0.4f),
                    ColorUtils.lightenColor(gradient.second, 0.4f),
                    ColorUtils.lightenColor(gradient.three, 0.4f)
                ),
                floatArrayOf(0.0f, 0.7f, 1.0f)
            )
            this.paintIpad.color = ColorUtils.darkenColor(gradient.second, 0.2f)
        } else {
            val createLinearGradientWithAngle = CreateGradient.createLinearGradientWithAngle(
                this.ipad_rect!!,
                gradient.angle.toFloat(),
                intArrayOf(gradient.color, gradient.second, gradient.three),
                floatArrayOf(0.0f, 0.7f, 1.0f)
            )
            this.linearGradient_classic = createLinearGradientWithAngle
            this.paintIpad.shader = createLinearGradientWithAngle
            this.paintIpad.color = color
        }
        this.color_line_bg = ColorUtils.darkenColor(color, 0.4f)
        if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.paintLecture.color = color
        } else if (this.mIpadType == IpadType.BLUE_TYPE.ordinal) {
            this.paintLecture.color = ColorUtils.lightenColor(color, 0.7f)
        } else if (this.mIpadType == IpadType.CASSET.ordinal ||
            this.mIpadType == IpadType.CASSET_IMG.ordinal ||
            this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal
        ) {
            val generateScheme = ColorSchemeGenerator.generateScheme(color, gradient.angle.toFloat())
            this.scheme = generateScheme
            if (ColorUtils.isColorDark(generateScheme.label)) {
                this.paintLecture.color = -1
            } else {
                this.paintLecture.color = ViewCompat.MEASURED_STATE_MASK
            }
        } else {
            this.paintIpad.alpha = 190
            if (ColorUtils.isColorDark(this.paintIpad.color)) {
                this.paintLecture.color = -1
            } else {
                this.paintLecture.color = ViewCompat.MEASURED_STATE_MASK
            }
        }
        this.paintText.color = this.paintLecture.color
    }

    //  setIcon

    fun setIcon(str: String, vectorDrawable: VectorDrawable) {
        for (quranEntity in this.quranEntities) {
            if (quranEntity.getIcon() != null && quranEntity.getIcon() != str && quranEntity.getNumber() != -1) {
                quranEntity.setVectorDrawable(vectorDrawable)
                quranEntity.setIcon(str)
                quranEntity.updateIconDraw()
            }
        }
        updateSizeAya()
        invalidate()
    }

    //  setTypeface

    fun setTypeface(typeface: Typeface, str: String) {
        val entityView = this.entity_select
        if (entityView is QuranEntity) {
            for (quranEntity in this.quranEntities) {
                if (quranEntity.getNameFont() != null && quranEntity.getNameFont() != str) {
                    quranEntity.setTypeface(typeface, str)
                }
            }
            updateSizeAyaResize()
        } else if (entityView is TranslationQuranEntity) {
            for (translationQuranEntity in this.translationEntities) {
                if (translationQuranEntity.getNameFont() != null && translationQuranEntity.getNameFont() != str) {
                    translationQuranEntity.setTypeface(typeface, str)
                }
            }
            updateSizeTrslAyaResize()
        }
        invalidate()
    }

    //  setPreset

    fun setPreset(ayaTextPreset: AyaTextPreset) {
        for (quranEntity in this.quranEntities) {
            quranEntity.setPreset(ayaTextPreset)
        }
        if (this.mIsti3adhaEntity != null && this.mIsti3adhaEntity!!.getBismilahTimeline()?.visible() == true) {
            this.mIsti3adhaEntity!!.setPreset(ayaTextPreset)
        }
        if (this.bismilahEntity != null && this.bismilahEntity!!.getBismilahTimeline()?.visible() == true) {
            this.bismilahEntity!!.setPreset(ayaTextPreset)
        }
        invalidate()
    }

    //  setTrslPreset

    fun setTrslPreset(ayaTextPreset: AyaTextPreset) {
        for (t in this.translationEntities) {
            t.setPreset(ayaTextPreset)
        }
        invalidate()
    }

    //  setColorAya

    fun setColorAya(i: Int) {
        setClr_aya(i)
        for (q in this.quranEntities) {
            q.setColor(i)
        }
        if (this.mIsti3adhaEntity != null && this.mIsti3adhaEntity!!.getBismilahTimeline()?.visible() == true) {
            this.mIsti3adhaEntity!!.setColor(i)
        }
        if (this.bismilahEntity != null && this.bismilahEntity!!.getBismilahTimeline()?.visible() == true) {
            this.bismilahEntity!!.setColor(i)
        }
        invalidate()
    }

    //  setColorTrsl

    fun setColorTrsl(i: Int) {
        setClr_trsl(i)
        for (t in this.translationEntities) {
            t.setColor(i)
        }
        invalidate()
    }

    //  applyAll

    fun applyAll(f: Float, rectF: RectF, i: Int, i2: Int) {
        val entityView = this.entity_select ?: return
        if (entityView is QuranEntity) {
            val quranEntity = entityView as QuranEntity
            for (quranEntity2 in this.quranEntities) {
                if (quranEntity2 !== quranEntity) {
                    quranEntity2.applyAll(
                        getmCanvas_width(), rectF,
                        quranEntity.getPaintAya().textSize,
                        quranEntity.factorSize
                    )
                }
            }
            invalidate()
            return
        }
        if (entityView is TranslationQuranEntity) {
            val translationQuranEntity = entityView as TranslationQuranEntity
            for (translationQuranEntity2 in this.translationEntities) {
                if (translationQuranEntity2 !== translationQuranEntity) {
                    translationQuranEntity2.applyAll(
                        getmCanvas_width(), rectF,
                        translationQuranEntity.getPaintAya().textSize,
                        translationQuranEntity.factorSize
                    )
                }
            }
            invalidate()
        }
    }

    //  setCurrentTime

    fun setCurrentTime(str: String, str2: String) {
        this.currentTime = str
        this.remainingTime = "-$str2"
    }

    //  radius_square

    fun getRadius_square(): Int = this.radius_square
    fun setRadius_square(i: Int) { this.radius_square = i }

    //  setBitmapBlured

    fun setBitmapBlured(bitmap: Bitmap?) { this.bitmapBlured = bitmap }

    //  setBitmapSquare

    fun setBitmapSquare(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        this.bitmapSquare = bitmap
    }

    //  setBitmap (3 overloads) and updateBitmap (2 overloads)

    fun setBitmap(bitmap: Bitmap?, bitmap2: Bitmap?, i: Int, i2: Int, i3: Int, rect: Rect?) {
        this.bitmapBlured = bitmap
        if (bitmap2 != null) {
            this.bitmapSquare = bitmap2
        }
        this.rectSquare = rect
        this.mIpadType = i2
        if (i != -1) {
            setColorIpad(i)
        } else if (bitmap2 != null) {
            setColorIpad(ColorUtils.getAverageColor(bitmap2))
        }
        this.mResizetype = i3
        if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.07f
        } else if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.027f
        } else {
            this.paintText.textSize = this.ipad_rect!!.width() * 0.0388f
        }
        createRect()
    }

    fun updateBitmap(bitmap: Bitmap?, bitmap2: Bitmap?, i: Int, i2: Int, i3: Int, rect: Rect?) {
        this.bitmapBlured = bitmap
        if (bitmap2 != null) {
            this.bitmapSquare = bitmap2
        }
        this.rectSquare = rect
        this.mIpadType = i2
        if (i != -1) {
            setColorIpad(i)
        } else if (bitmap2 != null) {
            setColorIpad(ColorUtils.getAverageColor(bitmap2))
        }
        this.mResizetype = i3
        if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.07f
        } else if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.027f
        } else {
            this.paintText.textSize = this.ipad_rect!!.width() * 0.0388f
        }
    }

    fun setBitmap(bitmap: Bitmap?, bitmap2: Bitmap?, gradient: Gradient, i: Int, i2: Int, rect: Rect?) {
        this.bitmapBlured = bitmap
        if (bitmap2 != null) {
            this.bitmapSquare = bitmap2
        }
        this.rectSquare = rect
        this.mIpadType = i
        setColorIpad(gradient)
        this.mResizetype = i2
        if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.07f
        } else if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.027f
        } else {
            this.paintText.textSize = this.ipad_rect!!.width() * 0.0388f
        }
        createRect()
    }

    fun updateBitmap(bitmap: Bitmap?, bitmap2: Bitmap?, gradient: Gradient, i: Int, i2: Int, rect: Rect?) {
        this.bitmapBlured = bitmap
        if (bitmap2 != null) {
            this.bitmapSquare = bitmap2
        }
        this.rectSquare = rect
        this.mIpadType = i
        setColorIpad(gradient)
        this.mResizetype = i2
        if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.07f
        } else if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.027f
        } else {
            this.paintText.textSize = this.ipad_rect!!.width() * 0.0388f
        }
    }

    //  getIpad_rect, setmIpadType, changeTypeIpad, getRectSquare,
    //  getRectFAya, getRectFProgress, getRectFSurahName

    fun getIpad_rect(): RectF? = this.ipad_rect

    fun setmIpadType(i: Int) { this.mIpadType = i }

    fun changeTypeIpad(i: Int) {
        this.mIpadType = i
        updateIpad()
        if (this.mIpadType == IpadType.BOTTOM_RECT.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.07f
        } else if (this.mIpadType == IpadType.BORDER.ordinal) {
            this.paintText.textSize = min(this.ipad_rect!!.width(), this.ipad_rect!!.height()) * 0.027f
        } else {
            this.paintText.textSize = this.ipad_rect!!.width() * 0.0388f
        }
    }

    fun getRectSquare(): Rect? = this.rectSquare
    fun getRectFAya(): RectF? = this.rectFAya
    fun getRectFProgress(): RectF? = this.rectFProgress
    fun getRectFSurahName(): RectF? = this.rectFSurahName
    fun getRectFLecture(): RectF? = this.rectFLecture

    //  getBitmapBlured

    fun getBitmapBlured(): Bitmap? = this.bitmapBlured

    //  reset, resetWatermark, animWatermark

    fun reset() {
        val bitmap = this.bitmapBlured
        if (bitmap != null && !bitmap.isRecycled) {
            this.bitmapBlured!!.recycle()
        }
        val bitmap2 = this.bitmapSquare
        if (bitmap2 == null || bitmap2.isRecycled) {
            return
        }
        this.bitmapSquare!!.recycle()
    }

    fun resetWatermark() {
        this.wmAlpha = 1.0f
        this.wmScale = 1.0f
        this.wmTranslateY = 0.0f
        this.isAnimWatermk = false
    }

    fun animWatermark(alpha: Float, scale: Float, translateY: Float) {
        this.isAnimWatermk = true
        this.wmAlpha = alpha
        this.wmScale = scale
        this.wmTranslateY = translateY
        invalidate()
    }

    //  setNotDraw, setiViewCallback

    fun setNotDraw(z: Boolean) { this.isNotDraw = z }

    fun setiViewCallback(callback: IViewCallback?) { this.iViewCallback = callback }

    //  Bismilah and SurahName entity helpers

    fun getmIsti3adhaEntity(): BismilahEntity? = this.mIsti3adhaEntity

    fun addIsti3adhaEntity(entity: BismilahEntity) {
        this.mIsti3adhaEntity = entity
    }

    fun getBismilahEntity(): BismilahEntity? = this.bismilahEntity

    fun addBismilahEntity(entity: BismilahEntity) {
        this.bismilahEntity = entity
    }

    fun getSurahNameEntity(): SurahNameEntity? = this.surahNameEntity

    //  updateAlignmentSurah

    fun updateAlignmentSurah(textValue: String): Layout.Alignment {
        if (this.mIpadType == IpadType.IPAD_NEOMORPHIC.ordinal || this.mIpadType == IpadType.CASSET.ordinal || this.mIpadType == IpadType.CASSET_IMG.ordinal || this.mIpadType == IpadType.CASSET_IMG_BLUR.ordinal) {
            return Layout.Alignment.ALIGN_CENTER
        }
        return if (!Utils.isProbablyLArabic(textValue)) {
            Layout.Alignment.ALIGN_NORMAL
        } else {
            Layout.Alignment.ALIGN_OPPOSITE
        }
    }

    // Additional accessors needed by extension files
    fun getTranslationEntities(): List<TranslationQuranEntity> = translationEntities
    fun getLeft_square(): Float = left_square
    fun getTop_square(): Float = top_square
    fun getLinePaint(): Paint = linePaint
    fun getPaintText(): TextPaint = paintText
    fun getSelectTool(): EntitySelectTool? = selectTool
    fun getStartTime(): Long = startTime
    fun setStartTime(t: Long) { startTime = t }
    fun getFrameInterval(): Long = frameInterval
    fun getScheme(): ColorSchemeGenerator.Scheme? = scheme
    fun setShowCenterLineX(show: Boolean) { this.showCenterLineX = show }
    fun setShowCenterLineY(show: Boolean) { this.showCenterLineY = show }
    fun isSquare(): Boolean = isSquare
    fun setSquare(square: Boolean) { isSquare = square }
    fun isWattermark(): Boolean = isWattermark
    fun setWattermark(wattermark: Boolean) { isWattermark = wattermark }
    fun isAnimWatermk(): Boolean = isAnimWatermk
    fun setAnimWatermk(animWatermk: Boolean) { isAnimWatermk = animWatermk }
    fun isNotDraw(): Boolean = isNotDraw
    fun isOnScale(): Boolean = isOnScale

    //  findEntityAtPoint — checks entities in the original Java order:
    //  surahNameEntity → mIsti3adhaEntity → bismilahEntity → quranEntities → translationEntities

    private fun findEntityAtPoint(x: Float, y: Float): EntityView? {
        val surahName = this.surahNameEntity
        if (surahName != null && surahName.rect.contains(x, y)) {
            return this.surahNameEntity
        }
        val isti3adha = this.mIsti3adhaEntity
        if (isti3adha != null && isti3adha.isVisible && this.mIsti3adhaEntity!!.getBismilahTimeline()?.visible() == true && this.mIsti3adhaEntity!!.rect.contains(x, y)) {
            return this.mIsti3adhaEntity
        }
        val bismilah = this.bismilahEntity
        if (bismilah != null && bismilah.isVisible && this.bismilahEntity!!.getBismilahTimeline()?.visible() == true && this.bismilahEntity!!.rect.contains(x, y)) {
            return this.bismilahEntity
        }
        for (i in this.quranEntities.indices.reversed()) {
            val quranEntity = this.quranEntities[i]
            if (quranEntity.isVisible && quranEntity.entityQuran?.visible() == true && quranEntity.rect.contains(x, y)) {
                return quranEntity
            }
        }
        for (i in this.translationEntities.indices.reversed()) {
            val translationQuranEntity = this.translationEntities[i]
            if (translationQuranEntity.isVisible && translationQuranEntity.entityTrslTimeline?.visible() == true && translationQuranEntity.rect.contains(x, y)) {
                return translationQuranEntity
            }
        }
        return null
    }

    //  updateSelectionOnTap — takes MotionEvent as in the original

    fun updateSelectionOnTap(motionEvent: MotionEvent) {
        setEntity_select(findEntityAtPoint(motionEvent.x, motionEvent.y))
        val callback = this.iViewCallback
        if (callback != null) {
            val entityView = this.entity_select
            if (entityView != null) {
                callback.onSelect(entityView)
            } else {
                callback.onEmtyClick()
            }
        }
        invalidate()
    }

    //  Inner class: MoveListener

    private inner class MoveListener : MoveGestureDetector.SimpleOnMoveGestureListener() {
        override fun onMove(detector: MoveGestureDetector): Boolean {
            handleTranslate(detector.getFocusDelta())
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            super.onMoveEnd(detector)
            if (entity_select == null || selectTool == null) return
            selectTool!!.setApply_all(true)
        }
    }

    //  Inner class: ScaleListener

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            if (entity_select != null) {
                isOnScale = true
                selectTool!!.setApply_Scale(true)
                selectTool!!.setOnProgress(true)
            }
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (entity_select == null) return true
            entity_select!!.scale(detector.scaleFactor, mCanvas_width, mCanvas_height)
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            if (entity_select is QuranEntity) {
                selectTool!!.setApply_all(true)
                selectTool!!.setOnProgress(false)
            }
            super.onScaleEnd(detector)
        }
    }

    //  onTouch — faithfully reproduces the original's coordinate
    //  translation, single-finger scale, and ACTION_UP logic

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent == null) {
            return false
        }
        // Translate coordinates from view space to canvas space
        motionEvent.setLocation(
            (motionEvent.x + paddingLeft) - this.mDrawingTranslationX,
            (motionEvent.y + paddingTop) - this.mDrawingTranslationY
        )

        // If multi-touch, delegate to scale detector
        if (motionEvent.pointerCount > 1) {
            return this.scaleGestureDetector!!.onTouchEvent(motionEvent)
        }

        // Single-finger scale gesture (when selectTool is in onProgress mode)
        val entitySelectTool = this.selectTool
        if (entitySelectTool != null && entitySelectTool.isOnProgress && this.selectTool!!.isApply_Scale && this.entity_select != null) {
            if (motionEvent.action == MotionEvent.ACTION_MOVE && this.prevDistance > 0.0f) {
                var distanceToCenter = distanceToCenter(motionEvent.x, motionEvent.y)
                if (distanceToCenter < 1.0f) {
                    distanceToCenter = 1.0f
                }
                if (this.prevDistance < 1.0f) {
                    this.prevDistance = 1.0f
                }
                val prevDist = this.prevDistance
                var scaleFactor = (distanceToCenter - prevDist) / prevDist
                if (scaleFactor > 0.5f) {
                    scaleFactor = 0.5f
                }
                if (scaleFactor < -0.5f) {
                    scaleFactor = -0.5f
                }
                this.entity_select!!.scale(scaleFactor + 1.0f, getmCanvas_width(), getmCanvas_height())
                invalidate()
                this.prevDistance = distanceToCenter
                return true
            }
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                this.prevDistance = -1.0f
                this.selectTool!!.setOnProgress(false)
                if (this.selectTool!!.isApply_Scale && this.iViewCallback != null) {
                    val entityView = this.entity_select
                    if (((entityView is QuranEntity) || (entityView is TranslationQuranEntity)) && !this.selectTool!!.isApply_all) {
                        this.selectTool!!.setApply_all(true)
                        invalidate()
                    }
                }
                return true
            }
        }

        // Reset center-line guides on ACTION_UP
        if (motionEvent.action == MotionEvent.ACTION_UP && (this.showCenterLineX || this.showCenterLineY)) {
            this.showCenterLineY = false
            this.showCenterLineX = false
            invalidate()
        }

        // Move gesture (only when not in pinch-scale mode)
        if (!this.isOnScale) {
            this.moveGestureDetector!!.onTouchEvent(motionEvent)
        }
        this.isOnScale = false

        return this.gestureDetector!!.onTouchEvent(motionEvent)
    }

    //  handleTranslate — includes snap-to-center logic with
    //  SNAP_THRESHOLD=30.0f and SNAP_FORCE=0.2f

    fun handleTranslate(pointF: PointF) {
        if (this.entity_select != null && abs(pointF.x) <= 80.0f && abs(pointF.y) <= 80.0f) {
            val rect = this.entity_select!!.rect
            val centerX = rect.centerX()
            val centerY = rect.centerY()
            val newX = centerX + pointF.x
            val newY = centerY + pointF.y
            val halfCanvasH = this.mCanvas_height / 2.0f
            var moved = false
            this.showCenterLineX = false
            this.showCenterLineY = false
            var dxAdjusted = pointF.x
            var dyAdjusted = pointF.y

            // Snap-to-center-X: if entity center is within SNAP_THRESHOLD of canvas center X
            val deltaXFromCenter = newX - (this.mCanvas_width / 2.0f)
            if (abs(deltaXFromCenter) < SNAP_THRESHOLD) {
                this.showCenterLineX = true
                dxAdjusted -= (deltaXFromCenter * SNAP_FORCE) * (1.0f - (abs(deltaXFromCenter) / SNAP_THRESHOLD))
            }

            // Snap-to-center-Y: if entity center is within SNAP_THRESHOLD of canvas center Y
            val deltaYFromCenter = newY - halfCanvasH
            if (abs(deltaYFromCenter) < SNAP_THRESHOLD) {
                this.showCenterLineY = true
                dyAdjusted -= (SNAP_FORCE * deltaYFromCenter) * (1.0f - (abs(deltaYFromCenter) / SNAP_THRESHOLD))
            }

            // Apply horizontal translation if within view bounds
            var movedX = false
            if (newX >= 0.0f && newX <= getWidth()) {
                this.entity_select!!.postTranslate(dxAdjusted, 0.0f)
                this.selectTool!!.setApply_Move(true)
                movedX = true
            }

            // Apply vertical translation if within view bounds
            var movedY = movedX
            if (newY >= 0.0f && newY <= getHeight()) {
                this.entity_select!!.postTranslate(0.0f, dyAdjusted)
                this.selectTool!!.setApply_Move(true)
                movedY = true
            }

            if (movedY) {
                invalidate()
            }
        }
    }

    //  distanceToCenter — ORIGINAL signature (float x, float y),
    //  calculates distance from (x, y) to entity_select center

    fun distanceToCenter(x: Float, y: Float): Float {
        return hypot(
            (x - this.entity_select!!.rect.centerX()).toDouble(),
            (y - this.entity_select!!.rect.centerY()).toDouble()
        ).toFloat()
    }

    //  drawLineHelper — draws snap-to-center guide lines

    fun drawLineHelper(canvas: Canvas) {
        if (this.showCenterLineX || this.showCenterLineY) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.parseColor("#80FF4081")
            paint.strokeWidth = 5.0f
            val centerX = this.mCanvas_width / 2.0f
            val canvasH = this.mCanvas_height
            val centerY = canvasH / 2.0f
            if (this.showCenterLineX) {
                canvas.drawLine(centerX, 0.0f, centerX, canvasH.toFloat(), paint)
            }
            if (this.showCenterLineY) {
                canvas.drawLine(0.0f, centerY, this.mCanvas_width.toFloat(), centerY, paint)
            }
        }
    }

    //  onDraw — delegates to BlurredRenderer.onDrawExt()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onDrawExt(canvas)
    }
}
