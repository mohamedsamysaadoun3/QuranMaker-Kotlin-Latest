package hazem.nurmontage.videoquran.adapter

import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.ResizeType
import hazem.nurmontage.videoquran.model.ItemDimension
import hazem.nurmontage.videoquran.views.TextCustumFont

class DimensionAdabters(
    private var mDimensionList: List<ItemDimension>?,
    private var mIDimensionCallback: IDimensionCallback?,
    private val listDim: List<Pair<Int, Int>>,
    private var selected: Int = 0
) : RecyclerView.Adapter<DimensionAdabters.ViewHolder>() {

    interface IDimensionCallback {
        fun done()
        fun isCustomSize(isCustom: Boolean, resizeType: ResizeType)
        fun onCustumSize(w: Int, h: Int, resizeTypeOrdinal: Int, id: String, image: Int)
    }

    fun setSelected(position: Int) {
        selected = position
    }

    fun getSelected(): Int = selected

    fun get(): Int = mDimensionList?.get(getSelected())?.resizeType?.ordinal
        ?: ResizeType.SQUARE.ordinal

    fun getResizeSelected(): ResizeType =
        mDimensionList?.get(getSelected())?.resizeType ?: ResizeType.SQUARE

    fun update(list: List<ItemDimension>) {
        mDimensionList?.let { (it as? MutableList)?.clear() }
        mDimensionList = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_aspect, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dimension = mDimensionList?.get(position) ?: return
        val pair = listDim[position]

        holder.layout.layoutParams.width = pair.first
        holder.layout.layoutParams.height = pair.second

        val parts = dimension.name.split("\n")
        holder.name.text = parts[0]
        if (parts.size > 1) {
            holder.dimension.text = parts[1]
        }

        Glide.with(holder.itemView)
            .asBitmap()
            .centerInside()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .load(dimension.image)
            .into(holder.imageView)

        if (position == selected) {
            holder.layout.setBackgroundResource(R.drawable.rect_btn_select)
        } else {
            holder.layout.setBackgroundResource(R.drawable.rect_btn)
        }
    }

    override fun getItemCount(): Int = mDimensionList?.size ?: 0

    fun clear() {
        mDimensionList?.let { (it as? MutableList)?.clear() }
        mDimensionList = null
        mIDimensionCallback = null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layout: FrameLayout = itemView.findViewById(R.id.layout)
        val imageView: ImageView = itemView.findViewById(R.id.icon)
        val name: TextCustumFont = itemView.findViewById(R.id.aspect_name)
        val dimension: TextCustumFont = itemView.findViewById(R.id.aspect_size)

        init {
            itemView.setOnClickListener {
                val callback = mIDimensionCallback ?: return@setOnClickListener
                val pos = adapterPosition
                if (pos == -1) return@setOnClickListener

                val oldSelected = selected
                selected = pos
                notifyItemChanged(oldSelected)
                notifyItemChanged(selected)

                mDimensionList?.get(pos)?.let { item ->
                    callback.onCustumSize(
                        item.w,
                        item.h,
                        item.resizeType.ordinal,
                        item.id,
                        item.image
                    )
                }
            }
        }
    }
}
