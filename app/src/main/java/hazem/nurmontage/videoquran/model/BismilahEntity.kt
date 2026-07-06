package hazem.nurmontage.videoquran.model

import android.animation.ObjectAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.core.common.Constants.IpadType
import hazem.nurmontage.videoquran.core.common.Constants.TransitionType
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.model.EntityView
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.Serializable
import java.lang.ref.WeakReference

class BismilahEntity : EntityView, Serializable {

    internal var bismilahTimeline: EntityBismilahTimeline? = null
    internal var clrAya: Int = 0
    internal var index: Int = 0
    internal var ipadType: Int = 0
    internal var isFadeIn: Boolean = false
    internal var isFadeOut: Boolean = false
    override var isVisible: Boolean = false
    internal var mPreset: Int = 0
    internal var objectAnimator: ObjectAnimator? = null
    internal var offsetX: Float = 0f
    internal var otherAnimation: ObjectAnimator? = null
    private val paintAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintAyaOutline: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    internal var staticLayout: StaticLayout? = null
    internal var staticLayoutOutline: StaticLayout? = null
    internal var txt: String = ""
    internal var viewWeakReference: WeakReference<TrackEntityView>? = null
    internal var viewWidth: Int = 0
    internal var weakBlurredImageView: WeakReference<BlurredImageView>? = null
    internal var xTranslation: Float = 0f
    internal var scaleX: Float = 1.0f
    internal var nameFont: String = Constants.FONT_QURAN

    constructor(txt: String, rectF: RectF, typeface: Typeface, color: Int) {
        this.txt = txt
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        isVisible = true
        viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = color
        paintAya.textSize = 0.05f
        setClrAya(color)
        maxH = (rect.height() * 0.85f).toInt()
        maxW = (rect.width() * 0.85f).toInt()
        createStaticLayout()
    }

    constructor(txt: String, rectF: RectF, typeface: Typeface, color: Int, preset: Int) {
        this.txt = txt
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        isVisible = true
        viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = color
        paintAya.textSize = 0.05f
        setClrAya(color)
        maxH = (rect.height() * 0.85f).toInt()
        maxW = (rect.width() * 0.85f).toInt()
        mPreset = preset
    }

    fun getmPreset(): Int = mPreset
    fun setmPreset(preset: Int) { mPreset = preset }

    fun setIpadType(type: Int) { ipadType = type }
    fun getIpadType(): Int = ipadType

    fun getBismilahTimeline(): EntityBismilahTimeline? = bismilahTimeline
    fun setBismilahTimeline(timeline: EntityBismilahTimeline?) { bismilahTimeline = timeline }

    fun applyAyaPreset(paint: Paint, preset: AyaTextPreset, color: Int, typeface: Typeface, textSize: Float) {
        paint.reset()
        paint.typeface = typeface
        paint.textSize = textSize
        paint.isAntiAlias = true
        paint.isSubpixelText = true
        paint.isDither = true
        paint.style = Paint.Style.FILL
        paint.color = color

        when (preset) {
            AyaTextPreset.OUTLINE -> {
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.strokeWidth = textSize * 0.06f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                val isLightFrame = ipadType == IpadType.HEART.ordinal ||
                        ipadType == IpadType.BATTERY.ordinal ||
                        ipadType == IpadType.BLUE_TYPE.ordinal
                paint.color = if (isLightFrame) {
                    hazem.nurmontage.videoquran.utils.ColorUtils.lightenColor(color, 0.85f)
                } else {
                    hazem.nurmontage.videoquran.utils.ColorUtils.darkenColor(color, 0.85f)
                }
            }
            AyaTextPreset.SHADOW -> {
                val radius = 0.18f * textSize
                val dx = textSize * 0.08f
                val isLightFrame = ipadType == IpadType.HEART.ordinal ||
                        ipadType == IpadType.BATTERY.ordinal ||
                        ipadType == IpadType.BLUE_TYPE.ordinal
                val shadowColor = ColorUtils.setAlphaComponent(
                    if (isLightFrame) -1 else ViewCompat.MEASURED_STATE_MASK, 120
                )
                paint.setShadowLayer(radius, dx, dx, shadowColor)
            }
            AyaTextPreset.GLOW -> {
                paint.setShadowLayer(textSize * 0.45f, 0f, 0f, ColorUtils.setAlphaComponent(color, 255))
            }
            AyaTextPreset.NONE -> { /* no effect */ }
        }
    }

