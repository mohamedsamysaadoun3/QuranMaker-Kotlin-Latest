package hazem.nurmontage.videoquran.ui.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.TransitionType
import hazem.nurmontage.videoquran.model.EntityBismilahTemplate
import hazem.nurmontage.videoquran.model.EntityMedia
import hazem.nurmontage.videoquran.model.EntityQuranTemplate
import hazem.nurmontage.videoquran.model.EntityTranslationTemplate
import hazem.nurmontage.videoquran.model.RenderManager
import hazem.nurmontage.videoquran.model.SquareBitmapModel
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.model.Transition
import hazem.nurmontage.videoquran.utils.ColorUtils
import hazem.nurmontage.videoquran.utils.audio.FfmpegCodecChecker
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * FFmpeg export command builder for the ProgressViewActivity.
 *
 * This object encapsulates the video export pipeline that was originally
 * embedded in ProgressViewActivity.java (~3000 lines). The commands are
 * preserved EXACTLY to ensure identical rendering output.
 *
 * The export pipeline has multiple stages:
 * 1. **Pre-render video segments** — crop, scale, apply masks (rounded/circle)
 * 2. **Generate timer overlay** — drawtext with elapsed/remaining time
 * 3. **Build overlay filter_complex** — overlay all entities (bismilah, quran, translation, media)
 * 4. **Apply fade/slide transitions** — per-entity transition effects
 * 5. **Mix audio tracks** — amix for media audio overlays
 * 6. **Final encode** — produce output MP4 with proper codec/bitrate
 *
 * All FFmpeg command strings and filter chains are treated as sacred.
 */
object ExportCommandBuilder {

    /**
     * Stores the last overlay filter_complex string built by [buildCommand].
     * Used by [ProgressViewActivity.showError] to include the filter chain
     * in the bug report, matching the original Java's `this.overlay` field.
     */
    var lastOverlayFilter: String = ""
        private set

    // ════════════════════════════════════════════════════════════════════
    //  Fade filter builders — preserved from ProgressViewActivity.java
    // ════════════════════════════════════════════════════════════════════

    fun mFadeFilter(startTime: Float, duration: Float, isIn: Boolean): String {
        val safeDuration = if (duration - 0.05f <= 0f) 0.01f else duration
        val direction = if (isIn) "in" else "out"
        return "fade=t=$direction:st=${abs(startTime)}:d=${abs(safeDuration)}:alpha=1:color=white,fps=60,format=rgba"
    }

    fun fadeInOut(endTime: Float, fadeInDur: Float, fadeOutDur: Float, fps: Int = 60): String {
        val safeFadeIn = if (fadeInDur <= 0f) 0.01f else fadeInDur
        val safeFadeOutDur = if (fadeOutDur - 0.05f <= 0f) 0.01f else fadeOutDur
        val safeEnd = if (endTime - 0.05f <= 0f) 0.01f else endTime
        return "fade=t=in:st=0:d=${abs(safeFadeIn)}:alpha=1:color=white,fps=$fps,format=rgba," +
               "fade=t=out:st=${abs(safeEnd)}:d=${abs(safeFadeOutDur)}:alpha=1:color=white,fps=$fps,format=rgba"
    }

    fun fadeFilter(label: String, index: Int, startTime: Float, duration: Float, isIn: Boolean): String {
        val direction = if (isIn) "in" else "out"
        return "${label}fade=t=$direction:st=$startTime:d=${abs(duration - 0.05f)}:alpha=1:color=white,fps=60,format=rgba[${direction}_$index];"
    }

    fun fadeFilter(label: String, startTime: Float, duration: Float, isIn: Boolean): String {
        val direction = if (isIn) "in" else "out"
        return "[$label]fade=t=$direction:st=$startTime:d=${abs(duration - 0.05f)}:alpha=1:color=white,fps=60,format=rgba[${direction}_$label];"
    }

    fun fadeFilter(index: Int, startTime: Float, duration: Float, isIn: Boolean): String {
        val direction = if (isIn) "in" else "out"
        return "[$index]fade=t=$direction:st=$startTime:d=${abs(duration - 0.05f)}:alpha=1:color=white,fps=60,format=rgba[${direction}_$index];"
    }

    // ════════════════════════════════════════════════════════════════════
    //  Slide animation expression builders
    // ════════════════════════════════════════════════════════════════════

    fun slideX(start: Float, duration: Float, offset: Float, scale: Float, from: Float, to: Float): String {
        val t = "clip((t-$start)/$duration,0,1)"
        val smooth = "($t*$t*(3-2*$t))"
        val diff = to - from
        return "'$offset+((${from}+(${diff})*$smooth)*$scale)'"
    }

    fun mSlideX(start: Float, duration: Float, offset: Float, scale: Float, from: Float, to: Float): String {
        val t = "clip((t-$start)/$duration,0,1)"
        val smooth = "($t*$t*(3-2*$t))"
        val diff = to - from
        return "$offset+((${from}+(${diff})*$smooth)*$scale)"
    }

    // ════════════════════════════════════════════════════════════════════
    //  Mask generation — rounded rect and circle
    // ════════════════════════════════════════════════════════════════════

