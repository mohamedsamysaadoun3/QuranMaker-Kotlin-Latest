package hazem.nurmontage.videoquran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.fragment.audio_effect.Reverbe
import hazem.nurmontage.videoquran.views.TextCustumFont

/**
 * RecyclerView Adapter for displaying and selecting audio reverb/sound presets.
 *
 * Originally: ReverbeAdabter.java (preserved typo in original package)
 * Converted to: SoundAdapter.kt — clean naming, idiomatic Kotlin
 *
 * Features:
 * - Displays reverb preset names with play/pause toggle icons
 * - Single-selection mode with visual highlight
 * - Click to select a reverb preset (sends FFmpeg command via callback)
 * - Click again on the selected item to deselect (removes effect)
 * - Callback interface for audio playback commands and pause control
 *
 * @property list List of available reverb presets
 * @property iReverbCallback Callback for reverb selection/playback events
 * @property select Currently selected position, or -1 if none selected
 */
class SoundAdapter(
    private var list: List<Reverbe>?,
    private val iReverbCallback: IReverbPresetCallback?,
    private var select: Int
) : RecyclerView.Adapter<SoundAdapter.ViewHolder>() {

    /** Callback interface for reverb/sound preset interactions. */
    interface IReverbPresetCallback {
        /**
         * Sends an FFmpeg command for the selected reverb preset.
         * @param cmd The FFmpeg filter command string
         * @param position The adapter position of the selected preset
         */
        fun cmd(cmd: String?, position: Int)

        /** Pauses current audio playback before applying a new preset. */
        fun pause()
    }

    /** Returns the current list of reverb presets. */
    fun getList(): List<Reverbe>? = list

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_reverbe, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reverb = list?.get(position) ?: return

        holder.text.text = reverb.name

        if (select == position) {
            holder.itemView.setBackgroundResource(R.drawable.item_reverb_select)
            holder.ivBtnPlay.setImageResource(R.drawable.pause_24px)
        } else {
            holder.itemView.setBackgroundResource(R.drawable.round_btn_in_dark)
            holder.ivBtnPlay.setImageResource(R.drawable.play_arrow_24px)
        }
    }

    override fun getItemCount(): Int = list?.size ?: 0

    /**
     * ViewHolder for reverb/sound preset items.
     * Handles click events for preset selection/deselection with playback control.
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextCustumFont = itemView.findViewById(R.id.word_aya)
        val ivBtnPlay: ImageView = itemView.findViewById(R.id.iv_btn_play)

        init {
            itemView.setOnClickListener {
                val callback = iReverbCallback ?: return@setOnClickListener
                val pos = adapterPosition

                callback.pause()

                if (select == pos) {
                    // Deselect: clicking the same item removes the effect
                    val oldSelect = select
                    select = -1
                    notifyItemChanged(oldSelect)
                    notifyItemChanged(pos)
                    return@setOnClickListener
                }

                // Select new preset
                val oldSelect = select
                select = pos
                notifyItemChanged(oldSelect)
                notifyItemChanged(select)

                list?.get(pos)?.let { reverb ->
                    callback.cmd(reverb.cmdFfmpeg, pos)
                }
            }
        }
    }
}
