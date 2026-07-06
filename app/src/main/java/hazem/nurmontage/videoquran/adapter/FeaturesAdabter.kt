package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.ModelFeatures
import hazem.nurmontage.videoquran.views.TextCustumFont

class FeaturesAdabter(
    private var list: List<ModelFeatures>?
) : RecyclerView.Adapter<FeaturesAdabter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_feature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = list?.get(position) ?: return
        holder.text.text = feature.name
    }

    override fun getItemCount(): Int = list?.size ?: 0

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextCustumFont = itemView.findViewById(R.id.tv_feature)
    }
}
