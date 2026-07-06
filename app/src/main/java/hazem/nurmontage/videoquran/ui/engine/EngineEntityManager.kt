package hazem.nurmontage.videoquran.ui.engine

import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.core.view.InputDeviceCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.utils.DrawableHelper
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.UtilsFileLast
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.SurahNameStyle
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.EntityBismilahTemplate
import hazem.nurmontage.videoquran.model.EntityQuranTemplate
import hazem.nurmontage.videoquran.model.EntityTranslationTemplate
import hazem.nurmontage.videoquran.model.MRectF
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.model.Transition
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.fragment.EditEntityFragment
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import hazem.nurmontage.videoquran.fragment.EditS_NameFragment
import hazem.nurmontage.videoquran.fragment.EditTrslEntityFragment
import hazem.nurmontage.videoquran.views.blurred.setSurahNameEntity
import java.lang.ref.WeakReference

// ==========================================================================
// EngineEntityManager.kt
// Entity/layer management methods for EngineActivity, extracted as extension functions.
// ==========================================================================

fun EngineActivity.addEntity(
    str: String, str2: String, str3: String, str4: String,
    i: Int, i2: Int, str5: String, i3: Int, i4: Int
) {
    val z = mTemplate!!.ipad_type == IpadType.GRADIENT.ordinal ||
            mTemplate!!.ipad_type == IpadType.MASK_BRUSH.ordinal ||
            mTemplate!!.ipad_type == IpadType.BLACK_LAYER.ordinal
    val nameFont = if (blurredImageView.getQuranEntities().isEmpty()) {
        "arabic/Quran.ttf"
    } else {
        blurredImageView.getQuranEntities()[0].nameFont
    }
    val str6 = nameFont
    val quranEntity = QuranEntity(
        this, DrawableHelper.getIDDrawableIconByName(str5),
        str, str2, str3, str4,
        blurredImageView.getRectFAya()!!,
        UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/$str6")!!,
        Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")!!,
        i, i2,
        UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/خط فارس الكوفي.otf")!!,
        blurredImageView.clr_aya,
        blurredImageView.clr_trsl,
        str6, z
    )
    quranEntity.ipad_type = mTemplate!!.ipad_type
    quranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    quranEntity.startWord_index = i3
    quranEntity.endWord_index = i4
    quranEntity.icon = str5
    quranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    val addTimeLineQuran = addTimeLineQuran(quranEntity)
    addTimeLineQuran.scaleFactor = trackViewEntity.scaleFactor
    quranEntity.entityQuran = addTimeLineQuran
    addTimeLineQuran.setEntityView(quranEntity)
    blurredImageView.addEntity(quranEntity)
}

fun EngineActivity.addTranslationEntity(str: String, i: Int, z: Boolean) {
    val translationQuranEntity = TranslationQuranEntity(
        str, blurredImageView.getRectFAya()!!,
        Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")!!,
        i, InputDeviceCompat.SOURCE_ANY, "ReadexPro_Medium.ttf",
        blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height()
    )
    translationQuranEntity.ipad_type = mTemplate!!.ipad_type
    translationQuranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    translationQuranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    val addTimeLineTrslQuran = addTimeLineTrslQuran(translationQuranEntity)
    addTimeLineTrslQuran.scaleFactor = trackViewEntity.scaleFactor
    translationQuranEntity.entityTrslTimeline = addTimeLineTrslQuran
    addTimeLineTrslQuran.setEntityView(translationQuranEntity)
    blurredImageView.addEntity(translationQuranEntity)
}

fun EngineActivity.addEntityBissmilah(
    str: String, f: Float, f2: Float, i: Int,
    transition: Transition, f3: Float, f4: Float,
    rectF: RectF?, i2: Int
) {
    val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(this, "fonts/خط البسملة.ttf")!!
    val rectF2 = if (rectF == null) {
        blurredImageView.getRectFAya()!!
    } else {
        RectF(
            rectF.left * blurredImageView.getmCanvas_width(),
            rectF.top * blurredImageView.getmCanvas_height(),
            rectF.right * blurredImageView.getmCanvas_width(),
            rectF.bottom * blurredImageView.getmCanvas_height()
        )
    }
    val bismilahEntity = BismilahEntity(str, rectF2, loadFontFromAsset!!, i, i2)
    bismilahEntity.setFcSize(f4)
    bismilahEntity.setFactor_scale(f3)
    bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    if (bismilahEntity.factorSize == 1.0f) {
        bismilahEntity.createStaticLayout()
    } else {
        bismilahEntity.setupScaleSave(bismilahEntity.factorSize, blurredImageView.getmCanvas_width())
    }
    bismilahEntity.initPreset(i2)
    bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    val addTimeLineBismilah = addTimeLineBismilah(bismilahEntity, f, f2)
    bismilahEntity.bismilahTimeline = addTimeLineBismilah
    addTimeLineBismilah.setTransition(transition)
    addTimeLineBismilah.setEntityView(bismilahEntity)
    blurredImageView.addBismilahEntity(bismilahEntity)
}

