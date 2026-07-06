package hazem.nurmontage.videoquran.entity_timeline

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.media.MediaPlayer
import android.net.Uri
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.utils.waveform.WaveformBitmapRenderer
import hazem.nurmontage.videoquran.views.TrackEntityView

class EntityAudio : Entity {

    internal var amps: FloatArray? = null
    internal var downX: Float = 0f
    internal var duration: Int = 0
    var effectAudio: EffectAudio = EffectAudio()
        set(value) {
            field.reverbPreset = value.reverbPreset
            field.speed = value.speed
            field.volume = value.volume
            field.fade_in = value.fade_in
            field.fade_out = value.fade_out
            field.decays = value.decays
            field.isRemoveNoice = value.isRemoveNoice
            field.delays_cmd = value.delays_cmd
            field.delays = value.delays
            field.decays_cmd = value.decays_cmd
            field.outGain = value.outGain
            field.volume_echo = value.volume_echo
            field.isEnhance = value.isEnhance
            field.reverbPreset_index_list = value.reverbPreset_index_list
        }
    internal var h: Float = 0f
    internal var iTrimLineCallback: TrackEntityView.ITrimLineCallback? = null
    internal var isApplyEffectInPreview: Boolean = false
    internal var isPlay: Boolean = false
    internal var isStartFadeIn: Boolean = false
    internal var isStartFadeOut: Boolean = false
    internal var lastLeft: Float = 0f
    internal var lastRight: Float = 0f
    internal var mediaPlayer: MediaPlayer? = null
    internal var minDuration: Int = 0
    internal var objectAnimator: ObjectAnimator? = null
    internal var paintLine: Paint? = null
    internal var paintPath: Paint? = null
    internal var path: Path? = null
    internal var pathFfmpeg: String? = null
    internal var pathFfmpegEffect: String? = null
    internal var pathsHttp: MutableList<String>? = null
    internal var renderer: WaveformBitmapRenderer? = null
    internal var scaleEffect: Float = 0f
    internal var tmpOffset: Float = 0f
    internal var uri: Uri? = null
    internal var videoPath: String? = null
    var waveformValues: ByteArray? = null

    override var secondInScreen: Float
        get() = super.secondInScreen * scaleFactor
        set(value) { super.secondInScreen = value }

    override var right: Float
        get() = super.right
        set(value) {
            super.right = value
            rect.right = value
        }

    constructor(
        bitmap: Bitmap?,
        uri: Uri?,
        left: Float,
        top: Float,
        h: Float,
        right: Float,
        max: Float,
        secondInScreen: Float,
        durationSec: Int,
        offset: Float,
        offsetRight: Float,
        offsetLeft: Float
    ) : super(secondInScreen) {
        this.effectAudio = EffectAudio()
        setOffsetRight(offsetRight)
        this.offset = offset
        setOffsetLeft(offsetLeft)
        this.duration = durationSec * 1000
        this.end = durationSec.toFloat()
        this.secondInScreen = secondInScreen
        isVisible = true
        this.uri = uri
        this.max = max
        this.h = h
        this.rect = RectF(left, top, right, h)
        this.left = rect.left
        this.right = rect.right
        this.color = Common.COLOR_BLOCK_AUDIO
        initPaints(h)
    }

    constructor(
        bitmap: Bitmap?,
        uri: Uri?,
        left: Float,
        top: Float,
        h: Float,
        right: Float,
        max: Float,
        secondInScreen: Float,
        durationSec: Int
    ) : super(secondInScreen) {
        this.effectAudio = EffectAudio()
        setOffsetRight(0f)
        this.offset = 0f
        this.duration = durationSec * 1000
        this.end = durationSec.toFloat()
        this.secondInScreen = secondInScreen
        isVisible = true
        this.uri = uri
        this.max = max
        this.h = h
        this.rect = RectF(left, top, right, h)
        this.left = rect.left
        this.right = rect.right
        this.color = Common.COLOR_BLOCK_AUDIO
        initPaints(h)
    }

