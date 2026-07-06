package hazem.nurmontage.videoquran.utils

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max

object PCMWaveformExtractor {

    @JvmStatic
    @Throws(IOException::class)
    fun extractWaveform(filePath: String, numPoints: Int): FloatArray {
        val result = FloatArray(numPoints)
        val buffer = ByteArray(8192)
        val totalSamples = (File(filePath).length() / 2).toInt()
        val samplesPerPoint = totalSamples.toFloat() / numPoints.toFloat()

        FileInputStream(filePath).use { fis ->
            var sampleIndex = 0
            while (true) {
                val read = fis.read(buffer)
                if (read <= 0) break

                var i = 0
                while (i < read - 1) {
                    val pointFloat = sampleIndex / samplesPerPoint
                    if (pointFloat >= numPoints) break

                    val pointIndex = pointFloat.toInt()
                    // Decode 16-bit little-endian PCM sample
                    val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                    result[pointIndex] = max(result[pointIndex], abs(sample.toInt()) / 32767.0f)
                    sampleIndex++
                    i += 2
                }
            }
        }
        return result
    }
}
