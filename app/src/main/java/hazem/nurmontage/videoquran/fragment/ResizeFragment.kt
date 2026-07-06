package hazem.nurmontage.videoquran.fragment

import android.app.Activity
import android.content.res.Resources
import android.os.Bundle
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.DimensionAdabters
import hazem.nurmontage.videoquran.common.DataDimension
import hazem.nurmontage.videoquran.databinding.FragmentResizeBinding
import hazem.nurmontage.videoquran.model.ItemDimension
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.utils.Utils

class ResizeFragment : Fragment {

    companion object {
        @JvmStatic var instance: ResizeFragment? = null

        fun getInstance(
            callback: DimensionAdabters.IDimensionCallback?,
            resources: Resources?,
            selectResize: String?
        ): ResizeFragment {
            if (instance == null) {
                instance = ResizeFragment(callback, resources, selectResize)
            }
            return instance!!
        }
    }

    private var adabter: DimensionAdabters? = null
    private var binding: FragmentResizeBinding? = null
    private var iDimensionCallback: DimensionAdabters.IDimensionCallback? = null
    private var posSelectResize: Int = -1
    private var recyclerView: RecyclerView? = null
    private var res: Resources? = null
    private var selectResize: String? = null

    constructor()

    constructor(
        iDimensionCallback: DimensionAdabters.IDimensionCallback?,
        resources: Resources?,
        selectResize: String?
    ) {
        this.iDimensionCallback = iDimensionCallback
        this.selectResize = selectResize
        this.res = resources
    }

    fun scrollToSelectedPosition() {
        try {
            val lm = recyclerView?.layoutManager as? LinearLayoutManager ?: return
            lm.scrollToPositionWithOffset(
                adabter?.getSelected() ?: 0,
                (recyclerView?.width ?: 0) / 2 - 50
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentResizeBinding.inflate(inflater, container, false)
        binding = bind
        val root: RelativeLayout = bind.root

        if (res != null && iDimensionCallback != null) {
            root.findViewById<View>(R.id.btn_done).setOnClickListener {
                iDimensionCallback?.done()
            }

            val rv = root.findViewById<RecyclerView>(R.id.rv)
            recyclerView = rv
            rv.setHasFixedSize(true)
            recyclerView?.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            recyclerView?.itemAnimator = null

            val allDimensions = DataDimension.getALl(res!!)
            val dimensionAdapter = DimensionAdabters(
                allDimensions,
                iDimensionCallback,
                getListDimension(requireActivity(), allDimensions),
                posSelectResize
            )
            adabter = dimensionAdapter
            recyclerView?.adapter = dimensionAdapter

            if (posSelectResize > 0) {
                recyclerView?.scrollToPosition(posSelectResize - 1)
            } else {
                recyclerView?.scrollToPosition(posSelectResize)
            }
        }
        return root
    }

    fun getListDimension(
        activity: Activity,
        list: List<ItemDimension>
    ): List<Pair<Int, Int>> {
        val screenWidth = (ScreenUtils.getScreenWidth(activity) * 0.27f).toInt()
        val result = arrayListOf<Pair<Int, Int>>()
        for (i in list.indices) {
            val item = list[i]
            if (item.id == selectResize) {
                posSelectResize = i
            }
            result.add(Utils.getDimension(item.resizeType, screenWidth))
        }
        return result
    }

    override fun onDestroyView() {
        iDimensionCallback = null
        instance = null
        binding?.root?.removeAllViews()
        binding = null
        super.onDestroyView()
    }
}
