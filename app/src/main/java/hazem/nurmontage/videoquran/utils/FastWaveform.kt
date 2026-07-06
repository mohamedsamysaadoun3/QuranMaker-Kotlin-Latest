package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCrypto
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object FastWaveform {

    private const val DEFAULT_TIMEOUT_US = 10000L

    @JvmStatic
    @Suppress("DEPRECATION")
    fun decodeWaveform(context: Context, uri: Uri, numPoints: Int): FloatArray {
        val result = FloatArray(numPoints)
        val extractor = MediaExtractor()
        var mediaFormat: MediaFormat? = null
        var audioTrackIndex = -1

        try {
            extractor.setDataSource(context, uri, null as? Map<String, String>)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    mediaFormat = format
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (audioTrackIndex < 0) {
            return result
        }

        extractor.selectTrack(audioTrackIndex)

        val codec = MediaCodec.createDecoderByType(mediaFormat!!.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(mediaFormat, null as Surface?, null as MediaCrypto?, 0)
        codec.start()

        val samplesPerPoint = ((mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000)
                * mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)) / numPoints

        val bufferInfo = MediaCodec.BufferInfo()
        var eos = false
        var pointIndex = 0
        var maxAmp = 0.0f
        var samplesInBucket = 0L

        while (!eos) {
            val timeoutUs: Long
            val inputBufferIndex = codec.dequeueInputBuffer(DEFAULT_TIMEOUT_US)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
                timeoutUs = DEFAULT_TIMEOUT_US
            } else {
                timeoutUs = 10000
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                outputBuffer!!.order(ByteOrder.LITTLE_ENDIAN)

                while (outputBuffer.remaining() > 1) {
                    val amp = abs(outputBuffer.short.toInt()) / 32768.0f
                    if (amp > maxAmp) {
                        maxAmp = amp
                    }
                    samplesInBucket++
                    if (samplesInBucket >= samplesPerPoint) {
                        result[pointIndex] = maxAmp
                        pointIndex++
                        if (pointIndex >= numPoints) {
                            eos = true
                            break
                        }
                        maxAmp = 0.0f
                        samplesInBucket = 0
                    }
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    eos = true
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()
        return result
    }
}
