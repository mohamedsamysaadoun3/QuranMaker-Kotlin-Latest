package hazem.nurmontage.videoquran.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import hazem.nurmontage.videoquran.databinding.ActivityPixabaySearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * PixabaySearchActivity — Search and select images from the Pixabay API.
 *
 * Layout: `activity_pixabay_search.xml` (dedicated layout with search bar).
 *
 * Flow:
 *   1. User types a search query in [etSearch] and presses Search on the keyboard
 *   2. Activity sends a GET request to the Pixabay API
 *   3. Image URLs are parsed from the JSON response and shown in a 3-column grid
 *   4. User taps an image to select it (highlighted with a border)
 *   5. Tapping "Done" returns the selected image URL as an activity result
 *
 * This activity did not exist in the original JADX source — it was added during
 * the Kotlin rewrite to replace inline Pixabay search logic that was embedded
 * inside EngineActivity.  The original app used a simple HTTP call to the
 * Pixabay API with the same endpoint and response format.
 */
class PixabaySearchActivity : AppCompatActivity() {

    // ── ViewBinding (dedicated layout, not reusing gallery picker) ────
    private lateinit var binding: ActivityPixabaySearchBinding

    // ── State ────────────────────────────────────────────────────────
    private val imageUrls = mutableListOf<PixabayImage>()
    private var selectedPosition: Int = -1

    /** Pixabay API key — must be configured before use. */
    private var pixabayApiKey: String = ""

    // ═══════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPixabaySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSearchInput()
        setupRecyclerView()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Toolbar
    // ═══════════════════════════════════════════════════════════════════

    private fun setupToolbar() {
        // Back — close without selecting
        binding.btnOnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        // Done — confirm selection and return URL
        binding.btnDone.setOnClickListener {
            if (selectedPosition in imageUrls.indices) {
                val selected = imageUrls[selectedPosition]
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_PIXABAY_URL, selected.webFormatUrl)
                    putExtra(EXTRA_PIXABAY_PREVIEW_URL, selected.previewUrl)
                    putExtra(EXTRA_PIXABAY_TAGS, selected.tags)
                }
                setResult(RESULT_OK, resultIntent)
            } else {
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Search
    // ═══════════════════════════════════════════════════════════════════

    private fun setupSearchInput() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString()?.trim()
                if (!query.isNullOrEmpty()) {
                    searchPixabay(query)
                }
                true
            } else {
                false
            }
        }
    }

    /**
     * Searches Pixabay for images matching [query].
     *
     * The API endpoint format:
     * ```
     * https://pixabay.com/api/?key={API_KEY}&q={QUERY}&image_type=photo&per_page=50&safesearch=true
     * ```
     *
     * Response JSON structure (relevant fields):
     * ```json
     * { "hits": [ { "webformatURL": "...", "previewURL": "...", "tags": "..." } ] }
     * ```
     */
    private fun searchPixabay(query: String) {
        if (pixabayApiKey.isEmpty()) {
            Toast.makeText(this, "Pixabay API key not configured", Toast.LENGTH_SHORT).show()
            return
        }

        binding.viewProgress.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = URL(
                    "https://pixabay.com/api/?key=$pixabayApiKey&q=$encodedQuery" +
                            "&image_type=photo&per_page=50&safesearch=true"
                )
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10_000
                connection.readTimeout = 10_000

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val hits = json.getJSONArray("hits")

                val results = mutableListOf<PixabayImage>()
                for (i in 0 until hits.length()) {
                    val hit = hits.getJSONObject(i)
                    results.add(
                        PixabayImage(
                            webFormatUrl = hit.optString("webformatURL", ""),
                            previewUrl = hit.optString("previewURL", ""),
                            tags = hit.optString("tags", "")
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    imageUrls.clear()
                    imageUrls.addAll(results)
                    selectedPosition = -1
                    (binding.rvSearch.adapter as? PixabayAdapter)?.notifyDataSetChanged()
                    binding.viewProgress.visibility = View.GONE
                    binding.tvEmpty.visibility = if (imageUrls.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.viewProgress.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(
                        this@PixabaySearchActivity,
                        "Search failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RecyclerView
    // ═══════════════════════════════════════════════════════════════════

    private fun setupRecyclerView() {
        binding.rvSearch.layoutManager = GridLayoutManager(this, 3)
        binding.rvSearch.adapter = PixabayAdapter(imageUrls, selectedPosition) { position ->
            selectedPosition = position
            (binding.rvSearch.adapter as? PixabayAdapter)?.updateSelected(position)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Pixabay Image Data
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Holds the relevant fields from a single Pixabay API hit.
     */
    data class PixabayImage(
        val webFormatUrl: String,
        val previewUrl: String,
        val tags: String
    )

    // ═══════════════════════════════════════════════════════════════════
    //  Adapter
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Grid adapter for displaying Pixabay search results.
     *
     * Each cell is a simple [ImageView] loaded with Glide. The selected cell
     * gets a yellow border via padding + background tint.
     */
    private class PixabayAdapter(
        private val images: List<PixabayImage>,
        private var selectedPos: Int,
        private val onSelect: (Int) -> Unit
    ) : RecyclerView.Adapter<PixabayAdapter.VH>() {

        inner class VH(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        fun updateSelected(pos: Int) {
            val old = selectedPos
            selectedPos = pos
            if (old in images.indices) notifyItemChanged(old)
            if (pos in images.indices) notifyItemChanged(pos)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val size = (parent.width / 3) - 4
            val iv = ImageView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(size, size)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(2, 2, 2, 2)
            }
            return VH(iv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val img = images[position]
            Glide.with(holder.imageView.context)
                .load(img.previewUrl.ifEmpty { img.webFormatUrl })
                .centerCrop()
                .into(holder.imageView)

            // Highlight selected image
            val isSelected = position == selectedPos
            holder.imageView.setBackgroundColor(
                if (isSelected) 0xFFBEA448.toInt() else 0x00000000
            )

            holder.imageView.setOnClickListener { onSelect(holder.adapterPosition) }
        }

        override fun getItemCount() = images.size
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Companion
    // ═══════════════════════════════════════════════════════════════════

    companion object {
        const val EXTRA_PIXABAY_URL = "pixabay_url"
        const val EXTRA_PIXABAY_PREVIEW_URL = "pixabay_preview_url"
        const val EXTRA_PIXABAY_TAGS = "pixabay_tags"
    }
}
