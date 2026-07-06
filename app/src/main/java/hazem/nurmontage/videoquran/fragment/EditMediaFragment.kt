package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.EffectAudioType
import hazem.nurmontage.videoquran.databinding.FragmentEditMediaBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.model.EffectAudio
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.views.TextCustumFont

class EditMediaFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditMediaFragment? = null

        fun getInstance(
            callback: IEditMediaCallback?,
            resources: Resources?,
            entityAudio: EntityAudio?,
            posCursor: Float
        ): EditMediaFragment {
            if (instance == null) {
                instance = EditMediaFragment(callback, resources, entityAudio, posCursor)
            }
            return instance!!
        }
    }

    interface IEditMediaCallback {
        fun echoEffect()
        fun enhanceVoice()
        fun fadeEffect()
        fun noice()
        fun onCmd(cmd: String)
        fun onCmdAll(effectAudio: EffectAudio)
        fun onCmdPlay(cmd: String)
        fun onCut()
        fun onDelete()
        fun onDone()
        fun onDuplicate()
        fun onReplace()
        fun pausePreview()
        fun pitchEffect()
        fun reverbEffect()
        fun speedEffect()
        fun startPreview()
        fun updateEntity(type: EffectAudioType, entityAudio: EntityAudio)
        fun volumeEffect()
    }

    private var btnCut: LinearLayout? = null
    private var btnEcho: LinearLayout? = null
    private var btnEnhanceVoice: LinearLayout? = null
    private var btnFade: LinearLayout? = null
    private var btnRemoveNoice: LinearLayout? = null
    private var btnReverb: LinearLayout? = null
    private var btnSpeed: LinearLayout? = null
    private var btnVolume: LinearLayout? = null
    private var entitySelect: EntityAudio? = null
    private var fragmentBinding: FragmentEditMediaBinding? = null
    private var iEditMediaCallback: IEditMediaCallback? = null
    private var ivCut: ImageView? = null
    private var posCursor: Float = 0f
    private var resourcesRef: Resources? = null
    private var tvCut: TextCustumFont? = null

    constructor()

    constructor(
        callback: IEditMediaCallback?,
        resources: Resources?,
        entityAudio: EntityAudio?,
        posCursor: Float
    ) {
        this.iEditMediaCallback = callback
        this.resourcesRef = resources
        this.entitySelect = entityAudio
        this.posCursor = posCursor
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditMediaBinding.inflate(inflater, container, false)
        fragmentBinding = binding
        val root: RelativeLayout = binding.root

        if (iEditMediaCallback == null || resourcesRef == null || entitySelect == null) {
            return root
        }

        ivCut = root.findViewById(R.id.iv_cut)
        val scrollView: HorizontalScrollView = root.findViewById(R.id.view_scroll)
        btnCut = root.findViewById(R.id.btn_cut)

        val scrollX = MyPreferences.getScrollX(requireContext())
        if (scrollX != 0) {
            MyPreferences.putScrollX(requireContext(), 0)
            scrollView.post { scrollView.scrollTo(scrollX, 0) }
        }

        val btnShowLeft: ImageView = root.findViewById(R.id.btn_show_left)
        val btnShowRight: ImageView = root.findViewById(R.id.btn_show_right)

        scrollView.setOnScrollChangeListener { _, scrollPosX, _, _, _ ->
            try {
                if (scrollPosX > (btnCut?.width ?: 0) * 0.3f) {
                    btnShowRight.visibility = View.GONE
                    btnShowLeft.visibility = View.VISIBLE
                } else {
                    btnShowLeft.visibility = View.GONE
                    btnShowRight.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
            }
        }

        (root.findViewById(R.id.tv_enhance) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.enhance))
        (root.findViewById(R.id.tv_delete) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.delete))
        (root.findViewById(R.id.tv_duplicate) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.duplicate))
        (root.findViewById(R.id.tv_volume) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.volume))
        (root.findViewById(R.id.tv_reverbe) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.reverb))
        (root.findViewById(R.id.tv_echo) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.echo))
        (root.findViewById(R.id.tv_fade) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.fade))
        (root.findViewById(R.id.tv_noice) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.noice))
        (root.findViewById(R.id.tv_speed) as TextCustumFont)
            .setText(resourcesRef?.getString(R.string.speed))

        tvCut = root.findViewById(R.id.tv_cut) as TextCustumFont
        tvCut?.setText(resourcesRef?.getString(R.string.cut))

        root.findViewById<View>(R.id.btn_delete).setOnClickListener {
            iEditMediaCallback?.onDelete()
        }
        root.findViewById<View>(R.id.btn_duplicate).setOnClickListener {
            iEditMediaCallback?.onDuplicate()
        }

        btnReverb = root.findViewById(R.id.btn_reverb)
        btnReverb?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.reverbEffect()
            }
        }

        btnEnhanceVoice = root.findViewById(R.id.btn_enhance_voice)
        btnEnhanceVoice?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.enhanceVoice()
            }
        }

        btnRemoveNoice = root.findViewById(R.id.btn_remove_noice)
        btnRemoveNoice?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.noice()
            }
        }

        btnEcho = root.findViewById(R.id.btn_echo)
        btnEcho?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.echoEffect()
            }
        }

        btnVolume = root.findViewById(R.id.btn_volume)
        btnVolume?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.volumeEffect()
            }
        }

        btnFade = root.findViewById(R.id.btn_fade)
        btnFade?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.fadeEffect()
            }
        }

        btnSpeed = root.findViewById(R.id.btn_speed)
        btnSpeed?.setOnClickListener {
            iEditMediaCallback?.let { callback ->
                MyPreferences.putScrollX(requireContext(), scrollView.scrollX)
                callback.speedEffect()
            }
        }

        btnCut?.setOnClickListener {
            iEditMediaCallback?.onCut()
        }

        updateBtn()
        initCheckSplit(entitySelect!!, posCursor)

        return root
    }

    fun updateBtn() {
        try {
            val effect = entitySelect?.effectAudio ?: return

            btnReverb?.background = if (effect.reverbPreset != null)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null

            btnEnhanceVoice?.background = if (effect.isEnhance)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null

            btnRemoveNoice?.background = if (effect.isRemoveNoice)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null

            btnEcho?.background = if (effect.decays != 0 && effect.delays != 0 && effect.volume_echo != 0)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null

            btnVolume?.background = if (effect.volume != 1.0f)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null

            btnSpeed?.background = if (effect.speed != 1.0f)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null

            btnFade?.background = if (effect.fade_in > 0 && effect.fade_out > 0)
                requireContext().getDrawable(R.drawable.bg_item_effect) else null
        } catch (_: Exception) {
        }
    }

    fun initCheckSplit(entityAudio: EntityAudio, posCursor: Float) {
        try {
            if (entityAudio.rect.left <= posCursor && entityAudio.rect.right >= posCursor) {
                btnCut?.isClickable = true
                tvCut?.setTextColor(-0x1)
                ivCut?.setColorFilter(-0x1, PorterDuff.Mode.SRC_IN)
            }
            tvCut?.setTextColor(-0x7F7F80)
            ivCut?.setColorFilter(-0x7F7F80, PorterDuff.Mode.SRC_IN)
            btnCut?.isClickable = false
        } catch (_: Exception) {
        }
    }

    fun checkSplit(entityAudio: EntityAudio?, posCursor: Float) {
        if (entityAudio == null) return
        entitySelect = entityAudio
        updateBtn()
        try {
            if (entityAudio.rect.left <= posCursor && entityAudio.rect.right >= posCursor) {
                btnCut?.isClickable = true
                tvCut?.setTextColor(-0x1)
                ivCut?.setColorFilter(-0x1, PorterDuff.Mode.SRC_IN)
            }
            tvCut?.setTextColor(-0x7F7F80)
            ivCut?.setColorFilter(-0x7F7F80, PorterDuff.Mode.SRC_IN)
            btnCut?.isClickable = false
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        fragmentBinding = null
        instance = null
        iEditMediaCallback = null
        super.onDestroyView()
    }
}
