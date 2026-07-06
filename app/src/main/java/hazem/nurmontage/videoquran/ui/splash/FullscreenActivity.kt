package hazem.nurmontage.videoquran.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.databinding.ActivityFullscreenBinding
import hazem.nurmontage.videoquran.ui.engine.EngineActivity
import hazem.nurmontage.videoquran.ui.home.WorkUserActivity
import hazem.nurmontage.videoquran.ui.settings.SeettingActivity
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.LocaleHelper

/**
 * Splash / Launcher activity — faithful port of original FullscreenActivity.java.
 *
 * Smart navigation:
 * 1. If launched with `from_setting` extra → route to SeettingActivity
 * 2. If no template temp file and no saved templates → route to WorkUserActivity
 * 3. Otherwise → route to EngineActivity
 *
 * Also adds EdgeToEdge.enable() and attachBaseContext(LocaleHelper).
 */
@SuppressLint("CustomSplashScreen")
class FullscreenActivity : BaseActivity() {

    private lateinit var binding: ActivityFullscreenBinding
    private var keepSplash = true

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplash }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setStatusBarColor(-1)
        setNavigationBarColor(-1)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true

        // Smart navigation after splash delay
        val allTemplates = getSharedPreferences("MTemplate", 0).all

        Handler(Looper.getMainLooper()).postDelayed({
            val targetIntent: Intent

            // 1. If coming from settings language change, go to SeettingActivity
            if (getIntent() != null && getIntent().getBooleanExtra("from_setting", false)) {
                targetIntent = Intent(this, SeettingActivity::class.java)
            } else {
                // 2. Check templates (matches original logic exactly):
                //    If TEMPLATE_TMP is null AND saved templates exist → WorkUserActivity (template picker)
                //    Otherwise → EngineActivity (resume editing or create new)
                val hasTemplateTmp = LocalPersistence.readObjectFromFile(
                    this, Constants.TEMPLATE_TMP
                ) != null

                if (!hasTemplateTmp && allTemplates != null && allTemplates.isNotEmpty()) {
                    targetIntent = Intent(this, WorkUserActivity::class.java)
                } else {
                    targetIntent = Intent(this, EngineActivity::class.java)
                }
            }

            keepSplash = false
            startActivity(targetIntent)
            finish()
        }, SPLASH_DELAY)
    }

    companion object {
        private const val SPLASH_DELAY = 1200L
    }
}
