package hazem.nurmontage.videoquran.utils.waveform

import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Extracts amplitude data from an audio file using [MediaExtractor].
 *
 * The extraction pipeline:
 * 1. Opens the audio file and finds the first audio track.
 * 2. Reads raw PCM samples via [MediaExtractor.readSampleData].
 * 3. Converts bytes to 16-bit shorts via [ShortBuffer].
 * 4. Downsamples the samples into [targetSamples] buckets by taking
 *    the peak absolute value in each bucket.
 * 5. Normalizes the peak values to the range [0.0, 1.0] by dividing by 32767.
 *
 * The extraction runs on a single-thread executor and delivers results
 * on the main thread via [Handler].
 *
 * Converted from AmplitudeExtract.java — logic preserved exactly.
 */
class AmplitudeExtract {

    companion object {
        private const val TAG = "AudioAmplitudeReader"
    }

    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Callback interface for amplitude extraction results. */
    interface AmplitudeDataCallback {
        fun onComplete(amplitudes: List<Float>)
        fun onError(exception: Exception)
    }

    /**
     * Asynchronously extract amplitude data from [filePath] and deliver
     * [targetSamples] amplitude values via [callback].
     *
     * @param filePath      Path to the audio file on disk
     * @param targetSamples Number of amplitude samples to produce
     * @param callback      Receives results on the main thread
     */
    fun extractAmplitudeDataAsync(
        filePath: String,
        targetSamples: Int,
        callback: AmplitudeDataCallback
    ) {
        executorService.execute {
            try {
                val result = extractAmplitudeData(filePath, targetSamples)
                mainHandler.post { callback.onComplete(result) }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Error extracting amplitude data", e)
                mainHandler.post { callback.onError(e) }
            } catch (e: IOException) {
                Log.e(TAG, "Error extracting amplitude data", e)
                mainHandler.post { callback.onError(e) }
            }
        }
    }

    /**
     * Core extraction algorithm — runs on a background thread.
     *
     * @throws IllegalArgumentException if filePath is empty or targetSamples <= 0
     * @throws IOException if no audio track is found or file cannot be read
     */
    private fun extractAmplitudeData(filePath: String, targetSamples: Int): List<Float> {
        require(filePath.isNotEmpty()) { "File path cannot be null or empty." }
        require(targetSamples > 0) { "Target samples must be greater than zero." }

        val result = mutableListOf<Float>()
        var extractor: MediaExtractor? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(filePath)

            // Find the first audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString("mime")
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IOException("No audio track found in $filePath")
            }

            extractor.selectTrack(audioTrackIndex)

            if (audioFormat.getLong("durationUs") <= 0) {
                Log.w(TAG, "Duration not available or invalid, results might be inaccurate for downsampling.")
            }

            // Read all raw PCM samples into a list of Shorts
            val buffer = ByteBuffer.allocate(16384)
            buffer.order(ByteOrder.nativeOrder())
            val rawSamples = mutableListOf<Short>()

            while (true) {
                val readBytes = extractor.readSampleData(buffer, 0)
                if (readBytes < 0) break
                if (readBytes > 0) {
                    buffer.position(0)
                    buffer.limit(readBytes)
                    val shortBuffer: ShortBuffer = buffer.asShortBuffer()
                    while (shortBuffer.hasRemaining()) {
                        rawSamples.add(shortBuffer.get())
                    }
                }
                buffer.clear()
                extractor.advance()
            }

            // Downsample into targetSamples buckets by peak detection
            if (rawSamples.isNotEmpty()) {
                val sampleCount = rawSamples.size
                val bucketSize = maxOf(1, sampleCount / targetSamples)

                for (i in 0 until targetSamples) {
                    var from = i * bucketSize
                    val to = minOf(from + bucketSize, sampleCount)
                    if (from >= sampleCount) {
                        result.add(0.0f)
                    } else {
                        var peak: Short = 0
                        while (from < to) {
                            val sample = rawSamples[from]
                            if (Math.abs(sample.toInt()) > Math.abs(peak.toInt())) {
                                peak = sample
                            }
                            from++
                        }
                        result.add(Math.abs(peak.toInt()) / 32767.0f)
                    }
                }
            } else {
                // No samples: return all zeros
                repeat(targetSamples) { result.add(0.0f) }
            }
        } finally {
            extractor?.release()
        }

        return result
    }
}
