package hazem.nurmontage.videoquran.ui.engine

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaPlayer
import android.util.Log
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.model.Transition
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.utils.DrawableHelper
import hazem.nurmontage.videoquran.utils.TimeFormatter
import hazem.nurmontage.videoquran.utils.UtilsFileLast
import hazem.nurmontage.videoquran.utils.animator.SmoothTimelineAnimator
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.round

/**
 * TimelineEngine
 *
 * Consolidates timeline playback/animation and entity (layer) management
 * into a single class. In the original EngineActivity these responsibilities
 * were split across two large extension-function files:
 *
 *   - **EngineTimelineManager.kt** — timeline animation, seeking, time display,
 *     cursor position, frame processing, and playback control.
 *   - **EngineEntityManager.kt** — adding / duplicating / splitting Quran entities,
 *     translation entities, Bismilah/Isti3adah entities, and their corresponding
 *     timeline entries.
 *
 * This class provides method signatures that map 1-to-1 to the original
 * extension functions, with full implementations moved from EngineActivity.
 *
 * @param context          Android context (for resource / asset access)
 * @param blurredImageView The canvas view that renders entities
 * @param trackViewEntity  The timeline track view that manages entity blocks
 * @param template         The project template with configuration
 * @param callbacks        UI callback interface for state changes
 */
