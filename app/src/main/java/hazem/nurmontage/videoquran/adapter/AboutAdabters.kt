package hazem.nurmontage.videoquran.adapter

import android.content.Context
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R

class AboutAdabters(
    private val mContext: Context,
    private val APP_VERSION: String,
    private val mModelAboutList: List<ModelAbout>?,
    private val mDimensionW: Int,
    private val mDimensionH: Int
) : RecyclerView.Adapter<AboutAdabters.ViewHolder>() {

    class ModelAbout(
        val text: Pair<String, Int>,
        val image_1: Int = -1,
        val image_2: Int = -1,
        val sizeText: Int = 16
    ) {
        constructor(text: Pair<String, Int>, image_1: Int) : this(text, image_1, -1, 16)
        constructor(sizeText: Int, text: Pair<String, Int>, image_1: Int) : this(text, image_1, -1, sizeText)
        constructor(image_1: Int, image_2: Int, text: Pair<String, Int>) : this(text, image_1, image_2, 16)
        constructor(text: Pair<String, Int>, image_1: Int, sizeText: Int) : this(text, image_1, -1, sizeText)
        constructor(sizeText: Int, text: Pair<String, Int>) : this(text, -1, -1, sizeText)

        fun getText(): String = text.first
        fun geGravity(): Int = text.second
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_about_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val modelAbout = mModelAboutList?.get(position) ?: return

        holder.textView.gravity = modelAbout.geGravity()

        if (modelAbout.sizeText == 19) {
            holder.textView.paint.isFakeBoldText = true
        } else {
            holder.textView.paint.isFakeBoldText = false
        }

        holder.textView.setTextSize(2, modelAbout.sizeText.toFloat())
        holder.textView.text = HtmlCompat.fromHtml(modelAbout.getText(), 0)

        if (modelAbout.image_1 != -1) {
            holder.imageView1.visibility = View.VISIBLE
            Glide.with(mContext)
                .asBitmap()
                .load(modelAbout.image_1)
                .override(mDimensionW, mDimensionH)
                .centerInside()
                .signature(ObjectKey(APP_VERSION))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.imageView1)
        } else {
            holder.imageView1.visibility = View.GONE
            Glide.with(mContext).clear(holder.imageView1)
        }
    }

    override fun getItemCount(): Int = mModelAboutList?.size ?: 0

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tv)
        val imageView1: ImageView = itemView.findViewById(R.id.img)
    }
}
