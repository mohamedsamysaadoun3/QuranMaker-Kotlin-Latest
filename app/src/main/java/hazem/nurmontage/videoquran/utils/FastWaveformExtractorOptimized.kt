package hazem.nurmontage.videoquran.utils

import android.media.MediaCodec
import android.media.MediaCrypto
import android.media.MediaExtractor
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

object FastWaveformExtractorOptimized {

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

        val result = FloatArray(numPoints)
        val chunkDurationUs = trackFormat.getLong(MediaFormat.KEY_DURATION) / numPoints
        val bufferInfo = MediaCodec.BufferInfo()

        @Suppress("DEPRECATION")
        val inputBuffers = codec.inputBuffers
        @Suppress("DEPRECATION")
        val outputBuffers = codec.outputBuffers

        var timeoutUs: Long = 0
        var pointIndex = 0
        var seekPositionUs: Long = 0

        while (pointIndex < numPoints) {
            extractor.seekTo(seekPositionUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val nextSeekPosition = seekPositionUs + chunkDurationUs

            val inputBufferIndex = codec.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex >= 0) {
                val sampleSize = extractor.readSampleData(inputBuffers[inputBufferIndex], 0)
                if (sampleSize < 0) {
                    break
                }
                codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                extractor.advance()
                timeoutUs = 0
            } else {
                timeoutUs = timeoutUs
            }

            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputBufferIndex >= 0) {
                result[pointIndex] = computeAmp(outputBuffers[outputBufferIndex], bufferInfo.size)
                codec.releaseOutputBuffer(outputBufferIndex, false)
                pointIndex++
            }

            seekPositionUs = nextSeekPosition
        }

        codec.stop()
        codec.release()
        extractor.release()
        return result
    }

    private fun computeAmp(buffer: ByteBuffer, size: Int): Float {
        buffer.position(0)
        var maxAmp = 0.0f
        var i = 0
        while (i < size - 1) {
            maxAmp = max(maxAmp, abs(buffer.getShort(i).toInt()) / 32767.0f)
            i += 2
        }
        return maxAmp
    }
}
