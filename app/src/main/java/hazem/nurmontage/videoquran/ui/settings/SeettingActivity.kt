package hazem.nurmontage.videoquran.ui.settings

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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivitySeettingBinding
import hazem.nurmontage.videoquran.ui.home.WorkUserActivity
import hazem.nurmontage.videoquran.ui.home.YoutuberActivity
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.views.TextCustumFont
import hazem.nurmontage.videoquran.views.TextCustumFontBold

/**
 * Settings activity — faithful port of original SeettingActivity.java.
 *
 * Displays all settings options: rate, more apps, share, about, youtuber,
 * copyright, language, social media links, and contact.
 *
 * Billing/subscription code removed (buttons hidden as no-ops).
 * EdgeToEdge and LocaleHelper attached.
 */
class SeettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySeettingBinding
    private var dialog: Dialog? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            startActivity(Intent(this@SeettingActivity, WorkUserActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySeettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        val resources = resources
        if (resources == null) {
            finish()
            return
        }

        init()
    }

    override fun onResume() {
        super.onResume()
        // Billing removed — hide restore button since there's no subscription to restore
        binding.btnRestore.visibility = View.GONE
    }

    override fun onPause() {
        cancelDialog()
        super.onPause()
    }

    private fun init() {
        // Back button
        binding.btnOnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // Version text
        setupVersionText()

        // Rate app
        binding.btnRateApp.setOnClickListener {
            openPlayStoreForRating()
        }

        // More apps
        binding.btnMoreApp.setOnClickListener {
            openMoreApps()
        }

        // Share
        binding.btnShare.setOnClickListener {
            shareApp()
        }

        // Language
        binding.btnLang.setOnClickListener {
            changeLang()
        }

        // Copyright
        binding.btnCopyRight.setOnClickListener {
            dialogCopyRight()
        }

        // To Pro — billing removed, hide the button
        binding.btnToPro.visibility = View.GONE

        // About
        binding.btnAbout.setOnClickListener {
            toAbout()
        }

        // Youtuber
        binding.btnImBloger.setOnClickListener {
            toYoutuber()
        }

        // Social media buttons
        binding.btnInstagram.setOnClickListener {
            openInstagramPage()
        }

        binding.btnYoutbe.setOnClickListener {
            openYouTubePage()
        }

        binding.btnTicktock.setOnClickListener {
            openTikTokPage()
        }

        binding.btnWhatsap.setOnClickListener {
            help()
        }
    }

    private fun setupVersionText() {
        try {
            var versionName = packageManager.getPackageInfo(packageName, 0).versionName
            if (versionName != null) {
                versionName = versionName
                    .replace("-nurmontage4kb", "")
                    .replace("-nurmontage16kb", "")
            }
            val tvVersion = binding.tvVersion as TextCustumFont
            if (LocaleHelper.getLanguage(this) == "ar") {
                tvVersion.text = "إصدار : $versionName"
            } else {
                tvVersion.text = "Version : $versionName"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Navigation helpers ────────────────────────────────────────────

    private fun toYoutuber() {
        startActivity(Intent(this, YoutuberActivity::class.java))
        overridePendingTransition(0, 0)
    }

    private fun toAbout() {
        MyPreferences.putVueAbout(this)
        startActivity(Intent(this, AboutActivity::class.java))
        overridePendingTransition(0, 0)
    }

    private fun changeLang() {
        val intent = Intent(this, ChoiceLangActivity::class.java)
        intent.putExtra("from_setting", true)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    // ── Rate / More Apps / Share ──────────────────────────────────────

    private fun openPlayStoreForRating() {
        val packageName = packageName
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        )
        intent.setPackage("com.android.vending")
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Unable to open app store or browser.", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    private fun openMoreApps() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://dev?id=8943620497392395895")
        )
        intent.setPackage("com.android.vending")
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/dev?id=8943620497392395895")
                )
            )
        }
    }

    private fun shareApp() {
        val url: String = if (LocaleHelper.getLanguage(this) == "ar") {
            "أنشئ ريلز قرآنية جميلة بسهولة 🎧✨\nجرّب NurMontage:\nhttps://play.google.com/store/apps/details?id=hazem.nurmontage.videoquran"
        } else {
            "Create beautiful Quran Reels easily 🎧✨\nTry NurMontage:\nhttps://play.google.com/store/apps/details?id=hazem.nurmontage.videoquran"
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
        intent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    // ── Social media ─────────────────────────────────────────────────

    private fun openInstagramPage() {
        val uri = Uri.parse("https://www.instagram.com/nurmontage.app/")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.instagram.android")
        try {
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } catch (_: ActivityNotFoundException) {
            // Ignore
        }
    }

    private fun openYouTubePage() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/@NurMontageApp/")
        )
        intent.setPackage("com.google.android.youtube")
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/@NurMontageApp/")
                )
            )
        }
    }

    private fun openTikTokPage() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.tiktok.com/@nurmontagesupport")
        )
        intent.setPackage("com.zhiliaoapp.musically")
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.tiktok.com/@nurmontagesupport")
                )
            )
        }
    }

    // ── WhatsApp help ────────────────────────────────────────────────

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

    // ── Copyright dialog ─────────────────────────────────────────────

    private fun dialogCopyRight() {
        try {
            dialog = Dialog(this).apply {
                setCancelable(true)
                requestWindowFeature(1)
                window?.setLayout(-1, -2)
                window?.setBackgroundDrawable(ColorDrawable(0))
            }
            val inflate = LayoutInflater.from(this)
                .inflate(R.layout.layout_dialog_copyright, null as ViewGroup?)
            dialog?.setContentView(inflate)

            val dialogTitle = inflate.findViewById<TextCustumFontBold>(R.id.dialog_title)
            val tvMsj = inflate.findViewById<TextCustumFont>(R.id.tv_msj)

            inflate.findViewById<View>(R.id.dialog_no).setOnClickListener {
                cancelDialog()
            }

            if (LocaleHelper.getLanguage(this) == "ar") {
                dialogTitle.text = "تنبيه حقوق الاستخدام ⚠️"
                tvMsj.text =
                    "بعض تسجيلات تلاوات القرّاء محمية بحقوق النشر، وهي مخصّصة للاستخدام الشخصي فقط.\n\nقد تسمح بعض المنصات باستخدام هذه الأصوات دون مشاكل، لكن ذلك لا يُعدّ تصريحًا بالنشر أو الاستخدام التجاري.\n\nللنشر الآمن، يُرجى اختيار قارئ مذكور على أنه مسموح بالنشر أو استخدام صوتك الخاص.\n\nالمستخدم مسؤول بالكامل عن الالتزام بسياسات حقوق النشر الخاصة بكل منصة."
            } else {
                dialogTitle.text = "⚠️ Copyright Notice"
                tvMsj.text =
                    "Some reciters' audio recordings are protected by copyright and are intended for personal use only.\n\nCertain platforms may allow these sounds without issues, but this does not constitute permission to publish or use them commercially.\n\nFor safe publishing, please select a reciter marked as allowed for publishing or use your own audio.\n\nThe user is solely responsible for complying with the copyright policies of each platform."
            }

            dialog?.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelDialog() {
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        dialog = null
    }
}
