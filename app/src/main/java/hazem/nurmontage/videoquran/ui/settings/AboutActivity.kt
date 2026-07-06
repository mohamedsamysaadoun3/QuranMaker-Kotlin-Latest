package hazem.nurmontage.videoquran.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.AboutAdabters
import hazem.nurmontage.videoquran.adapter.AboutAdabters.ModelAbout
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityAboutBinding
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.utils.ScreenUtils

/**
 * About activity — faithful port of original AboutActivity.java.
 *
 * Displays rich HTML-styled content about the app including:
 * - Free sites for images/videos
 * - Best apps for Quran montage
 * - Free version vs subscription explanation
 * - The story of how the app started
 * - FAQ / Help section
 * - Meaning of NurMontage
 * - Help me help you
 * - Developer signature
 *
 * Billing/subscribe references preserved in informational content only;
 * no billing screens are navigated to.
 */
class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

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

        // Mark about as viewed
        MyPreferences.putVueAbout(this)

        init()
    }

    private fun init() {
        // Back button
        binding.btnOnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // Help button — opens WhatsApp group (btn_help is in the included layout_help_whatsapp)
        binding.layoutHelpWhatsapp.btnHelp.setOnClickListener {
            help()
        }

        // Set help label text
        binding.layoutHelpWhatsapp.tvHelp.text = getString(R.string.help)

        // Build about items list using the original HTML-styled content
        val items = buildAboutItems()

        // Setup RecyclerView with AboutAdabters
        val recyclerView = binding.rv
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AboutAdabters(
            this,
            AppUtils.getAppVersionName(this),
            items,
            ScreenUtils.getScreenWidth(this),
            (ScreenUtils.getScreenHeight(this) * 0.3f).toInt()
        )
    }

    private fun buildAboutItems(): ArrayList<ModelAbout> {
        val items = ArrayList<ModelAbout>()

        // Gravity: Arabic → center (5), English → START
        val gravity = if (LocaleHelper.getLanguage(this) == "ar") 5 else GravityCompat.START

        // 1. Free sites for images/videos
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.about_free_site)}</font>",
                    gravity
                ),
                R.drawable.about_site_video
            )
        )

        // 2. Free sites description
        items.add(
            ModelAbout(
                14,
                Pair(getString(R.string.about_free_site_desc), gravity)
            )
        )

        // 3. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 4. Best apps for Quran montage
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.about_free_app)}</font>",
                    gravity
                ),
                R.drawable.about_best_app
            )
        )

        // 5. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 6. Is the free version enough?
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.about_dont_subscribe)}</font>",
                    gravity
                ),
                R.drawable.about_money
            )
        )

        // 7. Why you might subscribe
        items.add(
            ModelAbout(
                14,
                Pair(
                    "<font color='#ffffff'>${getString(R.string.about_dont_subscribe_why)}</font>",
                    gravity
                )
            )
        )

        // 8. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 9. How the idea started
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.this_begeing_idea)}</font>",
                    gravity
                ),
                R.drawable.about_hazem
            )
        )

        // 10. Idea description
        items.add(
            ModelAbout(
                14,
                Pair(
                    "<font color='#ffffff'>${getString(R.string.this_begeing_idea_decp)}</font>",
                    gravity
                )
            )
        )

        // 11. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 12. Questions / Help
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.about_help_tittle)}</font>",
                    gravity
                ),
                R.drawable.about_help
            )
        )

        // 13. Help body (FAQ)
        items.add(
            ModelAbout(
                14,
                Pair(
                    "<font color='#ffffff'>${getString(R.string.about_help_body)}</font>",
                    gravity
                )
            )
        )

        // 14. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 15. NurMontage meaning
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.nurmontage_means)}</font>",
                    gravity
                ),
                R.drawable.nurmontage_means
            )
        )

        // 16. NurMontage meaning description
        items.add(
            ModelAbout(
                14,
                Pair(
                    "<font color='#ffffff'>${getString(R.string.nurmontage_means_descrp)}</font>",
                    gravity
                )
            )
        )

        // 17. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 18. Help me help you
        items.add(
            ModelAbout(
                19,
                Pair(
                    "<font color=#F8B195>${getString(R.string.help_me_help_you)}</font>",
                    gravity
                ),
                R.drawable.about_help_me_help_you
            )
        )

        // 19. Help me help you description
        items.add(
            ModelAbout(
                14,
                Pair(
                    "<font color='#ffffff'>${getString(R.string.help_me_help_you_descrp)}</font>",
                    gravity
                )
            )
        )

        // 20. Spacer
        items.add(ModelAbout(14, Pair("\n", gravity)))

        // 21. Signature image
        items.add(
            ModelAbout(
                0,
                Pair("", gravity),
                R.drawable.signature_hazem
            )
        )

        return items
    }

    private fun help() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://chat.whatsapp.com/DDdUegENpg83easzYDba2K?mode=wwt")
            intent.setPackage("com.whatsapp")
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
