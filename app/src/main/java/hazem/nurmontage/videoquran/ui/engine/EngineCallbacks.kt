package hazem.nurmontage.videoquran.ui.engine

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.ViewCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.StreamInformation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.BitmapCropper
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.utils.DrawableHelper
import hazem.nurmontage.videoquran.utils.FileUtils
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.PCMWaveformExtractor
import hazem.nurmontage.videoquran.utils.UtilsBitmap
import hazem.nurmontage.videoquran.utils.UtilsFileLast
import hazem.nurmontage.videoquran.adapter.DimensionAdabters
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.constant.EffectAudioType
import hazem.nurmontage.videoquran.constant.EntityAction
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.constant.SurahNameStyle
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline
import hazem.nurmontage.videoquran.entity_timeline.EntityTrslTimeline
import hazem.nurmontage.videoquran.fragment.*
import hazem.nurmontage.videoquran.fragment.audio_effect.*
import hazem.nurmontage.videoquran.model.BgItem
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.model.EntityMedia
import hazem.nurmontage.videoquran.model.EntityView
import hazem.nurmontage.videoquran.model.Gradient
import hazem.nurmontage.videoquran.model.MRectF
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.model.RecitersModel
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.model.Transition
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.adapter.TransitionBismilahAdabters
import hazem.nurmontage.videoquran.adapter.TransitionEntityAdabters
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.ButtonCustumFont
import hazem.nurmontage.videoquran.views.TextCustumFont
import hazem.nurmontage.videoquran.views.TrackEntityView
import hazem.nurmontage.videoquran.views.blurred.updateIpad
import hazem.nurmontage.videoquran.views.blurred.setupBitmapDraw
import hazem.nurmontage.videoquran.views.blurred.setSurahNameEntity
import hazem.nurmontage.videoquran.views.blurred.updateSizeAya
import hazem.nurmontage.videoquran.views.blurred.updateSizeAyaTrsl
import hazem.nurmontage.videoquran.views.blurred.resizeEntity
import hazem.nurmontage.videoquran.views.blurred.updatePosSurahName
import hazem.nurmontage.videoquran.utils.AspectRatioCalculator
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.core.base.BaseActivity
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale

// ==========================================================================
// EngineCallbacks.kt
// All callback interface implementations and activity result launchers
// for EngineActivity, extracted as factory extension functions.
// ==========================================================================

