package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.common.Constants.AyaTextPreset
import hazem.nurmontage.videoquran.databinding.FragmentEditEntityBinding
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.views.TextCustumFont

class EditBismilahEntityFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditBismilahEntityFragment? = null

        fun getInstance(
            callback: IBismilahEntityCallback?,
            resources: Resources?,
            entity: Entity?,
            posCursor: Float
        ): EditBismilahEntityFragment {
            if (instance == null) {
                instance = EditBismilahEntityFragment(callback, resources, entity, posCursor)
            }
            return instance!!
        }

        private const val DISABLED_COLOR = -8355712
        private const val ENABLED_COLOR = -1
    }

    interface IBismilahEntityCallback {
        fun fromNow()
        fun fromTheStart()
        fun onAnim()
        fun onColor()
        fun onDelete()
        fun onDone()
        fun untilNow()
        fun untilTheEnd()
        fun update()
        fun updateAya(color: Int)
        fun updatePreset(preset: AyaTextPreset)
    }

    private var btnDelete: LinearLayout? = null
    private var btnFromNow: LinearLayout? = null
    private var btnUntilNow: LinearLayout? = null
    private var entitySelect: Entity? = null
    private var fragmentBinding: FragmentEditEntityBinding? = null
    private var iEditEntityCallback: IBismilahEntityCallback? = null
    private var ivFromNow: ImageView? = null
    private var ivUntilNow: ImageView? = null
    private var posCursur: Float = 0f
    private var resourcesRef: Resources? = null
    private var tvFromNow: TextCustumFont? = null
    private var tvUntilNow: TextCustumFont? = null

    constructor()

    constructor(
        iEditEntityCallback: IBismilahEntityCallback?,
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
            root.findViewById<View>(R.id.btn_duplicate).visibility = View.GONE
            root.findViewById<View>(R.id.btn_font).visibility = View.GONE
            root.findViewById<View>(R.id.btn_icon).visibility = View.GONE
            root.findViewById<View>(R.id.btn_edit).visibility = View.GONE
            root.findViewById<View>(R.id.btn_show_left).visibility = View.GONE
            root.findViewById<View>(R.id.btn_show_right).visibility = View.GONE
            root.findViewById<View>(R.id.btn_cut).visibility = View.GONE

            ivFromNow = root.findViewById(R.id.iv_from_now)
            ivUntilNow = root.findViewById(R.id.iv_until_now)

            (root.findViewById(R.id.tv_delete) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.delete)
            (root.findViewById(R.id.tv_color) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.color)

            root.findViewById<View>(R.id.btn_color).setOnClickListener {
                iEditEntityCallback?.onColor()
            }

            btnDelete = root.findViewById(R.id.btn_delete)
            btnDelete!!.setOnClickListener {
                iEditEntityCallback?.onDelete()
            }

            tvFromNow = root.findViewById(R.id.tv_from_now) as TextCustumFont
            tvFromNow!!.text = resourcesRef!!.getString(R.string.from_now)
            (root.findViewById(R.id.tv_from_the_start) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.from_the_start)
            tvUntilNow = root.findViewById(R.id.tv_until_now) as TextCustumFont
            tvUntilNow!!.text = resourcesRef!!.getString(R.string.until_now)
            (root.findViewById(R.id.tv_until_the_end) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.until_the_end)
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
            root.findViewById<View>(R.id.btn_anim).setOnClickListener {
                iEditEntityCallback?.onAnim()
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
