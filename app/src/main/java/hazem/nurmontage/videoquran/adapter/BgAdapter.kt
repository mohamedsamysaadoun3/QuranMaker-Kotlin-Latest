package hazem.nurmontage.videoquran.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.BgItem
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class BgAdapter(
    private val appVersion: String,
    private val iBgCallback: IBgCallback?,
    private val images: List<BgItem>,
    private val size: Int,
    selected: Int
) : RecyclerView.Adapter<BgAdapter.ViewHolder>() {

    private var selected: Int = selected

    init {
        setHasStableIds(true)
    }

    fun add(bgItem: BgItem) {
        val insertPos = images.size
        (images as? MutableList)?.add(bgItem)
        notifyItemInserted(insertPos)
    }

    fun getSelectedPosition(): Int = selected

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_img_bg, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        applyState(holder, position)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            applyState(holder, position)
        } else {
            Glide.with(holder.imageView)
                .load(images[position].id)
                .override(size, size)
                .signature(ObjectKey(appVersion))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transform(
                    MultiTransformation(
                        CenterCrop(),
                        RoundedCornersTransformation(10, 8)
                    )
                )
                .into(holder.imageView)
            applyState(holder, position)
        }
    }

    override fun getItemCount(): Int = images.size

    override fun getItemId(position: Int): Long = images[position].id.toLong()

    private fun applyState(holder: ViewHolder, position: Int) {
        val isSelected = position == selected
        holder.imageView.alpha = if (isSelected) 1.0f else 0.65f

        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.ipad_selected)
        } else {
            holder.itemView.setBackgroundColor(0)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.img)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (pos == selected) {
                    iBgCallback?.onAdd(images[pos])
                    return@setOnClickListener
                }

                val prevSelected = selected
                selected = pos

                if (prevSelected != -1) {
                    notifyItemChanged(prevSelected, "alpha")
                }
                notifyItemChanged(selected, "alpha")

                iBgCallback?.onAdd(images[pos])
            }
        }
    }
}
