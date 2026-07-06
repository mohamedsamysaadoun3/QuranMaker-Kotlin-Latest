package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.TransitionBismilahAdabters
import hazem.nurmontage.videoquran.adapter.TransitionBismilahAdabters.TransitionItem
import hazem.nurmontage.videoquran.constant.TransitionType
import hazem.nurmontage.videoquran.databinding.FragmentEffectAyaBinding
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline
import hazem.nurmontage.videoquran.model.Transition
import hazem.nurmontage.videoquran.views.TextCustumFont
import nl.dionsegijn.konfetti.core.Angle

@Suppress("DEPRECATION")
class EffectBismilahFragment : Fragment {

    companion object {
        @JvmStatic var instance: EffectBismilahFragment? = null

        @Synchronized
        fun get(
            transition: Transition?,
            resources: Resources?,
            iTransition: TransitionBismilahAdabters.ITransition?,
            entityBismilahTimeline: EntityBismilahTimeline?
        ): EffectBismilahFragment {
            if (instance == null) {
                instance = EffectBismilahFragment(transition, resources, iTransition, entityBismilahTimeline!!)
            }
            return instance!!
        }

        private const val DISABLED_COLOR = -8355712
        private const val ENABLED_COLOR = -1
    }

    private var btnApplyAll: LinearLayout? = null
    private var btnUnEffect: ImageButton? = null
    private var entityQuranTimeline: EntityBismilahTimeline? = null
    private var iTransition: TransitionBismilahAdabters.ITransition? = null
    private var index: Int = 0
    private var ivApplyAll: ImageView? = null
    private var recyclerView: RecyclerView? = null
    private var resourcesRef: Resources? = null
    private var seekBarDuration: SeekBar? = null
    private var tabSelected: Int = 0
    private var time: Float = 0f
    private var transition: Transition? = null
    private var transitionEntityAdabters: TransitionBismilahAdabters? = null
    private var transitionEntityBinding: FragmentEffectAyaBinding? = null
    private var tvDuration: TextCustumFont? = null
    private var tvApplyAll: TextCustumFont? = null

    constructor()

    constructor(
        transition: Transition?,
        resources: Resources?,
        iTransition: TransitionBismilahAdabters.ITransition?,
        entityBismilahTimeline: EntityBismilahTimeline
    ) {
        this.resourcesRef = resources
        this.iTransition = iTransition
        this.transition = transition
        this.time = (entityBismilahTimeline.rect.width() / entityBismilahTimeline.secondInScreen) * 0.5f
        this.entityQuranTimeline = entityBismilahTimeline
    }

