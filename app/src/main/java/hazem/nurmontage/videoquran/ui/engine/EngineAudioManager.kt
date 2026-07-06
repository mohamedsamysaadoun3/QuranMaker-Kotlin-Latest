package hazem.nurmontage.videoquran.ui.engine

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.ui.editor.ChoiceBgFromVideoActivity
import hazem.nurmontage.videoquran.ui.gallery.GalleryPickerVideo
import hazem.nurmontage.videoquran.ui.gallery_photos.GalleryPickerOneImage
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.PCMWaveformExtractor
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.constant.IpadType
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.model.EntityMedia
import hazem.nurmontage.videoquran.model.RecitersModel
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.ProgressViewFragment
import java.io.File
import java.util.Locale

// ==========================================================================
// EngineAudioManager.kt
// All audio-related methods for EngineActivity, extracted as extension functions.
// ==========================================================================

fun EngineActivity.checkPermissionAudio(): Boolean {
    if (android.os.Build.VERSION.SDK_INT < 33 || androidx.core.app.ActivityCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_AUDIO") == 0) {
        return true
    }
    androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf("android.permission.READ_MEDIA_AUDIO"), 2)
    return false
}

fun EngineActivity.pickAudio() {
    try {
        val intent = Intent("android.intent.action.OPEN_DOCUMENT")
        intent.addCategory("android.intent.category.OPENABLE")
        intent.type = "audio/*"
        activityLauncher!!.launch(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.addAudio(uri: Uri) {
    try {
        val mediaPlayer = MediaPlayer()
        mPlayer = mediaPlayer
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
            mPlayer!!.setDataSource(uri.toString())
        } else {
            mPlayer!!.setDataSource(this, uri)
        }
        mPlayer!!.setOnErrorListener { _, _, _ ->
            runOnUiThread {
                hideProgressFragment()
                hideFragment()
            }
            true
        }
        mPlayer!!.prepareAsync()
        mPlayer!!.setOnPreparedListener { mediaPlayer2 ->
            if (mediaPlayer2 == null) return@setOnPreparedListener
            changeEntityAudio(mediaPlayer2.duration, uri)
        }
    } catch (e: Exception) {
        hideProgressFragment()
        hideFragment()
        e.printStackTrace()
    }
}

fun EngineActivity.addAudio(uri: Uri, list: List<String>, i: Int, str: String) {
    try {
        val mediaPlayer = MediaPlayer()
        mPlayer = mediaPlayer
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
            mPlayer!!.setDataSource(uri.toString())
        } else {
            mPlayer!!.setDataSource(this, uri)
        }
        mPlayer!!.setOnErrorListener { _, _, _ ->
            runOnUiThread {
                hideProgressFragment()
                hideFragment()
            }
            true
        }
        mPlayer!!.prepareAsync()
        mPlayer!!.setOnPreparedListener { mediaPlayer2 ->
            if (mediaPlayer2 == null) return@setOnPreparedListener
            changeEntityAudio(mediaPlayer2.duration, uri, list, i, str)
        }
    } catch (e: Exception) {
        hideProgressFragment()
        hideFragment()
        e.printStackTrace()
    }
}

fun EngineActivity.addAudioFromVideo(uri: Uri, str: String) {
    try {
        val mediaPlayer = MediaPlayer()
        mPlayer = mediaPlayer
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
            mPlayer!!.setDataSource(uri.toString())
        } else {
            mPlayer!!.setDataSource(this, uri)
        }
        mPlayer!!.setOnErrorListener { _, _, _ ->
            runOnUiThread {
                hideProgressFragment()
                hideFragment()
            }
            true
        }
        mPlayer!!.prepareAsync()
        mPlayer!!.setOnPreparedListener { mediaPlayer2 ->
            if (mediaPlayer2 == null) return@setOnPreparedListener
            changeEntityAudioFromVideo(mediaPlayer2.duration, uri, str)
            try {
                runOnUiThread { updateTimeToEndAya() }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    hideProgressFragment()
                    hideFragment()
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hideFragment()
        hideProgressFragment()
    }
}

fun EngineActivity.addAudioReciters(list: List<RecitersModel>) {
    val newSingleThreadExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    newSingleThreadExecutor.execute {
        addAudioRecitersBackground(list, handler)
    }
}

@Suppress("UNCHECKED_CAST")
fun EngineActivity.addAudioRecitersBackground(list: List<RecitersModel>, handler: android.os.Handler) {
    val arrayList = ArrayList<String>()
    val arrayList2 = ArrayList<String>()
    val sb = StringBuilder()
    try {
        val it = list.iterator()
        var i = 0
        while (it.hasNext()) {
            val recitersModel = it.next()
            try {
                val str = if (recitersModel.isTarteel) {
                    "https://audio-cdn.tarteel.ai/quran/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3"
                } else {
                    "https://everyayah.com/data/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3"
                }
                val downloadFile = AudioUtils.downloadFile(this@addAudioRecitersBackground, str, mTemplate!!.folder_template!!)
                if (downloadFile != null) {
                    arrayList.add(downloadFile)
                    arrayList2.add(str)
                    sb.append("file '").append(downloadFile.replace("'", "\\'")).append("'\n")
                    i++
                    try {
                        handler.post {
                            updateProgress(i, list.size)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (arrayList.isEmpty()) {
            handler.post {
                hideProgressFragment()
                hideFragment()
                updateTimeToEndAya()
                updateBtnToEnd()
                updateBtnToStart()
            }
            return
        }
        val file = File(mTemplate!!.folder_template, "concat_${System.currentTimeMillis()}.txt")
        val fileOutputStream = java.io.FileOutputStream(file)
        fileOutputStream.write(sb.toString().toByteArray())
        fileOutputStream.close()
        val file2 = File(mTemplate!!.folder_template, "${System.currentTimeMillis()}_output.mp3")
        val file3 = File(mTemplate!!.folder_template, "${System.currentTimeMillis()}_output.pcm")
        val arrayList3 = ArrayList<String>()
        arrayList3.add("-f")
        arrayList3.add("concat")
        arrayList3.add("-safe")
        arrayList3.add("0")
        arrayList3.add("-i")
        arrayList3.add(file.absolutePath)
        arrayList3.add("-map")
        arrayList3.add("0:a")
        arrayList3.add("-c")
        arrayList3.add("copy")
        arrayList3.add(file2.absolutePath)
        arrayList3.add("-map")
        arrayList3.add("0:a")
        arrayList3.add("-ac")
        arrayList3.add("1")
        arrayList3.add("-ar")
        arrayList3.add("44100")
        arrayList3.add("-f")
        arrayList3.add("s16le")
        arrayList3.add(file3.absolutePath)
        arrayList3.add("-y")
        val strArr = arrayList3.toTypedArray()
        handler.post {
            addAudioRecitersFfmpeg(strArr, file2, arrayList2, file3)
        }
    } catch (e3: Exception) {
        e3.printStackTrace()
        handler.post {
            hideProgressFragment()
            hideFragment()
        }
    }
}

fun EngineActivity.addAudioRecitersFfmpeg(
    strArr: Array<String>, file: File, list: List<String>, file2: File
) {
    id_ffmpeg.add(
        FFmpegKit.executeWithArgumentsAsync(strArr) { fFmpegSession ->
            if (fFmpegSession.returnCode.isValueSuccess()) {
                addAudio(Uri.fromFile(file), list, -1, file2.absolutePath)
            } else {
                android.util.Log.e("FFMPEG", "Failed: ${fFmpegSession.failStackTrace}")
                runOnUiThread {
                    hideProgressFragment()
                    hideFragment()
                }
            }
        }.sessionId
    )
}

fun EngineActivity.addAudioReciters(list: List<RecitersModel>, i: Int) {
    try {
        if (isDestroyed) return
        updateProgress(i + 1, list.size)
        if (i >= list.size) {
            runOnUiThread {
                updateTime()
                trackViewEntity.translateToEnd()
                updateBtnToEnd()
                updateBtnToStart()
                hideProgressFragment()
                hideFragment()
            }
            return
        }
        val recitersModel = list[i]
        val parse = if (recitersModel.isTarteel) {
            Uri.parse("https://audio-cdn.tarteel.ai/quran/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3")
        } else {
            Uri.parse("https://everyayah.com/data/${recitersModel.identifer}/${recitersModel.surah_index}${recitersModel.number_aya}.mp3")
        }
        val mediaPlayer = MediaPlayer()
        mPlayer = mediaPlayer
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (parse.scheme != null && parse.scheme!!.startsWith("http")) {
            mPlayer!!.setDataSource(parse.toString())
        } else {
            mPlayer!!.setDataSource(this, parse)
        }
        mPlayer!!.prepareAsync()
        // BEFORE: Missing onErrorListener — if prepareAsync fails, spinner runs forever
        mPlayer!!.setOnErrorListener { _, _, _ ->
            hideProgressFragment()
            true
        }
        mPlayer!!.setOnPreparedListener { mediaPlayer2 ->
            if (mediaPlayer2 == null) {
                hideProgressFragment()
            } else {
                changeEntityAudioReciters(mediaPlayer2.duration, parse, mediaPlayer2, list, i)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
    }
}

fun EngineActivity.addAudioRecitersTemplate(list: List<String>, index: Int, pathVideo: String) {
    java.util.concurrent.Executors.newSingleThreadExecutor().execute(
        addAudioRecitersTemplateRunnable(list, index, pathVideo)
    )
}

fun EngineActivity.buildSpeedFilters(f: Float): List<String> {
    val arrayList = ArrayList<String>()
    var speed = f
    if (speed < 0.5f) {
        while (speed < 0.5f) {
            arrayList.add("atempo=0.5")
            speed /= 0.5f
        }
        arrayList.add(String.format(Locale.US, "atempo=%.2f", speed))
    } else if (speed > 2.0f) {
        while (speed > 2.0f) {
            arrayList.add("atempo=2.0")
            speed /= 2.0f
        }
        arrayList.add(String.format(Locale.US, "atempo=%.2f", speed))
    } else {
        arrayList.add(String.format(Locale.US, "atempo=%.2f", speed))
    }
    return arrayList
}

fun EngineActivity.createCmd(effectAudio: EffectAudio, f: Float, f2: Float): String {
    val arrayList = ArrayList<String>()
    arrayList.add(String.format(Locale.US, "atrim=start=%.2f:end=%.2f", f, f2))
    arrayList.add("asetpts=N/SR/TB")
    if (effectAudio.isRemoveNoice) {
        arrayList.add("afftdn=nf=-25")
    }
    arrayList.add(String.format(Locale.US, "volume=%.2f", effectAudio.volume))
    if (effectAudio.fade_in > 0) {
        arrayList.add("afade=t=in:st=0:d=${effectAudio.fade_in}")
    }
    if (effectAudio.fade_out > 0) {
        val fade_out = effectAudio.fade_out
        arrayList.add("afade=t=out:st=${(f2 - f) - fade_out}:d=$fade_out")
    }
    if (effectAudio.isEnhance) {
        arrayList.add(Common.ENHANCE_CMD)
    }
    if (effectAudio.reverbPreset != null) {
        arrayList.add(effectAudio.reverbPreset!!)
    }
    if (effectAudio.decays > 0) {
        arrayList.add(String.format(Locale.US, "aecho=%.2f:%.2f:%s:%s", 1.0f, effectAudio.outGain, effectAudio.delays_cmd, effectAudio.decays_cmd))
    }
    if (effectAudio.speed != 1.0f) {
        arrayList.addAll(buildSpeedFilters(effectAudio.speed))
    }
    return TextUtils.join(",", arrayList)
}

fun EngineActivity.applyffectAll(effectAudio: EffectAudio, i: Int) {
    if (i >= trackViewEntity.entityListAudio.size) {
        runOnUiThread {
            trackViewEntity.invalidate()
            hideProgressFragment()
            if (iEditMediaCallback != null) {
                iEditMediaCallback!!.onDone()
            }
        }
        return
    }
    val entityAudioNotDeleted = trackViewEntity.getEntityAudioNotDeleted(i)
    if (entityAudioNotDeleted == null) {
        runOnUiThread {
            trackViewEntity.invalidate()
            hideProgressFragment()
            if (iEditMediaCallback != null) {
                iEditMediaCallback!!.onDone()
            }
        }
        return
    }
    val entityAudio = entityAudioNotDeleted.second as EntityAudio
    val intValue = entityAudioNotDeleted.first as Int
    val createCmd = createCmd(effectAudio, entityAudio.effectAudio.start / 1000.0f, entityAudio.effectAudio.end / 1000.0f)
    val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_audio_echo.mp3")
    val fromFile = Uri.fromFile(file)
    id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayOf("-i", entityAudio.getPathFfmpeg(), "-af", createCmd, "-y", file.absolutePath), object : FFmpegSessionCompleteCallback {
        override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
            if (fFmpegSession.returnCode.isValueSuccess()) {
                try {
                    mPlayer = MediaPlayer()
                    mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    if (fromFile.scheme != null && fromFile.scheme!!.startsWith("http")) {
                        mPlayer!!.setDataSource(fromFile.toString())
                    } else {
                        mPlayer!!.setDataSource(this@applyffectAll, fromFile)
                    }
                    mPlayer!!.prepareAsync()
                    mPlayer!!.setOnPreparedListener { mediaPlayer ->
                        if (entityAudio.mediaPlayer != null && mediaPlayer.duration != entityAudio.mediaPlayer!!.duration) {
                            entityAudio.right = entityAudio.rect.left + Math.round(trackViewEntity.second_in_screen * (mediaPlayer.duration / 1000.0f))
                            entityAudio.duration = mediaPlayer.duration * 1000
                            entityAudio.end = mediaPlayer.duration.toFloat()
                            entityAudio.start = 0.0f
                            entityAudio.max = (entityAudio.rect.right / entityAudio.scaleFactor) - ((entityAudio.rect.left / entityAudio.scaleFactor) - entityAudio.getOffsetLeft())
                            trackViewEntity.updateWhenEffect(entityAudio)
                        }
                        entityAudio.mediaPlayer = mPlayer
                        applyffectAll(effectAudio, intValue + 1)
                    }
                    entityAudio.pathFfmpegEffect = file.absolutePath
                    entityAudio.isApplyEffectInPreview = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        hideProgressFragment()
                        if (iEditMediaCallback != null) {
                            iEditMediaCallback!!.onDone()
                        }
                    }
                }
            }
        }
    }).sessionId)
}

fun EngineActivity.applyffect(str: String, entityAudio: EntityAudio) {
    showProgressSimple()
    val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_audio_echo.mp3")
    val uri = Uri.fromFile(file)
    id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayOf("-i", entityAudio.getPathFfmpeg(), "-af", str, "-y", file.absolutePath), object : FFmpegSessionCompleteCallback {
        override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
            if (fFmpegSession.returnCode.isValueSuccess()) {
                try {
                    mPlayer = MediaPlayer()
                    mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                    if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
                        mPlayer!!.setDataSource(uri.toString())
                    } else {
                        mPlayer!!.setDataSource(this@applyffect, uri)
                    }
                    mPlayer!!.prepareAsync()
                    mPlayer!!.setOnPreparedListener { mediaPlayer ->
                        if (entityAudio.mediaPlayer != null && mediaPlayer.duration != entityAudio.mediaPlayer!!.duration) {
                            entityAudio.right = entityAudio.rect.left + Math.round(trackViewEntity.second_in_screen * (mediaPlayer.duration / 1000.0f))
                            entityAudio.duration = mediaPlayer.duration * 1000
                            entityAudio.max = (entityAudio.rect.right / entityAudio.scaleFactor) - ((entityAudio.rect.left / entityAudio.scaleFactor) - entityAudio.getOffsetLeft())
                            trackViewEntity.updateWhenEffect(entityAudio)
                            runOnUiThread {
                                trackViewEntity.invalidate()
                                hideProgressFragment()
                            }
                        } else {
                            runOnUiThread { hideProgressFragment() }
                        }
                        entityAudio.mediaPlayer = mediaPlayer
                    }
                    entityAudio.pathFfmpegEffect = file.absolutePath
                    entityAudio.isApplyEffectInPreview = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread { hideProgressFragment() }
                }
            }
        }
    }).sessionId)
}

fun EngineActivity.applyffectPlayAuto(str: String, entityAudio: EntityAudio) {
    showProgressSimple()
    val file = File(mTemplate!!.folder_template, "${System.currentTimeMillis()}_audio_echo.mp3")
    id_ffmpeg.add(
        FFmpegKit.executeWithArgumentsAsync(
            arrayOf("-i", entityAudio.getPathFfmpeg()!!, "-af", str, "-y", file.absolutePath)
        ) { fFmpegSession ->
            if (fFmpegSession.returnCode.isValueSuccess()) {
                try {
                    val uri = Uri.fromFile(file)
                    mPlayer = MediaPlayer()
                    mPlayer!!.setAudioStreamType(3)
                    if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
                        mPlayer!!.setDataSource(uri.toString())
                    } else {
                        mPlayer!!.setDataSource(this@applyffectPlayAuto, uri)
                    }
                    mPlayer!!.prepareAsync()
                    mPlayer!!.setOnPreparedListener { mediaPlayer ->
                        if (entityAudio.mediaPlayer != null &&
                            mediaPlayer.duration != entityAudio.mediaPlayer!!.duration
                        ) {
                            entityAudio.right =
                                entityAudio.rect.left + Math.round(
                                    trackViewEntity.getSecond_in_screen() * (mediaPlayer.duration / 1000.0f)
                                )
                            entityAudio.end = mediaPlayer.duration.toFloat()
                            entityAudio.start = 0.0f
                            entityAudio.max =
                                (entityAudio.rect.right / entityAudio.scaleFactor) -
                                    ((entityAudio.rect.left / entityAudio.scaleFactor) - entityAudio.getOffsetLeft())
                            trackViewEntity.updateWhenEffect(entityAudio)
                            runOnUiThread {
                                trackViewEntity.invalidate()
                                entityAudio.mediaPlayer = mediaPlayer
                                iEditMediaCallback!!.startPreview()
                                hideProgressFragment()
                            }
                        } else {
                            runOnUiThread {
                                entityAudio.mediaPlayer = mediaPlayer
                                iEditMediaCallback!!.startPreview()
                                hideProgressFragment()
                            }
                        }
                    }
                    entityAudio.setApplyEffectInPreview(true)
                    entityAudio.setPathFfmpegEffect(file.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        hideProgressFragment()
                    }
                }
            }
        }.sessionId
    )
}

