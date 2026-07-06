package hazem.nurmontage.videoquran.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.Gradient

/**
 * RecyclerView Adapter for displaying and selecting gradient/color presets.
 *
 * Originally: GradientAdabter.java (preserved typo in original package)
 * Converted to: PresetAdapter.kt — clean naming, idiomatic Kotlin
 *
 * Features:
 * - Displays gradient previews as rounded pill shapes
 * - Single-selection mode with stroke highlight border
 * - All gradients are available (billing removed)
 * - Static utility method [setGradientBackground] for applying gradient
 *   backgrounds to any View (used externally by other components)
 * - Callback delivers the selected gradient and its position
 *
 * @property iColorCallback Callback for gradient selection events
 * @property colors List of available gradient presets
 * @property posSelect Currently selected gradient position
 */
class PresetAdapter(
    private var iColorCallback: IColor?,
    private val colors: List<Gradient>?,
    private var posSelect: Int
) : RecyclerView.Adapter<PresetAdapter.ViewHolder>() {

    /** Callback interface for gradient preset selection events. */
    interface IColor {
        /**
         * Called when a gradient preset is selected.
         * @param gradient The selected gradient
         * @param position The adapter position of the selection
         */
        fun onGradient(gradient: Gradient, position: Int)
    }

    /** Returns the currently selected gradient, or null if none selected. */
    fun getSelect(): Gradient? {
        return if (posSelect >= 0 && colors != null && posSelect < colors.size) {
            colors[posSelect]
        } else null
    }

    /** Returns the currently selected position. */
    fun getPosSelect(): Int = posSelect

    /**
     * Applies a gradient background to [view] with optional selection stroke on [view2].
     * Used both internally by the adapter and externally by other components.
     *
     * @param view The view to apply the gradient background to
     * @param view2 The view to apply the selection stroke to (typically itemView)
     * @param gradient The gradient data (3 color stops)
     * @param isSelected Whether this item is currently selected (shows white stroke)
     */
    fun setGradientBackground(view: View, view2: View, gradient: Gradient, isSelected: Boolean) {
        // Selection border: white stroke on the container
        if (isSelected) {
            val strokeDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                cornerRadius = 100f
                setStroke(3, -1) // White 3px stroke
            }
            view2.background = strokeDrawable
        } else {
            view2.background = null
        }

        // Gradient fill on the color preview
        val gradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            cornerRadius = 100f
            colors = intArrayOf(gradient.color, gradient.second, gradient.three)
        }
        view.background = gradientDrawable
    }

    /**
     * Applies a solid color background to [view] as a rounded pill shape.
     * Utility method for single-color backgrounds.
     *
     * @param view The view to apply the solid color background to
     * @param color The color value
     */
    fun setGradientBackground(view: View, color: Int) {
        val drawable = GradientDrawable().apply {
            setColor(color)
            shape = GradientDrawable.OVAL
            cornerRadius = 100f
        }
        view.background = drawable
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_color, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gradient = colors?.get(position) ?: return
        setGradientBackground(holder.imageView, holder.itemView, gradient, position == posSelect)
        holder.imageLayer.visibility = View.GONE
    }

    override fun getItemCount(): Int = colors?.size ?: 0

    /**
     * ViewHolder for gradient preset items.
     * Handles click events for gradient selection.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val imageLayer: ImageView = itemView.findViewById(R.id.layer)

        init {
            // Apply default background tint
            setGradientBackground(imageView, -1895825408)

            itemView.setOnClickListener {
                val callback = iColorCallback ?: return@setOnClickListener
                val pos = adapterPosition

                // Skip if already selected
                if (posSelect == pos) return@setOnClickListener

                val oldPos = posSelect
                posSelect = pos
                notifyItemChanged(oldPos)
                notifyItemChanged(posSelect)

                colors?.get(pos)?.let { gradient ->
                    callback.onGradient(gradient, pos)
                }
            }
        }
    }
}
