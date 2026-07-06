package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.FontProvider
import hazem.nurmontage.videoquran.fragment.FontFragment
import hazem.nurmontage.videoquran.views.TextCustumFont

class FontAdapter(
    private val fontProvider: FontProvider,
    private var fontCallback: FontFragment.IFontCallback?,
    private val fontList: List<String>?,
    private var selected: Int
) : RecyclerView.Adapter<FontAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_font, parent, false)
        return ViewHolder(view)
    }

    fun setSelected(index: Int) {
        try {
            val oldSelected = selected
            selected = index
            notifyItemChanged(oldSelected)
            notifyItemChanged(selected)

            fontList?.let { list ->
                val fontName = list[index]
                fontCallback?.onAdd(fontProvider.getFullName(fontName), fontProvider.getTypeface(fontName))
            }
        } catch (_: Exception) {
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        fontList?.let { list ->
            val fontName = list[position]
            holder.nameFont.text = fontName
            holder.tvNumber.text = (position + 1).toString()

            try {
                holder.nameFont.typeface = fontProvider.getTypeface(fontName)

                if (selected == position) {
                    holder.nameFont.setTextColor(-14540254)
                    holder.nameFont.setBackgroundResource(R.drawable.btn_item_font_state)
                } else {
                    holder.nameFont.setTextColor(-1)
                    holder.nameFont.background = null
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun getItemCount(): Int = fontList?.size ?: 0

    fun clear() {
        fontCallback = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameFont: TextCustumFont = itemView.findViewById(R.id.tv_font)
        val tvNumber: TextCustumFont = itemView.findViewById(R.id.tv_number)

        init {
            nameFont.setOnClickListener {
                val callback = fontCallback ?: return@setOnClickListener
                val pos = adapterPosition
                if (selected == pos) return@setOnClickListener

                val oldSelected = selected
                selected = pos
                notifyItemChanged(oldSelected)
                notifyItemChanged(selected)

                fontList?.let { list ->
                    val fontName = list[selected]
                    callback.onAdd(fontProvider.getFullName(fontName), fontProvider.getTypeface(fontName))
                }
            }
        }
    }
}
