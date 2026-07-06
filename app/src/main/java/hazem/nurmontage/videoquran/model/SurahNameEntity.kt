package hazem.nurmontage.videoquran.model

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextDirectionHeuristics
import androidx.core.graphics.ColorUtils
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.SurahNameStyle
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import java.io.Serializable

class SurahNameEntity : EntityView, Serializable {

    companion object {
        private const val serialVersionUID = 1L
        private const val ALPHA_BG = 120  // Matches nl.dionsegijn.konfetti.core.Angle.LEFT value (120)
    }

    private val paintAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintAyaOutline: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintAyaStyle: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintAyaStyleOutline: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paintBg: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    private var alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
    var clrBg: Int = 0
        internal set
    var clrS_name: Int = 0
        private set
    override var entityQuran: EntityQuranTimeline? = null
    var index_surah: Int = 0
        internal set
    var ipad_type: Int = 0
        internal set
    var isHaveBg: Boolean = false
        internal set
    var mPreset: Int = 0
        private set
    var name: String = ""
        internal set
    var nameFont: String = "خط الإبل.otf"
        internal set
    private var name_style: String = ""
    var style: Int = 0
        private set
    private var typefaceStyle: Typeface? = null
    private var viewWidth: Int = 0
    private var x: Float = 0f
    private var y: Float = 0f
    override var isVisible: Boolean = false
    var reader: String = ""
        internal set

    private var staticLayout: StaticLayout? = null
    private var staticLayoutOutline: StaticLayout? = null
    private var staticLayoutStyle: StaticLayout? = null
    private var staticLayoutStyleOutline: StaticLayout? = null

    //  Constructors

    constructor(
        alignment: Layout.Alignment,
        name: String,
        reader: String,
        rectF: RectF,
        typeface: Typeface,
        color: Int,
        factorScale: Float,
        fontName: String,
        preset: Int,
        styleTypeface: Typeface?,
        style: Int,
        surahIndex: Int,
        ipadType: Int,
        haveBg: Boolean,
        bgColor: Int
    ) {
        this.name = name
        this.reader = reader
        this.nameFont = fontName
        this.factorScale = factorScale
        this.clrBg = bgColor
        this.isHaveBg = haveBg
        this.ipad_type = ipadType
        this.style = style
        this.index_surah = surahIndex
        setupSurahFont()
        this.typefaceStyle = styleTypeface
        paintAyaStyle.typeface = styleTypeface
        this.alignment = alignment
        this.mPreset = preset
        this.x = rectF.left
        this.y = rectF.top
        rect = rectF
        isVisible = true
        this.viewWidth = rectF.width().toInt()
        paintAya.typeface = typeface
        paintAya.color = color
        paintAyaStyle.color = paintAya.color
        paintBg.color = bgColor
        paintBg.alpha = ALPHA_BG
        setClrS_name(color)
        paintAya.textSize = 0.05f
        if (factorScale != 1.0f) {
            scale(factorScale, 1, 1)
        } else {
            createStaticLayout()
        }
    }

    constructor()

    //  Surah name style string

    fun setupSurahFont() {
        name_style = when {
            index_surah < 10 -> "00${index_surah}sura"
            index_surah < 100 -> "0${index_surah}sura"
            else -> "${index_surah}sura"
        }
    }

    //  Colour & preset

    fun setClrBg(color: Int) {
        clrBg = color
        paintBg.color = color
        paintBg.alpha = ALPHA_BG
    }

    fun setClrS_name(color: Int) { clrS_name = color }

    fun setColor(color: Int) {
        setClrS_name(color)
        paintAya.color = color
        paintAyaStyle.color = color
    }

    fun setTypeface(typeface: Typeface, fontName: String) {
        paintAya.typeface = typeface
        this.nameFont = fontName
        createStaticLayout()
    }

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

        // ZAGHRAFAT style adjustments
        if (style == SurahNameStyle.ZAGHRAFAT.ordinal) {
            if (paint === paintAyaStyleOutline) ts *= 0.5f
            if (paint === paintAyaOutline) ts *= 1.3f
        }

