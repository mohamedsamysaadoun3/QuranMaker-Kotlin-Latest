package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat

class CassetteView : View {

    private var labelText: String = "Titanium – David Guetta Ft. Sia"

    private val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E74C3C") }
    private val paintShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A93226") }
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAE5D3") }
    private val paintReel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ViewCompat.MEASURED_STATE_MASK }
    private val paintHole = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5DADE2") }
    private val paintAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E67E22") }
    private val paintScrew = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1 }
    private val paintFloor = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6EC6E9") }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ViewCompat.MEASURED_STATE_MASK
        textSize = 36.0f
    }

    constructor(context: Context) : super(context) { labelText = "Titanium – David Guetta Ft. Sia" }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { labelText = "Titanium – David Guetta Ft. Sia" }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { labelText = "Titanium – David Guetta Ft. Sia" }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawColor(Color.parseColor("#87CEEB"))
        val floorTop = 0.7f * h
        val floorPath = Path().apply {
            moveTo(0f, floorTop)
            lineTo(w, floorTop)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        canvas.drawPath(floorPath, paintFloor)

        val leftX = w * 0.1f
        val topY = h * 0.3f
        val rightX = 0.9f * w
        val shadowRect = RectF(leftX, topY, rightX, floorTop)
        canvas.drawRoundRect(shadowRect, 20.0f, 20.0f, paintShadow)

        val bodyRect = RectF(leftX, topY, rightX, floorTop)
        canvas.drawRoundRect(bodyRect, 20.0f, 20.0f, paintBody)

        val accentTopRight = w * 0.85f
        val accentTopBottom = h * 0.35f
        val accentLeft = 0.15f * w
        val topAccent = Path().apply {
            moveTo(leftX, topY)
            lineTo(rightX, topY)
            lineTo(accentTopRight, accentTopBottom)
            lineTo(accentLeft, accentTopBottom)
            close()
        }
        canvas.drawPath(topAccent, paintAccent)

        val accentBottomTop = h * 0.65f
        val bottomAccent = Path().apply {
            moveTo(leftX, floorTop)
            lineTo(rightX, floorTop)
            lineTo(accentTopRight, accentBottomTop)
            lineTo(accentLeft, accentBottomTop)
            close()
        }
        canvas.drawPath(bottomAccent, paintAccent)

        val labelLeft = 0.2f * w
        val labelRight = 0.8f * w
        canvas.drawRect(RectF(labelLeft, 0.36f * h, labelRight, 0.44f * h), paintLabel)
        canvas.drawText(labelText, (w - paintText.measureText(labelText)) / 2.0f, 0.415f * h, paintText)
        canvas.drawRect(RectF(labelLeft, 0.48f * h, labelRight, 0.62f * h), paintLabel)

        val reelRadius = h * 0.1f
        val innerRadius = reelRadius * 0.3f
        val outerRadius = reelRadius * 0.45f
        val reelY = h * 0.55f
        val leftReelX = 0.35f * w
        val rightReelX = w * 0.65f

        canvas.drawCircle(leftReelX, reelY, reelRadius, paintReel)
        drawInnerGear(canvas, leftReelX, reelY, innerRadius, outerRadius, 8, paintHole)
        canvas.drawCircle(rightReelX, reelY, reelRadius, paintReel)
        drawInnerGear(canvas, rightReelX, reelY, innerRadius, outerRadius, 8, paintHole)

        val screwRadius = w * 0.015f
        val screwTopY = 0.34f * h
        val screwBottomY = h * 0.66f
        canvas.drawCircle(accentLeft, screwTopY, screwRadius, paintScrew)
        canvas.drawCircle(accentTopRight, screwTopY, screwRadius, paintScrew)
        canvas.drawCircle(accentLeft, screwBottomY, screwRadius, paintScrew)
        canvas.drawCircle(accentTopRight, screwBottomY, screwRadius, paintScrew)
    }

    private fun drawInnerGear(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        innerR: Float,
        outerR: Float,
        teeth: Int,
        paint: Paint
    ) {
        val path = Path()
        val totalPoints = teeth * 2
        val angleStep = 2.0 * Math.PI / totalPoints

        for (i in 0 until totalPoints) {
            val angle = i * angleStep
            val r = if (i % 2 == 0) innerR else outerR
            val x = cx + (Math.cos(angle) * r).toFloat()
            val y = cy + (r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        path.close()
        canvas.drawPath(path, paint)
    }
}
