package hazem.nurmontage.videoquran.ui.gallery_photos

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.ExploreAdabters
import hazem.nurmontage.videoquran.adapter.GalleryPickerAdabters
import hazem.nurmontage.videoquran.adapter.IPicker
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.databinding.ActivityGalleryPickerVideoBinding
import hazem.nurmontage.videoquran.model.ExploreItem
import hazem.nurmontage.videoquran.model.GallerySelected
import hazem.nurmontage.videoquran.model.PhotoItem
import hazem.nurmontage.videoquran.model.VideoItem
import hazem.nurmontage.videoquran.utils.AppSettingsHelper
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.io.File

/**
 * Activity for picking a single image from the device gallery.
 * Faithful port of original GalleryPickerOneImage.java (391 lines).
 *
 * Shows images in a grid with optional folder browsing via rv_explore.
 * Returns the selected image URI via RESULT_OK.
 *
 * On back/cancel: if rv_explore is visible, closes it first.
 * Otherwise: clears Common.LIST_SELECT and finishes.
 */
class GalleryPickerOneImage : BaseActivity() {

    private lateinit var binding: ActivityGalleryPickerVideoBinding
    private var btnDone: ImageButton? = null
    private var btnExplore: TextCustumFont? = null
    private var galleryPickerAdabters: GalleryPickerAdabters? = null
    private var isUpdate: Boolean = false
    private var layoutSetting: LinearLayout? = null
    private var mPhotoItem: PhotoItem? = null
    private var mResources: Resources? = null
    private var rvExplore: RecyclerView? = null

    // ── Callbacks (nullable for onDestroy cleanup — matches Java) ─────

