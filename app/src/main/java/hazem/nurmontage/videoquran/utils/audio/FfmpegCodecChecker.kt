package hazem.nurmontage.videoquran.utils.audio

import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode

object FfmpegCodecChecker {

    private var cachedCodecs: CodecInfo? = null

    interface CodecCallback {
        fun onResult(codecInfo: CodecInfo)
    }

    data class CodecInfo(
        var videoCodec: String? = null,
        var audioCodec: String? = null,
        var isVideoHwAccelerated: Boolean = false
    )

    fun detectCodecsAsync(callback: CodecCallback) {
        cachedCodecs?.let {
            callback.onResult(it)
            return
        }

        FFmpegKit.executeAsync("-hide_banner -encoders", FFmpegSessionCompleteCallback { session ->
            val info = parseEncoders(session)
            cachedCodecs = info
            callback.onResult(info)
        })
    }

    private fun parseEncoders(session: FFmpegSession): CodecInfo {
        val info = CodecInfo()

        if (!ReturnCode.isSuccess(session.returnCode)) {
            Log.e("CodecCheck", "Failed to query FFmpeg encoders")
            return info
        }

        val output = session.output ?: return info

        var hasLibx264 = false
        var hasLibfdkAac = false
        var hasAac = false

        for (line in output.split("\n")) {
            val lower = line.trim().lowercase()
            if (!hasLibx264 && lower.contains("libx264")) hasLibx264 = true
            if (!hasLibfdkAac && lower.contains("libfdk_aac")) hasLibfdkAac = true
            if (!hasAac && lower.contains("aac")) hasAac = true
        }

        if (hasLibx264) {
            info.videoCodec = "libx264"
            info.isVideoHwAccelerated = false
        } else {
            info.videoCodec = null
            info.isVideoHwAccelerated = false
        }

        info.audioCodec = when {
            hasLibfdkAac -> "libfdk_aac"
            hasAac -> "aac"
            else -> null
        }

        return info
    }
}
