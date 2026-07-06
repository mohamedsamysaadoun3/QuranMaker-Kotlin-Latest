package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.ColorAdapter
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.databinding.FragmentColorAyaBinding
import hazem.nurmontage.videoquran.fragment.EditBismilahEntityFragment
import hazem.nurmontage.videoquran.model.BismilahEntity
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.views.TextCustumFont

class ColorBismilahFragment : Fragment {

    companion object {
        @JvmStatic var instance: ColorBismilahFragment? = null

        fun getInstance(
            callback: EditBismilahEntityFragment.IBismilahEntityCallback?,
            entity: BismilahEntity?,
            resources: Resources?
        ): ColorBismilahFragment {
            if (instance == null) {
                instance = ColorBismilahFragment(callback, entity, resources)
            }
            return instance!!
        }
    }

    private var adapter: ColorAdapter? = null
    private var binding: FragmentColorAyaBinding? = null
    private var entitySelect: BismilahEntity? = null
    private var iColor: ColorAdapter.IColor? = null
    private var iEditSName: EditBismilahEntityFragment.IBismilahEntityCallback? = null
    private var recyclerView: RecyclerView? = null
    private var resourcesRef: Resources? = null

    constructor()

    constructor(
        iEditSName: EditBismilahEntityFragment.IBismilahEntityCallback?,
        entity: BismilahEntity?,
        resources: Resources?
    ) {
        this.iEditSName = iEditSName
        this.entitySelect = entity
        this.resourcesRef = resources
    }

    private fun setupPresetButtons(view: View) {
        val btnNone = view.findViewById<TextCustumFont>(R.id.btnNone)
        val btnOutline = view.findViewById<TextCustumFont>(R.id.btnOutline)
        val btnShadow = view.findViewById<TextCustumFont>(R.id.btnShadow)
        val btnGlow = view.findViewById<TextCustumFont>(R.id.btnGlow)

        btnNone.text = resourcesRef!!.getString(R.string.preset_none)
        btnOutline.text = resourcesRef!!.getString(R.string.preset_outline)
        btnShadow.text = resourcesRef!!.getString(R.string.preset_shadow)
        btnGlow.text = resourcesRef!!.getString(R.string.preset_glow)

        val buttons = arrayOf<TextView>(btnNone, btnOutline, btnShadow, btnGlow)
        val presets = arrayOf(
            AyaTextPreset.NONE, AyaTextPreset.OUTLINE,
            AyaTextPreset.SHADOW, AyaTextPreset.GLOW
        )

        for (i in 0..3) {
            buttons[i].setOnClickListener {
                selectPreset(buttons, i)
                iEditSName?.updatePreset(presets[i])
            }
        }

        val entity = entitySelect ?: return
        val currentPreset = AyaTextPreset.values()[entity.getmPreset()]
        val selectedIndex = when (currentPreset) {
            AyaTextPreset.OUTLINE -> 1
            AyaTextPreset.SHADOW -> 2
            AyaTextPreset.GLOW -> 3
            else -> 0
        }
        selectPreset(buttons, selectedIndex)
    }

    private fun selectPreset(buttons: Array<TextView>, selected: Int) {
        for (i in buttons.indices) {
            buttons[i].isSelected = i == selected
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentColorAyaBinding.inflate(inflater, container, false)
        binding = bind
        val root: LinearLayout = bind.root

        if (iEditSName != null && entitySelect != null && resourcesRef != null) {
            recyclerView = root.findViewById(R.id.rv_color)

            iColor = object : ColorAdapter.IColor {
                override fun onColor(color: Int, position: Int) {
                    if (iEditSName == null || entitySelect == null) return
                    scrollToSelectedPosition()
                    iEditSName!!.updateAya(color)
                }
            }

            adapter = ColorAdapter(
                iColor,
                Constants.MUSLIM_AYA_COLORS,
                Utils.indexOf(Constants.MUSLIM_AYA_COLORS, entitySelect!!.clrAya)
            )
            recyclerView?.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            recyclerView?.itemAnimator = null
            recyclerView?.setHasFixedSize(true)
            recyclerView?.adapter = adapter

            if (adapter!!.getPosSelect() > 2) {
                scrollToSelectedPosition(adapter!!.getPosSelect() - 2)
            }

            initTab(root)
            setupPresetButtons(root)

            root.findViewById<View>(R.id.btn_done).setOnClickListener {
                iEditSName?.onDone()
            }
        }
        return root
    }

    fun scrollToSelectedPosition() {
        val lm = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        lm.scrollToPositionWithOffset(
            adapter?.getPosSelect() ?: 0,
            (recyclerView?.width ?: 0) / 2 - 50
        )
    }

    fun scrollToSelectedPosition(position: Int) {
        val lm = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        lm.scrollToPositionWithOffset(position, recyclerView!!.width / 2)
    }

    private fun initTab(view: View) {
        view.findViewById<View>(R.id.tab_layout).visibility = View.GONE
    }

    override fun onDestroyView() {
        binding = null
        instance = null
        iColor = null
        super.onDestroyView()
    }
}
