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
import hazem.nurmontage.videoquran.model.VideoItem
import hazem.nurmontage.videoquran.views.image.SquareImageView
import hazem.nurmontage.videoquran.views.TextCustumFont

class GalleryVideoAdabters(
    private val appVersion: String,
    resources: Resources,
    private val gallerySelectedList: List<GallerySelected>?,
    size: Int,
    private val iPicker: IPicker?
) : RecyclerView.Adapter<GalleryVideoAdabters.MyViewHolder>() {

    private val size: Int
    private val bitmapPlaceHolder: BitmapDrawable
    private var videoItems: MutableList<VideoItem>? = null
    private var allVideoItems: List<VideoItem>? = null
    private var videoItemSelect: VideoItem? = null

    init {
        this.size = size
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        bitmap.eraseColor(ViewCompat.MEASURED_STATE_MASK)
        this.bitmapPlaceHolder = BitmapDrawable(resources, bitmap)
    }

    fun addItems(list: List<VideoItem>?) {
        videoItems = list?.toMutableList()
        if (iPicker != null && list.isNullOrEmpty()) {
            iPicker.onEmptyList()
        }
    }

    fun doneItems(list: List<VideoItem>) {
        videoItems = list.toMutableList()
        allVideoItems = ArrayList(list)
    }

    fun updateAll() {
        if (allVideoItems == null || videoItems == null) return
        videoItems!!.clear()
        videoItems = ArrayList(allVideoItems!!)
        notifyDataSetChanged()
    }

    fun update(folderPath: String) {
        val currentItems = videoItems ?: return
        currentItems.clear()
        for (videoItem in allVideoItems ?: return) {
            if (videoItem.folderPath == folderPath) {
                currentItems.add(videoItem)
            }
        }
        notifyDataSetChanged()
    }

    fun setFolder(folder: String) {
        notifyDataSetChanged()
    }

    fun inselectItem(position: Int) {
        val currentItems = videoItems ?: return
        if (position >= currentItems.size) return
        val videoItem = currentItems[position]
        videoItem.isSelect = false
        notifyItemChanged(position)
        updateNumbers(videoItem.number)
    }

    fun clear() {
        videoItems?.clear()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.row_gallery, viewGroup, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentItems = videoItems ?: return
        val videoItem = currentItems[position]

        holder.imageView.setNumber(videoItem.number)
        holder.imageView.onSelect(videoItem.isSelect)

        Glide.with(holder.itemView)
            .load(videoItem.path)
            .override(size, size)
            .centerCrop()
            .signature(ObjectKey(appVersion))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .placeholder(bitmapPlaceHolder)
            .into(holder.imageView)

        holder.tvTime.text = videoItem.time
    }

    override fun getItemCount(): Int = videoItems?.size ?: 0

    fun updateNumbers(removedNumber: Int) {
        val selectedList = gallerySelectedList ?: return
        var i = removedNumber
        while (i < selectedList.size) {
            val gallerySelected = selectedList[i]

            gallerySelected.videoItem?.let { videoItem ->
                videoItem.number -= 1
                notifyItemChanged(videoItem.adabter_pos)
            }

            gallerySelected.photoItem?.let { photoItem ->
                photoItem.number -= 1
            }
            i++
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imageView: SquareImageView = itemView.findViewById(R.id.img)
        val tvTime: TextCustumFont = itemView.findViewById(R.id.tv_time)

        init {
            tvTime.visibility = View.VISIBLE

            itemView.setOnClickListener {
                if (iPicker == null || adapterPosition < 0) return@setOnClickListener

                val currentItems = videoItems ?: return@setOnClickListener

                if (gallerySelectedList == null) {
                    val videoItem = currentItems[adapterPosition]

                    if (videoItem === videoItemSelect) return@setOnClickListener

                    videoItemSelect?.let { prev ->
                        prev.isSelect = false
                        notifyItemChanged(prev.adabter_pos)
                    }

                    videoItemSelect = videoItem
                    videoItem.isSelect = true
                    imageView.onSelect(true)
                    videoItem.adabter_pos = adapterPosition
                    iPicker.onAdd(videoItem, adapterPosition)
                } else {
                    val videoItem = currentItems[adapterPosition]

                    videoItem.isSelect = !videoItem.isSelect
                    imageView.onSelect(videoItem.isSelect)

                    if (videoItem.isSelect) {
                        imageView.setNumber(gallerySelectedList.size + 1)
                        videoItem.number = imageView.getAnInt()
                        videoItem.adabter_pos = adapterPosition
                        iPicker.onAdd(videoItem, adapterPosition)
                    } else {
                        updateNumbers(imageView.getAnInt())
                        videoItem.gallerySelected?.let { iPicker.onDelete(it) }
                    }
                }
            }
        }
    }
}
