package hazem.nurmontage.videoquran.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.PresetAdapter
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.databinding.FragmentGradientBinding
import hazem.nurmontage.videoquran.model.Gradient
import hazem.nurmontage.videoquran.views.TextCustumFont

class GradientFragment : Fragment {

    companion object {
        @JvmStatic var instance: GradientFragment? = null

        fun getInstance(
            callback: EditIpadFragment.IIpadEditCallback?,
            index: Int
        ): GradientFragment {
            if (instance == null) {
                instance = GradientFragment(callback, index)
            }
            return instance!!
        }
    }

    private var adapter: PresetAdapter? = null
    private var binding: FragmentGradientBinding? = null
    private var gradient: Gradient? = null
    private var iIpadEditCallback: EditIpadFragment.IIpadEditCallback? = null
    private var index: Int = 0
    private var recyclerView: RecyclerView? = null
    private var seekBarAngle: SeekBar? = null
    private var tvAngle: TextCustumFont? = null

    private val iColorCallback = object : PresetAdapter.IColor {
        override fun onGradient(gradient: Gradient, position: Int) {
            if (this@GradientFragment.gradient == null) {
                binding?.layoutEditGradient?.root?.visibility = View.VISIBLE
            }
            this@GradientFragment.gradient = gradient
            this@GradientFragment.gradient?.angle = seekBarAngle?.progress ?: 0
            this@GradientFragment.index = position
            scrollToSelectedPosition()
            iIpadEditCallback?.onClick(gradient, position)
        }
    }

    constructor()
    constructor(callback: EditIpadFragment.IIpadEditCallback?, index: Int) {
        this.iIpadEditCallback = callback
        this.index = index
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentGradientBinding.inflate(inflater, container, false)
        binding = bind
        val root: LinearLayout = bind.root

        recyclerView = root.findViewById(R.id.rv_color)
        adapter = PresetAdapter(
            iColorCallback,
            Common.getListGradientColor(),
            index
        )

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            itemAnimator = null
            setHasFixedSize(true)
            this@GradientFragment.adapter?.let { adapter = it }
        }

        gradient = adapter?.getSelect()
        tvAngle = root.findViewById(R.id.tv_angle)
        seekBarAngle = root.findViewById(R.id.seekbar)

        if (gradient != null) {
            binding?.layoutEditGradient?.root?.visibility = View.VISIBLE
            seekBarAngle?.progress = gradient?.angle ?: 0
        }

        tvAngle?.setText(seekBarAngle?.progress?.toString() ?: "0")

        seekBarAngle?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (gradient == null || iIpadEditCallback == null) return
                gradient?.angle = progress
                tvAngle?.setText(progress.toString())
                iIpadEditCallback?.onClick(gradient!!, index)
            }
        })

        try {
            if (index > 3) {
                recyclerView?.scrollToPosition(index - 3)
            }
        } catch (_: Exception) {
        }

        return root
    }

    fun scrollToSelectedPosition() {
        val lm = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        adapter?.let { adp ->
            lm.scrollToPositionWithOffset(
                adp.getPosSelect(),
                (recyclerView?.width ?: 0) / 2 - 50
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        instance = null
        binding = null
        iIpadEditCallback = null
    }
}
