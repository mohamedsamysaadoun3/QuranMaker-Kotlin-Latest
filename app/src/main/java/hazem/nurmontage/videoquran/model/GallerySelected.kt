package hazem.nurmontage.videoquran.model

class GallerySelected {

    var photoItem: PhotoItem? = null
        private set
    var videoItem: VideoItem? = null
        private set
    var index: Int = 0
        private set

    constructor(photoItem: PhotoItem, index: Int) {
        this.photoItem = photoItem
        this.index = index
    }

    constructor(videoItem: VideoItem, index: Int) {
        this.videoItem = videoItem
        this.index = index
    }
}