fun EngineActivity.addEntityIsti3ada(
    str: String, f: Float, f2: Float, i: Int,
    transition: Transition, f3: Float, f4: Float,
    rectF: RectF?, i2: Int
) {
    val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(this, "fonts/خط الاستعاذه.ttf")!!
    val rectF2 = if (rectF == null) {
        blurredImageView.getRectFAya()!!
    } else {
        RectF(
            rectF.left * blurredImageView.getmCanvas_width(),
            rectF.top * blurredImageView.getmCanvas_height(),
            rectF.right * blurredImageView.getmCanvas_width(),
            rectF.bottom * blurredImageView.getmCanvas_height()
        )
    }
    val bismilahEntity = BismilahEntity(str, rectF2, loadFontFromAsset!!, i, i2)
    bismilahEntity.setFcSize(f4)
    bismilahEntity.setFactor_scale(f3)
    bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    if (bismilahEntity.factorSize == 1.0f) {
        bismilahEntity.createStaticLayout()
    } else {
        bismilahEntity.setupScaleSave(bismilahEntity.factorSize, blurredImageView.getmCanvas_width())
    }
    bismilahEntity.initPreset(i2)
    bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    val addTimeLineIsti3ada = addTimeLineIsti3ada(bismilahEntity, f, f2)
    bismilahEntity.bismilahTimeline = addTimeLineIsti3ada
    addTimeLineIsti3ada.setTransition(transition)
    addTimeLineIsti3ada.setEntityView(bismilahEntity)
    blurredImageView.addIsti3adhaEntity(bismilahEntity)
}

fun EngineActivity.addEntityBissmilah(): Boolean {
    if (blurredImageView.getBismilahEntity() != null) {
        if (blurredImageView.getBismilahEntity()!!.bismilahTimeline!!.visible()) {
            return false
        }
        blurredImageView.getBismilahEntity()!!.bismilahTimeline!!.visible(true)
        return false
    }
    val bismilahEntity = BismilahEntity(
        "1", blurredImageView.getRectFAya()!!,
        UtilsFileLast.loadFontFromAsset(this, "fonts/خط البسملة.ttf")!!,
        blurredImageView.clr_aya
    )
    bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    bismilahEntity.setFcSize(bismilahEntity.getPaintAya().textSize / blurredImageView.getmCanvas_width())
    bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    val addTimeLineBismilah = addTimeLineBismilah(bismilahEntity)
    addTimeLineBismilah.scaleFactor = trackViewEntity.scaleFactor
    bismilahEntity.bismilahTimeline = addTimeLineBismilah
    addTimeLineBismilah.setEntityView(bismilahEntity)
    blurredImageView.addBismilahEntity(bismilahEntity)
    if (trackViewEntity.getQuran() != null) {
        trackViewEntity.translateToRightBismilah(addTimeLineBismilah)
    }
    return true
}

fun EngineActivity.addEntityIste3adha(): Boolean {
    if (blurredImageView.getmIsti3adhaEntity() != null) {
        if (blurredImageView.getmIsti3adhaEntity()!!.bismilahTimeline!!.visible()) {
            return false
        }
        blurredImageView.getmIsti3adhaEntity()!!.bismilahTimeline!!.visible(true)
        return false
    }
    val bismilahEntity = BismilahEntity(
        "4", blurredImageView.getRectFAya()!!,
        UtilsFileLast.loadFontFromAsset(this, "fonts/خط الاستعاذه.ttf")!!,
        blurredImageView.clr_aya
    )
    bismilahEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    bismilahEntity.setFcSize(bismilahEntity.getPaintAya().textSize / blurredImageView.getmCanvas_width())
    bismilahEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    val addTimeLineIsti3ada = addTimeLineIsti3ada(bismilahEntity)
    addTimeLineIsti3ada.scaleFactor = trackViewEntity.scaleFactor
    bismilahEntity.bismilahTimeline = addTimeLineIsti3ada
    addTimeLineIsti3ada.setEntityView(bismilahEntity)
    blurredImageView.addIsti3adhaEntity(bismilahEntity)
    if (trackViewEntity.getQuran() != null) {
        trackViewEntity.translateToRightBismilah(addTimeLineIsti3ada)
    }
    return true
}

fun EngineActivity.duplicateEntity(quranEntity: QuranEntity) {
    var typefaceNumber = quranEntity.getTypefaceNumber()
    if (typefaceNumber == null) {
        typefaceNumber = UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/خط فارس الكوفي.otf")!!
    }
    val typeface = typefaceNumber
    var typeface2 = quranEntity.getPaintAya().typeface!!
    if (typeface2 == null) {
        typeface2 = UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/${quranEntity.nameFont!!}")!!
    }
    val typeface3 = typeface2
    var typeface4: Typeface? = quranEntity.getPaintTranslationAya()?.typeface
    if (typeface4 == null) {
        typeface4 = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")!!
    }
    val quranEntity2 = QuranEntity(
        quranEntity.txt!!, quranEntity.getComplete_aya()!!,
        quranEntity.translation!!, quranEntity.translation_complete!!,
        blurredImageView.getRectFAya()!!, typeface3, typeface4,
        quranEntity.getIndexNumber(), quranEntity.getNumber(), typeface,
        quranEntity.clrAya, quranEntity.clrTrsl, quranEntity.nameFont!!,
        quranEntity.getPaintAya().textSize,
        quranEntity.getPaintTranslationAya()?.textSize ?: 0f,
        quranEntity.getPaintAya().isUnderlineText,
        quranEntity.getVectorDrawable()!!
    )
    quranEntity2.setFcSize(quranEntity.factorSize)
    quranEntity2.factorSizeTrl = quranEntity.factorSizeTrl
    quranEntity2.setFactor_scale(quranEntity.getFactor_scale())
    quranEntity2.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    quranEntity2.ipad_type = mTemplate!!.ipad_type
    quranEntity2.startWord_index = quranEntity.startWord_index
    quranEntity2.endWord_index = quranEntity.endWord_index
    quranEntity2.icon = quranEntity.icon
    quranEntity2.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    quranEntity2.isVisible = false
    quranEntity2.setupScaleSave(quranEntity2.factorSize, blurredImageView.getmCanvas_width())
    quranEntity2.setColor(quranEntity.clrAya)
    quranEntity2.setColorTranslation(
        if (quranEntity.getPaintTranslationAya() != null) quranEntity.clrTrsl else InputDeviceCompat.SOURCE_ANY
    )
    quranEntity2.initPreset(quranEntity.getmPreset())
    val addTimeLineQuran = addTimeLineQuran(
        quranEntity.entityQuran!!.index + 1, quranEntity2,
        quranEntity.entityQuran!!.rect.right,
        quranEntity.entityQuran!!.rect.right + quranEntity.entityQuran!!.rect.width()
    )
    addTimeLineQuran.scaleFactor = quranEntity.entityQuran!!.scaleFactor
    quranEntity2.entityQuran = addTimeLineQuran
    addTimeLineQuran.setEntityView(quranEntity2)
    if (quranEntity.entityQuran!!.getTransition() != null) {
        addTimeLineQuran.setTransition(quranEntity.entityQuran!!.getTransition()!!.duplicate())
    }
    blurredImageView.addEntity(quranEntity2, quranEntity.index + 1)
    trackViewEntity.selectEntity(quranEntity2.entityQuran, false)
    iTrimLineCallback!!.onSelectEntity(quranEntity2.entityQuran!!, -1.0f)
    trackViewEntity.updateCursurToSelectEntity()
}

