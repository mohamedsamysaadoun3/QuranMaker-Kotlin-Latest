package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.FrameAdapter
import hazem.nurmontage.videoquran.core.common.Constants.IpadType
import hazem.nurmontage.videoquran.databinding.FragmentEditIpadBinding
import hazem.nurmontage.videoquran.model.IpadItem
import hazem.nurmontage.videoquran.views.TextCustumFont

class EditIpadFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditIpadFragment? = null

        fun getInstance(
            resources: Resources?,
            ipadType: Int,
            callback: IIpadEditCallback?,
            indexSelect: Int,
            isGradient: Boolean,
            isGlass: Boolean
        ): EditIpadFragment {
            if (instance == null) {
                instance = EditIpadFragment(resources, ipadType, callback, indexSelect, isGradient, isGlass)
            }
            return instance!!
        }
    }

    interface IIpadEditCallback {
        fun onCancel()
        fun onChangeType(type: Int)
        fun onClick(color: Int, position: Int)
        fun onClick(gradient: hazem.nurmontage.videoquran.model.Gradient, position: Int)
        fun onDone()
        fun onGlassType(isGlass: Boolean)
    }

    private var fragmentBinding: FragmentEditIpadBinding? = null
    private var iIpadEditCallback: IIpadEditCallback? = null
    private var indexSelect: Int = 0
    private var ipadAdapter: FrameAdapter? = null
    private var ipadType: Int = 0
    private var isGlass: Boolean = false
    private var isGradient: Boolean = false
    private var mCurrentPosFragment: Int = 0
    private var resourcesRef: Resources? = null
    private var rvType: RecyclerView? = null

    constructor()

    constructor(
        resources: Resources?,
        ipadType: Int,
        callback: IIpadEditCallback?,
        indexSelect: Int,
        isGradient: Boolean,
        isGlass: Boolean
    ) {
        this.iIpadEditCallback = callback
        this.ipadType = ipadType
        this.isGlass = isGlass
        this.resourcesRef = resources
        this.indexSelect = indexSelect
        this.isGradient = isGradient
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditIpadBinding.inflate(inflater, container, false)
        fragmentBinding = binding
        val root: RelativeLayout = binding.root

        rvType = root.findViewById(R.id.rv_type)

        val ipadItems = arrayListOf<IpadItem>(
            IpadItem(R.drawable.ipad_t, IpadType.IPAD),
            IpadItem(R.drawable.ipad_unblur, IpadType.IPAD_UNBLUR),
            IpadItem(R.drawable.ipad_classic, IpadType.IPAD_CLASSIC),
            IpadItem(R.drawable.ipad_neomorphic, IpadType.IPAD_NEOMORPHIC),
            IpadItem(R.drawable.ipad_caset, IpadType.CASSET),
            IpadItem(R.drawable.ipad_caset_img, IpadType.CASSET_IMG),
            IpadItem(R.drawable.ipad_caset_img_blur, IpadType.CASSET_IMG_BLUR),
            IpadItem(R.drawable.ipad_rect, IpadType.RECT),
            IpadItem(R.drawable.ipad_rect_round, IpadType.ROUND_RECT),
            IpadItem(R.drawable.ipad_bottom_rect, IpadType.BOTTOM_RECT),
            IpadItem(R.drawable.ipad_layer_black, IpadType.BLACK_LAYER),
            IpadItem(R.drawable.ipad_gradient, IpadType.GRADIENT),
            IpadItem(R.drawable.ipad_mask, IpadType.MASK_BRUSH),
            IpadItem(R.drawable.ipad_blue_type, IpadType.BLUE_TYPE),
            IpadItem(R.drawable.ic_heart_ipad, IpadType.HEART),
            IpadItem(R.drawable.ic_battery, IpadType.BATTERY)
        )

        val posSelect = getPosSelect(ipadType, ipadItems)

        ipadAdapter = FrameAdapter(
            posSelect,
            ipadType,
            iIpadEditCallback,
            ipadItems,
            isGlass
        )

        rvType?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            itemAnimator = null
            adapter = ipadAdapter
        }

        if (posSelect > 3) {
            try {
                rvType?.scrollToPosition(posSelect - 3)
            } catch (_: Exception) {
            }
        }

        root.findViewById<View>(R.id.btn_done).setOnClickListener {
            iIpadEditCallback?.onDone()
        }

        initTab(root)
        return root
    }

    fun scrollToSelectedPosition() {
        try {
            val lm = rvType?.layoutManager as? LinearLayoutManager ?: return
            lm.scrollToPositionWithOffset(
                ipadAdapter?.getPosSelect() ?: 0,
                (rvType?.width ?: 0) / 2 - 50
            )
        } catch (_: Exception) {
        }
    }

    private fun addCustomViewToTab(tab: TabLayout.Tab) {
        val customView = layoutInflater.inflate(R.layout.layout_tablayout, null as ViewGroup?)
        (customView.findViewById(R.id.name) as TextCustumFont)
            .setText(tab.text.toString())
        tab.customView = customView
    }

    private fun initTab(view: View) {
        val tabLayout: TabLayout = view.findViewById(R.id.tab_layout)

        val colorTab = tabLayout.newTab().apply {
            setText(resourcesRef?.getString(R.string.color))
        }
        addCustomViewToTab(colorTab)

        val gradientTab = tabLayout.newTab().apply {
            setText(resourcesRef?.getString(R.string.gradient))
        }
        addCustomViewToTab(gradientTab)

        if (isGradient) {
            tabLayout.addTab(colorTab, false)
            tabLayout.addTab(gradientTab, true)
        } else {
            tabLayout.addTab(colorTab, true)
            tabLayout.addTab(gradientTab, false)
        }

        tabLayout.tabMode = TabLayout.MODE_FIXED
        mCurrentPosFragment = if (isGradient) 1 else 0

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {}
            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                if (mCurrentPosFragment != tab.position) {
                    mCurrentPosFragment = tab.position
                    childFragmentManager.beginTransaction()
                        .replace(R.id.container, getFragment(mCurrentPosFragment))
                        .addToBackStack(null)
                        .commit()
                }
            }
        })

        childFragmentManager.beginTransaction()
            .replace(R.id.container, getFragment(mCurrentPosFragment))
            .addToBackStack(null)
            .commit()

        indexSelect = -1
    }

    private fun getFragment(position: Int): Fragment {
        return if (position == 1) {
            GradientFragment.getInstance(iIpadEditCallback, indexSelect)
        } else {
            ColorsFragment.getInstance(iIpadEditCallback, indexSelect)
        }
    }

    private fun getPosSelect(ipadType: Int, items: List<IpadItem>): Int {
        for ((index, item) in items.withIndex()) {
            if (item.ipadType.ordinal == ipadType) {
                return index
            }
        }
        return 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        instance = null
        fragmentBinding = null
        iIpadEditCallback = null
    }
}
