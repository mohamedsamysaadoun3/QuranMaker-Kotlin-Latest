package hazem.nurmontage.videoquran.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.FontAdapter
import hazem.nurmontage.videoquran.utils.FontProvider
import hazem.nurmontage.videoquran.databinding.FragmentFontBinding

class FontFragment : Fragment {

    companion object {
        @JvmStatic var instance: FontFragment? = null

        fun getInstance(callback: IFontCallback?, fontName: String?, typeface: Typeface?): FontFragment {
            if (instance == null) {
                instance = FontFragment(callback, fontName, typeface)
            }
            return instance!!
        }
    }

    interface IFontCallback {
        fun onAdd(fontName: String?, typeface: Typeface?)
        fun onCancel(fontName: String?, typeface: Typeface?)
        fun onDone(fontName: String?, typeface: Typeface?)
    }

    private var fontSelect: String? = null
    private var fragmentBinding: FragmentFontBinding? = null
    private var iFontCallback: IFontCallback? = null
    private var isInit: Boolean = true
    private var lastTypeface: Typeface? = null
    private var lastFont: String? = null
    private var linearLayoutManager: LinearLayoutManager? = null
    private var recyclerView: RecyclerView? = null
    private var typeface: Typeface? = null

    constructor()
    constructor(callback: IFontCallback?, fontName: String?, typeface: Typeface?) {
        this.iFontCallback = callback
        this.lastFont = fontName
        this.lastTypeface = typeface
    }

    fun add(typeface: Typeface?, fontName: String?) {
        this.typeface = typeface
        this.fontSelect = fontName
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
            val fontProvider = FontProvider(resources)
            recyclerView = root.findViewById(R.id.rv)

            val fontIndex = fontProvider.getFontNamesQuran()
                .indexOf(lastFont?.substring(0, lastFont!!.length - 4))

            val fontAdapter = FontAdapter(
                fontProvider,
                iFontCallback,
                fontProvider.getFontNamesQuran(),
                fontIndex
            )

            linearLayoutManager = LinearLayoutManager(requireContext())
            recyclerView?.apply {
                layoutManager = linearLayoutManager
                setHasFixedSize(true)
                adapter = fontAdapter
            }

            val snapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(recyclerView)

            recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isInit) {
                        isInit = false
                        return
                    }
                    val snapView = snapHelper.findSnapView(linearLayoutManager) ?: return
                    val position = linearLayoutManager!!.getPosition(snapView)
                    this@FontFragment.recyclerView?.post {
                        fontAdapter?.setSelected(position)
                    }
                }
            })

            if (fontIndex > 1) {
                recyclerView?.scrollToPosition(fontIndex - 1)
            } else if (fontIndex >= 0) {
                recyclerView?.scrollToPosition(fontIndex)
            }

            root.findViewById<View>(R.id.btn_done).setOnClickListener {
                iFontCallback?.onDone(fontSelect, typeface)
            }

            root.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                if (iFontCallback != null && lastFont != null && lastTypeface != null) {
                    iFontCallback!!.onCancel(lastFont, lastTypeface)
                }
            }
        } catch (_: Exception) {
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
        iFontCallback = null
        instance = null
    }
}
