package hazem.nurmontage.videoquran.common

import android.content.res.Resources
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.model.ItemDimension

object DataDimension {

    fun getALl(resources: Resources): List<ItemDimension> = arrayListOf(
        ItemDimension(resources.getString(R.string.tiktok), R.drawable.ic_tiktok, ResizeType.SOCIAL_STORY, 720, 1280, "t"),
        ItemDimension(resources.getString(R.string.youtube_thumbnail), R.drawable.ic_youtube, ResizeType.YOUTUBE_THUMBNAIL, 1280, 720, "y_16:9"),
        ItemDimension(resources.getString(R.string.youtube_short), R.drawable.ic_youtube_shorts_icon, ResizeType.SOCIAL_STORY, 720, 1280, "y_9:16"),
        ItemDimension(resources.getString(R.string.instagram_post), R.drawable.ic_instagram, ResizeType.SQUARE, 1080, 1080, "i_1:1"),
        ItemDimension(resources.getString(R.string.instagram_story), R.drawable.ic_instagram, ResizeType.SOCIAL_STORY, 720, 1280, "i_9:16")
    )
}