    fun getOrCreateMask(width: Int, height: Int, radius: Int, filesDir: File): File {
        val file = File(filesDir, "mask_${width}x${height}_r$radius.png")
        if (file.exists()) return file

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1 }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius.toFloat(), radius.toFloat(), paint)

        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }

    fun getOrCreateMaskCircle(width: Int, height: Int, templateDir: String): File {
        val file = File(templateDir, "circle_${width}x${height}.png")

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1 }
        canvas.drawCircle(width / 2.0f, height / 2.0f, min(width, height) / 2.0f, paint)

        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }

    fun createTransparentBg(width: Int, height: Int, templateDir: String): File {
        val file = File(templateDir, "bg_tr_.png")
        if (file.exists()) return file

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return file
    }

    // ════════════════════════════════════════════════════════════════════
    //  Pre-render command builders
    // ════════════════════════════════════════════════════════════════════

    fun preRenderMaskRoundedArgs(
        template: Template,
        model: SquareBitmapModel,
        durationMs: Int,
        filesDir: File
    ): Triple<Array<String>, String, File> {
        val outputPath = "${template.folder_template}/rounded_${System.currentTimeMillis()}.mov"
        val maxSize = max(template.width, template.height)
        val right = Math.round(model.right)
        val bottom = Math.round(model.bottom)
        val left = Math.round(model.lef_square)
        val top = Math.round(model.top_square)
        var w = Math.round(model.width_sqaure)
        var h = Math.round(model.height_square)
        if ((w and 1) == 1) w++
        if ((h and 1) == 1) h++

        val maskFile = getOrCreateMask(w, h, model.raduis.toInt(), filesDir)
        val filterComplex = "[0:v]scale=$maxSize:$maxSize:force_original_aspect_ratio=increase," +
                           "crop=$right:$bottom:$left:$top," +
                           "scale=$w:$h:flags=lanczos[v];[v][1:v]alphamerge,format=rgba"

        val args = buildPreRenderArgs(template.uri_media_video, maskFile.absolutePath,
            filterComplex, durationMs, outputPath, isAlpha = true, codec = null, template.fps)
        return Triple(args, outputPath, maskFile)
    }

    fun preRenderMaskCircleArgs(
        template: Template,
        model: SquareBitmapModel,
        durationMs: Int,
        filesDir: File
    ): Pair<Array<String>, String> {
        val outputPath = "${template.folder_template}/circle_${System.currentTimeMillis()}.mov"
        val maxSize = max(template.width, template.height)
        val right = Math.round(model.right)
        val bottom = Math.round(model.bottom)
        val left = Math.round(model.lef_square)
        val top = Math.round(model.top_square)
        var w = Math.round(model.width_sqaure)
        var h = Math.round(model.height_square)
        if ((w and 1) == 1) w++
        if ((h and 1) == 1) h++

        val maskFile = getOrCreateMaskCircle(w, h, template.folder_template ?: filesDir.absolutePath)
        val filterComplex = "[0:v]scale=$maxSize:$maxSize:force_original_aspect_ratio=increase," +
                           "crop=$right:$bottom:$left:$top," +
                           "scale=$w:$h:flags=lanczos[v];[v][1:v]alphamerge,format=rgba"

        val args = buildPreRenderArgs(template.uri_media_video, maskFile.absolutePath,
            filterComplex, durationMs, outputPath, isAlpha = true, codec = null, template.fps)
        return Pair(args, outputPath)
    }

    fun preRenderNoMaskArgs(
        template: Template,
        model: SquareBitmapModel,
        durationMs: Int,
        codec: String?
    ): Pair<Array<String>, String> {
        val outputPath = "${template.folder_template}/nomask_${System.currentTimeMillis()}.mp4"
        val maxSize = max(template.width, template.height)
        val right = Math.round(model.right)
        val bottom = Math.round(model.bottom)
        val left = Math.round(model.lef_square)
        val top = Math.round(model.top_square)
        var w = Math.round(model.width_sqaure)
        var h = Math.round(model.height_square)
        if ((w and 1) == 1) w++
        if ((h and 1) == 1) h++

        val filterComplex = "scale=$maxSize:$maxSize:force_original_aspect_ratio=increase," +
                           "crop=$right:$bottom:$left:$top," +
                           "scale=$w:$h:flags=lanczos,format=yuv420p"

        val args = buildPreRenderArgs(template.uri_media_video, null,
            filterComplex, durationMs, outputPath, isAlpha = false, codec = codec, template.fps)
        return Pair(args, outputPath)
    }

    fun preRenderVideoArgs(
        template: Template,
        durationMs: Int,
        codec: String?
    ): Pair<Array<String>?, String> {
        val outputPath = "${template.folder_template}/layer_video_${System.currentTimeMillis()}.mp4"
        val maxSize = max(template.width, template.height)
        val filterComplex = "[0:v]scale=$maxSize:$maxSize:force_original_aspect_ratio=increase:flags=lanczos," +
                           "crop=${template.width}:${template.height}:" +
                           "(iw-${template.width})/2:(ih-${template.height})/2[v];" +
                           "[v][1:v]overlay,format=rgba"

        val bgFile = File(template.uri_bg_ffmpeg ?: "")
        if (!bgFile.exists() || !bgFile.isFile) {
            return Pair(null, outputPath)
        }

        val args = arrayListOf(
            "-hide_banner", "-y",
            "-stream_loop", "-1",
            "-i", template.uri_media_video ?: "",
            "-i", template.uri_bg_ffmpeg!!,
            "-filter_complex", filterComplex
        )

        if (codec != null) {
            args.addAll(listOf("-threads", "0", "-c:v", codec, "-preset", "fast", "-crf", "18"))
        } else {
            args.addAll(listOf("-b:v", "4M"))
        }

        args.addAll(listOf(
            "-r", template.fps.toString(),
            "-t", "${max(durationMs, 500)}ms",
            "-movflags", "+faststart",
            "-an",
            outputPath
        ))

        return Pair(args.toTypedArray(), outputPath)
    }

    /**
     * Build pre-render args for video with hue adjustment AND progress bar overlay.
     * This is the full preRenderVideoHue from the original that includes the
     * progress bar (line_progress/line_bg) overlay with cosine-based animation.
     */
    fun preRenderVideoHueArgs(
        template: Template,
        durationMs: Int,
        codec: String?
    ): Pair<Array<String>?, String> {
        val uriMediaVideo = template.uri_media_video ?: return Pair(null, "")
        val outputPath = "${template.folder_template}/layer_video_${System.currentTimeMillis()}.mp4"
        val maxSize = max(template.width, template.height)
        val width = template.width
        val height = template.height
        val timeModel = template.mTimeModel ?: return Pair(null, "")
        val progressTemplate = template.entityProgressTemplate ?: return Pair(null, "")

        val filterComplex = "[0:v]scale=$maxSize:$maxSize:force_original_aspect_ratio=increase:flags=lanczos," +
            "hue=s=0,crop=$width:$height:(iw-$width)/2:(ih-$height)/2[main];" +
            "[main][1]overlay[fm];" +
            "[2:v]loop=loop=-1:size=1:start=0,setpts=N/FRAME_RATE/TB[lineProg];" +
            "[3:v]loop=loop=-1:size=1:start=0,setpts=N/FRAME_RATE/TB[lineBg];" +
            "[lineProg][lineBg]overlay=x=" +
            (-timeModel.width_bitmap_progress) +
            " + ((cos((t / (" + (durationMs / 1000.0) + ") + 1) * PI) / 2 + 0.5) * " +
            (timeModel.width_bitmap_progress - timeModel.progress_offset) + ")" +
            ":y=0[bgApplied];" +
            "[fm][bgApplied]overlay=${progressTemplate.left}:${progressTemplate.top}"

        val bgPath = template.uri_bg_ffmpeg
        if (bgPath == null || !File(bgPath).let { it.exists() && it.isFile }) {
            return Pair(null, outputPath)
        }

        val args = arrayListOf(
            "-hide_banner", "-y",
            "-i", uriMediaVideo,
            "-i", bgPath,
            "-i", "${template.folder_template}/line_progress.png",
            "-i", "${template.folder_template}/line_bg.png",
            "-filter_complex", filterComplex
        )

        if (codec != null) {
            args.addAll(listOf("-c:v", codec, "-preset", "fast", "-crf", "18"))
        } else {
            args.addAll(listOf("-c:v", "libx264", "-preset", "veryfast", "-crf", "18"))
        }

        args.addAll(listOf(
            "-r", template.fps.toString(),
            "-t", "${max(durationMs, 500)}ms",
            "-movflags", "+faststart",
            "-an",
            outputPath
        ))

        return Pair(args.toTypedArray(), outputPath)
    }

    fun generateVideoTimerArgs(
        template: Template,
        durationMs: Int
    ): Pair<Array<String>, String> {
        val outputPath = "${template.folder_template}/timer.mov"
        val maxSeconds = max(durationMs / 1000, 1)
        val timeModel = template.mTimeModel ?: return Pair(emptyArray(), "")
        val fontPath = "${template.folder_template}/NotoNaskhArabic.ttf"
        val bgColor = if (ColorUtils.isColorDark(Color.parseColor(timeModel.color))) "black@0" else "white@0"
        val value2 = maxSeconds + 1

        val args = arrayOf(
            "-y",
            "-f", "lavfi",
            "-i", "color=size=${Math.round(timeModel.width_bitmap_progress * 1.3f)}x${timeModel.height_bitmap_progress}:rate=10:duration=$maxSeconds:color=$bgColor,format=rgba",
            "-vf", "drawtext=fontfile='$fontPath':text='%{eif\\:trunc(t/60)\\:d\\:2}\\:%{eif\\:trunc(mod(t\\,60))\\:d\\:2}':x=0.0:y=0.0:fontsize=${timeModel.size}:fontcolor=${timeModel.color}," +
                    "drawtext=fontfile='$fontPath':text='-%{eif\\:trunc(($value2-t)/60)\\:d\\:2}\\:%{eif\\:trunc(mod($value2-t\\,60))\\:d\\:2}':x=${timeModel.posXRight}:y=0.0:fontsize=${timeModel.size}:fontcolor=${timeModel.color}",
            "-c:v", "qtrle",
            "-pix_fmt", "argb",
            "-preset", "veryfast",
            "-avoid_negative_ts", "make_zero",
            outputPath
        )

        return Pair(args, outputPath)
    }

    /**
     * Generate a pre-rendered video segment from an ayah/bismilah image with filter applied.
     * Used for transition animations (fade in/out, slide).
     */
    fun generateVideoSegmentArgs(
        template: Template,
        entityFile: String,
        outputName: String,
        filter: String,
        durationSec: Int,
        index: Int
    ): Pair<Array<String>, String> {
        val filePath = "${template.folder_template}/${outputName}_$index.mov"
        val args = arrayOf(
            "-y",
            "-loop", "1",
            "-i", "${template.folder_template}/$entityFile",
            "-vf", filter,
            "-t", "${max(durationSec, 1)}",
            "-c:v", "qtrle",
            "-pix_fmt", "argb",
            "-preset", "veryfast",
            "-avoid_negative_ts", "make_zero",
            filePath
        )
        return Pair(args, filePath)
    }

    // ════════════════════════════════════════════════════════════════════
    //  Helper: build pre-render args
    // ════════════════════════════════════════════════════════════════════

    private fun buildPreRenderArgs(
        inputVideo: String?,
        maskPath: String?,
        filterComplex: String,
        durationMs: Int,
        outputPath: String,
        isAlpha: Boolean,
        codec: String?,
        fps: Int = 25
    ): Array<String> {
        val args = mutableListOf("-hide_banner", "-y", "-stream_loop", "-1", "-i", inputVideo ?: "")

        if (maskPath != null) {
            args.addAll(listOf("-i", maskPath))
        }

        args.addAll(listOf("-filter_complex", filterComplex))

        when {
            isAlpha -> {
                args.addAll(listOf("-c:v", "qtrle", "-pix_fmt", "rgba"))
            }
            codec != null -> {
                args.addAll(listOf("-threads", "0", "-c:v", codec, "-preset", "fast", "-crf", "18"))
            }
            else -> {
                args.addAll(listOf("-b:v", "4M"))
            }
        }

        args.addAll(listOf("-r", fps.toString(), "-t", "${max(durationMs, 500)}ms"))

        if (!isAlpha) {
            args.addAll(listOf("-movflags", "+faststart"))
        }

        args.add(outputPath)

        return args.toTypedArray()
    }

    fun buildAacTestArgs(outputPath: String): Array<String> {
        return arrayOf(
            "-y", "-f", "lavfi",
            "-i", "anullsrc=channel_layout=stereo:sample_rate=44100",
            "-t", "1", "-c:a", "aac", "-b:a", "64k",
            outputPath
        )
    }

    // ════════════════════════════════════════════════════════════════════
    //  Full pipeline builder — builds the complete FFmpeg command
    //  This is the main method that mirrors setupCommand() from the original
    // ════════════════════════════════════════════════════════════════════

    /**
     * Build the complete FFmpeg export command.
     *
     * This method replicates the original setupCommand() from ProgressViewActivity.java.
     * It builds a monolithic filter_complex that overlays all entities:
     * - Background image/video
     * - Timer overlay
     * - Progress bar (line_progress/line_bg with cosine animation)
     * - Video square (masked or unmasked based on IpadType)
     * - Bismilah and Isti3ada with transitions
     * - Quran ayahs with transitions
     * - Translations with enable-based overlays
     * - Media audio mixing
     *
     * @param template The project template
     * @param codecInfo Detected FFmpeg codecs
     * @param preRenderExecutor Callback to execute pre-render FFmpeg commands and return output paths
     * @return The complete FFmpeg command array, or null on failure
     */
    fun buildCommand(
        template: Template,
        codecInfo: FfmpegCodecChecker.CodecInfo,
        preRenderExecutor: PreRenderExecutor
    ): Array<String>? {
        val durationMs = max(template.duration, 1000)
        val folder = template.folder_template ?: return null
        val ipadType = IpadType.entries[template.ipad_type.coerceIn(0, IpadType.entries.size - 1)]

        val args = mutableListOf<String>()
        val overlay = StringBuilder()

        val durationSec = durationMs / 1000
        val videoCodec = codecInfo.videoCodec
        val semaphore = Semaphore(4)

        renderManager.addTask("Video prerender", durationSec)

        args.add("-hide_banner")

        // ── IpadType-specific setup ──────────────────────────────────────
        val isIpadType = ipadType == IpadType.IPAD || ipadType == IpadType.IPAD_UNBLUR ||
                         ipadType == IpadType.IPAD_CLASSIC || ipadType == IpadType.IPAD_NEOMORPHIC ||
                         ipadType == IpadType.ROUND_RECT || ipadType == IpadType.BOTTOM_RECT ||
                         ipadType == IpadType.RECT

        var inputIndex = 0
        val countDownLatch: CountDownLatch

        if (isIpadType) {
            val bgFile = File(template.uri_bg_ffmpeg ?: "")
            if (bgFile.exists() && bgFile.isFile) {
                val totalEntities = template.quranEntityList.size +
                    template.translationTemplateList.size +
                    (if (template.isVideoSquare) 1 else 0) + 1 +
                    (if (template.entityBismilahTemplate != null) 1 else 0) +
                    (if (template.entityIsti3adaTemplate != null) 1 else 0)
                countDownLatch = CountDownLatch(totalEntities)

                // Add background image
                args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))

                // Generate timer overlay
                val timerPath = preRenderExecutor.executeGenerateTimer(durationMs, countDownLatch, semaphore)
                if (timerPath == null) {
                    // Timer pre-render failed (e.g. missing font file in template folder).
                    // Previously this used an empty string for the -i argument which caused
                    // FFmpeg to fail silently.  Now we fail fast so the caller can report
                    // the error properly.
                    return null
                }
                args.addAll(listOf("-i", timerPath))

                // Overlay timer on background
                val timeModel = template.mTimeModel ?: return null
                val progressTemplate = template.entityProgressTemplate ?: return null
                overlay.append("[0][1]overlay=${progressTemplate.left}:${timeModel.posY + progressTemplate.top}[bg];")

                // Add line_progress image
                args.addAll(listOf("-i", "$folder/line_progress.png"))
                overlay.append("[2:v]loop=loop=-1:size=1:start=0,format=rgba[lp];")

                // Add line_bg image
                args.addAll(listOf("-i", "$folder/line_bg.png"))

                if (ipadType == IpadType.IPAD_NEOMORPHIC) {
                    overlay.append("[lp][3]overlay=x='(-${timeModel.width_bitmap_progress}*(1-clip(t/($durationSec,0,1))))':y=0:shortest=0[tmp2];")
                    args.addAll(listOf("-i", "$folder/line_bg_tmp.png"))
                    overlay.append("[bg][tmp2]overlay=${progressTemplate.left}:${progressTemplate.top}[ps];")
                    inputIndex = 4
                    overlay.append("[ps][4]overlay='if(lte(t,0),-100,${progressTemplate.left})':${progressTemplate.top}[ov4];")
                } else {
                    val widthProgress = timeModel.width_bitmap_progress - timeModel.progress_offset
                    overlay.append("[3][lp]overlay='if(lte(t,0),-${timeModel.width_bitmap_progress},min($widthProgress,($widthProgress * ((cos((t / (${durationMs}/1000.0) + 1) * PI) / 2) + 0.5))))':0[ov2];")
                    overlay.append("[bg][ov2]overlay=${progressTemplate.left}:${progressTemplate.top}[ov3];")
                    inputIndex = 3
                }
            } else {
                val totalEntities = template.quranEntityList.size +
                    template.translationTemplateList.size +
                    (if (template.isVideoSquare) 1 else 0) +
                    (if (template.entityBismilahTemplate != null) 1 else 0) +
                    (if (template.entityIsti3adaTemplate != null) 1 else 0)
                countDownLatch = CountDownLatch(totalEntities)
                inputIndex = 0
            }
        } else {
            // Non-iPad types
            val totalEntities = template.quranEntityList.size +
                template.translationTemplateList.size +
                (if (template.isVideoSquare) 1 else 0) +
                (if (template.entityBismilahTemplate != null) 1 else 0) +
                (if (template.entityIsti3adaTemplate != null) 1 else 0)
            countDownLatch = CountDownLatch(totalEntities)

            when (ipadType) {
                IpadType.HEART -> {
                    val bgFile = File(template.uri_bg_ffmpeg ?: "")
                    if (bgFile.exists() && bgFile.isFile) {
                        args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                    }
                    args.addAll(listOf("-i", "$folder/line_bg.png"))
                    args.addAll(listOf("-f", "lavfi"))
                    args.addAll(listOf("-i", "color=size=${template.width}x${template.mTimeModel?.heightShape ?: 0}:color=#00000000"))

                    val durationStr = (durationMs / 1000.0).toString()
                    val heightShape = template.mTimeModel?.heightShape?.toFloat() ?: 0f
                    overlay.append("[2][1]overlay=x=0:y='${heightShape}*(1-clip(t/$durationStr,0,1))*0.8 + ${heightShape}*(1-(0.5-0.5*cos(PI*clip(t/$durationStr,0,1))))*0.2'[ov1];")
                    overlay.append("[0][ov1]overlay=0:${template.entityProgressTemplate?.top?.let { it + (template.mTimeModel?.startShape ?: 0f) }}[ov2];")

                    args.addAll(listOf("-i", "$folder/line_progress.png"))
                    overlay.append("[ov2][3]overlay=0:${template.entityProgressTemplate?.top ?: 0}[ov3];")
                    inputIndex = 3
                }

                IpadType.BATTERY -> {
                    val bgFile = File(template.uri_bg_ffmpeg ?: "")
                    if (bgFile.exists() && bgFile.isFile) {
                        args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                    }
                    args.addAll(listOf("-loop", "1"))
                    args.addAll(listOf("-i", "$folder/line_bg.png"))

                    val durationStr = (durationMs / 1000.0).toString()
                    val startShape = (-template.mTimeModel?.widthShape?.toFloat()!!) + template.mTimeModel?.startShape!!
                    val widthShape = template.mTimeModel?.widthShape?.toFloat()!!
                    overlay.append("[0][1]overlay=x='${startShape}+(${widthShape}*(clip(t/$durationStr,0,1))*0.8+${widthShape}*(0.5-0.5*cos(PI*clip(t/$durationStr,0,1)))*0.2)':y=${template.entityProgressTemplate?.top ?: 0}[ov1];")

                    args.addAll(listOf("-i", "$folder/line_progress.png"))
                    overlay.append("[ov1][2]overlay=0:${template.entityProgressTemplate?.top ?: 0}[ov2];")
                    inputIndex = 2
                }

                IpadType.CASSET, IpadType.CASSET_IMG_BLUR -> {
                    val bgFile = File(template.uri_bg_ffmpeg ?: "")
                    if (bgFile.exists() && bgFile.isFile) {
                        args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                    }
                    args.addAll(listOf("-loop", "1"))
                    args.addAll(listOf("-i", "$folder/line_bg.png"))

                    overlay.append("[1]rotate=angle=0.4*PI*t:ow=iw:oh=ih:fillcolor=#00000000[rot1];")
                    overlay.append("[0][rot1]overlay=${template.mTimeModel?.startShape}:${template.mTimeModel?.heightShape}[ov1];")
                    overlay.append("[1]rotate=angle=-0.5*PI*t:ow=iw:oh=ih:fillcolor=#00000000[rot1];")
                    overlay.append("[ov1][rot1]overlay=${template.mTimeModel?.widthShape}:${template.mTimeModel?.heightShape}[ov1];")
                    inputIndex = 1
                }

                IpadType.CASSET_IMG -> {
                    if (template.isVideoSquare) {
                        args.addAll(listOf("-stream_loop", "-1"))
                        args.addAll(listOf("-i", template.uri_media_video ?: ""))

                        val maxSize = max(template.width, template.height)
                        overlay.append("[0:v]scale=$maxSize:$maxSize:force_original_aspect_ratio=increase[sc];[sc]crop=${template.width}:${template.height}:(iw-${template.width})/2:(ih-${template.height})/2,format=yuva420p[ov0];")

                        val bgFile = File(template.uri_bg_ffmpeg ?: "")
                        val baseOverlay: String
                        if (bgFile.exists() && bgFile.isFile) {
                            args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                            overlay.append("[ov0][1]overlay[ov1];")
                            baseOverlay = "[ov1]"
                            inputIndex = 1
                        } else {
                            baseOverlay = "[ov0]"
                            inputIndex = 0
                        }

                        args.addAll(listOf("-loop", "1"))
                        args.addAll(listOf("-i", "$folder/line_bg.png"))
                        val reelIndex = inputIndex + 1
                        overlay.append("[$reelIndex]rotate=angle=0.4*PI*t:ow=iw:oh=ih:fillcolor=#00000000[rot$reelIndex];")
                        overlay.append("$baseOverlay[rot$reelIndex]overlay=${template.mTimeModel?.startShape}:${template.mTimeModel?.heightShape}[ov$reelIndex];")
                        overlay.append("[$reelIndex]rotate=angle=-0.5*PI*t:ow=iw:oh=ih:fillcolor=#00000000[rot$reelIndex];")
                        overlay.append("[ov$reelIndex][rot$reelIndex]overlay=${template.mTimeModel?.widthShape}:${template.mTimeModel?.heightShape}[ov$reelIndex];")
                        inputIndex = reelIndex
                    } else {
                        val bgFile = File(template.uri_bg_ffmpeg ?: "")
                        if (bgFile.exists() && bgFile.isFile) {
                            args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                        }

                        args.addAll(listOf("-loop", "1"))
                        args.addAll(listOf("-i", "$folder/line_bg.png"))
                        val reelIndex = inputIndex + 1
                        overlay.append("[$reelIndex]rotate=angle=0.4*PI*t:ow=iw:oh=ih:fillcolor=#00000000[rot$reelIndex];")
                        overlay.append("[0][rot$reelIndex]overlay=${template.mTimeModel?.startShape}:${template.mTimeModel?.heightShape}[ov$reelIndex];")
                        overlay.append("[$reelIndex]rotate=angle=-0.5*PI*t:ow=iw:oh=ih:fillcolor=#00000000[rot$reelIndex];")
                        overlay.append("[ov$reelIndex][rot$reelIndex]overlay=${template.mTimeModel?.widthShape}:${template.mTimeModel?.heightShape}[ov$reelIndex];")
                        inputIndex = reelIndex
                    }
                }

                IpadType.BLUE_TYPE -> {
                    val bgFile = File(template.uri_bg_ffmpeg ?: "")
                    if (bgFile.exists() && bgFile.isFile) {
                        args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                        args.addAll(listOf("-i", "$folder/line_progress.png"))
                        overlay.append("[1]loop=loop=-1:size=1:start=0[lp];")

                        args.addAll(listOf("-i", "$folder/line_bg.png"))
                        val timeModel = template.mTimeModel ?: return null
                        overlay.append("[lp][2]overlay=x=${-timeModel.width_bitmap_progress} + ( ((cos((t / (${durationMs}/1000.0) + 1) * PI) / 2) + 0.5) * ${timeModel.width_bitmap_progress - timeModel.progress_offset} ):y=0[ov1];")
                        overlay.append("[0][ov1]overlay=${template.entityProgressTemplate?.left}:${template.entityProgressTemplate?.top}[ov2];")
                        inputIndex = 2
                    } else {
                        countDownLatch.countDown()
                        inputIndex = 0
                    }
                }

                IpadType.BLACK_LAYER, IpadType.GRADIENT, IpadType.MASK_BRUSH -> {
                    // These types use a pre-rendered video (background + media video) as base
                    // The background image will be added by preRenderVideo if needed
                    inputIndex = 0
                }

                IpadType.BORDER -> {
                    // Simple border type — background image as base
                    val bgFile = File(template.uri_bg_ffmpeg ?: "")
                    if (bgFile.exists() && bgFile.isFile) {
                        args.addAll(listOf("-loop", "1"))
                        args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                        overlay.append("[0]format=yuv420p[ov0];")
                    }
                    inputIndex = 0
                    countDownLatch.countDown()
                }

                else -> {
                    // Default fallback — no special setup
                    val bgFile = File(template.uri_bg_ffmpeg ?: "")
                    if (bgFile.exists() && bgFile.isFile) {
                        args.addAll(listOf("-loop", "1"))
                        args.addAll(listOf("-i", template.uri_bg_ffmpeg!!))
                        overlay.append("[0]format=yuv420p[ov0];")
                    }
                    inputIndex = 0
                    countDownLatch.countDown()
                }
            }
        }

        // ── Video square overlay OR pre-render video for special types ─────
        if (template.isVideoSquare) {
            val videoFile = File(template.uri_media_video ?: "")
            if (videoFile.isFile && videoFile.exists()) {
                renderManager.addTask("Video prerender", durationSec)

                val squareModel = template.squareBitmapModel
                if (squareModel != null) {
                    val preRendered: String? = when {
                        ipadType == IpadType.IPAD || ipadType == IpadType.IPAD_UNBLUR ||
                        ipadType == IpadType.ROUND_RECT || ipadType == IpadType.RECT ->
                            preRenderExecutor.executePreRenderMaskRounded(squareModel, durationMs, countDownLatch, semaphore)
                        ipadType == IpadType.IPAD_NEOMORPHIC ->
                            preRenderExecutor.executePreRenderMaskCircle(squareModel, durationMs, countDownLatch, semaphore)
                        ipadType == IpadType.BOTTOM_RECT || ipadType == IpadType.IPAD_CLASSIC ->
                            preRenderExecutor.executePreRenderNoMask(squareModel, durationMs, countDownLatch, semaphore, videoCodec)
                        else ->
                            preRenderExecutor.executePreRenderMaskRounded(squareModel, durationMs, countDownLatch, semaphore)
                    }
                    if (preRendered != null) {
                        args.addAll(listOf("-i", preRendered))
                        val nextIndex = inputIndex + 1
                        overlay.append("[ov$inputIndex][$nextIndex:v]overlay=${squareModel.posX}:${squareModel.posY}[ov$nextIndex];")
                        inputIndex = nextIndex
                    }
                }
            }
        } else {
            // Non-video-square: add video/hue pre-render for certain types
            when (ipadType) {
                IpadType.BLACK_LAYER, IpadType.GRADIENT, IpadType.MASK_BRUSH -> {
                    val preRendered = preRenderExecutor.executePreRenderVideo(durationMs, countDownLatch, semaphore, videoCodec)
                    if (preRendered != null) {
                        // Pre-rendered video IS the base — add as input 0 and use as base
                        args.add(0, "-i")
                        args.add(1, preRendered)
                        overlay.append("[0:v]format=yuv420p[ov0];")
                        inputIndex = 0
                    }
                }
                IpadType.BLUE_TYPE -> {
                    val preRendered = preRenderExecutor.executePreRenderVideoHue(durationMs, countDownLatch, semaphore, videoCodec)
                    if (preRendered != null) {
                        // Pre-rendered video IS the base — add as input 0 and use as base
                        args.add(0, "-i")
                        args.add(1, preRendered)
                        overlay.append("[0:v]format=yuv420p[ov0];")
                        inputIndex = 0
                    }
                }
                else -> {
                    // Other types without video square may still need a base overlay
                }
            }
        }

        // ── FPS-based render progress factor ─────────────────────────────
        val fps = (durationSec.toFloat() / template.fps.toFloat()) * 2.0E-4f

        // ── Add Bismilah with transitions ────────────────────────────────
        inputIndex = addBismilah(
            template.entityIsti3adaTemplate, inputIndex, semaphore, countDownLatch, args, overlay, fps,
            template, preRenderExecutor
        )
        inputIndex = addBismilah(
            template.entityBismilahTemplate, inputIndex, semaphore, countDownLatch, args, overlay, fps,
            template, preRenderExecutor
        )

        // ── Add Quran ayahs with transitions ─────────────────────────────
        for (quranEntity in template.quranEntityList) {
            val startSec = abs(quranEntity.start)
            val endSec = abs(quranEntity.end)

            if (startSec >= endSec) {
                countDownLatch.countDown()
                continue
            }

            val entityFile = quranEntity.file
            if (entityFile == null) {
                countDownLatch.countDown()
                continue
            }

            val file = File("${template.folder_template}/$entityFile")
            if (!file.exists() || !file.isFile || file.length() <= 0) {
                countDownLatch.countDown()
                continue
            }

            val transition = quranEntity.transition
            if (transition != null) {
                val isFadeOut = transition.isOut && transition.duration_out > 0f
                val isFadeIn = transition.isIn && transition.duration_in > 0f

                if (isFadeIn && isFadeOut) {
                    inputIndex = addEntityWithBothTransitions(
                        quranEntity, inputIndex, startSec, endSec, transition,
                        args, overlay, fps, template, preRenderExecutor, countDownLatch, semaphore,
                        isQuran = true
                    )
                } else if (isFadeIn) {
                    inputIndex = addEntityWithFadeIn(
                        quranEntity, inputIndex, startSec, endSec, transition,
                        args, overlay, fps, template, preRenderExecutor, countDownLatch, semaphore
                    )
                } else if (isFadeOut) {
                    inputIndex = addEntityWithFadeOut(
                        quranEntity, inputIndex, startSec, endSec, transition,
                        args, overlay, fps, template, preRenderExecutor, countDownLatch, semaphore
                    )
                } else {
                    // No active transition — simple overlay
                    args.addAll(listOf("-i", "${template.folder_template}/$entityFile"))
                    val nextIndex = inputIndex + 1
                    overlay.append("[ov$inputIndex][$nextIndex]overlay=${quranEntity.btm_x}:${quranEntity.btm_y}:enable='between(t,$startSec,${abs(endSec - fps)})'[ov$nextIndex];")
                    countDownLatch.countDown()
                    inputIndex = nextIndex
                }
            } else {
                // No transition — simple overlay
                args.addAll(listOf("-i", "${template.folder_template}/$entityFile"))
                val nextIndex = inputIndex + 1
                overlay.append("[ov$inputIndex][$nextIndex]overlay=${quranEntity.btm_x}:${quranEntity.btm_y}:enable='between(t,$startSec,${abs(endSec - fps)})'[ov$nextIndex];")
                countDownLatch.countDown()
                inputIndex = nextIndex
            }
        }

        // ── Add Translations ─────────────────────────────────────────────
        for (translationEntity in template.translationTemplateList) {
            val startSec = abs(translationEntity.start)
            val endSec = abs(translationEntity.end)

            if (startSec >= endSec) {
                countDownLatch.countDown()
                continue
            }

            val entityFile = translationEntity.file
            if (entityFile == null) {
                countDownLatch.countDown()
                continue
            }

            val file = File("${template.folder_template}/$entityFile")
            if (!file.exists() || !file.isFile || file.length() <= 0) {
                countDownLatch.countDown()
                continue
            }

            args.addAll(listOf("-i", "${template.folder_template}/$entityFile"))
            val nextIndex = inputIndex + 1
            overlay.append("[ov$inputIndex][$nextIndex]overlay=0:${translationEntity.btm_y}:enable='between(t,$startSec,${abs(endSec - fps)})'[ov$nextIndex];")
            countDownLatch.countDown()
            inputIndex = nextIndex
        }

        // ── Add Media audio tracks ───────────────────────────────────────
        var audioMixInputs = 0
        val audioMixLabels = StringBuilder()
        val lastVideoLabel = "[ov$inputIndex]"

        for ((mediaIndex, media) in template.entityMediaList.withIndex()) {
            if (media.end < media.start) continue

            val pathFfmpeg = media.path_ffmpeg_effect
            if (pathFfmpeg == null || !File(pathFfmpeg).let { it.isFile && it.exists() }) continue

            args.addAll(listOf("-i", pathFfmpeg))
            val mediaInputIndex = inputIndex + 1
            inputIndex = mediaInputIndex

            val startMs = media.start / 1000.0f
            val endMs = media.end / 1000.0f
            val delayMs = Math.round(media.posXFFmpeg)

            // Audio processing
            overlay.append("[$mediaInputIndex:a]volume=0.5[vlm$mediaIndex];")

            if (media.isApplyEffectInPreview) {
                overlay.append("[vlm$mediaIndex]adelay=$delayMs|$delayMs[d$mediaIndex];")
            } else {
                overlay.append("[vlm$mediaIndex]atrim=start=$startMs:end=$endMs,asetpts=PTS-STARTPTS[ao$mediaIndex];")
                overlay.append("[ao$mediaIndex]adelay=$delayMs|$delayMs[d$mediaIndex];")
            }

            audioMixLabels.append("[d$mediaIndex]")
            audioMixInputs++
        }

        // ── Audio mixing ─────────────────────────────────────────────────
        val hasAudioMix = audioMixInputs > 0
        if (hasAudioMix) {
            overlay.append("${audioMixLabels}amix=inputs=$audioMixInputs:duration=longest:normalize=0:dropout_transition=0,volume=2[a]")
        }

        // ── Build final command ──────────────────────────────────────────
        args.addAll(listOf("-filter_complex", overlay.toString()))

        // Audio mapping
        if (hasAudioMix) {
            args.addAll(listOf("-map", lastVideoLabel, "-map", "[a]"))
            val audioCodec = codecInfo.audioCodec
            if (audioCodec != null) {
                args.addAll(listOf("-c:a", audioCodec, "-b:a", "256k", "-ar", "44100", "-ac", "2"))
            }
        } else {
            args.addAll(listOf("-map", lastVideoLabel))
        }

        // Common encoding params
        args.addAll(listOf("-y", "-level", "4.1", "-g", "120"))

        // Video codec
        if (videoCodec != null) {
            args.addAll(listOf("-threads", "0", "-c:v", videoCodec, "-preset", "fast", "-crf", "18"))
        } else {
            args.addAll(listOf("-b:v", "4M"))
        }

        args.addAll(listOf(
            "-pix_fmt", "yuv420p",
            "-framerate", template.fps.toString(),
            "-movflags", "+faststart",
            "-t", "${durationMs}ms"
        ))

        // Output path
        val outputPath = template.uri_video ?: "$folder/export_${System.currentTimeMillis()}.mp4"
        args.add(outputPath)

        renderManager.computeWeights()

        // Store overlay filter chain for bug reporting (matches original Java's this.overlay field)
        lastOverlayFilter = overlay.toString()

        return args.toTypedArray()
    }

    // ════════════════════════════════════════════════════════════════════
    //  Bismilah / Isti3ada overlay builder with transitions
    // ════════════════════════════════════════════════════════════════════

    /**
     * Add a bismilah/isti3ada entity overlay with transition support.
     * Returns the updated inputIndex.
     */
    private fun addBismilah(
        entity: EntityBismilahTemplate?,
        inputIndex: Int,
        semaphore: Semaphore,
        countDownLatch: CountDownLatch,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor
    ): Int {
        if (entity == null) return inputIndex

        val startSec = abs(entity.start)
        val endSec = abs(entity.end)

        val file = File("${template.folder_template}/${entity.file}")
        if (!file.exists() || !file.isFile || file.length() <= 0) return inputIndex

        val transition = entity.transition
        if (transition != null) {
            val isFadeOut = transition.isOut && transition.duration_out > 0f
            val isFadeIn = transition.isIn && transition.duration_in > 0f

            if (isFadeIn && isFadeOut) {
                return addBismilahWithBothTransitions(
                    entity, inputIndex, startSec, endSec, transition,
                    args, overlay, fps, template, preRenderExecutor, countDownLatch, semaphore
                )
            } else if (isFadeIn) {
                return addBismilahWithFadeIn(
                    entity, inputIndex, startSec, endSec, transition,
                    args, overlay, fps, template, preRenderExecutor, countDownLatch, semaphore
                )
            } else if (isFadeOut) {
                return addBismilahWithFadeOut(
                    entity, inputIndex, startSec, endSec, transition,
                    args, overlay, fps, template, preRenderExecutor, countDownLatch, semaphore
                )
            }
        }

        // No transition — simple overlay
        args.addAll(listOf("-i", file.absolutePath))
        val nextIndex = inputIndex + 1
        overlay.append("[ov$inputIndex][$nextIndex]overlay=${entity.btm_x}:${entity.btm_y}:enable='between(t,$startSec,${abs(endSec - fps)})'[ov$nextIndex];")
        countDownLatch.countDown()
        return nextIndex
    }

    private fun addBismilahWithBothTransitions(
        entity: EntityBismilahTemplate,
        inputIndex: Int,
        startSec: Float,
        endSec: Float,
        transition: Transition,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor,
        countDownLatch: CountDownLatch,
        semaphore: Semaphore
    ): Int {
        val durationIn = transition.duration_in
        val btmX = entity.btm_x
        val btmY = entity.btm_y
        val fromW = transition.fromW
        val typeIn = transition.type_in
        val durationOut = transition.duration_out
        val fadeOutStart = abs(endSec - durationOut)
        val durationSec = (endSec - startSec).toInt()

        val typeOut = transition.type_out
        val filter = fadeInOut(fadeOutStart - startSec, durationIn, durationOut, template.fps)

        val segmentPath = preRenderExecutor.executeGenerateVideoSegment(
            entity.file ?: "", "bismilah", filter, durationSec, inputIndex,
            countDownLatch, semaphore
        )

        if (segmentPath != null) {
            args.addAll(listOf("-i", segmentPath))
            val nextIndex = inputIndex + 1
            overlay.append("[$nextIndex]setpts=PTS-STARTPTS+${startSec}/TB[seg$nextIndex];")

            // Determine transition x-expression
            val xExpr = when {
                typeIn == TransitionType.FADE_IN.value && typeOut == TransitionType.FADE_OUT.value -> {
                    // Both fade — simple position
                    "$btmX"
                }
                typeIn == TransitionType.FADE_IN.value -> {
                    // Fade in, slide out
                    val slideOut = when {
                        typeOut == TransitionType.SLIDE_TO_RIGHT.value ->
                            slideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, 1.0f)
                        typeOut == TransitionType.SLIDE_TO_LEFT.value ->
                            slideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, -1.0f)
                        else -> "$btmX"
                    }
                    slideOut
                }
                typeIn == TransitionType.SLIDE_TO_RIGHT.value -> {
                    val slideIn = mSlideX(startSec, durationIn, btmX, fromW, -1.0f, 0.0f)
                    val slideOut = when {
                        typeOut == TransitionType.SLIDE_TO_RIGHT.value ->
                            mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, 1.0f)
                        else -> mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, -1.0f)
                    }
                    val fromOff = btmX - fromW
                    val toOff = if (typeOut == TransitionType.SLIDE_TO_RIGHT.value) btmX + fromW else btmX - fromW
                    "'if(lt(t,$startSec),$fromOff,if(lt(t,${startSec + durationIn}),$slideIn,if(lt(t,$fadeOutStart),$btmX,if(lt(t,${fadeOutStart + durationOut}),$slideOut,$toOff))))'"
                }
                typeIn == TransitionType.SLIDE_TO_LEFT.value -> {
                    val slideIn = mSlideX(startSec, durationIn, btmX, fromW, 1.0f, 0.0f)
                    val slideOut = when {
                        typeOut == TransitionType.SLIDE_TO_RIGHT.value ->
                            mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, 1.0f)
                        else -> mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, -1.0f)
                    }
                    val fromOff = btmX + fromW
                    val toOff = if (typeOut == TransitionType.SLIDE_TO_RIGHT.value) btmX + fromW else btmX - fromW
                    "'if(lt(t,$startSec),$fromOff,if(lt(t,${startSec + durationIn}),$slideIn,if(lt(t,$fadeOutStart),$btmX,if(lt(t,${fadeOutStart + durationOut}),$slideOut,$toOff))))'"
                }
                else -> "$btmX"
            }

            overlay.append("[ov$inputIndex][seg$nextIndex]overlay=x=$xExpr:y=$btmY:enable='between(t,$startSec,$endSec)'[ov$nextIndex];")
            return nextIndex
        }

        countDownLatch.countDown()
        return inputIndex
    }

    private fun addBismilahWithFadeIn(
        entity: EntityBismilahTemplate,
        inputIndex: Int,
        startSec: Float,
        endSec: Float,
        transition: Transition,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor,
        countDownLatch: CountDownLatch,
        semaphore: Semaphore
    ): Int {
        val durationIn = transition.duration_in
        val btmX = entity.btm_x
        val btmY = entity.btm_y
        val fromW = transition.fromW
        val typeIn = transition.type_in
        val durationSec = (endSec - startSec).toInt()

        val filter = mFadeFilter(0f, durationIn, true)
        val segmentPath = preRenderExecutor.executeGenerateVideoSegment(
            entity.file ?: "", "bismilah", filter, durationSec, inputIndex,
            countDownLatch, semaphore
        )

        if (segmentPath != null) {
            args.addAll(listOf("-i", segmentPath))
            val nextIndex = inputIndex + 1
            overlay.append("[$nextIndex]setpts=PTS-STARTPTS+${startSec}/TB[seg$nextIndex];")

            val xExpr = when (typeIn) {
                TransitionType.FADE_IN.value -> "$btmX"
                TransitionType.SLIDE_TO_RIGHT.value -> slideX(startSec, durationIn, btmX, fromW, -1.0f, 0.0f)
                TransitionType.SLIDE_TO_LEFT.value -> slideX(startSec, durationIn, btmX, fromW, 1.0f, 0.0f)
                else -> "$btmX"
            }

            overlay.append("[ov$inputIndex][seg$nextIndex]overlay=x=$xExpr:y=$btmY:enable='between(t,$startSec,$endSec)'[ov$nextIndex];")
            return nextIndex
        }

        countDownLatch.countDown()
        return inputIndex
    }

    private fun addBismilahWithFadeOut(
        entity: EntityBismilahTemplate,
        inputIndex: Int,
        startSec: Float,
        endSec: Float,
        transition: Transition,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor,
        countDownLatch: CountDownLatch,
        semaphore: Semaphore
    ): Int {
        val durationOut = transition.duration_out
        val btmX = entity.btm_x
        val btmY = entity.btm_y
        val fromW = transition.fromW
        val typeOut = transition.type_out
        val durationSec = (endSec - startSec).toInt()
        val fadeOutStart = durationSec - durationOut

        val filter = mFadeFilter(fadeOutStart, durationOut, false)
        val segmentPath = preRenderExecutor.executeGenerateVideoSegment(
            entity.file ?: "", "bismilah", filter, durationSec, inputIndex,
            countDownLatch, semaphore
        )

        if (segmentPath != null) {
            args.addAll(listOf("-i", segmentPath))
            val nextIndex = inputIndex + 1
            overlay.append("[$nextIndex]setpts=PTS-STARTPTS+${startSec}/TB[seg$nextIndex];")

            val fadeOutStartAbs = abs(endSec - durationOut)
            val xExpr = when (typeOut) {
                TransitionType.SLIDE_TO_RIGHT.value -> slideX(fadeOutStartAbs, durationOut, btmX, fromW, 0.0f, 1.0f)
                TransitionType.SLIDE_TO_LEFT.value -> slideX(fadeOutStartAbs, durationOut, btmX, fromW, 0.0f, -1.0f)
                TransitionType.FADE_OUT.value -> "$btmX"
                else -> "$btmX"
            }

            overlay.append("[ov$inputIndex][seg$nextIndex]overlay=x=$xExpr:y=$btmY:enable='between(t,$startSec,$endSec)'[ov$nextIndex];")
            return nextIndex
        }

        countDownLatch.countDown()
        return inputIndex
    }

    // ════════════════════════════════════════════════════════════════════
    //  Quran ayah overlay builders with transitions
    // ════════════════════════════════════════════════════════════════════

    private fun addEntityWithBothTransitions(
        entity: EntityQuranTemplate,
        inputIndex: Int,
        startSec: Float,
        endSec: Float,
        transition: Transition,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor,
        countDownLatch: CountDownLatch,
        semaphore: Semaphore,
        isQuran: Boolean = false
    ): Int {
        val durationIn = transition.duration_in
        val btmX = entity.btm_x
        val btmY = entity.btm_y
        val fromW = transition.fromW
        val typeIn = transition.type_in
        val durationOut = transition.duration_out
        val typeOut = transition.type_out
        val fadeOutStart = abs(endSec - durationOut)
        val durationSec = (endSec - startSec).toInt()

        val filter = fadeInOut(fadeOutStart - startSec, durationIn, durationOut, template.fps)
        val segmentPath = preRenderExecutor.executeGenerateVideoSegment(
            entity.file ?: "", "ayah", filter, durationSec, inputIndex,
            countDownLatch, semaphore
        )

        if (segmentPath != null) {
            args.addAll(listOf("-i", segmentPath))
            val nextIndex = inputIndex + 1
            overlay.append("[$nextIndex]setpts=PTS-STARTPTS+${startSec}/TB[seg$nextIndex];")

            val xExpr = when {
                typeIn == TransitionType.SLIDE_TO_RIGHT.value -> {
                    val slideIn = mSlideX(startSec, durationIn, btmX, fromW, -1.0f, 0.0f)
                    val slideOut = when {
                        typeOut == TransitionType.SLIDE_TO_RIGHT.value ->
                            mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, 1.0f)
                        else -> mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, -1.0f)
                    }
                    val fromOff = btmX - fromW
                    val toOff = if (typeOut == TransitionType.SLIDE_TO_RIGHT.value) btmX + fromW else btmX - fromW
                    "'if(lt(t,$startSec),$fromOff,if(lt(t,${startSec + durationIn}),$slideIn,if(lt(t,$fadeOutStart),$btmX,if(lt(t,${fadeOutStart + durationOut}),$slideOut,$toOff))))'"
                }
                typeIn == TransitionType.SLIDE_TO_LEFT.value -> {
                    val slideIn = mSlideX(startSec, durationIn, btmX, fromW, 1.0f, 0.0f)
                    val slideOut = when {
                        typeOut == TransitionType.SLIDE_TO_RIGHT.value ->
                            mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, 1.0f)
                        else -> mSlideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, -1.0f)
                    }
                    val fromOff = btmX + fromW
                    val toOff = if (typeOut == TransitionType.SLIDE_TO_RIGHT.value) btmX + fromW else btmX - fromW
                    "'if(lt(t,$startSec),$fromOff,if(lt(t,${startSec + durationIn}),$slideIn,if(lt(t,$fadeOutStart),$btmX,if(lt(t,${fadeOutStart + durationOut}),$slideOut,$toOff))))'"
                }
                typeIn == TransitionType.FADE_IN.value -> {
                    when (typeOut) {
                        TransitionType.SLIDE_TO_RIGHT.value ->
                            slideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, 1.0f)
                        TransitionType.SLIDE_TO_LEFT.value ->
                            slideX(fadeOutStart, durationOut, btmX, fromW, 0.0f, -1.0f)
                        else -> "$btmX"
                    }
                }
                else -> "$btmX"
            }

            overlay.append("[ov$inputIndex][seg$nextIndex]overlay=x=$xExpr:y=$btmY:enable='between(t,$startSec,$endSec)'[ov$nextIndex];")
            return nextIndex
        }

        countDownLatch.countDown()
        return inputIndex
    }

    private fun addEntityWithFadeIn(
        entity: EntityQuranTemplate,
        inputIndex: Int,
        startSec: Float,
        endSec: Float,
        transition: Transition,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor,
        countDownLatch: CountDownLatch,
        semaphore: Semaphore
    ): Int {
        val durationIn = transition.duration_in
        val btmX = entity.btm_x
        val btmY = entity.btm_y
        val fromW = transition.fromW
        val typeIn = transition.type_in
        val durationSec = (endSec - startSec).toInt()

        val filter = mFadeFilter(0f, durationIn, true)
        val segmentPath = preRenderExecutor.executeGenerateVideoSegment(
            entity.file ?: "", "ayah", filter, durationSec, inputIndex,
            countDownLatch, semaphore
        )

        if (segmentPath != null) {
            args.addAll(listOf("-i", segmentPath))
            val nextIndex = inputIndex + 1
            overlay.append("[$nextIndex]setpts=PTS-STARTPTS+${startSec}/TB[seg$nextIndex];")

            val xExpr = when (typeIn) {
                TransitionType.FADE_IN.value -> "$btmX"
                TransitionType.SLIDE_TO_RIGHT.value -> slideX(startSec, durationIn, btmX, fromW, -1.0f, 0.0f)
                TransitionType.SLIDE_TO_LEFT.value -> slideX(startSec, durationIn, btmX, fromW, 1.0f, 0.0f)
                else -> "$btmX"
            }

            overlay.append("[ov$inputIndex][seg$nextIndex]overlay=x=$xExpr:y=$btmY:enable='between(t,$startSec,$endSec)'[ov$nextIndex];")
            return nextIndex
        }

        countDownLatch.countDown()
        return inputIndex
    }

    private fun addEntityWithFadeOut(
        entity: EntityQuranTemplate,
        inputIndex: Int,
        startSec: Float,
        endSec: Float,
        transition: Transition,
        args: MutableList<String>,
        overlay: StringBuilder,
        fps: Float,
        template: Template,
        preRenderExecutor: PreRenderExecutor,
        countDownLatch: CountDownLatch,
        semaphore: Semaphore
    ): Int {
        val durationOut = transition.duration_out
        val btmX = entity.btm_x
        val btmY = entity.btm_y
        val fromW = transition.fromW
        val typeOut = transition.type_out
        val durationSec = (endSec - startSec).toInt()
        val fadeOutStart = durationSec - durationOut

        val filter = mFadeFilter(fadeOutStart, durationOut, false)
        val segmentPath = preRenderExecutor.executeGenerateVideoSegment(
            entity.file ?: "", "ayah", filter, durationSec, inputIndex,
            countDownLatch, semaphore
        )

        if (segmentPath != null) {
            args.addAll(listOf("-i", segmentPath))
            val nextIndex = inputIndex + 1
            overlay.append("[$nextIndex]setpts=PTS-STARTPTS+${startSec}/TB[seg$nextIndex];")

            val fadeOutStartAbs = abs(endSec - durationOut)
            val xExpr = when (typeOut) {
                TransitionType.SLIDE_TO_RIGHT.value -> slideX(fadeOutStartAbs, durationOut, btmX, fromW, 0.0f, 1.0f)
                TransitionType.SLIDE_TO_LEFT.value -> slideX(fadeOutStartAbs, durationOut, btmX, fromW, 0.0f, -1.0f)
                TransitionType.FADE_OUT.value -> "$btmX"
                else -> "$btmX"
            }

            overlay.append("[ov$inputIndex][seg$nextIndex]overlay=x=$xExpr:y=$btmY:enable='between(t,$startSec,$endSec)'[ov$nextIndex];")
            return nextIndex
        }

        countDownLatch.countDown()
        return inputIndex
    }

    // ════════════════════════════════════════════════════════════════════
    //  Pre-render executor interface — the Activity provides the
    //  actual FFmpeg execution, ExportCommandBuilder just builds args
    // ════════════════════════════════════════════════════════════════════

    /**
     * Interface for executing pre-render FFmpeg commands.
     * The Activity provides the implementation since it manages
     * FFmpeg session IDs, CountDownLatch coordination, and Semaphore.
     */
    interface PreRenderExecutor {
        fun executePreRenderMaskRounded(
            model: SquareBitmapModel, durationMs: Int,
            latch: CountDownLatch, semaphore: Semaphore
        ): String?

        fun executePreRenderMaskCircle(
            model: SquareBitmapModel, durationMs: Int,
            latch: CountDownLatch, semaphore: Semaphore
        ): String?

        fun executePreRenderNoMask(
            model: SquareBitmapModel, durationMs: Int,
            latch: CountDownLatch, semaphore: Semaphore, codec: String?
        ): String?

        fun executePreRenderVideo(
            durationMs: Int, latch: CountDownLatch,
            semaphore: Semaphore, codec: String?
        ): String?

        fun executePreRenderVideoHue(
            durationMs: Int, latch: CountDownLatch,
            semaphore: Semaphore, codec: String?
        ): String?

        fun executeGenerateTimer(
            durationMs: Int, latch: CountDownLatch,
            semaphore: Semaphore
        ): String?

        fun executeGenerateVideoSegment(
            entityFile: String, outputName: String, filter: String,
            durationSec: Int, index: Int,
            latch: CountDownLatch, semaphore: Semaphore
        ): String?
    }

    // ════════════════════════════════════════════════════════════════════
    //  RenderManager reference — set by Activity before calling buildCommand
    // ════════════════════════════════════════════════════════════════════

    private val renderManager = RenderManager()

    fun getRenderManager(): RenderManager = renderManager

    // ════════════════════════════════════════════════════════════════════
    //  Bitrate calculator
    // ════════════════════════════════════════════════════════════════════

    fun getBitrate(width: Int, height: Int, fps: Int): String {
        val isHighFps = fps > 30
        val max = max(width, height)
        return when {
            max <= 720 -> if (isHighFps) "2000k" else "1500k"
            max <= 1280 -> if (isHighFps) "4000k" else "3000k"
            max <= 1920 -> if (isHighFps) "6000k" else "4500k"
            else -> "8000k"
        }
    }
}
