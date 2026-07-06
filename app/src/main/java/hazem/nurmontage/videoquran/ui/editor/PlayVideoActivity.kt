package hazem.nurmontage.videoquran.ui.editor

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityPlayVideoBinding

/**
 * Simple video playback preview activity.
 * Faithful port of original PlayVideoActivity.java.
 *
 * Receives a video URI via intent data and plays it in a VideoView
 * with standard media controls.
 */
class PlayVideoActivity : BaseActivity() {

    private lateinit var binding: ActivityPlayVideoBinding
    private var mediaController: MediaController? = null
    private lateinit var parentLayout: RelativeLayout

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            pause()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPlayVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), OnApplyWindowInsetsListener { view, windowInsets ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        })

        parentLayout = binding.parentLayout

        if (intent != null && intent.data != null) {
            val videoView = binding.videoView
            val mc = MediaController(this)
            mediaController = mc
            mc.setMediaPlayer(videoView)
            mc.setAnchorView(videoView)
            videoView.setMediaController(mc)
            videoView.setVideoURI(intent.data)

            videoView.setOnCompletionListener { mediaPlayer ->
                if (mediaController == null || mediaController!!.isShowing) return@setOnCompletionListener
                mediaController!!.show()
            }

            videoView.setOnPreparedListener { mediaPlayer ->
                adjustVideoViewSize(mediaPlayer)
            }

            videoView.start()
        }

        binding.btnOnBack.setOnClickListener {
            pause()
            finish()
        }
    }

    private fun adjustVideoViewSize(mediaPlayer: MediaPlayer?) {
        if (mediaPlayer == null) return
        var videoWidth = mediaPlayer.videoWidth
        var videoHeight = mediaPlayer.videoHeight
        var width = parentLayout.width
        var height = parentLayout.height
        val f = videoWidth.toFloat() / videoHeight.toFloat()
        val widthRatio = width.toFloat()
        val heightRatio = height.toFloat()
        if (f > widthRatio / heightRatio) {
            height = (widthRatio / f).toInt()
        } else {
            width = (heightRatio * f).toInt()
        }
        val layoutParams = RelativeLayout.LayoutParams(width, height)
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        binding.videoView.layoutParams = layoutParams
    }

    private fun pause() {
        val videoView = binding.videoView
        if (!videoView.isPlaying) return
        videoView.pause()
    }

    override fun onPause() {
        pause()
        super.onPause()
    }

    override fun onDestroy() {
        val videoView = binding.videoView
        if (videoView != null) {
            videoView.pause()
        }
        super.onDestroy()
    }
}
