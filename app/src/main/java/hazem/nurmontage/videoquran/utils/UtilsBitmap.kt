package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur

@Suppress("DEPRECATION")
object UtilsBitmap {

    @JvmStatic
    fun cropToSquare(bitmap: Bitmap, rect: Rect, width: Int, height: Int): Bitmap {
        val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, width, height)
        return if (cropped.width == rect.width() && bitmap.height == rect.height()) {
            cropped
        } else {
            Bitmap.createScaledBitmap(cropped, rect.width(), rect.height(), true)
        }
    }

    @JvmStatic
    fun cropToSquare(bitmap: Bitmap, rect: Rect, scaleX: Float, scaleY: Float): Bitmap {
        val w = (bitmap.width * scaleX).toInt()
        val h = (bitmap.height * scaleY).toInt()
        val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, w, h)
        return if (cropped.width == rect.width() && bitmap.height == rect.height()) {
            cropped
        } else {
            Bitmap.createScaledBitmap(cropped, rect.width(), rect.height(), true)
        }
    }

    @JvmStatic
    fun cropToSquare(bitmap: Bitmap, rect: Rect): Bitmap {
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }

    @JvmStatic
    fun cropToSquareWithRoundCorners(
        bitmap: Bitmap, rect: Rect, radius: Int, scaleX: Float, scaleY: Float
    ): Bitmap {
        val w = (bitmap.width * scaleX).toInt()
        val h = (bitmap.height * scaleY).toInt()
        var cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, w, h)
        if (h != rect.height() || w != rect.width()) {
            cropped = Bitmap.createScaledBitmap(cropped, rect.width(), rect.height(), true)
        }
        return applyRoundCorners(cropped, radius.toFloat())
    }

    @JvmStatic
    fun cropToSquareWithRoundCorners(
        bitmap: Bitmap, rect: Rect, radius: Int, width: Int, height: Int
    ): Bitmap {
        var cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, width, height)
        if (height != rect.height() || width != rect.width()) {
            cropped = Bitmap.createScaledBitmap(cropped, rect.width(), rect.height(), true)
        }
        return applyRoundCorners(cropped, radius.toFloat())
    }

    @JvmStatic
    fun cropToSquareWithRoundCorners(bitmap: Bitmap, rect: Rect, radius: Int): Bitmap {
        val cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
        return applyRoundCorners(cropped, radius.toFloat())
    }

    @JvmStatic
    fun cropToSquareWithRoundCornersPlusScale(
        bitmap: Bitmap, rect: Rect, radius: Int, targetWidth: Int, targetHeight: Int
    ): Bitmap {
        var cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
        if (targetHeight != rect.height() || targetWidth != rect.width()) {
            cropped = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        }
        return applyRoundCorners(cropped, radius.toFloat())
    }

    @JvmStatic
    fun cropToSquareWithRoundCorners(
        bitmap: Bitmap, cropWidth: Int, cropHeight: Int, radius: Int
    ): Bitmap {
        val cropped = Bitmap.createBitmap(
            bitmap,
            (bitmap.width - cropWidth) / 2,
            (bitmap.height - cropHeight) / 2,
            cropWidth, cropHeight
        )
        return applyRoundCorners(cropped, radius.toFloat(), cropWidth, cropHeight)
    }

    @JvmStatic
    fun getResizedBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val matrix = Matrix()
        matrix.postScale(newWidth.toFloat() / width, newHeight.toFloat() / height)
        val result = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
        bitmap.recycle()
        return result
    }

    @JvmStatic
    fun blur(context: Context, bitmap: Bitmap, radius: Int, scaleFactor: Int): Bitmap? {
        if (radius < 1) return null

        val width = bitmap.width
        val height = bitmap.height
        val scaled = Bitmap.createScaledBitmap(bitmap, width / scaleFactor, height / scaleFactor, false)

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, scaled)
        val output = Allocation.createTyped(rs, input.type)
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setRadius(radius.toFloat())
        blur.setInput(input)
        blur.forEach(output)
        output.copyTo(scaled)
        rs.destroy()

        return Bitmap.createScaledBitmap(scaled, width, height, false)
    }

    @JvmStatic
    fun blurInSave(
        context: Context, bitmap: Bitmap, radius: Int, scaleFactor: Int,
        outputWidth: Int, outputHeight: Int
    ): Bitmap? {
        if (radius < 1) return null

        val scaled = Bitmap.createScaledBitmap(
            bitmap, bitmap.width / scaleFactor, bitmap.height / scaleFactor, false
        )

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, scaled)
        val output = Allocation.createTyped(rs, input.type)
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setRadius(radius.toFloat())
        blur.setInput(input)
        blur.forEach(output)
        output.copyTo(scaled)
        rs.destroy()

        return Bitmap.createScaledBitmap(scaled, outputWidth, outputHeight, false)
    }

    @JvmStatic
    fun cropBitmap(bitmap: Bitmap, cropWidth: Int, cropHeight: Int): Bitmap {
        var x = (bitmap.width - cropWidth) / 2
        var y = (bitmap.height - cropHeight) / 2
        var w = cropWidth
        var h = cropHeight

        if (x < 0) x = 0
        if (y < 0) y = 0
        if (x + w > bitmap.width) w = bitmap.width - x
        if (y + h > bitmap.height) h = bitmap.height - y

        return Bitmap.createBitmap(bitmap, x, y, w, h)
    }

    private fun applyRoundCorners(bitmap: Bitmap, radius: Float): Bitmap {
        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val path = Path()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path, paint)
        return output
    }

    private fun applyRoundCorners(bitmap: Bitmap, radius: Float, w: Int, h: Int): Bitmap {
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val path = Path()
        path.addRoundRect(rect, radius, radius, Path.Direction.CW)

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(path, paint)
        return output
    }
}
