package hazem.nurmontage.videoquran.core.common

import android.view.View

/**
 * Centralized constants for the entire QuranMaker application.
 *
 * This object consolidates every compile-time constant that was originally
 * scattered across `Common.java` and the `constant/` enum package into one
 * single source of truth.  All values are extracted directly from the
 * original JADX decompilation of QuranMaker v6.7.1.
 */
object Constants {

    // ──────────────────────────────────────────────
    //  Font file names  (assets/fonts/)
    // ──────────────────────────────────────────────
    const val FONT_APP = "ReadexPro_Medium.ttf"
    const val FONT_APP_BOLD = "ReadexPro_Bold.ttf"
    const val FONT_APP_LIGHT = "ReadexPro-Regular.ttf"
    const val FONT_NUMBER = "NotoNaskhArabic.ttf"
    const val FONT_QURAN = "\u0639\u062B\u0645\u0627\u0646\u064A.otf"          // عثماني.otf
    const val FONT_SURAH_NAME = "\u062E\u0637 \u0627\u0644\u0625\u0628\u0644.otf" // خط الإبل.otf
    const val FONT_TRANSLATION = "ReadexPro_Medium.ttf"
    const val FONT_TRANSLATION_AR = "\u062E\u0637 \u0627\u0644\u0625\u0628\u0644.otf" // خط الإبل.otf
    const val FONT_ENGLISH_APP = "Poppins-Regular.ttf"

    // ──────────────────────────────────────────────
    //  Color constants
    // ──────────────────────────────────────────────
    const val COLOR_AYA = -16441312
    const val COLOR_BLOCK_AUDIO = -3042963
    const val COLOR_BLOCK_QURAN = -5253382
    const val COLOR_BLOCK_TRANSLATION = -67133
    const val COLOR_LIGHT_TEXT = -1                           // 0xFFFFFFFF
    const val COLOR_TRANSLATION = -8780025
    const val COLOR_WAVE: String = "#522123"
    const val COLOR_WAVE_INT = -11394781
    const val COLOR_STATUS_BAR_DEFAULT = -14540254

    // ──────────────────────────────────────────────
    //  Layout dimension ratios (normalized 0f..1f)
    // ──────────────────────────────────────────────
    const val BLOCK_CORNER = 0.5f
    const val BLOCK_HEIGHT = 0.077f
    const val CIRCLE_SIZE = 4.2f
    const val FADE_TIME = 0.2f
    const val PADDING_BETWEEN_BLOCK = 0.026f
    const val PADDING_BOTTOM = 0.015f
    const val PADDING_LAYER = 0.015f
    const val SIZE_IF_ONE = 0.95f
    const val SIZE_IF_ONE_8_CHAR = 0.7f
    const val SIZE_TEXT_TIME = 0.0388f
    const val SIZE_TEXT_TIME_BORDER = 0.027f
    const val SIZE_TEXT_TIME_BOTTOM = 0.07f
    const val START_Y_BLOCK = 0.18f
    const val STROKE_BORDER = 0.013f
    const val AYA_H = 0.10071f
    const val LECTURE_H = 0.1109f
    const val PROGRESS_H = 0.064f
    const val SURAH_NAME_H = 0.075f
    const val SURAH_NAME_W = 0.4f
    const val SQUARE_PADDING_Y = 0.02f

    // ──────────────────────────────────────────────
    //  iPad-frame dimension ratios
    // ──────────────────────────────────────────────
    const val IPAD_NEOMORPHIC = 0.32f
    const val IPAD_H = 0.7601563f
    const val IPAD_H_BOTTOM = 0.2f
    const val IPAD_H_BOTTOM_SQUARE = 0.25f
    const val IPAD_H_LANDSCAPE = 0.7601563f
    const val IPAD_H_SQUARE = 0.7601563f
    const val IPAD_RADIUS = 0.12f
    const val IPAD_W = 0.56f
    const val IPAD_W_BOTTOM = 0.75f
    const val IPAD_W_BOTTOM_SQUARE = 0.7f
    const val IPAD_W_LANDSCAPE = 0.56f
    const val IPAD_W_SQUARE = 0.56f

