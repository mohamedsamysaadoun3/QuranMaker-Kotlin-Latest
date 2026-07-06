package hazem.nurmontage.videoquran.fragment.audio_effect

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.SoundAdapter
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.constant.EffectAudioType
import hazem.nurmontage.videoquran.databinding.FragmentReverbePresetBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import java.util.Locale

class ReverbePresetFragment : Fragment {

    companion object {
        @JvmStatic var instance: ReverbePresetFragment? = null

        fun getInstance(
            callback: EditMediaFragment.IEditMediaCallback?,
            entityAudio: EntityAudio?
        ): ReverbePresetFragment {
            if (instance == null) {
                instance = ReverbePresetFragment(callback, entityAudio)
            }
            return instance!!
        }
    }

    private var binding: FragmentReverbePresetBinding? = null
    private var entityAudio: EntityAudio? = null
    private var iEditMediaCallback: EditMediaFragment.IEditMediaCallback? = null

    private var iReverbPresetCallback: SoundAdapter.IReverbPresetCallback? =
        object : SoundAdapter.IReverbPresetCallback {

            override fun cmd(presetCmd: String?, position: Int) {
                if (iEditMediaCallback == null) return
                val effect = entityAudio?.effectAudio ?: return

                if (presetCmd == null && effect.reverbPreset == null) {
                    iEditMediaCallback?.startPreview()
                    return
                }

                effect.reverbPreset = presetCmd
                effect.reverbPreset_index_list = position

                iEditMediaCallback?.onCmdPlay(buildFilterChain(effect))
            }

            override fun pause() {
                iEditMediaCallback?.pausePreview()
            }
        }

    constructor()
    constructor(callback: EditMediaFragment.IEditMediaCallback?, entityAudio: EntityAudio?) {
        this.iEditMediaCallback = callback
        this.entityAudio = entityAudio
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentReverbePresetBinding.inflate(inflater, container, false)
        binding = bind
        val root: LinearLayout = bind.root

        if (iEditMediaCallback == null || entityAudio == null) {
            return root
        }

        val recyclerView: RecyclerView = root.findViewById(R.id.rv)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null

        val presets = arrayListOf(
            Reverbe(getString(R.string.reverb_normal), null),
            Reverbe(getString(R.string.reverb_masjid), "aecho=0.9:0.4:900|1800:0.20|0.15"),
            Reverbe(getString(R.string.reverb_masjid_2), "aecho=0.9:0.4:900:0.18"),
            Reverbe(getString(R.string.reverb_studio), "aecho=0.8:0.35:400|700:0.20|0.15"),
            Reverbe(getString(R.string.reverb_quiet_room), "aecho=0.6:0.3:300:0.12"),
            Reverbe(getString(R.string.reverb_tiled_room), "aecho=0.9:0.4:600|1200:0.20|0.15"),
            Reverbe(getString(R.string.reverb_deep), "aecho=0.6:0.35:1000:0.20")
        )

        recyclerView.adapter = SoundAdapter(
            presets,
            iReverbPresetCallback,
            entityAudio!!.effectAudio.reverbPreset_index_list
        )

        root.findViewById<View>(R.id.btn_done).setOnClickListener {
            iEditMediaCallback?.pausePreview()
            iEditMediaCallback?.onDone()
        }

        root.findViewById<View>(R.id.btn_appl_all).setOnClickListener {
            applyAll()
        }

        return root
    }

    private fun buildFilterChain(effect: hazem.nurmontage.videoquran.model.EffectAudio): String {
        val start = effect.start / 1000f
        val end = effect.end / 1000f
        val duration = end - start

        val filters = arrayListOf<String>()
        filters.add(String.format(Locale.US, "atrim=start=%.2f:end=%.2f", start, end))
        filters.add("asetpts=N/SR/TB")

        if (effect.isRemoveNoice) {
            filters.add("afftdn=nf=-25")
        }

        filters.add(String.format(Locale.US, "volume=%.2f", effect.volume))

        if (effect.fade_in > 0) {
            filters.add(String.format(Locale.US, "afade=t=in:st=0:d=%.2f", effect.fade_in / 1000f))
        }
        if (effect.fade_out > 0) {
            val fadeOutSec = effect.fade_out / 1000f
            filters.add(String.format(Locale.US, "afade=t=out:st=%.2f:d=%.2f", duration - fadeOutSec, fadeOutSec))
        }

        if (effect.isEnhance) {
            filters.add(Common.ENHANCE_CMD)
        }

        effect.reverbPreset?.let { filters.add(it) }

        if (effect.decays > 0) {
            filters.add(
                String.format(
                    Locale.US, "aecho=%.2f:%.2f:%s:%s",
                    1.0f, effect.outGain, effect.delays_cmd, effect.decays_cmd
                )
            )
        }

        if (effect.speed != 1.0f) {
            filters.addAll(buildSpeedFilters(effect.speed))
        }

        return TextUtils.join(",", filters)
    }

    private fun applyAll() {
        val effect = entityAudio?.effectAudio ?: return
        val start = effect.start / 1000f
        val end = effect.end / 1000f

        val filters = arrayListOf<String>()
        filters.add(String.format(Locale.US, "atrim=start=%.2f:end=%.2f", start, end))
        filters.add("asetpts=N/SR/TB")

        if (effect.isRemoveNoice) {
            filters.add("afftdn=nf=-25")
        }

        filters.add(String.format(Locale.US, "volume=%.2f", effect.volume))

        if (effect.fade_in > 0) {
            filters.add("afade=t=in:st=0:d=${effect.fade_in}")
        }
        if (effect.fade_out > 0) {
            val fadeOut = effect.fade_out.toFloat()
            filters.add("afade=t=out:st=${(end - start) - fadeOut}:d=$fadeOut")
        }

        if (effect.isEnhance) {
            filters.add(Common.ENHANCE_CMD)
        }

        effect.reverbPreset?.let { filters.add(it) }

        if (effect.decays > 0) {
            filters.add(
                String.format(
                    Locale.US, "aecho=%.2f:%.2f:%s:%s",
                    1.0f, effect.outGain, effect.delays_cmd, effect.decays_cmd
                )
            )
        }

        if (effect.speed != 1.0f) {
            filters.addAll(buildSpeedFilters(effect.speed))
        }

        iEditMediaCallback?.updateEntity(EffectAudioType.REVERB, entityAudio!!)
        iEditMediaCallback?.onCmdAll(effect)
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
        iEditMediaCallback?.pausePreview()
        iReverbPresetCallback = null
        super.onDestroyView()
        instance = null
        binding = null
    }
}
