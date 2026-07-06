package hazem.nurmontage.videoquran.model

import java.io.Serializable

class EntityMedia : Serializable {

    var path_ffmpeg: String? = null
    var path_ffmpeg_effect: String? = null
    var video_path: String? = null
    var paths_https: List<String>? = null

    var start: Float = 0f
    var end: Float = 0f
    var offset: Float = 0f
    var offset_left: Float = 0f
    var offset_right: Float = 0f
    var max: Float = 0f

    var posX: Float = 0f
    var posY: Float = 0f
    var posXFFmpeg: Float = 0f
    var topX: Float = 0f
    var topY: Float = 0f
    var x: Float = 0f
    var y: Float = 0f
    var w: Float = 1.0f
    var h: Float = 0f

    var mScale: Float = 1.0f

    var duration_fade_in: Float = 0f
    var duration_fade_out: Float = 0f

    var volume: Float = 1.0f
    var isSoundEnable: Boolean = true

    var effectAudio: EffectAudio? = null

    var isApplyEffectInPreview: Boolean = false
        set(_) { field = false }
        get() = false

    var uri: String? = null
    var name: String? = null
    var id_raw: Int = 0
    var start_original: Int = 0
    var time: Int = 0
    var index_start_thumbnail: Int = 0
    var index_end_thumbnail: Int = 0

    constructor(uri: String) {
        this.uri = uri
    }

    constructor(
        uri: String,
        start: Float,
        end: Float,
        posX: Float,
        posY: Float,
        fadeIn: Float,
        fadeOut: Float
    ) {
        this.uri = uri
        this.start = start
        this.end = end
        this.posX = posX
        this.posY = posY
        this.duration_fade_in = fadeIn
        this.duration_fade_out = fadeOut
    }

    constructor(
        uri: String,
        startOriginal: Int,
        start: Float,
        end: Float,
        time: Int,
        posX: Float,
        posY: Float,
        offsetRight: Float,
        offsetLeft: Float,
        offset: Float,
        max: Float,
        fadeIn: Float,
        fadeOut: Float,
        posXFFmpeg: Float
    ) {
        this.uri = uri
        this.offset_left = offsetLeft
        this.offset_right = offsetRight
        this.max = max
        this.offset = offset
        this.start_original = startOriginal
        this.start = start
        this.end = end
        this.posX = posX
        this.posY = posY
        this.duration_fade_in = fadeIn
        this.duration_fade_out = fadeOut
        this.time = time
        this.posXFFmpeg = posXFFmpeg
    }

    constructor(
        uri: String,
        startOriginal: Int,
        start: Float,
        end: Float,
        time: Int,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        offset: Float,
        isSoundEnable: Boolean,
        max: Float,
        fadeIn: Float,
        fadeOut: Float,
        posXFFmpeg: Float
    ) {
        this.uri = uri
        this.start = start
        this.offset = offset
        this.duration_fade_in = fadeIn
        this.duration_fade_out = fadeOut
        this.max = max
        this.end = end
        this.posXFFmpeg = posXFFmpeg
        this.time = time
        this.start_original = startOriginal
        this.x = x
        this.h = h
        this.y = y
        this.w = w
        this.isSoundEnable = isSoundEnable
    }

    fun duplicate(): EntityMedia {
        return EntityMedia(
            uri!!,
            start_original,
            start,
            end,
            time,
            x,
            y,
            w,
            h,
            offset,
            isSoundEnable,
            max,
            duration_fade_in,
            duration_fade_out,
            posXFFmpeg
        )
    }
}
