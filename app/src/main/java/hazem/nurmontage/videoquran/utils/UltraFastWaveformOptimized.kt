package hazem.nurmontage.videoquran.utils

import android.media.MediaCodec
import android.media.MediaCrypto
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

object UltraFastWaveformOptimized {

    @JvmStatic
    @Throws(IOException::class)
    @Suppress("DEPRECATION")
    fun extractAmplitudes(filePath: String, numPoints: Int): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var audioTrackIndex = -1
        var audioFormat: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                audioFormat = format
                break
            }
        }

        if (audioTrackIndex == -1) {
            throw IOException("No audio track found")
        }

        extractor.selectTrack(audioTrackIndex)

        val codec = MediaCodec.createDecoderByType(audioFormat!!.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(audioFormat, null as Surface?, null as MediaCrypto?, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val result = FloatArray(numPoints)
        @Suppress("UNUSED_VARIABLE")
        val durationUs = audioFormat.getLong(MediaFormat.KEY_DURATION)

        var eos = false
        var sampleIndex = 0

        while (!eos) {
            val inputBufferIndex = codec.dequeueInputBuffer(1000L)
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
                val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 1000L)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                    outputBuffer!!.position(0)
                    val numSamples = bufferInfo.size / 2
                    // Java uses integer division: float f = i4 / value
                    val samplesPerPoint = (numSamples / numPoints).toFloat()

                    var i = 0
                    while (i < numSamples) {
                        // Java reads at byte offsets 0, 2, 4, ... (step by 2 bytes = 1 short)
                        val sample = outputBuffer.getShort(i)
                        val pointIndex = (sampleIndex / samplesPerPoint).toInt()
                        if (pointIndex >= numPoints) break
                        result[pointIndex] = max(result[pointIndex], abs(sample.toInt()) / 32767.0f)
                        sampleIndex++
                        i += 2
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                } else {
                    break
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()
        return result
    }
}
