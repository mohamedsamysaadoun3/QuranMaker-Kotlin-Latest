package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.databinding.FragmentAddAudioBinding
import hazem.nurmontage.videoquran.views.TextCustumFont

class AddAudioFragment : Fragment {

    companion object {
        @JvmStatic var instance: AddAudioFragment? = null

        fun getInstance(callback: IAudioCallback?, resources: Resources?): AddAudioFragment {
            if (instance == null) {
                instance = AddAudioFragment(callback, resources)
            }
            return instance!!
        }
    }

    interface IAudioCallback {
        fun cancel()
        fun extract()
        fun upload()
    }

    private var addAudioBinding: FragmentAddAudioBinding? = null
    private var iAudioCallback: IAudioCallback? = null
    private var resourcesRef: Resources? = null

    constructor()

    constructor(iAudioCallback: IAudioCallback?, resources: Resources?) {
        this.iAudioCallback = iAudioCallback
        this.resourcesRef = resources
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAddAudioBinding.inflate(inflater, container, false)
        addAudioBinding = binding
        val root: LinearLayout = binding.root

        if (resourcesRef != null && iAudioCallback != null) {
            (root.findViewById(R.id.tv_extract) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.extract_audio)
            (root.findViewById(R.id.tv_audio) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.audio)

            root.findViewById<View>(R.id.btn_upload).setOnClickListener {
                iAudioCallback?.upload()
            }
            root.findViewById<View>(R.id.btn_extract).setOnClickListener {
                iAudioCallback?.extract()
            }
            root.findViewById<View>(R.id.btn_close).setOnClickListener {
                iAudioCallback?.cancel()
            }
        }
        return root
    }

    override fun onDestroyView() {
        addAudioBinding = null
        instance = null
        iAudioCallback = null
        super.onDestroyView()
    }
}
