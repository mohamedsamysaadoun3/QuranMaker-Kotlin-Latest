package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.databinding.FragmentEditSNameBinding
import hazem.nurmontage.videoquran.model.SurahNameEntity
import hazem.nurmontage.videoquran.views.TextCustumFont

class EditS_NameFragment : Fragment {

    companion object {
        @JvmStatic var instance: EditS_NameFragment? = null

        fun getInstance(
            callback: IEditS_Name?,
            resources: Resources?,
            entity: SurahNameEntity?
        ): EditS_NameFragment {
            if (instance == null) {
                instance = EditS_NameFragment(callback, resources, entity)
            }
            return instance!!
        }
    }

    interface IEditS_Name {
        fun onColor(entity: SurahNameEntity)
        fun onDone()
        fun onEdit(entity: SurahNameEntity)
        fun onFont(entity: SurahNameEntity)
        fun update()
    }

    private var entitySelect: SurahNameEntity? = null
    private var fragmentBinding: FragmentEditSNameBinding? = null
    private var iEditSName: IEditS_Name? = null
    private var resourcesRef: Resources? = null

    constructor()

    constructor(iEditSName: IEditS_Name?, resources: Resources?, entity: SurahNameEntity?) {
        this.iEditSName = iEditSName
        this.resourcesRef = resources
        this.entitySelect = entity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentEditSNameBinding.inflate(inflater, container, false)
        fragmentBinding = binding
        val root: LinearLayout = binding.root

        if (iEditSName != null && resourcesRef != null && entitySelect != null) {
            (root.findViewById(R.id.tv_color) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.color)
            (root.findViewById(R.id.tv_edit) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.edit)
            (root.findViewById(R.id.tv_font) as TextCustumFont)
                .text = resourcesRef!!.getString(R.string.font)

            root.findViewById<View>(R.id.btn_font).setOnClickListener {
                iEditSName?.onFont(entitySelect!!)
            }
            root.findViewById<View>(R.id.btn_color).setOnClickListener {
                iEditSName?.onColor(entitySelect!!)
            }
            root.findViewById<View>(R.id.btn_edit).setOnClickListener {
                iEditSName?.onEdit(entitySelect!!)
            }
        }
        return root
    }

    override fun onDestroyView() {
        instance = null
        iEditSName = null
        fragmentBinding = null
        super.onDestroyView()
    }
}
