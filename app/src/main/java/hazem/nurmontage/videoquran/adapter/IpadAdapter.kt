package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.fragment.EditIpadFragment
import hazem.nurmontage.videoquran.model.IpadItem

class IpadAdapter(
    private var posSelect: Int,
    private var ipadSelected: Int,
    val ipadEditCallback: EditIpadFragment.IIpadEditCallback?,
    private val ipadItems: List<IpadItem>?,
    private var isGlass: Boolean
) : RecyclerView.Adapter<IpadAdapter.ViewHolder>() {

    private fun isManyOption(position: Int): Boolean {
        return position == 0 || position == 1 || position == 7 || position == 8 || position == 9
    }

    fun getPosSelect(): Int = posSelect

    private fun updateDote(view1: View, view2: View) {
        if (isGlass) {
            view1.alpha = 1.0f
            view2.alpha = 0.5f
        } else {
            view2.alpha = 1.0f
            view1.alpha = 0.5f
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.img)
        val ivPro: ImageView = itemView.findViewById(R.id.iv_pro)
        val lytOption: LinearLayout = itemView.findViewById(R.id.view_option)
        val vDot1: View = itemView.findViewById(R.id.dot1)
        val vDot2: View = itemView.findViewById(R.id.dot2)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                // If clicking the already-selected item with glass toggle option
                if (posSelect == pos) {
                    if (!isManyOption(pos)) return@setOnClickListener
                    isGlass = !isGlass
                    ipadEditCallback?.onGlassType(isGlass)
                }

                if (ipadEditCallback != null && ipadItems != null) {
                    val ipadItem = ipadItems[pos]
                    notifyItemChanged(posSelect)
                    posSelect = pos
                    ipadSelected = ipadItem.ipadType.ordinal
                    notifyItemChanged(posSelect)
                    ipadEditCallback.onChangeType(ipadSelected)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_ipad, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val items = ipadItems ?: return
        val ipadItem = items[position]

        Glide.with(holder.imageView)
            .asBitmap()
            .load(ipadItem.img)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(holder.imageView)

        if (isManyOption(position)) {
            holder.lytOption.visibility = View.VISIBLE
            updateDote(holder.vDot1, holder.vDot2)
        } else {
            holder.lytOption.visibility = View.GONE
        }

        if (ipadItem.ipadType.ordinal == ipadSelected) {
            holder.itemView.alpha = 1.0f
            holder.imageView.setBackgroundResource(R.drawable.ipad_selected)
            posSelect = position
        } else {
            holder.itemView.alpha = 0.4f
            holder.imageView.setBackgroundResource(R.drawable.watch_btn_outline)
        }

        holder.ivPro.visibility = View.GONE
    }

    override fun getItemCount(): Int = ipadItems?.size ?: 0
}
