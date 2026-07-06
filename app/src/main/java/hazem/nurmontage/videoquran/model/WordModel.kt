package hazem.nurmontage.videoquran.model

class WordModel(
    var w: String,
    var isSelected: Boolean = false
) {
    constructor(w: String) : this(w, false)
}
