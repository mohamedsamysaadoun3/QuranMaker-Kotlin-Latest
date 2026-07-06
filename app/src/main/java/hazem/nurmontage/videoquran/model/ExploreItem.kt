package hazem.nurmontage.videoquran.model

import java.io.File

data class ExploreItem(
    val folder: File,
    val path: String,
    val size: String,
    val name: String,
    val firstFilePath: String
)
