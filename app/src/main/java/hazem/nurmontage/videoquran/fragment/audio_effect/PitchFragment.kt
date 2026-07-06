package hazem.nurmontage.videoquran.fragment.audio_effect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.databinding.FragmentVolumeBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.EditMediaFragment
import hazem.nurmontage.videoquran.views.TextCustumFont

class PitchFragment : Fragment {

    companion object {
        @JvmStatic var instance: PitchFragment? = null

        fun getInstance(
            iEditMediaCallback: EditMediaFragment.IEditMediaCallback?,
            entityAudio: EntityAudio?
        ): PitchFragment {
            if (instance == null) {
                instance = PitchFragment(iEditMediaCallback, entityAudio)
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
        val seekBar = root.findViewById<SeekBar>(R.id.volumeSeekBar)
        volumeSeekBar = seekBar
        seekBar.max = 40
        seekBar.progress = 20

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
                applyVolume()
            }
        })

        root.findViewById<View>(R.id.btn_done).setOnClickListener { done() }
        btnPreview = root.findViewById(R.id.btn_play)
        btnPreview?.setOnClickListener { previewAudio() }

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

    fun applyVolume() {
        Math.pow(2.0, 0.08333333333333333)
        iVolumeCallback?.onCmd("asetrate=44100*1.2,atempo=0.8333")
    }

    override fun onDestroyView() {
        iVolumeCallback?.pausePreview()
        super.onDestroyView()
        instance = null
        binding = null
    }
}