class TimelineEngine(
    private val context: Context,
    private val blurredImageView: BlurredImageView,
    private val trackViewEntity: TrackEntityView,
    private val template: Template,
    private val callbacks: Callbacks = Callbacks.EMPTY
) {

    // ──────────────────────────────────────────────
    //  Callbacks interface
    // ──────────────────────────────────────────────

    /**
     * Callback interface for UI updates that the engine cannot perform directly.
     * The host (typically EngineActivity) implements these to bridge the gap
     * between the engine and the UI layer.
     */
    interface Callbacks {
        /** Called when the start-time label needs updating. */
        fun onUpdateStartViewTime(positionMs: Int) {}
        /** Called when the end-time label needs updating. */
        fun onUpdateEndViewTime(positionMs: Int) {}
        /** Called when both start and end time labels need updating. */
        fun onUpdateViewTime(maxTimeMs: Int, currentMs: Int) {}
        /** Called when play/pause state changes. */
        fun onPlayStateChanged(isPlaying: Boolean) {}
        /** Called when an entity is selected on the timeline. */
        fun onSelectEntity(entity: Entity, delta: Float) {}
        /** Called when the timeline needs invalidation. */
        fun onInvalidate() {}
        /** Called to update cut button state. */
        fun onUpdateBtnCutState() {}
        /** Called to update the "to end" button state. */
        fun onUpdateBtnToEnd() {}
        /** Called to update the "to start" button state. */
        fun onUpdateBtnToStart() {}
        /** Called when starting the square-video animator. */
        fun onStartSquareVideoAnimator() {}
        /** Called when a frame path is ready for processing. */
        fun onProcessFrame(framePath: String) {}

        companion object {
            /** No-op implementation for convenience. */
            val EMPTY = object : Callbacks {}
        }
    }

    // ──────────────────────────────────────────────
    //  State
    // ──────────────────────────────────────────────

    /**
     * Immutable snapshot of the timeline's current state.
     *
     * @property currentCursor Current cursor position in milliseconds
     * @property maxTime       Total timeline duration in milliseconds
     * @property scaleFactor   Horizontal zoom factor for the timeline track
     * @property isPlaying     Whether the timeline is currently animating
     */
    data class TimelineState(
        val currentCursor: Int = 0,
        val maxTime: Int = 0,
        val scaleFactor: Float = 0.5f,
        val isPlaying: Boolean = false
    )

    /** Current timeline state. */
    var state = TimelineState(scaleFactor = template.scale_timeline)
        private set

    // ──────────────────────────────────────────────
    //  Internal state
    // ──────────────────────────────────────────────

    private var timeFormatter: TimeFormatter? = null
    private var valueAnimator: SmoothTimelineAnimator? = null
    private var startCursor: Int = 0

    // Audio playback tracking during animation
    private var entityAudioVisible: EntityAudio? = null
    private var entityAudioPlayer: EntityAudio? = null
    private var lastIndexVisible: Int = 0
    private var endTimeAudioVisible: Int = 0
    private var mPlayer: MediaPlayer? = null

    // ──────────────────────────────────────────────
    //  Convenience accessors
    // ──────────────────────────────────────────────

    /** Whether the template uses a gradient/mask/black-layer iPad type. */
    private val isGradientBg: Boolean
        get() = template.ipad_type == IpadType.GRADIENT.ordinal ||
                template.ipad_type == IpadType.MASK_BRUSH.ordinal ||
                template.ipad_type == IpadType.BLACK_LAYER.ordinal

    // ══════════════════════════════════════════════════════════════════
    //  Entity management (from EngineEntityManager.kt)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Add a Quran ayah entity to the timeline (simplified overload).
     *
     * Creates a [QuranEntity], adds it to the blurredImageView, and creates
     * an [EntityQuranTimeline] at the current cursor position.
     *
     * @param ayaText       Short ayah text
     * @param completeAya   Full ayah text
     * @param translation   Short translation
     * @param translationComplete Full translation
     * @param indexNumber   Ayah index number
     * @param number        Ayah number
     * @param nameFont      Font filename (e.g. "hafes")
     * @param startWordIdx  Start word highlight index
     * @param endWordIdx    End word highlight index
     */
    fun addEntity(
        ayaText: String, completeAya: String, translation: String, translationComplete: String,
        indexNumber: Int, number: Int, nameFont: String, startWordIdx: Int, endWordIdx: Int
    ) {
        val isGradientBg = this.isGradientBg
        val resolvedFont = if (blurredImageView.getQuranEntities().isEmpty()) {
            "arabic/Quran.ttf"
        } else {
            blurredImageView.getQuranEntities()[0].nameFont
        }
        val quranEntity = QuranEntity(
            context,
            DrawableHelper.getIDDrawableIconByName(nameFont),
            ayaText, completeAya, translation, translationComplete,
            blurredImageView.getRectFAya()!!,
            UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/$resolvedFont")!!,
            Typeface.createFromAsset(context.resources.assets, "fonts/ReadexPro_Medium.ttf")!!,
            indexNumber, number,
            UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/خط فارس الكوفي.otf")!!,
            blurredImageView.clr_aya,
            blurredImageView.clr_trsl,
            resolvedFont, isGradientBg
        )
        quranEntity.ipad_type = template.ipad_type
        quranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        quranEntity.startWord_index = startWordIdx
        quranEntity.endWord_index = endWordIdx
        quranEntity.icon = nameFont
        quranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        val timelineEntity = addTimeLineQuran(quranEntity)
        timelineEntity.scaleFactor = trackViewEntity.scaleFactor
        quranEntity.entityQuran = timelineEntity
        timelineEntity.setEntityView(quranEntity)
        blurredImageView.addEntity(quranEntity)
    }

    /**
     * Add a Quran ayah entity to the timeline (full overload with layout params).
     *
     * Used when restoring from a template — provides explicit left/right positions,
     * transition, scale factors, custom Typeface objects, and rect overrides.
     */
    fun addEntity(
        ayaText: String, completeAya: String, translation: String, translationComplete: String,
        left: Float, right: Float, indexNumber: Int, number: Int, color: Int,
        nameFont: String, transition: Transition, isGradientBg: Boolean,
        icon: String?, startWordIdx: Int, endWordIdx: Int,
        scale: Float, factorSize: Float, factorSizeTrl: Float,
        rectF: RectF?, typeface: Typeface, typeface2: Typeface,
        colorTrsl: Int, preset: Int
    ) {
        val resolvedIcon = icon ?: "hafes"
        val loadedFont = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/$nameFont")
        val resolvedRect = if (rectF == null) {
            blurredImageView.getRectFAya()!!
        } else {
            RectF(
                rectF.left * blurredImageView.getmCanvas_width(),
                rectF.top * blurredImageView.getmCanvas_height(),
                rectF.right * blurredImageView.getmCanvas_width(),
                rectF.bottom * blurredImageView.getmCanvas_height()
            )
        }
        val quranEntity = QuranEntity(
            context, ayaText, completeAya, translation, translationComplete,
            resolvedRect, loadedFont!!, typeface2, indexNumber, number,
            typeface, color, colorTrsl, nameFont, isGradientBg,
            DrawableHelper.getIDDrawableIconByName(resolvedIcon)
        )
        quranEntity.setFcSize(factorSize)
        quranEntity.factorSizeTrl = factorSizeTrl
        quranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        quranEntity.setFactor_scale(scale)
        quranEntity.ipad_type = template.ipad_type
        quranEntity.startWord_index = startWordIdx
        quranEntity.endWord_index = endWordIdx
        quranEntity.icon = resolvedIcon
        quranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        if (quranEntity.factorSize == 1.0f) {
            quranEntity.setTextSize(quranEntity.calculateTextSize())
        } else {
            quranEntity.setupScaleSave(quranEntity.factorSize, blurredImageView.getmCanvas_width())
        }
        quranEntity.initPreset(preset)
        val timelineEntity = addTimeLineQuranWithPosition(quranEntity, left, right)
        quranEntity.entityQuran = timelineEntity
        timelineEntity.setTransition(transition)
        timelineEntity.setEntityView(quranEntity)
        blurredImageView.addEntity(quranEntity)
    }

    /**
     * Add a translation entity to the timeline.
     *
     * Creates a [TranslationQuranEntity] at the current cursor position.
     *
     * @param text   Translation text
     * @param color  Text color
     * @param isRtl  Whether text is right-to-left
     */
    fun addTranslationEntity(text: String, color: Int, isRtl: Boolean) {
        @Suppress("MagicNumber")
        val defaultColor = 0xffffff00.toInt() // InputDeviceCompat.SOURCE_ANY used as default color
        val translationQuranEntity = TranslationQuranEntity(
            text, blurredImageView.getRectFAya()!!,
            Typeface.createFromAsset(context.resources.assets, "fonts/ReadexPro_Medium.ttf")!!,
            color, defaultColor, "ReadexPro_Medium.ttf",
            blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height()
        )
        translationQuranEntity.ipad_type = template.ipad_type
        translationQuranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        translationQuranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        val timelineEntity = addTimeLineTrslQuran(translationQuranEntity)
        timelineEntity.scaleFactor = trackViewEntity.scaleFactor
        translationQuranEntity.entityTrslTimeline = timelineEntity
        timelineEntity.setEntityView(translationQuranEntity)
        blurredImageView.addEntity(translationQuranEntity)
    }

    /**
     * Add a translation entity with explicit timeline position (from template).
     */
    fun addEntityTrsl(
        text: String, left: Float, right: Float, number: Int, color: Int,
        nameFont: String, transition: Transition, scale: Float, factorSize: Float,
        rectF: RectF?, preset: Int, clrBg: Int, isHaveBg: Boolean
    ) {
        val loadedFont = UtilsFileLast.loadFontFromAsset(context, "fonts/$nameFont")
        val resolvedRect = if (rectF == null) {
            blurredImageView.getRectFAya()!!
        } else {
            RectF(
                rectF.left * blurredImageView.getmCanvas_width(),
                rectF.top * blurredImageView.getmCanvas_height(),
                rectF.right * blurredImageView.getmCanvas_width(),
                rectF.bottom * blurredImageView.getmCanvas_height()
            )
        }
        val translationQuranEntity = TranslationQuranEntity(
            blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height(),
            text, resolvedRect, loadedFont!!, number, color, nameFont
        )
        translationQuranEntity.setHaveBg(isHaveBg)
        translationQuranEntity.clrBg = clrBg
        translationQuranEntity.setFcSize(factorSize)
        translationQuranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        translationQuranEntity.setFactor_scale(scale)
        translationQuranEntity.ipad_type = template.ipad_type
        translationQuranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        if (translationQuranEntity.factorSize == 1.0f) {
            translationQuranEntity.setTextSize(translationQuranEntity.calculateTextSize())
        } else {
            translationQuranEntity.setupScaleSave(translationQuranEntity.factorSize, blurredImageView.getmCanvas_width())
        }
        translationQuranEntity.initPreset(preset)
        val timelineEntity = addTimeLineTrslWithPosition(translationQuranEntity, left, right)
        translationQuranEntity.entityTrslTimeline = timelineEntity
        timelineEntity.setTransition(transition)
        timelineEntity.setEntityView(translationQuranEntity)
        blurredImageView.addEntity(translationQuranEntity)
    }

    /**
     * Add a Bismilah entity with explicit timeline position.
     */
    fun addEntityBismilah(
        text: String, left: Float, right: Float, color: Int,
        transition: Transition, scale: Float, factorSize: Float,
        rectF: RectF?, preset: Int
    ) {
        val loadedFont = UtilsFileLast.loadFontFromAsset(context, "fonts/خط البسملة.ttf")
        val resolvedRect = if (rectF == null) {
            blurredImageView.getRectFAya()!!
        } else {
            RectF(
                rectF.left * blurredImageView.getmCanvas_width(),
                rectF.top * blurredImageView.getmCanvas_height(),
                rectF.right * blurredImageView.getmCanvas_width(),
                rectF.bottom * blurredImageView.getmCanvas_height()
            )
        }
        val bismilahEntity = BismilahEntity(text, resolvedRect, loadedFont!!, color, preset)
        bismilahEntity.setFcSize(factorSize)
        bismilahEntity.setFactor_scale(scale)
        bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        if (bismilahEntity.factorSize == 1.0f) {
            bismilahEntity.createStaticLayout()
        } else {
            bismilahEntity.setupScaleSave(bismilahEntity.factorSize, blurredImageView.getmCanvas_width())
        }
        bismilahEntity.initPreset(preset)
        bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        val timelineEntity = addTimeLineBismilahWithPosition(bismilahEntity, left, right)
        bismilahEntity.bismilahTimeline = timelineEntity
        timelineEntity.setTransition(transition)
        timelineEntity.setEntityView(bismilahEntity)
        blurredImageView.addBismilahEntity(bismilahEntity)
    }

    /**
     * Add a Bismilah entity at the default cursor position.
     *
     * @return true if a new entity was created; false if one already existed (made visible instead)
     */
    fun addEntityBismilah(): Boolean {
        val existing = blurredImageView.getBismilahEntity()
        if (existing != null) {
            if (existing.bismilahTimeline?.visible() == true) {
                return false
            }
            existing.bismilahTimeline?.visible(true)
            return false
        }
        val bismilahEntity = BismilahEntity(
            "1", blurredImageView.getRectFAya()!!,
            UtilsFileLast.loadFontFromAsset(context, "fonts/خط البسملة.ttf")!!,
            blurredImageView.clr_aya
        )
        bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        bismilahEntity.setFcSize(bismilahEntity.getPaintAya().textSize / blurredImageView.getmCanvas_width())
        bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        val timelineEntity = addTimeLineBismilah(bismilahEntity)
        timelineEntity.scaleFactor = trackViewEntity.scaleFactor
        bismilahEntity.bismilahTimeline = timelineEntity
        timelineEntity.setEntityView(bismilahEntity)
        blurredImageView.addBismilahEntity(bismilahEntity)
        if (trackViewEntity.getQuran() != null) {
            trackViewEntity.translateToRightBismilah(timelineEntity)
        }
        return true
    }

    /**
     * Add an Isti3adah (audhu-billah) entity with explicit timeline position.
     */
    fun addEntityIsti3ada(
        text: String, left: Float, right: Float, color: Int,
        transition: Transition, scale: Float, factorSize: Float,
        rectF: RectF?, preset: Int
    ) {
        val loadedFont = UtilsFileLast.loadFontFromAsset(context, "fonts/خط الاستعاذه.ttf")
        val resolvedRect = if (rectF == null) {
            blurredImageView.getRectFAya()!!
        } else {
            RectF(
                rectF.left * blurredImageView.getmCanvas_width(),
                rectF.top * blurredImageView.getmCanvas_height(),
                rectF.right * blurredImageView.getmCanvas_width(),
                rectF.bottom * blurredImageView.getmCanvas_height()
            )
        }
        val bismilahEntity = BismilahEntity(text, resolvedRect, loadedFont!!, color, preset)
        bismilahEntity.setFcSize(factorSize)
        bismilahEntity.setFactor_scale(scale)
        bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        if (bismilahEntity.factorSize == 1.0f) {
            bismilahEntity.createStaticLayout()
        } else {
            bismilahEntity.setupScaleSave(bismilahEntity.factorSize, blurredImageView.getmCanvas_width())
        }
        bismilahEntity.initPreset(preset)
        bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        val timelineEntity = addTimeLineIsti3adaWithPosition(bismilahEntity, left, right)
        bismilahEntity.bismilahTimeline = timelineEntity
        timelineEntity.setTransition(transition)
        timelineEntity.setEntityView(bismilahEntity)
        blurredImageView.addIsti3adhaEntity(bismilahEntity)
    }

    /**
     * Add an Isti3adah entity at the default cursor position.
     *
     * @return true if created; false if already exists (made visible)
     */
    fun addEntityIste3adha(): Boolean {
        val existing = blurredImageView.getmIsti3adhaEntity()
        if (existing != null) {
            if (existing.bismilahTimeline?.visible() == true) {
                return false
            }
            existing.bismilahTimeline?.visible(true)
            return false
        }
        val bismilahEntity = BismilahEntity(
            "4", blurredImageView.getRectFAya()!!,
            UtilsFileLast.loadFontFromAsset(context, "fonts/خط الاستعاذه.ttf")!!,
            blurredImageView.clr_aya
        )
        bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        bismilahEntity.setFcSize(bismilahEntity.getPaintAya().textSize / blurredImageView.getmCanvas_width())
        bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        val timelineEntity = addTimeLineIsti3ada(bismilahEntity)
        timelineEntity.scaleFactor = trackViewEntity.scaleFactor
        bismilahEntity.bismilahTimeline = timelineEntity
        timelineEntity.setEntityView(bismilahEntity)
        blurredImageView.addIsti3adhaEntity(bismilahEntity)
        if (trackViewEntity.getQuran() != null) {
            trackViewEntity.translateToRightBismilah(timelineEntity)
        }
        return true
    }

    /**
     * Duplicate a Quran entity and insert it after the original.
     *
     * Creates a deep copy, adds a new timeline entry right after the original,
     * and selects the new entity.
     *
     * @param quranEntity The entity to duplicate
     */
    fun duplicateEntity(quranEntity: QuranEntity) {
        var typefaceNumber = quranEntity.getTypefaceNumber()
        if (typefaceNumber == null) {
            typefaceNumber = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/خط فارس الكوفي.otf")!!
        }
        val resolvedNumberTypeface = typefaceNumber
        var resolvedAyaTypeface = quranEntity.getPaintAya().typeface
        if (resolvedAyaTypeface == null) {
            resolvedAyaTypeface = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/${quranEntity.nameFont}")!!
        }
        val ayaTypeface = resolvedAyaTypeface
        var resolvedTrslTypeface: Typeface? = quranEntity.getPaintTranslationAya()?.typeface
        if (resolvedTrslTypeface == null) {
            resolvedTrslTypeface = Typeface.createFromAsset(context.resources.assets, "fonts/ReadexPro_Medium.ttf")!!
        }

        val duplicate = QuranEntity(
            quranEntity.txt!!, quranEntity.getComplete_aya()!!,
            quranEntity.translation!!, quranEntity.translation_complete!!,
            blurredImageView.getRectFAya()!!, ayaTypeface, resolvedTrslTypeface,
            quranEntity.getIndexNumber(), quranEntity.getNumber(), resolvedNumberTypeface,
            quranEntity.clrAya, quranEntity.clrTrsl, quranEntity.nameFont!!,
            quranEntity.getPaintAya().textSize,
            quranEntity.getPaintTranslationAya()?.textSize ?: 0f,
            quranEntity.getPaintAya().isUnderlineText,
            quranEntity.getVectorDrawable()!!
        )
        duplicate.setFcSize(quranEntity.factorSize)
        duplicate.factorSizeTrl = quranEntity.factorSizeTrl
        duplicate.setFactor_scale(quranEntity.getFactor_scale())
        duplicate.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        duplicate.ipad_type = template.ipad_type
        duplicate.startWord_index = quranEntity.startWord_index
        duplicate.endWord_index = quranEntity.endWord_index
        duplicate.icon = quranEntity.icon
        duplicate.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        duplicate.isVisible = false
        duplicate.setupScaleSave(duplicate.factorSize, blurredImageView.getmCanvas_width())
        duplicate.setColor(quranEntity.clrAya)
        @Suppress("MagicNumber")
        val trslColor = if (quranEntity.getPaintTranslationAya() != null) quranEntity.clrTrsl else 0xffffff00.toInt()
        duplicate.setColorTranslation(trslColor)
        duplicate.initPreset(quranEntity.getmPreset())

        val originalTimeline = quranEntity.entityQuran!!
        val timelineEntity = addTimeLineQuranAtIndex(
            originalTimeline.index + 1, duplicate,
            originalTimeline.rect.right,
            originalTimeline.rect.right + originalTimeline.rect.width()
        )
        timelineEntity.scaleFactor = originalTimeline.scaleFactor
        duplicate.entityQuran = timelineEntity
        timelineEntity.setEntityView(duplicate)
        if (originalTimeline.getTransition() != null) {
            timelineEntity.setTransition(originalTimeline.getTransition()!!.duplicate())
        }
        blurredImageView.addEntity(duplicate, quranEntity.index + 1)
        trackViewEntity.selectEntity(duplicate.entityQuran, false)
        callbacks.onSelectEntity(duplicate.entityQuran!!, -1.0f)
        trackViewEntity.updateCursurToSelectEntity()
    }

    /**
     * Duplicate a translation entity and insert it after the original.
     *
     * Handles splitting the in/out transitions so original and copy each get one.
     *
     * @param translationQuranEntity The translation entity to duplicate
     */
    fun duplicateEntity(translationQuranEntity: TranslationQuranEntity) {
        var resolvedTypeface = translationQuranEntity.getPaintAya().typeface
        if (resolvedTypeface == null) {
            resolvedTypeface = UtilsFileLast.loadFontFromAsset(context, "fonts/${translationQuranEntity.nameFont}")
        }

        val duplicate = TranslationQuranEntity(
            translationQuranEntity.txt!!, translationQuranEntity.rect,
            resolvedTypeface, translationQuranEntity.getNumber(),
            translationQuranEntity.clrAya, translationQuranEntity.nameFont!!,
            translationQuranEntity.getPaintAya().textSize
        )
        duplicate.setFcSize(translationQuranEntity.factorSize)
        duplicate.factorSizeTrl = translationQuranEntity.factorSizeTrl
        duplicate.setFactor_scale(translationQuranEntity.getFactor_scale())
        duplicate.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
        duplicate.ipad_type = template.ipad_type
        duplicate.isVisible = false
        duplicate.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
        duplicate.updatePaint(
            translationQuranEntity.getPaintAya().textSize,
            translationQuranEntity.getStaticLayout()!!.width
        )
        duplicate.setColor(translationQuranEntity.clrAya)
        duplicate.initPreset(translationQuranEntity.getmPreset())

        val originalTimeline = translationQuranEntity.entityTrslTimeline!!
        val timelineEntity = addTimeLineTrslQuranAtIndex(
            originalTimeline.index + 1, duplicate,
            originalTimeline.rect.right,
            originalTimeline.rect.right + originalTimeline.rect.width()
        )
        val transition = originalTimeline.getTransition()
        if (transition != null) {
            timelineEntity.setTransition(transition.duplicate())
            if (transition.isIn && transition.isOut) {
                timelineEntity.getTransition()!!.isIn = false
                transition.isOut = false
            } else if (transition.isIn) {
                timelineEntity.getTransition()!!.isIn = false
            } else if (transition.isOut) {
                transition.isOut = false
            }
        }
        timelineEntity.scaleFactor = originalTimeline.scaleFactor
        duplicate.entityTrslTimeline = timelineEntity
        timelineEntity.setEntityView(duplicate)
        if (originalTimeline.getTransition() != null) {
            timelineEntity.setTransition(originalTimeline.getTransition()!!.duplicate())
        }
        blurredImageView.addEntity(duplicate, translationQuranEntity.index + 1)
        trackViewEntity.selectEntity(duplicate.entityTrslTimeline, false)
        callbacks.onSelectEntity(duplicate.entityTrslTimeline!!, -1.0f)
        trackViewEntity.updateCursurToSelectEntity()
    }

    /**
     * Split a Quran entity at the current cursor position.
     *
     * The entity is split into two parts at the cursor. The right part is
     * inserted as a new entity. Has a 20%-edge guard to prevent tiny splits.
     *
     * @param quranEntity The entity to split
     */
    fun splitEntity(quranEntity: QuranEntity) {
        val cursorAbs = abs(trackViewEntity.getXCursur())
        val entityTimeline = quranEntity.entityQuran!!
        if (cursorAbs <= entityTimeline.rect.left || cursorAbs >= entityTimeline.rect.right) {
            return
        }
        val edgeGuard = trackViewEntity.getSecond_in_screen() * 0.2f
        if (cursorAbs <= entityTimeline.rect.left ||
            cursorAbs >= entityTimeline.rect.left + edgeGuard
        ) {
            if (cursorAbs >= entityTimeline.rect.right ||
                cursorAbs <= entityTimeline.rect.right - edgeGuard
            ) {
                // Create the split entity
                var typefaceNumber = quranEntity.getTypefaceNumber()
                if (typefaceNumber == null) {
                    typefaceNumber = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/خط فارس الكوفي.otf")!!
                }
                val resolvedNumberTypeface = typefaceNumber
                var resolvedAyaTypeface = quranEntity.getPaintAya().typeface
                if (resolvedAyaTypeface == null) {
                    resolvedAyaTypeface = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/${quranEntity.nameFont}")!!
                }
                val ayaTypeface = resolvedAyaTypeface
                var resolvedTrslTypeface: Typeface? = quranEntity.getPaintTranslationAya()?.typeface
                if (resolvedTrslTypeface == null) {
                    resolvedTrslTypeface = Typeface.createFromAsset(context.resources.assets, "fonts/ReadexPro_Medium.ttf")!!
                }

                val splitEntity = QuranEntity(
                    quranEntity.txt!!, quranEntity.getComplete_aya()!!,
                    quranEntity.translation!!, quranEntity.translation_complete!!,
                    blurredImageView.getRectFAya()!!, ayaTypeface, resolvedTrslTypeface,
                    quranEntity.getIndexNumber(), quranEntity.getNumber(), resolvedNumberTypeface,
                    quranEntity.clrAya, quranEntity.clrTrsl, quranEntity.nameFont!!,
                    quranEntity.getPaintAya().textSize,
                    quranEntity.getPaintTranslationAya()?.textSize ?: 0f,
                    quranEntity.getPaintAya().isUnderlineText,
                    quranEntity.getVectorDrawable()!!
                )
                splitEntity.setFcSize(quranEntity.factorSize)
                splitEntity.factorSizeTrl = quranEntity.factorSizeTrl
                splitEntity.setFactor_scale(quranEntity.getFactor_scale())
                splitEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
                splitEntity.ipad_type = template.ipad_type
                splitEntity.startWord_index = quranEntity.startWord_index
                splitEntity.endWord_index = quranEntity.endWord_index
                splitEntity.icon = quranEntity.icon
                splitEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
                splitEntity.setupScaleSave(splitEntity.factorSize, blurredImageView.getmCanvas_width())
                splitEntity.setColor(quranEntity.clrAya)
                @Suppress("MagicNumber")
                val trslColor = if (quranEntity.getPaintTranslationAya() != null) quranEntity.clrTrsl else 0xffffff00.toInt()
                splitEntity.setColorTranslation(trslColor)
                splitEntity.initPreset(quranEntity.getmPreset())

                trackViewEntity.stackSplit(entityTimeline)
                val splitTimeline = splitTimeLineQuran(
                    entityTimeline.index + 1, splitEntity,
                    abs(trackViewEntity.getCurrentPosition()),
                    entityTimeline.rect.right,
                    entityTimeline.scaleFactor
                )
                val transition = entityTimeline.getTransition()
                if (transition != null) {
                    splitTimeline.setTransition(transition.duplicate())
                    if (transition.isIn && transition.isOut) {
                        splitTimeline.getTransition()!!.isIn = false
                        transition.isOut = false
                    } else if (transition.isIn) {
                        splitTimeline.getTransition()!!.isIn = false
                    } else if (transition.isOut) {
                        transition.isOut = false
                    }
                }
                entityTimeline.setCurrentRect()
                entityTimeline.right = abs(trackViewEntity.getCurrentPosition())
                entityTimeline.onChange()
                splitEntity.entityQuran = splitTimeline
                splitTimeline.setEntityView(splitEntity)
                if (entityTimeline.getTransition() != null) {
                    splitTimeline.setTransition(entityTimeline.getTransition()!!.duplicate())
                }
                blurredImageView.addEntity(splitEntity, quranEntity.index + 1)
                trackViewEntity.invalidate()
            }
        }
    }

    /**
     * Split a translation entity at the current cursor position.
     *
     * @param translationQuranEntity The translation entity to split
     */
    fun splitEntity(translationQuranEntity: TranslationQuranEntity) {
        val cursorAbs = abs(trackViewEntity.getXCursur())
        val entityTimeline = translationQuranEntity.entityTrslTimeline!!
        if (cursorAbs <= entityTimeline.rect.left || cursorAbs >= entityTimeline.rect.right) {
            return
        }
        val edgeGuard = trackViewEntity.getSecond_in_screen() * 0.2f
        if (cursorAbs <= entityTimeline.rect.left ||
            cursorAbs >= entityTimeline.rect.left + edgeGuard
        ) {
            if (cursorAbs >= entityTimeline.rect.right ||
                cursorAbs <= entityTimeline.rect.right - edgeGuard
            ) {
                var resolvedTypeface = translationQuranEntity.getPaintAya().typeface
                if (resolvedTypeface == null) {
                    resolvedTypeface = UtilsFileLast.loadFontFromAsset(context, "fonts/${translationQuranEntity.nameFont}")
                }

                val splitEntity = TranslationQuranEntity(
                    translationQuranEntity.txt!!, translationQuranEntity.rect,
                    resolvedTypeface, translationQuranEntity.getNumber(),
                    translationQuranEntity.clrAya, translationQuranEntity.nameFont!!,
                    translationQuranEntity.getPaintAya().textSize
                )
                splitEntity.setFcSize(translationQuranEntity.factorSize)
                splitEntity.factorSizeTrl = translationQuranEntity.factorSizeTrl
                splitEntity.setFactor_scale(translationQuranEntity.getFactor_scale())
                splitEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
                splitEntity.ipad_type = template.ipad_type
                splitEntity.setViewWeakReference(
                    WeakReference(trackViewEntity), WeakReference(blurredImageView)
                )
                splitEntity.updatePaint(
                    translationQuranEntity.getPaintAya().textSize,
                    translationQuranEntity.getStaticLayout()!!.width
                )
                splitEntity.setColor(translationQuranEntity.clrAya)
                splitEntity.initPreset(translationQuranEntity.getmPreset())

                trackViewEntity.stackSplit(entityTimeline)
                val splitTimeline = splitTimeLineTrsl(
                    entityTimeline.index + 1, splitEntity,
                    abs(trackViewEntity.getCurrentPosition()),
                    entityTimeline.rect.right,
                    entityTimeline.scaleFactor
                )
                val transition = entityTimeline.getTransition()
                if (transition != null) {
                    splitTimeline.setTransition(transition.duplicate())
                    if (transition.isIn && transition.isOut) {
                        splitTimeline.getTransition()!!.isIn = false
                        transition.isOut = false
                    } else if (transition.isIn) {
                        splitTimeline.getTransition()!!.isIn = false
                    } else if (transition.isOut) {
                        transition.isOut = false
                    }
                }
                entityTimeline.setCurrentRect()
                entityTimeline.right = abs(trackViewEntity.getCurrentPosition())
                entityTimeline.onChange()
                splitEntity.entityTrslTimeline = splitTimeline
                splitTimeline.setEntityView(splitEntity)
                if (entityTimeline.getTransition() != null) {
                    splitTimeline.setTransition(entityTimeline.getTransition()!!.duplicate())
                }
                blurredImageView.addEntity(splitEntity, translationQuranEntity.index + 1)
                trackViewEntity.invalidate()
            }
        }
    }

    /**
     * Batch-add all entities from a template (Quran ayahs, translations, Bismilah, Isti3adah).
     *
     * Iterates over `template.quranEntityList`, `translationTemplateList`,
     * `entityIsti3adaTemplate`, and `entityBismilahTemplate`, calling the
     * appropriate `addEntity*` method for each.
     */
    fun addEntityFromTemplate() {
        val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/خط فارس الكوفي.otf")
        val createFromAsset = Typeface.createFromAsset(context.resources.assets, "fonts/ReadexPro_Medium.ttf")

        for (entityQuranTemplate in template.quranEntityList) {
            addEntity(
                entityQuranTemplate.aya,
                entityQuranTemplate.complete_aya,
                entityQuranTemplate.translation,
                entityQuranTemplate.translation_complete,
                entityQuranTemplate.left,
                entityQuranTemplate.right,
                entityQuranTemplate.indexNumber,
                entityQuranTemplate.number,
                entityQuranTemplate.color,
                entityQuranTemplate.name_font ?: "hafes",
                entityQuranTemplate.transition ?: Transition(),
                isGradientBg,
                entityQuranTemplate.icon,
                entityQuranTemplate.startWord_index,
                entityQuranTemplate.endWord_index,
                entityQuranTemplate.scale,
                entityQuranTemplate.factor_size,
                entityQuranTemplate.factor_sizeTrl,
                RectF(
                    entityQuranTemplate.rectF!!.l,
                    entityQuranTemplate.rectF!!.t,
                    entityQuranTemplate.rectF!!.r,
                    entityQuranTemplate.rectF!!.b
                ),
                loadFontFromAsset!!,
                createFromAsset!!,
                entityQuranTemplate.colorTrsl,
                entityQuranTemplate.preset
            )
        }

        for (translationTemplate in template.translationTemplateList) {
            addEntityTrsl(
                translationTemplate.aya,
                translationTemplate.left,
                translationTemplate.right,
                translationTemplate.number,
                translationTemplate.color,
                translationTemplate.name_font ?: "ReadexPro_Medium.ttf",
                translationTemplate.transition ?: Transition(),
                translationTemplate.scale,
                translationTemplate.factor_size,
                RectF(
                    translationTemplate.rectF!!.l,
                    translationTemplate.rectF!!.t,
                    translationTemplate.rectF!!.r,
                    translationTemplate.rectF!!.b
                ),
                translationTemplate.preset,
                translationTemplate.clr_bg,
                translationTemplate.isHaveBg
            )
        }

        template.entityIsti3adaTemplate?.let { isti3ada ->
            addEntityIsti3ada(
                isti3ada.aya,
                isti3ada.left,
                isti3ada.right,
                isti3ada.color,
                isti3ada.transition ?: Transition(),
                isti3ada.scale,
                isti3ada.factor_size,
                RectF(
                    isti3ada.rectF!!.l,
                    isti3ada.rectF!!.t,
                    isti3ada.rectF!!.r,
                    isti3ada.rectF!!.b
                ),
                isti3ada.preset
            )
        }

        template.entityBismilahTemplate?.let { bismilah ->
            addEntityBismilah(
                bismilah.aya,
                bismilah.left,
                bismilah.right,
                bismilah.color,
                bismilah.transition ?: Transition(),
                bismilah.scale,
                bismilah.factor_size,
                RectF(
                    bismilah.rectF!!.l,
                    bismilah.rectF!!.t,
                    bismilah.rectF!!.r,
                    bismilah.rectF!!.b
                ),
                bismilah.preset
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Timeline entity helpers (from EngineEntityManager.kt)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Create and add an [EntityQuranTimeline] at the current cursor position.
     */
    fun addTimeLineQuran(quranEntity: QuranEntity): EntityQuranTimeline {
        var xCursor = trackViewEntity.getXCursur()
        val lastQuran = trackViewEntity.getQuran()
        if (lastQuran != null) {
            xCursor = lastQuran.rect.right
        }
        val entityQuranTimeline = EntityQuranTimeline(
            quranEntity, xCursor, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            trackViewEntity.getQuran()!!.rect.right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addQuran(entityQuranTimeline)
        return entityQuranTimeline
    }

    /**
     * Create and add an [EntityQuranTimeline] at explicit left/right positions.
     */
    fun addTimeLineQuran(
        index: Int, quranEntity: QuranEntity, left: Float, right: Float
    ): EntityQuranTimeline {
        val entityQuranTimeline = EntityQuranTimeline(
            quranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addQuran_split(entityQuranTimeline, index)
        return entityQuranTimeline
    }

    /**
     * Create and add an [EntityQuranTimeline] at explicit left/right positions (alias for duplicate use).
     */
    private fun addTimeLineQuranAtIndex(
        index: Int, quranEntity: QuranEntity, left: Float, right: Float
    ): EntityQuranTimeline {
        val entityQuranTimeline = EntityQuranTimeline(
            quranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addQuran(entityQuranTimeline, index)
        return entityQuranTimeline
    }

    /**
     * Create and add an [EntityQuranTimeline] at explicit left/right positions
     * (used by template addEntity with explicit positions).
     */
    private fun addTimeLineQuranWithPosition(
        quranEntity: QuranEntity, left: Float, right: Float
    ): EntityQuranTimeline {
        val entityQuranTimeline = EntityQuranTimeline(
            quranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addQuran(entityQuranTimeline)
        return entityQuranTimeline
    }

    /**
     * Create and add an [EntityQuranTimeline] for a split operation.
     */
    fun splitTimeLineQuran(
        index: Int, quranEntity: QuranEntity, left: Float, right: Float, scaleFactor: Float
    ): EntityQuranTimeline {
        val entityQuranTimeline = EntityQuranTimeline(
            quranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        entityQuranTimeline.scaleFactor = scaleFactor
        trackViewEntity.addQuran_split(entityQuranTimeline, index)
        return entityQuranTimeline
    }

    /**
     * Split a translation timeline entity.
     */
    fun splitTimeLineQuran(
        index: Int, translationQuranEntity: TranslationQuranEntity,
        left: Float, right: Float, scaleFactor: Float
    ): EntityTrslTimeline = splitTimeLineTrsl(index, translationQuranEntity, left, right, scaleFactor)

    /**
     * Create and add an [EntityTrslTimeline] at the current cursor position.
     */
    fun addTimeLineTrslQuran(translationQuranEntity: TranslationQuranEntity): EntityTrslTimeline {
        var xCursor = trackViewEntity.getXCursur()
        val lastTrsl = trackViewEntity.getTrslQuran()
        if (lastTrsl != null) {
            xCursor = lastTrsl.rect.right
        }
        val entityTrslTimeline = EntityTrslTimeline(
            translationQuranEntity, xCursor, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            trackViewEntity.getQuran()!!.rect.right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addTrslQuran(entityTrslTimeline)
        return entityTrslTimeline
    }

    /**
     * Create and add an [EntityTrslTimeline] at explicit left/right positions.
     */
    fun addTimeLineTrslQuran(
        index: Int, translationQuranEntity: TranslationQuranEntity, left: Float, right: Float
    ): EntityTrslTimeline {
        val entityTrslTimeline = EntityTrslTimeline(
            translationQuranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addTrslQuran(entityTrslTimeline, index)
        return entityTrslTimeline
    }

    /**
     * Create and add an [EntityTrslTimeline] at explicit index (for duplicate).
     */
    private fun addTimeLineTrslQuranAtIndex(
        index: Int, translationQuranEntity: TranslationQuranEntity, left: Float, right: Float
    ): EntityTrslTimeline {
        val entityTrslTimeline = EntityTrslTimeline(
            translationQuranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addTrslQuran(entityTrslTimeline, index)
        return entityTrslTimeline
    }

    /**
     * Create and add an [EntityTrslTimeline] at explicit left/right positions
     * (used by template addEntityTrsl).
     */
    private fun addTimeLineTrslWithPosition(
        translationQuranEntity: TranslationQuranEntity, left: Float, right: Float
    ): EntityTrslTimeline {
        val entityTrslTimeline = EntityTrslTimeline(
            translationQuranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.addTrslQuran(entityTrslTimeline)
        return entityTrslTimeline
    }

    /**
     * Split a translation timeline entity.
     */
    fun splitTimeLineTrsl(
        index: Int, translationQuranEntity: TranslationQuranEntity,
        left: Float, right: Float, scaleFactor: Float
    ): EntityTrslTimeline {
        val entityTrslTimeline = EntityTrslTimeline(
            translationQuranEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        entityTrslTimeline.scaleFactor = scaleFactor
        trackViewEntity.addTrslQuran(entityTrslTimeline, index)
        return entityTrslTimeline
    }

    /**
     * Create and add a Bismilah timeline entity at the default cursor position.
     */
    fun addTimeLineBismilah(bismilahEntity: BismilahEntity): EntityBismilahTimeline {
        val left = trackViewEntity.getmIsi3adaTimeline()?.rect?.right ?: 0.0f
        val entityBismilahTimeline = EntityBismilahTimeline(
            bismilahEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            left + trackViewEntity.getSecond_in_screen() * 4.0f,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.bismilahTimeline = entityBismilahTimeline
        return entityBismilahTimeline
    }

    /**
     * Create and add a Bismilah timeline entity at explicit left/right positions.
     */
    fun addTimeLineBismilah(
        bismilahEntity: BismilahEntity, left: Float?, right: Float?
    ): EntityBismilahTimeline {
        if (left != null && right != null) {
            return addTimeLineBismilahWithPosition(bismilahEntity, left, right)
        }
        return addTimeLineBismilah(bismilahEntity)
    }

    /**
     * Create and add a Bismilah timeline entity at explicit left/right positions.
     */
    private fun addTimeLineBismilahWithPosition(
        bismilahEntity: BismilahEntity, left: Float, right: Float
    ): EntityBismilahTimeline {
        val entityBismilahTimeline = EntityBismilahTimeline(
            bismilahEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.bismilahTimeline = entityBismilahTimeline
        return entityBismilahTimeline
    }

    /**
     * Create and add an Isti3adah timeline entity at the default position.
     */
    fun addTimeLineIsti3ada(bismilahEntity: BismilahEntity): EntityBismilahTimeline {
        val entityBismilahTimeline = EntityBismilahTimeline(
            bismilahEntity, 0.0f, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            trackViewEntity.getSecond_in_screen() * 4.0f + 0.0f,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.setmIsi3adaTimeline(entityBismilahTimeline)
        return entityBismilahTimeline
    }

    /**
     * Create and add an Isti3adah timeline entity at explicit left/right positions.
     */
    fun addTimeLineIsti3ada(
        bismilahEntity: BismilahEntity, left: Float?, right: Float?
    ): EntityBismilahTimeline {
        if (left != null && right != null) {
            return addTimeLineIsti3adaWithPosition(bismilahEntity, left, right)
        }
        return addTimeLineIsti3ada(bismilahEntity)
    }

    /**
     * Create and add an Isti3adah timeline entity at explicit left/right positions.
     */
    private fun addTimeLineIsti3adaWithPosition(
        bismilahEntity: BismilahEntity, left: Float, right: Float
    ): EntityBismilahTimeline {
        val entityBismilahTimeline = EntityBismilahTimeline(
            bismilahEntity, left, 0.0f,
            trackViewEntity.getWidth() * 0.077f,
            right,
            trackViewEntity.getSecond_in_screen()
        )
        trackViewEntity.setmIsi3adaTimeline(entityBismilahTimeline)
        return entityBismilahTimeline
    }

    // ══════════════════════════════════════════════════════════════════
    //  Timeline animation (from EngineTimelineManager.kt)
    // ══════════════════════════════════════════════════════════════════

    /**
     * Start smooth timeline animation from the current cursor position.
     *
     * Creates a [SmoothTimelineAnimator] that iterates from [startCursor] to
     * [maxTime], updating the cursor position, blurredImageView progress,
     * audio playback, and time display on each frame. On animation end it
     * pauses playback, resets the cursor, and updates play/pause button state.
     *
     * This is the main "play" entry point for the engine.
     */
    fun startAnimation() {
        entityAudioVisible = null
        entityAudioPlayer = null
        lastIndexVisible = 0
        val maxTime = trackViewEntity.maxTime
        val timeLineW = trackViewEntity.timeLineW
        timeFormatter = TimeFormatter(maxTime.toLong())
        val animator = SmoothTimelineAnimator(startCursor, maxTime, object : SmoothTimelineAnimator.AnimatorListener {
            override fun onUpdate(i: Int) {
                if (!state.isPlaying || i == 0) return
                val f = i.toFloat() / maxTime
                updateTime(i.toLong())
                blurredImageView.progress = f
                trackViewEntity.updateCursur(f * timeLineW)
                trackViewEntity.current_cursur_position = i
                val absTime = abs(
                    round((trackViewEntity.currentPosition / trackViewEntity.second_in_screen) * 1000.0f).toInt()
                )
                if (absTime > endTimeAudioVisible) {
                    entityAudioVisible = null
                }
                if (entityAudioVisible == null) {
                    for (idx in lastIndexVisible until trackViewEntity.entityListAudio.size) {
                        val ea = trackViewEntity.entityListAudio[idx]
                        if (ea.visible() && ea.isVisible) {
                            entityAudioVisible = ea
                            endTimeAudioVisible = round(
                                (entityAudioVisible!!.rect.right / trackViewEntity.second_in_screen) * 1000.0f
                            ).toInt()
                            lastIndexVisible = idx
                            break
                        }
                    }
                }
                try {
                    if (entityAudioVisible != null) {
                        if (entityAudioPlayer !== entityAudioVisible && mPlayer != null && mPlayer!!.isPlaying) {
                            mPlayer!!.pause()
                        }
                        mPlayer = entityAudioVisible!!.mediaPlayer
                        if (mPlayer != null && !mPlayer!!.isPlaying) {
                            entityAudioPlayer = entityAudioVisible
                            val seekPos = ((absTime - abs(
                                round((entityAudioVisible!!.rect.left / trackViewEntity.second_in_screen) * 1000.0f).toInt()
                            )) + entityAudioVisible!!.start).toInt()
                            if (seekPos <= mPlayer!!.duration) {
                                mPlayer!!.seekTo(seekPos)
                            }
                            Log.d("TimelineEngine", "mPlayer currentPos: ${mPlayer!!.currentPosition}")
                            mPlayer!!.start()
                        }
                    } else if (mPlayer != null && mPlayer!!.isPlaying) {
                        mPlayer!!.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                updateStartViewTime(trackViewEntity.current_cursur_position)
                callbacks.onUpdateBtnCutState()
            }

            override fun onEnd() {
                if (state.isPlaying) {
                    state = state.copy(isPlaying = false)
                    trackViewEntity.isPlaying = false
                    blurredImageView.isPlaying = false
                    stopAnimator()
                    trackViewEntity.current_cursur_position = trackViewEntity.maxTime
                    trackViewEntity.updateCursur(trackViewEntity.maxTime)
                    try {
                        entityAudioVisible?.let { ea ->
                            ea.mediaPlayer?.let { mp ->
                                if (mp.isPlaying) mp.pause()
                            }
                        }
                        mPlayer?.let { mp ->
                            if (mp.isPlaying) mp.pause()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    startCursor = 0
                    callbacks.onPlayStateChanged(false)
                    callbacks.onUpdateBtnToEnd()
                    callbacks.onUpdateBtnToStart()
                }
            }
        })
        valueAnimator = animator
        animator.start()
        if (template.isVideoSquare) {
            callbacks.onStartSquareVideoAnimator()
        }
    }

    /**
     * Start timeline animation in preview mode for a specific audio entity.
     *
     * Similar to [startAnimation] but only plays the given [EntityAudio]'s
     * MediaPlayer. On end, it does NOT reset the cursor — it saves the
     * current position so the user can continue editing from there.
     *
     * @param entityAudio The audio entity to preview
     */
    fun startAnimationPreview(entityAudio: EntityAudio) {
        val maxTime = trackViewEntity.maxTime
        val timeLineW = trackViewEntity.timeLineW
        timeFormatter = TimeFormatter(maxTime.toLong())
        val animator = SmoothTimelineAnimator(startCursor, maxTime, object : SmoothTimelineAnimator.AnimatorListener {
            override fun onUpdate(i: Int) {
                if (!state.isPlaying || i == 0) return
                val f = i.toFloat() / maxTime
                updateTime(i.toLong())
                blurredImageView.progress = f
                trackViewEntity.updateCursur(f * timeLineW)
                trackViewEntity.current_cursur_position = i
                try {
                    if (entityAudio.mediaPlayer != null && !entityAudio.mediaPlayer!!.isPlaying) {
                        val absTime = abs(
                            round((trackViewEntity.currentPosition / trackViewEntity.second_in_screen) * 1000.0f).toInt()
                        )
                        val seekPos = (absTime - abs(
                            round((entityAudio.rect.left / trackViewEntity.second_in_screen) * 1000.0f).toInt()
                        ) + entityAudio.start).toInt()
                        if (seekPos <= entityAudio.mediaPlayer!!.duration) {
                            entityAudio.mediaPlayer!!.seekTo(seekPos)
                        }
                        entityAudio.mediaPlayer!!.start()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                updateStartViewTime(trackViewEntity.current_cursur_position)
            }

            override fun onEnd() {
                if (state.isPlaying) {
                    state = state.copy(isPlaying = false)
                    trackViewEntity.isPlaying = false
                    blurredImageView.isPlaying = false
                    stopAnimator()
                    try {
                        if (entityAudio.mediaPlayer != null && entityAudio.mediaPlayer!!.isPlaying) {
                            entityAudio.mediaPlayer!!.pause()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    startCursor = trackViewEntity.current_cursur_position
                }
                callbacks.onPlayStateChanged(false)
            }
        })
        valueAnimator = animator
        animator.start()
        if (template.isVideoSquare) {
            callbacks.onStartSquareVideoAnimator()
        }
    }

    /**
     * Pause the running timeline animation.
     *
     * Stops the [SmoothTimelineAnimator].
     */
    fun pauseAnimation() {
        valueAnimator?.stop()
    }

    /**
     * Seek the timeline cursor to a specific time position.
     *
     * @param timeMs Target position in milliseconds
     */
    fun seekTo(timeMs: Long) {
        val maxTime = trackViewEntity.maxTime
        if (maxTime <= 0) return
        val f = timeMs.toFloat() / maxTime
        trackViewEntity.updateCursur((f * trackViewEntity.timeLineW).toInt())
        trackViewEntity.current_cursur_position = timeMs.toInt()
        startCursor = timeMs.toInt()
        updateTime(timeMs)
        updateStartViewTime(timeMs.toInt())
        blurredImageView.progress = f
    }

    /**
     * Recalculate the timeline's max time and refresh the time display.
     *
     * Calls `trackViewEntity.calculMaxTime()`, then updates the current/end
     * time labels and the blurredImageView progress bar.
     */
    fun updateTime() {
        trackViewEntity.calculMaxTime()
        updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
        if (trackViewEntity.current_cursur_position <= trackViewEntity.maxTime) {
            updateTime(trackViewEntity.current_cursur_position.toLong())
            trackViewEntity.current_cursur_position = trackViewEntity.current_cursur_position
            if (trackViewEntity.maxTime > 0) {
                blurredImageView.progress =
                    trackViewEntity.current_cursur_position.toFloat() / trackViewEntity.maxTime
            }
        }
    }

    /**
     * Update the time display to reflect the given timestamp.
     *
     * Uses a [TimeFormatter] to format milliseconds into a display string
     * and passes it to the blurredImageView.
     *
     * @param timeMs Current time in milliseconds
     */
    fun updateTime(timeMs: Long) {
        val tf = timeFormatter
        if (tf == null) {
            timeFormatter = TimeFormatter(trackViewEntity.maxTime.toLong())
        } else {
            tf.setTotalDurationMs(trackViewEntity.maxTime.toLong())
        }
        val formatTime = timeFormatter!!.formatTime(timeMs)
        blurredImageView.setCurrentTime(formatTime.first as String, formatTime.second as String)
    }

    /**
     * Update time display and translate the timeline to the end of the last entity.
     *
     * Used after adding a new audio entity to ensure the timeline scrolls
     * to show the latest content.
     */
    fun updateTimeToEndAya() {
        trackViewEntity.calculMaxTime()
        trackViewEntity.translateToEnd()
        updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
        if (trackViewEntity.current_cursur_position <= trackViewEntity.maxTime) {
            updateTime(trackViewEntity.current_cursur_position.toLong())
            trackViewEntity.current_cursur_position = trackViewEntity.current_cursur_position
            if (trackViewEntity.maxTime > 0) {
                blurredImageView.progress =
                    trackViewEntity.current_cursur_position.toFloat() / trackViewEntity.maxTime
            }
        }
    }

    /**
     * Process a single video frame for square-video display.
     *
     * Loads a frame image, applies iPad-type-specific cropping
     * (round corners, square, 16:9, 9:16), and updates the
     * blurredImageView's square bitmap.
     *
     * @param framePath Absolute path to the JPEG frame file
     */
    fun processFrame(framePath: String) {
        callbacks.onProcessFrame(framePath)
    }

    /**
     * Initialize the timeline track view.
     *
     * Sets up the [TrackEntityView], configures the scale factor from the
     * template, computes screen-dependent dimensions, and positions the cursor.
     *
     * @param screenWidth Screen width in pixels
     */
    fun initTimeLineView(screenWidth: Int) {
        trackViewEntity.scaleFactor = template.scale_timeline
        trackViewEntity.post {
            val audioPosition = screenWidth * 0.12f
            trackViewEntity.setSecond_in_screen(audioPosition)
            trackViewEntity.setSecond_in_screen(audioPosition, 0, screenWidth)
            trackViewEntity.maxTime = 0
            trackViewEntity.init(screenWidth, trackViewEntity.height)
            trackViewEntity.setPosCursur(template.currentCursur)
            startCursor = trackViewEntity.current_cursur_position
            updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
        }
    }

    /**
     * Update the frame display when scrolling (not playing).
     *
     * Computes the correct frame number from the cursor position and calls
     * [processFrame] or `updateSquareBitmap` to refresh the preview.
     */
    fun updateFrame() {
        // Frame update logic is delegated to the host via callbacks.
        // The host can compute the frame index from trackViewEntity state.
        callbacks.onInvalidate()
    }

    /**
     * Update the start-time label.
     *
     * @param positionMs Current position in milliseconds
     */
    fun updateStartViewTime(positionMs: Int) {
        callbacks.onUpdateStartViewTime(positionMs)
    }

    /**
     * Update the end-time label.
     *
     * @param positionMs Duration in milliseconds
     */
    fun updateEndViewTime(positionMs: Int) {
        callbacks.onUpdateEndViewTime(positionMs)
    }

    /**
     * Update both start and end time labels.
     *
     * @param maxTimeMs    Maximum time in milliseconds
     * @param currentMs    Current cursor position in milliseconds
     */
    fun updateViewTime(maxTimeMs: Int, currentMs: Int) {
        callbacks.onUpdateViewTime(maxTimeMs, currentMs)
    }

    /**
     * Copy transition settings from one entity to another (Bismilah overload).
     *
     * @param target   Entity whose transition will be updated
     * @param source   Entity whose transition values to copy
     */
    fun copyTransition(target: EntityBismilahTimeline?, source: EntityBismilahTimeline) {
        if (target == null) return
        if (target.getTransition() == null) {
            target.setTransition(Transition())
        }
        source.getTransition()?.let { src ->
            target.getTransition()!!.isOut = src.isOut
            target.getTransition()!!.type_out = src.type_out
            target.getTransition()!!.duration_out = src.duration_out
            target.getTransition()!!.isIn = src.isIn
            target.getTransition()!!.type_in = src.type_in
            target.getTransition()!!.duration_in = src.duration_in
        }
    }

    /**
     * Copy transition settings from a Quran timeline entity to a Bismilah timeline entity.
     */
    fun copyTransition(target: EntityBismilahTimeline?, source: EntityQuranTimeline) {
        if (target == null) return
        if (target.getTransition() == null) {
            target.setTransition(Transition())
        }
        source.getTransition()?.let { src ->
            target.getTransition()!!.isOut = src.isOut
            target.getTransition()!!.type_out = src.type_out
            target.getTransition()!!.duration_out = src.duration_out
            target.getTransition()!!.isIn = src.isIn
            target.getTransition()!!.type_in = src.type_in
            target.getTransition()!!.duration_in = src.duration_in
        }
    }

    /**
     * Check if the current cursor position is within the selected Quran entity for splitting.
     */
    fun checkSplitEntity(): Boolean {
        val selectedEntity = trackViewEntity.selectedEntity ?: return false
        if (selectedEntity !is EntityQuranTimeline) return false
        val cursorAbs = -trackViewEntity.getCurrentPosition()
        return cursorAbs > selectedEntity.rect.left && cursorAbs < selectedEntity.rect.right
    }

    /**
     * Check if the current cursor position is within the selected translation entity for splitting.
     */
    fun checkSplitTrslEntity(): Boolean {
        val selectedEntity = trackViewEntity.selectedEntity ?: return false
        if (selectedEntity !is EntityTrslTimeline) return false
        val cursorAbs = -trackViewEntity.getCurrentPosition()
        return cursorAbs > selectedEntity.rect.left && cursorAbs < selectedEntity.rect.right
    }

    /**
     * Check if the current cursor position is within the selected audio entity for splitting.
     */
    fun checkSplitAudio(): Boolean {
        val selectedEntity = trackViewEntity.selectedEntity ?: return false
        if (selectedEntity !is EntityAudio) return false
        val cursorAbs = -trackViewEntity.getCurrentPosition()
        return cursorAbs > selectedEntity.rect.left && cursorAbs < selectedEntity.rect.right
    }

    // ══════════════════════════════════════════════════════════════════
    //  Play state management
    // ══════════════════════════════════════════════════════════════════

    /**
     * Set the playing state. Updates internal state and notifies the track/canvas views.
     */
    fun setPlaying(playing: Boolean) {
        state = state.copy(isPlaying = playing)
        trackViewEntity.isPlaying = playing
        blurredImageView.isPlaying = playing
    }

    /** Get the current playing state. */
    fun isPlaying(): Boolean = state.isPlaying

    /** Set the start cursor for the next animation. */
    fun setStartCursor(cursor: Int) {
        startCursor = cursor
    }

    /** Get the current start cursor. */
    fun getStartCursor(): Int = startCursor

    /** Set the MediaPlayer reference for audio playback during animation. */
    fun setMediaPlayer(player: MediaPlayer?) {
        mPlayer = player
    }

    /** Get the current MediaPlayer reference. */
    fun getMediaPlayer(): MediaPlayer? = mPlayer

    // ══════════════════════════════════════════════════════════════════
    //  Internal helpers
    // ══════════════════════════════════════════════════════════════════

    /**
     * Stop the [SmoothTimelineAnimator] value animator.
     */
    private fun stopAnimator() {
        valueAnimator?.stop()
    }

    /**
     * Release all timeline resources (animators, handlers, etc.).
     *
     * Stops the SmoothTimelineAnimator and releases MediaPlayer references.
     */
    fun release() {
        stopAnimator()
        try {
            mPlayer?.let { mp ->
                if (mp.isPlaying) mp.pause()
            }
        } catch (_: Exception) {
        }
        mPlayer = null
        entityAudioVisible = null
        entityAudioPlayer = null
    }

    // ══════════════════════════════════════════════════════════════════
    //  Static time formatting utilities
    // ══════════════════════════════════════════════════════════════════

    companion object {
        /**
         * Format milliseconds to a compact time string in M:SS format.
         * Used by the host to format time labels.
         */
        fun formatTimeMs(ms: Int): String {
            val j = ms.toLong()
            val minutes = TimeUnit.MILLISECONDS.toMinutes(j)
            val seconds = TimeUnit.MILLISECONDS.toSeconds(j) - TimeUnit.MINUTES.toSeconds(minutes)
            return if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }
        }

        /**
         * Format milliseconds to a duration string with "/" prefix.
         * Used for the end-time label.
         */
        fun formatDurationMs(ms: Int): String {
            return "/${formatTimeMs(ms)}"
        }
    }
}