    private fun addCustomViewToTab(tab: TabLayout.Tab) {
        val inflate = layoutInflater.inflate(R.layout.layout_tablayout, null as ViewGroup?)
        val textCustumFont = inflate.findViewById<TextCustumFont>(R.id.name)
        inflate.findViewById<View>(R.id.icon).visibility = View.GONE
        textCustumFont.text = tab.text.toString()
        tab.customView = inflate
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentEffectAyaBinding.inflate(inflater, container, false)
        transitionEntityBinding = bind
        val root: LinearLayout = bind.root

        val res = resourcesRef
        val callback = iTransition
        if (res != null && callback != null) {
            callback.playing(entityQuranTimeline!!)

            val tabLayout = root.findViewById<TabLayout>(R.id.tab_layout)
            tvDuration = root.findViewById(R.id.status_duration)

            val newTab = tabLayout.newTab()
            newTab.text = res.getString(R.string.in_transition)
            tabLayout.addTab(newTab)
            addCustomViewToTab(newTab)

            val newTab2 = tabLayout.newTab()
            newTab2.text = res.getString(R.string.out_transition)
            tabLayout.addTab(newTab2)
            addCustomViewToTab(newTab2)

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab) {}
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabSelected(tab: TabLayout.Tab) {
                    tabSelected = tab.position
                    loadTransition(tab.position)
                }
            })
            tabLayout.getTabAt(0)?.select()

            val seekBar = root.findViewById<SeekBar>(R.id.seekbar)
            seekBarDuration = seekBar
            seekBar.max = (time * 4.0f).toInt()
            seekBarDuration!!.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    tvDuration?.text = (progress / 10.0f).toString()
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (iTransition != null) {
                        if (tabSelected == 0) {
                            iTransition?.updateDurationIn(seekBar.progress / 4.0f, entityQuranTimeline!!)
                        } else if (tabSelected == 1) {
                            iTransition?.updateDurationOut(seekBar.progress / 4.0f, entityQuranTimeline!!)
                        }
                        visibleApplyAll()
                    }
                }
            })

            btnUnEffect = root.findViewById(R.id.btn_unEffect)
            val tr = transition
            if (tr != null && tr.isIn) {
                val min = minOf(tr.duration_in, time)
                seekBarDuration!!.progress = (4.0f * min).toInt()
                tvDuration?.text = (seekBarDuration!!.progress / 10.0f).toString()
                iTransition?.updateDurationIn(min, entityQuranTimeline!!)
                btnUnEffect!!.setBackgroundResource(R.drawable.circle_effect)
            } else {
                seekBarDuration!!.visibility = View.GONE
                tvDuration?.visibility = View.GONE
                btnUnEffect!!.setBackgroundResource(R.drawable.circle_item_menu_select)
            }

            val rv = root.findViewById<RecyclerView>(R.id.rv)
            recyclerView = rv
            rv.setHasFixedSize(true)
            rv.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            rv.setItemViewCacheSize(20)
            rv.isDrawingCacheEnabled = true
            rv.itemAnimator = null
            rv.setDrawingCacheQuality(RecyclerView.DRAWING_CACHE_QUALITY_HIGH)

            btnUnEffect!!.setOnClickListener {
                if (transitionEntityAdabters?.isHaveSelect() == true) {
                    iTransition?.remove(tabSelected, entityQuranTimeline!!)
                    transitionEntityAdabters?.unselect()
                    btnUnEffect?.setBackgroundResource(R.drawable.circle_item_menu_select)
                    seekBarDuration?.visibility = View.GONE
                    tvDuration?.visibility = View.GONE
                    visibleApplyAll()
                }
            }

            root.findViewById<View>(R.id.btn_close).setOnClickListener {
                iTransition?.onHideFragment(entityQuranTimeline!!)
            }

            btnApplyAll = root.findViewById(R.id.btn_appl_all)
            tvApplyAll = root.findViewById(R.id.tv_apply_all)
            ivApplyAll = root.findViewById(R.id.iv_apply_all)
            tvApplyAll?.text = res.getString(R.string.applyall)
            btnApplyAll!!.isEnabled = false
            btnApplyAll!!.isClickable = false
            btnApplyAll!!.setOnClickListener {
                if (iTransition != null) {
                    iTransition?.applyAll(entityQuranTimeline!!)
                    invisibleApplyAll()
                }
            }

            root.post {
                val inTransition = getInTransition()
                val selectIndex: Int = if (transition == null || !transition!!.isIn) {
                    -1
                } else {
                    getIndex(inTransition, transition!!.type_in)
                }
                transitionEntityAdabters = TransitionBismilahAdabters(
                    iTransition, inTransition, selectIndex, entityQuranTimeline!!
                )
                recyclerView?.adapter = transitionEntityAdabters
                scroll(transitionEntityAdabters!!.getSelect())
            }
        }
        return root
    }

    fun updateView(duration: Float, transition: Transition) {
        this.transition = transition
        if (seekBarDuration?.visibility != View.VISIBLE) {
            seekBarDuration?.visibility = View.VISIBLE
            tvDuration?.visibility = View.VISIBLE
        }
        btnUnEffect?.setBackgroundResource(R.drawable.circle_effect)
        updateSeek(duration, true)
        visibleApplyAll()
    }

    private fun invisibleApplyAll() {
        if (btnApplyAll?.isEnabled == true) {
            btnApplyAll?.isEnabled = false
            btnApplyAll?.isClickable = false
            tvApplyAll?.setTextColor(DISABLED_COLOR)
            ivApplyAll?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)
        }
    }

    fun visibleApplyAll() {
        if (btnApplyAll?.isEnabled == true) return
        btnApplyAll?.isEnabled = true
        btnApplyAll?.isClickable = true
        tvApplyAll?.setTextColor(ENABLED_COLOR)
        ivApplyAll?.setColorFilter(ENABLED_COLOR, PorterDuff.Mode.SRC_IN)
    }

    fun scroll(position: Int) {
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        val viewByPosition = layoutManager.findViewByPosition(position)
        layoutManager.scrollToPositionWithOffset(
            position,
            (recyclerView!!.width - (viewByPosition?.width ?: 0)) / 2
        )
    }

    fun updateButton(transition: Transition) {
        this.transition = transition
        btnUnEffect?.setBackgroundResource(R.drawable.circle_effect)
        visibleSeekbar()
    }

    fun getIndex(list: List<TransitionItem>, type: String): Int {
        for (i in list.indices) {
            if (type == list[i].type) return i
        }
        return -1
    }

    private fun updateSeek(duration: Float, fromUser: Boolean) {
        seekBarDuration?.progress = (duration * 4.0f).toInt()
        tvDuration?.text = (seekBarDuration!!.progress / 10.0f).toString()
    }

    private fun visibleSeekbar() {
        seekBarDuration?.visibility = View.VISIBLE
        tvDuration?.visibility = View.VISIBLE
    }

    private fun invisibleSeekbar() {
        seekBarDuration?.visibility = View.GONE
        tvDuration?.visibility = View.GONE
    }

    fun loadTransition(tabPosition: Int) {
        index = -1
        if (tabPosition == 0) {
            val inTransition = getInTransition()
            val tr = transition
            if (tr != null) {
                if (tr.isIn) {
                    val idx = getIndex(inTransition, tr.type_in)
                    index = idx
                    if (idx != -1) {
                        visibleSeekbar()
                        iTransition?.updateDurationIn(tr.duration_in, entityQuranTimeline!!)
                        btnUnEffect?.setBackgroundResource(R.drawable.circle_effect)
                    } else {
                        entityQuranTimeline?.quranEntity?.endAnimator()
                        invisibleSeekbar()
                        btnUnEffect?.setBackgroundResource(R.drawable.circle_item_menu_select)
                    }
                } else {
                    entityQuranTimeline?.quranEntity?.endAnimator()
                    invisibleSeekbar()
                    btnUnEffect?.setBackgroundResource(R.drawable.circle_item_menu_select)
                }
            }
            transitionEntityAdabters?.update(inTransition, "in", index)
            scroll(index)
            val tr2 = transition
            if (tr2 != null) {
                updateSeek(tr2.duration_in, tr2.isIn)
            }
        } else if (tabPosition == 1) {
            val outTransition = getOutTransition()
            val tr = transition
            if (tr != null) {
                if (tr.isOut) {
                    val idx = getIndex(outTransition, tr.type_out)
                    index = idx
                    if (idx != -1) {
                        visibleSeekbar()
                        iTransition?.updateDurationOut(tr.duration_out, entityQuranTimeline!!)
                        btnUnEffect?.setBackgroundResource(R.drawable.circle_effect)
                    } else {
                        entityQuranTimeline?.quranEntity?.endAnimator()
                        invisibleSeekbar()
                        btnUnEffect?.setBackgroundResource(R.drawable.circle_item_menu_select)
                    }
                } else {
                    entityQuranTimeline?.quranEntity?.endAnimator()
                    invisibleSeekbar()
                    btnUnEffect?.setBackgroundResource(R.drawable.circle_item_menu_select)
                }
            }
            transitionEntityAdabters?.update(outTransition, "out", index)
            scroll(index)
            val tr2 = transition
            if (tr2 != null) {
                updateSeek(tr2.duration_out, tr2.isOut)
            }
        }
    }

    fun getInTransition(): List<TransitionItem> {
        return listOf(
            TransitionItem(TransitionType.FADE_IN.value, R.drawable.ic_linear_gradient, 0),
            TransitionItem(TransitionType.SLIDE_TO_RIGHT.value, R.drawable.ic_btn_back, Angle.LEFT),
            TransitionItem(TransitionType.SLIDE_TO_LEFT.value, R.drawable.ic_btn_back, 0)
        )
    }

    private fun getOutTransition(): List<TransitionItem> {
        return listOf(
            TransitionItem(TransitionType.FADE_OUT.value, R.drawable.ic_linear_gradient, 0),
            TransitionItem(TransitionType.SLIDE_TO_RIGHT.value, R.drawable.ic_btn_back, Angle.LEFT),
            TransitionItem(TransitionType.SLIDE_TO_LEFT.value, R.drawable.ic_btn_back, 0)
        )
    }

    override fun onDestroyView() {
        iTransition?.destroy(entityQuranTimeline!!)
        transitionEntityBinding?.let {
            it.root.removeAllViews()
        }
        transitionEntityBinding = null
        instance = null
        super.onDestroyView()
    }
}
