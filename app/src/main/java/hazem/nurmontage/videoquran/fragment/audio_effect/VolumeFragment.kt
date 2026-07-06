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
import hazem.nurmontage.videoquran.databinding.FragmentVolumeBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.util.Locale

class VolumeFragment : Fragment {

    companion object {
        @JvmStatic var instance: VolumeFragment? = null

        fun getInstance(
            iEditMediaCallback: EditMediaFragment.IEditMediaCallback?,
            entityAudio: EntityAudio?
        ): VolumeFragment {
            if (instance == null) {
                instance = VolumeFragment(iEditMediaCallback, entityAudio)
            }
            return instance!!
        }
    }

    private var binding: FragmentVolumeBinding? = null
    private var btnPreview: ImageButton? = null
    private var entityAudio: EntityAudio? = null
    private var iVolumeCallback: EditMediaFragment.IEditMediaCallback? = null
    private var isPlay: Boolean = false
    private var tvProgress: TextCustumFont? = null
    private var volumeSeekBar: SeekBar? = null

    constructor(iEditMediaCallback: EditMediaFragment.IEditMediaCallback?, entityAudio: EntityAudio?) {
        this.iVolumeCallback = iEditMediaCallback
        this.entityAudio = entityAudio
    }

    constructor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentVolumeBinding.inflate(inflater, container, false)
        binding = bind
        val root: LinearLayout = bind.root

        val audio = entityAudio
        if (audio == null || audio.mediaPlayer == null) {
            return root
        }

        tvProgress = root.findViewById(R.id.tv_volume_size)
        val volume = (audio.effectAudio.volume * 100f).toInt()
        tvProgress?.text = volume.toString()

        val seekBar = root.findViewById<SeekBar>(R.id.volumeSeekBar)
        volumeSeekBar = seekBar
        seekBar.progress = volume
        volumeSeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvProgress?.text = progress.toString()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (isPlay) {
                    previewAudio()
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                applyVolume(false)
            }
        })

        root.findViewById<View>(R.id.btn_done).setOnClickListener { done() }
        btnPreview = root.findViewById(R.id.btn_play)
        btnPreview?.setOnClickListener { previewAudio() }
        root.findViewById<View>(R.id.btn_appl_all).setOnClickListener { applyVolume(true) }

        return root
    }

    private fun done() {
        iVolumeCallback?.let {
            it.pausePreview()
            it.onDone()
        }
    }

    fun previewAudio() {
        val wasPlaying = isPlay
        isPlay = !wasPlaying
        iVolumeCallback?.let {
            if (!wasPlaying) {
                it.startPreview()
                btnPreview?.setImageResource(R.drawable.pause_24px)
            } else {
                it.pausePreview()
                btnPreview?.setImageResource(R.drawable.play_arrow_24px)
            }
        }
    }

    fun applyVolume(applyAll: Boolean) {
        val effectAudio = entityAudio?.effectAudio ?: return
        effectAudio.volume = (volumeSeekBar?.progress ?: 0) / 100.0f

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

        iVolumeCallback?.let {
            if (applyAll) {
                it.updateEntity(EffectAudioType.VOLUME, entityAudio!!)
                iVolumeCallback?.onCmdAll(effectAudio)
            } else {
                iVolumeCallback?.onCmd(TextUtils.join(",", filters))
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
        iVolumeCallback?.pausePreview()
        super.onDestroyView()
        instance = null
        binding = null
    }

    fun updateButton() {
        btnPreview?.setImageResource(R.drawable.play_arrow_24px)
        isPlay = false
    }
}