        when (ayaTextPreset) {
            AyaTextPreset.OUTLINE -> {
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.strokeWidth = ts * 0.12f
                paint.strokeCap = Paint.Cap.ROUND
                paint.strokeJoin = Paint.Join.ROUND
                val isLightFrame = ipad_type == IpadType.HEART.ordinal ||
                        ipad_type == IpadType.BATTERY.ordinal ||
                        ipad_type == IpadType.BLUE_TYPE.ordinal || isHaveBg
                paint.color = if (isLightFrame) {
                    hazem.nurmontage.videoquran.utils.ColorUtils.lightenColor(color, 0.85f)
                } else {
                    hazem.nurmontage.videoquran.utils.ColorUtils.darkenColor(color, 0.85f)
                }
            }
            AyaTextPreset.SHADOW -> {
                val radius = 0.18f * ts
                val dx = ts * 0.08f
                val isLightFrame = ipad_type == IpadType.HEART.ordinal ||
                        ipad_type == IpadType.BATTERY.ordinal ||
                        ipad_type == IpadType.BLUE_TYPE.ordinal || isHaveBg
                val shadowColor = ColorUtils.setAlphaComponent(
                    if (isLightFrame) -1 else android.view.View.MEASURED_STATE_MASK,
                    ALPHA_BG
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
            applyAyaPreset(paintAyaStyleOutline, AyaTextPreset.OUTLINE, clrS_name, paintAyaStyle.typeface, paintAyaStyle.textSize)
            staticLayoutStyleOutline = getStaticLayoutStyleOutline()
            applyAyaPreset(paintAyaStyle, AyaTextPreset.NONE, clrS_name, paintAyaStyle.typeface, paintAyaStyle.textSize)
            applyAyaPreset(paintAyaOutline, AyaTextPreset.OUTLINE, clrS_name, paintAya.typeface, paintAya.textSize)
            staticLayoutOutline = getStaticLayoutOutline()
            applyAyaPreset(paintAya, AyaTextPreset.NONE, clrS_name, paintAya.typeface, paintAya.textSize)
        } else {
            applyAyaPreset(paintAya, ayaTextPreset, clrS_name, paintAya.typeface, paintAya.textSize)
            updatePaintStyle()
        }
    }

    private fun updatePaintStyle() {
        if (staticLayoutStyle != null) {
            val textSize = paintAyaStyle.textSize
            paintAyaStyle.reset()
            paintAyaStyle.set(paintAya)
            paintAyaStyle.typeface = typefaceStyle
            paintAyaStyle.textSize = textSize
        }
    }

    fun get(ordinal: Int): AyaTextPreset = when (ordinal) {
        AyaTextPreset.SHADOW.ordinal -> AyaTextPreset.SHADOW
        AyaTextPreset.OUTLINE.ordinal -> AyaTextPreset.OUTLINE
        AyaTextPreset.GLOW.ordinal -> AyaTextPreset.GLOW
        else -> AyaTextPreset.NONE
    }

    fun initPreset(ordinal: Int) { setPreset(get(ordinal)) }

    //  Scale & translate

    override fun scale(factor: Float, canvasW: Int, canvasH: Int) {
        factorScale = factor
        val width = rect.width() * factor
        val height = rect.height() * factor
        val halfW = width * 0.5f
        rect.left = rect.centerX() - halfW
        rect.right = rect.centerX() + halfW
        val halfH = height * 0.5f
        rect.top = rect.centerY() - halfH
        rect.bottom = rect.centerY() + halfH
        viewWidth = rect.width().toInt()
        createStaticLayout()
        x = rect.left
    }

    override fun postTranslate(dx: Float, dy: Float) {
        rect.offset(dx, dy)
        x = rect.left
        y = if (style == SurahNameStyle.ZAGHRAFAT.ordinal && staticLayoutStyle != null) {
            rect.centerY() - (staticLayoutStyle!!.height + staticLayout!!.height) * 0.5f
        } else {
            rect.centerY() - staticLayout!!.height * 0.5f
        }
    }

    //  Text size calculation (binary search)

    fun calculateTextSize(text: String, paint: Paint, maxWidth: Int, maxHeight: Int): Float {
        var low = 0f
        var high = 1000f
        if (text.isEmpty() || maxWidth <= 0 || maxHeight <= 0) return 0f
        paint.textSize = 1.0f
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
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

    //  Static layout creation

    private fun createStaticLayout() {
        val displayText: String
        val align: Layout.Alignment

        if (style == SurahNameStyle.ZAGHRAFAT.ordinal) {
            displayText = reader
        } else {
            displayText = name
            if (reader.length > 3) {
                // Will add reader as second line below
            }
        }

        val spannable = SpannableString(
            if (style == SurahNameStyle.ZAGHRAFAT.ordinal) {
                reader
            } else {
                if (reader.length > 3) "$name\n$reader" else name
            }
        )

        if (style == SurahNameStyle.ZAGHRAFAT.ordinal) {
            // Style text (decorative surah name)
            paintAyaStyle.textSize = calculateTextSize(name_style, paintAyaStyle, (viewWidth * 0.9f).toInt(), (rect.height() * 0.5f).toInt())
            staticLayoutStyle = StaticLayout.Builder.obtain(name_style, 0, name_style.length, paintAyaStyle, viewWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, 1f)
                .setTextDirection(TextDirectionHeuristics.LTR)
                .setIncludePad(false)
                .build()
            align = Layout.Alignment.ALIGN_CENTER
            paintAya.textSize = calculateTextSize(reader, paintAya, (viewWidth * 0.9f).toInt(), (rect.height() * 0.4f).toInt())
        } else {
            align = alignment
            val longerText = if (name.length > reader.length) name else reader
            paintAya.textSize = calculateTextSize(longerText, paintAya, (viewWidth * 0.8f).toInt(), (rect.height() * 0.8f).toInt())
        }

        staticLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, paintAya, viewWidth)
            .setAlignment(align)
            .setLineSpacing(0f, 1f)
            .setTextDirection(TextDirectionHeuristics.LTR)
            .setIncludePad(false)
            .build()

        setPreset(get(mPreset))
        move()
    }

    private fun getStaticLayoutOutline(): StaticLayout {
        val text = if (style == SurahNameStyle.ZAGHRAFAT.ordinal) {
            reader
        } else {
            if (reader.length > 3) "$name\n$reader" else name
        }
        return StaticLayout.Builder.obtain(text, 0, text.length, paintAyaOutline, viewWidth)
            .setAlignment(if (style == SurahNameStyle.ZAGHRAFAT.ordinal) Layout.Alignment.ALIGN_CENTER else alignment)
            .setLineSpacing(0f, 1f)
            .setTextDirection(TextDirectionHeuristics.LTR)
            .setIncludePad(false)
            .build()
    }

    private fun getStaticLayoutStyleOutline(): StaticLayout {
        return StaticLayout.Builder.obtain(name_style, 0, name_style.length, paintAyaStyleOutline, viewWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setTextDirection(TextDirectionHeuristics.LTR)
            .setIncludePad(false)
            .build()
    }

    //  Position update

    fun move() {
        x = rect.left
        y = if (style == SurahNameStyle.ZAGHRAFAT.ordinal && staticLayoutStyle != null) {
            rect.centerY() - (staticLayoutStyle!!.height + staticLayout!!.height) * 0.5f
        } else {
            rect.centerY() - (staticLayout?.height ?: 0) * 0.5f
        }
    }

    fun setNameAndReader(alignment: Layout.Alignment, name: String, reader: String) {
        this.name = name
        this.reader = reader
        this.alignment = alignment
        paintAya.textSize = 0.05f
        createStaticLayout()
    }

    fun setStyle(context: android.content.Context, style: Int, reader: String, haveBg: Boolean) {
        if (typefaceStyle == null) {
            typefaceStyle = Typeface.createFromAsset(context.resources.assets, "fonts/surah_name.otf")
        }
        isHaveBg = haveBg
        paintAyaStyle.typeface = typefaceStyle
        this.style = style
        this.reader = reader
        paintAya.textSize = 0.05f
        createStaticLayout()
    }

    override var rect: RectF = RectF()
        set(value) {
            field = value
            y = value.top
            x = value.left
        }

    fun setAlignment(alignment: Layout.Alignment) { this.alignment = alignment }

    fun update(rectF: RectF) {
        rect = rectF
        y = rectF.top
        x = rectF.left
        viewWidth = rectF.width().toInt()
        paintAya.textSize = 0.05f
        createStaticLayout()
    }

    //  Drawing

    fun draw(canvas: Canvas) {
        val layout = staticLayout ?: return

        // Background
        if (isHaveBg) {
            if (style == SurahNameStyle.ZAGHRAFAT.ordinal && staticLayoutStyle != null) {
                val bgHeight = (layout.height + staticLayoutStyle!!.height * 0.93f) * 0.5f
                canvas.drawRect(0f, rect.centerY() - bgHeight, canvas.width.toFloat(), rect.centerY() + bgHeight, paintBg)
            } else {
                canvas.drawRect(0f, rect.top, canvas.width.toFloat(), rect.bottom, paintBg)
            }
        }

        canvas.save()
        canvas.translate(x, y)

        // ZAGHRAFAT style: decorative name above reader
        if (style == SurahNameStyle.ZAGHRAFAT.ordinal && staticLayoutStyle != null) {
            if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutStyleOutline != null) {
                paintAyaStyleOutline.textSize = paintAyaStyle.textSize
                staticLayoutStyleOutline!!.draw(canvas)
            }
            staticLayoutStyle!!.draw(canvas)
            canvas.translate(0f, staticLayoutStyle!!.height * 0.93f)
        }

        // Main text with optional outline
        if (mPreset == AyaTextPreset.OUTLINE.ordinal && staticLayoutOutline != null) {
            paintAyaOutline.textSize = paintAya.textSize
            staticLayoutOutline!!.draw(canvas)
        }
        layout.draw(canvas)
        canvas.restore()
    }

    fun singleDraw(canvas: Canvas) {
        createStaticLayout()
        staticLayout?.draw(canvas)
    }

    //  EntityView overrides

    override fun endAnimator() { /* No animations in this entity */ }

    // entityQuran and isVisible are already overridden as properties

    fun getX(): Float = x
    fun getY(): Float = y
    fun getStaticLayout(): StaticLayout? = staticLayout
    fun getPaintAya(): TextPaint = paintAya

    fun getmPreset(): Int = mPreset
}
