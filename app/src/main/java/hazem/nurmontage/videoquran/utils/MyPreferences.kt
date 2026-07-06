package hazem.nurmontage.videoquran.utils

import android.content.Context

object MyPreferences {

    private const val PREFS_NAME = "MyPrefs"
    private const val FIRST_RUN_KEY = "firstRun"
    private const val IS_VU_ABOUT = "is_about"
    private const val IS_VU_COPYRIGHT = "is_vu_copyright"
    private const val SCROLL_X = "scroll_view_x"
    private const val HINT_CROP_SCALE = "hint_crop_scale"
    private const val ICON_QURAN = "icon_quran"
    private const val INCLUDE_BISMILAH = "IncludeBismilah"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCopyRight(context: Context): Boolean =
        prefs(context).getBoolean(IS_VU_COPYRIGHT, false)

    fun putVuCopyRight(context: Context) {
        prefs(context).edit().putBoolean(IS_VU_COPYRIGHT, true).apply()
    }

    fun getScrollX(context: Context): Int =
        prefs(context).getInt(SCROLL_X, 0)

    fun putScrollX(context: Context, x: Int) {
        prefs(context).edit().putInt(SCROLL_X, x).apply()
    }

    fun isShowHint(context: Context): Boolean =
        prefs(context).getBoolean(HINT_CROP_SCALE, false)

    fun putShowHint(context: Context) {
        prefs(context).edit().putBoolean(HINT_CROP_SCALE, true).apply()
    }

    fun getLastIconIndex(context: Context): Int =
        prefs(context).getInt(ICON_QURAN, 0)

    fun putIndexLastIcon(context: Context, index: Int) {
        prefs(context).edit().putInt(ICON_QURAN, index).apply()
    }

    fun isIncludeBismilah(context: Context): Boolean =
        prefs(context).getBoolean(INCLUDE_BISMILAH, false)

    fun putIncludeBismilah(context: Context, include: Boolean) {
        prefs(context).edit().putBoolean(INCLUDE_BISMILAH, include).apply()
    }

    fun isVueAbout(context: Context): Boolean =
        prefs(context).getBoolean(IS_VU_ABOUT, false)

    fun putVueAbout(context: Context) {
        prefs(context).edit().putBoolean(IS_VU_ABOUT, true).apply()
    }

    fun isFirstRun(context: Context): Boolean =
        prefs(context).getBoolean(FIRST_RUN_KEY, true)

    fun putFirstRun(context: Context) {
        prefs(context).edit().putBoolean(FIRST_RUN_KEY, false).apply()
    }
}