    fun setPreset(preset: AyaTextPreset) {
        mPreset = preset.ordinal
        if (preset == AyaTextPreset.OUTLINE) {
            applyAyaPreset(paintAyaOutline, AyaTextPreset.OUTLINE, clrAya, paintAya.typeface, paintAya.textSize)
            staticLayoutOutline = getStaticLayoutOutline()
            applyAyaPreset(paintAya, AyaTextPreset.NONE, clrAya, paintAya.typeface, paintAya.textSize)
        } else {
            applyAyaPreset(paintAya, preset, clrAya, paintAya.typeface, paintAya.textSize)
        }
    }

    private fun presetFromOrdinal(ordinal: Int): AyaTextPreset = when (ordinal) {
        AyaTextPreset.SHADOW.ordinal -> AyaTextPreset.SHADOW
        AyaTextPreset.OUTLINE.ordinal -> AyaTextPreset.OUTLINE
        AyaTextPreset.GLOW.ordinal -> AyaTextPreset.GLOW
        else -> AyaTextPreset.NONE
    }

    fun initPreset(ordinal: Int) {
        mPreset = ordinal
        val preset = presetFromOrdinal(ordinal)
        if (preset == AyaTextPreset.NONE) return
        if (preset == AyaTextPreset.OUTLINE) {
            applyAyaPreset(paintAyaOutline, AyaTextPreset.OUTLINE, clrAya, paintAya.typeface, paintAya.textSize)
            staticLayoutOutline = getStaticLayoutOutline()
            applyAyaPreset(paintAya, AyaTextPreset.NONE, clrAya, paintAya.typeface, paintAya.textSize)
        } else {
            applyAyaPreset(paintAya, preset, clrAya, paintAya.typeface, paintAya.textSize)
        }
    }

    fun initPresetAya(ordinal: Int) = initPreset(ordinal)

    fun getViewWidth(): Int = viewWidth

    fun setViewWeakReference(
        trackRef: WeakReference<TrackEntityView>?,
        blurRef: WeakReference<BlurredImageView>?
    ) {
        viewWeakReference = trackRef
        weakBlurredImageView = blurRef
    }

    fun getDurationFade(): Int {
        val timeline = bismilahTimeline ?: return 0
        return ((Math.abs(timeline.rect.right / timeline.secondInScreen) -
                Math.abs(timeline.rect.left / timeline.secondInScreen)) * 0.2f * 1000f).toInt()
    }

    fun getNameFont(): String = nameFont
    fun setIndex(index: Int) { this.index = index }
    fun getIndex(): Int = index

