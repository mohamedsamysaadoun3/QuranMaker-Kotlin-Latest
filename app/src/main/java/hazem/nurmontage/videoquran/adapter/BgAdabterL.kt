package hazem.nurmontage.videoquran.adapter

import android.graphics.Bitmap
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

class BgAdabterL(
    private val appVersion: String,
    private val iBgCallback: IBgCallback?,
    private val images: List<BgItem>,
    private val size: Int
) : RecyclerView.Adapter<BgAdabterL.ViewHolder>() {

    private var selected: Int = 0

    fun add(bgItem: BgItem) {
        try {
            (images as? MutableList)?.add(bgItem)
            notifyItemInserted(images.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPosSelect(): Int = selected

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_img_bg, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.imageView)
            .asBitmap()
            .load(images[position].id)
            .override(size, size)
            .signature(ObjectKey(appVersion))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .transform(
                MultiTransformation(
                    CenterCrop(),
                    RoundedCornersTransformation(8, 0, RoundedCornersTransformation.CornerType.ALL)
                )
            )
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.img)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

                if (iBgCallback != null) {
                    selected = pos
                    iBgCallback.onAdd(images[pos])
                }
            }
        }
    }
}
