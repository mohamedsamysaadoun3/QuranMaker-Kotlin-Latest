package hazem.nurmontage.videoquran.audio

import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced reverb effect with early reflections.
 * Multiple presets for different masjid sizes.
 * Generates FFmpeg aecho + reverb filter chains.
 */
class MasjidReverbFilter {

    enum class ReverbPreset(
        val displayName: String,
        val reverbGain: Float,
        val reverbDelay: Float,
        val earlyReflections: Float,
        val decayFactor: Float
    ) {
        SMALL_MASJID("Small Masjid", 0.6f, 0.4f, 0.3f, 0.7f),
        LARGE_MASJID("Large Masjid", 0.8f, 0.6f, 0.7f, 0.5f),
        HARAMAIN("Haramain", 0.9f, 0.8f, 0.9f, 0.3f)
    }

    var preset: ReverbPreset = ReverbPreset.LARGE_MASJID
    var isEnabled: Boolean = true

    var wetLevel: Float = 0.4f
        set(value) {
            field = max(0f, min(1f, value))
        }

    var dryLevel: Float = 1.0f
        set(value) {
            field = max(0f, min(1f, value))
        }

    constructor()

    constructor(preset: ReverbPreset) {
        this.preset = preset
        this.isEnabled = true
        this.wetLevel = 0.4f
        this.dryLevel = 1.0f
    }

    /**
     * Build FFmpeg filter chain for the reverb effect.
     * @param inputLabel Input audio label (e.g., "[0:a]")
     * @param outputLabel Output audio label (e.g., "[reverb_out]")
     * @return FFmpeg filter string
     */
    fun buildFFmpegFilter(inputLabel: String, outputLabel: String): String {
        if (!isEnabled) {
            return "${inputLabel}acopy$outputLabel"
        }

        val gain = preset.reverbGain
        val delay = preset.reverbDelay
        val early = preset.earlyReflections
        val decay = preset.decayFactor

        // Calculate echo parameters based on preset
        // aecho filter: in_gain:out_gain:delays:decays
        // Early reflections: short delays (15-40ms)
        // Main reverb: longer delays (60-200ms) with decay

        val delay1 = (15 + delay * 15).toInt()   // 15-30ms first reflection
        val delay2 = (25 + delay * 25).toInt()   // 25-50ms second reflection
        val delay3 = (50 + early * 80).toInt()    // 50-130ms early cluster
        val delay4 = (80 + early * 120).toInt()   // 80-200ms main reverb

        val decay1 = 0.5f + decay * 0.3f
        val decay2 = 0.4f + decay * 0.3f
        val decay3 = 0.3f + decay * 0.3f
        val decay4 = 0.2f + decay * 0.2f

        val inGain = 0.8f
        val outGain = wetLevel

        val sb = StringBuilder()

        // Apply aecho for early reflections + reverb tail
        sb.append(inputLabel)
        sb.append("aecho=")
        sb.append("%.2f".format(inGain)).append(":")
        sb.append("%.2f".format(outGain)).append(":")
        sb.append(delay1).append("|").append(delay2).append("|").append(delay3).append("|").append(delay4)
        sb.append(":")
        sb.append("%.2f".format(decay1)).append("|")
          .append("%.2f".format(decay2)).append("|")
          .append("%.2f".format(decay3)).append("|")
          .append("%.2f".format(decay4))

        // Add lowpass for natural reverb damping
        val cutoffFreq = (4000 + (1f - gain) * 4000).toInt() // 4-8kHz
        sb.append(",lowpass=f=").append(cutoffFreq)

        // Add a second aecho layer for larger spaces to simulate longer tail
        if (preset == ReverbPreset.LARGE_MASJID || preset == ReverbPreset.HARAMAIN) {
            val tailDelay = if (preset == ReverbPreset.HARAMAIN) 150 else 100
            val tailDecay = if (preset == ReverbPreset.HARAMAIN) 0.4f else 0.3f
            sb.append(",aecho=0.8:")
            sb.append("%.2f".format(outGain * 0.5f)).append(":")
            sb.append(tailDelay).append("|").append(tailDelay + 40)
            sb.append(":")
            sb.append("%.2f".format(tailDecay)).append("|")
              .append("%.2f".format(tailDecay * 0.7f))
        }

        // Mix wet and dry signals
        // We need to keep the dry signal and mix with the reverb
        // Using amix approach
        sb.append(outputLabel)

        return sb.toString()
    }

    /**
     * Build a full filter that preserves the dry signal and mixes with reverb.
     */
    fun buildFullFilter(inputLabel: String, outputLabel: String): String {
        if (!isEnabled) {
            return "${inputLabel}acopy$outputLabel"
        }

        // Split input into dry and wet paths
        val dryLabel = "[reverb_dry]"
        val wetLabel = "[reverb_wet]"

        val sb = StringBuilder()
        sb.append(inputLabel).append("asplit=2").append(dryLabel).append(wetLabel).append(";")

        // Process wet path with reverb
        sb.append(wetLabel)
        // Reverb processing inline
        val delay = preset.reverbDelay
        val early = preset.earlyReflections
        val decay = preset.decayFactor

        val d1 = (15 + delay * 15).toInt()
        val d2 = (25 + delay * 25).toInt()
        val d3 = (50 + early * 80).toInt()
        val dc1 = 0.5f + decay * 0.3f
        val dc2 = 0.4f + decay * 0.3f
        val dc3 = 0.3f + decay * 0.3f

        sb.append("aecho=0.8:").append("%.2f".format(wetLevel)).append(":")
        sb.append(d1).append("|").append(d2).append("|").append(d3)
        sb.append(":")
        sb.append("%.2f".format(dc1)).append("|")
          .append("%.2f".format(dc2)).append("|")
          .append("%.2f".format(dc3))
        sb.append(",lowpass=f=").append((4000 + (1f - preset.reverbGain) * 4000).toInt())
        sb.append("[wet];")

        // Mix dry and wet
        sb.append(dryLabel).append("volume=").append("%.2f".format(dryLevel)).append("[dry_vol];")
        sb.append("[dry_vol][wet]amix=inputs=2:duration=longest:dropout_transition=3")
        sb.append(outputLabel)

        return sb.toString()
    }

    companion object {
        private const val TAG = "MasjidReverbFilter"
    }
}
