package hazem.nurmontage.videoquran.utils

import hazem.nurmontage.videoquran.R
import java.util.Random

object DrawableHelper {

    private val drawableIconMap: Map<String, Int> = mapOf(
        "hafes" to R.drawable.hafes_icon,
        "warach" to R.drawable.warach_icon,
        "shamerli" to R.drawable.shamerli_icon,
        "nour_hode" to R.drawable.nour_hoda_icon,
        "amiri" to R.drawable.amiri_icon,
        "taha" to R.drawable.taha_icon
    )

    private val drawableMap: Map<String, Int> = mapOf(
        "bg_1" to R.drawable.bg_1, "bg_2" to R.drawable.bg_2,
        "bg_3" to R.drawable.bg_3, "bg_4" to R.drawable.bg_4,
        "bg_5" to R.drawable.bg_5, "bg_6" to R.drawable.bg_6,
        "bg_7" to R.drawable.bg_7, "bg_8" to R.drawable.bg_8,
        "bg_9" to R.drawable.bg_9, "bg_10" to R.drawable.bg_10,
        "bg_11" to R.drawable.bg_11, "bg_12" to R.drawable.bg_12,
        "bg_13" to R.drawable.bg_13, "bg_14" to R.drawable.bg_14,
        "bg_15" to R.drawable.bg_15, "bg_16" to R.drawable.bg_16,
        "bg_17" to R.drawable.bg_17, "bg_18" to R.drawable.bg_18,
        "bg_19" to R.drawable.bg_19, "bg_20" to R.drawable.bg_20,
        "bg_21" to R.drawable.bg_21, "bg_22" to R.drawable.bg_22,
        "bg_23" to R.drawable.bg_23, "bg_24" to R.drawable.bg_24,
        "bg_25" to R.drawable.bg_25, "bg_26" to R.drawable.bg_26,
        "bg_27" to R.drawable.bg_27, "bg_28" to R.drawable.bg_28,
        "bg_29" to R.drawable.bg_29, "bg_30" to R.drawable.bg_30,
        "bg_31" to R.drawable.bg_31, "bg_32" to R.drawable.bg_32,
        "bg_33" to R.drawable.bg_33, "bg_34" to R.drawable.bg_34,
        "bg_35" to R.drawable.bg_35, "bg_36" to R.drawable.bg_36,
        "bg_37" to R.drawable.bg_37, "bg_38" to R.drawable.bg_38
    )

    fun getIdResource(platformId: String?): Int {
        if (platformId == null || platformId.contains("init")) {
            return R.drawable.ic_instagram
        }
        return when {
            platformId.contains("t") -> R.drawable.ic_tiktok
            platformId == "y_16:9" -> R.drawable.ic_youtube
            else -> R.drawable.ic_youtube_shorts_icon
        }
    }

    fun getIDDrawableIconByName(name: String): Int {
        return drawableIconMap[name] ?: R.drawable.hafes_icon
    }

    fun getIDDrawableByName(name: String): Int {
        return drawableMap[name] ?: R.drawable.bg_24
    }

    fun getRandomDrawableEntry(): Map.Entry<String, Int> {
        val entries = drawableMap.entries.toList()
        return entries[Random().nextInt(entries.size)]
    }
}
