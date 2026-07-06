package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.WordModel
import hazem.nurmontage.videoquran.views.TextCustumFont

class WordAyaAdabter(
    private var list: List<WordModel>?,
    private val iWordAya: IWordAya? = null
) : RecyclerView.Adapter<WordAyaAdabter.ViewHolder>() {

    interface IWordAya {
        fun onClick()
    }

    fun setList(newList: List<WordModel>) {
        list = newList
        notifyDataSetChanged()
    }

    fun getList(): List<WordModel>? = list

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_word_aya, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wordModel = list?.get(position) ?: return

        holder.text.text = wordModel.w

        if (wordModel.isSelected) {
            holder.text.setBackgroundResource(R.drawable.round_btn_quran_select)
            holder.text.setTextColor(-12434878)
        } else {
            holder.text.setBackgroundResource(R.drawable.round_btn_in_dark)
            holder.text.setTextColor(-1)
        }
    }

    override fun getItemCount(): Int = list?.size ?: 0

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextCustumFont = itemView.findViewById(R.id.word_aya)

        init {
            text.setOnClickListener {
                val pos = adapterPosition
                if (pos == -1) return@setOnClickListener

                list?.get(pos)?.let { word ->
                    word.isSelected = !word.isSelected
                    notifyItemChanged(pos)
                    iWordAya?.onClick()
                }
            }
        }
    }
}
