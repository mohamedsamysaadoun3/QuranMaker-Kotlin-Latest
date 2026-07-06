package hazem.nurmontage.videoquran.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityChoiceLangBinding
import hazem.nurmontage.videoquran.ui.engine.EngineActivity
import hazem.nurmontage.videoquran.ui.home.WorkUserActivity
import hazem.nurmontage.videoquran.ui.splash.FullscreenActivity
import hazem.nurmontage.videoquran.utils.LocaleHelper

/**
 * Language selection activity — faithful port of original ChoiceLangActivity.java.
 *
 * Allows the user to choose between Arabic and English.
 *
 * Features:
 * - `from_setting` extra handling: routes back to SeettingActivity when coming from settings
 * - Uses original radio_selected/radio_unselected drawables
 * - On confirm: persists language, recreates activity, and navigates appropriately
 * - On cancel: navigates based on from_setting flag and existing templates
 *
 * EdgeToEdge and LocaleHelper attached.
 */
class ChoiceLangActivity : BaseActivity() {

    private lateinit var binding: ActivityChoiceLangBinding

    private var isFromSetting = false
    private var lang = "en"
    private var isEnglishSelected = true

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            toStartWork()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityChoiceLangBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        // Read from_setting flag
        if (intent != null) {
            isFromSetting = intent.getBooleanExtra("from_setting", false)
        }

        initViews()
    }

    private fun initViews() {
        val layoutEnglish: RelativeLayout = binding.layoutEnglish
        val layoutArabic: RelativeLayout = binding.layoutArabic
        val radioEnglish: ImageView = binding.radioEnglish
        val radioArabic: ImageView = binding.radioArabic

        // Set button texts from resources
        binding.btnConfirm.text = resources.getString(R.string.confirm)
        binding.tvCancel.text = resources.getString(R.string.cancel)
        binding.tvTittle.text = resources.getString(R.string.select_language)
        binding.tvSubTittle.text = resources.getString(R.string.choose_your_preferred_language)

        // Set initial selection based on current language
        if ("ar" == LocaleHelper.getLanguage(this)) {
            isEnglishSelected = false
            layoutEnglish.setBackgroundResource(R.drawable.bg_item_unselected)
            layoutArabic.setBackgroundResource(R.drawable.bg_item_selected)
            radioArabic.setBackgroundResource(R.drawable.radio_selected)
            radioEnglish.setBackgroundResource(R.drawable.radio_unselected)
        } else {
            isEnglishSelected = true
            layoutEnglish.setBackgroundResource(R.drawable.bg_item_selected)
            layoutArabic.setBackgroundResource(R.drawable.bg_item_unselected)
            radioEnglish.setBackgroundResource(R.drawable.radio_selected)
            radioArabic.setBackgroundResource(R.drawable.radio_unselected)
        }

        // English layout click
        layoutEnglish.setOnClickListener {
            isEnglishSelected = true
            radioEnglish.setBackgroundResource(R.drawable.radio_selected)
            radioArabic.setBackgroundResource(R.drawable.radio_unselected)
            layoutEnglish.setBackgroundResource(R.drawable.bg_item_selected)
            layoutArabic.setBackgroundResource(R.drawable.bg_item_unselected)
        }

        // Arabic layout click
        layoutArabic.setOnClickListener {
            isEnglishSelected = false
            radioEnglish.setBackgroundResource(R.drawable.radio_unselected)
            radioArabic.setBackgroundResource(R.drawable.radio_selected)
            layoutArabic.setBackgroundResource(R.drawable.bg_item_selected)
            layoutEnglish.setBackgroundResource(R.drawable.bg_item_unselected)
        }

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            lang = if (isEnglishSelected) "en" else "ar"
            startLanguageChange()
        }

        // Cancel text
        binding.tvCancel.setOnClickListener {
            toStartWork()
        }
    }

    /**
     * Navigate based on current state:
     * - If from settings → go to SeettingActivity
     * - Otherwise, check for existing templates and route accordingly
     */
    private fun toStartWork() {
        val intent: Intent
        val sharedPreferences = getSharedPreferences("Template", 0)

        if (isFromSetting) {
            intent = Intent(this, SeettingActivity::class.java)
        } else {
            val all = sharedPreferences.all
            if (all != null && all.isNotEmpty()) {
                intent = Intent(this, WorkUserActivity::class.java)
            } else {
                intent = Intent(this, EngineActivity::class.java)
            }
        }

        // Java: setFlags(268468224) = 0x10008000 = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    /**
     * Apply language change:
     * - If same language selected, just go back to settings
     * - Otherwise persist, recreate, and navigate via FullscreenActivity
     */
    private fun startLanguageChange() {
        if (LocaleHelper.getLanguage(this) == lang) {
            // Same language — just go to SeettingActivity
            startActivity(Intent(this, SeettingActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
            return
        }

        // Persist and apply the new language
        LocaleHelper.persist(applicationContext, lang)
        LocaleHelper.onAttach(this)
        recreate()

        // Navigate to FullscreenActivity with from_setting flag to route back to settings
        val intent = Intent(this, FullscreenActivity::class.java)
        intent.putExtra("from_setting", true)
        // Java: setFlags(268468224) = 0x10008000 = FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}
