package hazem.nurmontage.videoquran.ui.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.core.view.InputDeviceCompat
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.model.*
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.FileHelper
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.TrackEntityView
import hazem.nurmontage.videoquran.views.blurred.setupBitmapDraw
import hazem.nurmontage.videoquran.views.blurred.updateIpad
import java.io.File
import java.util.concurrent.Executor

/**
 * ExportPipeline
 *
 * Encapsulates the export / save flow for the engine screen.
 * In the original code this was spread across:
 *   - save()             (EngineActivity.kt:2998 — main export entry point)
 *   - saveTemplateTmp()  (EngineActivity.kt:3310 — auto-save on onPause)
 *   - saveTemplate()     (EngineActivity.kt:3548 — full template serialization before render)
 *
 * The export flow is:
 *   1. [save()] — prepares bitmap dimensions, crops, and iPad frames at export resolution
 *   2. [saveTemplate()] — serialises all entities (Quran, translation, bismilah, surah name,
 *      media, effects) from TrackEntityView into the Template object, then writes to disk
 *   3. Launches ProgressViewActivity for the FFmpeg render
 *
 * [saveTemplate] and [saveTemplateTmp] share ~90% of their logic; the common
 * serialization is extracted into [serializeTemplate] with an `isTmp` flag that
 * controls the persistence key and minor behavioural differences.
 *
 * @param context           Application context for file / preference operations
 * @param executor          Background thread executor for bitmap-heavy work
 * @param bitmapLoader      Callback to load a background bitmap at export resolution.
 *                          The caller (EngineActivity) uses Glide for this. Parameters:
 *                          (uriOrFrameBg: String?, isVideoSquare: Boolean, maxDim: Int) -> Bitmap
 * @param onProgress        Progress callback (0..100)
 * @param onSuccess         Called with the template file on successful export
 * @param onError           Called with an error message on failure
 * @param onExportStarted   Called when export begins (before executor work)
 * @param onProgressShow    Called to show a progress UI overlay
 * @param onProgressHide    Called to hide a progress UI overlay
 */