fun EngineActivity.duplicateEntityAudio(i: Int, entityAudio: EntityAudio) {
    try {
        val f = entityAudio.rect.right
        val entityAudio2 = EntityAudio(null, entityAudio.uri, f, entityAudio.rect.top, entityAudio.h, f + entityAudio.rect.width().toFloat(), entityAudio.max, entityAudio.secondInScreen, (i / 1000.0f).toInt(), 0.0f, 0.0f, 0.0f)
        entityAudio2.setAmps(entityAudio.amps)
        entityAudio2.setRenderer(entityAudio.getRenderer())
        entityAudio2.addPathHttp(entityAudio.pathsHttp)
        entityAudio2.mediaPlayer = entityAudio.mediaPlayer
        entityAudio2.rect.bottom = entityAudio.rect.bottom
        entityAudio2.pathFfmpeg = entityAudio.getPathFfmpeg()
        entityAudio2.effectAudio = entityAudio.effectAudio
        entityAudio2.videoPath = entityAudio.videoPath
        entityAudio2.isApplyEffectInPreview = entityAudio.isApplyEffectInPreview
        entityAudio2.scaleFactor = entityAudio.scaleFactor
        entityAudio2.index = entityAudio.index + 1
        entityAudio2.setOffsetRight(entityAudio.getOffsetRight())
        entityAudio2.setOffsetLeft(entityAudio.getOffsetLeft())
        entityAudio2.offset = entityAudio.offset
        entityAudio2.end = Math.round((Math.abs(Math.round((entityAudio.rect.right / trackViewEntity.second_in_screen) * 1000.0f)) - Math.abs(Math.round((entityAudio.rect.left / trackViewEntity.second_in_screen) * 1000.0f))) + entityAudio.start).toFloat()
        entityAudio2.start = entityAudio.start
        entityAudio2.minDuration = entityAudio.minDuration
        trackViewEntity.addAudio(entityAudio2, entityAudio.index + 1)
        trackViewEntity.invalidate()
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
        hideFragment()
    }
}

