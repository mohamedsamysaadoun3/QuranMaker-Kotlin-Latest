package hazem.nurmontage.videoquran.model

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.model.EntityView

class TextEntity : EntityView {

    override var entityQuran: EntityQuranTimeline? = null
    override var isVisible: Boolean = false
    private var paintAya: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var staticLayout: StaticLayout? = null
    private var txt: String? = null
    private var viewWidth: Int = 0
    private var x: Float = 0f
    private var y: Float = 0f

    constructor(txt: String, x: Float, y: Float, entityQuran: EntityQuranTimeline) {
        this.txt = txt
        this.x = x
        this.y = y
        isVisible = true
        this.entityQuran = entityQuran
    }

    constructor(txt: String, x: Float, y: Float, viewWidth: Int) {
        this.txt = txt
        this.x = x
        this.y = y
        isVisible = true
        this.viewWidth = viewWidth
        paintAya.color = -1
        paintAya.textSize = viewWidth * 0.06f
        createStaticLayout()
    }

    override fun endAnimator() {}
    override fun postTranslate(dx: Float, dy: Float) {}
    override fun scale(factor: Float, canvasW: Int, canvasH: Int) {}

    fun setTxt(txt: String) {
        this.txt = txt
        staticLayout = StaticLayout.Builder.obtain(
            txt, 0, txt.length, paintAya, viewWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    private fun createStaticLayout() {
        val txt = this.txt ?: return
        staticLayout = StaticLayout.Builder.obtain(
            txt, 0, txt.length, paintAya, viewWidth
        ).setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
    }

    fun update(canvasH: Int, canvasW: Int) {
        y = canvasH * 0.67f
        viewWidth = canvasW
        paintAya.textSize = canvasW * 0.06f
        createStaticLayout()
    }

    fun getStaticLayout(): StaticLayout? = staticLayout

    fun getEntityQuranTimeline(): EntityQuranTimeline? = entityQuran
    fun setEntityQuranTimeline(timeline: EntityQuranTimeline?) { entityQuran = timeline }

    fun draw(canvas: Canvas) {
        val layout = staticLayout ?: return
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    fun singleDraw(canvas: Canvas) {
        staticLayout?.draw(canvas)
    }

    fun getX(): Float = x
    fun getY(): Float = y
    fun getTxt(): String? = txt
}
