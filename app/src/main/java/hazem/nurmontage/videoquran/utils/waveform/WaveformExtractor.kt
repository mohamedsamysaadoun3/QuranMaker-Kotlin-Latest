package hazem.nurmontage.videoquran.utils.waveform

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat

object WaveformExtractor {

    fun extractAmplitudes(filePath: String, targetSamples: Int): FloatArray {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(filePath)
            val audioTrackIndex = selectAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return FloatArray(targetSamples)
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            @Suppress("UNUSED_VARIABLE")
            val sampleRate = format.getInteger("sample-rate")

            val codec = MediaCodec.createDecoderByType(format.getString("mime")!!)
            codec.configure(format, null, null, 0)
            codec.start()

            @Suppress("DEPRECATION")
            var inputBuffers = codec.inputBuffers
            @Suppress("DEPRECATION")
            var outputBuffers = codec.outputBuffers

            val amplitudes = FloatArray(targetSamples)
            val counts = FloatArray(targetSamples)
            val bucketDuration = (format.getLong("durationUs") / 1000000.0f) / targetSamples

            val bufferInfo = MediaCodec.BufferInfo()
            var eos = false

            while (true) {
                if (!eos) {
                    val inputIndex = codec.dequeueInputBuffer(10000L)
                    if (inputIndex >= 0) {
                        val readBytes = extractor.readSampleData(inputBuffers[inputIndex], 0)
                        if (readBytes < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eos = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, readBytes, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                if (outputIndex >= 0) {
                    val outputBuffer = outputBuffers[outputIndex]
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                    val shortBuffer = outputBuffer.asShortBuffer()
                    val remaining = shortBuffer.remaining()
                    for (i in 0 until remaining) {
                        val absValue = Math.abs(shortBuffer[i].toInt()) / 32768.0f
                        val bucket = (bufferInfo.presentationTimeUs / 1000000.0f / bucketDuration).toInt()
                        if (bucket < targetSamples) {
                            amplitudes[bucket] += absValue
                            counts[bucket] += 1.0f
                        }
                    }

                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }

            for (i in 0 until targetSamples) {
                if (counts[i] > 0f) {
                    amplitudes[i] /= counts[i]
                }
            }

            codec.stop()
            codec.release()
            extractor.release()
            return amplitudes

        } catch (_: Exception) {
            return FloatArray(targetSamples)
        }
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString("mime")?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }
}
