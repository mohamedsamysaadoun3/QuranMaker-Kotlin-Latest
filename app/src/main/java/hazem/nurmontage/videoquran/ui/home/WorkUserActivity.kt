package hazem.nurmontage.videoquran.ui.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.Insets
import androidx.media3.common.MimeTypes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.GsonBuilder
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.WorkUserAdapter
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.ui.engine.EngineActivity
import hazem.nurmontage.videoquran.ui.settings.SeettingActivity
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.MFileUtils
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.utils.LocaleHelper
import java.io.File

class WorkUserActivity : BaseActivity() {

    private var dialog: Dialog? = null
    private var popupWindow: PopupWindow? = null
    private var workUserAdapter: WorkUserAdapter? = null
    private var backPressedOnce = false
    private var mToast: Toast? = null

    // ── Back-press: double-tap to exit ─────────────────────────────

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            try {
                if (backPressedOnce) {
                    mToast?.cancel()
                    finish()
                } else {
                    backPressedOnce = true
                    mToast = Toast.makeText(
                        this@WorkUserActivity,
                        getString(R.string.press_again_to_exit),
                        Toast.LENGTH_SHORT
                    )
                    mToast?.show()
                    Handler(Looper.getMainLooper()).postDelayed(
                        { backPressedOnce = false },
                        BACK_PRESS_TIMEOUT_MS
                    )
                }
            } catch (_: Exception) {
                finish()
            }
        }
    }

    // ── Adapter callback: item click & popup menu ──────────────────

    private var iWorkUserCallback: WorkUserAdapter.IWorkUserCallback? =
        object : WorkUserAdapter.IWorkUserCallback {
            override fun onClick(template: Template) {
                val intent = Intent(this@WorkUserActivity, EngineActivity::class.java)
                if (template.idTemplate == null) {
                    template.idTemplate = template.uri_video
                }
                intent.putExtra(Common.TEMPLATE, template.idTemplate)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
                finish()
            }

            override fun toMenu(template: Template, view: View, position: Int) {
                showPopup(view, template, position)
            }
        }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_work_user)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsets ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        setStatusBarColor(-1)
        setNavigationBarColor(-1)

        initRv()

        // Settings button
        findViewById<View>(R.id.btn_menu).setOnClickListener {
            startActivity(Intent(this, SeettingActivity::class.java))
            finish()
        }

        // Secret 5-tap unlock skipped — billing removed, all features
        // unlocked. The original code had a 5-tap Easter egg on tv_secret
        // that called BillingPreferences.saveSubscriptionStatus().
    }

    override fun onPause() {
        super.onPause()
        cancelDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Glide.get(this).clearMemory()
        } catch (_: Exception) {
        }
        iWorkUserCallback = null
        cancelDialog()
    }

    // ── RecyclerView initialisation ────────────────────────────────

    private fun initRv() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val all = sharedPreferences.all

        if (!all.isNullOrEmpty()) {
            val gson = GsonBuilder().create()
            val templates = ArrayList<Template>()

            for (key in all.keys) {
                try {
                    val json = sharedPreferences.getString(key, "") ?: ""
                    val template = gson.fromJson(json, Template::class.java)
                    if (template != null) {
                        if (template.fileInfo == null) {
                            template.fileInfo = MFileUtils.getFileInfo(
                                applicationContext,
                                template.uri_video
                            )
                        }
                        templates.add(template)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Sort by idTemplate descending (newest first)
            templates.sortWith { t1, t2 ->
                if (t1.idTemplate == null || t2.idTemplate == null) 0
                else t2.idTemplate!!.compareTo(t1.idTemplate!!)
            }

            val recyclerView = findViewById<RecyclerView>(R.id.rv)
            recyclerView.post {
                val screenWidth = (ScreenUtils.getScreenWidth(this@WorkUserActivity) * 0.3f).toInt()
                workUserAdapter = WorkUserAdapter(
                    appVersion = AppUtils.getAppVersionName(this@WorkUserActivity),
                    images = templates,
                    iWorkUserCallback = iWorkUserCallback,
                    w = screenWidth,
                    h = screenWidth
                )
                recyclerView.layoutManager = LinearLayoutManager(
                    this@WorkUserActivity,
                    RecyclerView.VERTICAL,
                    false
                )
                recyclerView.setHasFixedSize(true)
                recyclerView.itemAnimator = null
                recyclerView.adapter = workUserAdapter
            }
        }

        // "Create Video" button
        val createBtn = findViewById<hazem.nurmontage.videoquran.views.ButtonCustumFont>(R.id.btn_to_studio)
        createBtn.text = getString(R.string.create_video)
        createBtn.setOnClickListener {
            val intent = Intent(this@WorkUserActivity, EngineActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }
    }

    // ── Popup menu (share / delete / duplicate) ────────────────────

    private fun showPopup(anchor: View, template: Template, position: Int) {
        if (template.uri_video == null) return

        val contentView = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.layout_work_setup, null as ViewGroup?)

        popupWindow = PopupWindow(contentView, ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setBackgroundDrawable(ColorDrawable(0))
            isOutsideTouchable = true
            isFocusable = true
        }

        // ── Share ──────────────────────────────────────────
        contentView.findViewById<RelativeLayout>(R.id.btn_share).setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra("act", "ACT_SHARE")
                    putExtra(Intent.EXTRA_TITLE, "Send To")
                    putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                            this@WorkUserActivity,
                            getString(R.string.file_provider),
                            File(Uri.parse(template.uri_video).path ?: "")
                        )
                    )
                    type = MimeTypes.VIDEO_MP4
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Send To"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popupWindow?.dismiss()
        }

        // ── Delete ─────────────────────────────────────────
        contentView.findViewById<RelativeLayout>(R.id.btn_delete).setOnClickListener {
            try {
                showDialog(position, template, Uri.parse(template.uri_video))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // ── Duplicate ──────────────────────────────────────
        contentView.findViewById<RelativeLayout>(R.id.btn_duplicate).setOnClickListener {
            try {
                val duplicate = template.duplicate()
                if (duplicate != null) {
                    val templateName = duplicate.idTemplate + "_copy"
                    duplicate.idTemplate = templateName
                    LocalPersistence.duplicateTemplate(this@WorkUserActivity, duplicate, templateName)
                    workUserAdapter?.add(position + 1, duplicate)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popupWindow?.dismiss()
        }

        // Set localized labels
        contentView.findViewById<hazem.nurmontage.videoquran.views.TextCustumFont>(R.id.tv_share)
            .text = getString(R.string.just_share)
        contentView.findViewById<hazem.nurmontage.videoquran.views.TextCustumFont>(R.id.tv_duplicate)
            .text = getString(R.string.duplicate)
        contentView.findViewById<hazem.nurmontage.videoquran.views.TextCustumFont>(R.id.tv_delete)
            .text = getString(R.string.delete)

        // Position popup below the anchor view
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        popupWindow?.showAtLocation(anchor, 0, location[0], location[1] + anchor.height)
    }

    // ── Delete confirmation dialog ─────────────────────────────────

    private fun showDialog(position: Int, template: Template, uri: Uri?) {
        val dlg = Dialog(this)
        dialog = dlg
        dlg.setCancelable(true)
        dlg.requestWindowFeature(1)
        dlg.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dlg.window?.setBackgroundDrawable(ColorDrawable(0))

        val view = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null as ViewGroup?)
        dlg.setContentView(view)

        // Hide the title — reuse the generic dialog layout
        view.findViewById<View>(R.id.dialog_title).visibility = View.GONE

        view.findViewById<hazem.nurmontage.videoquran.views.TextCustumFont>(R.id.dialog_message)
            .text = getString(R.string.are_you_sure_to_delete_this_work)

        // "Delete" confirm button (uses dialog_no slot from the layout)
        val btnDelete = view.findViewById<hazem.nurmontage.videoquran.views.ButtonCustumFont>(R.id.dialog_no)
        btnDelete.text = getString(R.string.delete)
        btnDelete.setTextColor(-1499549)
        btnDelete.setBackgroundResource(R.drawable.btn_dialog_delete)
        btnDelete.setOnClickListener {
            try {
                // Delete the video file
                if (uri != null) {
                    val file = File(uri.path ?: "")
                    if (file.exists()) file.delete()
                }
                // Remove from SharedPreferences
                if (template.idTemplate != null) {
                    LocalPersistence.deleteTemplate(this, template.idTemplate!!)
                } else if (template.uri_video != null) {
                    LocalPersistence.deleteTemplate(this, template.uri_video!!)
                }
                workUserAdapter?.remove(position)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popupWindow?.dismiss()
            dialog?.dismiss()
        }

        // "No" cancel button (uses dialog_yes slot from the layout)
        val btnCancel = view.findViewById<hazem.nurmontage.videoquran.views.ButtonCustumFont>(R.id.dialog_yes)
        btnCancel.text = getString(R.string.no)
        btnCancel.setOnClickListener {
            dialog?.dismiss()
        }

        dlg.show()
    }

    // ── Dialog cleanup ─────────────────────────────────────────────

    private fun cancelDialog() {
        dialog?.let {
            if (it.isShowing) it.dismiss()
        }
        dialog = null
    }

    // ── Constants ──────────────────────────────────────────────────

    companion object {
        private const val PREFS_NAME = "MTemplate"
        /** Timeout for double-tap back-to-exit (same as ExoPlayer.DEFAULT_DETACH_SURFACE_TIMEOUT_MS = 2000) */
        private const val BACK_PRESS_TIMEOUT_MS = 2000L
    }
}
