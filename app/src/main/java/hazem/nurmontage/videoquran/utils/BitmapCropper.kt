package hazem.nurmontage.videoquran.utils

import android.graphics.Bitmap
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Utility for cropping bitmaps to specific aspect ratios with optional size constraints.
 *
 * Supports three common aspect ratios used throughout the app:
 * - **16:9** — Landscape video (YouTube, standard TV)
 * - **9:16** — Portrait video (Stories, Reels, TikTok)
 * - **1:1** — Square (Instagram posts)
 *
 * Each ratio has two variants:
 * - `cropToXxY(bitmap)` — Crops to the exact aspect ratio, no size constraint
 * - `cropToXxY(bitmap, maxWidth, maxHeight)` — Crops to the aspect ratio within
 *   a maximum bounding box, ensuring the result is no larger than the constraints
 *
 * All cropping calculations ensure even pixel dimensions (required by many video
 * encoders including FFmpeg) by using [roundEven].
 *
 * Converted from BitmapCropper.java — logic preserved exactly.
 */
object BitmapCropper {

    // ════════════════════════════════════════════════════════════════════
    //  16:9 — Landscape
    // ════════════════════════════════════════════════════════════════════

    /**
     * Crop a bitmap to 16:9 landscape aspect ratio without size constraints.
     *
     * @param bitmap The source bitmap
     * @return A new bitmap cropped to 16:9, or `null` if the source is null
     */
    fun cropTo16x9(bitmap: Bitmap?): Bitmap? {
        return cropToAspectRatio(bitmap, 16, 9)
    }

    /**
     * Crop a bitmap to 16:9 landscape aspect ratio with maximum size constraints.
     *
     * @param bitmap    The source bitmap
     * @param maxWidth  Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @return A new bitmap cropped to 16:9 within the constraints, or `null` if the source is null
     */
    fun cropTo16x9(bitmap: Bitmap?, maxWidth: Int, maxHeight: Int): Bitmap? {
        return cropToAspectRatioWithConstraint(bitmap, 16, 9, maxWidth, maxHeight)
    }

    // ════════════════════════════════════════════════════════════════════
    //  9:16 — Portrait
    // ════════════════════════════════════════════════════════════════════

    /**
     * Crop a bitmap to 9:16 portrait aspect ratio without size constraints.
     *
     * @param bitmap The source bitmap
     * @return A new bitmap cropped to 9:16, or `null` if the source is null
     */
    fun cropTo9x16(bitmap: Bitmap?): Bitmap? {
        return cropToAspectRatio(bitmap, 9, 16)
    }

    /**
     * Crop a bitmap to 9:16 portrait aspect ratio with maximum size constraints.
     *
     * @param bitmap    The source bitmap
     * @param maxWidth  Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @return A new bitmap cropped to 9:16 within the constraints, or `null` if the source is null
     */
    fun cropTo9x16(bitmap: Bitmap?, maxWidth: Int, maxHeight: Int): Bitmap? {
        return cropToAspectRatioWithConstraint(bitmap, 9, 16, maxWidth, maxHeight)
    }

    // ════════════════════════════════════════════════════════════════════
    //  1:1 — Square
    // ════════════════════════════════════════════════════════════════════

    /**
     * Crop a bitmap to 1:1 square aspect ratio without size constraints.
     *
     * @param bitmap The source bitmap
     * @return A new bitmap cropped to 1:1, or `null` if the source is null
     */
    fun cropTo1x1(bitmap: Bitmap?): Bitmap? {
        return cropToAspectRatio(bitmap, 1, 1)
    }

    /**
     * Crop a bitmap to 1:1 square aspect ratio with maximum size constraints.
     *
     * @param bitmap    The source bitmap
     * @param maxWidth  Maximum width in pixels
     * @param maxHeight Maximum height in pixels
     * @return A new bitmap cropped to 1:1 within the constraints, or `null` if the source is null
     */
    fun cropTo1x1(bitmap: Bitmap?, maxWidth: Int, maxHeight: Int): Bitmap? {
        return cropToAspectRatioWithConstraint(bitmap, 1, 1, maxWidth, maxHeight)
    }

    // ════════════════════════════════════════════════════════════════════
    //  Core cropping logic
    // ════════════════════════════════════════════════════════════════════