@Suppress("TYPE_CHECKING_HAS_RUN_INTO_RECURSIVE_PROBLEM")
fun EngineActivity.createITrimLineCallback(): TrackEntityView.ITrimLineCallback {
    return object : TrackEntityView.ITrimLineCallback {
        override fun fadeInAudio(f: Float) {}

        override fun fadeOutAudio(f: Float) {}

        override fun onMove() {}

        override fun onUpdatePlayerAudio(entityAudio: EntityAudio) {}

        override fun onSelectMultiple(i: Int) {
            showEditMultipleEntity(i)
        }

        override fun onDelete(entityView: EntityView) {
            try {
                blurredImageView.entity_select = null
                blurredImageView.postInvalidate()
                hideFragment()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onEmptySelect() {
            blurredImageView.entity_select = null
            blurredImageView.postInvalidate()
            pausePlayer()
            hideFragment()
        }

        override fun onUpdate() {
            if (try { blurredImageView; true } catch (_: UninitializedPropertyAccessException) { false }) {
                blurredImageView.postInvalidate()
            }
        }

        override fun onUp() {
            isOnScroll = false
            updateBtnCutState()
        }

        override fun onAddStack(entityAction: EntityAction) {
            enableUndoBtn()
        }

        override fun onSeekPlayer(f: Float) {
            try {
                isOnScroll = true
                for (entityAudio in trackViewEntity.entityListAudio) {
                    try {
                        if (entityAudio.mediaPlayer != null && entityAudio.mediaPlayer!!.isPlaying) {
                            entityAudio.mediaPlayer!!.pause()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (mIsPlaying) {
                    if (try { btnPlayPause; true } catch (_: UninitializedPropertyAccessException) { false }) {
                        btnPlayPause.setImageResource(R.drawable.play_btn)
                    }
                    mIsPlaying = false
                    trackViewEntity.isPlaying = false
                    blurredImageView.isPlaying = false
                }
                pauseTimelineAnimation()
                stop()
                val round = Math.round(Math.abs((f / trackViewEntity.getSecond_in_screen()) * (-1000.0f))).toInt()
                if (try { blurredImageView; true } catch (_: UninitializedPropertyAccessException) { false } && (round <= trackViewEntity.maxTime || blurredImageView.progress < 1.0f)) {
                    val min = Math.min(1.0f, round.toFloat() / trackViewEntity.maxTime)
                    updateTime(round.toLong())
                    blurredImageView.progress = min
                }
                trackViewEntity.update_current_cursur_position(round)
                current_position_time = System.currentTimeMillis().toInt()
                startCursur = trackViewEntity.current_cursur_position
                updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
                updateBtnCutState()
                updateBtnToStart()
                updateBtnToEnd()
                updateFrame()
            } catch (unused: Exception) {
            }
        }

        override fun pause() {
            pausePlayer()
        }

        override fun onPlayVibration() {
            pausePlayer()
            runOnUiThread {
                if (vibrationHelper != null) {
                    vibrationHelper!!.vibrate()
                }
            }
        }

        override fun onSelectEntity(entity: Entity, f: Float) {
            stop()
            if (entity is EntityQuranTimeline) {
                blurredImageView.entity_select = entity.getEntityView()
                blurredImageView.invalidate()
                if (EditEntityFragment.instance != null) {
                    EditEntityFragment.instance!!.checkSplitEntity(entity, -trackViewEntity.getCurrentPosition())
                    EditEntityFragment.instance!!.checkIcon(entity)
                    return
                } else if (EditTextFragment.instance != null) {
                    EditTextFragment.instance!!.update((entity as EntityQuranTimeline).quranEntity)
                    return
                } else {
                    showEditEntity(entity)
                    return
                }
            }
            if (entity is EntityTrslTimeline) {
                blurredImageView.entity_select = entity.getEntityView()
                blurredImageView.invalidate()
                if (EditTrslEntityFragment.instance != null) {
                    EditTrslEntityFragment.instance!!.checkSplitEntity(entity, -trackViewEntity.getCurrentPosition())
                    return
                } else {
                    showEditTrslEntity(entity)
                    return
                }
            }
            if (entity is EntityBismilahTimeline) {
                blurredImageView.entity_select = entity.getEntityView()
                blurredImageView.invalidate()
                showEditBismilahEntity(entity)
            } else if (entity is EntityAudio) {
                val entityAudio = entity as EntityAudio
                if (EditMediaFragment.instance != null) {
                    EditMediaFragment.instance!!.checkSplit(entityAudio, -trackViewEntity.getCurrentPosition())
                } else {
                    showEditAudioEntity(entityAudio)
                }
            }
        }

        override fun enableRedo(z: Boolean) {
            if (z) {
                enableRedoBtn()
            } else {
                disableRedoBtn()
            }
        }

        override fun enableUndo(z: Boolean) {
            if (z) {
                enableUndoBtn()
            } else {
                disableUndoBtn()
            }
        }

        override fun progress(z: Boolean) {
            runOnUiThread {
                if (z) {
                    showProgress()
                } else {
                    hideProgressFragment()
                }
            }
        }

        override fun onUpdateTime() {
            startCursur = trackViewEntity.current_cursur_position
            updateTime()
        }
    }
}

fun EngineActivity.createIAddQuran(): AddQuranFragment.IAddQuran {
    return object : AddQuranFragment.IAddQuran {
        override fun onBismilah() {
            val addEntityIste3adha = addEntityIste3adha()
            val addEntityBissmilah = addEntityBissmilah()
            if (!addEntityIste3adha || !addEntityBissmilah) {
                trackViewEntity.translateToRight(addEntityIste3adha)
            } else {
                trackViewEntity.translateToRight()
            }
        }

        override fun onVuCopyRight() {
            dialogCopyRight()
        }

        override fun progress() {
            runOnUiThread {
                showProgress()
            }
        }

        override fun onSearch() {
            isToCrop = true
            searchAyaResult.launch(Intent(this@createIAddQuran, hazem.nurmontage.videoquran.ui.search.QuranSearchActivity::class.java))
        }

        override fun uploadRecitation() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = AddAudioFragment.getInstance(iAudioCallback, mResources!!)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.audio))
            } catch (unused: Exception) {
            }
        }

        override fun onAddTranslation(str: String, i: Int, z: Boolean) {
            addTranslationEntity(str, i, z)
        }

        override fun onAdd(str: String, str2: String, str3: String?, str4: String?, i: Int, i2: Int, str5: String, i3: Int, i4: Int) {
            // BEFORE: str3!! and str4!! threw NPE when null (Java String was nullable)
            // WHY_CHANGED: splitAya() passes null for str3/str4 in many cases
            // FIXED_BY: Use null-safe defaults instead of force unwrap
            // REF: EngineActivity.java line 475 — Java passed null strings through
            // VISUAL_IMPACT: Quran Add button no longer crashes silently, spinner stops
            addEntity(str, "$str2 $i2", str3 ?: "", str4 ?: "", i, i2, str5, i3, i4)
        }

        override fun onDone(str: String, i: Int, str2: String?, uri: Uri?, str3: String?) {
            runOnUiThread {
                hideFragment()
            }
            blurredImageView.updateSizeAya()
            blurredImageView.updateSizeAyaTrsl()
            blurredImageView.setSurahNameEntity(
                str!!, str2!!, null, 1.0f, "\u062E\u0637 \u0627\u0644\u0625\u0628\u0644.otf",
                blurredImageView.clr_aya, 0,
                if (blurredImageView.surahNameEntity != null) blurredImageView.surahNameEntity!!.style else SurahNameStyle.NONE.ordinal,
                i,
                blurredImageView.surahNameEntity != null && blurredImageView.surahNameEntity!!.isHaveBg,
                if (blurredImageView.surahNameEntity != null) blurredImageView.surahNameEntity!!.clrBg else ViewCompat.MEASURED_STATE_MASK
            )
            if (str3 == null) {
                if (uri != null) addAudio(uri) else iAddQuran?.onCancel()
            } else {
                if (uri != null) addAudioFromVideo(uri, str3) else iAddQuran?.onCancel()
            }
        }

        override fun onDone(str: String, i: Int, str2: String?, list: List<RecitersModel>?) {
            runOnUiThread {
                hideFragment()
            }
            blurredImageView.updateSizeAya()
            blurredImageView.updateSizeAyaTrsl()
            blurredImageView.setSurahNameEntity(
                str!!, str2!!, null, 1.0f, "\u062E\u0637 \u0627\u0644\u0625\u0628\u0644.otf",
                blurredImageView.clr_aya, 0,
                if (blurredImageView.surahNameEntity != null) blurredImageView.surahNameEntity!!.style else SurahNameStyle.NONE.ordinal,
                i,
                blurredImageView.surahNameEntity != null && blurredImageView.surahNameEntity!!.isHaveBg,
                if (blurredImageView.surahNameEntity != null) blurredImageView.surahNameEntity!!.clrBg else ViewCompat.MEASURED_STATE_MASK
            )
            if (NetworkUtils.isNetworkAvailable(this@createIAddQuran) && list != null && list.isNotEmpty()) {
                addAudioReciters(list)
            } else {
                runOnUiThread {
                    updateTimeToEndAya()
                    updateBtnToEnd()
                    updateBtnToStart()
                    hideProgressFragment()
                }
            }
        }

        override fun onCancel() {
            hideFragment()
            hideProgressFragment()
        }

        override fun onErrorLimitation() {
            runOnUiThread {
                Toast.makeText(this@createIAddQuran, mResources!!.getString(R.string.error_limit), Toast.LENGTH_SHORT).show()
            }
        }

        override fun onAddReaderName(str: String?, str2: String?, uri: Uri?) {
            isToCrop = true
            val intent = Intent(this@createIAddQuran, hazem.nurmontage.videoquran.ui.editor.audio.AddReaderNameActivity::class.java)
            intent.putExtra("name", str)
            if (uri != null) {
                intent.putExtra("audio", uri.toString())
            }
            intent.putExtra("path_video_copy", str2)
            nameReaderResult.launch(intent)
        }
    }
}

fun EngineActivity.createIChangeBgCallback(): ChangeBgFragment.IChangeBgCallback {
    return object : ChangeBgFragment.IChangeBgCallback {
        override fun onCancel() {
            hideFragment()
            hideProgressFragment()
        }

        override fun onDone() {
            hideFragment()
        }

        override fun onCrop() {
            toCrop()
        }

        override fun onAdd(bgItem: BgItem) {
            if (bgItem.name_drawable == mTemplate!!.name_drawable) {
                return
            }
            if (ChangeBgFragment.instance != null) {
                ChangeBgFragment.instance!!.scrollToSelected()
            }
            mTemplate!!.name_drawable = bgItem.name_drawable
            uri_bg = "android.resource://" + packageName + "/drawable/" + bgItem.id
            showProgressSimple()
            executor.execute {
                var engineActivity: EngineActivity
                var runnable: Runnable
                var cropTo16x9: Bitmap? = null
                var bitmap: Bitmap
                var bitmap2: Bitmap
                var rect: Rect
                try {
                    try {
                        try {
                            mTemplate!!.uri_bg = uri_bg
                            var i = 0
                            mTemplate!!.isVideoSquare = false
                            blurredImageView.isVideo = false
                            val height = blurredImageView.getHeight()
                            blurredImageView.bitmapOriginal = Glide.with(this@createIChangeBgCallback as androidx.fragment.app.FragmentActivity)
                                    .asBitmap()
                                    .load(uri_bg)
                                    .override(height, height)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .submit()
                                    .get()
                            cropTo16x9 = if (mTemplate!!.geTypeResize() == ResizeType.SOCIAL_STORY.ordinal) {
                                BitmapCropper.cropTo9x16(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())
                            } else if (mTemplate!!.geTypeResize() == ResizeType.SQUARE.ordinal) {
                                BitmapCropper.cropTo1x1(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())
                            } else {
                                BitmapCropper.cropTo16x9(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())
                            }
                            blurredImageView.updatePosCanvas(cropTo16x9)
                            blurredImageView.updateIpad(cropTo16x9!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())
                            if (mTemplate!!.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                                val width = (blurredImageView.ipad_rect!!.width() * 0.6f).toInt()
                                var round = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                                var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                                var i2 = width + round
                                if (i2 > blurredImageView.bitmapOriginal!!.width) {
                                    round -= i2 - blurredImageView.bitmapOriginal!!.width
                                    i2 = blurredImageView.bitmapOriginal!!.width
                                }
                                var i3 = width + round2
                                if (i3 > blurredImageView.bitmapOriginal!!.height) {
                                    round2 -= i3 - blurredImageView.bitmapOriginal!!.height
                                    i3 = blurredImageView.bitmapOriginal!!.height
                                }
                                if (round < 0) {
                                    round = 0
                                }
                                if (round2 >= 0) {
                                    i = round2
                                }
                                val rect2 = Rect(round, i, i2, i3)
                                blurredImageView.setRadius_square(width)
                                val width2 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                                val height2 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                                val cropToSquareWithRoundCorners = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect2, width, width2, height2)
                                rect2.right = rect2.left + width2
                                rect2.bottom = rect2.top + height2
                                blurredImageView.rectSquare = rect2
                                bitmap2 = cropToSquareWithRoundCorners
                                rect = rect2
                            } else {
                                if (mTemplate!!.ipad_type != IpadType.IPAD.ordinal && mTemplate!!.ipad_type != IpadType.IPAD_UNBLUR.ordinal && mTemplate!!.ipad_type != IpadType.IPAD_CLASSIC.ordinal) {
                                    val width3 = (blurredImageView.ipad_rect!!.width() * 1.0f).toInt()
                                    val height3 = (cropTo16x9!!.height * 0.5355f).toInt()
                                    var round3 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                                    var round4 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                                    var i4 = width3 + round3
                                    if (i4 > blurredImageView.bitmapOriginal!!.width) {
                                        round3 -= i4 - blurredImageView.bitmapOriginal!!.width
                                        i4 = blurredImageView.bitmapOriginal!!.width
                                    }
                                    var i5 = height3 + round4
                                    if (i5 > blurredImageView.bitmapOriginal!!.height) {
                                        round4 -= i5 - blurredImageView.bitmapOriginal!!.height
                                        i5 = blurredImageView.bitmapOriginal!!.height
                                    }
                                    if (round3 < 0) {
                                        round3 = 0
                                    }
                                    if (round4 < 0) {
                                        round4 = 0
                                    }
                                    val rect3 = Rect(round3, round4, i4, i5)
                                    val width4 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                                    val height4 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                                    val cropToSquare = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect3, width4, height4)
                                    blurredImageView.bitmapSquare = cropToSquare
                                    blurredImageView.setRadius_square(0)
                                    rect3.right = rect3.left + width4
                                    rect3.bottom = rect3.top + height4
                                    blurredImageView.rectSquare = rect3
                                    bitmap2 = cropToSquare
                                    rect = rect3
                                }
                                val width5 = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                                val i6 = (width5 * 1.13f).toInt()
                                val min = Math.min(width5, i6)
                                var round5 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                                var round6 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                                var i7 = width5 + round5
                                if (i7 > blurredImageView.bitmapOriginal!!.width) {
                                    round5 -= i7 - blurredImageView.bitmapOriginal!!.width
                                    i7 = blurredImageView.bitmapOriginal!!.width
                                }
                                var i8 = i6 + round6
                                if (i8 > blurredImageView.bitmapOriginal!!.height) {
                                    round6 -= i8 - blurredImageView.bitmapOriginal!!.height
                                    i8 = blurredImageView.bitmapOriginal!!.height
                                }
                                if (round5 < 0) {
                                    round5 = 0
                                }
                                if (round6 < 0) {
                                    round6 = 0
                                }
                                val rect4 = Rect(round5, round6, i7, i8)
                                if (mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                                    val width6 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                                    val height5 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                                    val cropToSquare2 = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect4, width6, height5)
                                    blurredImageView.bitmapSquare = cropToSquare2
                                    blurredImageView.setRadius_square(0)
                                    rect4.right = rect4.left + width6
                                    rect4.bottom = rect4.top + height5
                                    blurredImageView.rectSquare = rect4
                                    bitmap = cropToSquare2
                                } else {
                                    val i9 = (min * 0.10800001f).toInt()
                                    blurredImageView.setRadius_square(i9)
                                    val width7 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                                    val height6 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                                    val cropToSquareWithRoundCorners2 = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect4, i9, width7, height6)
                                    rect4.right = rect4.left + width7
                                    rect4.bottom = rect4.top + height6
                                    blurredImageView.rectSquare = rect4
                                    bitmap = cropToSquareWithRoundCorners2
                                }
                                bitmap2 = bitmap
                                rect = rect4
                            }
                            if (mTemplate!!.ipad_type == IpadType.GRADIENT.ordinal) {
                                blurredImageView.updateBitmap(UtilsBitmap.blur(this@createIChangeBgCallback, cropTo16x9!!, 20, 1), bitmap2!!, ViewCompat.MEASURED_STATE_MASK, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                            } else if (mTemplate!!.ipad_type == IpadType.BLUE_TYPE.ordinal) {
                                if (blurredImageView.color_gradient != null) {
                                    blurredImageView.updateBitmap(UtilsBitmap.blur(this@createIChangeBgCallback, cropTo16x9!!, 20, 1), bitmap2!!, blurredImageView.color_gradient!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                                } else {
                                    blurredImageView.updateBitmap(UtilsBitmap.blur(this@createIChangeBgCallback, cropTo16x9!!, 20, 1), bitmap2!!, blurredImageView.color_ipad, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                                }
                            } else {
                                blurredImageView.updateBitmap(UtilsBitmap.blur(this@createIChangeBgCallback, cropTo16x9!!, 20, 1), bitmap2!!, -1, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                            }
                            mTemplate!!.color_ipad = blurredImageView.colorIpad()
                            runOnUiThread {
                                blurredImageView.invalidate()
                            }
                            engineActivity = this@createIChangeBgCallback
                            runnable = Runnable { hideProgressFragment() }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            engineActivity = this@createIChangeBgCallback
                            runnable = Runnable { hideProgressFragment() }
                        }
                        engineActivity.runOnUiThread(runnable)
                    } catch (unused: Exception) {
                    }
                } finally {
                }
            }
        }

        override fun onUploadVideo() {
            pickVideoFromGallery()
        }

        override fun onUploadImg() {
            pickImageFromGallery()
        }
    }
}

fun EngineActivity.createIDimensionCallback(): DimensionAdabters.IDimensionCallback {
    return object : DimensionAdabters.IDimensionCallback {
        override fun isCustomSize(z: Boolean, resizeType: ResizeType) {}

        override fun done() {
            hideFragment()
        }

        override fun onCustumSize(i: Int, i2: Int, i3: Int, str: String, i4: Int) {
            updateHitRatio(i3, str)
            if (i3 == mTemplate!!.geTypeResize()) {
                return
            }
            if (ResizeFragment.instance != null) {
                ResizeFragment.instance!!.scrollToSelectedPosition()
            }
            showProgressSimple()
            executor.execute {
                var runnable: Runnable
                try {
                    try {
                        // FIX: Use reset() instead of invalidate() to recycle old bitmap (matches Java ref)
                        blurredImageView.reset()
                        mTemplate!!.resizeType = i3
                        mTemplate!!.imgResize = str
                        val size = AspectRatioCalculator.getSize(i3, mTemplate!!.resolution)
                        mTemplate!!.setWidthAndHeight(size.first, size.second)
                        blurredImageView.initCanvasDimension(blurredImageView.getWidth(), blurredImageView.getHeight(), i3)

                        val cropTo16x9 = if (mTemplate!!.geTypeResize() == ResizeType.SOCIAL_STORY.ordinal) {
                            BitmapCropper.cropTo9x16(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())
                        } else if (mTemplate!!.geTypeResize() == ResizeType.SQUARE.ordinal) {
                            BitmapCropper.cropTo1x1(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())
                        } else {
                            BitmapCropper.cropTo16x9(blurredImageView.bitmapOriginal, blurredImageView.getW(), blurredImageView.getH())
                        }

                        blurredImageView.updatePosCanvas(cropTo16x9)
                        // FIX: Store unblurred reference (matches Java ref line 7761)
                        blurredImageView.setBitmapBlured(cropTo16x9)
                        blurredImageView.updateIpad(cropTo16x9!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize())

                        // Recalculate the iPad square bitmap based on type
                        var bitmap2: Bitmap? = blurredImageView.bitmapSquare
                        var rect: Rect? = blurredImageView.rectSquare

                        if (mTemplate!!.ipad_type == IpadType.IPAD_NEOMORPHIC.ordinal) {
                            val width = (blurredImageView.ipad_rect!!.width() * 0.6f).toInt()
                            var round = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                            var round2 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                            var i6 = width + round
                            if (i6 > blurredImageView.bitmapOriginal!!.width) {
                                round -= i6 - blurredImageView.bitmapOriginal!!.width
                                i6 = blurredImageView.bitmapOriginal!!.width
                            }
                            var i7 = width + round2
                            if (i7 > blurredImageView.bitmapOriginal!!.height) {
                                round2 -= i7 - blurredImageView.bitmapOriginal!!.height
                                i7 = blurredImageView.bitmapOriginal!!.height
                            }
                            if (round < 0) round = 0
                            if (round2 < 0) round2 = 0
                            val rect2 = Rect(round, round2, i6, i7)
                            blurredImageView.setRadius_square(width)
                            val width2 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                            val height2 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                            bitmap2 = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect2, width, width2, height2)
                            rect2.right = rect2.left + width2
                            rect2.bottom = rect2.top + height2
                            blurredImageView.rectSquare = rect2
                            rect = rect2
                        } else if (mTemplate!!.ipad_type == IpadType.IPAD.ordinal ||
                                   mTemplate!!.ipad_type == IpadType.IPAD_UNBLUR.ordinal ||
                                   mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal
                        ) {
                            val width5 = (blurredImageView.ipad_rect!!.width() * 0.87530595f).toInt()
                            val i7 = (width5 * 1.13f).toInt()
                            val min = Math.min(width5, i7)
                            var round5 = Math.round(blurredImageView.bitmapOriginal!!.width * mTemplate!!.x_square)
                            var round6 = Math.round(blurredImageView.bitmapOriginal!!.height * mTemplate!!.y_square)
                            var i8 = width5 + round5
                            if (i8 > blurredImageView.bitmapOriginal!!.width) {
                                round5 -= i8 - blurredImageView.bitmapOriginal!!.width
                                i8 = blurredImageView.bitmapOriginal!!.width
                            }
                            var i9 = i7 + round6
                            if (i9 > blurredImageView.bitmapOriginal!!.height) {
                                round6 -= i9 - blurredImageView.bitmapOriginal!!.height
                                i9 = blurredImageView.bitmapOriginal!!.height
                            }
                            if (round5 < 0) round5 = 0
                            if (round6 < 0) round6 = 0
                            val rect4 = Rect(round5, round6, i8, i9)
                            val width7 = (blurredImageView.bitmapOriginal!!.width * mTemplate!!.width_square).toInt()
                            val height6 = (blurredImageView.bitmapOriginal!!.height * mTemplate!!.height_square).toInt()
                            if (mTemplate!!.ipad_type == IpadType.IPAD_CLASSIC.ordinal) {
                                bitmap2 = UtilsBitmap.cropToSquare(blurredImageView.bitmapOriginal!!, rect4, width7, height6)
                                blurredImageView.setRadius_square(0)
                            } else {
                                val i10 = (min * 0.10800001f).toInt()
                                blurredImageView.setRadius_square(i10)
                                bitmap2 = UtilsBitmap.cropToSquareWithRoundCorners(blurredImageView.bitmapOriginal!!, rect4, i10, width7, height6)
                            }
                            rect4.right = rect4.left + width7
                            rect4.bottom = rect4.top + height6
                            blurredImageView.rectSquare = rect4
                            rect = rect4
                        }

                        // FIX: Use setBitmap (NOT updateBitmap) to trigger createRect() (matches Java ref)
                        val blurred = UtilsBitmap.blur(this@createIDimensionCallback, cropTo16x9!!, 20, 1)
                        if (bitmap2 != null && rect != null) {
                            if (mTemplate!!.gradient != null) {
                                blurredImageView.setBitmap(blurred, bitmap2, mTemplate!!.gradient!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                            } else {
                                blurredImageView.setBitmap(blurred, bitmap2, mTemplate!!.color_ipad, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                            }
                        } else {
                            blurredImageView.bitmapBlured = blurred
                        }

                        // FIX: Resize entities and surah name to match new canvas (matches Java ref)
                        blurredImageView.resizeEntity()
                        blurredImageView.updatePosSurahName()

                        // FIX: Store non-blur reference and do second setBitmap pass (matches Java ref)
                        blurredImageView.setBitmapNotBlur(cropTo16x9)
                        val cropCopy = cropTo16x9?.let { Bitmap.createBitmap(it) }
                        if (cropCopy != null) {
                            val blurred2 = UtilsBitmap.blur(this@createIDimensionCallback, cropCopy, 20, 1)
                            if (bitmap2 != null && rect != null) {
                                if (mTemplate!!.gradient != null) {
                                    blurredImageView.setBitmap(blurred2, bitmap2, mTemplate!!.gradient!!, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                                } else {
                                    blurredImageView.setBitmap(blurred2, bitmap2, mTemplate!!.color_ipad, mTemplate!!.ipad_type, mTemplate!!.geTypeResize(), rect)
                                }
                            }
                            blurredImageView.resizeEntity()
                            blurredImageView.updatePosSurahName()
                        }

                        runnable = Runnable {
                            blurredImageView.invalidate()
                            trackViewEntity?.invalidate()
                            updateTime()
                            hideProgressFragment()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Tag resize : ", "init " + e.message)
                        e.printStackTrace()
                        runnable = Runnable { hideProgressFragment() }
                    }
                    this@createIDimensionCallback.runOnUiThread(runnable)
                } catch (unused: Exception) {
                    unused.printStackTrace()
                }
            }
        }
    }
}

fun EngineActivity.createIAudioCallback(): AddAudioFragment.IAudioCallback {
    return object : AddAudioFragment.IAudioCallback {
        override fun upload() {
            if (checkPermissionAudio()) {
                pickAudio()
            }
        }

        override fun extract() {
            pickVideoForAudio()
        }

        override fun cancel() {
            hideFragment()
            try {
                setupShowFragment(mResources!!.getString(R.string.quran))
                val beginTransaction = supportFragmentManager.beginTransaction()
                val addQuranInstance = createAddQuranFragment(iAddQuran, mResources!!)
                mCurrentFragment = addQuranInstance
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (unused: Exception) {
            }
        }
    }
}

@Suppress("TYPE_CHECKING_HAS_RUN_INTO_RECURSIVE_PROBLEM")
fun EngineActivity.createIEditSName(): EditS_NameFragment.IEditS_Name {
    return object : EditS_NameFragment.IEditS_Name {
        override fun onFont(surahNameEntity: SurahNameEntity) {
            val beginTransaction = supportFragmentManager.beginTransaction()
            mCurrentFragment = FontFragment.getInstance(iFontCallback, surahNameEntity.nameFont!!, surahNameEntity.getPaintAya().typeface)
            beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
            beginTransaction.commit()
            setupShowFragment(mResources!!.getString(R.string.font))
        }

        override fun onEdit(surahNameEntity: SurahNameEntity) {
            try {
                isToCrop = true
                val intent = Intent(this@createIEditSName, hazem.nurmontage.videoquran.ui.editor.EditSNameActivity::class.java)
                intent.putExtra("surah_name", blurredImageView.surahNameEntity!!.name)
                intent.putExtra("reader_name", blurredImageView.surahNameEntity!!.reader)
                intent.putExtra("style", blurredImageView.surahNameEntity!!.style)
                intent.putExtra(StreamInformation.KEY_INDEX, blurredImageView.surahNameEntity!!.index_surah)
                intent.putExtra("isBg", blurredImageView.surahNameEntity!!.isHaveBg)
                intent.putExtra("clrBg", blurredImageView.surahNameEntity!!.clrBg)
                editSurahNameResult.launch(intent)
                overridePendingTransition(0, 0)
            } catch (unused: Exception) {
            }
        }

        override fun update() {
            blurredImageView.postInvalidate()
        }

        override fun onDone() {
            selectSurahName()
        }

        override fun onColor(surahNameEntity: SurahNameEntity) {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val fragSNameObj = createColorSNameFragment(iEditSName, surahNameEntity, mResources!!)
                mCurrentFragment = fragSNameObj
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.color))
            } catch (unused: Exception) {
            }
        }
    }
}

fun EngineActivity.createIFontCallback(): FontFragment.IFontCallback {
    return object : FontFragment.IFontCallback {
        override fun onAdd(str: String?, typeface: Typeface?) {
            try {
                if (trackViewEntity.selectedEntity != null) {
                    val entityView = trackViewEntity.selectedEntity!!.getEntityView()
                    if (entityView is QuranEntity) {
                        entityView.nameFont = str!!
                        entityView.getPaintAya().typeface = typeface!!
                        entityView.setupScaleSave(entityView.factorSize, blurredImageView.getmCanvas_width())
                        blurredImageView.invalidate()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDone(str: String?, typeface: Typeface?) {
            hideFragment()
            try {
                if (trackViewEntity.selectedEntity != null) {
                    val entityView = trackViewEntity.selectedEntity!!.getEntityView()
                    if (entityView is QuranEntity) {
                        entityView.nameFont = str!!
                        entityView.getPaintAya().typeface = typeface!!
                        entityView.setupScaleSave(entityView.factorSize, blurredImageView.getmCanvas_width())
                        blurredImageView.invalidate()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCancel(str: String?, typeface: Typeface?) {
            hideFragment()
        }
    }
}

@Suppress("TYPE_CHECKING_HAS_RUN_INTO_RECURSIVE_PROBLEM")
fun EngineActivity.createIBismilahEntityCallback(): EditBismilahEntityFragment.IBismilahEntityCallback {
    return object : EditBismilahEntityFragment.IBismilahEntityCallback {
        override fun updatePreset(ayaTextPreset: AyaTextPreset) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as BismilahEntity).setPreset(ayaTextPreset)
                blurredImageView.invalidate()
            }
        }

        override fun updateAya(i: Int) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as BismilahEntity).setColor(i)
                blurredImageView.invalidate()
            }
        }

        override fun onAnim() {
            try {
                val bismilahEntity = trackViewEntity.selectedEntity!!.getEntityView() as BismilahEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = EffectBismilahFragment.get(bismilahEntity.bismilahTimeline!!.getTransition(), mResources, iTransitionBismilahCallback, bismilahEntity.bismilahTimeline!!)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.animtion))
            } catch (unused: Exception) {
            }
        }

        override fun onDelete() {
            try {
                blurredImageView.entity_select = null
                blurredImageView.postInvalidate()
                hideFragment()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun update() {
            blurredImageView.postInvalidate()
        }

        override fun onDone() {
            hideFragment()
            blurredImageView.invalidate()
        }

        override fun onColor() {
            try {
                val bismilahEntity = trackViewEntity.selectedEntity!!.getEntityView() as BismilahEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                val fragBismilahObj = createColorBismilahFragment(iBismilahEntityCallback, bismilahEntity, mResources!!)
                mCurrentFragment = fragBismilahObj
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.color))
            } catch (unused: Exception) {
            }
        }

        override fun fromTheStart() {
            trackViewEntity.translateToStart()
        }

        override fun fromNow() {
            // translateFromNowBismilah not available, using approximate
        }

        override fun untilNow() {
            // translateUntilNowBismilah not available, using approximate
        }

        override fun untilTheEnd() {
            trackViewEntity.translateToEnd()
        }
    }
}

@Suppress("TYPE_CHECKING_HAS_RUN_INTO_RECURSIVE_PROBLEM")
fun EngineActivity.createIEditEntityCallback(): EditEntityFragment.IEditEntityCallback {
    return object : EditEntityFragment.IEditEntityCallback {
        override fun updatePreset(ayaTextPreset: AyaTextPreset) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity).setPreset(ayaTextPreset)
                blurredImageView.invalidate()
            }
        }

        override fun updateAya(i: Int) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity).setColor(i)
                blurredImageView.invalidate()
            }
        }

        override fun updateTrsl(i: Int) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity).setColorTranslation(i)
                blurredImageView.invalidate()
            }
        }

        override fun onFont() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = FontFragment.getInstance(iFontCallback, quranEntity.nameFont!!, quranEntity.getPaintAya().typeface)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.font))
            } catch (unused: Exception) {
            }
        }

        override fun onIcon() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = EditIconQuranFragment.getInstance(iQuranIconCallback, quranEntity.icon)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (unused: Exception) {
            }
        }

        override fun onAnim() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = EffectAyaFragment.get(quranEntity.entityQuran!!.getTransition(), mResources, iTransitionCallback, quranEntity.entityQuran!!)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.animtion))
            } catch (unused: Exception) {
            }
        }

        override fun onDelete() {
            try {
                blurredImageView.entity_select = null
                blurredImageView.postInvalidate()
                hideFragment()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDone() {
            hideFragment()
            blurredImageView.invalidate()
        }

        override fun onColor() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                val fragAyaObj: Any = ColorAyaFragment.getInstance(iEditTrstEntityCallback, quranEntity, mResources!!)
                mCurrentFragment = fragAyaObj as androidx.fragment.app.Fragment?
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.color))
            } catch (unused: Exception) {
            }
        }

        override fun onEdit() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = EditTextFragment.getInstance(iEdiTextCallback, quranEntity)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (unused: Exception) {
            }
        }

        override fun onCut() {
            splitEntity(trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity)
        }

        override fun onDuplicate() {
            duplicateEntity(trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity)
        }

        override fun fromTheStart() {
            trackViewEntity.translateToStart()
        }

        override fun fromNow() {
            trackViewEntity.translateFromNow()
        }

        override fun untilNow() {
            trackViewEntity.translateUntilNow()
        }

        override fun untilTheEnd() {
            trackViewEntity.translateToEnd()
        }
    }
}