fun EngineActivity.duplicateEntity(translationQuranEntity: TranslationQuranEntity) {
    var typeface = translationQuranEntity.getPaintAya().typeface
    if (typeface == null) {
        typeface = UtilsFileLast.loadFontFromAsset(this, "fonts/${translationQuranEntity.nameFont}")
    }
    val translationQuranEntity2 = TranslationQuranEntity(
        translationQuranEntity.txt!!, translationQuranEntity.rect,
        typeface, translationQuranEntity.getNumber(),
        translationQuranEntity.clrAya, translationQuranEntity.nameFont!!,
        translationQuranEntity.getPaintAya().textSize
    )
    translationQuranEntity2.setFcSize(translationQuranEntity.factorSize)
    translationQuranEntity2.factorSizeTrl = translationQuranEntity.factorSizeTrl
    translationQuranEntity2.setFactor_scale(translationQuranEntity.getFactor_scale())
    translationQuranEntity2.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    translationQuranEntity2.ipad_type = mTemplate!!.ipad_type
    translationQuranEntity2.isVisible = false
    translationQuranEntity2.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    translationQuranEntity2.updatePaint(
        translationQuranEntity.getPaintAya().textSize,
        translationQuranEntity.getStaticLayout()!!.width
    )
    translationQuranEntity2.setColor(translationQuranEntity.clrAya)
    translationQuranEntity2.initPreset(translationQuranEntity.getmPreset())
    val addTimeLineTrsl = addTimeLineTrslQuran(
        translationQuranEntity.entityTrslTimeline!!.index + 1,
        translationQuranEntity2,
        translationQuranEntity.entityTrslTimeline!!.rect.right,
        translationQuranEntity.entityTrslTimeline!!.rect.right + translationQuranEntity.entityTrslTimeline!!.rect.width()
    )
    val transition = translationQuranEntity.entityTrslTimeline!!.getTransition()
    if (transition != null) {
        addTimeLineTrsl.setTransition(transition.duplicate())
        if (transition.isIn && transition.isOut) {
            addTimeLineTrsl.getTransition()!!.isIn = false
            transition.isOut = false
        } else if (transition.isIn) {
            addTimeLineTrsl.getTransition()!!.isIn = false
        } else if (transition.isOut) {
            transition.isOut = false
        }
    }
    addTimeLineTrsl.scaleFactor = translationQuranEntity.entityTrslTimeline!!.scaleFactor
    translationQuranEntity2.entityTrslTimeline = addTimeLineTrsl
    addTimeLineTrsl.setEntityView(translationQuranEntity2)
    if (translationQuranEntity.entityTrslTimeline!!.getTransition() != null) {
        addTimeLineTrsl.setTransition(translationQuranEntity.entityTrslTimeline!!.getTransition()!!.duplicate())
    }
    blurredImageView.addEntity(translationQuranEntity2, translationQuranEntity.index + 1)
    trackViewEntity.selectEntity(translationQuranEntity2.entityTrslTimeline, false)
    iTrimLineCallback!!.onSelectEntity(translationQuranEntity2.entityTrslTimeline!!, -1.0f)
    trackViewEntity.updateCursurToSelectEntity()
}