fun EngineActivity.changeEntityAudio(i: Int, uri: Uri) {
    try {
        val audio = trackViewEntity.getAudio()
        val scaleFactor = if (trackViewEntity.entityListAudio.isEmpty() || audio == null) 0.0f else audio.rect.right / trackViewEntity.scaleFactor
        val round = Math.round(trackViewEntity.width * 0.077f)
        val round2 = Math.round(trackViewEntity.second_in_screenNoScale * (i / 1000.0f))
        val f = round2.toFloat()
        val entityAudio = EntityAudio(null, uri, scaleFactor, 0.0f, round.toFloat(), f + scaleFactor, f, trackViewEntity.second_in_screenNoScale, i)
        entityAudio.mediaPlayer = mPlayer
        entityAudio.effectAudio.end = entityAudio.end
        entityAudio.effectAudio.start = entityAudio.start
        entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
        trackViewEntity.addAudio(entityAudio)
        if (round2 > 0 && round > 0) {
            val uri2 = if (!uri.toString().contains("share_with_me")) {
                AudioUtils.copyFromUri(this@changeEntityAudio, uri, mTemplate!!.folder_template!!)!!
            } else {
                uri.toString()
            }
            val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_output.pcm")
            val arrayList = ArrayList<String>()
            arrayList.add("-i")
            arrayList.add(uri2)
            arrayList.add("-map")
            arrayList.add("0:a")
            arrayList.add("-ac")
            arrayList.add("1")
            arrayList.add("-ar")
            arrayList.add("44100")
            arrayList.add("-f")
            arrayList.add("s16le")
            arrayList.add(file.absolutePath)
            arrayList.add("-y")
            val str = uri2
            id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayList.toTypedArray(), object : FFmpegSessionCompleteCallback {
                override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
                    if (fFmpegSession.returnCode.isValueSuccess()) {
                        try {
                            entityAudio.setAmps(PCMWaveformExtractor.extractWaveform(file.absolutePath, round2 / ((round * 0.1f).toInt() + (round * 0.07f).toInt())))
                            entityAudio.pathFfmpeg = str
                            runOnUiThread {
                                updateTimeToEndAya()
                                updateBtnToEnd()
                                updateBtnToStart()
                                hideProgressFragment()
                                hideFragment()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread {
                                hideProgressFragment()
                                hideFragment()
                            }
                        }
                    } else {
                        runOnUiThread {
                            hideProgressFragment()
                            hideFragment()
                        }
                    }
                }
            }).sessionId)
            trackViewEntity.invalidate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
        hideFragment()
    }
}

