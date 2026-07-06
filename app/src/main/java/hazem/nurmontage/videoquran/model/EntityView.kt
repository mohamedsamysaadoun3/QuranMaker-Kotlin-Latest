package hazem.nurmontage.videoquran.model

import android.graphics.RectF
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline

abstract class EntityView {

    open var rect: RectF = RectF()
    open var posX: Float = 0f
    open var posY: Float = 0f
    open var maxW: Int = 0
    open var maxH: Int = 0

    open var factorScale: Float = 1.0f
    var factorSize: Float = 1.0f
        private set
    open var factorSizeTrl: Float = 1.0f

    private var canvasW: Int = 0
    private var canvasH: Int = 0
    var copyRect: RectF? = null

    open var entityQuran: EntityQuranTimeline? = null
    var entityTrslTimeline: EntityTrslTimeline? = null

    open var isAnimTest: Boolean = false

    abstract fun endAnimator()
    abstract var isVisible: Boolean
    abstract fun postTranslate(dx: Float, dy: Float)
    abstract fun scale(factor: Float, canvasW: Int, canvasH: Int)

    val max_w: Int get() = maxW
    val max_h: Int get() = maxH

    fun setFcSize(factorSize: Float) {
        this.factorSize = factorSize
    }

    var scaleFactor: Float
        get() = factorScale
        set(value) { factorScale = value }

    fun setFactor_scale(factorScale: Float) {
        this.factorScale = factorScale
    }

    fun getFactor_scale(): Float = factorScale

    fun setCanvasWH(w: Int, h: Int) {
        canvasW = w
        canvasH = h
    }

    fun getCanvasW(): Int = canvasW
    fun getCanvasH(): Int = canvasH

    fun setCopyRect() {
        if (rect.isEmpty && canvasW == 0 && canvasH == 0) return
        if (canvasW == 0 || canvasH == 0) return
        copyRect = RectF(
            rect.left / canvasW,
            rect.top / canvasH,
            rect.right / canvasW,
            rect.bottom / canvasH
        )
    }
}