fun EngineActivity.splitEntity(translationQuranEntity: TranslationQuranEntity) {
    val abs = Math.abs(trackViewEntity.getXCursur())
    if (abs <= translationQuranEntity.entityTrslTimeline!!.rect.left ||
        abs >= translationQuranEntity.entityTrslTimeline!!.rect.right
    ) {
        return
    }
    val secondInScreen = trackViewEntity.getSecond_in_screen() * 0.2f
    if (abs <= translationQuranEntity.entityTrslTimeline!!.rect.left ||
        abs >= translationQuranEntity.entityTrslTimeline!!.rect.left + secondInScreen
    ) {
        if (abs >= translationQuranEntity.entityTrslTimeline!!.rect.right ||
            abs <= translationQuranEntity.entityTrslTimeline!!.rect.right - secondInScreen
        ) {
            var typeface = translationQuranEntity.getPaintAya().typeface
            if (typeface == null) {
                typeface = UtilsFileLast.loadFontFromAsset(this, "fonts/${translationQuranEntity.nameFont}")
            }
            val translationQuranEntity2 = TranslationQuranEntity(
                translationQuranEntity.txt!!, translationQuranEntity.rect,
                typeface, translationQuranEntity.getNumber(),
                translationQuranEntity.clrAya, translationQuranEntity.nameFont!!,
                translationQuranEntity.getPaintAya().textSize
            )
            translationQuranEntity2.setFcSize(translationQuranEntity.factorSize)
            translationQuranEntity2.factorSizeTrl = translationQuranEntity.factorSizeTrl
            translationQuranEntity2.setFactor_scale(translationQuranEntity.getFactor_scale())
            translationQuranEntity2.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
            translationQuranEntity2.ipad_type = mTemplate!!.ipad_type
            translationQuranEntity2.setViewWeakReference(
                WeakReference(trackViewEntity), WeakReference(blurredImageView)
            )
            translationQuranEntity2.updatePaint(
                translationQuranEntity.getPaintAya().textSize,
                translationQuranEntity.getStaticLayout()!!.width
            )
            translationQuranEntity2.setColor(translationQuranEntity.clrAya)
            translationQuranEntity2.initPreset(translationQuranEntity.getmPreset())
            trackViewEntity.stackSplit(translationQuranEntity.entityTrslTimeline!!)
            val splitTimeLineQuran = splitTimeLineQuran(
                translationQuranEntity.entityTrslTimeline!!.index + 1,
                translationQuranEntity2,
                Math.abs(trackViewEntity.getCurrentPosition()),
                translationQuranEntity.entityTrslTimeline!!.rect.right,
                translationQuranEntity.entityTrslTimeline!!.scaleFactor
            )
            val transition = translationQuranEntity.entityTrslTimeline!!.getTransition()
            if (transition != null) {
                splitTimeLineQuran.setTransition(transition.duplicate())
                if (transition.isIn && transition.isOut) {
                    splitTimeLineQuran.getTransition()!!.isIn = false
                    transition.isOut = false
                } else if (transition.isIn) {
                    splitTimeLineQuran.getTransition()!!.isIn = false
                } else if (transition.isOut) {
                    transition.isOut = false
                }
            }
            translationQuranEntity.entityTrslTimeline!!.setCurrentRect()
            translationQuranEntity.entityTrslTimeline!!.right = Math.abs(trackViewEntity.getCurrentPosition())
            translationQuranEntity.entityTrslTimeline!!.onChange()
            translationQuranEntity2.entityTrslTimeline = splitTimeLineQuran
            splitTimeLineQuran.setEntityView(translationQuranEntity2)
            if (translationQuranEntity.entityTrslTimeline!!.getTransition() != null) {
                splitTimeLineQuran.setTransition(
                    translationQuranEntity.entityTrslTimeline!!.getTransition()!!.duplicate()
                )
            }
            blurredImageView.addEntity(translationQuranEntity2, translationQuranEntity.index + 1)
            trackViewEntity.invalidate()
        }
    }
}