class ExportPipeline(
    private val context: Context,
    private val executor: Executor,
    private val bitmapLoader: (uriOrFrameBg: String?, isVideoSquare: Boolean, maxDim: Int) -> Bitmap,
    private val onProgress: (Int) -> Unit,
    private val onSuccess: (File) -> Unit,
    private val onError: (String) -> Unit,
    private val onExportStarted: () -> Unit = {},
    private val onProgressShow: () -> Unit = {},
    private val onProgressHide: () -> Unit = {}
) {
    // References set externally before calling save()
    var template: Template? = null
    var blurredImageView: BlurredImageView? = null
    var trackViewEntity: TrackEntityView? = null
    var uriBg: String? = null

    /** App name string — used for the video output folder. Defaults to "NurMontage". */
    var appName: String = "NurMontage"

    // Prevents double-export
    var isExporting: Boolean = false

    // ──────────────────────────────────────────────
    //  Main export entry point
    // ──────────────────────────────────────────────

    /**
     * Start the export pipeline.
     *
     * Corresponds to `EngineActivity.save()` (line ~2998).
     *
     * This method:
     *  1. Guards against double-export
     *  2. Prepares the BlurredImageView for export resolution
     *  3. Re-crops background and iPad frames at the target resolution
     *  4. Serialises the template via [saveTemplate]
     *  5. Calls [onSuccess] with the template file
     */
    fun save() {
        if (isExporting) return
        isExporting = true

        val tve = trackViewEntity ?: run { isExporting = false; return }
        val biv = blurredImageView ?: run { isExporting = false; return }
        val tmpl = template ?: run { isExporting = false; return }

        tve.finishScroll()
        tve.setOnProgress(true)
        biv.setNotDraw(true)
        // No watermark for any user — billing removed

        onExportStarted()
        onProgressShow()

        executor.execute {
            try {
                tve.calculMaxTime()
                biv.invalidate()
                biv.initCanvasDimension(tmpl.width, tmpl.height, tmpl.geTypeResize())
                val max = Math.max(tmpl.width, tmpl.height)

                if (tmpl.ipad_type != IpadType.HEART.ordinal && tmpl.ipad_type != IpadType.BATTERY.ordinal) {
                    // ── Non-HEART/BATTERY types ────────────────────────────────
                    if (tmpl.isVideoSquare && (
                        tmpl.ipad_type == IpadType.GRADIENT.ordinal ||
                        tmpl.ipad_type == IpadType.BLACK_LAYER.ordinal ||
                        tmpl.ipad_type == IpadType.MASK_BRUSH.ordinal ||
                        tmpl.ipad_type == IpadType.BLUE_TYPE.ordinal ||
                        tmpl.ipad_type == IpadType.CASSET_IMG.ordinal
                    )) {
                        // Video square + layered types — solid canvas, crop from center
                        biv.bitmapOriginal = Bitmap.createBitmap(max, max, Bitmap.Config.ARGB_8888)
                        val cropTo16x92 = when (tmpl.geTypeResize()) {
                            ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(biv.bitmapOriginal)
                            ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(biv.bitmapOriginal)
                            else -> BitmapCropper.cropTo16x9(biv.bitmapOriginal)
                        }
                        biv.updatePosCanvas(tmpl.width, tmpl.height, cropTo16x92)
                        biv.bitmapBlured = cropTo16x92
                        biv.updateIpad(cropTo16x92!!, tmpl.ipad_type, tmpl.geTypeResize())
                        val width = (biv.ipad_rect!!.width() * 1.0f).toInt()
                        val height = (cropTo16x92!!.height * 0.5355f).toInt()
                        tmpl.setDrawingTranslation(biv.btmX, biv.btmY)

                        var round = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                        var round2 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                        var i4 = width + round
                        if (i4 > biv.bitmapOriginal!!.width) {
                            round -= i4 - biv.bitmapOriginal!!.width
                            i4 = biv.bitmapOriginal!!.width
                        }
                        var i5 = height + round2
                        if (i5 > biv.bitmapOriginal!!.height) {
                            round2 -= i5 - biv.bitmapOriginal!!.height
                            i5 = biv.bitmapOriginal!!.height
                        }
                        if (round < 0) round = 0
                        if (round2 < 0) round2 = 0
                        val rect2 = Rect(round, round2, i4, i5)
                        val width2 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                        val height2 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                        val cropToSquare = UtilsBitmap.cropToSquare(biv.bitmapOriginal!!, rect2, width2, height2)
                        biv.bitmapSquare = cropToSquare
                        biv.radius_square = 0
                        rect2.right = rect2.left + width2
                        rect2.bottom = rect2.top + height2
                        biv.rectSquare = rect2
                        tmpl.uri_bg_ffmpeg = biv.setupBitmapDraw(cropTo16x92!!, cropToSquare!!, tmpl)
                        tmpl.squareBitmapModel!!.set(
                            biv.left_square, biv.top_square,
                            round.toFloat(), round2.toFloat(),
                            rect2.width().toFloat(), rect2.height().toFloat(),
                            cropToSquare.width.toFloat(), cropToSquare.height.toFloat(),
                            0f
                        )
                    } else {
                        // Standard types — reload background at export resolution via callback
                        val loadedBitmap = bitmapLoader(
                            if (tmpl.isVideoSquare) tmpl.frame_bg else tmpl.uri_bg,
                            tmpl.isVideoSquare,
                            max
                        )
                        biv.bitmapOriginal = setupOriginalBitmap(loadedBitmap, max)
                        val cropTo16x9 = when (tmpl.geTypeResize()) {
                            ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(biv.bitmapOriginal, tmpl.width, tmpl.height)
                            ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(biv.bitmapOriginal, tmpl.width, tmpl.height)
                            else -> BitmapCropper.cropTo16x9(biv.bitmapOriginal, tmpl.width, tmpl.height)
                        }
                        val bitmap = cropTo16x9
                        biv.updatePosCanvas(tmpl.width, tmpl.height, bitmap)
                        biv.bitmapBlured = bitmap
                        biv.updateIpad(bitmap!!, tmpl.ipad_type, tmpl.geTypeResize())

                        var rect: Rect
                        var cropToSquareWithRoundCorners: Bitmap? = null
                        var i2: Int
                        var i3: Int

                        if (tmpl.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                            val radius = (biv.ipad_rect!!.width() * 0.6f).toInt()
                            tmpl.setDrawingTranslation(biv.btmX, biv.btmY)
                            i2 = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                            i3 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                            var i6 = radius + i2
                            if (i6 > biv.bitmapOriginal!!.width) {
                                i2 -= i6 - biv.bitmapOriginal!!.width
                                i6 = biv.bitmapOriginal!!.width
                            }
                            var i7 = radius + i3
                            if (i7 > biv.bitmapOriginal!!.height) {
                                i3 -= i7 - biv.bitmapOriginal!!.height
                                i7 = biv.bitmapOriginal!!.height
                            }
                            if (i2 < 0) i2 = 0
                            if (i3 < 0) i3 = 0
                            rect = Rect(i2, i3, i6, i7)
                            val width3 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                            val height3 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                            cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(biv.bitmapOriginal!!, rect, radius, width3, height3)
                            rect.right = rect.left + width3
                            rect.bottom = rect.top + height3
                            biv.rectSquare = rect
                        } else {
                            if (tmpl.ipad_type != IpadType.IPAD.ordinal &&
                                tmpl.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                                tmpl.ipad_type != IpadType.IPAD_CLASSIC.ordinal
                            ) {
                                val width4 = (biv.ipad_rect!!.width() * 1.0f).toInt()
                                val height4 = (bitmap!!.height * 0.5355f).toInt()
                                tmpl.setDrawingTranslation(biv.btmX, biv.btmY)
                                var round3 = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                                var round4 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                                var i8 = width4 + round3
                                if (i8 > biv.bitmapOriginal!!.width) {
                                    round3 -= i8 - biv.bitmapOriginal!!.width
                                    i8 = biv.bitmapOriginal!!.width
                                }
                                var i9 = height4 + round4
                                if (i9 > biv.bitmapOriginal!!.height) {
                                    round4 -= i9 - biv.bitmapOriginal!!.height
                                    i9 = biv.bitmapOriginal!!.height
                                }
                                if (round3 < 0) round3 = 0
                                if (round4 < 0) round4 = 0
                                rect = Rect(round3, round4, i8, i9)
                                val width5 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                                val height5 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                                cropToSquareWithRoundCorners = UtilsBitmap.cropToSquare(biv.bitmapOriginal!!, rect, width5, height5)
                                biv.bitmapSquare = cropToSquareWithRoundCorners
                                biv.radius_square = 0
                                rect.right = rect.left + width5
                                rect.bottom = rect.top + height5
                                biv.rectSquare = rect
                                i2 = round3
                                i3 = round4
                            } else {
                                i2 = 0
                                i3 = 0
                                rect = Rect() // placeholder, will be reassigned
                            }

                            if (tmpl.ipad_type == IpadType.IPAD.ordinal ||
                                tmpl.ipad_type == IpadType.IPAD_UNBLUR.ordinal ||
                                tmpl.ipad_type == IpadType.IPAD_CLASSIC.ordinal
                            ) {
                                val width6 = (biv.ipad_rect!!.width() * 0.87530595f).toInt()
                                val i10 = (width6 * 1.13f).toInt()
                                val min = Math.min(width6, i10)
                                tmpl.setDrawingTranslation(biv.btmX, biv.btmY)
                                var round5 = Math.round(biv.bitmapOriginal!!.width * tmpl.x_square)
                                var round6 = Math.round(biv.bitmapOriginal!!.height * tmpl.y_square)
                                var i11 = width6 + round5
                                if (i11 > biv.bitmapOriginal!!.width) {
                                    round5 -= i11 - biv.bitmapOriginal!!.width
                                    i11 = biv.bitmapOriginal!!.width
                                }
                                var i12 = i10 + round6
                                if (i12 > biv.bitmapOriginal!!.height) {
                                    round6 -= i12 - biv.bitmapOriginal!!.height
                                    i12 = biv.bitmapOriginal!!.height
                                }
                                if (round5 < 0) round5 = 0
                                if (round6 < 0) round6 = 0
                                rect = Rect(round5, round6, i11, i12)
                                var radiusVal = 0
                                if (tmpl.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                                    val width7 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                                    val height6 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                                    val cropToSquare2 = UtilsBitmap.cropToSquare(biv.bitmapOriginal!!, rect, width7, height6)
                                    biv.bitmapSquare = cropToSquare2
                                    biv.radius_square = 0
                                    rect.right = rect.left + width7
                                    rect.bottom = rect.top + height6
                                    biv.rectSquare = rect
                                    cropToSquareWithRoundCorners = cropToSquare2
                                    radiusVal = 0
                                } else {
                                    radiusVal = (min * 0.10800001f).toInt()
                                    val width8 = (biv.bitmapOriginal!!.width * tmpl.width_square).toInt()
                                    val height7 = (biv.bitmapOriginal!!.height * tmpl.height_square).toInt()
                                    cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(biv.bitmapOriginal!!, rect, radiusVal, width8, height7)
                                    rect.right = rect.left + width8
                                    rect.bottom = rect.top + height7
                                    biv.rectSquare = rect
                                }
                                i2 = round5
                                i3 = round6
                            }

                            val rect3 = rect
                            val bitmap2 = cropToSquareWithRoundCorners
                            tmpl.uri_bg_ffmpeg = biv.setupBitmapDraw(
                                UtilsBitmap.blurInSave(context, bitmap!!, 20, 1, tmpl.width, tmpl.height)!!,
                                bitmap2!!,
                                tmpl
                            )
                            tmpl.squareBitmapModel!!.set(
                                biv.left_square, biv.top_square,
                                i2.toFloat(), i3.toFloat(),
                                rect3.width().toFloat(), rect3.height().toFloat(),
                                bitmap2.width.toFloat(), bitmap2.height.toFloat(),
                                0f
                            )
                        }

                        // BOTTOM_RECT, ROUND_RECT, RECT, BORDER, CASSET types:
                        // These don't go through NEOMORPHIC/IPAD paths, so they need their uri_bg_ffmpeg set here
                        if (tmpl.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal &&
                            tmpl.ipad_type != IpadType.IPAD.ordinal &&
                            tmpl.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                            tmpl.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                            tmpl.ipad_type != IpadType.HEART.ordinal &&
                            tmpl.ipad_type != IpadType.BATTERY.ordinal
                        ) {
                            tmpl.uri_bg_ffmpeg = biv.setupBitmapDraw(
                                UtilsBitmap.blurInSave(context, bitmap!!, 20, 1, tmpl.width, tmpl.height)!!,
                                cropToSquareWithRoundCorners!!,
                                tmpl
                            )
                        }
                    }

                    saveTemplate()
                    onProgress(100)
                    onSuccess(File(tmpl.idTemplate ?: ""))
                } else {
                    // ── HEART / BATTERY type — solid black background ──────────────
                    val createBitmap = Bitmap.createBitmap(tmpl.width, tmpl.height, Bitmap.Config.RGB_565)
                    createBitmap.eraseColor(android.view.View.MEASURED_STATE_MASK)
                    biv.updatePosCanvas(tmpl.width, tmpl.height, createBitmap)
                    biv.bitmapBlured = createBitmap
                    biv.updateIpad(createBitmap, tmpl.ipad_type, tmpl.geTypeResize())
                    tmpl.uri_bg_ffmpeg = biv.setupBitmapDraw(
                        createBitmap,
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                        tmpl
                    )

                    saveTemplate()
                    onProgress(100)
                    onSuccess(File(tmpl.idTemplate ?: ""))
                }
            } catch (e: Exception) {
                Log.e(TAG, "save failed", e)
                onError(e.message ?: "Export failed")
            } finally {
                isExporting = false
                onProgressHide()
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Bitmap helpers
    // ──────────────────────────────────────────────

    /**
     * Scale a bitmap so its smallest dimension equals [maxSize].
     * Mirrors `EngineActivity.setupOriginalBitmap(Bitmap, Int)`.
     */
    private fun setupOriginalBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val min = maxSize / Math.min(bitmap.width, bitmap.height).toFloat()
        return Bitmap.createScaledBitmap(
            bitmap, Math.round(bitmap.width * min), Math.round(bitmap.height * min), true
        )
    }

    // ──────────────────────────────────────────────
    //  Template serialisation
    // ──────────────────────────────────────────────

    /**
     * Fully serialise the current project state into the template and write to disk.
     *
     * Corresponds to `EngineActivity.saveTemplate()` (line ~3548).
     *
     * Iterates through all entities in [TrackEntityView] and writes their
     * properties into the Template's serialisable lists, then writes the
     * Template object to local storage via [LocalPersistence].
     * Generates a new template ID and cleans up the tmp file.
     */
    fun saveTemplate() {
        try {
            val tmpl = template ?: return
            serializeTemplate(tmpl, isTmp = false)

            // Generate new persistent template ID
            val idStr = "Template_" + System.currentTimeMillis()
            val idTemplate = tmpl.idTemplate
            tmpl.idTemplate = idStr

            // Set video output path
            tmpl.uri_video = createVideoOutputPath()

            // Write under the new key, removing the old one
            LocalPersistence.writeTemplate(context, tmpl, idTemplate ?: "", tmpl.idTemplate ?: "")
            // Clean up the temporary template
            LocalPersistence.deleteTemplate(context, Constants.TEMPLATE_TMP)
        } catch (e: Exception) {
            Log.e(TAG, "saveTemplate failed", e)
        }
    }

    /**
     * Auto-save a temporary copy of the template (called on onPause).
     *
     * Corresponds to `EngineActivity.saveTemplateTmp()` (line ~3310).
     *
     * Similar to [saveTemplate] but writes to a fixed "template_tmp" key.
     */
    fun saveTemplateTmp() {
        try {
            val tmpl = template ?: return
            if (tmpl.idTemplate == null) {
                tmpl.idTemplate = Constants.TEMPLATE_TMP
            }
            serializeTemplate(tmpl, isTmp = true)

            // Set video output path for tmp save
            tmpl.uri_video = createVideoOutputPath()

            LocalPersistence.writeTemplate(context, tmpl, Constants.TEMPLATE_TMP, Constants.TEMPLATE_TMP)
        } catch (e: Exception) {
            Log.e(TAG, "saveTemplateTmp failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  Shared serialisation core
    // ──────────────────────────────────────────────

    /**
     * Core template serialization logic shared between [saveTemplate] and [saveTemplateTmp].
     *
     * Reads all entity state from [trackViewEntity] and [blurredImageView] and
     * populates the [tmpl]'s serialisable lists. When [isTmp] is false (full save),
     * entity audio clips are released after serialization; when true (tmp save),
     * they are retained.
     *
     * @param tmpl  The template to populate
     * @param isTmp If true, this is an auto-save / tmp save; if false, full export save
     */
    private fun serializeTemplate(tmpl: Template, isTmp: Boolean) {
        val tve = trackViewEntity ?: return
        val biv = blurredImageView ?: return

        tmpl.setNewCode()
        tmpl.isGlass = biv.isGlass
        tmpl.currentCursur = tve.current_cursur_position
        tmpl.scale_timeline = tve.scaleFactor
        tmpl.duration = tve.maxTime
        tmpl.gradient = biv.color_gradient
        tmpl.color_ipad = biv.colorIpad()
        tmpl.quranEntityList.clear()
        tmpl.translationTemplateList.clear()
        tmpl.uri_bg = uriBg

        // ── Quran entities ──────────────────────────────────────────
        try {
            for (entityQuranTimeline in tve.entityListQuran) {
                if (entityQuranTimeline.visible()) {
                    val f2 = Utils.f2(kotlin.math.abs(entityQuranTimeline.rect.left / tve.second_in_screen))
                    val f22 = Utils.f2(kotlin.math.abs(entityQuranTimeline.rect.right / tve.second_in_screen))
                    if (entityQuranTimeline.quranEntity.copyRect == null) {
                        entityQuranTimeline.quranEntity.setCopyRect()
                    }
                    val entityQuranTemplate = EntityQuranTemplate(
                        transition = entityQuranTimeline.transition,
                        start = f2,
                        end = f22,
                        btm_x = entityQuranTimeline.quranEntity.copyRect!!.left * tmpl.width,
                        btm_y = tmpl.height * entityQuranTimeline.quranEntity.copyRect!!.top,
                        left = entityQuranTimeline.rect.left / entityQuranTimeline.scaleFactor,
                        right = entityQuranTimeline.rect.right / entityQuranTimeline.scaleFactor,
                        aya = entityQuranTimeline.quranEntity.txt!!,
                        complete_aya = entityQuranTimeline.quranEntity.complete_aya!!,
                        indexNumber = entityQuranTimeline.quranEntity.number,
                        number = entityQuranTimeline.quranEntity.number,
                        color = entityQuranTimeline.quranEntity.clrAya,
                        name_font = entityQuranTimeline.quranEntity.nameFont,
                        colorTrsl = if (entityQuranTimeline.quranEntity.paintTranslationAya != null)
                            entityQuranTimeline.quranEntity.clrTrsl
                        else
                            InputDeviceCompat.SOURCE_ANY,
                        preset = entityQuranTimeline.quranEntity.mPreset
                    )
                    entityQuranTemplate.height =
                        (entityQuranTimeline.quranEntity.copyRect!!.bottom * tmpl.height) -
                        (entityQuranTimeline.quranEntity.copyRect!!.top * tmpl.height)
                    entityQuranTemplate.factor_size = entityQuranTimeline.quranEntity.factorSize
                    entityQuranTemplate.factor_sizeTrl = entityQuranTimeline.quranEntity.factorSizeTrl
                    entityQuranTemplate.scale = entityQuranTimeline.quranEntity.scaleFactor
                    entityQuranTemplate.translation = entityQuranTimeline.quranEntity.translation!!
                    entityQuranTemplate.translation_complete = entityQuranTimeline.quranEntity.translation_complete!!
                    entityQuranTemplate.startWord_index = entityQuranTimeline.quranEntity.startWord_index
                    entityQuranTemplate.endWord_index = entityQuranTimeline.quranEntity.endWord_index
                    entityQuranTemplate.icon = entityQuranTimeline.quranEntity.icon!!
                    entityQuranTemplate.file = entityQuranTimeline.file
                    entityQuranTemplate.file_in = entityQuranTimeline.file_in
                    entityQuranTemplate.file_out = entityQuranTimeline.file_out
                    entityQuranTemplate.rectF = MRectF(
                        entityQuranTimeline.quranEntity.copyRect!!.left,
                        entityQuranTimeline.quranEntity.copyRect!!.top,
                        entityQuranTimeline.quranEntity.copyRect!!.right,
                        entityQuranTimeline.quranEntity.copyRect!!.bottom
                    )
                    tmpl.addQuranEntityList(entityQuranTemplate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "save template quran: ${e.message}")
        }

        // ── Translation entities ────────────────────────────────────
        try {
            for (entityTrslTimeline in tve.entityListTrslQuran) {
                if (entityTrslTimeline.visible()) {
                    val f23 = Utils.f2(kotlin.math.abs(entityTrslTimeline.rect.left / tve.second_in_screen))
                    val f24 = Utils.f2(kotlin.math.abs(entityTrslTimeline.rect.right / tve.second_in_screen))
                    if (entityTrslTimeline.quranEntity.copyRect == null) {
                        entityTrslTimeline.quranEntity.setCopyRect()
                    }
                    val entityTranslationTemplate = EntityTranslationTemplate(
                        entityTrslTimeline.transition, f23, f24,
                        entityTrslTimeline.quranEntity.copyRect!!.left * tmpl.width,
                        tmpl.height * entityTrslTimeline.quranEntity.copyRect!!.top,
                        entityTrslTimeline.rect.left / entityTrslTimeline.scaleFactor,
                        entityTrslTimeline.rect.right / entityTrslTimeline.scaleFactor,
                        entityTrslTimeline.quranEntity.txt!!,
                        entityTrslTimeline.quranEntity.nameFont,
                        entityTrslTimeline.quranEntity.number,
                        entityTrslTimeline.quranEntity.clrAya,
                        entityTrslTimeline.quranEntity.mPreset
                    )
                    entityTranslationTemplate.height =
                        (entityTrslTimeline.quranEntity.copyRect!!.bottom * tmpl.height) -
                        (entityTrslTimeline.quranEntity.copyRect!!.top * tmpl.height)
                    entityTranslationTemplate.factor_size = entityTrslTimeline.quranEntity.factorSize
                    entityTranslationTemplate.factor_sizeTrl = entityTrslTimeline.quranEntity.factorSizeTrl
                    entityTranslationTemplate.scale = entityTrslTimeline.quranEntity.scaleFactor
                    entityTranslationTemplate.file = entityTrslTimeline.file
                    entityTranslationTemplate.file_in = entityTrslTimeline.file_in
                    entityTranslationTemplate.file_out = entityTrslTimeline.file_out
                    entityTranslationTemplate.clr_bg = entityTrslTimeline.quranEntity.clrBg
                    entityTranslationTemplate.isHaveBg = entityTrslTimeline.quranEntity.isHaveBg
                    entityTranslationTemplate.rectF = MRectF(
                        entityTrslTimeline.quranEntity.copyRect!!.left,
                        entityTrslTimeline.quranEntity.copyRect!!.top,
                        entityTrslTimeline.quranEntity.copyRect!!.right,
                        entityTrslTimeline.quranEntity.copyRect!!.bottom
                    )
                    tmpl.addTrslEntityList(entityTranslationTemplate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "save template trsl quran: ${e.message}")
        }

        // ── Isti3adha (isti3ada) entity ─────────────────────────────
        tmpl.entityIsti3adaTemplate = null
        if (biv.mIsti3adhaEntity != null && biv.mIsti3adhaEntity!!.bismilahTimeline!!.visible()) {
            val bismilahTimeline = biv.mIsti3adhaEntity!!.bismilahTimeline
            val f25 = Utils.f2(kotlin.math.abs(bismilahTimeline!!.rect.left / tve.second_in_screen))
            val f26 = Utils.f2(kotlin.math.abs(bismilahTimeline!!.rect.right / tve.second_in_screen))
            if (bismilahTimeline!!.quranEntity.copyRect == null) {
                bismilahTimeline!!.quranEntity.setCopyRect()
            }
            val entityBismilahTemplate = EntityBismilahTemplate(
                bismilahTimeline!!.transition, f25, f26,
                bismilahTimeline!!.quranEntity.copyRect!!.left * tmpl.width,
                tmpl.height * bismilahTimeline!!.quranEntity.copyRect!!.top,
                bismilahTimeline!!.rect.left / bismilahTimeline!!.scaleFactor,
                bismilahTimeline!!.rect.right / bismilahTimeline!!.scaleFactor,
                bismilahTimeline!!.quranEntity.txt!!,
                bismilahTimeline!!.quranEntity.clrAya,
                bismilahTimeline!!.quranEntity.mPreset
            )
            entityBismilahTemplate.height =
                (bismilahTimeline!!.quranEntity.copyRect!!.bottom * tmpl.height) -
                (bismilahTimeline!!.quranEntity.copyRect!!.top * tmpl.height)
            entityBismilahTemplate.factor_size = bismilahTimeline!!.quranEntity.factorSize
            entityBismilahTemplate.scale = bismilahTimeline!!.quranEntity.scaleFactor
            entityBismilahTemplate.file = bismilahTimeline!!.file
            entityBismilahTemplate.file_in = bismilahTimeline!!.file_in
            entityBismilahTemplate.file_out = bismilahTimeline!!.file_out
            entityBismilahTemplate.rectF = MRectF(
                bismilahTimeline!!.quranEntity.copyRect!!.left,
                bismilahTimeline!!.quranEntity.copyRect!!.top,
                bismilahTimeline!!.quranEntity.copyRect!!.right,
                bismilahTimeline!!.quranEntity.copyRect!!.bottom
            )
            tmpl.entityIsti3adaTemplate = entityBismilahTemplate
        }

        // ── Bismilah entity ─────────────────────────────────────────
        tmpl.entityBismilahTemplate = null
        if (biv.bismilahEntity != null && biv.bismilahEntity!!.bismilahTimeline!!.visible()) {
            val bismilahTimeline2 = biv.bismilahEntity!!.bismilahTimeline
            val f27 = Utils.f2(kotlin.math.abs(bismilahTimeline2!!.rect.left / tve.second_in_screen))
            val f28 = Utils.f2(kotlin.math.abs(bismilahTimeline2!!.rect.right / tve.second_in_screen))
            if (bismilahTimeline2!!.quranEntity.copyRect == null) {
                bismilahTimeline2!!.quranEntity.setCopyRect()
            }
            val entityBismilahTemplate2 = EntityBismilahTemplate(
                bismilahTimeline2!!.transition, f27, f28,
                bismilahTimeline2!!.quranEntity.copyRect!!.left * tmpl.width,
                tmpl.height * bismilahTimeline2!!.quranEntity.copyRect!!.top,
                bismilahTimeline2!!.rect.left / bismilahTimeline2!!.scaleFactor,
                bismilahTimeline2!!.rect.right / bismilahTimeline2!!.scaleFactor,
                bismilahTimeline2!!.quranEntity.txt!!,
                bismilahTimeline2!!.quranEntity.clrAya,
                bismilahTimeline2!!.quranEntity.mPreset
            )
            entityBismilahTemplate2.height =
                (bismilahTimeline2!!.quranEntity.copyRect!!.bottom * tmpl.height) -
                (bismilahTimeline2!!.quranEntity.copyRect!!.top * tmpl.height)
            entityBismilahTemplate2.factor_size = bismilahTimeline2!!.quranEntity.factorSize
            entityBismilahTemplate2.scale = bismilahTimeline2!!.quranEntity.scaleFactor
            entityBismilahTemplate2.file = bismilahTimeline2!!.file
            entityBismilahTemplate2.file_in = bismilahTimeline2!!.file_in
            entityBismilahTemplate2.file_out = bismilahTimeline2!!.file_out
            entityBismilahTemplate2.rectF = MRectF(
                bismilahTimeline2!!.quranEntity.copyRect!!.left,
                bismilahTimeline2!!.quranEntity.copyRect!!.top,
                bismilahTimeline2!!.quranEntity.copyRect!!.right,
                bismilahTimeline2!!.quranEntity.copyRect!!.bottom
            )
            tmpl.entityBismilahTemplate = entityBismilahTemplate2
        }

        // ── Surah name entity ───────────────────────────────────────
        if (biv.surahNameEntity != null) {
            if (tmpl.entitySurahTemplate == null) {
                try {
                    if (biv.surahNameEntity!!.copyRect == null) {
                        biv.surahNameEntity!!.setCopyRect()
                    }
                    tmpl.entitySurahTemplate = EntitySurahTemplate(
                        biv.surahNameEntity!!.name,
                        biv.surahNameEntity!!.reader,
                        tmpl.mDrawingTranslationX + biv.rectFSurahName!!.left,
                        tmpl.mDrawingTranslationY + biv.rectFSurahName!!.top,
                        MRectF(
                            biv.surahNameEntity!!.copyRect!!.left,
                            biv.surahNameEntity!!.copyRect!!.top,
                            biv.surahNameEntity!!.copyRect!!.right,
                            biv.surahNameEntity!!.copyRect!!.bottom
                        ),
                        biv.surahNameEntity!!.scaleFactor,
                        biv.surahNameEntity!!.nameFont,
                        biv.surahNameEntity!!.clrS_name,
                        biv.surahNameEntity!!.mPreset,
                        biv.surahNameEntity!!.style,
                        biv.surahNameEntity!!.index_surah,
                        biv.surahNameEntity!!.isHaveBg,
                        biv.surahNameEntity!!.clrBg
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                tmpl.entitySurahTemplate!!.clrBg = biv.surahNameEntity!!.clrBg
                tmpl.entitySurahTemplate!!.isHaveBg = biv.surahNameEntity!!.isHaveBg
                tmpl.entitySurahTemplate!!.index_surah = biv.surahNameEntity!!.index_surah
                tmpl.entitySurahTemplate!!.style = biv.surahNameEntity!!.style
                tmpl.entitySurahTemplate!!.clr = biv.surahNameEntity!!.clrS_name
                tmpl.entitySurahTemplate!!.preset = biv.surahNameEntity!!.mPreset
                tmpl.entitySurahTemplate!!.name_font = biv.surahNameEntity!!.nameFont
                tmpl.entitySurahTemplate!!.factor_scale = biv.surahNameEntity!!.scaleFactor
                tmpl.entitySurahTemplate!!.rectF = MRectF(
                    biv.surahNameEntity!!.copyRect!!.left,
                    biv.surahNameEntity!!.copyRect!!.top,
                    biv.surahNameEntity!!.copyRect!!.right,
                    biv.surahNameEntity!!.copyRect!!.bottom
                )
                tmpl.entitySurahTemplate!!.name = biv.surahNameEntity!!.name
                tmpl.entitySurahTemplate!!.reader = biv.surahNameEntity!!.reader
                tmpl.entitySurahTemplate!!.setPos(
                    biv.rectFSurahName!!.left + tmpl.mDrawingTranslationX,
                    biv.rectFSurahName!!.top + tmpl.mDrawingTranslationY
                )
            }
        }

        // ── Progress overlay ────────────────────────────────────────
        if (tmpl.entityProgressTemplate == null) {
            tmpl.entityProgressTemplate = EntityProgressTemplate(
                Utils.f2(biv.rectFProgress!!.left + tmpl.mDrawingTranslationX),
                Utils.f2(biv.rectFProgress!!.top + tmpl.mDrawingTranslationY)
            )
        } else {
            tmpl.entityProgressTemplate!!.left = Utils.f2(biv.rectFProgress!!.left + tmpl.mDrawingTranslationX)
            tmpl.entityProgressTemplate!!.top = Utils.f2(biv.rectFProgress!!.top + tmpl.mDrawingTranslationY)
        }

        // ── Media / audio entities ──────────────────────────────────
        tmpl.entityMediaList.clear()
        for (entityAudio in tve.entityListAudio) {
            if (entityAudio.visible() && entityAudio.end > entityAudio.start) {
                val entityMedia = EntityMedia(
                    entityAudio.uri.toString(),
                    entityAudio.minDuration,
                    entityAudio.start,
                    entityAudio.end,
                    (entityAudio.rect.left / tve.scaleFactor).toInt(),
                    (entityAudio.rect.right / tve.scaleFactor).toFloat(),
                    Math.round(entityAudio.end - entityAudio.start).toFloat(),
                    entityAudio.offset,
                    entityAudio.getOffsetRight(),
                    entityAudio.getOffsetLeft(),
                    entityAudio.max,
                    entityAudio.effectAudio.fade_in.toFloat(),
                    entityAudio.effectAudio.fade_out.toFloat(),
                    (entityAudio.rect.left / tve.second_in_screen) * 1000.0f
                )
                entityMedia.paths_https = entityAudio.pathsHttp
                entityMedia.effectAudio = entityAudio.effectAudio
                entityMedia.path_ffmpeg = entityAudio.getPathFfmpeg()
                entityMedia.path_ffmpeg_effect = entityAudio.getPathFfmpegEffect()
                entityMedia.video_path = entityAudio.videoPath
                entityMedia.isApplyEffectInPreview = entityAudio.isApplyEffectInPreview
                tmpl.addMedia(entityMedia)
                // Release audio resources only on full export, not tmp save
                if (!isTmp) {
                    entityAudio.release()
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────

    /**
     * Reset the export guard (e.g. when returning from render).
     */
    fun resetExportFlag() {
        isExporting = false
    }

    /**
     * Build the video output path using [FileHelper].
     * Falls back to a default if the folder cannot be created.
     */
    private fun createVideoOutputPath(): String {
        return try {
            val folder = FileHelper(context).createPublicVideoFolder(appName)
            (folder?.absolutePath ?: context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.absolutePath) +
                "/" + System.currentTimeMillis() + "_NurMontage.mp4"
        } catch (e: Exception) {
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                ?.absolutePath + "/" + System.currentTimeMillis() + "_NurMontage.mp4"
        }
    }

    companion object {
        private const val TAG = "ExportPipeline"
    }
}
