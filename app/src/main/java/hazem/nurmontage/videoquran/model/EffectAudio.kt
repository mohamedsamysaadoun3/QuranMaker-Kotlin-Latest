package hazem.nurmontage.videoquran.model

import java.io.Serializable

data class EffectAudio(
    var decays: Int = 0,
    var decays_cmd: String? = null,
    var delays: Int = 0,
    var delays_cmd: String? = null,
    var duration: Int = 0,
    var end: Float = 0f,
    var fade_in: Int = 0,
    var fade_out: Int = 0,
    var isEnhance: Boolean = false,
    var isRemoveNoice: Boolean = false,
    var outGain: Float = 0f,
    var reverbPreset: String? = null,
    var reverbPreset_index_list: Int = 0,
    var start: Float = 0f,
    var volume_echo: Int = 0,
    var volume: Float = 1.0f,
    var speed: Float = 1.0f
) : Serializable
