package hazem.nurmontage.videoquran.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.IconQuranAdabters
import hazem.nurmontage.videoquran.databinding.FragmentFontBinding

class EditIconQuranFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditIconQuranFragment? = null

        fun getInstance(callback: IQuranIconCallback?, icon: String?): EditIconQuranFragment {
            if (instance == null) {
                instance = EditIconQuranFragment(callback, icon)
            }
            return instance!!
        }
    }

    interface IQuranIconCallback {
        fun add(icon: String)
        fun onCancel(lastIcon: String)
        fun onDone(icon: String)
    }

    private var fragmentBinding: FragmentFontBinding? = null
    private var iQuranIconCallback: IQuranIconCallback? = null
    private var icon: String? = null
    private var lastIcon: String? = null
    private var iconQuranCallback: IconQuranAdabters.IIconQuranCallback? = null

    constructor()

    constructor(iQuranIconCallback: IQuranIconCallback?, icon: String?) {
        this.iQuranIconCallback = iQuranIconCallback
        this.icon = icon
        this.lastIcon = icon
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFontBinding.inflate(inflater, container, false)
        fragmentBinding = binding
        val root: LinearLayout = binding.root

        try {
            val recyclerView = root.findViewById<RecyclerView>(R.id.rv)
            recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            recyclerView.itemAnimator = null
            recyclerView.setHasFixedSize(true)

            val iconList = arrayListOf("hafes", "shamerli", "nour_hode", "amiri")

            iconQuranCallback = object : IconQuranAdabters.IIconQuranCallback {
                override fun onIcon(iconName: String) {
                    icon = iconName
                    iQuranIconCallback?.add(icon!!)
                }
            }

            val adapter = IconQuranAdabters(iconQuranCallback, iconList, iconList.indexOf(icon))
            if (adapter.getSelect() != -1) {
                icon = iconList[adapter.getSelect()]
            }
            recyclerView.adapter = adapter

            root.findViewById<View>(R.id.btn_done).setOnClickListener {
                iQuranIconCallback?.onDone(icon!!)
            }
            root.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                iQuranIconCallback?.onCancel(lastIcon!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        iconQuranCallback = null
        fragmentBinding = null
        iQuranIconCallback = null
        instance = null
    }
}