    private var onBackPressedCallback: OnBackPressedCallback? = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (rvExplore != null && rvExplore?.visibility == View.VISIBLE) {
                btnExplore?.performClick()
                return
            }
            Common.listSelect = null
            Common.indexListSelect = 1
            finish()
        }
    }

    private var iPicker: IPicker? = object : IPicker {
        override fun onAdd(videoItem: VideoItem, resourceId: Int) {
            // No-op in image picker — matches Java
        }

        override fun onDelete(gallerySelected: GallerySelected) {
            // No-op in image picker — matches Java
        }

        override fun onEmptyList() {
            setSetting(false)
        }

        override fun onAdd(photoItem: PhotoItem, resourceId: Int) {
            mPhotoItem = photoItem
        }
    }

    private var iExplore: ExploreAdabters.IExplore? = object : ExploreAdabters.IExplore {
        override fun folder(file: File, textValue: String, textValue2: String) {
            if (rvExplore?.visibility != View.GONE) {
                rvExplore?.visibility = View.GONE
            }
            changeFolder(textValue2)
            btnExplore?.text = textValue
        }

        override fun done() {
            if (rvExplore?.visibility != View.GONE) {
                rvExplore?.visibility = View.GONE
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onDestroy() {
        super.onDestroy()
        iExplore = null
        iPicker = null
        onBackPressedCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGalleryPickerVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        Common.listSelect = null
        Common.indexListSelect = 1

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)

        mResources = resources
        btnDone = binding.tvDone
        rvExplore = binding.rvExplore

        btnDone?.setOnClickListener {
            if (mPhotoItem != null) {
                val intent = Intent()
                intent.data = Uri.parse(mPhotoItem!!.path)
                setResult(RESULT_OK, intent)
            }
            finish()
        }

        initViews()
        initFolder()

        // Permission checking — matches Java pattern exactly
        if (Build.VERSION.SDK_INT >= 33 &&
            (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") == 0 ||
             ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") == 0)
        ) {
            setSetting(true)
        } else if (Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == 0
        ) {
            setSetting(false)
        } else if (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0) {
            setSetting(true)
        } else {
            setSetting(false)
        }
    }

    // ── Permission / Settings ─────────────────────────────────────────

    private fun setSetting(isFlag: Boolean) {
        if (isFlag) return
        val linearLayout = binding.toSetting.root as? LinearLayout ?: return
        layoutSetting = linearLayout
        linearLayout.visibility = View.VISIBLE
        layoutSetting?.setOnClickListener {
            isUpdate = true
            AppSettingsHelper.openAppSettings(this@GalleryPickerOneImage)
        }
    }

    private fun updateSetting() {
        if (Build.VERSION.SDK_INT >= 33 &&
            (ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_IMAGES") == 0 ||
             ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VIDEO") == 0)
        ) {
            recreate()
        } else if ((Build.VERSION.SDK_INT < 34 ||
            ContextCompat.checkSelfPermission(this, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") != 0) &&
            ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0
        ) {
            recreate()
        }
        isUpdate = false
    }

    override fun onResume() {
        super.onResume()
        if (isUpdate) {
            updateSetting()
        }
    }

    // ── Folder browsing ───────────────────────────────────────────────

    /**
     * Initialize folder browsing UI and load gallery images in background.
     *
     * Faithful port of Java initFolder() — two-phase MediaStore scan:
     * Phase 1: Load first 50 images and show initial grid.
     * Phase 2: Continue scanning remaining images and build folder list.
     * Finally: Show ExploreAdabters in rvExplore for folder navigation.
     *
     * Uses MediaStore.Files with media_type=1 (IMAGE) — matches Java exactly.
     */
    private fun initFolder() {
        val btnExploreLocal = binding.tvFolders
        btnExplore = btnExploreLocal
        btnExploreLocal.text = mResources?.getString(R.string.all)
        btnExplore?.setOnClickListener {
            if (rvExplore == null || btnExplore == null) return@setOnClickListener
            if (rvExplore?.visibility != View.VISIBLE) {
                rvExplore?.visibility = View.VISIBLE
                btnExplore?.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_up_float, 0)
            } else {
                rvExplore?.visibility = View.GONE
                btnExplore?.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.arrow_down_float, 0)
            }
        }

        Thread {
            val exploreItems = ArrayList<ExploreItem>()
            val photoItems = ArrayList<PhotoItem>()
            val folderSet = HashSet<String>()

            val query = contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf("_id", "_data", "parent"),
                "media_type=1",
                null,
                "date_added DESC"
            )

            Log.e("query start", "$query")

            var totalCount = 0
            var firstPhaseCount = 0
            var firstFilePath: String? = null

            if (query != null) {
                // ── Phase 1: Read first 50 items ──
                while (query.moveToNext()) {
                    val data = query.getString(query.getColumnIndexOrThrow("_data"))
                    val parent = File(data).parent

                    if (!folderSet.contains(parent)) {
                        folderSet.add(parent)
                        val folder = File(parent)
                        val listFiles = folder.listFiles()
                        var imageCount = 0
                        var folderFirstFile: String? = null

                        if (listFiles != null) {
                            for (file in listFiles) {
                                if (isImageFile(file)) {
                                    imageCount++
                                    if (folderFirstFile == null) {
                                        folderFirstFile = file.absolutePath
                                        if (firstFilePath == null) {
                                            firstFilePath = folderFirstFile
                                        }
                                    }
                                }
                            }
                        }

                        if (imageCount > 0) {
                            totalCount += imageCount
                            exploreItems.add(ExploreItem(folder, parent, "$imageCount", folder.name, folderFirstFile ?: ""))
                        }
                    }

                    val id = query.getLong(query.getColumnIndexOrThrow("_id"))
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    photoItems.add(PhotoItem(parent, uri, false))

                    firstPhaseCount++
                    if (firstPhaseCount > 50) break
                }

                // Show first batch on UI immediately — matches Java
                runOnUiThread {
                    val recyclerView = binding.rv
                    recyclerView.setHasFixedSize(true)
                    recyclerView.layoutManager = GridLayoutManager(this@GalleryPickerOneImage, 3)
                    recyclerView.setItemViewCacheSize(20)
                    @Suppress("DEPRECATION")
                    recyclerView.isDrawingCacheEnabled = true
                    recyclerView.itemAnimator = null
                    galleryPickerAdabters = GalleryPickerAdabters(
                        AppUtils.getAppVersionName(this@GalleryPickerOneImage),
                        mResources!!,
                        null,
                        (ScreenUtils.getScreenWidth(this@GalleryPickerOneImage) * 0.3f).toInt(),
                        iPicker
                    )
                    galleryPickerAdabters?.addItems(photoItems)
                    recyclerView.adapter = galleryPickerAdabters
                }

                // ── Phase 2: Read remaining items ──
                var firstFileRemaining = firstFilePath
                while (query.moveToNext()) {
                    val data = query.getString(query.getColumnIndexOrThrow("_data"))
                    val parent = File(data).parent

                    if (!folderSet.contains(parent)) {
                        folderSet.add(parent)
                        val folder = File(parent)
                        val listFiles = folder.listFiles()
                        var imageCount = 0
                        var folderFirstFile: String? = null

                        if (listFiles != null) {
                            for (file in listFiles) {
                                if (isImageFile(file)) {
                                    imageCount++
                                    if (folderFirstFile == null) {
                                        folderFirstFile = file.absolutePath
                                        if (firstFileRemaining == null) {
                                            firstFileRemaining = folderFirstFile
                                        }
                                    }
                                }
                            }
                        }

                        if (imageCount > 0) {
                            totalCount += imageCount
                            exploreItems.add(ExploreItem(folder, parent, "$imageCount", folder.name, folderFirstFile ?: ""))
                        }
                    }

                    val id = query.getLong(query.getColumnIndexOrThrow("_id"))
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    photoItems.add(PhotoItem(parent, uri, false))
                }

                query.close()

                // Add "All" item at position 0 — matches Java
                exploreItems.add(0, ExploreItem(
                    File(""),
                    mResources?.getString(R.string.all) ?: "All",
                    "$totalCount",
                    mResources?.getString(R.string.all) ?: "All",
                    firstFileRemaining ?: ""
                ))

                // Show final results on UI — matches Java
                runOnUiThread {
                    galleryPickerAdabters?.doneItems(photoItems)
                    galleryPickerAdabters?.notifyDataSetChanged()
                    rvExplore?.setHasFixedSize(true)
                    rvExplore?.layoutManager = LinearLayoutManager(this@GalleryPickerOneImage)
                    rvExplore?.setItemViewCacheSize(20)
                    @Suppress("DEPRECATION")
                    rvExplore?.isDrawingCacheEnabled = true
                    rvExplore?.itemAnimator = null
                    rvExplore?.adapter = ExploreAdabters(
                        exploreItems,
                        (ScreenUtils.getScreenWidth(this@GalleryPickerOneImage) * 0.2f).toInt(),
                        iExplore,
                        btnExplore?.text?.toString() ?: ""
                    )
                    binding.viewProgress.visibility = View.GONE
                    btnExplore?.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    /**
     * Check if a file is an image by extension.
     * Matches Java: .jpg, .jpeg, .png
     */
    fun isImageFile(file: File): Boolean {
        val lowerCase = file.name.lowercase()
        return lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") || lowerCase.endsWith(".png")
    }

    private fun initViews() {
        binding.btnOnBack.setOnClickListener { finish() }
    }

    /**
     * Change the current folder filter.
     * Matches Java: if "All" → updateAll(), else → update(folderPath)
     */
    fun changeFolder(textValue: String) {
        if (textValue == mResources?.getString(R.string.all)) {
            galleryPickerAdabters?.updateAll()
        } else {
            galleryPickerAdabters?.update(textValue)
        }
    }
}
