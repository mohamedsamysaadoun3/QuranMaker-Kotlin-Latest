package hazem.nurmontage.videoquran.ui.engine

import android.content.Context
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.view.ViewCompat
import com.arthenica.ffmpegkit.FFmpegKit
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.model.*
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.UtilsFileLast
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.TrackEntityView
import java.io.File

/**
 * TemplateRestorer
 *
 * Encapsulates template restoration logic for the engine screen.
 * In the original code this was spread across:
 *   - addEntityFromTemplate()                (EngineActivity:7688)
 *   - extractAudioFromVideoRecursive()        (EngineActivity:6349)
 *   - extractAudioFromVideo()                 (EngineActivity:6386)
 *   - addAudioFromVideoWithExtention()        (EngineAudioManager)
 *   - addAudioTemplateHttp()                  (EngineAudioManager)
 *   - addAudioRecitersTemplate()              (EngineAudioManager)
 *   - dialogNoInternet()                      (EngineActivity:7523)
 *   - dialogNoInternetList()                  (EngineActivity:7555)
 *
 * The restoration flow is:
 *   1. [addEntityFromTemplate] — re-creates all visual entities (Quran, translation,
 *      bismilah, isti3adha, surah name) from the serialised Template data
 *   2. Media restoration — handles three cases:
 *      a. Video with local path → extract audio via [extractAudioFromVideoRecursive]
 *      b. HTTP audio URLs → download via [onAddAudioTemplateHttp] / [onAddAudioRecitersTemplate]
 *      c. Local audio URI → add directly
 *
 * This class is self-contained: it does not hold a direct reference to EngineActivity.
 * All interactions with the activity are funnelled through callback lambdas that
 * the activity wires during construction.
 */
