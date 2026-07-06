package hazem.nurmontage.videoquran.utils

import android.media.MediaCodec
import android.media.MediaCrypto
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

object FastWaveformExtractorPro {

    private const val RAW_POINTS = 2000

    @JvmStatic
    @Throws(Exception::class)
    @Suppress("DEPRECATION")
    fun extract(filePath: String, numPoints: Int): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var audioTrackIndex = 0
        while (audioTrackIndex < extractor.trackCount) {
            if (extractor.getTrackFormat(audioTrackIndex).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                break
            }
            audioTrackIndex++
        }
        if (audioTrackIndex >= extractor.trackCount) {
            audioTrackIndex = -1
        }

        extractor.selectTrack(audioTrackIndex)
        val trackFormat = extractor.getTrackFormat(audioTrackIndex)

        val codec = MediaCodec.createDecoderByType(trackFormat.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(trackFormat, null as Surface?, null as MediaCrypto?, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val rawAmplitudes = FloatArray(RAW_POINTS)
        var eos = false
        var rawIndex = 0

        while (!eos) {
            val inputBufferIndex = codec.dequeueInputBuffer(0L)
            if (inputBufferIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                if (sampleSize < 0) {
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    eos = true
                } else {
                    codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }

            while (true) {
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                if (outputBufferIndex < 0) break

                if (rawIndex < RAW_POINTS) {
                    rawAmplitudes[rawIndex] = computeMaxAmp(codec.getOutputBuffer(outputBufferIndex)!!, bufferInfo.size)
                    rawIndex++
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    eos = true
                    break
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        return downsample(rawAmplitudes, rawIndex, numPoints)
    }

    private fun computeMaxAmp(buffer: ByteBuffer, size: Int): Float {
        buffer.position(0)
        var maxAmp = 0.0f
        var i = 0
        while (i < size - 1) {
            maxAmp = max(maxAmp, abs(buffer.getShort(i).toInt()).toFloat())
            i += 2
        }
        return maxAmp / 32767.0f
    }

    private fun downsample(data: FloatArray, rawCount: Int, targetPoints: Int): FloatArray {
        val result = FloatArray(targetPoints)
        val ratio = rawCount.toFloat() / targetPoints.toFloat()

        var i = 0
        while (i < targetPoints) {
            val nextI = i + 1
            val end = (nextI * ratio).toInt()
            var maxVal = 0.0f
            var j = (i * ratio).toInt()
            while (j < end && j < rawCount) {
                maxVal = max(maxVal, data[j])
                j++
            }
            result[i] = maxVal
            i = nextI
        }
        return result
    }
}