fun EngineActivity.changeEntityAudio(i: Int, uri: Uri, list: List<String>, i2: Int, str: String) {
    try {
        val audio = trackViewEntity.getAudio()
        val scaleFactor = if (trackViewEntity.entityListAudio.isEmpty() || audio == null) 0.0f else audio.rect.right / trackViewEntity.scaleFactor
        val round = Math.round(trackViewEntity.width * 0.077f)
        val round2 = Math.round(trackViewEntity.second_in_screenNoScale * (i / 1000.0f))
        val f = round2.toFloat()
        val entityAudio = EntityAudio(null, uri, scaleFactor, 0.0f, round.toFloat(), f + scaleFactor, f, trackViewEntity.second_in_screenNoScale, i)
        entityAudio.mediaPlayer = mPlayer
        entityAudio.setPathHttp(list)
        entityAudio.effectAudio.end = entityAudio.end
        entityAudio.effectAudio.start = entityAudio.start
        entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
        trackViewEntity.addAudio(entityAudio)
        if (round2 > 0 && round > 0) {
            executor.execute {
                changeEntityAudioLambda(uri, round, round2, str, entityAudio, i2)
            }
            trackViewEntity.invalidate()
        } else {
            // BEFORE: Missing else — spinner stayed forever when audio duration was 0
            // WHY_CHANGED: If round2 or round is 0, the audio lambda never runs and never hides progress
            // FIXED_BY: Add else clause to hide progress and fragment
            // VISUAL_IMPACT: Spinner always stops, even when audio has zero duration
            hideProgressFragment()
            hideFragment()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
        hideFragment()
    }
}

fun EngineActivity.changeEntityAudioLambda(uri: Uri, i: Int, i2: Int, str: String, entityAudio: EntityAudio, i3: Int) {
    try {
        val copyFromUri = AudioUtils.copyFromUri(this@changeEntityAudioLambda, uri, mTemplate!!.folder_template!!)!!
        val f = i.toFloat()
        entityAudio.setAmps(PCMWaveformExtractor.extractWaveform(str, i2 / ((0.1f * f).toInt() + (f * 0.07f).toInt())))
        entityAudio.pathFfmpeg = copyFromUri
        if (i3 != -1) {
            val i4 = i3 + 1
            if (i4 >= mTemplate!!.entityMediaList.size) {
                try {
                    runOnUiThread {
                        updateTimeToEndAya()
                        updateBtnToEnd()
                        updateBtnToStart()
                        hideProgressFragment()
                        hideFragment()
                    }
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                    hideProgressFragment()
                    hideFragment()
                    return
                }
            }
            val entityMedia = mTemplate!!.entityMediaList[i3]
            val entityMedia2 = mTemplate!!.entityMediaList[i4]
            if (entityMedia2.video_path != null) {
                entityMedia.video_path = AudioUtils.copyFromUri(this@changeEntityAudioLambda, Uri.parse(mTemplate!!.uri_upload_extract_audio_video), mTemplate!!.folder_template!!)
                if (mTemplate!!.extension != null) {
                    addAudioFromVideoWithExtention(mTemplate!!.extension!!, entityMedia.video_path!!, i4)
                    return
                } else {
                    start_extenstion = 0
                    extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, i4)
                    return
                }
            }
            if (entityMedia2.paths_https != null) {
                addAudioRecitersTemplate(entityMedia2.paths_https!!, i4, "")
                return
            } else {
                addAudioTemplateHttp(Uri.parse(entityMedia2.uri), i4, null)
                return
            }
        }
        try {
            runOnUiThread {
                trackViewEntity.calculMaxTime()
                updateViewTime(trackViewEntity.maxTime, trackViewEntity.current_cursur_position)
                trackViewEntity.translateToEnd()
                updateTimeToEndAya()
                updateBtnToEnd()
                updateBtnToStart()
                trackViewEntity.invalidate()
                hideProgressFragment()
                hideFragment()
            }
            return
        } catch (e2: Exception) {
            e2.printStackTrace()
            hideProgressFragment()
            hideFragment()
            return
        }
    } catch (e3: Exception) {
        e3.printStackTrace()
        hideProgressFragment()
        hideFragment()
    }
}

