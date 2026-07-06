package hazem.nurmontage.videoquran.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.Log

/**
 * Detects hardware encoder availability and selects the best codec for the device.
 *
 * Scans [MediaCodecList] for hardware-accelerated encoders, filtering out
 * software-only implementations (OMX.google, ffmpeg, avcodec, software).
 * The selection priority is: H.265 HW → H.264 HW → H.264 SW.
 *
 * Also provides recommended encoding parameters (bitrate, frame rate, i-frame interval)
 * tailored to the selected encoder type.
 *
 * Converted from CodecOptimizer.java — logic preserved exactly.
 */
object CodecOptimizer {

    private const val TAG = "CodecOptimizer"

    const val MIMETYPE_VIDEO_AVC = "video/avc"
    const val MIMETYPE_VIDEO_HEVC = "video/hevc"
    const val MIMETYPE_VIDEO_VP9 = "video/vp9"

    /**
     * Enumeration of supported encoder types.
     *
     * Each value encodes both the codec standard (H.264, H.265, VP9)
     * and the implementation (hardware vs. software).
     */
    enum class EncoderType {
        H264_HARDWARE,
        H264_SOFTWARE,
        H265_HARDWARE,
        H265_SOFTWARE,
        VP9_HARDWARE,
        VP9_SOFTWARE
    }

    /**
     * Check if a hardware encoder is available for the given MIME type.
     *
     * Iterates over all codecs reported by [MediaCodecList] and returns `true`
     * only if an encoder is found that supports the requested MIME type **and**
     * is NOT a software-only implementation (OMX.google, ffmpeg, avcodec, software).
     *
     * @param mimeType The MIME type to search for (e.g. [MIMETYPE_VIDEO_AVC])
     * @return `true` if a hardware encoder for the given MIME type exists
     */
    fun hasHardwareEncoder(mimeType: String): Boolean {
        val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val codecs = codecList.codecInfos

        for (codec in codecs) {
            if (codec.isEncoder) {
                val types = codec.supportedTypes
                for (type in types) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        val name = codec.name.lowercase()
                        if (!name.contains("omx.google") &&
                            !name.contains("ffmpeg") &&
                            !name.contains("avcodec") &&
                            !name.contains("software")
                        ) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Select the best encoder type for this device.
     *
     * Selection priority:
     * 1. H.265 hardware encoder (best compression, modern devices)
     * 2. H.264 hardware encoder (widest compatibility)
     * 3. H.264 software fallback (guaranteed availability via libx264)
     *
     * @return The recommended [EncoderType] for this device
     */
    fun selectBestEncoder(): EncoderType {
        if (hasHardwareEncoder(MIMETYPE_VIDEO_HEVC)) {
            Log.i(TAG, "Selected H.265 hardware encoder")
            return EncoderType.H265_HARDWARE
        }

        if (hasHardwareEncoder(MIMETYPE_VIDEO_AVC)) {
            Log.i(TAG, "Selected H.264 hardware encoder")
            return EncoderType.H264_HARDWARE
        }

        Log.i(TAG, "No hardware encoder found, using H.264 software")
        return EncoderType.H264_SOFTWARE
    }

    /**
     * Get the MIME type for an encoder type.
     *
     * Maps each [EncoderType] to its corresponding MIME type string.
     *
     * @param type The encoder type
     * @return The MIME type string (defaults to [MIMETYPE_VIDEO_AVC])
     */
    fun getMimeType(type: EncoderType): String = when (type) {
        EncoderType.H264_HARDWARE,
        EncoderType.H264_SOFTWARE -> MIMETYPE_VIDEO_AVC

        EncoderType.H265_HARDWARE,
        EncoderType.H265_SOFTWARE -> MIMETYPE_VIDEO_HEVC

        EncoderType.VP9_HARDWARE,
        EncoderType.VP9_SOFTWARE -> MIMETYPE_VIDEO_VP9
    }

    /**
     * Get recommended encoding parameters for the device.
     *
     * Provides sensible defaults for bitrate, frame rate, and i-frame interval
     * based on the selected [EncoderType]. H.265 hardware gets a lower bitrate
     * (8 Mbps) than H.264 hardware (12 Mbps) due to better compression efficiency.
     * Software encoding falls back to 10 Mbps.
     *
     * @param encoderType The selected encoder type
     * @return [EncodingParams] with recommended settings
     */
    fun getRecommendedParams(encoderType: EncoderType): EncodingParams {
        val params = EncodingParams(
            mimeType = getMimeType(encoderType),
            isHardware = encoderType == EncoderType.H264_HARDWARE ||
                    encoderType == EncoderType.H265_HARDWARE ||
                    encoderType == EncoderType.VP9_HARDWARE
        )

        when (encoderType) {
            EncoderType.H265_HARDWARE -> {
                params.bitrate = 8_000_000
                params.frameRate = 30
                params.iFrameInterval = 2
            }
            EncoderType.H264_HARDWARE -> {
                params.bitrate = 12_000_000
                params.frameRate = 30
                params.iFrameInterval = 2
            }
            else -> {
                params.bitrate = 10_000_000
                params.frameRate = 30
                params.iFrameInterval = 2
            }
        }

        return params
    }

    /**
     * Holds recommended encoding parameters for video export.
     *
     * @property mimeType       The MIME type for the encoder (e.g. "video/avc")
     * @property bitrate        Target bitrate in bits per second
     * @property frameRate      Target frame rate in frames per second
     * @property iFrameInterval Key-frame interval in seconds
     * @property isHardware     Whether a hardware-accelerated encoder is being used
     */
    data class EncodingParams(
        var mimeType: String = MIMETYPE_VIDEO_AVC,
        var bitrate: Int = 10_000_000,
        var frameRate: Int = 30,
        var iFrameInterval: Int = 2,
        var isHardware: Boolean = false
    )
}
