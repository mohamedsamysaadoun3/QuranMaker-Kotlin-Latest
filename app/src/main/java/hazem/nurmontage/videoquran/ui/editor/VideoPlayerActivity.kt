package hazem.nurmontage.videoquran.ui.editor

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : BaseActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var videoUri: Uri? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            releasePlayer()
            returnAct()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        videoUri = intent.data
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemBars()
    }

    private fun initializePlayer() {
        if (player != null || videoUri == null) return

        val exoPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
            .build()
        player = exoPlayer
        binding.playerView.player = exoPlayer
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUri!!))
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.prepare()
        exoPlayer.play()
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                retryWithFallbackDecoder()
            }
        })
    }

    private fun retryWithFallbackDecoder() {
        if (videoUri == null) return
        val newPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(DefaultRenderersFactory(this).setEnableDecoderFallback(true))
            .build()
        binding.playerView.player = newPlayer
        newPlayer.setMediaItem(MediaItem.fromUri(videoUri!!))
        newPlayer.prepare()
        newPlayer.play()
        player?.release()
        player = newPlayer
    }

    private fun releasePlayer() {
        player?.let {
            binding.playerView.useController = false
            binding.playerView.player = null
            it.release()
        }
        player = null
    }

    private fun returnAct() {
        finish()
    }

    private fun setupButtons() {
        val btnBack: View? = binding.playerView.findViewById(R.id.btnBack)
        val btnRotate: View? = binding.playerView.findViewById(R.id.btnRotate)
        val btnPlayPause: View? = findViewById(R.id.btn_play_pause)

        btnBack?.setOnClickListener {
            releasePlayer()
            returnAct()
        }

        btnRotate?.setOnClickListener {
            requestedOrientation = if (requestedOrientation == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        btnPlayPause?.setOnClickListener {
            player?.let { p ->
                if (p.isPlaying) {
                    p.pause()
                    (it as? android.widget.ImageButton)?.setImageResource(R.drawable.play_arrow_24px)
                } else {
                    p.play()
                    (it as? android.widget.ImageButton)?.setImageResource(R.drawable.pause_24px)
                }
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)
    }

    companion object {
        private const val SEEK_INCREMENT_MS = 5000L
    }
}