    // ──────────────────────────────────────────────
    //  Square / resize dimension ratios
    // ──────────────────────────────────────────────
    const val SQUARE_H = 1.13f
    const val SQUARE_H_LANDSCAPE = 1.13f
    const val SQUARE_H_NO_RADIUS = 0.5355f
    const val SQUARE_H_SQUARE = 0.4f
    const val SQUARE_RADIUS = 0.10800001f
    const val SQUARE_W = 0.87530595f
    const val SQUARE_W_NEOMORPHIC = 0.6f
    const val SQUARE_W_LANDSCAPE = 0.87530595f
    const val SQUARE_W_NO_RADIUS = 1.0f
    const val SQUARE_W_SQUARE = 0.5623592f

    // ──────────────────────────────────────────────
    //  File / folder name constants
    // ──────────────────────────────────────────────
    const val LINE_BG = "line_bg.png"
    const val LINE_BG_TMP = "line_bg_tmp.png"
    const val LINE_PROGRESS = "line_progress.png"
    const val NUMBER_CHAR = "\u0646\u0635"  // نص
    const val READER = "reader"
    const val SURAH = "surah"
    const val SURAH_NAME_PNG = "surah_name.png"
    const val TEMPLATE = "template"
    const val TEMPLATE_TMP = "template_tmp"
    const val VIDEO_FRAME_FOLDER = "VideoFrame"

    // ──────────────────────────────────────────────
    //  FFmpeg audio-enhancement command
    // ──────────────────────────────────────────────
    const val ENHANCE_CMD =
        "equalizer=f=3000:t=h:width=200:g=2,compand=attacks=0.3:decays=0.8:points=-80/-80|-20/-10|0/-3"

    // ──────────────────────────────────────────────
    //  Muslim aya / palette color arrays
    // ──────────────────────────────────────────────
    val MUSLIM_AYA_COLORS: IntArray = intArrayOf(
        View.MEASURED_STATE_MASK, -1, -1096636, -340971, -15132186,
        -14498466, -5745162, -72990, -1096636, -2349530,
        COLOR_BLOCK_TRANSLATION, -340971, -1395960, -2037761,
        -12877066, -14856488, -2294553, -14498466, -15368131,
        -3343375, -15419226, -1185282, -5745162, -8635667
    )

    val MUSLIM_COLORS: IntArray = intArrayOf(
        -1, View.MEASURED_STATE_MASK, -478827, -626048, -4166524,
        -9675909, -13280131, -6702952, -78165, -31620, -1553825,
        -14010821, -5708082, -2298430, -11339, -21850, -29548,
        -5724249, -3386758, -1566883, -12105913, -13224394,
        -5823890, -1302455, -890056, -533681, -13658727, -1968700,
        -1186444, -404445, -224966, -45488, -113819, -221798,
        -406099, -3618647, -8147045, -2129313, -486033, -2117285,
        -11713425, -9410923
    )

    // ══════════════════════════════════════════════
    //  Enum: AyaTextPreset
    // ══════════════════════════════════════════════
    enum class AyaTextPreset {
        NONE,
        OUTLINE,
        SHADOW,
        GLOW
    }

    // ══════════════════════════════════════════════
    //  Enum: EffectAudioType
    // ══════════════════════════════════════════════
    enum class EffectAudioType {
        VOLUME,
        ECHO,
        REVERB,
        FADE,
        SPEED,
        ENHANCE,
        NOICE
    }

    // ══════════════════════════════════════════════
    //  Enum: EntityAction
    // ══════════════════════════════════════════════
    enum class EntityAction {
        SPLIT,
        TRIM,
        MOVE,
        ADD,
        ROTATE,
        TO_BACK,
        TO_FRONT,
        LAYER,
        COLOR_OUTLINE_TEXT,
        SIZE_OUTLINE_TEXT,
        COLOR_TEXT,
        SHADOW_IMAGE,
        ROUND_IMAGE,
        COLOR_OUTLINE_IMG,
        SIZE_OUTLINE_IMG,
        COLOR_TACHKIL,
        BG_TEXT,
        OPACITY_IMAGE,
        OPACITY_TEXT,
        DELETE,
        DELETE_MULTIPLE,
        SHADOW_TEXT,
        TO_HORIZONTAL_RIGHT,
        TO_HORIZONTAL_LEFT,
        TO_HORIZONTAL_CENTER,
        TO_VERTICAL_CENTER,
        TO_VERTICAL_TOP,
        TO_VERTICAL_BOTTOM,
        FONT_TEXT,
        TEXT_SIZE,
        GLOW_SHADOW,
        ICON_QURAN,
        BOLD_STYLE,
        STRIKE_LINE_STYLE,
        ITALIC_STYLE,
        UNDERLINE_STYLE,
        ALINGMENT_STYLE,
        TIME_LINE_VIEW,
        MOTION_VIEW,
        MOTION_AND_TIME_VIEW,
        ANIMATION
    }

