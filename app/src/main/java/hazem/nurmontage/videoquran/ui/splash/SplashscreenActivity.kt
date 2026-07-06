package hazem.nurmontage.videoquran.ui.splash

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityFullscreenBinding

/**
 * Splash-screen activity that displays the app's branding while the
 * [FullscreenActivity] (launcher) or the main flow initialises.
 *
 * Installs the AndroidX SplashScreen API so the system splash
 * seamlessly transitions into the activity content defined by
 * [ActivityFullscreenBinding].
 */
class SplashscreenActivity : BaseActivity() {

    private lateinit var binding: ActivityFullscreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
