package hazem.nurmontage.videoquran.model

import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class Template : Serializable {

    var width: Int = 0
    var height: Int = 0
    var idTemplate: String? = null
    var folder_template: String? = null
    var name_drawable: String? = null
    var extension: String? = null
    var fileInfo: Serializable? = null

    var currentCursur: Int = 0
    var duration: Int = 0
    var duration_video_media: Int = 0
    var scale_timeline: Float = 0.5f
    var isNewCode: Boolean = false

    var resolution: String = "720p"
    var fps: Int = 30
    var resizeType: Int = ResizeType.SOCIAL_STORY.ordinal
    var imgResize: String = "i_9:16"

    var uri_bg: String? = null
    var uri_bg_ffmpeg: String? = null
    var uri_media_video: String? = null
    var uri_video: String? = null
    var uri_original_upload_video: String? = null
    var uri_upload_extract_audio_video: String? = null
    var frame_bg: String? = null

    var ipad_type: Int = IpadType.IPAD.ordinal
    var color_ipad: Int = -1
    var index_color: Int = -1
    var gradient: Gradient? = null
    var isGlass: Boolean = false
    var isVideoSquare: Boolean = false

    var x_square: Float = 0.3f
    var y_square: Float = 0.2f
    var width_square: Float = 0.37218544f
    var height_square: Float = 0.41986755f
    var squareBitmapModel: SquareBitmapModel? = null
        get() = field ?: SquareBitmapModel().also { field = it }

    var mDrawingTranslationX: Float = 0f
    var mDrawingTranslationY: Float = 0f

    var mTimeModel: TimeModel? = null
    var entityProgressTemplate: EntityProgressTemplate? = null

    var entityBismilahTemplate: EntityBismilahTemplate? = null
    var entityIsti3adaTemplate: EntityBismilahTemplate? = null
    var entitySurahTemplate: EntitySurahTemplate? = null

    val entityMediaList: MutableList<EntityMedia> = arrayListOf()
    val quranEntityList: MutableList<EntityQuranTemplate> = arrayListOf()
    val translationTemplateList: MutableList<EntityTranslationTemplate> = arrayListOf()

    fun addMedia(entity: EntityMedia) { entityMediaList.add(entity) }
    fun addQuranEntityList(entity: EntityQuranTemplate) { quranEntityList.add(entity) }
    fun addTrslEntityList(entity: EntityTranslationTemplate) { translationTemplateList.add(entity) }

    fun setWidthAndHeight(w: Int, h: Int) { width = w; height = h }
    fun setDrawingTranslation(dx: Float, dy: Float) { mDrawingTranslationX = dx; mDrawingTranslationY = dy }
    fun setNewCode() { isNewCode = true }

    fun getIsVideoSquare(): Boolean = isVideoSquare
    fun geTypeResize(): Int = resizeType
    fun changeTypeIpad(type: Int) { ipad_type = type }
    fun setAnimTest(anim: Boolean) { /* no-op */ }

    fun duplicate(): Template? {
        return try {
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos ->
                    oos.writeObject(this)
                    oos.flush()
                }
                ObjectInputStream(ByteArrayInputStream(baos.toByteArray())).use { ois ->
                    ois.readObject() as Template
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
