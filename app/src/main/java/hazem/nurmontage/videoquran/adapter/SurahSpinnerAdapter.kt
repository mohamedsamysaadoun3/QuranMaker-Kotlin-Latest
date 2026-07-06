package hazem.nurmontage.videoquran.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import hazem.nurmontage.videoquran.R

class SurahSpinnerAdapter(
    context: Context,
    private val surahNames: Array<String>,
    private val isArabic: Boolean
) : ArrayAdapter<String>(context, R.layout.row_spinner_aya, surahNames) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent)
    }

    private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.row_spinner_aya, parent, false)

        val textView: TextView = view.findViewById(R.id.spinner_text)

        val parts = surahNames[position].split(" - ")
        val displayName = if (isArabic) {
            parts.getOrElse(0) { surahNames[position] }
        } else {
            parts.getOrElse(1) { surahNames[position] }
        }

        textView.text = displayName
        return view
    }
}