fun EngineActivity.splitEntity(quranEntity: QuranEntity) {
    val abs = Math.abs(trackViewEntity.getXCursur())
    if (abs <= quranEntity.entityQuran!!.rect.left ||
        abs >= quranEntity.entityQuran!!.rect.right
    ) {
        return
    }
    val secondInScreen = trackViewEntity.getSecond_in_screen() * 0.2f
    if (abs <= quranEntity.entityQuran!!.rect.left ||
        abs >= quranEntity.entityQuran!!.rect.left + secondInScreen
    ) {
        if (abs >= quranEntity.entityQuran!!.rect.right ||
            abs <= quranEntity.entityQuran!!.rect.right - secondInScreen
        ) {
            var typefaceNumber = quranEntity.getTypefaceNumber()
            if (typefaceNumber == null) {
                typefaceNumber = UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/خط فارس الكوفي.otf")!!
            }
            val typeface = typefaceNumber
            var typeface2 = quranEntity.getPaintAya().typeface!!
            if (typeface2 == null) {
                typeface2 = UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/${quranEntity.nameFont!!}")!!
            }
            val typeface3 = typeface2
            var typeface4: Typeface? = quranEntity.getPaintTranslationAya()?.typeface
            if (typeface4 == null) {
                typeface4 = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")!!
            }
            val quranEntity2 = QuranEntity(
                quranEntity.txt!!, quranEntity.getComplete_aya()!!,
                quranEntity.translation!!, quranEntity.translation_complete!!,
                blurredImageView.getRectFAya()!!, typeface3, typeface4,
                quranEntity.getIndexNumber(), quranEntity.getNumber(), typeface,
                quranEntity.clrAya, quranEntity.clrTrsl, quranEntity.nameFont!!,
                quranEntity.getPaintAya().textSize,
                quranEntity.getPaintTranslationAya()?.textSize ?: 0f,
                quranEntity.getPaintAya().isUnderlineText,
                quranEntity.getVectorDrawable()!!
            )
            quranEntity2.setFcSize(quranEntity.factorSize)
            quranEntity2.factorSizeTrl = quranEntity.factorSizeTrl
            quranEntity2.setFactor_scale(quranEntity.getFactor_scale())
            quranEntity2.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
            quranEntity2.ipad_type = mTemplate!!.ipad_type
            quranEntity2.startWord_index = quranEntity.startWord_index
            quranEntity2.endWord_index = quranEntity.endWord_index
            quranEntity2.icon = quranEntity.icon
            quranEntity2.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
            quranEntity2.setupScaleSave(quranEntity2.factorSize, blurredImageView.getmCanvas_width())
            quranEntity2.setColor(quranEntity.clrAya)
            quranEntity2.setColorTranslation(
                if (quranEntity.getPaintTranslationAya() != null) quranEntity.clrTrsl else InputDeviceCompat.SOURCE_ANY
            )
            quranEntity2.initPreset(quranEntity.getmPreset())
            trackViewEntity.stackSplit(quranEntity.entityQuran!!)
            val splitTimeLineQuran = splitTimeLineQuran(
                quranEntity.entityQuran!!.index + 1, quranEntity2,
                Math.abs(trackViewEntity.getCurrentPosition()),
                quranEntity.entityQuran!!.rect.right,
                quranEntity.entityQuran!!.scaleFactor
            )
            val transition = quranEntity.entityQuran!!.getTransition()
            if (transition != null) {
                splitTimeLineQuran.setTransition(transition.duplicate())
                if (transition.isIn && transition.isOut) {
                    splitTimeLineQuran.getTransition()!!.isIn = false
                    transition.isOut = false
                } else if (transition.isIn) {
                    splitTimeLineQuran.getTransition()!!.isIn = false
                } else if (transition.isOut) {
                    transition.isOut = false
                }
            }
            quranEntity.entityQuran!!.setCurrentRect()
            quranEntity.entityQuran!!.right = Math.abs(trackViewEntity.getCurrentPosition())
            quranEntity.entityQuran!!.onChange()
            quranEntity2.entityQuran = splitTimeLineQuran
            splitTimeLineQuran.setEntityView(quranEntity2)
            if (quranEntity.entityQuran!!.getTransition() != null) {
                splitTimeLineQuran.setTransition(quranEntity.entityQuran!!.getTransition()!!.duplicate())
            }
            blurredImageView.addEntity(quranEntity2, quranEntity.index + 1)
            trackViewEntity.invalidate()
        }
    }
}

fun EngineActivity.addEntity(
    str: String, str2: String, str3: String, str4: String,
    f: Float, f2: Float, i: Int, i2: Int, i3: Int,
    str5: String, transition: Transition, z: Boolean,
    str6: String?, i4: Int, i5: Int, f3: Float, f4: Float,
    f5: Float, rectF: RectF?, typeface: Typeface,
    typeface2: Typeface, i6: Int, i7: Int
) {
    val str7 = str6 ?: "hafes"
    val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/$str5")
    val rectF2 = if (rectF == null) {
        blurredImageView.getRectFAya()!!
    } else {
        RectF(
            rectF.left * blurredImageView.getmCanvas_width(),
            rectF.top * blurredImageView.getmCanvas_height(),
            rectF.right * blurredImageView.getmCanvas_width(),
            rectF.bottom * blurredImageView.getmCanvas_height()
        )
    }
    val quranEntity = QuranEntity(
        this, str, str2, str3, str4, rectF2, loadFontFromAsset!!,
        typeface2, i, i2, typeface, i3, i6, str5, z,
        DrawableHelper.getIDDrawableIconByName(str7)
    )
    quranEntity.setFcSize(f4)
    quranEntity.factorSizeTrl = f5
    quranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    quranEntity.setFactor_scale(f3)
    quranEntity.ipad_type = mTemplate!!.ipad_type
    quranEntity.startWord_index = i4
    quranEntity.endWord_index = i5
    quranEntity.icon = str7
    quranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    if (quranEntity.factorSize == 1.0f) {
        quranEntity.setTextSize(quranEntity.calculateTextSize())
    } else {
        quranEntity.setupScaleSave(quranEntity.factorSize, blurredImageView.getmCanvas_width())
    }
    quranEntity.initPreset(i7)
    val addTimeLineQuran = addTimeLineQuran(quranEntity, f, f2)
    quranEntity.entityQuran = addTimeLineQuran
    addTimeLineQuran.setTransition(transition)
    addTimeLineQuran.setEntityView(quranEntity)
    blurredImageView.addEntity(quranEntity)
}

fun EngineActivity.addEntityTrsl(
    str: String, f: Float, f2: Float, i: Int, i2: Int,
    str2: String, transition: Transition, f3: Float, f4: Float,
    rectF: RectF?, i3: Int, i4: Int, z: Boolean
) {
    val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(this, "fonts/$str2")
    val rectF2 = if (rectF == null) {
        blurredImageView.getRectFAya()!!
    } else {
        RectF(
            rectF.left * blurredImageView.getmCanvas_width(),
            rectF.top * blurredImageView.getmCanvas_height(),
            rectF.right * blurredImageView.getmCanvas_width(),
            rectF.bottom * blurredImageView.getmCanvas_height()
        )
    }
    val translationQuranEntity = TranslationQuranEntity(
        blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height(),
        str, rectF2, loadFontFromAsset!!, i, i2, str2
    )
    translationQuranEntity.setHaveBg(z)
    translationQuranEntity.clrBg = i4
    translationQuranEntity.setFcSize(f4)
    translationQuranEntity.setCanvasWH(blurredImageView.getmCanvas_width(), blurredImageView.getmCanvas_height())
    translationQuranEntity.setFactor_scale(f3)
    translationQuranEntity.ipad_type = mTemplate!!.ipad_type
    translationQuranEntity.setViewWeakReference(WeakReference(trackViewEntity), WeakReference(blurredImageView))
    if (translationQuranEntity.factorSize == 1.0f) {
        translationQuranEntity.setTextSize(translationQuranEntity.calculateTextSize())
    } else {
        translationQuranEntity.setupScaleSave(translationQuranEntity.factorSize, blurredImageView.getmCanvas_width())
    }
    translationQuranEntity.initPreset(i3)
    val addTimeLineQuran = addTimeLineQuran(translationQuranEntity, f, f2)
    translationQuranEntity.entityTrslTimeline = addTimeLineQuran
    addTimeLineQuran.setTransition(transition)
    addTimeLineQuran.setEntityView(translationQuranEntity)
    blurredImageView.addEntity(translationQuranEntity)
}

