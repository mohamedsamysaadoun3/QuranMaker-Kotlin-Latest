package hazem.nurmontage.videoquran.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * Fixed pitch control using proper FFmpeg asetrate+atempo chain
 * that respects the actual seekbar value.
 *
 * The asetrate filter changes pitch by resampling, and atempo corrects
 * the speed back to normal, resulting in pitch change without tempo change.
 */
class PitchCorrector {

    var semitones: Float = 0f
        set(value) {
            field = clampSemitones(value)
        }
    var isEnabled: Boolean = true

    constructor()

    constructor(semitones: Float) {
        this.semitones = clampSemitones(semitones)
        this.isEnabled = true
    }

    /**
     * Set pitch shift from a seekbar value (0-100 range).
     * Maps 0 -> -12 semitones, 50 -> 0 semitones, 100 -> +12 semitones.
     */
    fun setFromSeekBarValue(seekBarValue: Int) {
        this.semitones = clampSemitones((seekBarValue - 50) * 24f / 100f)
    }

    /**
     * Get the seekbar value (0-100) for the current pitch setting.
     */
    fun getSeekBarValue(): Int {
        return ((semitones + 12f) * 100f / 24f).toInt()
    }

    /**
     * Build the FFmpeg filter chain for pitch correction.
     * Uses asetrate to change pitch, then atempo to correct duration.
     *
     * @param inputLabel Input audio label (e.g., "[0:a]")
     * @param outputLabel Output audio label (e.g., "[pitch_out]")
     * @param sampleRate The original sample rate of the audio
     * @return FFmpeg filter string
     */
    fun buildFFmpegFilter(inputLabel: String, outputLabel: String, sampleRate: Int): String {
        if (!isEnabled || abs(semitones) < 0.01f) {
            return "${inputLabel}acopy$outputLabel"
        }

        // Calculate the pitch ratio from semitones
        val pitchRatio = SEMITONE_RATIO.toDouble().pow(semitones.toDouble())

        // asetrate changes the sample rate, which changes both pitch and speed
        // new_rate = original_rate * pitchRatio
        val newRate = round(sampleRate * pitchRatio).toInt()

        // atempo corrects the speed back to normal
        // atempo = 1.0 / pitchRatio (to compensate for the speed change)
        // But atempo only supports 0.5 to 100.0 range, so we chain if needed
        val atempoValue = 1.0 / pitchRatio

        val sb = StringBuilder()
        sb.append(inputLabel)

        // Apply asetrate to change pitch
        sb.append("asetrate=").append(newRate)

        // Apply atempo to correct speed
        // atempo range is 0.5 to 100.0; if outside, chain multiple atempo filters
        sb.append(",").append(buildAtempoChain(atempoValue))

        // Resample back to original sample rate for compatibility
        sb.append(",aresample=").append(sampleRate)

        sb.append(outputLabel)
        return sb.toString()
    }

    /**
     * Build atempo filter chain handling the 0.5-100.0 range limitation.
     */
    private fun buildAtempoChain(targetTempo: Double): String {
        val chain = StringBuilder()

        var remaining = targetTempo
        while (remaining < 0.5) {
            chain.append("atempo=0.5,")
            remaining /= 0.5
        }
        while (remaining > 100.0) {
            chain.append("atempo=100.0,")
            remaining /= 100.0
        }
        chain.append("atempo=").append("%.6f".format(remaining))

        return chain.toString()
    }

    /**
     * Get the current pitch ratio.
     */
    fun getPitchRatio(): Float {
        return SEMITONE_RATIO.toDouble().pow(semitones.toDouble()).toFloat()
    }

    private fun clampSemitones(value: Float): Float {
        return max(MIN_SEMITONES, min(MAX_SEMITONES, value))
    }

    companion object {
        private const val TAG = "PitchCorrector"
        private const val SEMITONE_RATIO = 1.059463094f // 2^(1/12)
        const val MIN_SEMITONES = -12f
        const val MAX_SEMITONES = 12f

        @JvmStatic
        fun getMinSemitones(): Float = MIN_SEMITONES

        @JvmStatic
        fun getMaxSemitones(): Float = MAX_SEMITONES
    }
}
