package hazem.nurmontage.videoquran.adapter

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.ItemQuranSearch

class SearchQuranAdabters(
    private val callback: ISearchQuranCallback?
) : RecyclerView.Adapter<SearchQuranAdabters.ViewHolder>() {

    private val searchList: MutableList<ItemQuranSearch> = ArrayList()
    private var minSelected: Int = -1
    private var maxSelected: Int = -1

    interface ISearchQuranCallback {
        fun onClick(minSelected: Int, maxSelected: Int, item: ItemQuranSearch)
    }

    fun getSize(): Int = searchList.size

    fun getMinSelected(): Int = minSelected

    fun getMaxSelected(): Int = maxSelected

    fun setList(list: List<ItemQuranSearch>) {
        searchList.clear()
        searchList.addAll(list)
        notifyDataSetChanged()
    }

    fun add(item: ItemQuranSearch) {
        searchList.add(item)
        notifyItemInserted(searchList.size - 1)
    }

    fun clear() {
        val size = searchList.size
        if (size == 0) return
        searchList.clear()
        notifyItemRangeRemoved(0, size)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_search_quran, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = searchList[position]

        if (item.aya != null) {
            holder.name.text = "${item.surahName} (${item.to})"

            if (item.startSpannable != -1) {
                val spannableString = SpannableString(item.aya)
                spannableString.setSpan(
                    ForegroundColorSpan(-10929),
                    item.startSpannable,
                    item.endSpannble,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.aya.text = spannableString
            } else {
                holder.aya.text = item.aya
            }
        } else {
            holder.name.text = item.surahIndex.toString()
        }

        val isInRange = minSelected != -1 && position >= minSelected && position <= maxSelected
        holder.itemView.setBackgroundColor(if (isInRange) -14540254 else 0)
    }

    override fun getItemCount(): Int = searchList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val name: TextView = itemView.findViewById(R.id.tv_surah_name_and_number)
        val aya: TextView = itemView.findViewById(R.id.tv_surah)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (minSelected == -1) {
                    minSelected = pos
                    maxSelected = pos
                } else if (pos < minSelected) {
                    minSelected = pos
                } else if (pos > maxSelected) {
                    maxSelected = pos
                } else {
                    minSelected = pos
                    maxSelected = pos
                }

                notifyDataSetChanged()
                callback?.onClick(minSelected, maxSelected, searchList[pos])
            }
        }
    }
}
