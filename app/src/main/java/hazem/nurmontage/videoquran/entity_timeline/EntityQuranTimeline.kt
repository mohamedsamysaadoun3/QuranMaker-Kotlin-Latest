package hazem.nurmontage.videoquran.entity_timeline

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.core.common.Constants.COLOR_AYA
import hazem.nurmontage.videoquran.core.common.Constants.NUMBER_CHAR
import hazem.nurmontage.videoquran.model.Transition
import hazem.nurmontage.videoquran.model.QuranEntity

class EntityQuranTimeline(
    val quranEntity: QuranEntity,
    left: Float,
    top: Float,
    height: Float,
    right: Float,
    secondInScreen: Float
) : Entity(secondInScreen) {

    internal var h: Float = height
    internal var centerY: Float = 0f
    internal var downX: Float = 0f
    internal var lastLeft: Float = 0f
    internal var lastRight: Float = 0f
    internal var transition: Transition? = null
    internal var file: String? = null
    internal var file_in: String? = null
    internal var file_out: String? = null

    private val paintText: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBound: Rect = Rect()

    override var right: Float
        get() = super.right
        set(value) {
            super.right = value
            rect.right = value
        }

    init {
        rect = RectF(left, top, right, height)
        this.left = rect.left
        this.right = rect.right
        color = Constants.COLOR_BLOCK_QURAN
        paintText.style = Paint.Style.FILL
        paintText.textSize = rect.height() * 0.27f
        paintText.typeface = quranEntity.getPaintAya().typeface
        paintText.color = COLOR_AYA
        paintText.getTextBounds(quranEntity.getTxt() ?: "", 0, (quranEntity.getTxt() ?: "").length, textBound)
        centerY = rect.top + (rect.height() * 0.5f) + (textBound.height() * 0.5f)
        rectFLeft = RectF(0f, 0f, 0.46f * height, height)
        rectFRight = RectF(0f, 0f, rectFLeft.width(), height)
        round = rectFRight.width() * 0.5f
        padding = height * 0.07f
    }

    fun getTransition(): Transition? = transition
    fun setTransition(transition: Transition?) { this.transition = transition }

    fun getFile(): String? = file
    fun setFile(file: String?) { this.file = file }
    fun getFile_in(): String? = file_in
    fun setFile_in(fileIn: String?) { file_in = fileIn }
    fun getFile_out(): String? = file_out
    fun setFile_out(fileOut: String?) { file_out = fileOut }

    override fun updateStartTrim() {}

    override fun setDownX(downX: Float) { this.downX = downX }

    override fun getH(): Float = h

    override fun setLastLeft(lastLeft: Float) { this.lastLeft = lastLeft }

    override fun setLastRight(lastRight: Float) { this.lastRight = lastRight }

    override fun setX(x: Float) {
        val clamped = if (x < 0f) 0f else x
        rect.left = clamped
        left = clamped
    }

    override fun onUpRight() { right = lastRight }

    override fun onUpLeft() { left = lastLeft }

    override fun setY(y: Float) {
        rect.top = y
        rect.bottom = h + rect.top
        centerY = rect.top + (rect.height() * 0.5f) + (textBound.height() * 0.5f)
    }

    override fun draw(canvas: Canvas) {
        canvas.drawText(
            (quranEntity.getTxt() ?: "").replace(NUMBER_CHAR, "..."),
            round + rect.left, centerY, paintText
        )
    }

    override fun draw(canvas: Canvas, w: Int, h: Int) {
        canvas.drawText(
            (quranEntity.getTxt() ?: "").replace(NUMBER_CHAR, "..."),
            round + rect.left, centerY, paintText
        )
    }

    override fun onTouch(point: PointF): Boolean {
        selectTrim = null
        downX = point.x
        trimType = -1
        if (rectFLeft.contains(point.x, point.y)) {
            selectTrim = rectFLeft
            trimType = 0
            isSelect = true
        } else if (rectFRight.contains(point.x, point.y)) {
            selectTrim = rectFRight
            trimType = 1
            isSelect = true
        }
        return true
    }

    override fun getDownX(): Float = downX

    override fun contains(point: PointF): Boolean {
        if (isSelect) onTouch(point)
        isSelect = rect.contains(point.x, point.y)
        return isSelect
    }
}
