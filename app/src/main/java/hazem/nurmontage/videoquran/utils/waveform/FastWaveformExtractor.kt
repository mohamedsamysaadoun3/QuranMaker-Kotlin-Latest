package hazem.nurmontage.videoquran.utils.waveform

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

object FastWaveformExtractor {

    fun extract(filePath: String, targetSamples: Int): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(filePath)

        var audioTrackIndex = -1
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString("mime")?.startsWith("audio/") == true) {
                audioTrackIndex = i
                break
            }
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)

        val codec = MediaCodec.createDecoderByType(format.getString("mime")!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val result = FloatArray(targetSamples)
        val segmentDuration = format.getLong("durationUs") / targetSamples

        @Suppress("DEPRECATION")
        var inputBuffers = codec.inputBuffers
        @Suppress("DEPRECATION")
        val outputBuffers = codec.outputBuffers

        val bufferInfo = MediaCodec.BufferInfo()
        var currentTime = 0L
        var sampleIndex = 0

        while (sampleIndex < targetSamples) {
            extractor.seekTo(currentTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val segmentEnd = currentTime + segmentDuration

            val inputIndex = codec.dequeueInputBuffer(5000L)
            if (inputIndex >= 0) {
                val readBytes = extractor.readSampleData(inputBuffers[inputIndex], 0)
                if (readBytes < 0) break
                codec.queueInputBuffer(inputIndex, 0, readBytes, extractor.sampleTime, 0)
                extractor.advance()
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 5000L)
            if (outputIndex >= 0) {
                result[sampleIndex] = computeAmp(outputBuffers[outputIndex], bufferInfo.size)
                sampleIndex++
                codec.releaseOutputBuffer(outputIndex, false)
            }

            currentTime = segmentEnd
            @Suppress("DEPRECATION")
            inputBuffers = codec.inputBuffers
        }

        codec.stop()
        codec.release()
        extractor.release()
        return result
    }

    private fun computeAmp(buffer: ByteBuffer, size: Int): Float {
        buffer.position(0)
        var peak = 0f
        var i = 0
        while (i < size - 1) {
            peak = maxOf(peak, Math.abs(buffer.getShort(i).toInt()) / 32767.0f)
            i += 2
        }
        return peak
    }
}
