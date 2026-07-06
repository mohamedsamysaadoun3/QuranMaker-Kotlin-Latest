package hazem.nurmontage.videoquran.model

class VideoItem(
    val folderPath: String,
    val path: String,
    val time: String,
    var isSelect: Boolean
) {
    var adabter_pos: Int = 0
    var number: Int = 0
    var gallerySelected: GallerySelected? = null
}
