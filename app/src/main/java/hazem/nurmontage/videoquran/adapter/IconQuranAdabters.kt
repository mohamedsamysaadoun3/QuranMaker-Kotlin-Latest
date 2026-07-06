package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.utils.DrawableHelper

class IconQuranAdabters(
    private val iconQuranCallback: IIconQuranCallback?,
    private val list: List<String>,
    selected: Int
) : RecyclerView.Adapter<IconQuranAdabters.ViewHolder>() {

    private var select: Int = if (selected < list.size) selected else 0

    interface IIconQuranCallback {
        fun onIcon(iconName: String)
    }

    fun getSelect(): Int = select

    fun isHaveSelect(): Boolean = select != -1

    fun unselect() {
        if (select == -1) return
        val prevSelect = select
        select = -1
        notifyItemChanged(prevSelect)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_anim, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.animationItem.setImageResource(
            DrawableHelper.getIDDrawableIconByName(list[position])
        )

        if (position == select) {
            holder.animationItem.setBackgroundResource(R.drawable.circle_item_menu_select)
        } else {
            holder.animationItem.setBackgroundResource(R.drawable.circle_effect)
        }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val animationItem: ImageView = itemView.findViewById(R.id.anim_item)
        val disableView: ImageView = itemView.findViewById(R.id.iv_disable)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (iconQuranCallback == null || select == pos) return@setOnClickListener

                val prevSelect = select
                select = pos
                notifyItemChanged(prevSelect)
                notifyItemChanged(select)
                iconQuranCallback.onIcon(list[pos])
            }
        }
    }
}
