package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.ExploreItem
import hazem.nurmontage.videoquran.views.image.SquareImageViewSimple
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.io.File

class ExploreAdabters(
    private val exploreItems: List<ExploreItem>?,
    private val size: Int,
    private val iExplore: IExplore?,
    private val folderSelect: String
) : RecyclerView.Adapter<ExploreAdabters.MyViewHolder>() {

    interface IExplore {
        fun done()
        fun folder(folder: File, name: String, path: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_explore, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = exploreItems?.get(position) ?: return

        Glide.with(holder.itemView)
            .load(item.firstFilePath)
            .override(size, size)
            .centerCrop()
            .placeholder(R.drawable.image_24px)
            .into(holder.imageView)

        holder.tvName.text = item.name
        holder.tvSize.text = item.size
    }

    override fun getItemCount(): Int = exploreItems?.size ?: 0

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: SquareImageViewSimple = itemView.findViewById(R.id.img)
        val tvName: TextCustumFont = itemView.findViewById(R.id.tv_name)
        val tvSize: TextCustumFont = itemView.findViewById(R.id.tv_size)

        init {
            itemView.setOnClickListener {
                val callback = iExplore ?: return@setOnClickListener
                val pos = adapterPosition
                if (pos == -1) return@setOnClickListener

                exploreItems?.get(pos)?.let { item ->
                    callback.folder(item.folder, item.name, item.path)
                }
            }
        }
    }
}
