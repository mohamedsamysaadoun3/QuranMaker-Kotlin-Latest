package hazem.nurmontage.videoquran.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R

class ColorAdapter(
    private val iColorCallback: IColor?,
    private val colors: IntArray,
    private var posSelect: Int
) : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {

    interface IColor {
        fun onColor(color: Int, position: Int)
    }

    var enabled: Boolean = true

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (iColorCallback == null || posSelect == pos || !enabled) return@setOnClickListener
                val oldPos = posSelect
                posSelect = pos
                notifyItemChanged(oldPos)
                notifyItemChanged(posSelect)
                iColorCallback.onColor(colors[pos], pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_color, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        setGradientBackground(holder.imageView, holder.itemView, colors[position], position == posSelect)
    }

    override fun getItemCount(): Int = colors?.size ?: 0

    fun getPosSelect(): Int = posSelect

    private fun setGradientBackground(imageView: View, itemView: View, color: Int, isSelected: Boolean) {
        // Selected state: white stroke on the item background
        // Java: setShape(0) = RECTANGLE, setCornerRadius(10.0f), setStroke(3, -1)
        if (isSelected) {
            val selectedBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE // Java: setShape(0)
                cornerRadius = 10.0f               // Java: setCornerRadius(10.0f)
                setStroke(3, -0x1)                 // Java: setStroke(3, -1) white
            }
            itemView.background = selectedBg
        } else {
            itemView.background = null
        }

        // Color swatch
        // Java: setShape(0) = RECTANGLE, setCornerRadius(10.0f), setColor(color)
        val colorBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE // Java: setShape(0)
            cornerRadius = 10.0f               // Java: setCornerRadius(10.0f)
            setColor(color)
        }
        imageView.background = colorBg
    }
}
