package hazem.nurmontage.videoquran.ui.engine

import android.media.MediaPlayer
import android.net.Uri
import android.text.TextUtils
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.model.EntityMedia
import java.io.File
import java.util.Locale

/**
 * FFmpeg command builder for the EngineActivity.
 *
 * This object encapsulates all FFmpeg command composition logic that was
 * originally embedded in EngineActivity.java (~8000 lines). The commands
 * are preserved EXACTLY as they appear in the original Java source to
 * ensure identical rendering behavior.
 *
 * Command categories:
 * 1. **Audio effects pipeline** — volume, fade, noise removal, reverb, echo, speed
 * 2. **Speed filter cascading** — atempo chain for speeds outside [0.5, 2.0]
 * 3. **PCM extraction** — for waveform rendering via PCMWaveformExtractor
 * 4. **Video frame extraction** — for template-based frame playback
 * 5. **Audio extraction from video** — extract audio track for timeline editing
 *
 * All FFmpeg command strings and filter chains are treated as sacred —
 * any modification could break the rendering pipeline.
 */
object FfmpegCommandBuilder {

    /**
     * Build the complete audio effects FFmpeg filter chain for a given [EffectAudio].
     *
     * The filter chain is applied in this exact order:
     * 1. `atrim=start=X:end=Y` — clip to the specified time range
     * 2. `asetpts=N/SR/TB` — reset timestamps after trimming
     * 3. `afftdn=nf=-25` — noise removal (if enabled)
     * 4. `volume=X.XX` — volume adjustment
     * 5. `afade=t=in:st=0:d=X` — fade in (if duration > 0)
     * 6. `afade=t=out:st=X:d=Y` — fade out (if duration > 0)
     * 7. `enhance command` — voice enhancement (if enabled)
     * 8. `reverbPreset` — reverb filter string (if set)
     * 9. `aecho=1.0:outGain:delays:decays` — echo effect (if decays > 0)
     * 10. `atempo=X.XX` chain — speed adjustment (if speed != 1.0)
     *
     * @param effectAudio The audio effect parameters
     * @param startMs     Start time in seconds (from effectAudio.getStart() / 1000)
     * @param endMs       End time in seconds (from effectAudio.getEnd() / 1000)
     * @return Comma-separated FFmpeg audio filter string
     */
    fun buildAudioEffectsChain(effectAudio: EffectAudio, startMs: Float, endMs: Float): String {
        val filters = mutableListOf<String>()

        // 1. Trim to time range
        filters.add("atrim=start=$startMs:end=$endMs")
        filters.add("asetpts=N/SR/TB")

        // 2. Noise removal
        if (effectAudio.isRemoveNoice) {
            filters.add("afftdn=nf=-25")
        }

        // 3. Volume
        filters.add(String.format(Locale.US, "volume=%.2f", effectAudio.volume))

        // 4. Fade in
        if (effectAudio.fade_in > 0) {
            filters.add("afade=t=in:st=0:d=${effectAudio.fade_in}")
        }

        // 5. Fade out
        if (effectAudio.fade_out > 0) {
            val fadeOutStart = (endMs - startMs) - effectAudio.fade_out
            filters.add("afade=t=out:st=$fadeOutStart:d=${effectAudio.fade_out}")
        }

        // 6. Voice enhancement
        if (effectAudio.isEnhance) {
            filters.add(Common.ENHANCE_CMD)
        }

        // 7. Reverb preset
        if (effectAudio.reverbPreset != null) {
            filters.add(effectAudio.reverbPreset!!)
        }

        // 8. Echo effect
        if (effectAudio.decays > 0) {
            filters.add(String.format(
                Locale.US, "aecho=%.2f:%.2f:%s:%s",
                1.0f, effectAudio.outGain, effectAudio.delays_cmd, effectAudio.decays_cmd
            ))
        }

        // 9. Speed adjustment
        if (effectAudio.speed != 1.0f) {
            filters.addAll(buildSpeedFilters(effectAudio.speed))
        }

        return TextUtils.join(",", filters)
    }

