package hazem.nurmontage.videoquran.ui.engine

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.core.view.InputDeviceCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.model.EntityBismilahTemplate
import hazem.nurmontage.videoquran.model.EntityMedia
import hazem.nurmontage.videoquran.model.EntityProgressTemplate
import hazem.nurmontage.videoquran.model.EntityQuranTemplate
import hazem.nurmontage.videoquran.model.EntitySurahTemplate
import hazem.nurmontage.videoquran.model.EntityTranslationTemplate
import hazem.nurmontage.videoquran.model.MRectF
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.ui.render.ProgressViewActivity
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.FileHelper
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.views.blurred.updateIpad
import hazem.nurmontage.videoquran.views.blurred.setupBitmapDraw

fun EngineActivity.save() {
    if (oneExport) return
    oneExport = true
    trackViewEntity.finishScroll()
    trackViewEntity.setOnProgress(true)
    blurredImageView.setNotDraw(true)
    // Billing removed — no watermark for any user
    stop()
    showProgress()
    executor.execute(Runnable {
        try {
            trackViewEntity.calculMaxTime()
            blurredImageView.invalidate()
            blurredImageView.initCanvasDimension(mTemplate!!.width, mTemplate!!.height, mTemplate!!.geTypeResize())
            val max = Math.max(mTemplate!!.width, mTemplate!!.height)

            if (mTemplate!!.ipad_type != IpadType.HEART.ordinal && mTemplate!!.ipad_type != IpadType.BATTERY.ordinal) {
                if (mTemplate!!.isVideoSquare && (mTemplate!!.ipad_type == IpadType.GRADIENT.ordinal || mTemplate!!.ipad_type == IpadType.BLACK_LAYER.ordinal || mTemplate!!.ipad_type == IpadType.MASK_BRUSH.ordinal || mTemplate!!.ipad_type == IpadType.BLUE_TYPE.ordinal || mTemplate!!.ipad_type == IpadType.CASSET_IMG.ordinal)) {
                    blurredImageView.bitmapOriginal = Bitmap.createBitmap(max, max, Bitmap.Config.ARGB_8888)
                    val cropTo16x92 = when (mTemplate!!.geTypeResize()) {
                        ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(blurredImageView.bitmapOriginal)
                        ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(blurredImageView.bitmapOriginal)
                        else -> BitmapCropper.cropTo16x9(blurredImageView.bitmapOriginal)
                    }
                    blurredImageView.updatePosCanvas(mTemplate!!.width, mTemplate!!.height, cropTo16x92)
                    blurredImageView.bitmapBlured = cropTo16x92
                    blurredImageView.updateIpad(cropTo16x92!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())
                    val width = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                    val height = (cropTo16x92!!.height * 0.5355f).toInt()
                    mTemplate!!.setDrawingTranslation(blurredImageView.btmX, blurredImageView.btmY)
                    var round = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                    var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                    var i4 = width + round
                    if (i4 > blurredImageView.bitmapOriginal!!.width) {
                        round -= i4 - blurredImageView.bitmapOriginal!!.width
                        i4 = blurredImageView.bitmapOriginal!!.width
                    }
                    var i5 = height + round2
                    if (i5 > blurredImageView.bitmapOriginal!!.height) {
                        round2 -= i5 - blurredImageView.bitmapOriginal!!.height
                        i5 = blurredImageView.bitmapOriginal!!.height
                    }
                    if (round < 0) round = 0
                    if (round2 < 0) round2 = 0
                    val rect2 = Rect(round, round2, i4, i5)
                    val width2 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                    val height2 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                    val cropToSquare = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect2, width2, height2)
                    blurredImageView.bitmapSquare = cropToSquare
                    blurredImageView.radius_square = 0
                    rect2.right = rect2.left + width2
                    rect2.bottom = rect2.top + height2
                    blurredImageView.rectSquare = rect2
                    val tmpl1 = mTemplate!!; tmpl1.uri_bg_ffmpeg = blurredImageView.setupBitmapDraw(cropTo16x92, cropToSquare, tmpl1)
                    mTemplate!!.squareBitmapModel!!.set(blurredImageView.left_square, blurredImageView.top_square, round.toFloat(), round2.toFloat(), rect2.width().toFloat(), rect2.height().toFloat(), cropToSquare.width.toFloat(), cropToSquare.height.toFloat(), 0f)
                } else {
                    blurredImageView.bitmapOriginal = setupOriginalBitmap(
                        Glide.with(this as FragmentActivity).asBitmap()
                            .load(if (mTemplate!!.isVideoSquare) mTemplate!!.frame_bg else mTemplate!!.uri_bg)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .override(max, max)
                            .submit().get() as Bitmap,
                        max
                    )
                    val cropTo16x9 = when (mTemplate!!.geTypeResize()) {
                        ResizeType.SOCIAL_STORY.ordinal -> BitmapCropper.cropTo9x16(blurredImageView.bitmapOriginal, mTemplate!!.width, mTemplate!!.height)
                        ResizeType.SQUARE.ordinal -> BitmapCropper.cropTo1x1(blurredImageView.bitmapOriginal, mTemplate!!.width, mTemplate!!.height)
                        else -> BitmapCropper.cropTo16x9(blurredImageView.bitmapOriginal, mTemplate!!.width, mTemplate!!.height)
                    }
                    val bitmap = cropTo16x9
                    blurredImageView.updatePosCanvas(mTemplate!!.width, mTemplate!!.height, bitmap)
                    blurredImageView.bitmapBlured = bitmap
                    blurredImageView.updateIpad(bitmap!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())

                    var rect: Rect
                    var cropToSquareWithRoundCorners: Bitmap? = null
                    var i2: Int
                    var i3: Int

                    if (mTemplate!!.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                        val radius = (blurredImageView.ipad_rect!!.width() * 0.6f).toInt()
                        mTemplate!!.setDrawingTranslation(blurredImageView.btmX, blurredImageView.btmY)
                        i2 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                        i3 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                        var i6 = radius + i2
                        if (i6 > blurredImageView.bitmapOriginal!!.width) {
                            i2 -= i6 - blurredImageView.bitmapOriginal!!.width
                            i6 = blurredImageView.bitmapOriginal!!.width
                        }
                        var i7 = radius + i3
                        if (i7 > blurredImageView.bitmapOriginal!!.height) {
                            i3 -= i7 - blurredImageView.bitmapOriginal!!.height
                            i7 = blurredImageView.bitmapOriginal!!.height
                        }
                        if (i2 < 0) i2 = 0
                        if (i3 < 0) i3 = 0
                        rect = Rect(i2, i3, i6, i7)
                        val width3 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                        val height3 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                        cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect, radius, width3, height3)
                        rect.right = rect.left + width3
                        rect.bottom = rect.top + height3
                        blurredImageView.rectSquare = rect
                    } else {
                        if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal && mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal && mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal) {
                            val width4 = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                            val height4 = (bitmap!!.height * 0.5355f).toInt()
                            mTemplate!!.setDrawingTranslation(blurredImageView.btmX, blurredImageView.btmY)
                            var round3 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                            var round4 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                            var i8 = width4 + round3
                            if (i8 > blurredImageView.bitmapOriginal!!.width) {
                                round3 -= i8 - blurredImageView.bitmapOriginal!!.width
                                i8 = blurredImageView.bitmapOriginal!!.width
                            }
                            var i9 = height4 + round4
                            if (i9 > blurredImageView.bitmapOriginal!!.height) {
                                round4 -= i9 - blurredImageView.bitmapOriginal!!.height
                                i9 = blurredImageView.bitmapOriginal!!.height
                            }
                            if (round3 < 0) round3 = 0
                            if (round4 < 0) round4 = 0
                            rect = Rect(round3, round4, i8, i9)
                            val width5 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                            val height5 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                            cropToSquareWithRoundCorners = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect, width5, height5)
                            blurredImageView.bitmapSquare = cropToSquareWithRoundCorners
                            blurredImageView.radius_square = 0
                            rect.right = rect.left + width5
                            rect.bottom = rect.top + height5
                            blurredImageView.rectSquare = rect
                            i2 = round3
                            i3 = round4
                        } else {
                            i2 = 0
                            i3 = 0
                            rect = Rect() // placeholder, will be reassigned
                        }

                        if (mTemplate!!.ipad_type == IpadType.IPAD.ordinal || mTemplate!!.ipad_type == IpadType.IPAD_UNBLUR.ordinal || mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                            val width6 = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                            val i10 = (width6 * 1.13f).toInt()
                            val min = Math.min(width6, i10)
                            mTemplate!!.setDrawingTranslation(blurredImageView.btmX, blurredImageView.btmY)
                            var round5 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                            var round6 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                            var i11 = width6 + round5
                            if (i11 > blurredImageView.bitmapOriginal!!.width) {
                                round5 -= i11 - blurredImageView.bitmapOriginal!!.width
                                i11 = blurredImageView.bitmapOriginal!!.width
                            }
                            var i12 = i10 + round6
                            if (i12 > blurredImageView.bitmapOriginal!!.height) {
                                round6 -= i12 - blurredImageView.bitmapOriginal!!.height
                                i12 = blurredImageView.bitmapOriginal!!.height
                            }
                            if (round5 < 0) round5 = 0
                            if (round6 < 0) round6 = 0
                            rect = Rect(round5, round6, i11, i12)
                            var radiusVal = 0
                            if (mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                                val width7 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                                val height6 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                                val cropToSquare2 = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect, width7, height6)
                                blurredImageView.bitmapSquare = cropToSquare2
                                blurredImageView.radius_square = 0
                                rect.right = rect.left + width7
                                rect.bottom = rect.top + height6
                                blurredImageView.rectSquare = rect
                                cropToSquareWithRoundCorners = cropToSquare2
                                radiusVal = 0
                            } else {
                                radiusVal = (min * 0.10800001f).toInt()
                                val width8 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                                val height7 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                                cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect, radiusVal, width8, height7)
                                rect.right = rect.left + width8
                                rect.bottom = rect.top + height7
                                blurredImageView.rectSquare = rect
                            }
                            i2 = round5
                            i3 = round6
                        }

                        val rect3 = rect
                        val bitmap2 = cropToSquareWithRoundCorners
                        val engineActivity = this
                        val tmpl2 = mTemplate!!; tmpl2.uri_bg_ffmpeg = blurredImageView.setupBitmapDraw(UtilsBitmap.blurInSave(this, bitmap!!, 20, 1, mTemplate!!.width, tmpl2.height)!!, bitmap2!!, tmpl2)
                        mTemplate!!.squareBitmapModel!!.set(blurredImageView.left_square, blurredImageView.top_square, i2.toFloat(), i3.toFloat(), rect3.width().toFloat(), rect3.height().toFloat(), bitmap2.width.toFloat(), bitmap2.height.toFloat(), 0f)
                    }

                    // For non-NEOMORPHIC/non-IPAD/non-UNBLUR/non-CLASSIC types inside the else block
                    // (e.g. BOTTOM_RECT, ROUND_RECT, RECT, BORDER, CASSET)
                    // These types don't go through the NEOMORPHIC/IPAD paths above,
                    // so they need their uri_bg_ffmpeg set here
                    if (mTemplate!!.ipad_type != IpadType.IPAD_NEOMORPHIC.ordinal &&
                        mTemplate!!.ipad_type != IpadType.IPAD.ordinal &&
                        mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal &&
                        mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal &&
                        mTemplate!!.ipad_type != IpadType.HEART.ordinal &&
                        mTemplate!!.ipad_type != IpadType.BATTERY.ordinal
                    ) {
                        // BOTTOM_RECT, ROUND_RECT, RECT, BORDER, CASSET: use the blurred bitmap
                        mTemplate!!.uri_bg_ffmpeg = blurredImageView.setupBitmapDraw(UtilsBitmap.blurInSave(this, bitmap!!, 20, 1, mTemplate!!.width, mTemplate!!.height)!!, cropToSquareWithRoundCorners!!, mTemplate!!)
                    }
                }
                saveTemplate()
                val intent = Intent(this, ProgressViewActivity::class.java)
                intent.putExtra(Constants.TEMPLATE, mTemplate!!.idTemplate)
                intent.addFlags(65536)
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            } else {
                // HEART/BATTERY type handling — separate path with solid black background
                val createBitmap = Bitmap.createBitmap(mTemplate!!.width, mTemplate!!.height, Bitmap.Config.RGB_565)
                createBitmap.eraseColor(ViewCompat.MEASURED_STATE_MASK)
                blurredImageView.updatePosCanvas(mTemplate!!.width, mTemplate!!.height, createBitmap)
                blurredImageView.bitmapBlured = createBitmap
                blurredImageView.updateIpad(createBitmap, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())
                mTemplate!!.uri_bg_ffmpeg = blurredImageView.setupBitmapDraw(createBitmap, Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), mTemplate!!)
                saveTemplate()
                val intent2 = Intent(this, ProgressViewActivity::class.java)
                intent2.putExtra(Constants.TEMPLATE, mTemplate!!.idTemplate)
                intent2.addFlags(65536)
                startActivity(intent2)
                overridePendingTransition(0, 0)
                finish()
            }
        } catch (e: Exception) {
            Log.e("Tag : ", "init ${e.message}")
        }
    })
}