@Suppress("TYPE_CHECKING_HAS_RUN_INTO_RECURSIVE_PROBLEM")
fun EngineActivity.createIEditTrstEntityCallback(): EditTrslEntityFragment.IEditEntityCallback {
    return object : EditTrslEntityFragment.IEditEntityCallback {
        override fun updatePreset(ayaTextPreset: AyaTextPreset) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity).setPreset(ayaTextPreset)
                blurredImageView.invalidate()
            }
        }

        override fun updateAya(i: Int) {
            if (trackViewEntity.selectedEntity != null) {
                (trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity).setColor(i)
                blurredImageView.invalidate()
            }
        }

        override fun updateTrsl(i: Int) {
            // Translation entity doesn't have separate trsl color
        }

        override fun onFont() {
            try {
                val translationQuranEntity = trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = FontFragment.getInstance(iFontCallback, translationQuranEntity.nameFont!!, translationQuranEntity.getPaintAya().typeface)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.font))
            } catch (unused: Exception) {
            }
        }

        override fun onIcon() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = EditIconQuranFragment.getInstance(iQuranIconCallback, quranEntity.icon)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (unused: Exception) {
            }
        }

        override fun onAnim() {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = EffectAyaFragment.get(quranEntity.entityQuran!!.getTransition(), mResources, iTransitionCallback, quranEntity.entityQuran!!)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.animtion))
            } catch (unused: Exception) {
            }
        }

        override fun onDelete() {
            try {
                blurredImageView.entity_select = null
                blurredImageView.postInvalidate()
                hideFragment()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDone() {
            hideFragment()
            blurredImageView.invalidate()
        }

        override fun onColor() {
            try {
                val translationQuranEntity = trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity
                val beginTransaction = supportFragmentManager.beginTransaction()
                val fragTrslObj = createColorTrslAyaFragment(iEditTrstEntityCallback, translationQuranEntity, mResources!!)
                mCurrentFragment = fragTrslObj
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                setupShowFragment(mResources!!.getString(R.string.color))
            } catch (unused: Exception) {
            }
        }

        override fun onEdit() {
            try {
                val translationQuranEntity = trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity
                val intent = Intent(this@createIEditTrstEntityCallback, hazem.nurmontage.videoquran.ui.editor.EditTrslTxtActivity::class.java)
                intent.putExtra("txt", translationQuranEntity.txt)
                isToCrop = true
                editTrslResult.launch(intent)
                overridePendingTransition(0, 0)
            } catch (unused: Exception) {
            }
        }

        override fun onCut() {
            splitEntity(trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity)
        }

        override fun onDuplicate() {
            duplicateEntity(trackViewEntity.selectedEntity!!.getEntityView() as TranslationQuranEntity)
        }

        override fun fromTheStart() {
            trackViewEntity.translateToStart()
        }

        override fun fromNow() {
            trackViewEntity.translateFromNow()
        }

        override fun untilNow() {
            trackViewEntity.translateUntilNow()
        }

        override fun untilTheEnd() {
            trackViewEntity.translateToEnd()
        }
    }
}

