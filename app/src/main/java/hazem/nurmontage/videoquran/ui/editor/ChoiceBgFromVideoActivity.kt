package hazem.nurmontage.videoquran.ui.editor

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.databinding.ActivityChoiceBgFromVideoBinding
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.views.VideoFrameSelectorView

/**
 * Activity for selecting a video frame as a background image.
 * Faithful port of original ChoiceBgFromVideoActivity.java.
 *
 * Flow:
 *   1. Receives a video URI via intent data
 *   2. Shows the video preview and a frame scrubber (VideoFrameSelectorView)
 *   3. User scrubs to the desired frame
 *   4. On "Done", stores the frame bitmap in Common.bitmap and returns RESULT_OK
 *   5. On "Cancel" or back-press, returns RESULT_CANCELED
 */
class ChoiceBgFromVideoActivity : BaseActivity() {

    private lateinit var binding: ActivityChoiceBgFromVideoBinding
    private var imageView: ImageView? = null

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
        binding = ActivityChoiceBgFromVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

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

        val mResources = resources
        if (mResources != null) {
            binding.tvTittleFragment.text = mResources.getString(R.string.choice_bg)
        }

        binding.btnCancel.setOnClickListener {
            cancel()
        }

        if (intent != null) {
            init(intent.data)
        }
    }

    private fun init(uri: Uri?) {
        if (uri == null) {
            return
        }
        imageView = binding.ivView
        val videoFrameSelectorView = binding.frameSelectorView
        videoFrameSelectorView.setVideoUri(uri)
        videoFrameSelectorView.setOnFrameSelectedListener(object : VideoFrameSelectorView.OnFrameSelectedListener {
            override fun onFrameSelected(value: Int, bitmap: Bitmap) {
                if (imageView == null) return
                imageView?.setImageBitmap(bitmap)
            }
        })
        binding.btnDone.setOnClickListener {
            val frame = videoFrameSelectorView.getFrameBitmap()
            if (frame != null) {
                Common.bitmap = frame.getBitmap()
                setResult(RESULT_OK, Intent())
                finish()
            }
        }
    }
}
