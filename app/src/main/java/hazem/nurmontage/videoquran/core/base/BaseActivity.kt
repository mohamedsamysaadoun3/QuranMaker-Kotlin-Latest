package hazem.nurmontage.videoquran.core.base

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import hazem.nurmontage.videoquran.core.common.Constants

/**
 * Base activity that all QuranMaker Activities inherit from.
 *
 * Provides convenience helpers for manipulating the status bar, navigation bar,
 * and system-ui visibility flags.  These methods are used throughout the app
 * to create a fully-immersive editing experience.
 *
 * Originally: `hazem.nurmontage.videoquran.Base`
 */
abstract class BaseActivity : AppCompatActivity() {

    // ──────────────────────────────────────────────
    //  System-bars immersion
    // ──────────────────────────────────────────────

    /**
     * Hides both the status bar and navigation bar so the activity
     * runs in full-immersive mode.
     */
    fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hideSystemBarsApi30(-1)
        } else {
            hideSystemBarsBelowApi30(-1)
        }
    }

    /**
     * API 30+ implementation – uses [WindowInsetsControllerCompat].
     */
    private fun hideSystemBarsApi30(statusBarColor: Int) {
        val window = window
        window.statusBarColor = statusBarColor
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsets.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Pre-API 30 implementation – uses legacy system-ui flags.
     */
    @Suppress("DEPRECATION")
    private fun hideSystemBarsBelowApi30(statusBarColor: Int) {
        val window = window
        window.addFlags(WindowManagerFlags.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManagerFlags.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = statusBarColor
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        supportActionBar?.hide()
        window.addFlags(WindowManagerFlags.FLAG_KEEP_SCREEN_ON)
    }

    // ──────────────────────────────────────────────
    //  Status-bar color helpers
    // ──────────────────────────────────────────────

    /**
     * Sets the status-bar color to the given [color].
     */
    fun setStatusBarColor(color: Int) {
        val window = window
        window.clearFlags(WindowManagerFlags.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManagerFlags.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = color
    }

    /**
     * Sets the status-bar color to the application default
     * ([Constants.COLOR_STATUS_BAR_DEFAULT]).
     */
    fun setStatusBarColor() {
        val window = window
        window.clearFlags(WindowManagerFlags.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManagerFlags.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Constants.COLOR_STATUS_BAR_DEFAULT
    }

    // ──────────────────────────────────────────────
    //  Navigation-bar color helper
    // ──────────────────────────────────────────────

    /**
     * Sets the navigation-bar color to the given [color].
     */
    fun setNavigationBarColor(color: Int) {
        val window = window
        window.clearFlags(WindowManagerFlags.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManagerFlags.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.navigationBarColor = color
    }

    // ──────────────────────────────────────────────
    //  Light / dark status-bar icons
    // ──────────────────────────────────────────────

    /**
     * Toggles light (dark icons) or dark (light icons) status-bar appearance.
     *
     * @param light `true` to use dark status-bar icons (light status bar),
     *              `false` to use light icons (dark status bar).
     */
    @Suppress("DEPRECATION")
    fun setLightStatusBar(light: Boolean) {
        val decorView = window.decorView
        val flags = decorView.systemUiVisibility
        decorView.systemUiVisibility =
            if (light) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            else flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    }

    // ──────────────────────────────────────────────
    //  Wake-lock helper
    // ──────────────────────────────────────────────

    /**
     * Acquires a screen-on wake lock so the display stays active.
     * Silently catches any exceptions (e.g. if the window is not attached).
     */
    open fun wakeLockAcquire() {
        try {
            window.addFlags(0x00000080) // Original Java: 128 — legacy wake lock flag
        } catch (_: Exception) {
            // Window may not be attached yet – safe to ignore
        }
    }

    // ──────────────────────────────────────────────
    //  Private: Window-flag constants (readability)
    // ──────────────────────────────────────────────
    private object WindowManagerFlags {
        const val FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000.toInt()
        const val FLAG_TRANSLUCENT_STATUS = 0x04000000
        const val FLAG_KEEP_SCREEN_ON = 0x00000400
    }
}