    fun setTxt(txt: String) {
        this.txt = txt
        val spannable = SpannableString(txt)
        staticLayout = StaticLayout.Builder.obtain(
            spannable, 0, spannable.length, paintAya, viewWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    fun setClrAya(color: Int) { clrAya = color }
    fun getClrAya(): Int = clrAya
    fun getPaintAya(): TextPaint = paintAya

    fun calculateTextSize(text: String?, paint: Paint, maxWidth: Int, maxHeight: Int): Float {
        if (text.isNullOrEmpty() || maxWidth <= 0 || maxHeight <= 0) return 0f
        paint.textSize = 1.0f
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        var low = 0f
        var high = 1000f
        repeat(100) {
            val mid = (low + high) / 2f
            paint.textSize = mid
            paint.getTextBounds(text, 0, text.length, bounds)
            if (bounds.width() > maxWidth || bounds.height() > maxHeight) {
                high = mid
            } else {
                low = mid
            }
        }
        return low
    }

    fun calculateTextSize(): Float {
        val height = ((rect.height() / factorScale) * 0.85f).toInt()
        return calculateTextSize(txt, paintAya, ((rect.width() / factorScale) * 0.85f).toInt(), height)
    }

    fun setTextSize(size: Float) { paintAya.textSize = size }

    fun setTextSizeInBoucle(size: Float) {
        paintAya.textSize = size
        val spannable = SpannableString(txt)
        staticLayout = buildStaticLayout(spannable, paintAya, viewWidth)
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
    }

    fun setupScale(factor: Float, canvasW: Int, canvasH: Int) {
        paintAya.textSize = factor * canvasW
        val spannable = SpannableString(txt)
        viewWidth = maxOf(rect.width().toInt(), paintAya.measureText(spannable.toString()).toInt())
        val layout = buildStaticLayout(spannable, paintAya, viewWidth)
        staticLayout = layout
        val w = layout.width
        val halfW = w * 0.5f
        val halfH = rect.height() * (w / rect.width()) * 0.5f
        val cy = rect.centerY()
        val cx = rect.centerX()
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
    }

    fun setupScaleSave(factor: Float, canvasW: Int) {
        val f2 = canvasW.toFloat()
        paintAya.textSize = factor * f2
        val spannable = SpannableString(txt)
        viewWidth = rect.width().toInt()
        staticLayout = buildStaticLayout(spannable, paintAya, viewWidth)
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
    }

    override fun scale(factor: Float, canvasW: Int, canvasH: Int) {
        factorScale = factor
        val w = rect.width() * factor
        val h = rect.height() * factor
        val halfW = w * 0.5f
        rect.left = rect.centerX() - halfW
        rect.right = rect.centerX() + halfW
        val halfH = h * 0.5f
        rect.top = rect.centerY() - halfH
        rect.bottom = rect.centerY() + halfH
        viewWidth = rect.width().toInt()
        paintAya.textSize = calculateTextSize()
        createStaticLayout()
        setFcSize(paintAya.textSize / canvasW)
        initPreset(getmPreset())
    }

    override fun postTranslate(dx: Float, dy: Float) {
        rect.offset(dx, dy)
        val layout = staticLayout ?: return
        posX = rect.centerX() - (layout.width * 0.5f)
        posY = rect.centerY() - (layout.height * 0.5f)
    }

    fun setTranslate(cx: Float, cy: Float) {
        val halfW = rect.width() * 0.5f
        val halfH = rect.height() * 0.5f
        rect.left = cx - halfW
        rect.right = cx + halfW
        rect.top = cy - halfH
        rect.bottom = cy + halfH
        val layout = staticLayout ?: return
        posX = rect.centerX() - (layout.width * 0.5f)
        posY = rect.centerY() - (layout.height * 0.5f)
    }

    fun getWidth(): Float {
        paintAya.textSize = 3.0f
        return paintAya.measureText(txt)
    }

    fun createStaticLayout() {
        val tp = paintAya
        tp.textSize = calculateTextSize(txt, tp, (viewWidth * 0.8f).toInt(), (rect.height() * 0.8f).toInt())
        val spannable = SpannableString(txt)
        staticLayout = buildStaticLayout(spannable, paintAya, viewWidth)
        val layout = staticLayout ?: return
        posY = rect.centerY() - (layout.height * 0.5f)
        posX = rect.centerX() - (layout.width * 0.5f)
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

    private fun buildStaticLayout(spannable: SpannableString, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(spannable, 0, spannable.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

    fun setStaticLayout() {
        val spannable = SpannableString(txt)
        staticLayout = buildStaticLayout(spannable, paintAya, viewWidth)
        val layout = staticLayout ?: return
        posX = rect.centerX() - (layout.width * 0.5f)
        posY = rect.centerY() - (layout.height * 0.5f)
    }

    private fun getStaticLayoutOutline(): StaticLayout {
        val spannable = SpannableString(txt)
        return StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAyaOutline, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    fun setFadeIn(fadeIn: Boolean) { isFadeIn = fadeIn }
    fun setFadeOut(fadeOut: Boolean) { isFadeOut = fadeOut }
    fun isFadeIn(): Boolean = isFadeIn
    fun isFadeOut(): Boolean = isFadeOut

    fun isAnimRun(): Boolean {
        val a1 = objectAnimator
        val a2 = otherAnimation
        return (a1 != null && a1.isRunning) || (a2 != null && a2.isRunning)
    }

    fun setOpacityFade(alpha: Int) {
        paintAya.alpha = alpha
        paintAyaOutline.alpha = paintAya.alpha
        if (isAnimTest) {
            weakBlurredImageView?.get()?.invalidate()
        } else {
            viewWeakReference?.get()?.invalidate()
        }
    }

    override fun endAnimator() {
        try {
            objectAnimator?.takeIf { it.isRunning }?.end()
            otherAnimation?.takeIf { it.isRunning }?.end()
        } catch (_: Exception) {
        }
        objectAnimator = null
        otherAnimation = null
        setFadeIn(false)
        setFadeOut(false)
        offsetX = 0f
        paintAya.alpha = 255
        paintAyaOutline.alpha = paintAya.alpha
    }

    fun setSlideX(value: Float) {
        offsetX = value
        paintAya.alpha = ((1f - Math.abs(value)) * 255f).roundToInt()
        paintAyaOutline.alpha = paintAya.alpha
        if (isAnimTest) weakBlurredImageView?.get()?.invalidate()
    }

    fun setSlideXOut(value: Float) {
        offsetX = value
        paintAya.alpha = ((1f - Math.abs(value)) * 255f).roundToInt()
        paintAyaOutline.alpha = paintAya.alpha
        if (isAnimTest) weakBlurredImageView?.get()?.invalidate()
    }

    fun setFactorSize(factor: Float) {
        scaleX = factor
        if (isAnimTest) weakBlurredImageView?.get()?.invalidate()
    }

    fun slidToLeft(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofFloat(this, "SlideX", 1f, 0f)
        otherAnimation = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    fun slidToRightOut(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofFloat(this, "SlideXOut", 0f, 1f)
        otherAnimation = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    fun slidToLeftOut(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofFloat(this, "SlideXOut", 0f, -1f)
        otherAnimation = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    fun slidToRight(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofFloat(this, "SlideX", -1f, 0f)
        otherAnimation = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    fun zoomInIn(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofFloat(this, "FactorSize", 0f, 1f)
        otherAnimation = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    fun runIn(duration: Int, repeat: Boolean, transition: String) {
        when (transition) {
            TransitionType.SLIDE_TO_LEFT.value -> slidToLeft(duration, repeat)
            TransitionType.SLIDE_TO_RIGHT.value -> slidToRight(duration, repeat)
            TransitionType.ZOOM_IN.value -> zoomInIn(duration, repeat)
            TransitionType.FADE_IN.value -> fadeIn(duration, repeat)
        }
    }

    private fun fadeIn(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofInt(this, "OpacityFade", 0, 255)
        objectAnimator = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    private fun fadeOut(duration: Int, repeat: Boolean) {
        val anim = ObjectAnimator.ofInt(this, "OpacityFade", 255, 0)
        objectAnimator = anim
        anim.duration = duration.toLong()
        if (repeat) { anim.repeatMode = ObjectAnimator.RESTART; anim.repeatCount = -1 }
        anim.start()
    }

    fun runOut(duration: Int, repeat: Boolean, transition: String) {
        when (transition) {
            TransitionType.SLIDE_TO_LEFT.value -> slidToLeftOut(duration, repeat)
            TransitionType.SLIDE_TO_RIGHT.value -> slidToRightOut(duration, repeat)
            TransitionType.FADE_OUT.value -> fadeOut(duration, repeat)
        }
    }

    fun update(rectF: RectF, maxW: Int, maxH: Int) {
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        this.maxH = maxH
        this.maxW = maxW
        viewWidth = rect.width().toInt()
    }

    fun getStaticLayout(): StaticLayout? = staticLayout

    fun setTypeface(typeface: Typeface, fontName: String) {
        paintAya.typeface = typeface
        nameFont = fontName
    }

    fun setTypefaceOneAya(typeface: Typeface, fontName: String) {
        paintAya.typeface = typeface
        nameFont = fontName
    }

    fun setColor(color: Int) {
        setClrAya(color)
        paintAya.color = color
    }

    fun draw(canvas: Canvas) {
        val layout = staticLayout ?: return
        canvas.save()
        canvas.translate(posX + (offsetX * layout.width), posY)
        canvas.scale(scaleX, scaleX)
        if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutOutline != null) {
            paintAyaOutline.textSize = paintAya.textSize
            staticLayoutOutline!!.draw(canvas)
        }
        layout.draw(canvas)
        canvas.restore()
    }

    fun setupCanvasDraw(canvas: Canvas) {
        val layout = staticLayout ?: return
        posY = (canvas.height - layout.height) * 0.5f
        posX = (canvas.width - layout.width) * 0.5f
        canvas.save()
        canvas.translate(posX, posY)
    }

    fun restoreCanvas(canvas: Canvas) {
        try { canvas.restore() } catch (_: Exception) {}
    }

    fun singleDraw(canvas: Canvas, alpha: Int) {
        val layout = staticLayout ?: return
        paintAya.alpha = alpha
        layout.draw(canvas)
    }

    fun singleDraw(canvas: Canvas, alpha: Int, offsetFraction: Float) {
        val layout = staticLayout ?: return
        canvas.save()
        canvas.translate(offsetFraction * layout.width, 0f)
        paintAya.alpha = alpha
        layout.draw(canvas)
        canvas.restore()
    }

    fun singleDraw(canvas: Canvas) {
        val layout = staticLayout ?: return
        if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutOutline != null) {
            paintAyaOutline.textSize = paintAya.textSize
            staticLayoutOutline!!.draw(canvas)
        }
        layout.draw(canvas)
    }

    fun getX(): Float = posX
    fun getY(): Float = posY
    fun getTxt(): String = txt

    fun setUnderLine(underline: Boolean) { paintAya.isUnderlineText = underline }
}

private fun Float.roundToInt(): Int = Math.round(this)
