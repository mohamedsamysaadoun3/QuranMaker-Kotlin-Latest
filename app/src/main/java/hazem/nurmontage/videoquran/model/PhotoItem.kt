package hazem.nurmontage.videoquran.model

class PhotoItem(
    var folder: String,
    val path: String,
    var isSelect: Boolean
) {
    var adabter_pos: Int = 0
    var number: Int = 0
    var gallerySelected: GallerySelected? = null
}
