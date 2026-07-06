package hazem.nurmontage.videoquran.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.Gradient

class GradientAdabter(
    private val iColorCallback: IColor?,
    private val colors: List<Gradient>,
    selectedPos: Int
) : RecyclerView.Adapter<GradientAdabter.ViewHolder>() {

    private var posSelect: Int = selectedPos

    interface IColor {
        fun onGradient(gradient: Gradient, position: Int)
    }

    fun getSelect(): Gradient? {
        return if (posSelect >= 0 && posSelect < colors.size) colors[posSelect] else null
    }

    fun getPosSelect(): Int = posSelect

    fun setGradientBackground(
        view: ImageView,
        containerView: View,
        gradient: Gradient,
        isSelected: Boolean
    ) {
        if (isSelected) {
            val borderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                cornerRadius = 100f
                setStroke(3, -1)
            }
            containerView.background = borderDrawable
        } else {
            containerView.background = null
        }

        val gradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            cornerRadius = 100f
            colors = intArrayOf(gradient.color, gradient.second, gradient.three)
        }
        view.background = gradientDrawable
    }

    fun setGradientBackground(view: View, color: Int) {
        val drawable = GradientDrawable().apply {
            setColor(color)
            shape = GradientDrawable.OVAL
            cornerRadius = 100f
        }
        view.background = drawable
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_color, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        setGradientBackground(holder.imageView, holder.itemView, colors[position], position == posSelect)
        holder.imageLayer.visibility = View.GONE
    }

    override fun getItemCount(): Int = colors.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.image)
        val imageLayer: ImageView = itemView.findViewById(R.id.layer)

        init {
            setGradientBackground(imageLayer, -1895825408)

            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (iColorCallback == null) return@setOnClickListener
                if (posSelect == pos) return@setOnClickListener

                val prevSelected = posSelect
                posSelect = pos
                notifyItemChanged(prevSelected)
                notifyItemChanged(posSelect)
                iColorCallback.onGradient(colors[pos], pos)
            }
        }
    }
}