// Timeline entity creation helpers
fun EngineActivity.addTimeLineTrslQuran(translationQuranEntity: TranslationQuranEntity): EntityTrslTimeline {
    var xCursur = trackViewEntity.getXCursur()
    val trslQuran = trackViewEntity.getTrslQuran()
    if (trslQuran != null) {
        xCursur = trslQuran.rect.right
    }
    val entityTrslTimeline = EntityTrslTimeline(
        translationQuranEntity, xCursur, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        trackViewEntity.getQuran()!!.rect.right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.addTrslQuran(entityTrslTimeline)
    return entityTrslTimeline
}

fun EngineActivity.addTimeLineBismilah(bismilahEntity: BismilahEntity): EntityBismilahTimeline {
    val f = trackViewEntity.getmIsi3adaTimeline()?.rect?.right ?: 0.0f
    val entityBismilahTimeline = EntityBismilahTimeline(
        bismilahEntity, f, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        f + trackViewEntity.getSecond_in_screen() * 4.0f,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.bismilahTimeline = entityBismilahTimeline
    return entityBismilahTimeline
}

fun EngineActivity.addTimeLineIsti3ada(bismilahEntity: BismilahEntity): EntityBismilahTimeline {
    val entityBismilahTimeline = EntityBismilahTimeline(
        bismilahEntity, 0.0f, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        trackViewEntity.getSecond_in_screen() * 4.0f + 0.0f,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.setmIsi3adaTimeline(entityBismilahTimeline)
    return entityBismilahTimeline
}

fun EngineActivity.addTimeLineBismilah(bismilahEntity: BismilahEntity, index: Int, quranEntity: QuranEntity, left: Float, right: Float): EntityBismilahTimeline {
    val entityBismilahTimeline = EntityBismilahTimeline(
        bismilahEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.bismilahTimeline = entityBismilahTimeline
    return entityBismilahTimeline
}

fun EngineActivity.addTimeLineIsti3ada(bismilahEntity: BismilahEntity, index: Int, quranEntity: QuranEntity, left: Float, right: Float): EntityBismilahTimeline {
    val entityBismilahTimeline = EntityBismilahTimeline(
        bismilahEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.setmIsi3adaTimeline(entityBismilahTimeline)
    return entityBismilahTimeline
}

fun EngineActivity.addTimeLineQuran(quranEntity: QuranEntity): EntityQuranTimeline {
    var xCursur = trackViewEntity.getXCursur()
    val quran = trackViewEntity.getQuran()
    if (quran != null) {
        xCursur = quran.rect.right
    }
    val entityQuranTimeline = EntityQuranTimeline(
        quranEntity, xCursur, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        trackViewEntity.getQuran()!!.rect.right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.addQuran(entityQuranTimeline)
    return entityQuranTimeline
}

fun EngineActivity.addTimeLineQuran(index: Int, quranEntity: QuranEntity, left: Float, right: Float): EntityQuranTimeline {
    val entityQuranTimeline = EntityQuranTimeline(
        quranEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.addQuran_split(entityQuranTimeline, index)
    return entityQuranTimeline
}

fun EngineActivity.addTimeLineTrslQuran(index: Int, translationQuranEntity: TranslationQuranEntity, left: Float, right: Float): EntityTrslTimeline {
    val entityTrslTimeline = EntityTrslTimeline(
        translationQuranEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.addTrslQuran(entityTrslTimeline, index)
    return entityTrslTimeline
}

fun EngineActivity.splitTimeLineQuran(index: Int, quranEntity: QuranEntity, left: Float, right: Float, scaleFactor: Float): EntityQuranTimeline {
    val entityQuranTimeline = EntityQuranTimeline(
        quranEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    entityQuranTimeline!!.scaleFactor = scaleFactor
    trackViewEntity.addQuran_split(entityQuranTimeline, index)
    return entityQuranTimeline
}

fun EngineActivity.splitTimeLineQuran(index: Int, translationQuranEntity: TranslationQuranEntity, left: Float, right: Float, scaleFactor: Float): EntityTrslTimeline {
    val entityTrslTimeline = EntityTrslTimeline(
        translationQuranEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    entityTrslTimeline!!.scaleFactor = scaleFactor
    trackViewEntity.addTrslQuran(entityTrslTimeline, index)
    return entityTrslTimeline
}

fun EngineActivity.addTimeLineBismilah(bismilahEntity: BismilahEntity, left: Float, right: Float): EntityBismilahTimeline {
    val entityBismilahTimeline = EntityBismilahTimeline(
        bismilahEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.bismilahTimeline = entityBismilahTimeline
    return entityBismilahTimeline
}

fun EngineActivity.addTimeLineIsti3ada(bismilahEntity: BismilahEntity, left: Float, right: Float): EntityBismilahTimeline {
    val entityBismilahTimeline = EntityBismilahTimeline(
        bismilahEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.setmIsi3adaTimeline(entityBismilahTimeline)
    return entityBismilahTimeline
}

fun EngineActivity.addTimeLineQuran(quranEntity: QuranEntity, left: Float, right: Float): EntityQuranTimeline {
    val entityQuranTimeline = EntityQuranTimeline(
        quranEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.addQuran(entityQuranTimeline)
    return entityQuranTimeline
}

fun EngineActivity.addTimeLineQuran(translationQuranEntity: TranslationQuranEntity, left: Float, right: Float): EntityTrslTimeline {
    val entityTrslTimeline = EntityTrslTimeline(
        translationQuranEntity, left, 0.0f,
        trackViewEntity.getWidth() * 0.077f,
        right,
        trackViewEntity.getSecond_in_screen()
    )
    trackViewEntity.addTrslQuran(entityTrslTimeline)
    return entityTrslTimeline
}

fun EngineActivity.addEntityFromTemplate() {
    val template = mTemplate ?: return
    val isEnabled = template.ipad_type == IpadType.GRADIENT.ordinal ||
            template.ipad_type == IpadType.MASK_BRUSH.ordinal ||
            template.ipad_type == IpadType.BLACK_LAYER.ordinal
    val loadFontFromAsset = UtilsFileLast.loadFontFromAsset(this, "fonts/arabic/خط فارس الكوفي.otf")
    val createFromAsset = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")

    for (entityQuranTemplate in template.quranEntityList) {
        addEntity(
            entityQuranTemplate.aya!!,
            entityQuranTemplate.complete_aya!!,
            entityQuranTemplate.translation,
            entityQuranTemplate.translation_complete,
            entityQuranTemplate.left,
            entityQuranTemplate.right,
            entityQuranTemplate.indexNumber,
            entityQuranTemplate.number,
            entityQuranTemplate.color,
            entityQuranTemplate.name_font ?: "hafes",
            entityQuranTemplate.transition ?: Transition(),
            isEnabled,
            entityQuranTemplate.icon,
            entityQuranTemplate.startWord_index,
            entityQuranTemplate.endWord_index,
            entityQuranTemplate.scale,
            entityQuranTemplate.factor_size,
            entityQuranTemplate.factor_sizeTrl,
            RectF(
                entityQuranTemplate.rectF!!.l,
                entityQuranTemplate.rectF!!.t,
                entityQuranTemplate.rectF!!.r,
                entityQuranTemplate.rectF!!.b
            ),
            loadFontFromAsset!!,
            createFromAsset!!,
            entityQuranTemplate.colorTrsl,
            entityQuranTemplate.preset
        )
    }

    for (translationTemplate in template.translationTemplateList) {
        addEntityTrsl(
            translationTemplate.aya!!,
            translationTemplate.left,
            translationTemplate.right,
            translationTemplate.number,
            translationTemplate.color,
            translationTemplate.name_font ?: "ReadexPro_Medium.ttf",
            translationTemplate.transition ?: Transition(),
            translationTemplate.scale,
            translationTemplate.factor_size,
            RectF(
                translationTemplate.rectF!!.l,
                translationTemplate.rectF!!.t,
                translationTemplate.rectF!!.r,
                translationTemplate.rectF!!.b
            ),
            translationTemplate.preset,
            translationTemplate.clr_bg,
            translationTemplate.isHaveBg
        )
    }

    if (template.entityIsti3adaTemplate != null) {
        addEntityIsti3ada(
            template.entityIsti3adaTemplate!!.aya!!,
            template.entityIsti3adaTemplate!!.left,
            template.entityIsti3adaTemplate!!.right,
            template.entityIsti3adaTemplate!!.color,
            template.entityIsti3adaTemplate!!.transition ?: Transition(),
            template.entityIsti3adaTemplate!!.scale,
            template.entityIsti3adaTemplate!!.factor_size,
            RectF(
                template.entityIsti3adaTemplate!!.rectF!!.l,
                template.entityIsti3adaTemplate!!.rectF!!.t,
                template.entityIsti3adaTemplate!!.rectF!!.r,
                template.entityIsti3adaTemplate!!.rectF!!.b
            ),
            template.entityIsti3adaTemplate!!.preset
        )
    }

    if (template.entityBismilahTemplate != null) {
        addEntityBissmilah(
            template.entityBismilahTemplate!!.aya!!,
            template.entityBismilahTemplate!!.left,
            template.entityBismilahTemplate!!.right,
            template.entityBismilahTemplate!!.color,
            template.entityBismilahTemplate!!.transition ?: Transition(),
            template.entityBismilahTemplate!!.scale,
            template.entityBismilahTemplate!!.factor_size,
            RectF(
                template.entityBismilahTemplate!!.rectF!!.l,
                template.entityBismilahTemplate!!.rectF!!.t,
                template.entityBismilahTemplate!!.rectF!!.r,
                template.entityBismilahTemplate!!.rectF!!.b
            ),
            template.entityBismilahTemplate!!.preset
        )
    }

    if (template.entitySurahTemplate != null) {
        val rectF: RectF = if (template.entitySurahTemplate!!.rectF == null) {
            blurredImageView.rectFSurahName ?: RectF()
        } else {
            RectF(
                template.entitySurahTemplate!!.rectF!!.l * blurredImageView.getmCanvas_width(),
                template.entitySurahTemplate!!.rectF!!.t * blurredImageView.getmCanvas_height(),
                template.entitySurahTemplate!!.rectF!!.r * blurredImageView.getmCanvas_width(),
                template.entitySurahTemplate!!.rectF!!.b * blurredImageView.getmCanvas_height()
            )
        }
        blurredImageView.setSurahNameEntity(
            template.entitySurahTemplate!!.name,
            template.entitySurahTemplate!!.reader,
            rectF,
            template.entitySurahTemplate!!.factor_scale,
            template.entitySurahTemplate!!.name_font ?: "خط الإبل.otf",
            template.entitySurahTemplate!!.clr,
            template.entitySurahTemplate!!.preset,
            template.entitySurahTemplate!!.style,
            template.entitySurahTemplate!!.index_surah,
            template.entitySurahTemplate!!.isHaveBg,
            if (template.entitySurahTemplate!!.clrBg == 0) androidx.core.view.ViewCompat.MEASURED_STATE_MASK else template.entitySurahTemplate!!.clrBg
        )
    }

    if (template.entityMediaList.isNotEmpty()) {
        try {
            val entityMedia = template.entityMediaList[0]
            if (entityMedia.video_path != null) {
                if (template.uri_upload_extract_audio_video == null) {
                    runOnUiThread {
                        hideProgressFragment()
                    }
                } else {
                    AudioUtils.copyToLocalAsync(
                        this,
                        Uri.parse(template.uri_upload_extract_audio_video).toString(),
                        template.folder_template!!,
                        object : AudioUtils.Callback {
                            override fun onSuccess(textValue: String) {
                                entityMedia.video_path = textValue
                                if (template.extension != null) {
                                    addAudioFromVideoWithExtention(template.extension!!, entityMedia.video_path!!, 0)
                                } else {
                                    start_extenstion = 0
                                    extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, 0)
                                }
                            }

                            override fun onError(exc: Exception) {
                                exc.printStackTrace()
                            }
                        }
                    )
                }
            } else if (entityMedia.uri != null) {
                if (entityMedia.paths_https != null) {
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        addAudioRecitersTemplate(entityMedia.paths_https!!, 0, "")
                    } else {
                        runOnUiThread {
                            dialogNoInternetList(entityMedia.paths_https!!)
                        }
                    }
                } else if (entityMedia.uri!!.contains("http")) {
                    val parse = Uri.parse(entityMedia.uri)
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        addAudioTemplateHttp(parse, 0, null)
                    } else {
                        runOnUiThread {
                            dialogNoInternet(parse)
                        }
                    }
                } else {
                    addAudioTemplateHttp(Uri.parse(entityMedia.uri), 0, null)
                }
            }
            return
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                hideProgressFragment()
            }
            return
        }
    }

    runOnUiThread {
        trackViewEntity.invalidate()
        updateTime()
        if (template.quranEntityList.isEmpty()) {
            blurredImageView.invalidate()
        }
        hideProgressFragment()
    }
}

// Button state helpers
fun EngineActivity.enableUndoBtn() {
    try {
        val imageButton = btnUndo
        if (imageButton == null || imageButton.isEnabled) {
            return
        }
        runOnUiThread {
            btnUndo!!.setColorFilter(-1, android.graphics.PorterDuff.Mode.SRC_IN)
            btnUndo!!.isEnabled = true
            btnUndo!!.isClickable = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.enableRedoBtn() {
    try {
        val imageButton = btnRedo
        if (imageButton == null || imageButton.isEnabled) {
            return
        }
        runOnUiThread {
            btnRedo!!.setColorFilter(-1, android.graphics.PorterDuff.Mode.SRC_IN)
            btnRedo!!.isEnabled = true
            btnRedo!!.isClickable = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.disableRedoBtn() {
    try {
        val imageButton = btnRedo
        if (imageButton == null || !imageButton.isEnabled) {
            return
        }
        runOnUiThread {
            btnRedo!!.setColorFilter(-8355712, android.graphics.PorterDuff.Mode.SRC_IN)
            btnRedo!!.isEnabled = false
            btnRedo!!.isClickable = false
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.disableUndoBtn() {
    try {
        val imageButton = btnUndo
        if (imageButton == null || !imageButton.isEnabled) {
            return
        }
        runOnUiThread {
            btnUndo!!.setColorFilter(-8355712, android.graphics.PorterDuff.Mode.SRC_IN)
            btnUndo!!.isEnabled = false
            btnUndo!!.isClickable = false
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.updateBtnCutState() {
    try {
        checkSplitEntity()
        checkSplitTrslEntity()
        checkSplitAudio()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.selectSurahName() {
    findViewById<View>(R.id.layout_menu).visibility = 4
    val surahNameEntity = blurredImageView.surahNameEntity
    val beginTransaction = supportFragmentManager.beginTransaction()
    mCurrentFragment = EditS_NameFragment.getInstance(iEditSName, mResources, surahNameEntity)
    beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
    beginTransaction.commit()
}

fun EngineActivity.clearCallback() {
    // These are val properties and cannot be reassigned to null
    // In the original Java code these were nullable interface references
}

// Removed duplicate addUpdateAnim functions - they are defined in EngineTimelineManager.kt