    private fun initPaints(height: Float) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -2434342
            style = Paint.Style.STROKE
            strokeWidth = 0.01f * height
        }
        this.paintLine = linePaint

        val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = -1236326096
            style = Paint.Style.FILL
        }
        this.paintPath = pathPaint
        this.path = Path()

        this.rectFLeft = RectF(0f, 0f, 0.46f * height, height)
        this.rectFRight = RectF(0f, 0f, rectFLeft.width(), height)
        this.round = rectFRight.width() * 0.5f
        this.padding = height * 0.07f
    }

    fun updateEffect() {
        effectAudio.start = start
        effectAudio.end = end
        effectAudio.duration = (end - start).toInt()
    }

    fun setPathFfmpeg(path: String?) {
        this.pathFfmpeg = path
        this.pathFfmpegEffect = path
    }

    fun getPathFfmpeg(): String? = pathFfmpeg

    fun getPathFfmpegEffect(): String? = pathFfmpegEffect
    fun setPathFfmpegEffect(path: String?) { this.pathFfmpegEffect = path }

    fun addPathHttp(paths: List<String>?) {
        if (paths == null) return
        if (pathsHttp == null) pathsHttp = mutableListOf()
        pathsHttp!!.addAll(paths)
    }

    fun setPathHttp(paths: List<String>?) { this.pathsHttp = paths?.toMutableList() }
    fun getPathsHttp(): List<String>? = pathsHttp

    fun setApplyEffectInPreview(apply: Boolean) { isApplyEffectInPreview = apply }
    fun isApplyEffectInPreview(): Boolean = isApplyEffectInPreview

    fun setScaleEffect(scale: Float) { scaleEffect = scale }
    fun getScaleEffect(): Float = scaleEffect

    fun getDuration(): Int = duration
    fun setDuration(dur: Int) { this.duration = dur }

    fun getMinDuration(): Int = minDuration
    fun setMinDuration(min: Int) { this.minDuration = min }

    fun setVideoPath(path: String?) { this.videoPath = path }
    fun getVideoPath(): String? = videoPath

    fun getUri(): Uri? = uri

    fun isPlay(): Boolean = isPlay
    fun setPlay(play: Boolean) { this.isPlay = play }

    fun getAmps(): FloatArray? = amps
    fun getRenderer(): WaveformBitmapRenderer? = renderer

    fun setRenderer(r: WaveformBitmapRenderer?) { this.renderer = r }

    fun setAmps(a: FloatArray?) { this.amps = a }

    fun setAmps(a: FloatArray?, width: Int, height: Int) {
        this.amps = a
        this.renderer = WaveformBitmapRenderer(a, width, height, Common.COLOR_WAVE_INT)
    }

    fun isStartFadeIn(): Boolean = isStartFadeIn
    fun isStartFadeOut(): Boolean = isStartFadeOut
    fun setStartFadeIn(start: Boolean) { isStartFadeIn = start }
    fun setStartFadeOut(start: Boolean) { isStartFadeOut = start }

    fun setITrimLineCallback(callback: TrackEntityView.ITrimLineCallback?) {
        this.iTrimLineCallback = callback
    }

    fun setFadeInDelta(delta: Float) {
        iTrimLineCallback?.fadeInAudio(delta)
    }

    fun startFadeIn() {
        objectAnimator?.end()
        val fadeDuration = getFadeIn() * 1000f
        objectAnimator = ObjectAnimator.ofFloat(this, "FadeInDelta", 0f, 1f).apply {
            duration = fadeDuration.toLong()
            start()
        }
    }

    fun setFadeOutDelta(delta: Float) {
        iTrimLineCallback?.fadeOutAudio(delta)
    }

    fun startFadeOut() {
        objectAnimator?.end()
        val fadeDuration = getFadeOut() * 1000f
        objectAnimator = ObjectAnimator.ofFloat(this, "FadeOutDelta", 1f, 0f).apply {
            duration = fadeDuration.toLong()
            start()
        }
    }

    override fun getH(): Float = h

    override fun setLastLeft(ll: Float) { lastLeft = ll }
    override fun setLastRight(lr: Float) { lastRight = lr }

    override fun setX(x: Float) {
        val clamped = if (x < 0f) 0f else x
        left = clamped
        rect.left = clamped
    }

    override fun setY(y: Float) {
        rect.top = y
        rect.bottom = h + rect.top
    }

    override fun setDownX(downX: Float) { this.downX = downX }
    override fun getDownX(): Float = downX

    override fun onUpRight() {
        val round = (Math.round(rect.right / secondInScreen) * 1000).toFloat() - getOnTapTime()
        setOffsetRight(
            ((rect.left / scaleFactor) - getOffsetLeft()) + max - (rect.right / scaleFactor)
        )
        end += round
        if (end > duration) end = duration.toFloat()
        right = lastRight
    }

    override fun updateStartTrim() {
        tmpOffset = Math.abs(rect.left / scaleFactor) - Math.abs(getOnDown() / scaleFactor)
    }

    override fun onUpLeft() {
        start = Math.round(
            (Math.abs(Math.round((rect.left / secondInScreen) * 1000f)) - getOnTapTime()).toFloat() + start
        ).toFloat()
        setOffsetLeft(getOffsetLeft() + tmpOffset)
        tmpOffset = 0f
        if (start < minDuration) start = minDuration.toFloat()
        left = lastLeft
    }

    override fun onTouch(point: PointF): Boolean {
        selectTrim = null
        downX = point.x
        trimType = -1
        if (rectFLeft.contains(point.x, point.y)) {
            selectTrim = rectFLeft
            trimType = 0
            isSelect = true
        } else if (rectFRight.contains(point.x, point.y)) {
            selectTrim = rectFRight
            trimType = 1
            isSelect = true
        }
        return true
    }

    override fun contains(point: PointF): Boolean {
        if (isSelect) onTouch(point)
        isSelect = rect.contains(point.x, point.y)
        return isSelect
    }

    fun split(cursorX: Float): EntityAudio {
        return EntityAudio(
            null, uri, cursorX, rect.top, h, rect.right,
            ((rect.right / scaleFactor) + getOffsetRight()) - (cursorX / scaleFactor),
            secondInScreen, (duration / 1000), 0f, 0f, 0f
        ).also { split ->
            split.setFadeOut(getFadeOut())
            split.setFadeIn(getFadeIn())
            split.rect.bottom = rect.bottom
        }
    }

    private fun drawWave(canvas: Canvas, rect: RectF) {
        if (amps == null || renderer == null) return
        val offset = offset + getOffsetLeft() + tmpOffset
        renderer!!.draw(canvas, rect, scaleFactor + scaleEffect, offset)
    }

    override fun draw(canvas: Canvas, w: Int, h: Int) {
        try { drawWave(canvas, rect) } catch (_: Exception) {}
    }

    override fun draw(canvas: Canvas) {
        try { drawWave(canvas, rect) } catch (_: Exception) {}
    }

    override fun release() {
        super.release()
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.pause()
                mp.release()
                mediaPlayer = null
            }
            renderer?.release()
        } catch (_: Exception) {}
    }
}
