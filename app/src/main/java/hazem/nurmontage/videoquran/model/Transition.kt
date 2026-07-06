package hazem.nurmontage.videoquran.model

import java.io.Serializable

class Transition : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

    //  Transition types
    var type_in: String = "none"
    var type_out: String = "none"
    var type_both: String = "none"

    //  Durations (seconds)
    var duration_in: Float = 1.5f
    var duration_out: Float = 1.5f
    var duration_both: Float = 0.1f

    //  Active flags
    var isIn: Boolean = false
    var isOut: Boolean = false
    var isBoth: Boolean = false

    //  Frame offsets (used by FFmpeg transition rendering)
    var offset_frame_in: Float = 0f
    var offset_frame_out: Float = 0f

    //  Width reference (used by some transition types)
    var fromW: Float = 0f

    //  Constructors

    constructor()

    constructor(
        type_in: String,
        type_out: String,
        type_both: String,
        duration_in: Float,
        duration_out: Float,
        duration_both: Float,
        isIn: Boolean,
        isOut: Boolean,
        isBoth: Boolean
    ) {
        this.type_in = type_in
        this.type_out = type_out
        this.type_both = type_both
        this.duration_in = duration_in
        this.duration_out = duration_out
        this.duration_both = duration_both
        this.isIn = isIn
        this.isOut = isOut
        this.isBoth = isBoth
    }

    //  Duplicate

    fun duplicate(): Transition {
        val copy = Transition(
            type_in, type_out, type_both,
            duration_in, duration_out, duration_both,
            isIn, isOut, isBoth
        )
        copy.offset_frame_in = offset_frame_in
        copy.offset_frame_out = offset_frame_out
        copy.fromW = fromW
        return copy
    }
}
