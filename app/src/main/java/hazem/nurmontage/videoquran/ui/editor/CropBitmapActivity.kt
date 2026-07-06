package hazem.nurmontage.videoquran.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.databinding.ActivityCropBitmapBinding
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.views.TextCustumFont

/**
 * Activity for cropping a bitmap/image using the CropView.
 *
 * Faithful port of original CropBitmapActivity.java.
 * Premium/billing code has been removed as per project policy.
 *
 * Flow:
 *   1. Reads Common.bitmap / Common.rect / Common.radius as the source
 *   2. Loads them into CropView via setBitmap()
 *   3. User adjusts the crop region
 *   4. On "Done", crops the bitmap and stores it back in Common,
 *      returns normalized crop position (x, y, w, h) via intent extras
 *   5. On back/cancel: returns RESULT_CANCELED
 */
class CropBitmapActivity : BaseActivity() {

    private lateinit var binding: ActivityCropBitmapBinding

    companion object {
        var isActive: Boolean = false
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            cancel()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCropBitmapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)

        isActive = true

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        val tvTitle = findViewById<TextCustumFont>(R.id.tv_tittle_fragment)
        tvTitle.text = resources.getString(R.string.choice_screen_ipod)

        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        isActive = false
    }

    private fun init() {
        binding.btnCancel.setOnClickListener {
            cancel()
        }

        if (Common.bitmap == null || Common.rect == null) {
            return
        }

        val cropView = binding.cropView
        cropView.post {
            if (Common.bitmap == null) return@post
            cropView.setBitmap(
                Common.bitmap!!,
                Common.rect!!,
                Common.radius,
                MyPreferences.isShowHint(this)
            )
        }

        binding.btnDone.text = resources.getString(R.string.done)
        binding.btnDone.setOnClickListener {
            // Premium check removed — always allow cropping
            if (!MyPreferences.isShowHint(this)) {
                MyPreferences.putShowHint(this)
            }

            Common.bitmap = cropView.getCroppedBitmap()
            Common.rect = cropView.getRectSquare()

            val intent = Intent().apply {
                putExtra("x", cropView.getmX())
                putExtra("y", cropView.getmY())
                putExtra("w", cropView.getmW())
                putExtra("h", cropView.getmH())
            }
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}
