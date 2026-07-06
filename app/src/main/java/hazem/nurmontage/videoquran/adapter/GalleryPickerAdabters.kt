package hazem.nurmontage.videoquran.adapter

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.GallerySelected
import hazem.nurmontage.videoquran.model.PhotoItem
import hazem.nurmontage.videoquran.views.image.SquareImageView

class GalleryPickerAdabters(
    private val appVersion: String,
    resources: Resources,
    private val gallerySelectedList: List<GallerySelected>?,
    size: Int,
    private val iPicker: IPicker?
) : RecyclerView.Adapter<GalleryPickerAdabters.MyViewHolder>() {

    private val size: Int
    private val bitmapPlaceHolder: BitmapDrawable
    private var paths: MutableList<PhotoItem>? = null
    private var allPaths: List<PhotoItem>? = null
    private var photoItemSelected: PhotoItem? = null

    init {
        this.size = size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        bitmap.eraseColor(ViewCompat.MEASURED_STATE_MASK)
        this.bitmapPlaceHolder = BitmapDrawable(resources, bitmap)
    }

    fun addItems(list: List<PhotoItem>?) {
        paths = list?.toMutableList()
        if (iPicker != null && list.isNullOrEmpty()) {
            iPicker.onEmptyList()
        }
    }

    fun doneItems(list: List<PhotoItem>) {
        paths = list.toMutableList()
        allPaths = ArrayList(list)
    }

    fun updateAll() {
        if (allPaths == null || paths == null) return
        paths!!.clear()
        paths = ArrayList(allPaths!!)
        notifyDataSetChanged()
    }

    fun update(folder: String) {
        val currentPaths = paths ?: return
        currentPaths.clear()
        for (photoItem in allPaths ?: return) {
            if (photoItem.folder == folder) {
                currentPaths.add(photoItem)
            }
        }
        notifyDataSetChanged()
    }

    fun inselectItem(position: Int) {
        val currentPaths = paths ?: return
        if (position >= currentPaths.size) return
        val photoItem = currentPaths[position]
        photoItem.isSelect = false
        notifyItemChanged(position)
        updateNumbers(photoItem.number)
    }

    fun clear() {
        paths?.clear()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_gallery, viewGroup, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentPaths = paths ?: return
        val photoItem = currentPaths[position]

        holder.imageView.setNumber(photoItem.number)
        holder.imageView.onSelect(photoItem.isSelect)

        Glide.with(holder.itemView)
            .load(photoItem.path)
            .override(size, size)
            .centerCrop()
            .signature(ObjectKey(appVersion))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .placeholder(bitmapPlaceHolder)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = paths?.size ?: 0

    fun updateNumbers(removedNumber: Int) {
        val selectedList = gallerySelectedList ?: return
        var i = removedNumber
        while (i < selectedList.size) {
            val photoItem = selectedList[i].photoItem
            if (photoItem != null) {
                photoItem.number -= 1
                notifyItemChanged(photoItem.adabter_pos)
            }
            i++
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: SquareImageView = itemView.findViewById(R.id.img)

        init {
            itemView.setOnClickListener {
                if (iPicker == null || adapterPosition < 0) return@setOnClickListener

                val currentPaths = paths ?: return@setOnClickListener

                if (gallerySelectedList == null) {
                    val photoItem = currentPaths[adapterPosition]

                    if (photoItem === photoItemSelected) return@setOnClickListener

                    photoItemSelected?.let { prev ->
                        prev.isSelect = false
                        notifyItemChanged(prev.adabter_pos)
                    }

                    photoItem.isSelect = true
                    imageView.onSelect(true)
                    photoItemSelected = photoItem
                    photoItem.adabter_pos = adapterPosition
                    iPicker.onAdd(photoItem, adapterPosition)
                } else {
                    val photoItem = currentPaths[adapterPosition]

                    photoItem.isSelect = !photoItem.isSelect
                    imageView.onSelect(photoItem.isSelect)

                    if (photoItem.isSelect) {
                        imageView.setNumber(gallerySelectedList.size + 1)
                        photoItem.number = imageView.getAnInt()
                        photoItem.adabter_pos = adapterPosition
                        iPicker.onAdd(photoItem, adapterPosition)
                    } else {
                        updateNumbers(imageView.getAnInt())
                        photoItem.gallerySelected?.let { iPicker.onDelete(it) }
                    }
                }
            }
        }
    }
}
