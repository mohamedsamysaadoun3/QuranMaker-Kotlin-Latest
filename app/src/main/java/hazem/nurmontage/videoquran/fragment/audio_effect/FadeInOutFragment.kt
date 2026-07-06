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
import hazem.nurmontage.videoquran.databinding.FragmentFadeInOutBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.util.Locale

class FadeInOutFragment : Fragment {

    companion object {
        @JvmStatic var instance: FadeInOutFragment? = null

        fun getInstance(
            iEditMediaCallback: EditMediaFragment.IEditMediaCallback?,
            entityAudio: EntityAudio?
        ): FadeInOutFragment {
            if (instance == null) {
                instance = FadeInOutFragment(iEditMediaCallback, entityAudio)
            }
            return instance!!
        }
    }

    private var binding: FragmentFadeInOutBinding? = null
    private var btnPreview: ImageButton? = null
    private var entityAudio: EntityAudio? = null
    private var fadeInSeekBar: SeekBar? = null
    private var fadeOutSeekBar: SeekBar? = null
    private var hint_fade_in: TextCustumFont? = null
    private var hint_fade_out: TextCustumFont? = null
    private var iEditMediaCallback: EditMediaFragment.IEditMediaCallback? = null
    private var isPlay: Boolean = false

    constructor(iEditMediaCallback: EditMediaFragment.IEditMediaCallback?, entityAudio: EntityAudio?) {
        this.iEditMediaCallback = iEditMediaCallback
        this.entityAudio = entityAudio
    }

    constructor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentFadeInOutBinding.inflate(inflater, container, false)
        binding = bind
        val root: LinearLayout = bind.root

        val audio = entityAudio
        if (audio == null || audio.mediaPlayer == null) {
            return root
        }

        hint_fade_in = root.findViewById(R.id.hint_fade_in)
        hint_fade_out = root.findViewById(R.id.hint_fade_out)
        fadeInSeekBar = root.findViewById(R.id.fadeInSeekBar)
        fadeOutSeekBar = root.findViewById(R.id.fadeOutSeekBar)

        val secondInScreen = audio.secondInScreen
        val halfDuration = ((audio.rect.right / secondInScreen) - (audio.rect.left / secondInScreen)) * 0.5f
        fadeInSeekBar?.max = halfDuration.toInt()
        fadeOutSeekBar?.max = halfDuration.toInt()
        fadeInSeekBar?.progress = audio.effectAudio.fade_in
        fadeOutSeekBar?.progress = audio.effectAudio.fade_out
        hint_fade_in?.text = fadeInSeekBar?.progress.toString()
        hint_fade_out?.text = fadeOutSeekBar?.progress.toString()

        fadeInSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                hint_fade_in?.text = progress.toString()
            }
        })

        fadeOutSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                hint_fade_out?.text = progress.toString()
            }
        })

        root.findViewById<View>(R.id.btn_done).setOnClickListener { done() }
        btnPreview = root.findViewById(R.id.btn_play)
        btnPreview?.setOnClickListener { previewAudio() }
        root.findViewById<View>(R.id.btn_appl_all).setOnClickListener { applyFade(true, false) }

        return root
    }

    private fun done() {
        if (iEditMediaCallback != null) {
            val audio = entityAudio ?: return
            if (audio.effectAudio.fade_in != fadeInSeekBar?.progress ||
                audio.effectAudio.fade_out != fadeOutSeekBar?.progress
            ) {
                applyFade(false, false)
            }
            iEditMediaCallback?.pausePreview()
            iEditMediaCallback?.onDone()
        }
    }

    private fun previewAudio() {
        val wasPlaying = isPlay
        isPlay = !wasPlaying
        iEditMediaCallback?.let {
            if (!wasPlaying) {
                applyFade(false, true)
                btnPreview?.setImageResource(R.drawable.pause_24px)
            } else {
                it.pausePreview()
                btnPreview?.setImageResource(R.drawable.play_arrow_24px)
            }
        }
    }

    private fun applyFade(applyAll: Boolean, isPreview: Boolean) {
        val effectAudio = entityAudio?.effectAudio ?: return
        val audio = entityAudio ?: return

        if (audio.getFadeIn().toInt() == fadeInSeekBar?.progress &&
            audio.getFadeOut().toInt() == fadeOutSeekBar?.progress
        ) {
            if (applyAll) {
                iEditMediaCallback?.onDone()
            }
            return
        }

        effectAudio.fade_in = fadeInSeekBar?.progress ?: 0
        effectAudio.fade_out = fadeOutSeekBar?.progress ?: 0

        val start = effectAudio.start / 1000.0f
        val end = effectAudio.end / 1000.0f

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

        if (effectAudio.decays > 0) {
            filters.add(
                String.format(
                    Locale.US, "aecho=%.2f:%.2f:%s:%s",
                    1.0f, effectAudio.outGain, effectAudio.delays_cmd, effectAudio.decays_cmd
                )
            )
        }

        if (effectAudio.speed != 1.0f) {
            filters.addAll(buildSpeedFilters(effectAudio.speed))
        }

        val cmd = TextUtils.join(",", filters)
        iEditMediaCallback?.let {
            if (applyAll) {
                it.updateEntity(EffectAudioType.FADE, audio)
                iEditMediaCallback?.onCmdAll(effectAudio)
            } else if (isPreview) {
                it.onCmdPlay(cmd)
            } else {
                it.onCmd(cmd)
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

    fun updateButton() {
        btnPreview?.setImageResource(R.drawable.play_arrow_24px)
        isPlay = false
    }

    override fun onDestroyView() {
        iEditMediaCallback?.pausePreview()
        super.onDestroyView()
        instance = null
        binding = null
    }
}
