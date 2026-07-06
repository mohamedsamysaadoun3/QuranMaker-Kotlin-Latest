package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.utils.MFileUtils

class WorkUserAdapter(
    private val appVersion: String,
    private val images: List<Template>,
    val iWorkUserCallback: IWorkUserCallback?,
    private val w: Int,
    private val h: Int
) : RecyclerView.Adapter<WorkUserAdapter.ViewHolder>() {

    interface IWorkUserCallback {
        fun onClick(template: Template)
        fun toMenu(template: Template, view: View, position: Int)
    }

    fun remove(position: Int) {
        try {
            if (position < images.size) {
                (images as MutableList).removeAt(position)
            }
            notifyItemRemoved(position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun add(position: Int, template: Template) {
        try {
            if (position < images.size) {
                (images as MutableList).add(position, template)
            } else {
                (images as MutableList).add(template)
            }
            notifyItemInserted(position)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val btnMenu: ImageButton = itemView.findViewById(R.id.btn_menu)
        val ivRatio: ImageView = itemView.findViewById(R.id.iv_ratio)
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)

        init {
            btnMenu.setOnClickListener {
                val pos = adapterPosition
                if (iWorkUserCallback != null && pos != RecyclerView.NO_POSITION) {
                    iWorkUserCallback.toMenu(images[pos], it, pos)
                }
            }
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (iWorkUserCallback != null && pos != RecyclerView.NO_POSITION) {
                    iWorkUserCallback.onClick(images[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_work_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val template = images[position]
        if (template.fileInfo is MFileUtils.FileInfo) {
            holder.tvName.text = (template.fileInfo as MFileUtils.FileInfo).formattedDate
            holder.tvDate.text = (template.fileInfo as MFileUtils.FileInfo).timedDate
        }
        Glide.with(holder.imageView)
            .asBitmap()
            .load(template.uri_video)
            .frame(1000000L)
            .centerInside()
            .override(w, h)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .signature(ObjectKey(appVersion))
            .placeholder(R.drawable.broken_image_24px)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size
}
