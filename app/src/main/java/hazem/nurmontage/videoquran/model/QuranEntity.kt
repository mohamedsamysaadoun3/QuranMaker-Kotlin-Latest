package hazem.nurmontage.videoquran.model

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.VectorDrawable
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.core.common.Constants.FONT_QURAN
import hazem.nurmontage.videoquran.core.common.Constants.IpadType
import hazem.nurmontage.videoquran.core.common.Constants.TransitionType
import hazem.nurmontage.videoquran.model.EntityView
import hazem.nurmontage.videoquran.utils.ColorUtils.darkenColor
import hazem.nurmontage.videoquran.utils.ColorUtils.lightenColor
import hazem.nurmontage.videoquran.utils.EndOfAyaSpan
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.Serializable
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

class QuranEntity : EntityView, Serializable {

    //  Serial version UID for Serializable
    companion object {
        private const val serialVersionUID = 1L
    }

    //  State fields (names preserved for serialization)
    internal var clrAya: Int = 0
    internal var clrTrsl: Int = 0
    internal var complete_aya: String? = null
    internal var endWord_index: Int = 0
    internal var icon: String? = null
    internal var index: Int = 0
    internal var indexNumber: Int = 0
    internal var ipad_type: Int = 0
    internal var isFadeIn: Boolean = false
    internal var isFadeOut: Boolean = false
    override var isVisible: Boolean = false
    internal var mPreset: Int = 0
    internal var nameFont: String = FONT_QURAN
    internal var number: Int = 0
    internal var objectAnimator: ObjectAnimator? = null
    internal var offsetX: Float = 0f
    internal var otherAnimation: ObjectAnimator? = null
    private val paintAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintAyaOutline: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintAyaTrslOutline: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    internal val paintTranslationAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    internal var spannableString: SpannableString? = null
    internal var startWord_index: Int = 0
    internal var staticLayout: StaticLayout? = null
    internal var staticLayoutOutline: StaticLayout? = null
    internal var staticLayoutTranslation: StaticLayout? = null
    internal var staticLayoutTranslationOutline: StaticLayout? = null
    internal var translation: String? = null
    internal var translation_complete: String? = null
    internal var txt: String? = null
    private val typefaceNumber: Typeface?
    internal var vectorDrawable: VectorDrawable? = null
    internal var viewWeakReference: WeakReference<TrackEntityView>? = null
    internal var viewWidth: Int = 0
    internal var weakBlurredImageView: WeakReference<BlurredImageView>? = null
    internal var x_translation: Float = 0f
    internal var padding: Float = 1.0f
    internal var scaleX: Float = 1.0f

    //  Preset accessors

    fun getmPreset(): Int = mPreset

    fun setmPreset(preset: Int) {
        mPreset = preset
    }

    fun setIpad_type(type: Int) {
        ipad_type = type
    }

    fun getIpad_type(): Int = ipad_type

    //  Aya preset application

    fun applyAyaPreset(
        paint: Paint,
        ayaTextPreset: AyaTextPreset,
        color: Int,
        typeface: Typeface,
        textSize: Float
    ) {
        var ts = textSize
        paint.reset()
        paint.typeface = typeface
        paint.textSize = ts
        paint.isAntiAlias = true
        paint.isSubpixelText = true
        paint.isDither = true
        paint.style = Paint.Style.FILL
        paint.color = color

        if (paint === paintAyaTrslOutline) {
            ts *= 1.35f
        }

        when (ayaTextPreset) {
            AyaTextPreset.OUTLINE -> {
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.strokeWidth = ts * 0.12f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                val isLightFrame = ipad_type == IpadType.HEART.ordinal ||
                        ipad_type == IpadType.BATTERY.ordinal ||
                        ipad_type == IpadType.BLUE_TYPE.ordinal
                paint.color = if (isLightFrame) lightenColor(color, 0.85f) else darkenColor(color, 0.85f)
            }
            AyaTextPreset.SHADOW -> {
                val radius = 0.18f * ts
                val dx = ts * 0.08f
                val isLightFrame = ipad_type == IpadType.HEART.ordinal ||
                        ipad_type == IpadType.BATTERY.ordinal ||
                        ipad_type == IpadType.BLUE_TYPE.ordinal
                val shadowColor = ColorUtils.setAlphaComponent(
                    if (isLightFrame) -1 else ViewCompat.MEASURED_STATE_MASK, 120
                )
                paint.setShadowLayer(radius, dx, dx, shadowColor)
            }
            AyaTextPreset.GLOW -> {
                paint.setShadowLayer(ts * 0.45f, 0f, 0f, ColorUtils.setAlphaComponent(color, 255))
            }
            AyaTextPreset.NONE -> { /* no effect */ }
        }
    }

