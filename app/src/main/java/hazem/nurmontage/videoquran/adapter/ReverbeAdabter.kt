package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.fragment.audio_effect.Reverbe
import hazem.nurmontage.videoquran.views.TextCustumFont

class ReverbeAdabter(
    private val list: List<Reverbe>,
    private val iReverbCallback: IReverbPresetCallback?,
    selected: Int
) : RecyclerView.Adapter<ReverbeAdabter.ViewHolder>() {

    private var select: Int = selected

    interface IReverbPresetCallback {
        fun cmd(cmdFfmpeg: String?, position: Int)
        fun pause()
    }

    fun getList(): List<Reverbe> = list

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_reverbe, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = list[position].name

        if (select == position) {
            holder.itemView.setBackgroundResource(R.drawable.item_reverb_select)
            holder.ivBtnPlay.setImageResource(R.drawable.pause_24px)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.round_btn_in_dark)
            holder.ivBtnPlay.setImageResource(R.drawable.play_arrow_24px)
        }
    }

    override fun getItemCount(): Int = list.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val text: TextCustumFont = itemView.findViewById(R.id.word_aya)
        val ivBtnPlay: ImageView = itemView.findViewById(R.id.iv_btn_play)

        init {
            itemView.setOnClickListener {
                if (iReverbCallback == null) return@setOnClickListener

                iReverbCallback.pause()

                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (select == pos) {
                    val prevSelect = select
                    select = -1
                    notifyItemChanged(prevSelect)
                    notifyItemChanged(pos)
                } else {
                    val prevSelect = select
                    select = pos
                    if (prevSelect != -1) {
                        notifyItemChanged(prevSelect)
                    }
                    notifyItemChanged(select)
                    iReverbCallback.cmd(list[pos].cmdFfmpeg, pos)
                }
            }
        }
    }
}
