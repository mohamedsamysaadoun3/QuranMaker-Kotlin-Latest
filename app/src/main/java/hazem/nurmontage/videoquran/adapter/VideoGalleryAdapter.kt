package hazem.nurmontage.videoquran.adapter

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.model.VideoItem
import hazem.nurmontage.videoquran.views.image.SquareImageView
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.io.File
import java.util.Locale

/**
 * RecyclerView adapter for displaying video files in a grid gallery.
 *
 * This is a simplified, single-select video picker adapter. It shows video
 * thumbnails with duration overlays and supports folder-based grouping.
 *
 * Unlike [GalleryVideoAdabters] which supports both single-select and
 * multi-select modes with the [IPicker] interface, this adapter uses a
 * simple lambda callback ([onItemClick]) for single video selection.
 * This makes it easier to use in activities that only need one video.
 *
 * The adapter also provides a [queryVideos] companion method that queries
 * the MediaStore for all video files on the device, grouped by folder.
 * The folder structure is returned as a map of folder name → video list,
 * which the hosting Activity can use to populate a folder tab bar.
 *
 * @see VideoItem
 * @see GalleryVideoAdabters
 */
class VideoGalleryAdapter(
    private val onItemClick: (VideoItem) -> Unit
) : RecyclerView.Adapter<VideoGalleryAdapter.ViewHolder>() {

    private val items = mutableListOf<VideoItem>()

    /** The currently selected video position (-1 = none selected). */
    private var selectedPosition: Int = -1

    /**
     * Replace the adapter's data with a new list of videos.
     * Resets the selection state.
     */
    fun submitList(newItems: List<VideoItem>) {
        items.clear()
        items.addAll(newItems)
        selectedPosition = -1
        notifyDataSetChanged()
    }

    /**
     * Select the video at the given position and deselect the previous one.
     * Calls [onItemClick] with the selected [VideoItem].
     */
    fun selectItem(position: Int) {
        if (position < 0 || position >= items.size) return

        val prevPosition = selectedPosition
        selectedPosition = position

        items[position].isSelect = true
        notifyItemChanged(position)

        if (prevPosition >= 0 && prevPosition < items.size && prevPosition != position) {
            items[prevPosition].isSelect = false
            notifyItemChanged(prevPosition)
        }

        onItemClick(items[position])
    }

    // ──────────────────────────────────────────────────────────────────────
    // Adapter overrides
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.imageView.onSelect(item.isSelect)
        if (item.isSelect) {
            holder.imageView.setNumber(1)
        }

        // Load video thumbnail via Glide
        Glide.with(holder.itemView)
            .load(item.path)
            .centerCrop()
            .signature(ObjectKey("1"))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(holder.imageView)

        // Show video duration
        holder.tvTime.text = item.time
        holder.tvTime.visibility = View.VISIBLE

        holder.itemView.setOnClickListener {
            selectItem(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = items.size

    // ──────────────────────────────────────────────────────────────────────
    // ViewHolder
    // ──────────────────────────────────────────────────────────────────────

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: SquareImageView = itemView.findViewById(R.id.img)
        val tvTime: TextCustumFont = itemView.findViewById(R.id.tv_time)

        init {
            tvTime.visibility = View.VISIBLE
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // MediaStore query utility
    // ──────────────────────────────────────────────────────────────────────

    companion object {
        /**
         * Query the MediaStore for all video files on the device,
         * returning them as a map of folder path → list of [VideoItem]s.
         *
         * This method performs a content resolver query on
         * [MediaStore.Video.Media.EXTERNAL_CONTENT_URI] and groups the
         * results by their parent directory path (which serves as the folder).
         *
         * Each [VideoItem] is created with:
         * - [VideoItem.folderPath] = the parent directory path
         * - [VideoItem.path] = the content URI as a string
         *   (ContentUris.withAppendedId format)
         * - [VideoItem.time] = the duration formatted as "MM:SS"
         * - [VideoItem.isSelect] = false
         *
         * The folder name (for display in tabs) can be extracted from
         * the last segment of the folder path.
         *
         * This method MUST be called on a background thread as it performs
         * I/O operations (ContentResolver query).
         *
         * @param context A context for accessing the ContentResolver
         * @return A map of folder paths to their video lists
         */
        fun queryVideos(context: Context): Map<String, MutableList<VideoItem>> {
            val folderMap = linkedMapOf<String, MutableList<VideoItem>>()

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            val selection = "${MediaStore.Video.Media.IS_PENDING} = 0"
            val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

            try {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val bucketCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Video.Media.BUCKET_DISPLAY_NAME
                    )

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val data = cursor.getString(dataCol) ?: continue
                        val duration = cursor.getInt(durationCol)
                        val bucketName = cursor.getString(bucketCol) ?: "Videos"

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                        ).toString()

                        val folderPath = File(data).parent ?: continue

                        val videoItem = VideoItem(
                            folderPath = folderPath,
                            path = contentUri,
                            time = formatDuration(duration),
                            isSelect = false
                        )

                        folderMap.getOrPut(folderPath) { mutableListOf() }
                            .add(videoItem)
                    }
                }
            } catch (_: Exception) {
                // MediaStore query failed — return empty map
            }

            return folderMap
        }

        /**
         * Format a video duration from milliseconds to "MM:SS" or "H:MM:SS".
         * Matches the format used in the original Java source.
         */
        private fun formatDuration(durationMs: Int): String {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.ENGLISH, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.ENGLISH, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
