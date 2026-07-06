package hazem.nurmontage.videoquran.adapter

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.GallerySelected
import hazem.nurmontage.videoquran.views.image.SquareImageView
import hazem.nurmontage.videoquran.views.TextCustumFont

class GallerySelctedAdabters(
    resources: Resources,
    private val iGallerySelected: IGallerySelected,
    size: Int
) : RecyclerView.Adapter<GallerySelctedAdabters.MyViewHolder>() {

    private val size: Int
    private val bitmapPlaceHolder: BitmapDrawable
    private val gallerySelecteds: MutableList<GallerySelected> = ArrayList()

    interface IGallerySelected {
        fun inselectPhoto(index: Int)
        fun inselectVideo(index: Int)
    }

    init {
        this.size = size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        bitmap.eraseColor(ViewCompat.MEASURED_STATE_MASK)
        this.bitmapPlaceHolder = BitmapDrawable(resources, bitmap)
    }

    fun getGallerySelecteds(): MutableList<GallerySelected> = gallerySelecteds

    fun getSize(): Int = size

    fun addItemVideo(gallerySelected: GallerySelected) {
        gallerySelecteds.add(gallerySelected)
        gallerySelected.videoItem?.gallerySelected = gallerySelected
        notifyItemInserted(gallerySelecteds.size - 1)
    }

    fun addItemPhoto(gallerySelected: GallerySelected) {
        gallerySelecteds.add(gallerySelected)
        gallerySelected.photoItem?.gallerySelected = gallerySelected
        notifyItemInserted(gallerySelecteds.size - 1)
    }

    fun deletedItem(gallerySelected: GallerySelected) {
        val index = gallerySelecteds.indexOf(gallerySelected)
        if (index != -1) {
            gallerySelecteds.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun deletedItem(position: Int) {
        gallerySelecteds.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_gallery_select, viewGroup, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val gallerySelected = gallerySelecteds[position]
        val path: String

        if (gallerySelected.videoItem != null) {
            path = gallerySelected.videoItem!!.path
            holder.tvTime.visibility = View.VISIBLE
            holder.tvTime.text = gallerySelected.videoItem!!.time
        } else {
            path = gallerySelected.photoItem!!.path
            holder.tvTime.visibility = View.GONE
        }

        Glide.with(holder.itemView)
            .load(path)
            .override(size, size)
            .centerCrop()
            .placeholder(bitmapPlaceHolder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = gallerySelecteds.size

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: SquareImageView = itemView.findViewById(R.id.img)
        val tvTime: TextCustumFont = itemView.findViewById(R.id.tv_time)
        val btnDeleted: ImageButton = itemView.findViewById(R.id.btn_deleted)

        init {
            btnDeleted.visibility = View.VISIBLE
            tvTime.visibility = View.VISIBLE

            btnDeleted.setOnClickListener {
                val pos = adapterPosition
                if (pos < 0 || pos >= gallerySelecteds.size) return@setOnClickListener

                val gallerySelected = gallerySelecteds[pos]
                deletedItem(pos)

                if (gallerySelected.videoItem != null) {
                    iGallerySelected.inselectVideo(gallerySelected.index)
                } else {
                    iGallerySelected.inselectPhoto(gallerySelected.index)
                }
            }
        }
    }
}
