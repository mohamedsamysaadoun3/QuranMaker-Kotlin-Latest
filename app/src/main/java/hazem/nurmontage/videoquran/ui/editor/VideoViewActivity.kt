package hazem.nurmontage.videoquran.ui.editor

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MimeTypes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.databinding.ActivityVideoViewBinding
import hazem.nurmontage.videoquran.fragment.RatingBottomSheetDialog
import hazem.nurmontage.videoquran.ui.engine.EngineActivity
import hazem.nurmontage.videoquran.ui.home.WorkUserActivity
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.views.ButtonCustumFont
import hazem.nurmontage.videoquran.views.TextCustumFont
import hazem.nurmontage.videoquran.views.TextCustumFontBold
import java.io.File

class VideoViewActivity : BaseActivity() {

    private lateinit var binding: ActivityVideoViewBinding
    private var dialog: Dialog? = null
    private var idTemplate: String? = null
    private var reader: String? = null
    private var surah: String? = null
    private var mUri: String? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            toStudio()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun setSystemUiAppearance() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.clearFlags(1024)
        window.clearFlags(512)
        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
    }

    override fun onResume() {
        super.onResume()
        setSystemUiAppearance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setSystemUiAppearance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        LocalPersistence.deleteTemplate(this, Constants.TEMPLATE_TMP)

        if (intent != null) {
            val data = intent.data
            idTemplate = intent.getStringExtra(Common.TEMPLATE)
            reader = intent.getStringExtra(Common.READER)
            surah = intent.getStringExtra(Common.SURAH)

            if (data != null) {
                mUri = data.toString()
                binding.btnPlayPause.visibility = View.VISIBLE
                binding.videoView.post {
                    Glide.with(this)
                        .asBitmap()
                        .load(mUri)
                        .frame(1000000L)
                        .centerInside()
                        .override(
                            maxOf(50, (binding.parentLayout.width * 0.9f).toInt()),
                            maxOf(50, (binding.parentLayout.height * 0.9f).toInt())
                        )
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(ObjectKey(AppUtils.getAppVersionName(this)))
                        .into(binding.videoView)
                }

                binding.videoView.setOnClickListener {
                    val intent = Intent(applicationContext, VideoPlayerActivity::class.java)
                    intent.data = data
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                }
            }
        }

        // Tuffah app button
        binding.btnTuffah.setOnClickListener {
            if (Utils.isAppInstalled(this, "hazem.tuffah.quranaudio")) {
                val launchIntent = packageManager.getLaunchIntentForPackage("hazem.tuffah.quranaudio")
                if (launchIntent != null) {
                    startActivity(launchIntent)
                }
            } else {
                installTuffah()
            }
        }

        // Home button
        binding.btnHome.setOnClickListener {
            startActivity(Intent(this, WorkUserActivity::class.java))
            finish()
        }

        // Share button
        binding.btnShare.root.setOnClickListener {
            try {
                val format = if (!Utils.isProbablyLArabic(reader ?: "")) {
                    String.format("%s %s #NurMontage_app #قرآن_كريم ", surah, reader)
                } else {
                    String.format(" %s بصوت %s #تطبيق_NurMontage #قرآن_كريم", surah, reader)
                }
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra("act", "ACT_SHARE")
                shareIntent.putExtra(Intent.EXTRA_TITLE, "Send To")
                shareIntent.putExtra(Intent.EXTRA_TEXT, format)
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.nurmontage_app))
                val uri = Uri.parse(mUri)
                shareIntent.putExtra(
                    Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(
                        this,
                        getString(R.string.file_provider),
                        File(uri.path ?: "")
                    )
                )
                shareIntent.type = MimeTypes.VIDEO_MP4
                startActivity(Intent.createChooser(shareIntent, "Send To"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Back button
        binding.btnOnBack.setOnClickListener {
            toStudio()
        }

        // Help button
        val tvHelp = findViewById<TextCustumFont?>(R.id.tv_help)
        tvHelp?.text = getString(R.string.help)
        val btnHelp = findViewById<View?>(R.id.btn_help)
        btnHelp?.setOnClickListener {
            help()
        }

        ratingSetup()
    }

    private fun installTuffah() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=hazem.tuffah.quranaudio")
        ).apply {
            setPackage("com.android.vending")
            addFlags(1476395008)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=hazem.tuffah.quranaudio")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to open app store or browser.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun help() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://chat.whatsapp.com/F0kqOjZS1VuBAvoiOG4XEZ")
            intent.setPackage("com.whatsapp")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toStudio() {
        val intent = Intent(this, EngineActivity::class.java)
        intent.putExtra(Common.TEMPLATE, idTemplate)
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    private fun ratingSetup() {
        try {
            if (!RatingBottomSheetDialog.shouldShowRatingDialog(this) || trackerSession() < 4) {
                return
            }
            dialogRate()
        } catch (_: Exception) {
        }
    }

    private fun trackerSession(): Int {
        val sharedPreferences = getSharedPreferences("ActPreference", Context.MODE_PRIVATE)
        val count = sharedPreferences.getInt("session_count", 0) + 1
        sharedPreferences.edit().putInt("session_count", count).apply()
        return count
    }

    private fun resetTrackerSession() {
        getSharedPreferences("ActPreference", Context.MODE_PRIVATE)
            .edit()
            .putInt("session_count", 0)
            .apply()
    }

    private fun openPlayStoreForRating() {
        val packageName = packageName
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            setPackage("com.android.vending")
            addFlags(1476395008)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to open app store or browser.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dialogRate() {
        val dlg = Dialog(this)
        dialog = dlg
        dlg.setCancelable(false)
        dlg.requestWindowFeature(1)
        dlg.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dlg.window?.setBackgroundDrawable(ColorDrawable(0))

        val view = LayoutInflater.from(this).inflate(R.layout.layout_dialog_rate, null as ViewGroup?)
        dlg.setContentView(view)

        view.findViewById<TextCustumFontBold?>(R.id.tv_tittle)?.text = getString(R.string.how_many_stars)

        val btnRate = view.findViewById<ButtonCustumFont?>(R.id.btn_rate)
        btnRate?.text = getString(R.string.rate_now)
        btnRate?.setOnClickListener {
            try {
                openPlayStoreForRating()
                RatingBottomSheetDialog.setNeverAskAgain(this, true)
                cancelDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val btnRateNotNow = view.findViewById<ButtonCustumFont?>(R.id.btn_rate_not_now)
        btnRateNotNow?.text = getString(R.string.later)
        btnRateNotNow?.setOnClickListener {
            resetTrackerSession()
            cancelDialog()
        }

        dlg.show()
    }

    override fun onPause() {
        cancelDialog()
        super.onPause()
    }

    private fun cancelDialog() {
        dialog?.let {
            if (it.isShowing) it.dismiss()
        }
        dialog = null
    }
}
