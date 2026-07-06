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
import com.google.android.material.tabs.TabLayout
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.ColorAdapter
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.databinding.FragmentColorAyaBinding
import hazem.nurmontage.videoquran.fragment.EditTrslEntityFragment
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.views.TextCustumFont

class ColorAyaFragment : Fragment {

    companion object {
        @JvmStatic var instance: ColorAyaFragment? = null

        fun getInstance(
            callback: EditTrslEntityFragment.IEditEntityCallback?,
            entity: QuranEntity?,
            resources: Resources?
        ): ColorAyaFragment {
            if (instance == null) {
                instance = ColorAyaFragment(callback, entity, resources)
            }
            return instance!!
        }
    }

    private var adapter: ColorAdapter? = null
    private var binding: FragmentColorAyaBinding? = null
    private var entitySelect: QuranEntity? = null
    private var iColor: ColorAdapter.IColor? = null
    private var iEditEntityCallback: EditTrslEntityFragment.IEditEntityCallback? = null
    private var recyclerView: RecyclerView? = null
    private var resourcesRef: Resources? = null
    private var tabLayout: TabLayout? = null

    constructor()

    constructor(
        iEditEntityCallback: EditTrslEntityFragment.IEditEntityCallback?,
        entity: QuranEntity?,
        resources: Resources?
    ) {
        this.iEditEntityCallback = iEditEntityCallback
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
                iEditEntityCallback?.updatePreset(presets[i])
            }
        }

        val entity = entitySelect ?: return
        val currentPreset = AyaTextPreset.values()[entity.get(entity.getmPreset()).ordinal]
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

        if (iEditEntityCallback != null && entitySelect != null && resourcesRef != null) {
            recyclerView = root.findViewById(R.id.rv_color)

            iColor = object : ColorAdapter.IColor {
                override fun onColor(color: Int, position: Int) {
                    if (iEditEntityCallback == null) return
                    scrollToSelectedPosition()
                    if (tabLayout == null || tabLayout!!.selectedTabPosition == 0) {
                        iEditEntityCallback!!.updateAya(color)
                    } else {
                        iEditEntityCallback!!.updateTrsl(color)
                    }
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
                iEditEntityCallback?.onDone()
            }
        }
        return root
    }

    fun scrollToSelectedPosition(position: Int) {
        val lm = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        lm.scrollToPositionWithOffset(position, recyclerView!!.width / 2)
    }

    fun scrollToSelectedPosition() {
        val lm = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        lm.scrollToPositionWithOffset(
            adapter?.getPosSelect() ?: 0,
            (recyclerView?.width ?: 0) / 2 - 50
        )
    }

    private fun initTab(view: View) {
        if (entitySelect?.translation == null) {
            view.findViewById<View>(R.id.tab_layout).visibility = View.GONE
            return
        }
        val tl = view.findViewById<TabLayout>(R.id.tab_layout)
        tabLayout = tl

        val tabAya = tl.newTab()
        tabAya.setText(resourcesRef!!.getString(R.string.tab_aya))
        addCustomViewToTab(tabAya)

        val tabTrsl = tabLayout!!.newTab()
        tabTrsl.setText(resourcesRef!!.getString(R.string.tv_translation))
        addCustomViewToTab(tabTrsl)

        tabLayout!!.addTab(tabAya, true)
        tabLayout!!.addTab(tabTrsl, false)
        tabLayout!!.tabMode = TabLayout.MODE_FIXED
    }

    private fun addCustomViewToTab(tab: TabLayout.Tab) {
        val customView = layoutInflater.inflate(R.layout.layout_tablayout, null as ViewGroup?)
        (customView.findViewById(R.id.name) as TextCustumFont).text = tab.text.toString()
        tab.customView = customView
    }

    override fun onDestroyView() {
        binding = null
        instance = null
        iColor = null
        super.onDestroyView()
    }
}
