package hazem.nurmontage.videoquran.fragment

import android.content.ContentUris
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.GalleryVideoAdabters
import hazem.nurmontage.videoquran.adapter.IPicker
import hazem.nurmontage.videoquran.databinding.FragmentGalleryVideoBinding
import hazem.nurmontage.videoquran.model.GallerySelected
import hazem.nurmontage.videoquran.model.VideoItem
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.ScreenUtils
import java.io.File
import java.io.IOException

class GalleryPhotosFragment : Fragment {

    companion object {
        @JvmStatic var instance: GalleryPhotosFragment? = null

        @Synchronized
        fun get(
            gallerySelecteds: List<GallerySelected>?,
            folder: File?,
            iPicker: IPicker?
        ): GalleryPhotosFragment {
            if (instance == null) {
                instance = GalleryPhotosFragment(gallerySelecteds, folder, iPicker)
            }
            return instance!!
        }
    }

    private var adabters: GalleryVideoAdabters? = null
    private var folder: File? = null
    private var gallerySelecteds: List<GallerySelected>? = null
    private var galleryVideoBinding: FragmentGalleryVideoBinding? = null
    private var iPicker: IPicker? = null

    constructor()

    constructor(gallerySelecteds: List<GallerySelected>?, folder: File?, iPicker: IPicker?) {
        this.iPicker = iPicker
        this.folder = folder
        this.gallerySelecteds = gallerySelecteds
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentGalleryVideoBinding.inflate(inflater, container, false)
        galleryVideoBinding = binding
        val root: FrameLayout = binding.root
        loadVideos(root)
        return root
    }

    fun isContains(path: String): VideoItem? {
        val list = gallerySelecteds ?: return null
        for (selected in list) {
            if (selected.videoItem != null && selected.videoItem!!.path == path) {
                return selected.videoItem
            }
        }
        return null
    }

    fun inselect(position: Int) {
        adabters?.inselectItem(position)
    }

    @Suppress("DEPRECATION")
    private fun loadVideos(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_gallery)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = GridLayoutManager(context, 3)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.isDrawingCacheEnabled = true
        recyclerView.itemAnimator = null

        val adapter = GalleryVideoAdabters(
            AppUtils.getAppVersionName(requireContext()),
            resources,
            gallerySelecteds,
            (ScreenUtils.getScreenWidth(requireActivity()) * 0.24f).toInt(),
            iPicker
        )
        adabters = adapter
        recyclerView.adapter = adapter

        val f = folder
        if (f != null) {
            changeFolder(f)
        } else {
            Thread {
                val cursor: Cursor? = requireActivity().contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf("_id", "duration"),
                    null, null, null
                )
                val items = arrayListOf<VideoItem>()
                cursor?.use {
                    while (it.moveToNext()) {
                        it.getInt(it.getColumnIndexOrThrow("duration"))
                        val contained = isContains(
                            ContentUris.withAppendedId(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                it.getLong(it.getColumnIndexOrThrow("_id"))
                            ).toString()
                        )
                        if (contained != null) {
                            contained.isSelect = true
                            items.add(contained)
                        }
                    }
                }
                requireActivity().runOnUiThread {
                    adabters?.addItems(items)
                    adabters?.notifyDataSetChanged()
                }
            }.start()
        }
    }

    private fun isVideoFile(file: File): Boolean {
        val lower = file.name.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov") ||
                lower.endsWith(".mkv") || lower.endsWith(".wmv") || lower.endsWith(".flv") ||
                lower.endsWith(".webm") || lower.endsWith(".3gp") || lower.endsWith(".m4v") ||
                lower.endsWith(".mpg") || lower.endsWith(".mpeg")
    }

    fun changeFolder(file: File?) {
        adabters?.clear()
        val files = if (file != null && file.exists() && file.isDirectory) file.listFiles() else null
        if (files != null) {
            val items = arrayListOf<VideoItem>()
            for (f in files) {
                if (f.isFile && isVideoFile(f)) {
                    val absolutePath = f.absolutePath
                    getVideoDuration(absolutePath)
                    val contained = isContains(absolutePath)
                    if (contained != null) {
                        contained.isSelect = true
                        items.add(contained)
                    }
                }
            }
            if (items.isNotEmpty()) {
                adabters?.addItems(items)
                adabters?.let {
                    it.notifyItemInserted(it.itemCount - 1)
                }
                return
            }
        }
        if (adabters?.itemCount == 0) {
            adabters?.notifyDataSetChanged()
        }
    }

    private fun getVideoDuration(path: String): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val duration = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))
        try {
            retriever.release()
        } catch (_: IOException) {
        }
        return duration
    }

    override fun onDestroyView() {
        iPicker = null
        galleryVideoBinding?.root?.removeAllViews()
        galleryVideoBinding = null
        instance = null
        super.onDestroyView()
    }
}