    /**
     * Build the atempo filter chain for audio speed adjustment.
     *
     * FFmpeg's `atempo` filter only supports the range [0.5, 2.0].
     * For speeds outside this range, we cascade multiple atempo filters:
     *
     * - **speed < 0.5**: Chain `atempo=0.5` filters until the remainder
     *   falls within [0.5, 2.0], then apply one more atempo with the remainder.
     *
     * - **speed > 2.0**: Chain `atempo=2.0` filters until the remainder
     *   falls within [0.5, 2.0], then apply one more atempo with the remainder.
     *
     * - **0.5 <= speed <= 2.0**: Single `atempo=X.XX` filter.
     *
     * Example: speed=4.0 -> `["atempo=2.0", "atempo=2.0"]`
     * Example: speed=0.25 -> `["atempo=0.5", "atempo=0.5"]`
     *
     * @param speed The desired playback speed multiplier
     * @return List of atempo filter strings to be joined with commas
     */
    fun buildSpeedFilters(speed: Float): List<String> {
        val filters = mutableListOf<String>()
        var remaining = speed

        when {
            remaining < 0.5f -> {
                while (remaining < 0.5f) {
                    filters.add("atempo=0.5")
                    remaining /= 0.5f
                }
                filters.add(String.format(Locale.US, "atempo=%.2f", remaining))
            }
            remaining > 2.0f -> {
                while (remaining > 2.0f) {
                    filters.add("atempo=2.0")
                    remaining /= 2.0f
                }
                filters.add(String.format(Locale.US, "atempo=%.2f", remaining))
            }
            else -> {
                filters.add(String.format(Locale.US, "atempo=%.2f", remaining))
            }
        }

        return filters
    }

    /**
     * Build FFmpeg arguments for applying audio effects to a file.
     *
     * Command format:
     * `-i <inputPath> -af <filterChain> -y <outputPath>`
     */
    fun buildAudioEffectArgs(inputPath: String, outputPath: String, filterChain: String): Array<String> {
        return arrayOf("-i", inputPath, "-af", filterChain, "-y", outputPath)
    }

    /**
     * Build FFmpeg arguments for extracting PCM audio for waveform rendering.
     *
     * Command format:
     * `-i <inputPath> -map 0:a -ac 1 -ar 44100 -f s16le <outputPath> -y`
     */
    fun buildPcmExtractionArgs(inputPath: String, outputPath: String): Array<String> {
        return arrayOf(
            "-i", inputPath,
            "-map", "0:a",
            "-ac", "1",
            "-ar", "44100",
            "-f", "s16le",
            outputPath,
            "-y"
        )
    }

    /**
     * Build FFmpeg arguments for extracting video frames as JPEG images.
     *
     * Command format:
     * `-i <inputPath> -ss 0 -t <endFrame> -r 25 -vf scale=<size>:<size>:force_original_aspect_ratio=increase -q:v 0 -threads 4 -an -y <outputPath>`
     */
    fun buildFrameExtractionArgs(
        inputPath: String,
        outputPath: String,
        endFrame: Int,
        size: Int
    ): Array<String> {
        return arrayOf(
            "-i", inputPath,
            "-ss", "0",
            "-t", "$endFrame",
            "-r", "25",
            "-vf", "scale=$size:$size:force_original_aspect_ratio=increase",
            "-q:v", "0",
            "-threads", "4",
            "-an",
            "-y",
            outputPath
        )
    }

    /**
     * Build FFmpeg arguments for extracting audio from a video file (copy codec).
     *
     * Command format:
     * `-i <inputPath> -vn -acodec copy -y <outputPath>`
     */
    fun buildAudioExtractFromVideoArgs(inputPath: String, outputPath: String): Array<String> {
        return arrayOf("-i", inputPath, "-vn", "-acodec", "copy", "-y", outputPath)
    }

    /**
     * Build FFmpeg arguments for generating a waveform preview image.
     *
     * Command format:
     * `-i <inputPath> -filter_complex aformat=channel_layouts=mono,showwavespic=s=<width>x<height>:colors=#522123 -frames:v 1 -y <outputPath>`
     */
    fun buildWaveformPreviewArgs(
        inputPath: String,
        outputPath: String,
        width: Int,
        height: Int
    ): Array<String> {
        return arrayOf(
            "-i", inputPath,
            "-filter_complex",
            "aformat=channel_layouts=mono,showwavespic=s=${width}x${height}:colors=#522123",
            "-frames:v", "1",
            "-y",
            outputPath
        )
    }

    /**
     * Execute FFmpeg command asynchronously with session tracking.
     */
    fun executeAsync(args: Array<String>, callback: FFmpegSessionCompleteCallback): Long {
        return FFmpegKit.executeWithArgumentsAsync(args, callback).sessionId
    }

    /**
     * Cancel a running FFmpeg session by its ID.
     */
    fun cancel(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }
}
