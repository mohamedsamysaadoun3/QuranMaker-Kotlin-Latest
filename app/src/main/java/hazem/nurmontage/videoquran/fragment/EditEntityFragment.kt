package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.databinding.FragmentEditEntityBinding
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.model.QuranEntity
import hazem.nurmontage.videoquran.views.TextCustumFont

class EditEntityFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditEntityFragment? = null

        fun getInstance(
            callback: IEditEntityCallback?,
            resources: Resources?,
            entity: Entity?,
            posCursor: Float
        ): EditEntityFragment {
            if (instance == null) {
                instance = EditEntityFragment(callback, resources, entity, posCursor)
            }
            return instance!!
        }

        private const val DISABLED_COLOR = -8355712
        private const val ENABLED_COLOR = -1
    }

    interface IEditEntityCallback {
        fun fromNow()
        fun fromTheStart()
        fun onAnim()
        fun onColor()
        fun onCut()
        fun onDelete()
        fun onDone()
        fun onDuplicate()
        fun onEdit()
        fun onFont()
        fun onIcon()
        fun untilNow()
        fun untilTheEnd()
        fun updateAya(color: Int)
        fun updatePreset(preset: AyaTextPreset)
        fun updateTrsl(color: Int)
    }

    private var btnCut: LinearLayout? = null
    private var btnFromNow: LinearLayout? = null
    private var btnIcon: LinearLayout? = null
    private var btnUntilNow: LinearLayout? = null
    private var entitySelect: Entity? = null
    private var fragmentBinding: FragmentEditEntityBinding? = null
    private var iEditEntityCallback: IEditEntityCallback? = null
    private var ivCut: ImageView? = null
    private var ivFromNow: ImageView? = null
    private var ivIcon: ImageView? = null
    private var ivUntilNow: ImageView? = null
    private var posCursur: Float = 0f
    private var resourcesRef: Resources? = null
    private var tvCut: TextCustumFont? = null
    private var tvFromNow: TextCustumFont? = null
    private var tvIcon: TextCustumFont? = null
    private var tvUntilNow: TextCustumFont? = null

    constructor()

    constructor(
        iEditEntityCallback: IEditEntityCallback?,
        resources: Resources?,
        entity: Entity?,
        posCursor: Float
    ) {
        this.iEditEntityCallback = iEditEntityCallback
        this.resourcesRef = resources
        this.entitySelect = entity
        this.posCursur = posCursor
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentEditEntityBinding.inflate(inflater, container, false)
        fragmentBinding = bind
        val root: RelativeLayout = bind.root

        if (iEditEntityCallback != null && resourcesRef != null) {
            ivCut = root.findViewById(R.id.iv_cut)
            ivFromNow = root.findViewById(R.id.iv_from_now)
            ivUntilNow = root.findViewById(R.id.iv_until_now)

            (root.findViewById(R.id.tv_delete) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.delete)
            tvCut = root.findViewById(R.id.tv_cut) as TextCustumFont
            tvCut!!.text = resourcesRef!!.getString(R.string.cut)
            (root.findViewById(R.id.tv_edit) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.edit)
            (root.findViewById(R.id.tv_color) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.color)

            root.findViewById<View>(R.id.btn_color).setOnClickListener {
                iEditEntityCallback?.onColor()
            }
            root.findViewById<View>(R.id.btn_delete).setOnClickListener {
                iEditEntityCallback?.onDelete()
            }

            btnCut = root.findViewById(R.id.btn_cut)
            btnCut!!.setOnClickListener {
                iEditEntityCallback?.onCut()
            }
            root.findViewById<View>(R.id.btn_edit).setOnClickListener {
                iEditEntityCallback?.onEdit()
            }

            tvFromNow = root.findViewById(R.id.tv_from_now) as TextCustumFont
            tvFromNow!!.text = resourcesRef!!.getString(R.string.from_now)
            (root.findViewById(R.id.tv_from_the_start) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.from_the_start)
            tvUntilNow = root.findViewById(R.id.tv_until_now) as TextCustumFont
            tvUntilNow!!.text = resourcesRef!!.getString(R.string.until_now)
            (root.findViewById(R.id.tv_until_the_end) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.until_the_end)
            (root.findViewById(R.id.tv_duplicate) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.duplicate)
            (root.findViewById(R.id.tv_font) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.font)
            (root.findViewById(R.id.tv_anim) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.animtion)

            btnFromNow = root.findViewById(R.id.btn_from_now)
            btnFromNow!!.setOnClickListener {
                iEditEntityCallback?.fromNow()
            }
            root.findViewById<View>(R.id.btn_from_the_start).setOnClickListener {
                iEditEntityCallback?.fromTheStart()
            }
            btnUntilNow = root.findViewById(R.id.btn_until_now)
            btnUntilNow!!.setOnClickListener {
                iEditEntityCallback?.untilNow()
            }
            root.findViewById<View>(R.id.btn_until_the_end).setOnClickListener {
                iEditEntityCallback?.untilTheEnd()
            }
            root.findViewById<View>(R.id.btn_duplicate).setOnClickListener {
                iEditEntityCallback?.onDuplicate()
            }
            root.findViewById<View>(R.id.btn_font).setOnClickListener {
                iEditEntityCallback?.onFont()
            }

            tvIcon = root.findViewById(R.id.tv_icon) as TextCustumFont
            tvIcon!!.text = resourcesRef!!.getString(R.string.icon)
            ivIcon = root.findViewById(R.id.iv_icon)
            btnIcon = root.findViewById(R.id.btn_icon)
            btnIcon!!.setOnClickListener {
                iEditEntityCallback?.onIcon()
            }
            checkIcon(entitySelect)

            root.findViewById<View>(R.id.btn_anim).setOnClickListener {
                iEditEntityCallback?.onAnim()
            }

            val btnShowLeft: ImageView = root.findViewById(R.id.btn_show_left)
            val btnShowRight: ImageView = root.findViewById(R.id.btn_show_right)
            (root.findViewById<HorizontalScrollView>(R.id.scroll_menu))
                .setOnScrollChangeListener { _, scrollX, _, _, _ ->
                    try {
                        if (scrollX > btnCut!!.width * 0.3f) {
                            btnShowRight.visibility = View.GONE
                            btnShowLeft.visibility = View.VISIBLE
                        } else {
                            btnShowLeft.visibility = View.GONE
                            btnShowRight.visibility = View.VISIBLE
                        }
                    } catch (_: Exception) {
                    }
                }

            checkSplitEntity(entitySelect, posCursur)
        }
        return root
    }

    fun checkSplitEntity(entity: Entity?, cursorPos: Float) {
        if (entity == null) return
        try {
            if (entity.rect.right < cursorPos) {
                tvFromNow?.setTextColor(DISABLED_COLOR)
                ivFromNow?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)
                btnFromNow?.isClickable = false
            } else {
                btnFromNow?.isClickable = true
                tvFromNow?.setTextColor(ENABLED_COLOR)
                ivFromNow?.setColorFilter(ENABLED_COLOR, PorterDuff.Mode.SRC_IN)
            }
            if (entity.rect.left > cursorPos) {
                tvUntilNow?.setTextColor(DISABLED_COLOR)
                ivUntilNow?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)
                btnUntilNow?.isClickable = false
            } else {
                btnUntilNow?.isClickable = true
                tvUntilNow?.setTextColor(ENABLED_COLOR)
                ivUntilNow?.setColorFilter(ENABLED_COLOR, PorterDuff.Mode.SRC_IN)
            }
            if (entity.rect.left <= cursorPos && entity.rect.right >= cursorPos) {
                btnCut?.isClickable = true
                tvCut?.setTextColor(ENABLED_COLOR)
                ivCut?.setColorFilter(ENABLED_COLOR, PorterDuff.Mode.SRC_IN)
                return
            }
            tvCut?.setTextColor(DISABLED_COLOR)
            ivCut?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)
            btnCut?.isClickable = false
        } catch (_: Exception) {
        }
    }

    fun checkIcon(entity: Entity?) {
        try {
            if (entity?.getEntityView() is QuranEntity) {
                if ((entity.getEntityView() as QuranEntity).getNumber() == -1) {
                    tvIcon?.setTextColor(DISABLED_COLOR)
                    ivIcon?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)
                    btnIcon?.isClickable = false
                } else {
                    btnIcon?.isClickable = true
                    tvIcon?.setTextColor(ENABLED_COLOR)
                    ivIcon?.setColorFilter(ENABLED_COLOR, PorterDuff.Mode.SRC_IN)
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        fragmentBinding = null
        instance = null
        iEditEntityCallback = null
        super.onDestroyView()
    }
}