    /**
     * Crop a bitmap to an arbitrary aspect ratio without size constraints.
     *
     * Algorithm:
     * - If the source is wider than the target ratio -> crop the sides (center-crop horizontally)
     * - If the source is taller than the target ratio -> crop top/bottom (center-crop vertically)
     * - If the source already matches the target ratio -> return the original bitmap
     *
     * All crop dimensions are rounded to even numbers for encoder compatibility.
     *
     * @param bitmap       The source bitmap (may be null)
     * @param ratioWidth   The width component of the target aspect ratio
     * @param ratioHeight  The height component of the target aspect ratio
     * @return A new cropped bitmap, the original if already matching, or null
     */
    private fun cropToAspectRatio(bitmap: Bitmap?, ratioWidth: Int, ratioHeight: Int): Bitmap? {
        if (bitmap == null) return null

        var w = bitmap.width
        var h = bitmap.height
        val currentRatio = w.toFloat() / h.toFloat()
        val targetRatio = ratioWidth.toFloat() / ratioHeight.toFloat()

        var x = 0
        var y = 0

        if (currentRatio > targetRatio) {
            // Source is wider than target -> crop sides
            val newW = roundEven(h.toFloat() * targetRatio)
            x = (w - newW) / 2
            w = newW
            y = 0
        } else if (currentRatio < targetRatio) {
            // Source is taller than target -> crop top/bottom
            val newH = roundEven(w.toFloat() / targetRatio)
            y = (h - newH) / 2
            h = newH
        } else {
            // Already matches the target ratio
            return bitmap
        }

        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    /**
     * Crop a bitmap to an aspect ratio with maximum size constraints.
     *
     * This is a two-stage operation:
     * 1. **Downscale**: The bitmap is first constrained to fit within maxWidth x maxHeight,
     *    center-cropping if the bitmap exceeds either dimension.
     * 2. **Aspect-ratio crop**: The constrained region is then further cropped to the
     *    target aspect ratio, again center-cropping the excess dimension.
     *
     * All final dimensions are rounded to even numbers (& (-2) bitmask) to ensure
     * compatibility with video encoders that require even-pixel dimensions.
     *
     * @param bitmap       The source bitmap (may be null)
     * @param ratioWidth   The width component of the target aspect ratio
     * @param ratioHeight  The height component of the target aspect ratio
     * @param maxWidth     Maximum width constraint in pixels
     * @param maxHeight    Maximum height constraint in pixels
     * @return A new cropped and constrained bitmap, or null
     */
    private fun cropToAspectRatioWithConstraint(
        bitmap: Bitmap?,
        ratioWidth: Int,
        ratioHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        if (bitmap == null) return null

        val srcW = bitmap.width
        val srcH = bitmap.height

        // Stage 1: Constrain to max dimensions
        var constrainedW = min(srcW, maxWidth)
        var constrainedH = min(srcH, maxHeight)
        var offsetX = (srcW - constrainedW) / 2
        var offsetY = (srcH - constrainedH) / 2

        // Stage 2: Further crop to target aspect ratio within the constrained region
        val targetRatio = ratioWidth.toFloat() / ratioHeight.toFloat()
        val constrainedRatio = constrainedW.toFloat() / constrainedH.toFloat()

        var finalW: Int
        var finalH: Int

        if (constrainedRatio > targetRatio) {
            // Constrained region is wider than target -> reduce width
            finalW = roundEven(constrainedH.toFloat() * targetRatio)
            finalH = constrainedH
        } else if (constrainedRatio < targetRatio) {
            // Constrained region is taller than target -> reduce height
            finalW = constrainedW
            finalH = roundEven(constrainedW.toFloat() / targetRatio)
        } else {
            // Already matches target ratio
            finalW = constrainedW
            finalH = constrainedH
        }

        // Ensure even dimensions for encoder compatibility (& -2 clears the LSB)
        val evenW = min(finalW, constrainedW) and -2
        val evenH = min(finalH, constrainedH) and -2

        // Recalculate offsets to center the crop within the constrained region
        val cropX = offsetX + (constrainedW - evenW) / 2
        val cropY = offsetY + (constrainedH - evenH) / 2

        return Bitmap.createBitmap(bitmap, cropX, cropY, evenW, evenH)
    }

    /**
     * Round a float to the nearest even integer.
     *
     * Video encoders (including FFmpeg's libx264) typically require even-pixel
     * dimensions. This function rounds to the nearest integer, then subtracts 1
     * if the result is odd.
     *
     * Examples:
     * - 107.6 -> 108 (even, kept) -> 108
     * - 107.3 -> 107 (odd) -> 106
     * - 100.0 -> 100 (even, kept) -> 100
     *
     * @param value The float value to round
     * @return The nearest even integer
     */
    private fun roundEven(value: Float): Int {
        val rounded = round(value).roundToInt()
        return if (rounded % 2 == 0) rounded else rounded - 1
    }
}
