package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.entity_timeline.EntityBismilahTimeline

class TransitionBismilahAdabters(
    private val iTransition: ITransition?,
    list: List<TransitionItem>,
    select: Int,
    private val entityQuranTimeline: EntityBismilahTimeline
) : RecyclerView.Adapter<TransitionBismilahAdabters.ViewHolder>() {

    private var list: List<TransitionItem> = list
    private var max: Int = list.size
    private var select: Int = select
    private var type: String = "in"

    interface ITransition {
        fun `in`(type: String, entity: EntityBismilahTimeline)
        fun `out`(type: String, entity: EntityBismilahTimeline)
        fun applyAll(entity: EntityBismilahTimeline)
        fun destroy(entity: EntityBismilahTimeline)
        fun onHideFragment(entity: EntityBismilahTimeline)
        fun playing(entity: EntityBismilahTimeline)
        fun remove(index: Int, entity: EntityBismilahTimeline)
        fun updateDurationIn(duration: Float, entity: EntityBismilahTimeline)
        fun updateDurationOut(duration: Float, entity: EntityBismilahTimeline)
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
        val disableView: ImageView = itemView.findViewById(R.id.iv_disable)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (iTransition == null || select == pos) return@setOnClickListener

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
