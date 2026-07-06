package hazem.nurmontage.videoquran.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R

class ColorBgAdabter(
    private val iColorCallback: IColor?,
    private val colors: IntArray,
    selectedPos: Int
) : RecyclerView.Adapter<ColorBgAdabter.ViewHolder>() {

    private var posSelect: Int = selectedPos
    private var enabled: Boolean = true

    interface IColor {
        fun onColor(color: Int, position: Int)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun getPosSelect(): Int = posSelect

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_color, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        setGradientBackground(
            holder.imageView,
            holder.itemView,
            colors[position],
            position == posSelect
        )
    }

    override fun getItemCount(): Int = colors.size

    private fun setGradientBackground(
        view: ImageView,
        containerView: View,
        color: Int,
        isSelected: Boolean
    ) {
        if (isSelected) {
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                cornerRadius = 10f
                setStroke(3, -1)
            }
            containerView.background = borderDrawable
        } else {
            containerView.background = null
        }

        val colorDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            cornerRadius = 10f
            setColor(color)
        }
        view.background = colorDrawable
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.image)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (iColorCallback == null || posSelect == pos || !enabled) return@setOnClickListener

                val prevSelected = posSelect
                posSelect = pos
                notifyItemChanged(prevSelected)
                notifyItemChanged(posSelect)
                iColorCallback.onColor(colors[pos], pos)
            }
        }
    }
}
