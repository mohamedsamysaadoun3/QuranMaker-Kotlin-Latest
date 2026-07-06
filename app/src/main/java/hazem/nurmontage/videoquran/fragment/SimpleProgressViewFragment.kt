package hazem.nurmontage.videoquran.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.databinding.FragmentProgressViewBinding

class SimpleProgressViewFragment : Fragment() {

    companion object {
        @JvmStatic
        private var _instance: SimpleProgressViewFragment? = null

        @JvmStatic
        fun getInstance(): SimpleProgressViewFragment {
            if (_instance == null) {
                _instance = SimpleProgressViewFragment()
            }
            return _instance!!
        }
    }

    private var fragmentBinding: FragmentProgressViewBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentProgressViewBinding.inflate(inflater, container, false)
        fragmentBinding = binding
        val root: FrameLayout = binding.root
        root.setBackgroundColor(0)
        root.findViewById<View>(R.id.view_1).visibility = View.GONE
        root.findViewById<View>(R.id.view_2).visibility = View.GONE
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
        _instance = null
    }
}
