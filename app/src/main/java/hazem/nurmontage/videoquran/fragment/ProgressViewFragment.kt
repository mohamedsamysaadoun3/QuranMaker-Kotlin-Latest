package hazem.nurmontage.videoquran.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.databinding.FragmentProgressViewBinding
import hazem.nurmontage.videoquran.views.TextCustumFont

class ProgressViewFragment : Fragment() {

    companion object {
        private var _instance: ProgressViewFragment? = null

        @JvmStatic
        fun getInstance(): ProgressViewFragment {
            if (_instance == null) {
                _instance = ProgressViewFragment()
            }
            return _instance!!
        }
    }

    private var binding: FragmentProgressViewBinding? = null
    private var tvProgress: TextCustumFont? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentProgressViewBinding.inflate(inflater, container, false)
        binding = bind
        val root: FrameLayout = bind.root
        tvProgress = root.findViewById(R.id.tv_progress)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _instance = null
    }

    fun update(current: Int, total: Int) {
        tvProgress?.text = "$current/$total"
    }
}
