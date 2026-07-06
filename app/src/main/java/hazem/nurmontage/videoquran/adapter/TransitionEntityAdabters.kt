package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.entity_timeline.EntityQuranTimeline

class TransitionEntityAdabters(
    private val iTransition: ITransition?,
    list: List<TransitionItem>,
    select: Int,
    private val entityQuranTimeline: EntityQuranTimeline
) : RecyclerView.Adapter<TransitionEntityAdabters.ViewHolder>() {

    private var list: List<TransitionItem> = list
    private var max: Int = list.size
    private var select: Int = select
    private var type: String = "in"

    interface ITransition {
        fun `in`(type: String, entity: EntityQuranTimeline)
        fun `out`(type: String, entity: EntityQuranTimeline)
        fun applyAll(index: Int, entity: EntityQuranTimeline)
        fun destroy(entity: EntityQuranTimeline)
        fun onHideFragment(entity: EntityQuranTimeline)
        fun playing(entity: EntityQuranTimeline)
        fun remove(index: Int, entity: EntityQuranTimeline)
        fun updateDurationIn(duration: Float, entity: EntityQuranTimeline)
        fun updateDurationOut(duration: Float, entity: EntityQuranTimeline)
    }

    data class TransitionItem(
        val type: String,
        val idRessource: Int,
        val angle: Int
    )

    fun getSelect(): Int = select

    fun update(list: List<TransitionItem>, type: String, select: Int) {
        this.select = select
        this.list = list
        this.type = type
        this.max = list.size
        notifyDataSetChanged()
    }

    fun isHaveSelect(): Boolean = select != -1

    fun unselect() {
        if (select == -1) return
        val old = select
        select = -1
        notifyItemChanged(old)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_anim, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.animationItem.rotation = item.angle.toFloat()
        holder.animationItem.setImageResource(item.idRessource)
        if (position == select) {
            holder.animationItem.setBackgroundResource(R.drawable.circle_item_menu_select)
        } else {
            holder.animationItem.setBackgroundResource(R.drawable.circle_effect)
        }
    }

    override fun getItemCount(): Int = max

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val animationItem: ImageView = itemView.findViewById(R.id.anim_item)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (iTransition == null) return@setOnClickListener

                if (select == pos) return@setOnClickListener

                val oldSelect = select
                select = pos
                notifyItemChanged(oldSelect)
                notifyItemChanged(select)

                val item = list[pos]
                if (type == "in") {
                    iTransition.`in`(item.type, entityQuranTimeline)
                } else if (type == "out") {
                    iTransition.`out`(item.type, entityQuranTimeline)
                }
            }
        }
    }
}