internal fun EngineActivity.saveTemplateTmp() {
    var str: String
    try {
        if (mTemplate == null) {
            mTemplate = Template()
        }
        mTemplate!!.setNewCode()
        mTemplate!!.isGlass = blurredImageView.isGlass
        mTemplate!!.currentCursur = trackViewEntity.current_cursur_position
        mTemplate!!.scale_timeline = trackViewEntity.scaleFactor
        mTemplate!!.gradient = blurredImageView.color_gradient
        mTemplate!!.duration = trackViewEntity.maxTime
        mTemplate!!.color_ipad = blurredImageView.colorIpad()
        mTemplate!!.quranEntityList.clear()
        mTemplate!!.translationTemplateList.clear()
        mTemplate!!.uri_bg = uri_bg

        try {
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline!!.visible()) {
                    val f2 = Utils.f2(Math.abs(entityQuranTimeline!!.rect.left / trackViewEntity.second_in_screen))
                    val f22 = Utils.f2(Math.abs(entityQuranTimeline!!.rect.right / trackViewEntity.second_in_screen))
                    if (entityQuranTimeline!!.quranEntity.copyRect == null) {
                        entityQuranTimeline!!.quranEntity.setCopyRect()
                        if (entityQuranTimeline!!.quranEntity.copyRect == null) {
                            // skip
                        }
                    }
                    val entityQuranTemplate = EntityQuranTemplate(
                        transition = entityQuranTimeline!!.transition,
                        start = f2,
                        end = f22,
                        btm_x = entityQuranTimeline!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                        btm_y = mTemplate!!.height * entityQuranTimeline!!.quranEntity.copyRect!!.top,
                        left = entityQuranTimeline!!.rect.left / entityQuranTimeline!!.scaleFactor,
                        right = entityQuranTimeline!!.rect.right / entityQuranTimeline!!.scaleFactor,
                        aya = entityQuranTimeline!!.quranEntity.txt!!,
                        complete_aya = entityQuranTimeline!!.quranEntity.complete_aya!!,
                        indexNumber = entityQuranTimeline!!.quranEntity.number,
                        number = entityQuranTimeline!!.quranEntity.number,
                        color = entityQuranTimeline!!.quranEntity.clrAya,
                        name_font = entityQuranTimeline!!.quranEntity.nameFont,
                        colorTrsl = if (entityQuranTimeline!!.quranEntity.paintTranslationAya != null) entityQuranTimeline!!.quranEntity.clrTrsl else InputDeviceCompat.SOURCE_ANY,
                        preset = entityQuranTimeline!!.quranEntity.mPreset
                    )
                    entityQuranTemplate.height = (entityQuranTimeline!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (entityQuranTimeline!!.quranEntity.copyRect!!.top * mTemplate!!.height)
                    entityQuranTemplate.factor_size = entityQuranTimeline!!.quranEntity.factorSize
                    entityQuranTemplate.factor_sizeTrl = entityQuranTimeline!!.quranEntity.factorSizeTrl
                    entityQuranTemplate.scale = entityQuranTimeline!!.quranEntity.scaleFactor
                    entityQuranTemplate.translation = entityQuranTimeline!!.quranEntity.translation!!
                    entityQuranTemplate.translation_complete = entityQuranTimeline!!.quranEntity.translation_complete!!
                    entityQuranTemplate.startWord_index = entityQuranTimeline!!.quranEntity.startWord_index
                    entityQuranTemplate.endWord_index = entityQuranTimeline!!.quranEntity.endWord_index
                    entityQuranTemplate.icon = entityQuranTimeline!!.quranEntity.icon!!
                    entityQuranTemplate.file = entityQuranTimeline!!.file
                    entityQuranTemplate.file_in = entityQuranTimeline!!.file_in
                    entityQuranTemplate.file_out = entityQuranTimeline!!.file_out
                    entityQuranTemplate.rectF = MRectF(entityQuranTimeline!!.quranEntity.copyRect!!.left, entityQuranTimeline!!.quranEntity.copyRect!!.top, entityQuranTimeline!!.quranEntity.copyRect!!.right, entityQuranTimeline!!.quranEntity.copyRect!!.bottom)
                    mTemplate!!.addQuranEntityList(entityQuranTemplate)
                }
            }
        } catch (e: Exception) {
            Log.e("save templete quran", "" + e.message)
        }

        try {
            for (entityTrslTimeline in trackViewEntity.entityListTrslQuran) {
                if (entityTrslTimeline!!.visible()) {
                    val f23 = Utils.f2(Math.abs(entityTrslTimeline!!.rect.left / trackViewEntity.second_in_screen))
                    val f24 = Utils.f2(Math.abs(entityTrslTimeline!!.rect.right / trackViewEntity.second_in_screen))
                    if (entityTrslTimeline!!.quranEntity.copyRect == null) {
                        entityTrslTimeline!!.quranEntity.setCopyRect()
                        if (entityTrslTimeline!!.quranEntity.copyRect == null) {
                            // skip
                        }
                    }
                    val entityTranslationTemplate = EntityTranslationTemplate(
                        entityTrslTimeline!!.transition, f23, f24,
                        entityTrslTimeline!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                        mTemplate!!.height * entityTrslTimeline!!.quranEntity.copyRect!!.top,
                        entityTrslTimeline!!.rect.left / entityTrslTimeline!!.scaleFactor,
                        entityTrslTimeline!!.rect.right / entityTrslTimeline!!.scaleFactor,
                        entityTrslTimeline!!.quranEntity.txt!!,
                        entityTrslTimeline!!.quranEntity.nameFont,
                        entityTrslTimeline!!.quranEntity.number,
                        entityTrslTimeline!!.quranEntity.clrAya,
                        entityTrslTimeline!!.quranEntity.mPreset
                    )
                    entityTranslationTemplate.height = (entityTrslTimeline!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (entityTrslTimeline!!.quranEntity.copyRect!!.top * mTemplate!!.height)
                    entityTranslationTemplate.factor_size = entityTrslTimeline!!.quranEntity.factorSize
                    entityTranslationTemplate.factor_sizeTrl = entityTrslTimeline!!.quranEntity.factorSizeTrl
                    entityTranslationTemplate.scale = entityTrslTimeline!!.quranEntity.scaleFactor
                    entityTranslationTemplate.file = entityTrslTimeline!!.file
                    entityTranslationTemplate.file_in = entityTrslTimeline!!.file_in
                    entityTranslationTemplate.file_out = entityTrslTimeline!!.file_out
                    entityTranslationTemplate.clr_bg = entityTrslTimeline!!.quranEntity.clrBg
                    entityTranslationTemplate.isHaveBg = entityTrslTimeline!!.quranEntity.isHaveBg
                    entityTranslationTemplate.rectF = MRectF(entityTrslTimeline!!.quranEntity.copyRect!!.left, entityTrslTimeline!!.quranEntity.copyRect!!.top, entityTrslTimeline!!.quranEntity.copyRect!!.right, entityTrslTimeline!!.quranEntity.copyRect!!.bottom)
                    mTemplate!!.addTrslEntityList(entityTranslationTemplate)
                }
            }
        } catch (e2: Exception) {
            Log.e("save templete trsl quran", "" + e2.message)
        }

        mTemplate!!.entityIsti3adaTemplate = null
        if (blurredImageView.mIsti3adhaEntity != null && blurredImageView.mIsti3adhaEntity!!.bismilahTimeline!!.visible()) {
            val bismilahTimeline = blurredImageView.mIsti3adhaEntity!!.bismilahTimeline
            val f25 = Utils.f2(Math.abs(bismilahTimeline!!.rect.left / trackViewEntity.second_in_screen))
            val f26 = Utils.f2(Math.abs(bismilahTimeline!!.rect.right / trackViewEntity.second_in_screen))
            if (bismilahTimeline!!.quranEntity.copyRect == null) {
                bismilahTimeline!!.quranEntity.setCopyRect()
            }
            val entityBismilahTemplate = EntityBismilahTemplate(
                bismilahTimeline!!.transition, f25, f26,
                bismilahTimeline!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                mTemplate!!.height * bismilahTimeline!!.quranEntity.copyRect!!.top,
                bismilahTimeline!!.rect.left / bismilahTimeline!!.scaleFactor,
                bismilahTimeline!!.rect.right / bismilahTimeline!!.scaleFactor,
                bismilahTimeline!!.quranEntity.txt!!,
                bismilahTimeline!!.quranEntity.clrAya,
                bismilahTimeline!!.quranEntity.mPreset
            )
            entityBismilahTemplate.height = (bismilahTimeline!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (bismilahTimeline!!.quranEntity.copyRect!!.top * mTemplate!!.height)
            entityBismilahTemplate.factor_size = bismilahTimeline!!.quranEntity.factorSize
            entityBismilahTemplate.scale = bismilahTimeline!!.quranEntity.scaleFactor
            entityBismilahTemplate.file = bismilahTimeline!!.file
            entityBismilahTemplate.file_in = bismilahTimeline!!.file_in
            entityBismilahTemplate.file_out = bismilahTimeline!!.file_out
            entityBismilahTemplate.rectF = MRectF(bismilahTimeline!!.quranEntity.copyRect!!.left, bismilahTimeline!!.quranEntity.copyRect!!.top, bismilahTimeline!!.quranEntity.copyRect!!.right, bismilahTimeline!!.quranEntity.copyRect!!.bottom)
            mTemplate!!.entityIsti3adaTemplate = entityBismilahTemplate
        }

        mTemplate!!.entityBismilahTemplate = null
        if (blurredImageView.bismilahEntity != null && blurredImageView.bismilahEntity!!.bismilahTimeline!!.visible()) {
            val bismilahTimeline2 = blurredImageView.bismilahEntity!!.bismilahTimeline
            val f27 = Utils.f2(Math.abs(bismilahTimeline2!!.rect.left / trackViewEntity.second_in_screen))
            val f28 = Utils.f2(Math.abs(bismilahTimeline2!!.rect.right / trackViewEntity.second_in_screen))
            if (bismilahTimeline2!!.quranEntity.copyRect == null) {
                bismilahTimeline2!!.quranEntity.setCopyRect()
            }
            val entityBismilahTemplate2 = EntityBismilahTemplate(
                bismilahTimeline2!!.transition, f27, f28,
                bismilahTimeline2!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                mTemplate!!.height * bismilahTimeline2!!.quranEntity.copyRect!!.top,
                bismilahTimeline2!!.rect.left / bismilahTimeline2!!.scaleFactor,
                bismilahTimeline2!!.rect.right / bismilahTimeline2!!.scaleFactor,
                bismilahTimeline2!!.quranEntity.txt!!,
                bismilahTimeline2!!.quranEntity.clrAya,
                bismilahTimeline2!!.quranEntity.mPreset
            )
            entityBismilahTemplate2.height = (bismilahTimeline2!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (bismilahTimeline2!!.quranEntity.copyRect!!.top * mTemplate!!.height)
            entityBismilahTemplate2.factor_size = bismilahTimeline2!!.quranEntity.factorSize
            entityBismilahTemplate2.scale = bismilahTimeline2!!.quranEntity.scaleFactor
            entityBismilahTemplate2.file = bismilahTimeline2!!.file
            entityBismilahTemplate2.file_in = bismilahTimeline2!!.file_in
            entityBismilahTemplate2.file_out = bismilahTimeline2!!.file_out
            entityBismilahTemplate2.rectF = MRectF(bismilahTimeline2!!.quranEntity.copyRect!!.left, bismilahTimeline2!!.quranEntity.copyRect!!.top, bismilahTimeline2!!.quranEntity.copyRect!!.right, bismilahTimeline2!!.quranEntity.copyRect!!.bottom)
            mTemplate!!.entityBismilahTemplate = entityBismilahTemplate2
        }

        str = if (blurredImageView.surahNameEntity == null) {
            ""
        } else if (mTemplate!!.entitySurahTemplate == null) {
            ""
        } else {
            ""
        }

        if (blurredImageView.surahNameEntity != null) {
            if (mTemplate!!.entitySurahTemplate == null) {
                mTemplate!!.entitySurahTemplate = EntitySurahTemplate(
                    blurredImageView.surahNameEntity!!.name,
                    blurredImageView.surahNameEntity!!.reader,
                    mTemplate!!.mDrawingTranslationX + blurredImageView.rectFSurahName!!.left,
                    mTemplate!!.mDrawingTranslationY + blurredImageView.rectFSurahName!!.top,
                    MRectF(blurredImageView.surahNameEntity!!.copyRect!!.left, blurredImageView.surahNameEntity!!.copyRect!!.top, blurredImageView.surahNameEntity!!.copyRect!!.right, blurredImageView.surahNameEntity!!.copyRect!!.bottom),
                    blurredImageView.surahNameEntity!!.scaleFactor,
                    blurredImageView.surahNameEntity!!.nameFont,
                    blurredImageView.surahNameEntity!!.clrS_name,
                    blurredImageView.surahNameEntity!!.mPreset,
                    blurredImageView.surahNameEntity!!.style,
                    blurredImageView.surahNameEntity!!.index_surah,
                    blurredImageView.surahNameEntity!!.isHaveBg,
                    blurredImageView.surahNameEntity!!.clrBg
                )
            } else {
                mTemplate!!.entitySurahTemplate!!.clrBg = blurredImageView.surahNameEntity!!.clrBg
                mTemplate!!.entitySurahTemplate!!.isHaveBg = blurredImageView.surahNameEntity!!.isHaveBg
                mTemplate!!.entitySurahTemplate!!.index_surah = blurredImageView.surahNameEntity!!.index_surah
                mTemplate!!.entitySurahTemplate!!.style = blurredImageView.surahNameEntity!!.style
                mTemplate!!.entitySurahTemplate!!.clr = blurredImageView.surahNameEntity!!.clrS_name
                mTemplate!!.entitySurahTemplate!!.preset = blurredImageView.surahNameEntity!!.mPreset
                mTemplate!!.entitySurahTemplate!!.name_font = blurredImageView.surahNameEntity!!.nameFont
                mTemplate!!.entitySurahTemplate!!.factor_scale = blurredImageView.surahNameEntity!!.scaleFactor
                mTemplate!!.entitySurahTemplate!!.rectF = MRectF(blurredImageView.surahNameEntity!!.copyRect!!.left, blurredImageView.surahNameEntity!!.copyRect!!.top, blurredImageView.surahNameEntity!!.copyRect!!.right, blurredImageView.surahNameEntity!!.copyRect!!.bottom)
                mTemplate!!.entitySurahTemplate!!.name = blurredImageView.surahNameEntity!!.name
                mTemplate!!.entitySurahTemplate!!.reader = blurredImageView.surahNameEntity!!.reader
                mTemplate!!.entitySurahTemplate!!.setPos(blurredImageView.rectFSurahName!!.left + mTemplate!!.mDrawingTranslationX, blurredImageView.rectFSurahName!!.top + mTemplate!!.mDrawingTranslationY)
            }
        }

        if (mTemplate!!.entityProgressTemplate == null) {
            mTemplate!!.entityProgressTemplate = EntityProgressTemplate(Utils.f2(blurredImageView.rectFProgress!!.left + mTemplate!!.mDrawingTranslationX), Utils.f2(blurredImageView.rectFProgress!!.top + mTemplate!!.mDrawingTranslationY))
        } else {
            mTemplate!!.entityProgressTemplate!!.left = Utils.f2(blurredImageView.rectFProgress!!.left + mTemplate!!.mDrawingTranslationX)
            mTemplate!!.entityProgressTemplate!!.top = Utils.f2(blurredImageView.rectFProgress!!.top + mTemplate!!.mDrawingTranslationY)
        }

        mTemplate!!.entityMediaList.clear()
        for (entityAudio in trackViewEntity.entityListAudio) {
            if (entityAudio.visible() && entityAudio.end > entityAudio.start) {
                val entityMedia = EntityMedia(
                    entityAudio.uri.toString(), entityAudio.minDuration, entityAudio.start, entityAudio.end,
                    (entityAudio.rect.left / trackViewEntity.scaleFactor).toInt(), (entityAudio.rect.right / trackViewEntity.scaleFactor).toFloat(),
                    Math.round(entityAudio.end - entityAudio.start).toFloat(), entityAudio.offset, entityAudio.getOffsetRight(), entityAudio.getOffsetLeft(),
                    entityAudio.max, entityAudio.effectAudio.fade_in.toFloat(), entityAudio.effectAudio.fade_out.toFloat(),
                    (entityAudio.rect.left / trackViewEntity.second_in_screen) * 1000.0f
                )
                entityMedia.paths_https = entityAudio.pathsHttp
                entityMedia.effectAudio = entityAudio.effectAudio
                entityMedia.path_ffmpeg = entityAudio.getPathFfmpeg()
                entityMedia.path_ffmpeg_effect = entityAudio.getPathFfmpegEffect()
                entityMedia.video_path = entityAudio.videoPath
                entityMedia.isApplyEffectInPreview = entityAudio.isApplyEffectInPreview
                mTemplate!!.addMedia(entityMedia)
            }
        }
        mTemplate!!.uri_video = FileHelper(this).createPublicVideoFolder(mResources!!.getString(R.string.app_name))!!.absolutePath!! + "/" + System.currentTimeMillis() + "_NurMontage.mp4"
        LocalPersistence.writeTemplate(this, mTemplate, str, Constants.TEMPLATE_TMP)
    } catch (unused: Exception) {
    }
}

internal fun EngineActivity.saveTemplate() {
    var engineActivity = this
    try {
        if (mTemplate == null) {
            mTemplate = Template()
        }
        mTemplate!!.setNewCode()
        mTemplate!!.isGlass = blurredImageView.isGlass
        mTemplate!!.currentCursur = trackViewEntity.current_cursur_position
        mTemplate!!.scale_timeline = trackViewEntity.scaleFactor
        mTemplate!!.duration = trackViewEntity.maxTime
        mTemplate!!.gradient = blurredImageView.color_gradient
        mTemplate!!.color_ipad = blurredImageView.colorIpad()
        mTemplate!!.quranEntityList.clear()
        mTemplate!!.translationTemplateList.clear()
        mTemplate!!.uri_bg = uri_bg

        try {
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline!!.visible()) {
                    val f2 = Utils.f2(Math.abs(entityQuranTimeline!!.rect.left / trackViewEntity.second_in_screen))
                    val f22 = Utils.f2(Math.abs(entityQuranTimeline!!.rect.right / trackViewEntity.second_in_screen))
                    if (entityQuranTimeline!!.quranEntity.copyRect == null) {
                        entityQuranTimeline!!.quranEntity.setCopyRect()
                    }
                    val entityQuranTemplate = EntityQuranTemplate(
                        transition = entityQuranTimeline!!.transition,
                        start = f2,
                        end = f22,
                        btm_x = entityQuranTimeline!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                        btm_y = mTemplate!!.height * entityQuranTimeline!!.quranEntity.copyRect!!.top,
                        left = entityQuranTimeline!!.rect.left / entityQuranTimeline!!.scaleFactor,
                        right = entityQuranTimeline!!.rect.right / entityQuranTimeline!!.scaleFactor,
                        aya = entityQuranTimeline!!.quranEntity.txt!!,
                        complete_aya = entityQuranTimeline!!.quranEntity.complete_aya!!,
                        indexNumber = entityQuranTimeline!!.quranEntity.number,
                        number = entityQuranTimeline!!.quranEntity.number,
                        color = entityQuranTimeline!!.quranEntity.clrAya,
                        name_font = entityQuranTimeline!!.quranEntity.nameFont,
                        colorTrsl = if (entityQuranTimeline!!.quranEntity.paintTranslationAya != null) entityQuranTimeline!!.quranEntity.clrTrsl else InputDeviceCompat.SOURCE_ANY,
                        preset = entityQuranTimeline!!.quranEntity.mPreset
                    )
                    entityQuranTemplate.height = (entityQuranTimeline!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (entityQuranTimeline!!.quranEntity.copyRect!!.top * mTemplate!!.height)
                    entityQuranTemplate.factor_size = entityQuranTimeline!!.quranEntity.factorSize
                    entityQuranTemplate.factor_sizeTrl = entityQuranTimeline!!.quranEntity.factorSizeTrl
                    entityQuranTemplate.scale = entityQuranTimeline!!.quranEntity.scaleFactor
                    entityQuranTemplate.translation = entityQuranTimeline!!.quranEntity.translation!!
                    entityQuranTemplate.translation_complete = entityQuranTimeline!!.quranEntity.translation_complete!!
                    entityQuranTemplate.startWord_index = entityQuranTimeline!!.quranEntity.startWord_index
                    entityQuranTemplate.endWord_index = entityQuranTimeline!!.quranEntity.endWord_index
                    entityQuranTemplate.icon = entityQuranTimeline!!.quranEntity.icon!!
                    entityQuranTemplate.file = entityQuranTimeline!!.file
                    entityQuranTemplate.file_in = entityQuranTimeline!!.file_in
                    entityQuranTemplate.file_out = entityQuranTimeline!!.file_out
                    entityQuranTemplate.rectF = MRectF(entityQuranTimeline!!.quranEntity.copyRect!!.left, entityQuranTimeline!!.quranEntity.copyRect!!.top, entityQuranTimeline!!.quranEntity.copyRect!!.right, entityQuranTimeline!!.quranEntity.copyRect!!.bottom)
                    mTemplate!!.addQuranEntityList(entityQuranTemplate)
                }
            }
        } catch (e: Exception) {
            Log.e("save templete quran", "" + e.message)
        }

        try {
            for (entityTrslTimeline in trackViewEntity.entityListTrslQuran) {
                if (entityTrslTimeline!!.visible()) {
                    val f23 = Utils.f2(Math.abs(entityTrslTimeline!!.rect.left / trackViewEntity.second_in_screen))
                    val f24 = Utils.f2(Math.abs(entityTrslTimeline!!.rect.right / trackViewEntity.second_in_screen))
                    if (entityTrslTimeline!!.quranEntity.copyRect == null) {
                        entityTrslTimeline!!.quranEntity.setCopyRect()
                    }
                    val entityTranslationTemplate = EntityTranslationTemplate(
                        entityTrslTimeline!!.transition, f23, f24,
                        entityTrslTimeline!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                        mTemplate!!.height * entityTrslTimeline!!.quranEntity.copyRect!!.top,
                        entityTrslTimeline!!.rect.left / entityTrslTimeline!!.scaleFactor,
                        entityTrslTimeline!!.rect.right / entityTrslTimeline!!.scaleFactor,
                        entityTrslTimeline!!.quranEntity.txt!!, entityTrslTimeline!!.quranEntity.nameFont,
                        entityTrslTimeline!!.quranEntity.number, entityTrslTimeline!!.quranEntity.clrAya,
                        entityTrslTimeline!!.quranEntity.mPreset
                    )
                    entityTranslationTemplate.height = (entityTrslTimeline!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (entityTrslTimeline!!.quranEntity.copyRect!!.top * mTemplate!!.height)
                    entityTranslationTemplate.factor_size = entityTrslTimeline!!.quranEntity.factorSize
                    entityTranslationTemplate.factor_sizeTrl = entityTrslTimeline!!.quranEntity.factorSizeTrl
                    entityTranslationTemplate.scale = entityTrslTimeline!!.quranEntity.scaleFactor
                    entityTranslationTemplate.file = entityTrslTimeline!!.file
                    entityTranslationTemplate.file_in = entityTrslTimeline!!.file_in
                    entityTranslationTemplate.file_out = entityTrslTimeline!!.file_out
                    entityTranslationTemplate.clr_bg = entityTrslTimeline!!.quranEntity.clrBg
                    entityTranslationTemplate.isHaveBg = entityTrslTimeline!!.quranEntity.isHaveBg
                    entityTranslationTemplate.rectF = MRectF(entityTrslTimeline!!.quranEntity.copyRect!!.left, entityTrslTimeline!!.quranEntity.copyRect!!.top, entityTrslTimeline!!.quranEntity.copyRect!!.right, entityTrslTimeline!!.quranEntity.copyRect!!.bottom)
                    mTemplate!!.addTrslEntityList(entityTranslationTemplate)
                }
            }
        } catch (e2: Exception) {
            Log.e("save templete trsl quran", "" + e2.message)
        }

        mTemplate!!.entityIsti3adaTemplate = null
        if (blurredImageView.mIsti3adhaEntity != null && blurredImageView.mIsti3adhaEntity!!.bismilahTimeline!!.visible()) {
            val bismilahTimeline = blurredImageView.mIsti3adhaEntity!!.bismilahTimeline
            val f25 = Utils.f2(Math.abs(bismilahTimeline!!.rect.left / trackViewEntity.second_in_screen))
            val f26 = Utils.f2(Math.abs(bismilahTimeline!!.rect.right / trackViewEntity.second_in_screen))
            if (bismilahTimeline!!.quranEntity.copyRect == null) {
                bismilahTimeline!!.quranEntity.setCopyRect()
            }
            val entityBismilahTemplate = EntityBismilahTemplate(
                bismilahTimeline!!.transition, f25, f26,
                bismilahTimeline!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                mTemplate!!.height * bismilahTimeline!!.quranEntity.copyRect!!.top,
                bismilahTimeline!!.rect.left / bismilahTimeline!!.scaleFactor,
                bismilahTimeline!!.rect.right / bismilahTimeline!!.scaleFactor,
                bismilahTimeline!!.quranEntity.txt!!, bismilahTimeline!!.quranEntity.clrAya,
                bismilahTimeline!!.quranEntity.mPreset
            )
            entityBismilahTemplate.height = (bismilahTimeline!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (bismilahTimeline!!.quranEntity.copyRect!!.top * mTemplate!!.height)
            entityBismilahTemplate.factor_size = bismilahTimeline!!.quranEntity.factorSize
            entityBismilahTemplate.scale = bismilahTimeline!!.quranEntity.scaleFactor
            entityBismilahTemplate.file = bismilahTimeline!!.file
            entityBismilahTemplate.file_in = bismilahTimeline!!.file_in
            entityBismilahTemplate.file_out = bismilahTimeline!!.file_out
            entityBismilahTemplate.rectF = MRectF(bismilahTimeline!!.quranEntity.copyRect!!.left, bismilahTimeline!!.quranEntity.copyRect!!.top, bismilahTimeline!!.quranEntity.copyRect!!.right, bismilahTimeline!!.quranEntity.copyRect!!.bottom)
            mTemplate!!.entityIsti3adaTemplate = entityBismilahTemplate
        }

        mTemplate!!.entityBismilahTemplate = null
        if (blurredImageView.bismilahEntity != null && blurredImageView.bismilahEntity!!.bismilahTimeline!!.visible()) {
            val bismilahTimeline2 = blurredImageView.bismilahEntity!!.bismilahTimeline
            val f27 = Utils.f2(Math.abs(bismilahTimeline2!!.rect.left / trackViewEntity.second_in_screen))
            val f28 = Utils.f2(Math.abs(bismilahTimeline2!!.rect.right / trackViewEntity.second_in_screen))
            if (bismilahTimeline2!!.quranEntity.copyRect == null) {
                bismilahTimeline2!!.quranEntity.setCopyRect()
            }
            val entityBismilahTemplate2 = EntityBismilahTemplate(
                bismilahTimeline2!!.transition, f27, f28,
                bismilahTimeline2!!.quranEntity.copyRect!!.left * mTemplate!!.width,
                mTemplate!!.height * bismilahTimeline2!!.quranEntity.copyRect!!.top,
                bismilahTimeline2!!.rect.left / bismilahTimeline2!!.scaleFactor,
                bismilahTimeline2!!.rect.right / bismilahTimeline2!!.scaleFactor,
                bismilahTimeline2!!.quranEntity.txt!!, bismilahTimeline2!!.quranEntity.clrAya,
                bismilahTimeline2!!.quranEntity.mPreset
            )
            entityBismilahTemplate2.height = (bismilahTimeline2!!.quranEntity.copyRect!!.bottom * mTemplate!!.height) - (bismilahTimeline2!!.quranEntity.copyRect!!.top * mTemplate!!.height)
            entityBismilahTemplate2.factor_size = bismilahTimeline2!!.quranEntity.factorSize
            entityBismilahTemplate2.scale = bismilahTimeline2!!.quranEntity.scaleFactor
            entityBismilahTemplate2.file = bismilahTimeline2!!.file
            entityBismilahTemplate2.file_in = bismilahTimeline2!!.file_in
            entityBismilahTemplate2.file_out = bismilahTimeline2!!.file_out
            entityBismilahTemplate2.rectF = MRectF(bismilahTimeline2!!.quranEntity.copyRect!!.left, bismilahTimeline2!!.quranEntity.copyRect!!.top, bismilahTimeline2!!.quranEntity.copyRect!!.right, bismilahTimeline2!!.quranEntity.copyRect!!.bottom)
            mTemplate!!.entityBismilahTemplate = entityBismilahTemplate2
        }

        if (blurredImageView.surahNameEntity != null) {
            if (mTemplate!!.entitySurahTemplate == null) {
                try {
                    if (blurredImageView.surahNameEntity!!.copyRect == null) {
                        blurredImageView.surahNameEntity!!.setCopyRect()
                    }
                    mTemplate!!.entitySurahTemplate = EntitySurahTemplate(
                        blurredImageView.surahNameEntity!!.name,
                        blurredImageView.surahNameEntity!!.reader,
                        mTemplate!!.mDrawingTranslationX + blurredImageView.rectFSurahName!!.left,
                        mTemplate!!.mDrawingTranslationY + blurredImageView.rectFSurahName!!.top,
                        MRectF(blurredImageView.surahNameEntity!!.copyRect!!.left, blurredImageView.surahNameEntity!!.copyRect!!.top, blurredImageView.surahNameEntity!!.copyRect!!.right, blurredImageView.surahNameEntity!!.copyRect!!.bottom),
                        blurredImageView.surahNameEntity!!.scaleFactor,
                        blurredImageView.surahNameEntity!!.nameFont,
                        blurredImageView.surahNameEntity!!.clrS_name,
                        blurredImageView.surahNameEntity!!.mPreset,
                        blurredImageView.surahNameEntity!!.style,
                        blurredImageView.surahNameEntity!!.index_surah,
                        blurredImageView.surahNameEntity!!.isHaveBg,
                        blurredImageView.surahNameEntity!!.clrBg
                    )
                } catch (e3: Exception) {
                    e3.printStackTrace()
                }
            } else {
                mTemplate!!.entitySurahTemplate!!.clrBg = blurredImageView.surahNameEntity!!.clrBg
                mTemplate!!.entitySurahTemplate!!.isHaveBg = blurredImageView.surahNameEntity!!.isHaveBg
                mTemplate!!.entitySurahTemplate!!.index_surah = blurredImageView.surahNameEntity!!.index_surah
                mTemplate!!.entitySurahTemplate!!.style = blurredImageView.surahNameEntity!!.style
                mTemplate!!.entitySurahTemplate!!.clr = blurredImageView.surahNameEntity!!.clrS_name
                mTemplate!!.entitySurahTemplate!!.preset = blurredImageView.surahNameEntity!!.mPreset
                mTemplate!!.entitySurahTemplate!!.name_font = blurredImageView.surahNameEntity!!.nameFont
                mTemplate!!.entitySurahTemplate!!.factor_scale = blurredImageView.surahNameEntity!!.scaleFactor
                mTemplate!!.entitySurahTemplate!!.rectF = MRectF(blurredImageView.surahNameEntity!!.copyRect!!.left, blurredImageView.surahNameEntity!!.copyRect!!.top, blurredImageView.surahNameEntity!!.copyRect!!.right, blurredImageView.surahNameEntity!!.copyRect!!.bottom)
                mTemplate!!.entitySurahTemplate!!.name = blurredImageView.surahNameEntity!!.name
                mTemplate!!.entitySurahTemplate!!.reader = blurredImageView.surahNameEntity!!.reader
                mTemplate!!.entitySurahTemplate!!.setPos(blurredImageView.rectFSurahName!!.left + mTemplate!!.mDrawingTranslationX, blurredImageView.rectFSurahName!!.top + mTemplate!!.mDrawingTranslationY)
            }
        }

        if (mTemplate!!.entityProgressTemplate == null) {
            mTemplate!!.entityProgressTemplate = EntityProgressTemplate(Utils.f2(blurredImageView.rectFProgress!!.left + mTemplate!!.mDrawingTranslationX), Utils.f2(blurredImageView.rectFProgress!!.top + mTemplate!!.mDrawingTranslationY))
        } else {
            mTemplate!!.entityProgressTemplate!!.left = Utils.f2(blurredImageView.rectFProgress!!.left + mTemplate!!.mDrawingTranslationX)
            mTemplate!!.entityProgressTemplate!!.top = Utils.f2(blurredImageView.rectFProgress!!.top + mTemplate!!.mDrawingTranslationY)
        }

        mTemplate!!.entityMediaList.clear()
        val it2 = trackViewEntity.entityListAudio.iterator()
        while (it2.hasNext()) {
            val next = it2.next()
            if (next.visible() && next.end > next.start) {
                val entityMedia = EntityMedia(
                    next.uri.toString(), next.minDuration, next.start, next.end,
                    (next.rect.left / trackViewEntity.scaleFactor).toInt(),
                    (next.rect.right / trackViewEntity.scaleFactor).toFloat(),
                    Math.round(next.end - next.start).toFloat(), next.offset, next.getOffsetRight(), next.getOffsetLeft(),
                    next.max, next.effectAudio.fade_in.toFloat(), next.effectAudio.fade_out.toFloat(),
                    (next.rect.left / trackViewEntity.second_in_screen) * 1000.0f
                )
                entityMedia.paths_https = next.pathsHttp
                entityMedia.effectAudio = next.effectAudio
                entityMedia.path_ffmpeg = next.getPathFfmpeg()
                entityMedia.video_path = next.videoPath
                entityMedia.path_ffmpeg_effect = next.getPathFfmpegEffect()
                entityMedia.isApplyEffectInPreview = next.isApplyEffectInPreview
                mTemplate!!.addMedia(entityMedia)
                next.release()
            }
        }
        val idStr = "Template_" + System.currentTimeMillis()
        val idTemplate = mTemplate!!.idTemplate
        mTemplate!!.idTemplate = idStr
        mTemplate!!.uri_video = FileHelper(engineActivity).createPublicVideoFolder(engineActivity.mResources!!.getString(R.string.app_name))!!.absolutePath!! + "/" + System.currentTimeMillis() + "_NurMontage.mp4"
        val template = mTemplate
        LocalPersistence.writeTemplate(engineActivity, template!!, idTemplate!!, template.idTemplate!!)
        LocalPersistence.deleteTemplate(engineActivity, Constants.TEMPLATE_TMP)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
