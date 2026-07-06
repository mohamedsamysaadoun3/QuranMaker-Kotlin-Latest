package hazem.nurmontage.videoquran.core.common

import android.graphics.Bitmap
import android.graphics.Rect
import hazem.nurmontage.videoquran.model.FreeElement
import hazem.nurmontage.videoquran.model.GallerySelected
import hazem.nurmontage.videoquran.model.Gradient

object Common {
    const val READER = "reader"
    const val SURAH = "surah"

    val COLOR_BLOCK_AUDIO = Constants.COLOR_BLOCK_AUDIO
    val COLOR_BLOCK_QURAN = Constants.COLOR_BLOCK_QURAN
    val COLOR_BLOCK_TRANSLATION = Constants.COLOR_BLOCK_TRANSLATION
    val COLOR_WAVE_INT = Constants.COLOR_WAVE_INT
    const val ENHANCE_CMD = Constants.ENHANCE_CMD
    val MUSLIM_COLORS = Constants.MUSLIM_COLORS
    val FONT_ENGLISH_APP = Constants.FONT_ENGLISH_APP

    var listSelect: List<GallerySelected>? = null
    var indexListSelect: Int = 1
    var minSquareH: Int = 0
    var minSquareW: Int = 0
    var bitmap: Bitmap? = null
    var freeElements: List<FreeElement>? = null
    var radius: Int = 0
    var rect: Rect? = null
    var pHBorder: Float = 0.065f
    var PWBorder: Float = 0.1f

    fun getListGradientColor(): List<Gradient> = listOf(
        Gradient(-711565, -6000461, -10897425),
        Gradient(-4919188, -2572422, -356473),
        Gradient(-11748097, -7244289, -2414081),
        Gradient(-4185359, -3829603, -3410353),
        Gradient(-124555, -305327, -486100),
        Gradient(-16422401, -13654017, -10556161),
        Gradient(-40350, -28271, -14907),
        Gradient(-25322, -15052, -2728),
        Gradient(-4659057, -2833534, -817794),
        Gradient(-11456910, -6132874, -808581),
        Gradient(-11756549, -7236869, -1603589),
        Gradient(-1510924, -2760212, -4798496),
        Gradient(-7735856, -5524737, -1596929),
        Gradient(-1590320, -2173472, -2821648),
        Gradient(-14174241, -7829855, -2008728),
        Gradient(-3020841, -737321, -4732954),
        Gradient(-5008712, -3432023, -1657706),
        Gradient(-10687276, -8195195, -5506257),
        Gradient(-1446295, -5516436, -10439568),
        Gradient(-12709248, -14257011, -16191843),
        Gradient(-664919, -812139, -966279),
        Gradient(-7597819, -14343650, -14408642),
        Gradient(-14408642, -7597819, -14343650),
        Gradient(-4144960, -2039584, -1),
        Gradient(-13421773, -11513776, -9934744),
        Gradient(-5197648, -2302756, -657931),
        Gradient(-9408400, -7303024, -4868683),
        Gradient(-986896, -460552, -1),
        Gradient(-16111037, -15051912, -12810056),
        Gradient(-10443815, -8208152, -5711632),
        Gradient(-15901613, -13992072, -11492697),
        Gradient(-16773077, -16768435, -16761736),
        Gradient(-8858141, -6233877, -3608587),
        Gradient(-5731463, -2836059, -660512),
        Gradient(-8889786, -6259616, -3628928),
        Gradient(-5214128, -3108752, -1527664),
        Gradient(-11179217, -9728477, -7357297),
        Gradient(-1654861, -994103, -464416),
        Gradient(-16757440, -16750244, -16746133),
        Gradient(-14791381, -13734593, -11751600),
        Gradient(-7278960, -5248081, -3212592),
        Gradient(-7365251, -5719910, -4140873),
        Gradient(-11927438, -9826899, -7114533),
        Gradient(-1644806, -986881, -460545),
        Gradient(-14417850, -12842644, -10872678),
        Gradient(-13676721, -9404272, -5192482),
        Gradient(-8943463, -6250336, -3421237),
        Gradient(-12156236, -8934691, -5383962),
        Gradient(-5206697, -2838662, -466776),
        Gradient(-537911, -336415, -3851),
        Gradient(-16777168, -16771760, -16766352),
        Gradient(-7667712, 65535, -32897),
        Gradient(-3596489, -907757, -26215),
        Gradient(-10092544, -4194304, -38294),
        Gradient(-1897636, -52347, -18223),
        Gradient(-2081743, -41892, -21075),
        Gradient(-2555828, -65434, -32589),
        Gradient(-2532608, -23296, -9543),
        Gradient(-4671488, -1331, -32),
        Gradient(-4872638, -268383, -560),
        Gradient(-29696, -19641, -8014),
        Gradient(-5005056, -989556, -1828),
        Gradient(-2528000, -274767, -5424),
        Gradient(-16744448, -13447886, -7278960),
        Gradient(-12951797, -7357297, -4530771),
        Gradient(-11179217, -5374161, -2031693),
        Gradient(-11840736, -7886485, -3941975),
        Gradient(-4994142, -2495789, -983056),
        Gradient(-16777088, -12156236, -5383962),
        Gradient(-13625036, -10666029, -5006849),
        Gradient(-11927478, -8388480, -2461482),
        Gradient(-15073254, -12844996, -9820034),
        Gradient(-10667462, -5214096, -2051920),
        Gradient(-11916246, -8700160, -3890044),
        Gradient(-10667241, -7644629, -3628944),
        Gradient(-11912399, -8692934, -5732248),
        Gradient(-7733169, -2490246, -36680),
        Gradient(-5023232, -29696, -12173),
        Gradient(-4696225, -423032, -16181),
        Gradient(-2039584, -331546, -1),
        Gradient(-5028051, -19559, -6708)
    )

    const val COLOR_TRANSLATION = -8780025
    const val TEMPLATE = "template"
}
