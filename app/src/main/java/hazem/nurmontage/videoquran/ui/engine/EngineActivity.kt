package hazem.nurmontage.videoquran.ui.engine

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.StreamInformation
import com.bumptech.glide.Glide
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.constant.SurahNameStyle
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.core.common.Constants
import hazem.nurmontage.videoquran.entity_timeline.EntityAudio
import hazem.nurmontage.videoquran.fragment.AddQuranFragment
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.model.TranslationQuranEntity
import hazem.nurmontage.videoquran.utils.FileUtils
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.MyVibrationHelper
import hazem.nurmontage.videoquran.utils.TimeFormatter
import hazem.nurmontage.videoquran.utils.animator.SmoothTimelineAnimator
import hazem.nurmontage.videoquran.utils.video.SmoothVideoAnimator
import hazem.nurmontage.videoquran.views.BlurredImageView
import hazem.nurmontage.videoquran.views.ButtonCustumFont
import hazem.nurmontage.videoquran.views.CustomDiscreteSeekBar
import hazem.nurmontage.videoquran.views.TextCustumFont
import hazem.nurmontage.videoquran.views.TrackEntityView
import androidx.media3.common.MimeTypes

// Extension function modules (split files) — provide implementations for all
// non-lifecycle methods. Class members take precedence over extensions,
// so removing a member function here makes the extension version active.
// See: EngineAudioManager.kt, EngineEntityManager.kt, EngineUIHelper.kt,
//       EngineCallbacks.kt, EngineTimelineManager.kt, FfmpegCommandBuilder.kt,
//       EngineSaveHelper.kt, BackgroundManager.kt, ExportPipeline.kt,
//       AudioEffectProcessor.kt, AudioLoadingManager.kt, TimelineEngine.kt,
//       VideoPlayerController.kt, TemplateRestorer.kt

@Suppress("TYPE_CHECKING_HAS_RUN_INTO_RECURSIVE_PROBLEM")
class EngineActivity : BaseActivity() {

    // ── Companion ──────────────────────────────────────────
    companion object {
        private const val EXTRACT_AUDIO_VIDEO_PERMISSION_REQUEST_CODE = 12
        private const val FPS = 25
        private const val IMAGE_PERMISSION_REQUEST_CODE = 10
        private const val REQUEST_CODE_AUDIO = 2
        private const val REQUEST_WRITE_EXTERNAL_STORAGE = 1
        private const val VIDEO_PERMISSION_REQUEST_CODE = 11
    }

    // ── Properties ─────────────────────────────────────────
    internal var activityLauncher: ActivityResultLauncher<Intent>? = null
    internal var animator_frame_video: SmoothVideoAnimator? = null
    internal lateinit var blurredImageView: BlurredImageView
    internal lateinit var btnChangeResize: LinearLayout
    internal lateinit var btnIpod: LinearLayout
    internal lateinit var btnPlayPause: ImageButton
    internal lateinit var btnRedo: ImageButton
    internal lateinit var btnToEnd: ImageButton
    internal lateinit var btnToStart: ImageButton
    internal lateinit var btnUndo: ImageButton
    internal lateinit var btn_cancel: ImageButton
    internal lateinit var btn_export: ButtonCustumFont
    internal lateinit var btn_setup_fps: LinearLayout
    internal var dialog: Dialog? = null
    internal var dialogInternet: Dialog? = null
    internal var endFrame: Int = 0
    internal var endTimeAudioVisible: Int = 0
    internal var entityAudio_player: EntityAudio? = null
    internal var entityAudio_visible: EntityAudio? = null
    internal var isOnScroll: Boolean = false
    internal var isToCrop: Boolean = false
    internal lateinit var ivIpod: ImageView
    internal lateinit var ivResize: ImageView
    internal var lastIndexVisible: Int = 0
    internal lateinit var layout_resolution: LinearLayout
    internal var mCurrentFragment: androidx.fragment.app.Fragment? = null
    internal var mIsPlaying: Boolean = false
    internal var mPlayer: MediaPlayer? = null
    internal var mResources: Resources? = null
    internal var mTemplate: Template? = null
    internal var oneExport: Boolean = false
    internal lateinit var seekBar_fps: CustomDiscreteSeekBar
    internal lateinit var seekBar_res: CustomDiscreteSeekBar
    internal lateinit var textChangeResize: TextCustumFont
    internal var timeFormatter: TimeFormatter? = null
    internal lateinit var trackViewEntity: TrackEntityView
    internal lateinit var tv_currentTime: TextView
    internal lateinit var tv_endTime: TextView
    internal lateinit var tv_resolution: TextCustumFont
    internal lateinit var tv_tittle_fragment: TextCustumFont
    internal var uri_bg: String? = null
    internal var valueAnimator: SmoothTimelineAnimator? = null
    internal var vibrationHelper: MyVibrationHelper? = null
    internal var isSaveTmpTemplate: Boolean = true
    internal val executor: java.util.concurrent.Executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    internal val id_ffmpeg = mutableListOf<Long>()
    internal var current_position_time: Int = 0
    internal var startCursur: Int = 0