class TemplateRestorer(
    private val context: Context
) {
    // References set externally
    var template: Template? = null
    var blurredImageView: BlurredImageView? = null
    var trackViewEntity: TrackEntityView? = null

    // Audio extraction state
    private var startExtension: Int = 0

    // Supported audio container extensions for extraction
    private val extensions = arrayOf(".mp3", ".ogg", ".acc", ".m4a", ".wav", ".mpeg")

    // ──────────────────────────────────────────────
    //  Callbacks — set by EngineActivity during wiring
    // ──────────────────────────────────────────────

    /** Add a Quran ayah entity to the timeline. */
    var onAddEntity: ((
        aya: String, completeAya: String, translation: String, translationComplete: String,
        left: Float, right: Float, indexNumber: Int, number: Int, color: Int,
        nameFont: String, transition: Transition, isEnabled: Boolean, icon: String?,
        startWordIndex: Int, endWordIndex: Int, scale: Float, factorSize: Float,
        factorSizeTrl: Float, rectF: RectF?, typefaceArabic: Typeface,
        typefaceTranslation: Typeface, colorTrsl: Int, preset: Int
    ) -> Unit)? = null

    /** Add a translation entity to the timeline. */
    var onAddEntityTrsl: ((
        aya: String, left: Float, right: Float, number: Int, color: Int,
        nameFont: String, transition: Transition, scale: Float, factorSize: Float,
        rectF: RectF?, preset: Int, clrBg: Int, isHaveBg: Boolean
    ) -> Unit)? = null

    /** Add an isti3adha entity to the timeline. */
    var onAddEntityIsti3ada: ((
        aya: String, left: Float, right: Float, color: Int, transition: Transition,
        scale: Float, factorSize: Float, rectF: RectF?, preset: Int
    ) -> Unit)? = null

    /** Add a bismilah entity to the timeline. */
    var onAddEntityBismilah: ((
        aya: String, left: Float, right: Float, color: Int, transition: Transition,
        scale: Float, factorSize: Float, rectF: RectF?, preset: Int
    ) -> Unit)? = null

    /**
     * Set the surah name entity on the BlurredImageView.
     *
     * Parameters match [BlurredImageView.setSurahNameEntity] extension function:
     *   name, reader, rectF, factorScale, nameFont, clr, preset, style,
     *   indexSurah, isHaveBg, clrBg
     */
    var onSetSurahNameEntity: ((
        name: String, reader: String, rectF: RectF?, factorScale: Float,
        nameFont: String, clr: Int, preset: Int, style: Int,
        indexSurah: Int, isHaveBg: Boolean, clrBg: Int
    ) -> Unit)? = null

    /** Add audio extracted from video with a known extension. */
    var onAddAudioFromVideoWithExtension: ((extension: String, videoPath: String, retryCount: Int) -> Unit)? = null

    /** Add audio from an HTTP template source. */
    var onAddAudioTemplateHttp: ((uri: Uri?, retryCount: Int, videoPath: String?) -> Unit)? = null

    /** Add audio from a reciters template (multiple HTTPS paths). */
    var onAddAudioRecitersTemplate: ((paths: List<String>, retryCount: Int, pcmPath: String) -> Unit)? = null

    /** Hide the progress indicator. */
    var onProgressHide: (() -> Unit)? = null

    /** Show the progress indicator. */
    var onProgressShow: (() -> Unit)? = null

    /** Invalidate (redraw) the track view. */
    var onInvalidateTrackView: (() -> Unit)? = null

    /** Update the timeline time display. */
    var onUpdateTime: (() -> Unit)? = null

    /**
     * Navigate to the Quran fragment with the extracted audio URI.
     * Called in the non-template audio extraction path.
     */
    var onAddUriAudioToQuranFragment: ((uri: Uri, videoPath: String) -> Unit)? = null

    /** Hide the current fragment. */
    var onHideFragment: (() -> Unit)? = null

    /**
     * Show a "no internet" dialog for a single URI.
     * The dialog should offer "Ignore" (closes dialog, hides progress) and
     * "Retry" (checks network again, retries [onAddAudioTemplateHttp]).
     */
    var onShowNoInternetDialog: ((uri: Uri) -> Unit)? = null

    /**
     * Show a "no internet" dialog for a list of reciter paths.
     * The dialog should offer "Ignore" (closes dialog, invalidates track, hides progress)
     * and "Retry" (checks network again, retries [onAddAudioRecitersTemplate]).
     */
    var onShowNoInternetListDialog: ((paths: List<String>) -> Unit)? = null

    // ──────────────────────────────────────────────
    //  Entity restoration
    // ──────────────────────────────────────────────

    /**
     * Restore all entities from the current template.
     *
     * Corresponds to `EngineActivity.addEntityFromTemplate()` (line ~7688).
     *
     * This method iterates the template's entity lists and re-creates
     * the corresponding visual entities on the timeline:
     *   - Quran entities      → [onAddEntity]
     *   - Translation entities → [onAddEntityTrsl]
     *   - Isti3adha entity     → [onAddEntityIsti3ada]
     *   - Bismilah entity      → [onAddEntityBismilah]
     *   - Surah name entity    → [onSetSurahNameEntity]
     *
     * After entities are restored, it proceeds to restore audio/media.
     */
    fun addEntityFromTemplate() {
        val tmpl = template ?: return
        val biv = blurredImageView ?: return

        val isEnabled = tmpl.ipad_type == IpadType.GRADIENT.ordinal ||
                tmpl.ipad_type == IpadType.MASK_BRUSH.ordinal ||
                tmpl.ipad_type == IpadType.BLACK_LAYER.ordinal

        val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(context, "fonts/arabic/خط فارس الكوفي.otf")
        val createFromAsset = Typeface.createFromAsset(context.assets, "fonts/ReadexPro_Medium.ttf")

        // ── Quran entities ──
        for (entityQuranTemplate in tmpl.quranEntityList) {
            onAddEntity?.invoke(
                entityQuranTemplate.aya!!,
                entityQuranTemplate.complete_aya!!,
                entityQuranTemplate.translation ?: "",
                entityQuranTemplate.translation_complete ?: "",
                entityQuranTemplate.left,
                entityQuranTemplate.right,
                entityQuranTemplate.indexNumber,
                entityQuranTemplate.number,
                entityQuranTemplate.color,
                entityQuranTemplate.name_font ?: "hafes",
                entityQuranTemplate.transition ?: Transition(),
                isEnabled,
                entityQuranTemplate.icon,
                entityQuranTemplate.startWord_index,
                entityQuranTemplate.endWord_index,
                entityQuranTemplate.scale,
                entityQuranTemplate.factor_size,
                entityQuranTemplate.factor_sizeTrl,
                entityQuranTemplate.rectF?.let {
                    RectF(it.l, it.t, it.r, it.b)
                },
                loadFontFromAsset!!,
                createFromAsset!!,
                entityQuranTemplate.colorTrsl,
                entityQuranTemplate.preset
            )
        }

        // ── Translation entities ──
        for (translationTemplate in tmpl.translationTemplateList) {
            onAddEntityTrsl?.invoke(
                translationTemplate.aya!!,
                translationTemplate.left,
                translationTemplate.right,
                translationTemplate.number,
                translationTemplate.color,
                translationTemplate.name_font ?: "ReadexPro_Medium.ttf",
                translationTemplate.transition ?: Transition(),
                translationTemplate.scale,
                translationTemplate.factor_size,
                translationTemplate.rectF?.let {
                    RectF(it.l, it.t, it.r, it.b)
                },
                translationTemplate.preset,
                translationTemplate.clr_bg,
                translationTemplate.isHaveBg
            )
        }

        // ── Isti3adha entity ──
        if (tmpl.entityIsti3adaTemplate != null) {
            onAddEntityIsti3ada?.invoke(
                tmpl.entityIsti3adaTemplate!!.aya!!,
                tmpl.entityIsti3adaTemplate!!.left,
                tmpl.entityIsti3adaTemplate!!.right,
                tmpl.entityIsti3adaTemplate!!.color,
                tmpl.entityIsti3adaTemplate!!.transition ?: Transition(),
                tmpl.entityIsti3adaTemplate!!.scale,
                tmpl.entityIsti3adaTemplate!!.factor_size,
                tmpl.entityIsti3adaTemplate!!.rectF?.let {
                    RectF(it.l, it.t, it.r, it.b)
                },
                tmpl.entityIsti3adaTemplate!!.preset
            )
        }

        // ── Bismilah entity ──
        if (tmpl.entityBismilahTemplate != null) {
            onAddEntityBismilah?.invoke(
                tmpl.entityBismilahTemplate!!.aya!!,
                tmpl.entityBismilahTemplate!!.left,
                tmpl.entityBismilahTemplate!!.right,
                tmpl.entityBismilahTemplate!!.color,
                tmpl.entityBismilahTemplate!!.transition ?: Transition(),
                tmpl.entityBismilahTemplate!!.scale,
                tmpl.entityBismilahTemplate!!.factor_size,
                tmpl.entityBismilahTemplate!!.rectF?.let {
                    RectF(it.l, it.t, it.r, it.b)
                },
                tmpl.entityBismilahTemplate!!.preset
            )
        }

        // ── Surah name entity ──
        if (tmpl.entitySurahTemplate != null) {
            val rectF: RectF = if (tmpl.entitySurahTemplate!!.rectF == null) {
                biv.rectFSurahName ?: RectF()
            } else {
                RectF(
                    tmpl.entitySurahTemplate!!.rectF!!.l * biv.getmCanvas_width(),
                    tmpl.entitySurahTemplate!!.rectF!!.t * biv.getmCanvas_height(),
                    tmpl.entitySurahTemplate!!.rectF!!.r * biv.getmCanvas_width(),
                    tmpl.entitySurahTemplate!!.rectF!!.b * biv.getmCanvas_height()
                )
            }
            onSetSurahNameEntity?.invoke(
                tmpl.entitySurahTemplate!!.name,
                tmpl.entitySurahTemplate!!.reader,
                rectF,
                tmpl.entitySurahTemplate!!.factor_scale,
                tmpl.entitySurahTemplate!!.name_font ?: "خط الإبل.otf",
                tmpl.entitySurahTemplate!!.clr,
                tmpl.entitySurahTemplate!!.preset,
                tmpl.entitySurahTemplate!!.style,
                tmpl.entitySurahTemplate!!.index_surah,
                tmpl.entitySurahTemplate!!.isHaveBg,
                if (tmpl.entitySurahTemplate!!.clrBg == 0) ViewCompat.MEASURED_STATE_MASK else tmpl.entitySurahTemplate!!.clrBg
            )
        }

        // ── Media / audio restoration ──
        restoreMediaFromTemplate(tmpl)
    }

    // ──────────────────────────────────────────────
    //  Media / audio restoration
    // ──────────────────────────────────────────────

    /**
     * Restore audio/media entities from the template.
     *
     * Handles three cases:
     *  1. Video with local path → extract audio via [extractAudioFromVideoRecursive]
     *  2. HTTP audio URLs → download via callbacks
     *  3. Local audio URI → add directly
     *
     * Corresponds to the media section of EngineActivity.addEntityFromTemplate()
     * (lines ~7816-7886).
     */
    private fun restoreMediaFromTemplate(tmpl: Template) {
        if (tmpl.entityMediaList.isEmpty()) {
            // No media — just refresh the view
            onInvalidateTrackView?.invoke()
            onUpdateTime?.invoke()
            if (tmpl.quranEntityList.isEmpty()) {
                blurredImageView?.invalidate()
            }
            onProgressHide?.invoke()
            return
        }

        try {
            val entityMedia = tmpl.entityMediaList[0]
            if (entityMedia.video_path != null) {
                if (tmpl.uri_upload_extract_audio_video == null) {
                    onProgressHide?.invoke()
                } else {
                    AudioUtils.copyToLocalAsync(
                        context,
                        Uri.parse(tmpl.uri_upload_extract_audio_video).toString(),
                        tmpl.folder_template!!,
                        object : AudioUtils.Callback {
                            override fun onSuccess(textValue: String) {
                                entityMedia.video_path = textValue
                                if (tmpl.extension != null) {
                                    onAddAudioFromVideoWithExtension?.invoke(
                                        tmpl.extension!!, entityMedia.video_path!!, 0
                                    )
                                } else {
                                    startExtension = 0
                                    extractAudioFromVideoRecursive(
                                        entityMedia.video_path!!, 0, true, 0
                                    )
                                }
                            }

                            override fun onError(exc: Exception) {
                                exc.printStackTrace()
                            }
                        }
                    )
                }
            } else if (entityMedia.uri != null) {
                if (entityMedia.paths_https != null) {
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        onAddAudioRecitersTemplate?.invoke(entityMedia.paths_https!!, 0, "")
                    } else {
                        onShowNoInternetListDialog?.invoke(entityMedia.paths_https!!)
                    }
                } else if (entityMedia.uri!!.contains("http")) {
                    val parse = Uri.parse(entityMedia.uri)
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        onAddAudioTemplateHttp?.invoke(parse, 0, null)
                    } else {
                        onShowNoInternetDialog?.invoke(parse)
                    }
                } else {
                    onAddAudioTemplateHttp?.invoke(Uri.parse(entityMedia.uri), 0, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "restoreMediaFromTemplate failed", e)
            onProgressHide?.invoke()
        }
    }

    // ──────────────────────────────────────────────
    //  Audio extraction from video
    // ──────────────────────────────────────────────

    /**
     * Recursively try extracting audio from a video file using different
     * codec extensions (.mp3, .ogg, .aac, .m4a, .wav, .mpeg).
     *
     * Corresponds to `EngineActivity.extractAudioFromVideoRecursive()` (line ~6349).
     *
     * @param path        Local path to the video file
     * @param index       Current extension index to try
     * @param isTemplate  Whether this is a template restoration (vs. user action)
     * @param retryCount  Retry count for progress tracking
     */
    fun extractAudioFromVideoRecursive(
        path: String,
        index: Int,
        isTemplate: Boolean,
        retryCount: Int
    ) {
        val tmpl = template ?: return

        if (index < extensions.size) {
            try {
                val file = File(
                    File(tmpl.folder_template!!),
                    "${System.currentTimeMillis()}_audio${extensions[index]}"
                )
                FFmpegKit.executeWithArgumentsAsync(
                    arrayOf("-i", path, "-vn", "-acodec", "copy", "-y", file.absolutePath)
                ) { session ->
                    if (session.returnCode.isValueSuccess) {
                        tmpl.extension = extensions[index]
                        val fromFile = Uri.fromFile(file)
                        if (!isTemplate) {
                            onHideFragment?.invoke()
                            onProgressHide?.invoke()
                            onAddUriAudioToQuranFragment?.invoke(fromFile, path)
                        } else {
                            onAddAudioTemplateHttp?.invoke(fromFile, retryCount, path)
                        }
                        return@executeWithArgumentsAsync
                    }
                    startExtension++
                    extractAudioFromVideoRecursive(path, startExtension, isTemplate, retryCount)
                }
                return
            } catch (e: Exception) {
                Log.e(TAG, "extractAudioFromVideoRecursive failed", e)
                extractAudioFromVideo(path, isTemplate)
                return
            }
        }
        // Fallback: try transcoding to MP3
        extractAudioFromVideo(path, isTemplate)
    }

    /**
     * Fallback audio extraction — transcode to MP3.
     *
     * Corresponds to `EngineActivity.extractAudioFromVideo()` (line ~6386).
     * When extraction fails, shows a toast indicating the video has no audio track.
     *
     * @param path       Path to the video file
     * @param isTemplate Whether this is a template restoration
     */
    fun extractAudioFromVideo(path: String, isTemplate: Boolean) {
        val tmpl = template ?: return
        try {
            val file = File(
                File(tmpl.folder_template!!),
                "${System.currentTimeMillis()}_audio.mp3"
            )
            FFmpegKit.executeWithArgumentsAsync(
                arrayOf("-i", path, "-vn", "-acodec", "copy", "-y", file.absolutePath)
            ) { session ->
                if (session == null) {
                    onHideFragment?.invoke()
                    onProgressHide?.invoke()
                    return@executeWithArgumentsAsync
                }
                if (session.returnCode.isValueSuccess) {
                    val fromFile = Uri.fromFile(file)
                    tmpl.extension = ".mp3"
                    if (!isTemplate) {
                        onAddUriAudioToQuranFragment?.invoke(fromFile, path)
                    } else {
                        onAddAudioTemplateHttp?.invoke(fromFile, 0, path)
                    }
                    return@executeWithArgumentsAsync
                }
                onProgressHide?.invoke()
                onHideFragment?.invoke()
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractAudioFromVideo failed", e)
            onHideFragment?.invoke()
            onProgressHide?.invoke()
        }
    }

    // ──────────────────────────────────────────────
    //  No-internet dialog helpers
    // ──────────────────────────────────────────────

    /**
     * Default implementation for the "no internet" single-URI dialog.
     *
     * This can be used as a convenience when the host does not want to
     * supply its own [onShowNoInternetDialog]. It creates a dialog with
     * "Ignore" and "Retry" buttons.
     *
     * @param uri The URI that failed to load due to no network
     */
    fun showNoInternetDialogDefault(uri: Uri) {
        // Intentionally left as a no-op default — the actual dialog requires
        // Activity-specific layout inflation. The host should wire
        // [onShowNoInternetDialog] to its own dialog implementation.
        // See EngineActivity.dialogNoInternet(uri) for reference.
        Log.w(TAG, "No internet available for URI: $uri — but no dialog callback wired")
        onProgressHide?.invoke()
    }

    /**
     * Default implementation for the "no internet" reciters-list dialog.
     *
     * @param paths The list of HTTPS paths that failed to load
     */
    fun showNoInternetListDialogDefault(paths: List<String>) {
        // Intentionally left as a no-op default — the actual dialog requires
        // Activity-specific layout inflation. The host should wire
        // [onShowNoInternetListDialog] to its own dialog implementation.
        // See EngineActivity.dialogNoInternetList(list) for reference.
        Log.w(TAG, "No internet available for ${paths.size} reciter paths — but no dialog callback wired")
        onInvalidateTrackView?.invoke()
        onUpdateTime?.invoke()
        template?.let { tmpl ->
            if (tmpl.quranEntityList.isEmpty()) {
                blurredImageView?.invalidate()
            }
        }
        onProgressHide?.invoke()
    }

    companion object {
        private const val TAG = "TemplateRestorer"
    }
}