fun EngineActivity.createIEditMultipleCallback(): EditMultipleEntityFragment.IEditMultipleCallback {
    return object : EditMultipleEntityFragment.IEditMultipleCallback {
        override fun onDelete() {
            dialogDeleteSelected()
        }
    }
}

fun EngineActivity.createIEditMediaCallback(): EditMediaFragment.IEditMediaCallback {
    return object : EditMediaFragment.IEditMediaCallback {
        override fun onReplace() {}

        override fun updateEntity(effectAudioType: EffectAudioType, entityAudio: EntityAudio) {
            try {
                val entityAudio2 = trackViewEntity.entityListAudio[trackViewEntity.entityListAudio.indexOf(entityAudio)]
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDone() {
            hideFragment()
        }

        override fun startPreview() {
            try {
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mIsPlaying = true
                trackViewEntity.isPlaying = true
                blurredImageView.isPlaying = true
                startTimelineAnimationPreview(entityAudio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun pausePreview() {
            try {
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mIsPlaying = false
                trackViewEntity.isPlaying = false
                blurredImageView.isPlaying = false
                pauseTimelineAnimation()
                try {
                    if (entityAudio.mediaPlayer != null && entityAudio.mediaPlayer!!.isPlaying) {
                        entityAudio.mediaPlayer!!.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                VolumeFragment.instance?.updateButton()
                SpeedFragment.instance?.updateButton()
                FadeInOutFragment.instance?.updateButton()
                EchoEffectFragment.instance?.updateButton()
                EnhanceVoiceFragment.instance?.updateButton()
                RemoveNoiceFragment.instance?.updateButton()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCmdPlay(str: String) {
            try {
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                applyffectPlayAuto(str, entityAudio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCmd(str: String) {
            try {
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                applyffect(str, entityAudio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCmdAll(effectAudio: EffectAudio) {
            applyffectAll(effectAudio, 0)
        }

        override fun onDuplicate() {
            try {
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                duplicateEntityAudio(Math.round((entityAudio.rect.right - entityAudio.rect.left) / trackViewEntity.second_in_screen * 1000.0f), entityAudio)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDelete() {
            try {
                blurredImageView.entity_select = null
                blurredImageView.postInvalidate()
                hideFragment()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onCut() {
            try {
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                val abs = Math.abs(trackViewEntity.getCurrentPosition())
                if (abs <= entityAudio.rect.left || abs >= entityAudio.rect.right) {
                    return
                }
                val second_in_screenNoScale = trackViewEntity.second_in_screenNoScale * 0.1f
                if (abs <= entityAudio.rect.left || abs >= entityAudio.rect.left + second_in_screenNoScale) {
                    if (abs >= entityAudio.rect.right || abs <= entityAudio.rect.right - second_in_screenNoScale) {
                        val round = Math.round(
                            (abs - entityAudio.rect.left) / trackViewEntity.second_in_screen * 1000.0f
                        )
                        val split = entityAudio.split(abs)
                        if (split != null) {
                            trackViewEntity.stackSplit(entityAudio)
                            split.pathFfmpeg = entityAudio.getPathFfmpeg()
                            split.setAmps(entityAudio.amps)
                            trackViewEntity.addAudio(split, entityAudio.index + 1)
                            entityAudio.setCurrentRect()
                            entityAudio.right = abs
                            entityAudio.onChange()
                        }
                        trackViewEntity.invalidate()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun reverbEffect() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = ReverbePresetFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun echoEffect() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = EchoEffectFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun noice() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = RemoveNoiceFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun enhanceVoice() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = EnhanceVoiceFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun speedEffect() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = SpeedFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun volumeEffect() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = VolumeFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun pitchEffect() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = PitchFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun fadeEffect() {
            try {
                val beginTransaction = supportFragmentManager.beginTransaction()
                val entityAudio = trackViewEntity.selectedEntity!! as EntityAudio
                mCurrentFragment = FadeInOutFragment.getInstance(iEditMediaCallback, entityAudio)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun EngineActivity.createIEdiTextCallback(): EditTextFragment.IEdiTextCallback {
    return object : EditTextFragment.IEdiTextCallback {
        override fun onDone(entityQuranTimeline: EntityQuranTimeline?) {
            hideFragment()
        }

        override fun onUpdate(quranEntity: QuranEntity?) {
            blurredImageView.invalidate()
        }
    }
}

fun EngineActivity.createITransitionCallback(): TransitionEntityAdabters.ITransition {
    return object : TransitionEntityAdabters.ITransition {
        override fun `in`(type: String, entityQuranTimeline: EntityQuranTimeline) {
            if (entityQuranTimeline.getTransition() == null) {
                entityQuranTimeline.setTransition(Transition())
            }
            entityQuranTimeline.getTransition()!!.isIn = true
            entityQuranTimeline.getTransition()!!.type_in = type
            addUpdateAnim(blurredImageView.mIsti3adhaEntity?.bismilahTimeline, entityQuranTimeline)
            addUpdateAnim(blurredImageView.bismilahEntity?.bismilahTimeline, entityQuranTimeline)
            for (entityQuranTimeline2 in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline2!!.getTransition() == null) {
                    entityQuranTimeline2!!.setTransition(Transition())
                }
                entityQuranTimeline2!!.getTransition()!!.isIn = true
                entityQuranTimeline2!!.getTransition()!!.type_in = type
            }
        }

        override fun `out`(type: String, entityQuranTimeline: EntityQuranTimeline) {
            if (entityQuranTimeline.getTransition() == null) {
                entityQuranTimeline.setTransition(Transition())
            }
            entityQuranTimeline.getTransition()!!.isOut = true
            entityQuranTimeline.getTransition()!!.type_out = type
            addUpdateAnim(blurredImageView.mIsti3adhaEntity?.bismilahTimeline, entityQuranTimeline)
            addUpdateAnim(blurredImageView.bismilahEntity?.bismilahTimeline, entityQuranTimeline)
            for (entityQuranTimeline2 in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline2!!.getTransition() == null) {
                    entityQuranTimeline2!!.setTransition(Transition())
                }
                entityQuranTimeline2!!.getTransition()!!.isOut = true
                entityQuranTimeline2!!.getTransition()!!.type_out = type
            }
        }

        override fun destroy(entityQuranTimeline: EntityQuranTimeline) {
            entityQuranTimeline.setTransition(null)
        }

        override fun playing(entityQuranTimeline: EntityQuranTimeline) {}

        override fun onHideFragment(entityQuranTimeline: EntityQuranTimeline) {
            hideFragment()
        }

        override fun remove(i: Int, entityQuranTimeline: EntityQuranTimeline) {
            addUpdateAnim(blurredImageView.mIsti3adhaEntity?.bismilahTimeline, entityQuranTimeline)
            addUpdateAnim(blurredImageView.bismilahEntity?.bismilahTimeline, entityQuranTimeline)
            for (entityQuranTimeline2 in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline.getTransition() == null) {
                    entityQuranTimeline2!!.setTransition(null)
                    return
                }
                if (entityQuranTimeline2!!.getTransition() == null) {
                    entityQuranTimeline2!!.setTransition(Transition())
                }
                entityQuranTimeline2!!.getTransition()!!.isOut = entityQuranTimeline.getTransition()!!.isOut
                entityQuranTimeline2!!.getTransition()!!.type_out = entityQuranTimeline.getTransition()!!.type_out
                entityQuranTimeline2!!.getTransition()!!.duration_out = entityQuranTimeline.getTransition()!!.duration_out
                entityQuranTimeline2!!.getTransition()!!.isIn = entityQuranTimeline.getTransition()!!.isIn
                entityQuranTimeline2!!.getTransition()!!.type_in = entityQuranTimeline.getTransition()!!.type_in
                entityQuranTimeline2!!.getTransition()!!.duration_in = entityQuranTimeline.getTransition()!!.duration_in
            }
        }

        override fun updateDurationIn(f: Float, entityQuranTimeline: EntityQuranTimeline) {
            addUpdateAnim(blurredImageView.mIsti3adhaEntity?.bismilahTimeline, entityQuranTimeline)
            addUpdateAnim(blurredImageView.bismilahEntity?.bismilahTimeline, entityQuranTimeline)
            for (entityQuranTimeline2 in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline2!!.getTransition() != null) {
                    entityQuranTimeline2!!.getTransition()!!.isIn = entityQuranTimeline.getTransition()!!.isIn
                    entityQuranTimeline2!!.getTransition()!!.duration_in = entityQuranTimeline.getTransition()!!.duration_in
                    entityQuranTimeline2!!.getTransition()!!.type_in = entityQuranTimeline.getTransition()!!.type_in
                }
            }
        }

        override fun updateDurationOut(f: Float, entityQuranTimeline: EntityQuranTimeline) {
            addUpdateAnim(blurredImageView.mIsti3adhaEntity?.bismilahTimeline, entityQuranTimeline)
            addUpdateAnim(blurredImageView.bismilahEntity?.bismilahTimeline, entityQuranTimeline)
            for (entityQuranTimeline2 in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline2!!.getTransition() != null) {
                    entityQuranTimeline2!!.getTransition()!!.isOut = entityQuranTimeline.getTransition()!!.isOut
                    entityQuranTimeline2!!.getTransition()!!.duration_out = entityQuranTimeline.getTransition()!!.duration_out
                    entityQuranTimeline2!!.getTransition()!!.type_out = entityQuranTimeline.getTransition()!!.type_out
                }
            }
        }

        override fun applyAll(i: Int, entityQuranTimeline: EntityQuranTimeline) {
            addUpdateAnim(blurredImageView.mIsti3adhaEntity?.bismilahTimeline, entityQuranTimeline)
            addUpdateAnim(blurredImageView.bismilahEntity?.bismilahTimeline, entityQuranTimeline)
            for (entityQuranTimeline2 in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline.getTransition() == null) {
                    entityQuranTimeline2!!.setTransition(null)
                } else {
                    if (entityQuranTimeline2!!.getTransition() == null) {
                        entityQuranTimeline2!!.setTransition(Transition())
                    }
                    entityQuranTimeline2!!.getTransition()!!.isOut = entityQuranTimeline.getTransition()!!.isOut
                    entityQuranTimeline2!!.getTransition()!!.type_out = entityQuranTimeline.getTransition()!!.type_out
                    entityQuranTimeline2!!.getTransition()!!.duration_out = entityQuranTimeline.getTransition()!!.duration_out
                    entityQuranTimeline2!!.getTransition()!!.isIn = entityQuranTimeline.getTransition()!!.isIn
                    entityQuranTimeline2!!.getTransition()!!.type_in = entityQuranTimeline.getTransition()!!.type_in
                    entityQuranTimeline2!!.getTransition()!!.duration_in = entityQuranTimeline.getTransition()!!.duration_in
                }
            }
            hideProgressFragment()
        }
    }
}

fun EngineActivity.createITransitionBismilahCallback(): TransitionBismilahAdabters.ITransition {
    return object : TransitionBismilahAdabters.ITransition {
        override fun `in`(type: String, entityBismilahTimeline: EntityBismilahTimeline) {
            if (entityBismilahTimeline.getTransition() == null) {
                entityBismilahTimeline.setTransition(Transition())
            }
            entityBismilahTimeline.getTransition()!!.isIn = true
            entityBismilahTimeline.getTransition()!!.type_in = type
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline!!.getTransition() == null) {
                    entityQuranTimeline!!.setTransition(Transition())
                }
                entityQuranTimeline!!.getTransition()!!.isIn = true
                entityQuranTimeline!!.getTransition()!!.type_in = type
            }
        }

        override fun `out`(type: String, entityBismilahTimeline: EntityBismilahTimeline) {
            if (entityBismilahTimeline.getTransition() == null) {
                entityBismilahTimeline.setTransition(Transition())
            }
            entityBismilahTimeline.getTransition()!!.isOut = true
            entityBismilahTimeline.getTransition()!!.type_out = type
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline!!.getTransition() == null) {
                    entityQuranTimeline!!.setTransition(Transition())
                }
                entityQuranTimeline!!.getTransition()!!.isOut = true
                entityQuranTimeline!!.getTransition()!!.type_out = type
            }
        }

        override fun destroy(entityBismilahTimeline: EntityBismilahTimeline) {
            entityBismilahTimeline.setTransition(null)
        }

        override fun playing(entityBismilahTimeline: EntityBismilahTimeline) {}

        override fun onHideFragment(entityBismilahTimeline: EntityBismilahTimeline) {
            hideFragment()
        }

        override fun remove(i: Int, entityBismilahTimeline: EntityBismilahTimeline) {
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityBismilahTimeline.getTransition() == null) {
                    entityQuranTimeline!!.setTransition(null)
                    return
                }
                if (entityQuranTimeline!!.getTransition() == null) {
                    entityQuranTimeline!!.setTransition(Transition())
                }
                entityQuranTimeline!!.getTransition()!!.isOut = entityBismilahTimeline.getTransition()!!.isOut
                entityQuranTimeline!!.getTransition()!!.type_out = entityBismilahTimeline.getTransition()!!.type_out
                entityQuranTimeline!!.getTransition()!!.duration_out = entityBismilahTimeline.getTransition()!!.duration_out
                entityQuranTimeline!!.getTransition()!!.isIn = entityBismilahTimeline.getTransition()!!.isIn
                entityQuranTimeline!!.getTransition()!!.type_in = entityBismilahTimeline.getTransition()!!.type_in
                entityQuranTimeline!!.getTransition()!!.duration_in = entityBismilahTimeline.getTransition()!!.duration_in
            }
        }

        override fun updateDurationIn(f: Float, entityBismilahTimeline: EntityBismilahTimeline) {
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline!!.getTransition() != null) {
                    entityQuranTimeline!!.getTransition()!!.isIn = entityBismilahTimeline.getTransition()!!.isIn
                    entityQuranTimeline!!.getTransition()!!.duration_in = entityBismilahTimeline.getTransition()!!.duration_in
                    entityQuranTimeline!!.getTransition()!!.type_in = entityBismilahTimeline.getTransition()!!.type_in
                }
            }
        }

        override fun updateDurationOut(f: Float, entityBismilahTimeline: EntityBismilahTimeline) {
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityQuranTimeline!!.getTransition() != null) {
                    entityQuranTimeline!!.getTransition()!!.isOut = entityBismilahTimeline.getTransition()!!.isOut
                    entityQuranTimeline!!.getTransition()!!.duration_out = entityBismilahTimeline.getTransition()!!.duration_out
                    entityQuranTimeline!!.getTransition()!!.type_out = entityBismilahTimeline.getTransition()!!.type_out
                }
            }
        }

        override fun applyAll(entityBismilahTimeline: EntityBismilahTimeline) {
            for (entityQuranTimeline in trackViewEntity.entityListQuran) {
                if (entityBismilahTimeline.getTransition() == null) {
                    entityQuranTimeline!!.setTransition(null)
                    return
                }
                if (entityQuranTimeline!!.getTransition() == null) {
                    entityQuranTimeline!!.setTransition(Transition())
                }
                entityQuranTimeline!!.getTransition()!!.isOut = entityBismilahTimeline.getTransition()!!.isOut
                entityQuranTimeline!!.getTransition()!!.type_out = entityBismilahTimeline.getTransition()!!.type_out
                entityQuranTimeline!!.getTransition()!!.duration_out = entityBismilahTimeline.getTransition()!!.duration_out
                entityQuranTimeline!!.getTransition()!!.isIn = entityBismilahTimeline.getTransition()!!.isIn
                entityQuranTimeline!!.getTransition()!!.type_in = entityBismilahTimeline.getTransition()!!.type_in
                entityQuranTimeline!!.getTransition()!!.duration_in = entityBismilahTimeline.getTransition()!!.duration_in
            }
            hideProgressFragment()
        }
    }
}

fun EngineActivity.createIIpadEditCallback(): EditIpadFragment.IIpadEditCallback {
    return object : EditIpadFragment.IIpadEditCallback {
        override fun onClick(i: Int, i2: Int) {
            mTemplate!!.color_ipad = i
            mTemplate!!.index_color = i2
            mTemplate!!.gradient = null
            blurredImageView.setColorIpad(i)
            blurredImageView.invalidate()
        }

        override fun onClick(gradient: Gradient, i: Int) {
            mTemplate!!.gradient = gradient
            mTemplate!!.index_color = i
            blurredImageView.setColorIpad(gradient)
            blurredImageView.invalidate()
        }


        override fun onGlassType(z: Boolean) {
            mTemplate!!.isGlass = z
            blurredImageView.isGlass = z
            blurredImageView.invalidate()
        }

        override fun onChangeType(i: Int) {
            if (blurredImageView.getmIpadType() == i) {
                return
            }
            if (EditIpadFragment.instance != null) {
                EditIpadFragment.instance!!.scrollToSelectedPosition()
            }
            try {
                mTemplate!!.ipad_type = i
                mTemplate!!.changeTypeIpad(i)
                if (mTemplate!!.isVideoSquare) {
                    if (i != IpadType.GRADIENT.ordinal && i != IpadType.BLACK_LAYER.ordinal && i != IpadType.MASK_BRUSH.ordinal && i != IpadType.BLUE_TYPE.ordinal && i != IpadType.CASSET_IMG.ordinal) {
                        if (mTemplate!!.ipad_type == IpadType.CASSET_IMG_BLUR.ordinal) {
                            blurredImageView.bitmapSquare = blurredImageView.bitmapBlured
                            blurredImageView.setRadius_square(0)
                        }
                    }
                    blurredImageView.bitmapSquare = blurredImageView.bitmapNotBlur
                    blurredImageView.setRadius_square(0)
                }
                if (i == IpadType.IPAD.ordinal || i == IpadType.IPAD_UNBLUR.ordinal) {
                    // IPad type change handling - simplified for split
                    blurredImageView.invalidate()
                }
                blurredImageView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDone() {
            hideFragment()
        }

        override fun onCancel() {
            hideFragment()
        }
    }
}

fun EngineActivity.createIQuranIconCallback(): EditIconQuranFragment.IQuranIconCallback {
    return object : EditIconQuranFragment.IQuranIconCallback {
        override fun add(str: String) {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                quranEntity.icon = str
                val resId = DrawableHelper.getIDDrawableIconByName(str)
                quranEntity.setVectorDrawable(androidx.core.content.ContextCompat.getDrawable(this@createIQuranIconCallback, resId) as? android.graphics.drawable.VectorDrawable)
                blurredImageView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDone(str: String) {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                quranEntity.icon = str
                val resId = DrawableHelper.getIDDrawableIconByName(str)
                quranEntity.setVectorDrawable(androidx.core.content.ContextCompat.getDrawable(this@createIQuranIconCallback, resId) as? android.graphics.drawable.VectorDrawable)
                blurredImageView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hideFragment()
        }

        override fun onCancel(str: String) {
            try {
                val quranEntity = trackViewEntity.selectedEntity!!.getEntityView() as QuranEntity
                quranEntity.icon = str
                val resId = DrawableHelper.getIDDrawableIconByName(str)
                quranEntity.setVectorDrawable(androidx.core.content.ContextCompat.getDrawable(this@createIQuranIconCallback, resId) as? android.graphics.drawable.VectorDrawable)
                blurredImageView.invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            hideFragment()
        }
    }
}

// Frame processor properties and runnable - must be created by EngineActivity
fun EngineActivity.createFrameProcessorRunnable(): Runnable {
    return Runnable {
        var str: String?
        while (true) {
            synchronized(frameLock) {
                if (pendingFramePath == null) {
                    isProcessingFrame = false
                    return@Runnable
                } else {
                    str = pendingFramePath
                    pendingFramePath = null
                }
            }
            processFrame(str!!)
        }
    }
}

// ==========================================================================
// Type-inference helper functions — break recursive type inference
// ==========================================================================

internal fun EngineActivity.createAddQuranFragment(cb: AddQuranFragment.IAddQuran, res: android.content.res.Resources): AddQuranFragment =
    AddQuranFragment.getInstance(cb, res)

internal fun EngineActivity.getEditSNameCb(): EditS_NameFragment.IEditS_Name = iEditSName
internal fun EngineActivity.getBismilahEntityCb(): EditBismilahEntityFragment.IBismilahEntityCallback = iBismilahEntityCallback
internal fun EngineActivity.getEditTrstEntityCb(): EditTrslEntityFragment.IEditEntityCallback = iEditTrstEntityCallback

internal fun EngineActivity.createColorSNameFragment(cb: EditS_NameFragment.IEditS_Name, entity: hazem.nurmontage.videoquran.model.SurahNameEntity, res: android.content.res.Resources): hazem.nurmontage.videoquran.fragment.ColorS_NameFragment =
    hazem.nurmontage.videoquran.fragment.ColorS_NameFragment.getInstance(cb, entity, res)

internal fun EngineActivity.createColorBismilahFragment(cb: EditBismilahEntityFragment.IBismilahEntityCallback, entity: hazem.nurmontage.videoquran.model.BismilahEntity, res: android.content.res.Resources): hazem.nurmontage.videoquran.fragment.ColorBismilahFragment =
    hazem.nurmontage.videoquran.fragment.ColorBismilahFragment.getInstance(cb, entity, res)

internal fun EngineActivity.createColorTrslAyaFragment(cb: EditTrslEntityFragment.IEditEntityCallback, entity: hazem.nurmontage.videoquran.model.TranslationQuranEntity, res: android.content.res.Resources): hazem.nurmontage.videoquran.fragment.ColorTrslAyaFragment =
    hazem.nurmontage.videoquran.fragment.ColorTrslAyaFragment.getInstance(cb, entity, res)
