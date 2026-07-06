package hazem.nurmontage.videoquran.fragment.audio_effect

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.constant.EffectAudioType
import hazem.nurmontage.videoquran.databinding.FragmentEchoEffectBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.util.Locale

class EchoEffectFragment : Fragment {

    companion object {
        @JvmStatic var instance: EchoEffectFragment? = null

        fun getInstance(
            iEchoCallback: EditMediaFragment.IEditMediaCallback?,
            entityAudio: EntityAudio?
        ): EchoEffectFragment {
            if (instance == null) {
                instance = EchoEffectFragment(iEchoCallback, entityAudio)
            }
            return instance!!
        }
    }

    private var binding: FragmentEchoEffectBinding? = null
    private var btnPreview: ImageButton? = null
    private var delaySeekBar: SeekBar? = null
    private var entityAudio: EntityAudio? = null
    private var iEchoCallback: EditMediaFragment.IEditMediaCallback? = null
    private var isPlay: Boolean = false
    private var repeatSeekBar: SeekBar? = null
    private var tv_hint_delay: TextCustumFont? = null
    private var tv_hint_repeat: TextCustumFont? = null
    private var tv_hint_volume: TextCustumFont? = null
    private var volumeSeekBar: SeekBar? = null

    constructor()
    constructor(
        iEchoCallback: EditMediaFragment.IEditMediaCallback?,
        entityAudio: EntityAudio?
    ) {
        this.iEchoCallback = iEchoCallback
        this.entityAudio = entityAudio
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentEchoEffectBinding.inflate(inflater, container, false)
        binding = bind
        val root: LinearLayout = bind.root

        if (entityAudio == null || iEchoCallback == null) {
            return root
        }

        tv_hint_delay = root.findViewById(R.id.tv_delay_size)
        tv_hint_repeat = root.findViewById(R.id.tv_repeat_size)
        tv_hint_volume = root.findViewById(R.id.tv_volume_size)

        val seekDelay = root.findViewById<SeekBar>(R.id.delaySeekBar)
        delaySeekBar = seekDelay
        seekDelay.progress = entityAudio!!.effectAudio.delays
        delaySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tv_hint_delay?.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (isPlay) {
                    iEchoCallback?.pausePreview()
                    updateButton()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekRepeat = root.findViewById<SeekBar>(R.id.repeatSeekBar)
        repeatSeekBar = seekRepeat
        seekRepeat.progress = entityAudio!!.effectAudio.decays
        repeatSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tv_hint_repeat?.text = (progress + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (isPlay) {
                    iEchoCallback?.pausePreview()
                    updateButton()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekVolume = root.findViewById<SeekBar>(R.id.volumeSeekBar)
        volumeSeekBar = seekVolume
        seekVolume.progress = entityAudio!!.effectAudio.volume_echo
        volumeSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tv_hint_volume?.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (isPlay) {
                    iEchoCallback?.pausePreview()
                    updateButton()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        tv_hint_delay?.text = delaySeekBar?.progress.toString()
        tv_hint_repeat?.text = repeatSeekBar?.progress.toString()
        tv_hint_volume?.text = volumeSeekBar?.progress.toString()

        root.findViewById<View>(R.id.btn_done).setOnClickListener { done() }
        val imageButton = root.findViewById<ImageButton>(R.id.btn_play)
        btnPreview = imageButton
        imageButton.setOnClickListener { previewAudio() }
        root.findViewById<View>(R.id.btn_appl_all).setOnClickListener { applyEchoEffect(true, false) }

        return root
    }

    fun updateButton() {
        btnPreview?.setImageResource(R.drawable.play_arrow_24px)
        isPlay = false
    }

    private fun applyEchoEffect(applyAll: Boolean, isPreview: Boolean) {
        var delayProgress = delaySeekBar?.progress ?: 0
        val repeatCount = (repeatSeekBar?.progress ?: 0) + 1
        val volumeProgress = volumeSeekBar?.progress ?: 0

        val effectAudio = entityAudio?.effectAudio ?: return

        if (!applyAll &&
            effectAudio.delays == delayProgress &&
            effectAudio.decays == (repeatSeekBar?.progress ?: 0) &&
            effectAudio.volume_echo == volumeProgress
        ) {
            iEchoCallback?.startPreview()
            return
        }

        val start = effectAudio.start / 1000.0f
        val end = effectAudio.end / 1000.0f

        effectAudio.decays = repeatSeekBar?.progress ?: 0
        effectAudio.delays = delayProgress
        effectAudio.volume_echo = volumeProgress

        val filters = arrayListOf<String>()
        filters.add(String.format(Locale.US, "atrim=start=%.2f:end=%.2f", start, end))
        filters.add("asetpts=N/SR/TB")

        if (effectAudio.isRemoveNoice) {
            filters.add("afftdn=nf=-25")
        }

        filters.add(String.format(Locale.US, "volume=%.2f", effectAudio.volume))

        if (effectAudio.fade_in > 0) {
            filters.add("afade=t=in:st=0:d=" + effectAudio.fade_in)
        }
        if (effectAudio.fade_out > 0) {
            val fadeOut = effectAudio.fade_out.toFloat()
            filters.add("afade=t=out:st=" + ((end - start) - fadeOut) + ":d=" + fadeOut)
        }

        if (effectAudio.isEnhance) {
            filters.add(Common.ENHANCE_CMD)
        }

        effectAudio.reverbPreset?.let { filters.add(it) }

        val outGain: Float
        if (effectAudio.decays <= 0 || effectAudio.delays <= 0) {
            outGain = 1.0f
        } else {
            val volumeRatio = volumeProgress / 100.0f
            val maxDecay = maxOf(0.01f, 1.0f - volumeRatio)

            val delaysBuilder = StringBuilder()
            val decaysBuilder = StringBuilder()
            var i = 1
            while (i <= repeatCount) {
                val originalDelay = delayProgress
                val decayValue = maxOf(0.01f, (maxDecay * Math.pow(0.8, (i - 1).toDouble())).toFloat())
                delaysBuilder.append(originalDelay * i)
                decaysBuilder.append(String.format(Locale.US, "%.2f", decayValue))
                if (i < repeatCount) {
                    delaysBuilder.append("|")
                    decaysBuilder.append("|")
                }
                i++
                delayProgress = originalDelay
            }

            val computedOutGain = maxOf(0.01f, volumeRatio)
            effectAudio.outGain = computedOutGain
            effectAudio.decays_cmd = decaysBuilder.toString()
            effectAudio.delays_cmd = delaysBuilder.toString()
            outGain = 1.0f

            filters.add(
                String.format(
                    Locale.US, "aecho=%.2f:%.2f:%s:%s",
                    1.0f, computedOutGain, delaysBuilder, decaysBuilder
                )
            )
        }

        if (effectAudio.speed != outGain) {
            filters.addAll(buildSpeedFilters(effectAudio.speed))
        }

        iEchoCallback?.let {
            if (applyAll) {
                it.updateEntity(EffectAudioType.ECHO, entityAudio!!)
                iEchoCallback?.onCmdAll(effectAudio)
            } else {
                val cmd = TextUtils.join(",", filters)
                if (isPreview) {
                    iEchoCallback?.onCmdPlay(cmd)
                } else {
                    iEchoCallback?.onCmd(cmd)
                }
            }
        }
    }

    private fun done() {
        if (iEchoCallback != null) {
            val effectAudio = entityAudio?.effectAudio ?: return
            if (effectAudio.delays != (delaySeekBar?.progress ?: 0) ||
                effectAudio.decays != (repeatSeekBar?.progress ?: 0) ||
                effectAudio.volume_echo != (volumeSeekBar?.progress ?: 0)
            ) {
                applyEchoEffect(false, false)
            }
            iEchoCallback?.pausePreview()
            iEchoCallback?.onDone()
        }
    }

    private fun previewAudio() {
        val wasPlaying = isPlay
        isPlay = !wasPlaying
        iEchoCallback?.let {
            if (!wasPlaying) {
                applyEchoEffect(false, true)
                btnPreview?.setImageResource(R.drawable.pause_24px)
            } else {
                it.pausePreview()
                btnPreview?.setImageResource(R.drawable.play_arrow_24px)
            }
        }
    }

    private fun buildSpeedFilters(speed: Float): List<String> {
        val filters = arrayListOf<String>()
        var remaining = speed

        when {
            remaining < 0.5f -> {
                while (remaining < 0.5f) {
                    filters.add("atempo=0.5")
                    remaining /= 0.5f
                }
                filters.add(String.format(Locale.US, "atempo=%.2f", remaining))
            }
            remaining > 2.0f -> {
                while (remaining > 2.0f) {
                    filters.add("atempo=2.0")
                    remaining /= 2.0f
                }
                filters.add(String.format(Locale.US, "atempo=%.2f", remaining))
            }
            else -> {
                filters.add(String.format(Locale.US, "atempo=%.2f", remaining))
            }
        }

        return filters
    }

    override fun onDestroyView() {
        iEchoCallback?.pausePreview()
        super.onDestroyView()
        instance = null
        binding = null
    }
}