    // ── Callbacks (via factory functions from EngineCallbacks.kt) ──
    internal val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mCurrentFragment != null) hideFragment() else showExitDialog()
        }
    }

    internal val iTrimLineCallback by lazy { createITrimLineCallback() }
    internal val iAddQuran by lazy { createIAddQuran() }
    internal val iChangeBgCallback by lazy { createIChangeBgCallback() }
    internal val iDimensionCallback by lazy { createIDimensionCallback() }
    internal val iAudioCallback by lazy { createIAudioCallback() }
    internal val iIpadEditCallback by lazy { createIIpadEditCallback() }
    internal val iQuranIconCallback by lazy { createIQuranIconCallback() }
    internal val iEditSName by lazy { createIEditSName() }
    internal val iBismilahEntityCallback by lazy { createIBismilahEntityCallback() }
    internal val iEditTrstEntityCallback by lazy { createIEditTrstEntityCallback() }
    internal val iFontCallback by lazy { createIFontCallback() }
    internal val iEditEntityCallback by lazy { createIEditEntityCallback() }
    internal val iEditMultipleCallback by lazy { createIEditMultipleCallback() }
    internal val iEditMediaCallback by lazy { createIEditMediaCallback() }
    internal val iEdiTextCallback by lazy { createIEdiTextCallback() }
    internal val iTransitionCallback by lazy { createITransitionCallback() }
    internal val iTransitionBismilahCallback by lazy { createITransitionBismilahCallback() }
    internal val frameProcessorRunnable by lazy { createFrameProcessorRunnable() }

    // ── ActivityResult Launchers (MUST stay in class body) ──
    internal val searchAyaResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult ->
        isToCrop = false
        try {
            if (AddQuranFragment.instance != null) {
                AddQuranFragment.instance!!.addAyaIndex()
            } else {
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = AddQuranFragment.getInstance(iAddQuran, mResources!!) as androidx.fragment.app.Fragment?
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                runOnUiThread { setupShowFragment(mResources!!.getString(R.string.quran)) }
            }
        } catch (_: Exception) {}
    }

    internal val nameReaderResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult ->
        isToCrop = false
        val data = activityResult.data
        if (data != null) {
            if (AddQuranFragment.instance != null) {
                val parse = Uri.parse(data.getStringExtra(MimeTypes.BASE_TYPE_AUDIO)!!)
                val stringExtra = data.getStringExtra("path_video_copy")!!
                AddQuranFragment.instance!!.setNameReader(data.getStringExtra("name")!!, parse, stringExtra)
                return@registerForActivityResult
            }
            try {
                val parse2 = Uri.parse(data.getStringExtra(MimeTypes.BASE_TYPE_AUDIO)!!)
                val stringExtra2 = data.getStringExtra("path_video_copy")!!
                val stringExtra3 = data.getStringExtra("name")!!
                val beginTransaction = supportFragmentManager.beginTransaction()
                mCurrentFragment = AddQuranFragment.getInstance(iAddQuran, mResources!!, parse2, stringExtra2, stringExtra3)
                beginTransaction.replace(R.id.m_container, mCurrentFragment!!)
                beginTransaction.commit()
                runOnUiThread { setupShowFragment(mResources!!.getString(R.string.quran)) }
            } catch (_: Exception) {}
        }
    }

    internal val editSurahNameResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult ->
        isToCrop = false
        if (activityResult.resultCode != -1) return@registerForActivityResult
        val data = activityResult.data ?: return@registerForActivityResult
        val stringExtra = data.getStringExtra(Common.READER)!!
        val booleanExtra = data.getBooleanExtra("isBg", false)
        val intExtra = data.getIntExtra("style", 0)
        if (blurredImageView.surahNameEntity!!.index_surah == 0) {
            blurredImageView.surahNameEntity!!.index_surah = data.getIntExtra(StreamInformation.KEY_INDEX, 1)
        }
        blurredImageView.surahNameEntity!!.clrBg = data.getIntExtra("clrBg", ViewCompat.MEASURED_STATE_MASK)
        if (intExtra == SurahNameStyle.NONE.ordinal) {
            blurredImageView.surahNameEntity!!.setAlignment(blurredImageView.updateAlignmentSurah(stringExtra))
        }
        blurredImageView.surahNameEntity!!.setStyle(this@EngineActivity, intExtra, stringExtra, booleanExtra)
        blurredImageView.invalidate()
    }

    internal val editTrslResult: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult ->
        isToCrop = false
        if (activityResult.resultCode != -1) return@registerForActivityResult
        val data = activityResult.data ?: return@registerForActivityResult
        val stringExtra = data.getStringExtra(Common.READER)!!
        val booleanExtra = data.getBooleanExtra("isBg", true)
        val translationQuranEntity = blurredImageView.entity_select as TranslationQuranEntity
        translationQuranEntity.clrBg = data.getIntExtra("clrBg", ViewCompat.MEASURED_STATE_MASK)
        translationQuranEntity.txt = stringExtra
        translationQuranEntity.setHaveBg(booleanExtra)
        blurredImageView.invalidate()
    }

    internal val launchChoiceBgActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult -> onChoiceBgResult(activityResult) }

    internal val launchCropActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult -> onCropResult(activityResult) }

    internal val launchImg: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult -> onImgResult(activityResult) }

    internal val launchVideo: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult -> onVideoResult(activityResult) }

    internal val launchVideoExtract: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { activityResult: ActivityResult -> onVideoExtractResult(activityResult) }

    // ── Misc properties ────────────────────────────────────
    internal val extentions = arrayOf(".mp3", ".ogg", ".acc", ".m4a", ".wav", ".mpeg")
    internal var start_extenstion: Int = 0
    internal val frameLock = Any()
    internal var pendingFramePath: String? = null
    internal var isProcessingFrame: Boolean = false
    internal lateinit var seekbar_fps: CustomDiscreteSeekBar
    internal lateinit var seekbar_resolution: CustomDiscreteSeekBar

    // ── Lifecycle ──────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_time_line)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsetsCompat
        }

        mResources = resources
        setStatusBarColor(-15658735)
        setNavigationBarColor(-14935010)
        wakeLockAcquire()
        showProgress()
        loadTemplate()
        initLauncher()

        vibrationHelper = MyVibrationHelper(this)

        // Initialize the engine UI (order matters: timeline first, then views)
        initTimeLineView()
        initViews()

        checkUriShared()
    }

    override fun onPause() {
        super.onPause()
        try {
            if (isSaveTmpTemplate) saveTemplateTmp()
            if (isToCrop) return
            iTrimLineCallback.onEmptySelect()
            cancelDialog()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onResume() {
        super.onResume()
        isToCrop = false
        isSaveTmpTemplate = true
    }

    override fun onDestroy() {
        super.onDestroy()
        try { Glide.get(this).clearMemory() } catch (_: Exception) {}
        clearFFmpeg()
        releaseWakeLock()
        clearCallback()
        pausePlayer()
        // BUG-E03/E08/E09 fix: release every MediaPlayer still alive. The original
        // code only paused them, leaking native AudioTrack/codec resources for every
        // audio entity created during the editing session.
        try {
            for (entityAudio in trackViewEntity.entityListAudio) {
                try { entityAudio.mediaPlayer?.release() } catch (_: Throwable) {}
                entityAudio.mediaPlayer = null
            }
        } catch (_: Throwable) {}
        try { mPlayer?.release() } catch (_: Throwable) {}
        @Suppress("UNUSED_VARIABLE")
        val _unused = Unit // mPlayer field is val; cannot null-assign here
        // BUG-E01 fix: shut down the activity-scoped single-thread executor.
        // Without this the worker thread (non-daemon) keeps running and any in-flight
        // Runnable retains a reference to the destroyed Activity via captured fields.
        try {
            (executor as? java.util.concurrent.ExecutorService)?.shutdownNow()
        } catch (_: Throwable) {}
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onRequestPermissionsResult(i: Int, strArr: Array<String>, iArr: IntArray) {
        super.onRequestPermissionsResult(i, strArr, iArr)
        handleRequestPermissionsResult(i, strArr, iArr)
    }
}
