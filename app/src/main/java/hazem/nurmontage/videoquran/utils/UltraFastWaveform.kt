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
import kotlin.math.min

object UltraFastWaveform {

    @JvmStatic
    @Throws(IOException::class)
    fun extractAmplitudes(filePath: String, numPoints: Int): FloatArray {
        val pcmData = decodeToPCM(filePath)
        val length = pcmData.size
        val result = FloatArray(numPoints)
        val samplesPerPoint = length.toDouble() / numPoints.toDouble()

        var i = 0
        while (i < numPoints) {
            val nextI = i + 1
            val end = min((nextI * samplesPerPoint).toInt(), length)
            var maxAmp = 0.0f
            var j = (i * samplesPerPoint).toInt()
            while (j < end) {
                maxAmp = max(maxAmp, abs(pcmData[j].toInt()) / 32767.0f)
                j++
            }
            result[i] = maxAmp
            i = nextI
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun decodeToPCM(filePath: String): ShortArray {
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

        val sampleList = mutableListOf<Short>()
        val bufferInfo = MediaCodec.BufferInfo()
        var eos = false

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
                    while (outputBuffer.remaining() > 1) {
                        sampleList.add(outputBuffer.short)
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

        val result = ShortArray(sampleList.size)
        for (i in sampleList.indices) {
            result[i] = sampleList[i]
        }
        return result
    }
}
