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

/**
 * RecyclerView Adapter for displaying Quran ayah search results with
 * multi-ayah selection capability.
 *
 * Originally: SearchQuranAdabters.java (preserved typo in original package)
 * Converted to: AyaAdapter.kt — clean naming, idiomatic Kotlin
 *
 * Features:
 * - Displays ayah text with highlighted search matches (SpannableString)
 * - Supports multi-ayah range selection (minSelected ↔ maxSelected)
 * - Selection highlight with colored background
 * - Callback interface for click events with selected range info
 *
 * @property callback Interface for ayah selection events
 */
class AyaAdapter(
    private val callback: ISearchQuranCallback
) : RecyclerView.Adapter<AyaAdapter.ViewHolder>() {

    /** Callback interface for ayah search result interactions. */
    interface ISearchQuranCallback {
        /**
         * Called when an ayah is clicked.
         * @param minSelected The minimum selected position in the range
         * @param maxSelected The maximum selected position in the range
         * @param item The clicked search result item
         */
        fun onClick(minSelected: Int, maxSelected: Int, item: ItemQuranSearch)
    }

    private val searchList = mutableListOf<ItemQuranSearch>()
    private var minSelected = -1
    private var maxSelected = -1

    /** Returns the number of items currently in the search list. */
    val size: Int get() = searchList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_search_quran, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = searchList[position]

        if (item.aya != null) {
            // Show surah name with ayah number
            holder.name.text = "${item.surahName} (${item.to})"

            // Apply spannable highlighting if search match range is specified
            if (item.startSpannable != -1) {
                val spannableString = SpannableString(item.aya)
                spannableString.setSpan(
                    ForegroundColorSpan(-10929), // Highlight color (teal accent)
                    item.startSpannable,
                    item.endSpannble,
                    SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.aya.text = spannableString
            } else {
                holder.aya.text = item.aya
            }
        } else {
            // No ayah text — show surah index only
            holder.name.text = item.surahIndex.toString()
        }

        // Apply selection highlight background
        val isSelected = minSelected != -1 && position in minSelected..maxSelected
        holder.itemView.setBackgroundColor(if (isSelected) -14540254 else 0)
    }

    override fun getItemCount(): Int = searchList.size

    /**
     * Replaces the entire search list and notifies observers.
     * @param list The new list of search results
     */
    fun setList(list: List<ItemQuranSearch>) {
        searchList.clear()
        searchList.addAll(list)
        notifyDataSetChanged()
    }

    /**
     * Appends a single item to the end of the search list.
     * @param item The search result item to add
     */
    fun add(item: ItemQuranSearch) {
        searchList.add(item)
        notifyItemInserted(searchList.size - 1)
    }

    /**
     * Removes all items from the search list.
     * Does nothing if the list is already empty.
     */
    fun clear() {
        val size = searchList.size
        if (size == 0) return
        searchList.clear()
        notifyItemRangeRemoved(0, size)
    }

    /** Returns the minimum selected position in the current range. */
    fun getMinSelected(): Int = minSelected

    /** Returns the maximum selected position in the current range. */
    fun getMaxSelected(): Int = maxSelected

    /**
     * ViewHolder for ayah search result items.
     * Handles click events for multi-ayah range selection.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_surah_name_and_number)
        val aya: TextView = itemView.findViewById(R.id.tv_surah)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos == -1) return@setOnClickListener

                // Update selection range
                when {
                    minSelected == -1 -> {
                        minSelected = pos
                        maxSelected = pos
                    }
                    pos < minSelected -> {
                        minSelected = pos
                    }
                    pos > maxSelected -> {
                        maxSelected = pos
                    }
                    else -> {
                        // Clicked within existing range — collapse to single selection
                        minSelected = pos
                        maxSelected = pos
                    }
                }

                notifyDataSetChanged()
                callback.onClick(minSelected, maxSelected, searchList[pos])
            }
        }
    }
}
