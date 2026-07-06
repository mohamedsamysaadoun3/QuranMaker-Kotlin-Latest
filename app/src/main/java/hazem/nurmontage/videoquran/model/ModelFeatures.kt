package hazem.nurmontage.videoquran.model

data class ModelFeatures(
    val name: String,
    val isForFree: Boolean = false
) {
    constructor(name: String) : this(name, false)
}