fun EngineActivity.changeEntityAudioFromVideo(i: Int, uri: Uri, str: String) {
    try {
        val audio = trackViewEntity.getAudio()
        val scaleFactor = if (trackViewEntity.entityListAudio.isEmpty() || audio == null) 0.0f else audio.rect.right / trackViewEntity.scaleFactor
        val round = Math.round(trackViewEntity.width * 0.077f)
        val round2 = Math.round(trackViewEntity.second_in_screenNoScale * (i / 1000.0f))
        val f = round2.toFloat()
        val entityAudio = EntityAudio(null, uri, scaleFactor, 0.0f, round.toFloat(), f + scaleFactor, f, trackViewEntity.second_in_screenNoScale, i)
        entityAudio.mediaPlayer = mPlayer
        entityAudio.effectAudio.end = entityAudio.end
        entityAudio.effectAudio.start = entityAudio.start
        entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
        trackViewEntity.addAudio(entityAudio)
        if (round2 > 0 && round > 0) {
            val copyFromUri = AudioUtils.copyFromUri(this@changeEntityAudioFromVideo, uri, mTemplate!!.folder_template!!)!!
            val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_output.pcm")
            val arrayList = ArrayList<String>()
            arrayList.add("-i")
            arrayList.add(copyFromUri)
            arrayList.add("-map")
            arrayList.add("0:a")
            arrayList.add("-ac")
            arrayList.add("1")
            arrayList.add("-ar")
            arrayList.add("44100")
            arrayList.add("-f")
            arrayList.add("s16le")
            arrayList.add(file.absolutePath)
            arrayList.add("-y")
            id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayList.toTypedArray(), object : FFmpegSessionCompleteCallback {
                override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
                    if (fFmpegSession.returnCode.isValueSuccess()) {
                        try {
                            entityAudio.setAmps(PCMWaveformExtractor.extractWaveform(file.absolutePath, round2 / ((round * 0.1f).toInt() + (round * 0.07f).toInt())))
                            entityAudio.pathFfmpeg = uri.path
                            entityAudio.videoPath = str
                            runOnUiThread {
                                trackViewEntity.invalidate()
                                hideProgressFragment()
                                hideFragment()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread {
                                hideProgressFragment()
                                hideFragment()
                            }
                        }
                    } else {
                        runOnUiThread {
                            hideProgressFragment()
                            hideFragment()
                        }
                    }
                }
            }).sessionId)
            trackViewEntity.invalidate()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        runOnUiThread {
            hideProgressFragment()
            hideFragment()
        }
    }
}

fun EngineActivity.changeEntityAudioReciters(i: Int, uri: Uri, mediaPlayer: MediaPlayer, list: List<RecitersModel>, i2: Int) {
    try {
        val audio = trackViewEntity.getAudio()
        val scaleFactor = if (trackViewEntity.entityListAudio.isEmpty() || audio == null) 0.0f else audio.rect.right / trackViewEntity.scaleFactor
        val round = Math.round(trackViewEntity.width * 0.077f)
        val round2 = Math.round(trackViewEntity.second_in_screenNoScale * (i / 1000.0f))
        val f = round2.toFloat()
        val entityAudio = EntityAudio(null, uri, scaleFactor, 0.0f, round.toFloat(), f + scaleFactor, f, trackViewEntity.second_in_screenNoScale, i)
        entityAudio.effectAudio.end = entityAudio.end
        entityAudio.effectAudio.start = entityAudio.start
        entityAudio.effectAudio.duration = (entityAudio.end - entityAudio.start).toInt()
        entityAudio.mediaPlayer = mediaPlayer
        trackViewEntity.addAudio(entityAudio)
        if (round2 > 0 && round > 0) {
            AudioUtils.copyToLocalAsync(this@changeEntityAudioReciters, uri.toString(), mTemplate!!.folder_template!!, object : AudioUtils.Callback {
                override fun onSuccess(str: String) {
                    try {
                        val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_audio_wave.png")
                        id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayOf("-i", str, "-filter_complex", "aformat=channel_layouts=mono,showwavespic=s=${round}x${round2}:colors=#522123", "-frames:v", "1", "-y", file.absolutePath), object : FFmpegSessionCompleteCallback {
                            override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
                                if (fFmpegSession.returnCode.isValueSuccess()) {
                                    try {
                                        com.bumptech.glide.Glide.with(this@changeEntityAudioReciters as androidx.fragment.app.FragmentActivity).asBitmap().load(Uri.fromFile(file)).submit().get()
                                        entityAudio.pathFfmpeg = str
                                        runOnUiThread { trackViewEntity.invalidate() }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        hideProgressFragment()
                                    }
                                }
                                addAudioReciters(list, i2 + 1)
                            }
                        }).sessionId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        hideProgressFragment()
                    }
                }

                override fun onError(exc: Exception) {
                    exc.printStackTrace()
                    hideProgressFragment()
                }
            })
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
    }
}