    fun setPreset(ayaTextPreset: AyaTextPreset) {
        mPreset = ayaTextPreset.ordinal
        if (ayaTextPreset == AyaTextPreset.OUTLINE) {
            applyAyaPreset(paintAyaOutline, AyaTextPreset.OUTLINE, clrAya, paintAya.typeface, paintAya.textSize)
            staticLayoutOutline = getStaticLayoutOutline()
            applyAyaPreset(paintAya, AyaTextPreset.NONE, clrAya, paintAya.typeface, paintAya.textSize)
            if (isTrsl()) {
                applyAyaPreset(paintAyaTrslOutline, AyaTextPreset.OUTLINE, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
                staticLayoutTranslationOutline = buildStaticLayout(translation!!, paintAyaTrslOutline, staticLayoutTranslation!!.width)
                applyAyaPreset(paintTranslationAya, AyaTextPreset.NONE, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
            }
            return
        }
        applyAyaPreset(paintAya, ayaTextPreset, clrAya, paintAya.typeface, paintAya.textSize)
        if (isTrsl()) {
            applyAyaPreset(paintTranslationAya, ayaTextPreset, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
        }
    }

    fun presetFromOrdinal(ordinal: Int): AyaTextPreset = when (ordinal) {
        AyaTextPreset.SHADOW.ordinal -> AyaTextPreset.SHADOW
        AyaTextPreset.OUTLINE.ordinal -> AyaTextPreset.OUTLINE
        AyaTextPreset.GLOW.ordinal -> AyaTextPreset.GLOW
        else -> AyaTextPreset.NONE
    }

    fun get(ordinal: Int): AyaTextPreset = presetFromOrdinal(ordinal)

    fun initPreset(ordinal: Int) {
        mPreset = ordinal
        val ayaTextPreset = presetFromOrdinal(ordinal)
        if (ayaTextPreset == AyaTextPreset.NONE) return
        if (ayaTextPreset == AyaTextPreset.OUTLINE) {
            applyAyaPreset(paintAyaOutline, AyaTextPreset.OUTLINE, clrAya, paintAya.typeface, paintAya.textSize)
            staticLayoutOutline = getStaticLayoutOutline()
            applyAyaPreset(paintAya, AyaTextPreset.NONE, clrAya, paintAya.typeface, paintAya.textSize)
            if (isTrsl()) {
                applyAyaPreset(paintAyaTrslOutline, AyaTextPreset.OUTLINE, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
                staticLayoutTranslationOutline = buildStaticLayout(translation!!, paintAyaTrslOutline, staticLayoutTranslation!!.width)
                applyAyaPreset(paintTranslationAya, AyaTextPreset.NONE, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
            }
            return
        }
        applyAyaPreset(paintAya, ayaTextPreset, clrAya, paintAya.typeface, paintAya.textSize)
        if (!isTrsl() || staticLayoutTranslation == null) return
        applyAyaPreset(paintTranslationAya, ayaTextPreset, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
    }

    fun initPresetAya(ordinal: Int) {
        mPreset = ordinal
        val ayaTextPreset = presetFromOrdinal(ordinal)
        if (ayaTextPreset == AyaTextPreset.NONE) return
        if (ayaTextPreset == AyaTextPreset.OUTLINE) {
            applyAyaPreset(paintAyaOutline, AyaTextPreset.OUTLINE, clrAya, paintAya.typeface, paintAya.textSize)
            staticLayoutOutline = getStaticLayoutOutline()
            applyAyaPreset(paintAya, AyaTextPreset.NONE, clrAya, paintAya.typeface, paintAya.textSize)
        } else {
            applyAyaPreset(paintAya, ayaTextPreset, clrAya, paintAya.typeface, paintAya.textSize)
        }
    }

    fun initPresetTrsl(ordinal: Int) {
        mPreset = ordinal
        val ayaTextPreset = presetFromOrdinal(ordinal)
        if (ayaTextPreset == AyaTextPreset.NONE) return
        if (ayaTextPreset == AyaTextPreset.OUTLINE) {
            if (isTrsl()) {
                applyAyaPreset(paintAyaTrslOutline, AyaTextPreset.OUTLINE, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
                staticLayoutTranslationOutline = buildStaticLayout(translation!!, paintAyaTrslOutline, staticLayoutTranslation!!.width)
                applyAyaPreset(paintTranslationAya, AyaTextPreset.NONE, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
            }
            return
        }
        if (!isTrsl() || staticLayoutTranslation == null) return
        applyAyaPreset(paintTranslationAya, ayaTextPreset, clrTrsl, paintTranslationAya.typeface, paintTranslationAya.textSize)
    }

    //  Simple property accessors

    fun setVectorDrawable(vectorDrawable: VectorDrawable?) {
        this.vectorDrawable = vectorDrawable
    }

    fun getVectorDrawable(): VectorDrawable? = vectorDrawable

    fun setTranslation(translation: String?) {
        this.translation = translation
    }

    fun getTranslation(): String? = translation

    fun getTypefaceNumber(): Typeface? = typefaceNumber

    fun setTranslation_complete(translationComplete: String?) {
        translation_complete = translationComplete
    }

    fun getTranslation_complete(): String? = translation_complete

    fun setStartWord_index(startWordIndex: Int) {
        startWord_index = startWordIndex
    }

    fun setEndWord_index(endWordIndex: Int) {
        endWord_index = endWordIndex
    }

    fun getStartWord_index(): Int = startWord_index

    fun getEndWord_index(): Int = endWord_index

    fun setIcon(icon: String?) {
        this.icon = icon
    }

    fun getIcon(): String? = icon

    fun getViewWidth(): Int = viewWidth

    fun setViewWeakReference(
        trackRef: WeakReference<TrackEntityView>?,
        blurRef: WeakReference<BlurredImageView>?
    ) {
        viewWeakReference = trackRef
        weakBlurredImageView = blurRef
    }

    fun getDuration_fade(): Int {
        val quran = entityQuran!!
        return ((kotlin.math.abs(quran.rect.right / quran.secondInScreen) -
                kotlin.math.abs(quran.rect.left / quran.secondInScreen)) * 0.2f * 1000f).toInt()
    }

    fun getNameFont(): String = nameFont

    fun setIndex(index: Int) {
        this.index = index
    }

    fun getIndex(): Int = index

    //  Text & color

    fun setTxt(str: String) {
        txt = str
        val spannable = SpannableString(str)
        if (number != -1) {
            try {
                val length = str.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        if (isTrsl()) {
            staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, staticLayoutTranslation!!.width)
            posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
            x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
        }
    }

    fun setClrAya(color: Int) { clrAya = color }
    fun setClrTrsl(color: Int) { clrTrsl = color }
    fun getClrAya(): Int = clrAya
    fun getClrTrsl(): Int = clrTrsl
    fun getPaintAya(): TextPaint = paintAya
    fun getPaintTranslationAya(): TextPaint = paintTranslationAya

    //  Text-size calculation (binary search)

    fun calculateTextSize(text: String?, paint: Paint, maxWidth: Int, maxHeight: Int): Float {
        var low = 0f
        if (text.isNullOrEmpty() || maxWidth <= 0 || maxHeight <= 0) return low
        paint.textSize = 1.0f
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
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

    fun calculateOptimalTextSize(text: String, maxWidth: Int, maxHeight: Int, textPaint: TextPaint): Float {
        var low = 5f
        var high = 1000f
        var result = 5f
        while (low <= high) {
            val mid = (low + high) / 2f
            textPaint.textSize = mid
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setIncludePad(false)
                .build()
            val height = layout.height
            val maxLineWidth = getMaxLineWidth(layout)
            if (height > maxHeight || maxLineWidth > maxWidth) {
                high = mid - 0.03f
            } else {
                result = mid
                low = 0.03f + mid
            }
        }
        return result
    }

    private fun getMaxLineWidth(layout: StaticLayout): Float {
        var max = 0f
        for (i in 0 until layout.lineCount) {
            max = kotlin.math.max(max, layout.getLineWidth(i))
        }
        return max
    }

    fun calculateOptimalTextSize(maxWidth: Int, maxHeight: Int): Float {
        val trsl = translation ?: return 0f
        return calculateOptimalTextSize(trsl, maxWidth, maxHeight, paintTranslationAya)
    }

    private fun isTrsl(): Boolean {
        return translation != null &&
                ipad_type != IpadType.CASSET.ordinal &&
                ipad_type != IpadType.CASSET_IMG.ordinal &&
                ipad_type != IpadType.BOTTOM_RECT.ordinal &&
                ipad_type != IpadType.CASSET_IMG_BLUR.ordinal
    }

    fun calculateTextSize(): Float {
        return calculateTextSize(
            txt, paintAya,
            ((rect.width() / factorScale) * 0.85f).toInt(),
            (((if (isTrsl()) rect.height() * 0.5f else rect.height()) / factorScale) * 0.85f).toInt()
        )
    }

    fun setTextSize(size: Float) {
        paintAya.textSize = size
    }

    fun setTextSizeInBoucle(size: Float) {
        paintAya.textSize = size
        val spannable = SpannableString(txt!!)
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
    }

    //  Translation update

    fun updateTranslation(size: Float) {
        if (!isTrsl()) return
        paintTranslationAya.textSize = size
        var width = (viewWidth * 0.9f).toInt()
        staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, width)
        val step = width * 0.17f
        val minWidth = staticLayout!!.width * 0.4f
        val lastLine = staticLayoutTranslation!!.lineCount - 1
        while (true) {
            if (translation!!.substring(
                    staticLayoutTranslation!!.getLineStart(lastLine),
                    staticLayoutTranslation!!.getLineEnd(lastLine)
                ).trim().split("\\s+".toRegex()).size >= 2) {
                break
            }
            width = (width - step).toInt()
            if (width < minWidth) {
                paintTranslationAya.textSize = size
                staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, viewWidth)
                break
            }
            staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, width)
        }
        x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
        posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
    }

    fun updateTranslationSave(size: Float) {
        if (translation == null ||
            ipad_type == IpadType.CASSET.ordinal ||
            ipad_type == IpadType.CASSET_IMG.ordinal ||
            ipad_type == IpadType.BOTTOM_RECT.ordinal ||
            ipad_type == IpadType.CASSET_IMG_BLUR.ordinal
        ) return
        paintTranslationAya.textSize = size
        staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, (viewWidth * 0.9f).toInt())
        if (staticLayout!!.width > rect.width()) {
            val half = staticLayout!!.width * 0.5f
            rect.left = rect.centerX() - half
            rect.right = rect.centerX() + half
        }
        if (staticLayoutTranslation!!.width > rect.width()) {
            val half = staticLayoutTranslation!!.width * 0.5f
            rect.left = rect.centerX() - half
            rect.right = rect.centerX() + half
        }
        if (staticLayout!!.height > rect.height()) {
            val half = staticLayout!!.height * 0.5f
            rect.top = rect.centerY() - half
            rect.bottom = rect.centerY() + half
        }
        if (staticLayoutTranslation!!.height > rect.height()) {
            val half = staticLayoutTranslation!!.height * 0.5f
            rect.top = rect.centerY() - half
            rect.bottom = rect.centerY() + half
        }
    }

    //  Scale & setup

    fun setupScale(factor: Float, canvasW: Int, canvasH: Int) {
        paintAya.textSize = factor * canvasW
        val spannable = SpannableString(txt!!)
        var hasTrsl = false
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewWidth = maxOf(rect.width().toInt(), paintAya.measureText(spannable.toString()).roundToInt())
        val layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        staticLayout = layout
        val w = layout.width
        val halfW = w * 0.5f
        val halfH = rect.height() * (w / rect.width()) * 0.5f
        val cy = rect.centerY()
        val cx = rect.centerX()
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        if (translation != null && staticLayoutTranslation != null) {
            hasTrsl = true
        }
        when {
            hasTrsl && txt!!.length < 20 -> {
                maxH = (rect.height() * 0.55f).roundToInt()
                maxW = (rect.width() * 0.55f).roundToInt()
            }
            hasTrsl && canvasW > canvasH -> {
                maxH = (rect.height() * 0.64f).roundToInt()
                maxW = (rect.width() * 0.64f).roundToInt()
            }
            hasTrsl && canvasW == canvasH -> {
                maxH = (rect.height() * 0.8f).roundToInt()
                maxW = (rect.width() * 0.8f).roundToInt()
            }
            else -> {
                maxH = (rect.height() * 0.85f).roundToInt()
                maxW = (rect.width() * 0.85f).roundToInt()
            }
        }
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
    }

    fun setupScaleSave(factor: Float, canvasW: Int) {
        val f2 = canvasW.toFloat()
        paintAya.textSize = factor * f2
        val spannable = SpannableString(txt!!)
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewWidth = maxOf(rect.width().toInt(), paintAya.measureText(spannable.toString()).roundToInt())
        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        if (translation != null) {
            updateTranslationSave(factorSizeTrl * f2)
            if (staticLayoutTranslation != null) {
                x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
                posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
                return
            }
        }
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
        val hasTrsl = translation != null && staticLayoutTranslation != null
        viewWidth = rect.width().toInt()
        paintAya.textSize = calculateTextSize()
        createStaticLayout()
        val f4 = canvasW.toFloat()
        setFcSize(paintAya.textSize / f4)
        if (hasTrsl) {
            setTls()
            factorSizeTrl = paintTranslationAya.textSize / f4
        }
        initPreset(getmPreset())
    }

    private fun setTls() {
        updateTranslation(calculateOptimalTextSize((rect.width() * 0.85f).toInt(), (rect.height() * 0.5f * 0.83f).toInt()))
    }

    //  Apply-all helpers (batch position + size)

    fun applyAll(canvasW: Int, rectF: RectF, textSize: Float, fcSize: Float, quranEntity: QuranEntity) {
        paintAya.textSize = textSize
        val spannable = SpannableString(txt!!)
        var hasTrsl = false
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewWidth = (maxOf(rectF.width().toInt(), paintAya.measureText(spannable.toString()).roundToInt()) * 1.1f).toInt()
        val layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        staticLayout = layout
        val w = layout.width
        setFcSize(fcSize)
        val halfW = w * 0.5f
        val halfH = rect.height() * (w / rect.width()) * 0.5f
        rect.set(rectF.centerX() - halfW, rectF.centerY() - halfH, rectF.centerX() + halfW, rectF.centerY() + halfH)
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        if (translation != null && staticLayoutTranslation != null) {
            hasTrsl = true
        }
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
        if (hasTrsl) {
            paintTranslationAya.textSize = quranEntity.getPaintTranslationAya().textSize
            staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, (viewWidth * 0.9f).toInt())
            factorSizeTrl = paintTranslationAya.textSize / canvasW
        }
        initPreset(getmPreset())
    }

    fun applyAll(canvasW: Int, rectF: RectF, textSize: Float, fcSize: Float) {
        paintAya.textSize = textSize
        val spannable = SpannableString(txt!!)
        var hasTrsl = false
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewWidth = (maxOf(rectF.width().toInt(), paintAya.measureText(spannable.toString()).roundToInt()) * 1.1f).toInt()
        val layout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        staticLayout = layout
        val w = layout.width
        setFcSize(fcSize)
        val halfW = w * 0.5f
        val halfH = rect.height() * (w / rect.width()) * 0.5f
        rect.set(rectF.centerX() - halfW, rectF.centerY() - halfH, rectF.centerX() + halfW, rectF.centerY() + halfH)
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        if (translation != null && staticLayoutTranslation != null) {
            hasTrsl = true
        }
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
        if (hasTrsl) {
            updateTranslation(calculateOptimalTextSize((rect.width() * 0.85f).toInt(), (rect.height() * 0.5f * 0.83f).toInt()))
            factorSizeTrl = paintTranslationAya.textSize / canvasW
        }
        initPreset(getmPreset())
    }

    //  Translate

    override fun postTranslate(dx: Float, dy: Float) {
        rect.offset(dx, dy)
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        if (isTrsl() && staticLayoutTranslation != null) {
            x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
            posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
        } else {
            posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        }
    }

    fun setTranslate(cx: Float, cy: Float) {
        val halfW = rect.width() * 0.5f
        val halfH = rect.height() * 0.5f
        rect.left = cx - halfW
        rect.right = cx + halfW
        rect.top = cy - halfH
        rect.bottom = cy + halfH
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        if (translation != null && staticLayoutTranslation != null) {
            x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
            posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
        } else {
            posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        }
    }

    //  Width measurement

    fun getWidth(): Float {
        paintAya.textSize = 3.0f
        return paintAya.measureText(txt)
    }

    fun getTranslationWidth(): Float {
        paintTranslationAya.textSize = 3.0f
        return paintTranslationAya.measureText(translation)
    }

    //  Static layout creation

    fun createStaticLayout() {
        val spannable = SpannableString(txt!!)
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        val ipType = ipad_type
        if (translation != null &&
            ipType != IpadType.BOTTOM_RECT.ordinal &&
            ipType != IpadType.CASSET.ordinal &&
            ipType != IpadType.CASSET_IMG.ordinal &&
            ipType != IpadType.CASSET_IMG_BLUR.ordinal
        ) {
            val spacingFactor: Float = when (ipType) {
                IpadType.IPAD_NEOMORPHIC.ordinal -> 2.0f
                IpadType.MASK_BRUSH.ordinal,
                IpadType.BLUE_TYPE.ordinal,
                IpadType.HEART.ordinal,
                IpadType.BATTERY.ordinal,
                IpadType.BLACK_LAYER.ordinal,
                IpadType.GRADIENT.ordinal -> 0.45f
                else -> 1.2f
            }
            posX = rect.centerX() - (staticLayout!!.width * 0.5f)
            paintTranslationAya.textSize = calculateOptimalTextSize(
                translation!!,
                (viewWidth * 0.85f).toInt(),
                (rect.height() - (staticLayout!!.height * spacingFactor)).toInt(),
                paintTranslationAya
            )
            var width = viewWidth
            val trslLayout = buildStaticLayout(translation!!, paintTranslationAya, width)
            staticLayoutTranslation = trslLayout
            if (trslLayout.lineCount == 2) {
                val lastLine = staticLayoutTranslation!!.lineCount - 1
                while (translation!!.substring(
                        staticLayoutTranslation!!.getLineStart(lastLine),
                        staticLayoutTranslation!!.getLineEnd(lastLine)
                    ).trim().split("\\s+".toRegex()).size < 3 && width - 10 >= 10
                ) {
                    staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, width)
                }
                x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
            } else {
                x_translation = posX
            }
            posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
        } else {
            posY = rect.centerY() - (staticLayout!!.height * 0.5f)
        }
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

    fun setStaticLayout() {
        val spannable = SpannableString(txt!!)
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        posX = rect.centerX() - (staticLayout!!.width * 0.5f)
        val ipType = ipad_type
        if (translation != null &&
            ipType != IpadType.BOTTOM_RECT.ordinal &&
            ipType != IpadType.CASSET.ordinal &&
            ipType != IpadType.CASSET_IMG.ordinal &&
            ipType != IpadType.CASSET_IMG_BLUR.ordinal
        ) {
            var width = viewWidth
            val trslLayout = buildStaticLayout(translation!!, paintTranslationAya, width)
            staticLayoutTranslation = trslLayout
            if (trslLayout.lineCount == 2) {
                val lastLine = staticLayoutTranslation!!.lineCount - 1
                while (translation!!.substring(
                        staticLayoutTranslation!!.getLineStart(lastLine),
                        staticLayoutTranslation!!.getLineEnd(lastLine)
                    ).trim().split("\\s+".toRegex()).size < 3 && width - 10 >= 10
                ) {
                    staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, width)
                }
                x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
            } else {
                x_translation = posX
            }
            posY = rect.centerY() - ((staticLayout!!.height + staticLayoutTranslation!!.height) * 0.5f)
            return
        }
        posY = rect.centerY() - (staticLayout!!.height * 0.5f)
    }

    fun updateIconDraw() {
        val spannable = SpannableString(txt!!)
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun getStaticLayoutOutline(): StaticLayout {
        val spannable = SpannableString(txt!!)
        if (number != -1) {
            try {
                val length = txt!!.length
                if (length > indexNumber) {
                    spannable.setSpan(
                        EndOfAyaSpan(vectorDrawable, typefaceNumber, number.toString()),
                        indexNumber, length, 0
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAyaOutline, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    fun updateStaticLayout() {
        val spannable = spannableString
        staticLayout = StaticLayout.Builder.obtain(spannable!!, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        val ipType = ipad_type
        if (translation == null ||
            ipType == IpadType.BOTTOM_RECT.ordinal ||
            ipType == IpadType.CASSET.ordinal ||
            ipType == IpadType.CASSET_IMG.ordinal ||
            ipType == IpadType.CASSET_IMG_BLUR.ordinal
        ) return
        var width = viewWidth
        val trslLayout = buildStaticLayout(translation!!, paintTranslationAya, width)
        staticLayoutTranslation = trslLayout
        if (trslLayout.lineCount == 2) {
            val lastLine = staticLayoutTranslation!!.lineCount - 1
            while (translation!!.substring(
                    staticLayoutTranslation!!.getLineStart(lastLine),
                    staticLayoutTranslation!!.getLineEnd(lastLine)
                ).trim().split("\\s+".toRegex()).size < 3 && width - 10 >= 10
            ) {
                staticLayoutTranslation = buildStaticLayout(translation!!, paintTranslationAya, width)
            }
            x_translation = rect.centerX() - (staticLayoutTranslation!!.width * 0.5f)
            return
        }
        x_translation = posX
    }

    //  Fade / animation state

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
        paintTranslationAya.alpha = paintAya.alpha
        paintAyaTrslOutline.alpha = paintAya.alpha
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
            objectAnimator = null
            otherAnimation = null
        } catch (_: Exception) {
        }
        setFadeIn(false)
        setFadeOut(false)
        offsetX = 0f
        paintAya.alpha = 255
        paintTranslationAya.alpha = paintAya.alpha
        paintAyaTrslOutline.alpha = paintAya.alpha
        paintAyaOutline.alpha = paintAya.alpha
    }

    //  ObjectAnimator property setters

    fun setSlideX(value: Float) {
        offsetX = value
        val alpha = ((1f - kotlin.math.abs(value)) * 255f).roundToInt()
        paintAya.alpha = alpha
        paintTranslationAya.alpha = paintAya.alpha
        paintAyaTrslOutline.alpha = paintAya.alpha
        paintAyaOutline.alpha = paintAya.alpha
        if (isAnimTest) {
            weakBlurredImageView?.get()?.invalidate()
        }
    }

    fun setSlideXOut(value: Float) {
        offsetX = value
        val alpha = ((1f - kotlin.math.abs(value)) * 255f).roundToInt()
        paintAya.alpha = alpha
        paintTranslationAya.alpha = paintAya.alpha
        paintAyaTrslOutline.alpha = paintAya.alpha
        paintAyaOutline.alpha = paintAya.alpha
        if (isAnimTest) {
            weakBlurredImageView?.get()?.invalidate()
        }
    }

    fun setFactorSize(factor: Float) {
        scaleX = factor
        if (isAnimTest) {
            weakBlurredImageView?.get()?.invalidate()
        }
    }

    //  Transition animations

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

    fun zoomIn_In(duration: Int, repeat: Boolean) {
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
            TransitionType.ZOOM_IN.value -> zoomIn_In(duration, repeat)
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

    //  Index / number accessors

    fun getNumber(): Int = number
    fun setNumber(number: Int) { this.number = number }

    fun getIndexNumber(): Int = indexNumber
    fun setIndexNumber(indexNumber: Int) { this.indexNumber = indexNumber }

    //  Update helpers

    fun update(rectF: RectF, maxW: Int, maxH: Int) {
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        this.maxH = maxH
        this.maxW = maxW
        viewWidth = rect.width().toInt()
    }

    // maxH and maxW are already properties from EntityView

    fun getStaticLayout(): StaticLayout? = staticLayout

    //  Typeface / color setters

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

    fun setColorTranslation(color: Int) {
        setClrTrsl(color)
        paintTranslationAya.color = color
    }

    //  Drawing

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

        val ipType = ipad_type
        if (translation == null || staticLayoutTranslation == null ||
            ipType == IpadType.BOTTOM_RECT.ordinal ||
            ipType == IpadType.CASSET.ordinal ||
            ipType == IpadType.CASSET_IMG.ordinal ||
            ipType == IpadType.CASSET_IMG_BLUR.ordinal
        ) return

        canvas.save()
        canvas.translate(x_translation + (offsetX * layout.width), posY + (layout.height * padding))
        canvas.scale(scaleX, scaleX)
        if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutTranslationOutline != null) {
            paintAyaTrslOutline.textSize = paintTranslationAya.textSize
            staticLayoutTranslationOutline!!.draw(canvas)
        }
        staticLayoutTranslation!!.draw(canvas)
        canvas.restore()
    }

    fun setupCanvasDraw(canvas: Canvas) {
        val ipType = ipad_type
        if (translation != null && staticLayoutTranslation != null &&
            ipType != IpadType.BOTTOM_RECT.ordinal &&
            ipType != IpadType.CASSET.ordinal &&
            ipType != IpadType.CASSET_IMG.ordinal &&
            ipType != IpadType.CASSET_IMG_BLUR.ordinal
        ) {
            posY = ((canvas.height - staticLayout!!.height - staticLayoutTranslation!!.height) * 0.5f)
            x_translation = (canvas.width - staticLayoutTranslation!!.width) * 0.5f
        } else {
            posY = (canvas.height - staticLayout!!.height) * 0.5f
        }
        posX = (canvas.width - staticLayout!!.width) * 0.5f
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
        val ipType = ipad_type
        if (translation == null || staticLayoutTranslation == null ||
            ipType == IpadType.BOTTOM_RECT.ordinal ||
            ipType == IpadType.CASSET.ordinal ||
            ipType == IpadType.CASSET_IMG.ordinal ||
            ipType == IpadType.CASSET_IMG_BLUR.ordinal
        ) return
        canvas.save()
        canvas.translate((-posX) + x_translation, staticLayout!!.height * padding)
        paintTranslationAya.alpha = alpha
        staticLayoutTranslation!!.draw(canvas)
        canvas.restore()
    }

    fun singleDraw(canvas: Canvas, alpha: Int, offsetFraction: Float) {
        val layout = staticLayout ?: return
        canvas.save()
        canvas.translate(layout.width * offsetFraction, 0f)
        paintAya.alpha = alpha
        layout.draw(canvas)
        canvas.restore()
        val ipType = ipad_type
        if (translation == null || staticLayoutTranslation == null ||
            ipType == IpadType.BOTTOM_RECT.ordinal ||
            ipType == IpadType.CASSET.ordinal ||
            ipType == IpadType.CASSET_IMG.ordinal ||
            ipType == IpadType.CASSET_IMG_BLUR.ordinal
        ) return
        canvas.save()
        canvas.translate((-posX) + x_translation, staticLayout!!.height * padding)
        canvas.translate(offsetFraction * layout.width, 0f)
        paintTranslationAya.alpha = alpha
        staticLayoutTranslation!!.draw(canvas)
        canvas.restore()
    }

    fun singleDraw(canvas: Canvas) {
        val layout = staticLayout ?: return
        if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutOutline != null) {
            paintAyaOutline.textSize = paintAya.textSize
            staticLayoutOutline!!.draw(canvas)
        }
        layout.draw(canvas)
        val ipType = ipad_type
        if (translation == null || staticLayoutTranslation == null ||
            ipType == IpadType.BOTTOM_RECT.ordinal ||
            ipType == IpadType.CASSET.ordinal ||
            ipType == IpadType.CASSET_IMG.ordinal ||
            ipType == IpadType.CASSET_IMG_BLUR.ordinal
        ) return
        canvas.save()
        canvas.translate((-posX) + x_translation, staticLayout!!.height * padding)
        if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutTranslationOutline != null) {
            paintAyaTrslOutline.textSize = paintTranslationAya.textSize
            staticLayoutTranslationOutline!!.draw(canvas)
        }
        staticLayoutTranslation!!.draw(canvas)
        canvas.restore()
    }

    //  Visibility & accessors

    // isVisible is already overridden as property above

    fun getX(): Float = posX
    fun getY(): Float = posY
    fun getTxt(): String? = txt
    fun getComplete_aya(): String? = complete_aya

    // isAnimTest is already a property from EntityView

    fun setUnderLine(underline: Boolean) { paintAya.isUnderlineText = underline }

    //  Constructors

    constructor(
        context: Context,
        txt: String,
        complete_aya: String,
        translation: String,
        translation_complete: String,
        rectF: RectF,
        typeface: Typeface,
        typeface2: Typeface,
        indexNumber: Int,
        number: Int,
        typeface3: Typeface,
        clrAya: Int,
        clrTrsl: Int,
        nameFont: String,
        visible: Boolean,
        drawableResId: Int
    ) {
        this.nameFont = FONT_QURAN
        this.txt = txt
        this.complete_aya = complete_aya
        if (complete_aya.isEmpty()) {
            this.complete_aya = txt
        }
        this.translation = translation
        this.translation_complete = translation_complete
        this.nameFont = nameFont
        this.indexNumber = indexNumber
        this.number = number
        this.typefaceNumber = typeface3
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        isVisible = true
        viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = clrAya
        paintAya.textSize = 0.05f
        paintTranslationAya.textSize = 0.05f
        paintTranslationAya.color = clrTrsl
        paintTranslationAya.typeface = typeface2
        setClrAya(clrAya)
        setClrTrsl(clrTrsl)
        this.vectorDrawable = ContextCompat.getDrawable(context, drawableResId) as VectorDrawable
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
    }

    constructor(
        context: Context,
        drawableResId: Int,
        txt: String,
        complete_aya: String,
        translation: String,
        translation_complete: String,
        rectF: RectF,
        typeface: Typeface,
        typeface2: Typeface,
        indexNumber: Int,
        number: Int,
        typeface3: Typeface,
        clrAya: Int,
        clrTrsl: Int,
        nameFont: String,
        visible: Boolean
    ) {
        this.nameFont = FONT_QURAN
        this.txt = txt
        this.translation = translation
        this.translation_complete = translation_complete
        this.complete_aya = complete_aya
        if (complete_aya.isEmpty()) {
            this.complete_aya = txt
        }
        this.nameFont = nameFont
        this.indexNumber = indexNumber
        this.number = number
        this.typefaceNumber = typeface3
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        isVisible = true
        viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = clrAya
        paintAya.textSize = 0.05f
        paintTranslationAya.textSize = 0.05f
        paintTranslationAya.color = clrTrsl
        paintTranslationAya.typeface = typeface2
        setClrAya(clrAya)
        setClrTrsl(clrTrsl)
        this.vectorDrawable = ContextCompat.getDrawable(context, drawableResId) as VectorDrawable
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
    }

    constructor(
        txt: String,
        complete_aya: String,
        translation: String,
        translation_complete: String,
        rectF: RectF,
        typeface: Typeface,
        typeface2: Typeface,
        indexNumber: Int,
        number: Int,
        typeface3: Typeface,
        clrAya: Int,
        clrTrsl: Int,
        nameFont: String,
        ayaTextSize: Float,
        visible: Boolean,
        vectorDrawable: VectorDrawable
    ) {
        this.nameFont = FONT_QURAN
        this.txt = txt
        this.translation = translation
        this.translation_complete = translation_complete
        this.complete_aya = complete_aya
        this.nameFont = nameFont
        this.indexNumber = indexNumber
        this.number = number
        this.typefaceNumber = typeface3
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        isVisible = true
        viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = clrAya
        paintAya.textSize = ayaTextSize
        paintTranslationAya.textSize = 0.05f
        paintTranslationAya.typeface = typeface2
        paintTranslationAya.color = clrTrsl
        this.vectorDrawable = vectorDrawable
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
    }

    constructor(
        txt: String,
        complete_aya: String,
        translation: String,
        translation_complete: String,
        rectF: RectF,
        typeface: Typeface,
        typeface2: Typeface,
        indexNumber: Int,
        number: Int,
        typeface3: Typeface,
        clrAya: Int,
        clrTrsl: Int,
        nameFont: String,
        ayaTextSize: Float,
        trslTextSize: Float,
        visible: Boolean,
        vectorDrawable: VectorDrawable
    ) {
        this.nameFont = FONT_QURAN
        this.txt = txt
        this.translation = translation
        this.translation_complete = translation_complete
        this.complete_aya = complete_aya
        this.nameFont = nameFont
        this.indexNumber = indexNumber
        this.number = number
        this.typefaceNumber = typeface3
        rect = RectF(rectF.left, rectF.top, rectF.right, rectF.bottom)
        isVisible = true
        viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = clrAya
        paintAya.textSize = ayaTextSize
        paintTranslationAya.textSize = trslTextSize
        paintTranslationAya.typeface = typeface2
        paintTranslationAya.color = clrTrsl
        this.vectorDrawable = vectorDrawable
        maxH = (rect.height() * 0.85f).roundToInt()
        maxW = (rect.width() * 0.85f).roundToInt()
    }
}
