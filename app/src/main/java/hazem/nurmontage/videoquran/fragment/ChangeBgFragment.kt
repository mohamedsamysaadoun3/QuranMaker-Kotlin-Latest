package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.BgAdapter
import hazem.nurmontage.videoquran.adapter.IBgCallback
import hazem.nurmontage.videoquran.model.BgItem
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.views.TextCustumFont

class ChangeBgFragment : Fragment {

    companion object {
        @JvmStatic var instance: ChangeBgFragment? = null

        fun getInstance(
            callback: IChangeBgCallback?,
            resources: Resources?,
            selectedBg: String?
        ): ChangeBgFragment {
            if (instance == null) {
                instance = ChangeBgFragment(callback, resources, selectedBg)
            }
            return instance!!
        }
    }

    interface IChangeBgCallback : IBgCallback {
        fun onCancel()
        fun onCrop()
        fun onDone()
        fun onUploadImg()
        fun onUploadVideo()
    }

    private var adapter: BgAdapter? = null
    private var callback: IChangeBgCallback? = null
    private var layoutAddVideo: View? = null
    private var layoutBgRv: View? = null
    private var recyclerView: RecyclerView? = null
    private var res: Resources? = null
    private var selectedBg: String? = null

    constructor()

    constructor(callback: IChangeBgCallback?, resources: Resources?, selectedBg: String?) {
        this.callback = callback
        this.res = resources
        this.selectedBg = selectedBg
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            selectedBg = arguments?.getString("bg_select")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_change_bg, container, false)
        bindViews(view)
        setupRecycler()
        setupButtons(view)
        return view
    }

    private fun bindViews(view: View) {
        recyclerView = view.findViewById(R.id.rv)
        layoutBgRv = view.findViewById(R.id.layout_bg_rv)
        layoutAddVideo = view.findViewById(R.id.layout_add_video_img)
        if (res != null) {
            (view.findViewById(R.id.tv_img) as TextCustumFont).text = res!!.getString(R.string.image)
            (view.findViewById(R.id.tv_video) as TextCustumFont).text = res!!.getString(R.string.video)
        }
    }

    private fun setupRecycler() {
        val bgData = getBgData()
        adapter = BgAdapter(
            AppUtils.getAppVersionName(requireContext()),
            callback,
            bgData,
            (ScreenUtils.getScreenWidth(requireActivity()) * 0.2f).toInt(),
            findSelectedIndex(bgData)
        )
        val layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        layoutManager.isItemPrefetchEnabled = true
        layoutManager.initialPrefetchItemCount = 6
        recyclerView?.layoutManager = layoutManager
        recyclerView?.adapter = adapter
        recyclerView?.setHasFixedSize(true)
        recyclerView?.setItemViewCacheSize(12)
        recyclerView?.itemAnimator = null
        LinearSnapHelper().attachToRecyclerView(recyclerView)
        recyclerView?.post { scrollToSelected() }
    }

    private fun setupButtons(view: View) {
        view.findViewById<View>(R.id.btn_add).setOnClickListener {
            layoutAddVideo?.visibility = View.VISIBLE
            layoutBgRv?.visibility = View.INVISIBLE
        }
        view.findViewById<View>(R.id.btn_close).setOnClickListener {
            layoutAddVideo?.visibility = View.GONE
            layoutBgRv?.visibility = View.VISIBLE
        }
        view.findViewById<View>(R.id.btn_add_img).setOnClickListener {
            callback?.onUploadImg()
        }
        view.findViewById<View>(R.id.btn_add_video).setOnClickListener {
            callback?.onUploadVideo()
        }
        view.findViewById<View>(R.id.btn_done).setOnClickListener {
            callback?.onDone()
        }
        view.findViewById<View>(R.id.btn_cancel).setOnClickListener {
            callback?.onCancel()
        }
        view.findViewById<View>(R.id.btn_crop).setOnClickListener {
            callback?.onCrop()
        }
    }

    private fun getBgData(): List<BgItem> {
        return listOf(
            BgItem(R.drawable.bg_21, 0.1734694f, 0.31632653f, "bg_21"),
            BgItem(R.drawable.bg_22, 0.1734694f, 0.31632653f, "bg_22"),
            BgItem(R.drawable.bg_23, 0.1734694f, 0.31632653f, "bg_23"),
            BgItem(R.drawable.bg_24, 0.1734694f, 0.31632653f, "bg_24"),
            BgItem(R.drawable.bg_25, 0.1734694f, 0.31632653f, "bg_25"),
            BgItem(R.drawable.bg_26, 0.1734694f, 0.31632653f, "bg_26"),
            BgItem(R.drawable.bg_27, 0.1734694f, 0.31632653f, "bg_27"),
            BgItem(R.drawable.bg_32, 0.1734694f, 0.31632653f, "bg_32"),
            BgItem(R.drawable.bg_33, 0.1734694f, 0.31632653f, "bg_33"),
            BgItem(R.drawable.bg_34, 0.1734694f, 0.31632653f, "bg_34"),
            BgItem(R.drawable.bg_35, 0.1734694f, 0.31632653f, "bg_35"),
            BgItem(R.drawable.bg_36, 0.1734694f, 0.31632653f, "bg_36"),
            BgItem(R.drawable.bg_37, 0.1734694f, 0.31632653f, "bg_37"),
            BgItem(R.drawable.bg_38, 0.1734694f, 0.31632653f, "bg_38"),
            BgItem(R.drawable.bg_28, 0.1734694f, 0.31632653f, "bg_28"),
            BgItem(R.drawable.bg_29, 0.1734694f, 0.31632653f, "bg_29"),
            BgItem(R.drawable.bg_30, 0.1734694f, 0.31632653f, "bg_30"),
            BgItem(R.drawable.bg_31, 0.1734694f, 0.31632653f, "bg_31"),
            BgItem(R.drawable.bg_1, 0.1734694f, 0.51632655f, "bg_1"),
            BgItem(R.drawable.bg_2, 0.45918366f, 0.3392857f, "bg_2"),
            BgItem(R.drawable.bg_3, 0.21683674f, 0.073979594f, "bg_3"),
            BgItem(R.drawable.bg_4, 0.3469388f, 0.30612245f, "bg_4"),
            BgItem(R.drawable.bg_5, 0.19132653f, 0.26785713f, "bg_5"),
            BgItem(R.drawable.bg_6, 0.4486844f, 0.093112245f, "bg_6"),
            BgItem(R.drawable.bg_7, 0.41326532f, 0.45918366f, "bg_7"),
            BgItem(R.drawable.bg_8, 0.42091838f, 0.44005102f, "bg_8"),
            BgItem(R.drawable.bg_9, 0.3482143f, 0.2614796f, "bg_9"),
            BgItem(R.drawable.bg_10, 0.3137755f, 0.17219388f, "bg_10"),
            BgItem(R.drawable.bg_11, 0.49107143f, 0.17219388f, "bg_11"),
            BgItem(R.drawable.bg_12, 0.2755102f, 0.16709183f, "bg_12"),
            BgItem(R.drawable.bg_13, 0.35841838f, 0.1747449f, "bg_13"),
            BgItem(R.drawable.bg_14, 0.35841838f, 0.1747449f, "bg_14"),
            BgItem(R.drawable.bg_15, 0.35841838f, 0.1747449f, "bg_15"),
            BgItem(R.drawable.bg_16, 0.35841838f, 0.1747449f, "bg_16"),
            BgItem(R.drawable.bg_17, 0.35841838f, 0.1747449f, "bg_17"),
            BgItem(R.drawable.bg_18, 0.35841838f, 0.1747449f, "bg_18"),
            BgItem(R.drawable.bg_19, 0.35841838f, 0.1747449f, "bg_19"),
            BgItem(R.drawable.bg_20, 0.35841838f, 0.1747449f, "bg_20")
        )
    }

    private fun findSelectedIndex(list: List<BgItem>): Int {
        if (selectedBg == null) return 0
        for (i in list.indices) {
            if (selectedBg == list[i].name_drawable) {
                return i
            }
        }
        return 0
    }

    fun scrollToSelected() {
        val rv = recyclerView ?: return
        val lm = rv.layoutManager as? LinearLayoutManager ?: return
        lm.scrollToPositionWithOffset(adapter?.getSelectedPosition() ?: 0, rv.width / 2)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callback = null
        instance = null
    }
}
