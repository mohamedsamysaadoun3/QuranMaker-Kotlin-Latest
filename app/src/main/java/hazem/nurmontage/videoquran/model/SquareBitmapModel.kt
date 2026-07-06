package hazem.nurmontage.videoquran.model

import java.io.Serializable

class SquareBitmapModel : Serializable {

    var height_square: Float = 50f
    var width_sqaure: Float = 50f
    var top_square: Float = 0f
    var lef_square: Float = 0f
    var raduis: Float = 0f
    var right: Float = 0f
    var bottom: Float = 0f
    var posX: Float = 0f
    var posY: Float = 0f

    constructor()

    constructor(
        height_square: Float,
        width_sqaure: Float,
        top_square: Float,
        lef_square: Float,
        raduis: Float,
        right: Float,
        bottom: Float
    ) {
        this.height_square = height_square
        this.width_sqaure = width_sqaure
        this.top_square = top_square
        this.lef_square = lef_square
        this.raduis = raduis
        this.right = right
        this.bottom = bottom
    }

    fun set(
        posX: Float, posY: Float,
        lef_square: Float, top_square: Float,
        right: Float, bottom: Float,
        width_sqaure: Float, height_square: Float,
        raduis: Float
    ) {
        this.posX = posX
        this.posY = posY
        this.lef_square = lef_square
        this.top_square = top_square
        this.right = right
        this.bottom = bottom
        this.width_sqaure = width_sqaure
        this.height_square = height_square
        this.raduis = raduis
    }
}