fun EngineActivity.addAudioTemplateHttp(uri: Uri?, i: Int, str: String?) {
    try {
        if (isDestroyed) return
        if (uri == null) {
            hideProgressFragment()
            return
        }
        if (mTemplate!!.entityMediaList != null) {
            updateProgress(i + 1, mTemplate!!.entityMediaList.size)
        }
        val uri2 = if (str != null) {
            uri.path
        } else if (!uri.toString().contains("share_with_me")) {
            AudioUtils.copyFromUri(this@addAudioTemplateHttp, uri, mTemplate!!.folder_template!!)!!
        } else {
            uri.toString()
        }
        val str2 = uri2 ?: return
        val entityMedia = mTemplate!!.entityMediaList[i]

        if (entityMedia.isApplyEffectInPreview) {
            val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_audio_echo.mp3")
            val effectAudio = entityMedia.effectAudio
            val start = effectAudio!!.start / 1000.0f
            val end = effectAudio!!.end / 1000.0f
            val arrayList = ArrayList<String>()
            arrayList.add("atrim=start=$start:end=$end")
            arrayList.add("asetpts=N/SR/TB")
            if (effectAudio!!.isRemoveNoice) {
                arrayList.add("afftdn=nf=-25")
            }
            arrayList.add(String.format(Locale.US, "volume=%.2f", effectAudio!!.volume))
            if (effectAudio!!.fade_in > 0) {
                arrayList.add("afade=t=in:st=0:d=${effectAudio!!.fade_in}")
            }
            if (effectAudio!!.fade_out > 0) {
                val fade_out = effectAudio!!.fade_out
                arrayList.add("afade=t=out:st=${(end - start) - fade_out}:d=$fade_out")
            }
            if (effectAudio!!.isEnhance) {
                arrayList.add(Common.ENHANCE_CMD)
            }
            if (effectAudio!!.reverbPreset != null) {
                arrayList.add(effectAudio!!.reverbPreset!!)
            }
            if (effectAudio!!.decays > 0) {
                arrayList.add(String.format(Locale.US, "aecho=%.2f:%.2f:%s:%s", 1.0f, effectAudio!!.outGain, effectAudio!!.delays_cmd, effectAudio!!.decays_cmd))
            }
            if (effectAudio!!.speed != 1.0f) {
                arrayList.addAll(buildSpeedFilters(effectAudio!!.speed))
            }
            id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayOf("-i", str2, "-af", TextUtils.join(",", arrayList), "-y", file.absolutePath), object : FFmpegSessionCompleteCallback {
                override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
                    try {
                        mPlayer = MediaPlayer()
                        mPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        val fromFile = Uri.fromFile(file)
                        if (fromFile.scheme != null && fromFile.scheme!!.startsWith("http")) {
                            mPlayer!!.setDataSource(fromFile.toString())
                        } else {
                            mPlayer!!.setDataSource(this@addAudioTemplateHttp, fromFile)
                        }
                        mPlayer!!.prepareAsync()
                        mPlayer!!.setOnPreparedListener { mediaPlayer ->
                            if (mediaPlayer == null) return@setOnPreparedListener
                            try {
                                addEntitMediaHttp(entityMedia, effectAudio!!.duration, uri, mediaPlayer, entityMedia.paths_https!!, i, str2, str)
                            } catch (unused: Exception) {
                                hideProgressFragment()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }).sessionId)
            return
        }

        val mediaPlayer = MediaPlayer()
        mPlayer = mediaPlayer
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
            mPlayer!!.setDataSource(uri.toString())
        } else {
            mPlayer!!.setDataSource(this@addAudioTemplateHttp, uri)
        }
        mPlayer!!.prepareAsync()
        mPlayer!!.setOnPreparedListener { mediaPlayer2 ->
            if (mediaPlayer2 == null) return@setOnPreparedListener
            try {
                addEntitMediaHttp(entityMedia, mediaPlayer2.duration, uri, mediaPlayer2, entityMedia.paths_https!!, i, str2, str)
            } catch (unused: Exception) {
                hideProgressFragment()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
    }
}

fun EngineActivity.addEntitMediaHttp(entityMedia: EntityMedia, i: Int, uri: Uri, mediaPlayer: MediaPlayer, list: List<String>, i2: Int, str: String, str2: String?) {
    val round = Math.round(trackViewEntity.width * 0.077f)
    val round2 = Math.round(trackViewEntity.second_in_screenNoScale * (i / 1000.0f))

    val entityAudio: EntityAudio? = if (entityMedia.start != entityMedia.end) {
        val posX = if (mTemplate!!.isNewCode) entityMedia.posX else (entityMedia.posX / 1000.0f) * trackViewEntity.second_in_screen
        val posY = if (mTemplate!!.isNewCode) entityMedia.posY else (entityMedia.posY / 1000.0f) * trackViewEntity.second_in_screen
        EntityAudio(null, uri, posX, 0.0f, round.toFloat(), posY, entityMedia.max, trackViewEntity.second_in_screenNoScale, i, entityMedia.offset, entityMedia.offset_right, entityMedia.offset_left).also { ea ->
            ea.setPathHttp(list)
            ea.mediaPlayer = mediaPlayer
            ea.videoPath = str2
            ea.start = entityMedia.start
            ea.minDuration = entityMedia.start_original
            if (entityMedia.end != 0.0f) {
                ea.end = entityMedia.end
            }
            ea.effectAudio = entityMedia.effectAudio!!
            ea.setFadeIn(entityMedia.duration_fade_in)
            ea.setFadeOut(entityMedia.duration_fade_out)
            trackViewEntity.addAudio(ea)
        }
    } else null

    if (round2 <= 0 || round <= 0) {
        trackViewEntity.invalidate()
        hideProgressFragment()
        return
    }
    try {
        val file = File(mTemplate!!.folder_template, System.currentTimeMillis().toString() + "_output.pcm")
        val arrayList = ArrayList<String>()
        arrayList.add("-i")
        arrayList.add(str)
        arrayList.add("-map")
        arrayList.add("0:a")
        arrayList.add("-ac")
        arrayList.add("1")
        arrayList.add("-ar")
        arrayList.add("44100")
        arrayList.add("-f")
        arrayList.add("s16le")
        arrayList.add(file.absolutePath)
        arrayList.add("-y")
        id_ffmpeg.add(FFmpegKit.executeWithArgumentsAsync(arrayList.toTypedArray(), object : FFmpegSessionCompleteCallback {
            override fun apply(fFmpegSession: com.arthenica.ffmpegkit.FFmpegSession) {
                if (fFmpegSession.returnCode.isValueSuccess()) {
                    try {
                        entityAudio?.setAmps(PCMWaveformExtractor.extractWaveform(file.absolutePath, round2 / ((round * 0.1f).toInt() + (round * 0.07f).toInt())))
                        entityAudio?.pathFfmpeg = str
                        val i4 = i2 + 1
                        if (i4 >= mTemplate!!.entityMediaList.size) {
                            try {
                                runOnUiThread {
                                    updateTime()
                                    trackViewEntity.invalidate()
                                    hideProgressFragment()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            val entityMedia2 = mTemplate!!.entityMediaList[i4]
                            if (entityMedia2.video_path != null) {
                                entityMedia.video_path = AudioUtils.copyFromUri(this@addEntitMediaHttp, Uri.parse(mTemplate!!.uri_upload_extract_audio_video), mTemplate!!.folder_template!!)
                                if (mTemplate!!.extension != null) {
                                    addAudioFromVideoWithExtention(mTemplate!!.extension!!, entityMedia.video_path!!, i4)
                                } else {
                                    start_extenstion = 0
                                    extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, i4)
                                }
                            } else if (entityMedia2.paths_https != null) {
                                addAudioRecitersTemplate(entityMedia2.paths_https!!, i4, "")
                            } else {
                                addAudioTemplateHttp(Uri.parse(entityMedia2.uri), i4, null)
                            }
                        }
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                        runOnUiThread {
                            hideProgressFragment()
                            hideFragment()
                        }
                    }
                }
            }
        }).sessionId)
    } catch (e: Exception) {
        e.printStackTrace()
        hideProgressFragment()
    }
    trackViewEntity.invalidate()
}

fun EngineActivity.addEntitMediaHttp(entityMedia: EntityMedia, i: Int, uri: Uri, mediaPlayer: MediaPlayer, list: List<String>, i2: Int, str: String, str2: String, str3: String?) {
    val round = Math.round(trackViewEntity.width * 0.077f)
    val round2 = Math.round(trackViewEntity.second_in_screenNoScale * (i / 1000.0f))

    val entityAudioVal: EntityAudio? = if (entityMedia.start != entityMedia.end) {
        val posX = if (mTemplate!!.isNewCode) entityMedia.posX else (entityMedia.posX / 1000.0f) * trackViewEntity.second_in_screen
        val posY = if (mTemplate!!.isNewCode) entityMedia.posY else (entityMedia.posY / 1000.0f) * trackViewEntity.second_in_screen
        EntityAudio(null, uri, posX, 0.0f, round.toFloat(), posY, entityMedia.max, trackViewEntity.second_in_screenNoScale, i, entityMedia.offset, entityMedia.offset_right, entityMedia.offset_left).also { ea ->
            ea.setPathHttp(list)
            ea.mediaPlayer = mediaPlayer
            ea.videoPath = str3
            ea.start = entityMedia.start
            ea.minDuration = entityMedia.start_original
            if (entityMedia.end != 0.0f) {
                ea.end = entityMedia.end
            }
            ea.effectAudio = entityMedia.effectAudio!!
            ea.setFadeIn(entityMedia.duration_fade_in)
            ea.setFadeOut(entityMedia.duration_fade_out)
            trackViewEntity.addAudio(ea)
        }
    } else null

    val entityAudio2 = entityAudioVal
    if (round2 <= 0 || round <= 0) {
        trackViewEntity.invalidate()
        hideProgressFragment()
    } else {
        executor.execute {
            try {
                entityAudio2?.setAmps(PCMWaveformExtractor.extractWaveform(str2, round2 / ((round * 0.1f).toInt() + (round * 0.07f).toInt())))
                entityAudio2?.pathFfmpeg = str
                val i4 = i2 + 1
                if (i4 >= mTemplate!!.entityMediaList.size) {
                    try {
                        runOnUiThread {
                            updateTime()
                            trackViewEntity.invalidate()
                            hideProgressFragment()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val entityMedia2 = mTemplate!!.entityMediaList[i4]
                    if (entityMedia2.video_path != null) {
                        entityMedia.video_path = AudioUtils.copyFromUri(this, Uri.parse(mTemplate!!.uri_upload_extract_audio_video), mTemplate!!.folder_template!!)
                        if (mTemplate!!.extension != null) {
                            addAudioFromVideoWithExtention(mTemplate!!.extension!!, entityMedia.video_path!!, i4)
                        } else {
                            start_extenstion = 0
                            extractAudioFromVideoRecursive(entityMedia.video_path!!, 0, true, i4)
                        }
                    } else if (entityMedia2.paths_https != null) {
                        addAudioRecitersTemplate(entityMedia2.paths_https!!, i4, "")
                    } else {
                        addAudioTemplateHttp(Uri.parse(entityMedia2.uri), i4, null)
                    }
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
                runOnUiThread {
                    hideProgressFragment()
                    hideFragment()
                }
            }
        }
    }
}

fun EngineActivity.addAudioTemplate(uri: Uri, list: List<String>, i: Int, str: String, str2: String, str3: String) {
    try {
        val mediaPlayer = MediaPlayer()
        mPlayer = mediaPlayer
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        if (uri.scheme != null && uri.scheme!!.startsWith("http")) {
            mPlayer!!.setDataSource(uri.toString())
        } else {
            mPlayer!!.setDataSource(this, uri)
        }
        mPlayer!!.prepareAsync()
        mPlayer!!.setOnPreparedListener { mediaPlayer2 ->
            if (mediaPlayer2 != null && i < mTemplate!!.entityMediaList.size) {
                addEntitMediaHttp(mTemplate!!.entityMediaList[i], mediaPlayer2.duration, uri, mPlayer!!, list, i, str, str2, str3)
            }
        }
    } catch (e: Exception) {
        hideProgressFragment()
        hideFragment()
        e.printStackTrace()
    }
}

fun EngineActivity.addAudioFromVideoWithExtention(str: String, str2: String, i: Int) {
    try {
        val file = File(File(mTemplate!!.folder_template!!), "${System.currentTimeMillis()}_audio$str")
        FFmpegKit.executeWithArgumentsAsync(
            arrayOf("-i", str2, "-vn", "-acodec", "copy", "-y", file.absolutePath)
        ) { fFmpegSession ->
            if (fFmpegSession.returnCode.isValueSuccess()) {
                addAudioTemplateHttp(Uri.fromFile(file), i, str2)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun EngineActivity.extractAudioFromVideoRecursive(str: String, i: Int, z: Boolean, i2: Int) {
    if (isDestroyed) {
        return
    }
    if (i < extentions.size) {
        try {
            val file = File(File(mTemplate!!.folder_template!!), "${System.currentTimeMillis()}_audio${extentions[i]}")
            FFmpegKit.executeWithArgumentsAsync(
                arrayOf("-i", str, "-vn", "-acodec", "copy", "-y", file.absolutePath)
            ) { fFmpegSession ->
                if (fFmpegSession.returnCode.isValueSuccess()) {
                    mTemplate!!.extension = extentions[i]
                    val fromFile = Uri.fromFile(file)
                    if (!z) {
                        runOnUiThread {
                            hideFragment()
                            hideProgressFragment()
                        }
                        addUriAudioToQuranFragment(fromFile, str, 0)
                    } else {
                        addAudioTemplateHttp(fromFile, i2, str)
                    }
                    return@executeWithArgumentsAsync
                }
                start_extenstion++
                extractAudioFromVideoRecursive(str, start_extenstion, z, i)
            }
            return
        } catch (e: Exception) {
            e.printStackTrace()
            extractAudioFromVideo(str, z)
            return
        }
    }
    extractAudioFromVideo(str, z)
}

fun EngineActivity.extractAudioFromVideo(str: String, z: Boolean) {
    try {
        val file = File(File(mTemplate!!.folder_template!!), "${System.currentTimeMillis()}_audio.mp3")
        FFmpegKit.executeWithArgumentsAsync(
            arrayOf("-i", str, "-vn", "-acodec", "copy", "-y", file.absolutePath)
        ) { fFmpegSession ->
            if (fFmpegSession == null) {
                runOnUiThread {
                    hideFragment()
                    hideProgressFragment()
                }
                return@executeWithArgumentsAsync
            }
            if (fFmpegSession.returnCode.isValueSuccess) {
                val fromFile = Uri.fromFile(file)
                mTemplate!!.extension = ".mp3"
                if (!z) {
                    addUriAudioToQuranFragment(fromFile, str, 0)
                } else {
                    addAudioTemplateHttp(fromFile, 0, str)
                }
                return@executeWithArgumentsAsync
            }
            runOnUiThread {
                hideProgressFragment()
                hideFragment()
                Toast.makeText(
                    this,
                    mResources!!.getString(R.string.video_not_have_sound),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        runOnUiThread {
            hideFragment()
            hideProgressFragment()
        }
    }
}

fun EngineActivity.updateProgress(i: Int, i2: Int) {
    runOnUiThread {
        val frag = ProgressViewFragment.getInstance()
        if (frag != null) {
            frag.update(i, i2)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
fun EngineActivity.addAudioRecitersTemplateRunnable(
    pathes: List<String>, valIndex: Int, valPathVideo: String
): Runnable = Runnable {
    try {
        val arrayList2 = ArrayList<String>()
        val sb = StringBuilder()
        val it = pathes.iterator()
        var i = 0
        while (it.hasNext()) {
            val parse = Uri.parse(it.next())
            val uri = parse.toString()
            // BUG-E13 fix: the original code had inverted null checks:
            //   downloadFile = ...!!          // throws NPE on failure
            //   if (downloadFile == null) { sb.append(... downloadFile!!.replace(...)) }  // NPE inside null check!
            // The success case (write to concat list) was never executed, so
            // concat.txt was ALWAYS empty → FFmpeg concat demuxer fails silently.
            // Also `downloadFile(...)` returns String? — `!!` throws NPE on
            // network failure. Corrected below to nullable + != null branch.
            var downloadFile: String? = null
            if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
                downloadFile = AudioUtils.copyFromUri(this, parse, mTemplate!!.folder_template!!)
            } else {
                downloadFile = AudioUtils.downloadFile(this, uri, mTemplate!!.folder_template!!)
            }
            if (downloadFile != null) {
                sb.append("file '").append(downloadFile.replace("'", "\\'")).append("'\n")
            }
            i++
            updateProgress(i, pathes.size)
        }
        val file = File(mTemplate!!.folder_template, "concat.txt")
        val fileOutputStream = java.io.FileOutputStream(file)
        fileOutputStream.write(sb.toString().toByteArray())
        fileOutputStream.close()
        val file2 = File(mTemplate!!.folder_template, "${System.currentTimeMillis()}_output.mp3")
        val file3 = File(mTemplate!!.folder_template, "${System.currentTimeMillis()}_output.pcm")
        val arrayList = ArrayList<String>()
        arrayList.add("-f"); arrayList.add("concat"); arrayList.add("-safe"); arrayList.add("0")
        arrayList.add("-i"); arrayList.add(file.absolutePath)
        arrayList.add("-map"); arrayList.add("0:a"); arrayList.add("-c"); arrayList.add("copy"); arrayList.add(file2.absolutePath)
        arrayList.add("-map"); arrayList.add("0:a"); arrayList.add("-ac"); arrayList.add("1"); arrayList.add("-ar"); arrayList.add("44100")
        arrayList.add("-f"); arrayList.add("s16le"); arrayList.add(file3.absolutePath); arrayList.add("-y")
        id_ffmpeg.add(
            FFmpegKit.executeWithArgumentsAsync(arrayList.toTypedArray()) { fFmpegSession ->
                // BUG-E11 fix: guard against callbacks firing after activity destroy.
                if (isDestroyed || isFinishing) return@executeWithArgumentsAsync
                if (fFmpegSession.returnCode.isValueSuccess()) {
                    if (valIndex >= 0 && valIndex < mTemplate!!.entityMediaList.size) {
                        val entityMedia = mTemplate!!.entityMediaList[valIndex]
                        if (entityMedia.isApplyEffectInPreview) {
                            val file4 = File(mTemplate!!.folder_template, "${System.currentTimeMillis()}_audio_echo.mp3")
                            val effectAudio = entityMedia.effectAudio
                            val start = effectAudio!!.start / 1000.0f
                            val end = effectAudio!!.end / 1000.0f
                            val arrayList2Inner = ArrayList<String>()
                            arrayList2Inner.add("atrim=start=$start:end=$end")
                            arrayList2Inner.add("asetpts=N/SR/TB")
                            if (effectAudio!!.isRemoveNoice) arrayList2Inner.add("afftdn=nf=-25")
                            arrayList2Inner.add(String.format(Locale.US, "volume=%.2f", effectAudio!!.volume))
                            if (effectAudio!!.fade_in > 0) arrayList2Inner.add("afade=t=in:st=0:d=${effectAudio!!.fade_in}")
                            if (effectAudio!!.fade_out > 0) {
                                val fadeOut = effectAudio!!.fade_out
                                arrayList2Inner.add("afade=t=out:st=${(end - start) - fadeOut}:d=$fadeOut")
                            }
                            if (effectAudio!!.isEnhance) arrayList2Inner.add(Common.ENHANCE_CMD)
                            if (effectAudio!!.reverbPreset != null) arrayList2Inner.add(effectAudio!!.reverbPreset!!)
                            if (effectAudio!!.decays > 0) arrayList2Inner.add(String.format(Locale.US, "aecho=%.2f:%.2f:%s:%s", 1.0f, effectAudio!!.outGain, effectAudio!!.delays_cmd, effectAudio!!.decays_cmd))
                            if (effectAudio!!.speed != 1.0f) arrayList2Inner.addAll(buildSpeedFilters(effectAudio!!.speed))
                            id_ffmpeg.add(
                                FFmpegKit.executeWithArgumentsAsync(arrayOf("-i", file2.absolutePath, "-af", TextUtils.join(",", arrayList2Inner), "-y", file4.absolutePath)) { _ ->
                                    if (isDestroyed || isFinishing) return@executeWithArgumentsAsync
                                    addAudioTemplate(Uri.fromFile(file4), pathes, valIndex, file2.absolutePath, file3.absolutePath, valPathVideo ?: "")
                                }.sessionId
                            )
                            return@executeWithArgumentsAsync
                        }
                    }
                    addAudioTemplate(Uri.fromFile(file2), pathes, valIndex, file2.absolutePath, file3.absolutePath, valPathVideo ?: "")
                } else {
                    // BUG-E21 fix: hide spinner + notify on concat failure
                    // (previously no `else` branch → infinite spinner when concat.txt empty)
                    runOnUiThread {
                        hideProgressFragment()
                        hideFragment()
                        android.widget.Toast.makeText(
                            applicationContext,
                            "Audio merge failed",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }.sessionId
        )
    } catch (e: Exception) {
        hideProgressFragment()
        hideFragment()
        e.printStackTrace()
    }
}

// =====================================================================
// pickVideoForAudio / videoChooser / imageChooser / toChoiceBgFromVideo
// =====================================================================

fun EngineActivity.pickVideoForAudio() {
    if (Build.VERSION.SDK_INT >= 34) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") != 0 &&
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") != 0
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"),
                12
            )
            return
        }
    } else if (Build.VERSION.SDK_INT >= 33) {
        if (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") != 0) {
            ActivityCompat.requestPermissions(
                this, arrayOf("android.permission.READ_MEDIA_VIDEO"), 12
            )
            return
        }
    } else if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") != 0) {
        ActivityCompat.requestPermissions(
            this, arrayOf("android.permission.READ_EXTERNAL_STORAGE"), 12
        )
        return
    }
    videoChooserForAudio()
}

fun EngineActivity.videoChooserForAudio() {
    isToCrop = true
    launchVideoExtract!!.launch(Intent(this, GalleryPickerVideo::class.java))
}

fun EngineActivity.videoChooser() {
    launchVideo!!.launch(Intent(this, GalleryPickerVideo::class.java))
}

fun EngineActivity.imageChooser() {
    launchImg!!.launch(Intent(this, GalleryPickerOneImage::class.java))
}

fun EngineActivity.toChoiceBgFromVideo(uri: Uri) {
    val intent = Intent(this, ChoiceBgFromVideoActivity::class.java)
    intent.data = uri
    launchChoiceBgActivity!!.launch(intent)
}

// =====================================================================
// onRequestPermissionsResult helper (call from the override in EngineActivity)
// =====================================================================

fun EngineActivity.handleRequestPermissionsResult(i: Int, strArr: Array<String>, iArr: IntArray) {
    if (i == 1) {
        if (iArr.isNotEmpty() && iArr[0] == 0) {
            save()
        } else {
            Toast.makeText(this, mResources!!.getString(R.string.permission_img), Toast.LENGTH_SHORT).show()
        }
    }
    if (i == 2) {
        if (iArr.isNotEmpty() && iArr[0] == 0) {
            pickAudio()
        } else {
            Toast.makeText(this, mResources!!.getString(R.string.permission_audio), Toast.LENGTH_SHORT).show()
        }
    }
    if (i == 10) {
        if ((Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == 0) || (iArr.isNotEmpty() && iArr[0] == 0)) {
            imageChooser()
        } else {
            Toast.makeText(this, mResources!!.getString(R.string.permission_img), Toast.LENGTH_SHORT).show()
        }
    }
    if (i == 11) {
        if ((Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == 0) || (iArr.isNotEmpty() && iArr[0] == 0)) {
            videoChooser()
        } else {
            Toast.makeText(this, mResources!!.getString(R.string.permission_video), Toast.LENGTH_SHORT).show()
        }
    }
    if (i == 12) {
        if ((Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == 0) || (iArr.isNotEmpty() && iArr[0] == 0)) {
            videoChooserForAudio()
        } else {
            Toast.makeText(this, mResources!!.getString(R.string.permission_video), Toast.LENGTH_SHORT).show()
        }
    }
}
