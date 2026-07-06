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
import hazem.nurmontage.videoquran.databinding.FragmentEditMediaMultipleBinding
import hazem.nurmontage.videoquran.entity_timeline.Entity
import hazem.nurmontage.videoquran.views.TextCustumFont

class EditMultipleEntityFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditMultipleEntityFragment? = null

        private const val DISABLED_COLOR = -8355712
        private const val ENABLED_COLOR = -1

        fun getInstance(
            callback: IEditMultipleCallback?,
            resources: Resources?,
            countSelect: Int
        ): EditMultipleEntityFragment {
            if (instance == null) {
                instance = EditMultipleEntityFragment(callback, resources, countSelect)
            }
            return instance!!
        }
    }

    interface IEditMultipleCallback {
        fun onDelete()
    }

    private var btnCut: LinearLayout? = null
    private var countSelect: Int = 0
    private var fragmentBinding: FragmentEditMediaMultipleBinding? = null
    private var iEditMediaCallback: IEditMultipleCallback? = null
    private var ivCut: ImageView? = null
    private var resourcesRef: Resources? = null
    private var tvCut: TextCustumFont? = null
    private var tvDelete: TextCustumFont? = null

    constructor()

    constructor(iEditMultipleCallback: IEditMultipleCallback?, resources: Resources?, countSelect: Int) {
        this.iEditMediaCallback = iEditMultipleCallback
        this.resourcesRef = resources
        this.countSelect = countSelect
    }

    fun setCount_select(count: Int) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditMediaMultipleBinding.inflate(inflater, container, false)
        fragmentBinding = binding
        val root: RelativeLayout = binding.root

        if (iEditMediaCallback != null && resourcesRef != null) {
            ivCut = root.findViewById(R.id.iv_cut)
            ivCut?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)

            tvDelete = root.findViewById(R.id.tv_delete) as TextCustumFont
            tvDelete?.text = resourcesRef!!.getString(R.string.delete)

            tvCut = root.findViewById(R.id.tv_cut) as TextCustumFont
            tvCut?.text = resourcesRef!!.getString(R.string.cut)
            tvCut?.setTextColor(DISABLED_COLOR)

            root.findViewById<View>(R.id.btn_delete).setOnClickListener {
                iEditMediaCallback?.onDelete()
            }
        }
        return root
    }

    fun checkSplit(entity: Entity?, playheadX: Float) {
        if (entity == null) return
        try {
            if (entity.rect.left <= playheadX && entity.rect.right >= playheadX) {
                btnCut?.isClickable = true
                tvCut?.setTextColor(ENABLED_COLOR)
                ivCut?.setColorFilter(ENABLED_COLOR, PorterDuff.Mode.SRC_IN)
            }
            tvCut?.setTextColor(DISABLED_COLOR)
            ivCut?.setColorFilter(DISABLED_COLOR, PorterDuff.Mode.SRC_IN)
            btnCut?.isClickable = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        fragmentBinding = null
        instance = null
        iEditMediaCallback = null
        super.onDestroyView()
    }
}