    // ══════════════════════════════════════════════
    //  Enum: IpadType
    // ══════════════════════════════════════════════
    enum class IpadType {
        IPAD,
        IPAD_UNBLUR,
        IPAD_CLASSIC,
        ROUND_RECT,
        RECT,
        BOTTOM_RECT,
        BORDER,
        BLACK_LAYER,
        GRADIENT,
        BLUE_TYPE,
        MASK_BRUSH,
        IPAD_NEOMORPHIC,
        HEART,
        BATTERY,
        CASSET,
        CASSET_IMG,
        CASSET_IMG_BLUR
    }

    // ══════════════════════════════════════════════
    //  Enum: ResizeType
    // ══════════════════════════════════════════════
    enum class ResizeType(val value: Float) {
        IMAGE(0.0f),
        FREE(-1.0f),
        OVAL(-1.0f),
        SQUARE(1.0f),
        VERTICAL(0.5622189f),
        SOCIAL_STORY(0.5625f),
        SOCIAL_PORTRAIT(0.8f),
        PLAYSTORE_PORTRAIT(0.8f),
        PINTEREST(0.6669691f),
        HORIZONTAL(0.5625f),
        YOUTUBE_BANNER(0.5625f),
        TWITTER_POST(0.5625f),
        YOUTUBE_THUMBNAIL(0.5625f),
        COVER_PAGE_FACEBOOK(0.5625f),
        FACEBOOK_POST(0.525f),
        SOCIAL_LANDSCAPE(0.5240741f),
        LINKEDIN(0.5233333f),
        DRIBBLE(0.75f),
        PLAYSTORE_HORIZANTAL(0.8f),
        BANNER_PLAY_STORE(0.48828125f),
        COVER_FACEBOOK(0.3804878f),
        TWITTER_HEADER(0.33333334f),
        TWITCH_BANNER(0.25f),
        CUSTOM_SIZE(0.0f)
    }

    // ══════════════════════════════════════════════
    //  Enum: SurahNameStyle
    // ══════════════════════════════════════════════
    enum class SurahNameStyle {
        NONE,
        ZAGHRAFAT
    }

    // ══════════════════════════════════════════════
    //  Enum: TransitionType
    // ══════════════════════════════════════════════
    enum class TransitionType(val value: String) {
        NONE("none"),
        FADE("fade"),
        FADE_IN("fade_in"),
        FADE_OUT("fade_out"),
        FADE_WHITE("fade_white"),
        FADE_BLACK("fade_black"),
        DISTANCE("distance"),
        WIPE_RIGHT("wiperight"),
        WIPE_LEFT("wipeleft"),
        RADIAL("radial"),
        SLIDE_TOP("slidetop"),
        SLIDE_BOTTOM("slidebottom"),
        SLIDE_TO_RIGHT("slideright"),
        SLIDE_TO_LEFT("slideleft"),
        JUMP("jump"),
        SLIDE_TL("slide_tl"),
        SLIDE_BR("slide_br"),
        SLIDE_TR("slide_tr"),
        SLIDE_BL("slide_bl"),
        SLIDE_TC("slide_tc"),
        SLIDE_BC("slide_bc"),
        SLIDE_CR("slide_cr"),
        SLIDE_CL("slide_cl"),
        PIXELIZE("pixelize"),
        HBLUR("hblur"),
        HLSLICE("hlslice"),
        SPIN_LEFT("spin_left"),
        SPIN_RIGHT("spin_right"),
        ZOOM_IN("zoomin"),
        ZOOM_OUT("zoomout"),
        ROTATE_L("rotate_l"),
        ROTATE_R("rotate_r")
    }
}
