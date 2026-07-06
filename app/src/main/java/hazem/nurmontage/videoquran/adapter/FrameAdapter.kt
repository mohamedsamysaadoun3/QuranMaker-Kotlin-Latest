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
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.fragment.EditIpadFragment
import hazem.nurmontage.videoquran.model.IpadItem

/**
 * RecyclerView Adapter for displaying and selecting frame/template types
 * (iPad, Round Rect, Border, Gradient, Heart, Battery, Cassette, etc.).
 *
 * Originally: IpadAdabter.java (preserved typo in original package)
 * Converted to: FrameAdapter.kt — clean naming, idiomatic Kotlin
 *
 * Features:
 * - Displays frame preview thumbnails with Glide image loading
 * - Single-selection mode with alpha-based visual feedback
 * - Some frame types support dual variants (Glass/Normal) toggled by re-clicking
 * - All frames are available (billing removed)
 * - Callback interface for frame type changes
 *
 * @property posSelect Currently selected frame position
 * @property ipadSelected Currently selected [Constants.IpadType] ordinal
 * @property ipadEditCallback Callback for frame editing events
 * @property ipadItems List of available frame items
 * @property isGlass Whether the glass variant is active (for dual-option frames)
 */
class FrameAdapter(
    private var posSelect: Int,
    private var ipadSelected: Int,
    private val ipadEditCallback: EditIpadFragment.IIpadEditCallback?,
    private val ipadItems: List<IpadItem>?,
    private var isGlass: Boolean
) : RecyclerView.Adapter<FrameAdapter.ViewHolder>() {

    /**
     * Checks if the given position supports dual-option (Glass/Normal) toggle.
     * Positions 0, 1, 7, 8, 9 have dual options.
     */
    private fun isManyOption(position: Int): Boolean {
        return position == 0 || position == 1 || position == 7 || position == 8 || position == 9
    }

    /** Returns the currently selected position. */
    fun getPosSelect(): Int = posSelect

    /**
     * Updates the dot indicators to reflect Glass vs Normal state.
     * Glass: first dot full alpha, second dot half alpha.
     * Normal: reversed.
     */
    private fun updateDote(view1: View, view2: View) {
        if (isGlass) {
            view1.alpha = 1.0f
            view2.alpha = 0.5f
        } else {
            view2.alpha = 1.0f
            view1.alpha = 0.5f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_ipad, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = ipadItems?.get(position) ?: return

        // Load frame preview thumbnail
        Glide.with(holder.imageView)
            .asBitmap()
            .load(item.img)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(holder.imageView)

        // Show/hide dual-option dot indicators
        if (isManyOption(position)) {
            holder.lytOption.visibility = View.VISIBLE
            updateDote(holder.vDot1, holder.vDot2)
        } else {
            holder.lytOption.visibility = View.GONE
        }

        // Apply selection state visual feedback
        if (item.ipadType.ordinal == ipadSelected) {
            holder.itemView.alpha = 1.0f
            holder.imageView.setBackgroundResource(R.drawable.ipad_selected)
            posSelect = position
        } else {
            holder.itemView.alpha = 0.4f
            holder.imageView.setBackgroundResource(R.drawable.watch_btn_outline)
        }
    }

    override fun getItemCount(): Int = ipadItems?.size ?: 0

    /**
     * ViewHolder for frame/template items.
     * Handles click events for frame selection and glass-type toggling.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lytOption: LinearLayout = itemView.findViewById(R.id.view_option)
        val imageView: ImageView = itemView.findViewById(R.id.img)
        val vDot1: View = itemView.findViewById(R.id.dot1)
        val vDot2: View = itemView.findViewById(R.id.dot2)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                val callback = ipadEditCallback ?: return@setOnClickListener

                // Toggle glass variant if clicking the already-selected dual-option frame
                if (posSelect == pos) {
                    if (!isManyOption(pos)) return@setOnClickListener
                    isGlass = !isGlass
                    callback.onGlassType(isGlass)
                }

                // Select the new frame
                notifyItemChanged(posSelect)
                posSelect = pos
                val item = ipadItems?.get(pos) ?: return@setOnClickListener
                ipadSelected = item.ipadType.ordinal
                notifyItemChanged(posSelect)
                callback.onChangeType(ipadSelected)
            }
        }
    }
}
