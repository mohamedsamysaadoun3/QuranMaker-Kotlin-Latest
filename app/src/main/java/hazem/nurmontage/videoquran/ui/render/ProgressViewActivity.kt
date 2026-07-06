package hazem.nurmontage.videoquran.ui.render

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.Statistics
import com.arthenica.ffmpegkit.StatisticsCallback
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.databinding.ActivityProgressViewBinding
import hazem.nurmontage.videoquran.model.EntityMedia
import hazem.nurmontage.videoquran.model.SquareBitmapModel
import hazem.nurmontage.videoquran.model.Template
import hazem.nurmontage.videoquran.ui.editor.VideoViewActivity
import hazem.nurmontage.videoquran.ui.engine.EngineActivity
import hazem.nurmontage.videoquran.utils.Feadback
import hazem.nurmontage.videoquran.utils.FileMediaScanner
import hazem.nurmontage.videoquran.utils.LocalPersistence
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.audio.AudioUtils
import hazem.nurmontage.videoquran.utils.audio.FfmpegCodecChecker
import hazem.nurmontage.videoquran.core.common.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import kotlin.math.abs
import kotlin.math.max

/**
 * Activity that displays export progress and orchestrates the FFmpeg rendering pipeline.
 *
 * This is the **complete** export activity that:
 * - Displays a progress indicator during video export
 * - Delegates FFmpeg command composition to [ExportCommandBuilder]
 * - Executes pre-render steps (masked segments, video layers, timer overlay)
 * - Manages FFmpeg session lifecycle and cancellation
 * - Handles gallery insertion, sharing, and error reporting
 *
 * **Threading model (Kotlin rewrite)**:
 * - All background work uses `lifecycleScope.launch(Dispatchers.IO)`
 * - UI updates use `withContext(Dispatchers.Main)`
 * - Pre-render methods still accept `CountDownLatch`/`Semaphore` because
 *   [ExportCommandBuilder.buildCommand] coordinates them with the legacy pattern;
 *   the Activity's own flow is fully coroutine-based.
 *
 * Originally: ProgressViewActivity.java (~3190 lines)
 * Converted to: ProgressViewActivity.kt — complete export functionality
 */
class ProgressViewActivity : BaseActivity(), ExportCommandBuilder.PreRenderExecutor {

    // ════════════════════════════════════════════════════════════════════
    //  ViewBinding — ACTIVE
    // ════════════════════════════════════════════════════════════════════
    private lateinit var binding: ActivityProgressViewBinding

    // ════════════════════════════════════════════════════════════════════
    //  Fields
    // ════════════════════════════════════════════════════════════════════

    private var cancelDialog: Dialog? = null
    private var isCancel: Boolean = false

    @Volatile
    private var isDestroy: Boolean = false

    private var mTemplate: Template? = null
    private var mUri: String? = null

    /** Progress bar — accessed via binding.progressHorizontal */

    private var statistics: Statistics? = null

    /** Active FFmpeg session IDs for cancellation. */
    private val id_ffmpeg = mutableListOf<Long>()

    /** Smooth-progress animation state. */
    private var displayedProgress: Float = 0f
    private var targetProgress: Float = 0f
    private var isAnimating: Boolean = false

    /** Wake lock reference for screen-on during export. */
    private var wakeLock: PowerManager.WakeLock? = null

    /** Weighted multi-step render progress tracker. */
    private val renderManager = ExportCommandBuilder.getRenderManager()

    /** Worker thread for running the export after setupCommand. */
    private var workerThread: Thread? = null

    /** Back-press handler — triggers cancel dialog instead of immediate exit. */
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            showCancelDialog()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        // FLAG_KEEP_SCREEN_ON (0x00000400) | FLAG_TURN_SCREEN_ON (0x02000000)
        // Original Java used 1536 which is 0x600 — preserved verbatim.
        @Suppress("MagicNumber")
        window.setFlags(1536, 1536)

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // ViewBinding — inflate and set content view
        binding = ActivityProgressViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // Full dark system bars
        setStatusBarColor(ViewCompat.MEASURED_STATE_MASK)
        setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        // Edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        wakeLockAcquire()

        binding.progressHorizontal.max = 100
        binding.btnCancel.setOnClickListener { showCancelDialog() }

        try {
            startExport()
        } catch (e: Exception) {
            toStudio()
        }
    }

    override fun onPause() {
        super.onPause()
        dismissCancelDialog()
    }

    override fun onDestroy() {
        isDestroy = true
        clearFFmpeg()
        dismissCancelDialog()
        releaseWakeLock()
        super.onDestroy()

        // Clean up template folder using Commons IO on API 26+
        try {
            val template = mTemplate
            if (template != null) {
                Thread {
                    deleteFolderWithCommonsIO(File(template.folder_template ?: ""))
                }.start()
            }
            workerThread?.interrupt()
        } catch (_: Exception) {
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Wake lock
    // ════════════════════════════════════════════════════════════════════

    override fun wakeLockAcquire() {
        try {
            val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: run {
                super.wakeLockAcquire()
                return
            }
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "QuranMaker::ExportWakeLock"
            ).apply {
                acquire(10 * 60 * 1000L) // 10 min max
            }
        } catch (_: Exception) {
            super.wakeLockAcquire()
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    // ════════════════════════════════════════════════════════════════════
    //  Export pipeline
    // ════════════════════════════════════════════════════════════════════

    /**
     * Entry point for the export pipeline.
     *
     * Reads the [Template] from the intent, prepares all media files,
     * detects available FFmpeg codecs, then calls [setupCommand].
     */
    private fun startExport() {
        val templateKey = intent.getStringExtra(Common.TEMPLATE)
            ?: intent.getStringExtra("idTemplate")
            ?: intent.getStringExtra("template_key")

        if (templateKey != null) {
            mTemplate = LocalPersistence.readObjectFromFile(this, templateKey) as? Template
        }
        if (mTemplate == null) {
            @Suppress("DEPRECATION")
            mTemplate = intent.getSerializableExtra("template") as? Template
        }

        val template = mTemplate ?: run {
            toStudio()
            return
        }

        mUri = template.uri_video ?: template.uri_media_video

        lifecycleScope.launch(Dispatchers.IO) {
            // Step 0: Ensure required asset files (font) exist in template folder
            ensureTemplateAssets(template)

            // Step 1: Download / copy all media assets to local storage
            prepareAllMedia(template.entityMediaList, null)

            // Step 2: Detect FFmpeg codecs (callback on main thread)
            withContext(Dispatchers.Main) {
                FfmpegCodecChecker.detectCodecsAsync(object : FfmpegCodecChecker.CodecCallback {
                    override fun onResult(codecInfo: FfmpegCodecChecker.CodecInfo) {
                        setupCommand(codecInfo)
                    }
                })
            }
        }
    }

    /**
     * Prepare all media files — downloads from HTTPS or copies from local URIs.
     *
     * Replaces the original `Executor + Handler` pattern with a coroutine-based
     * approach. Runs on [Dispatchers.IO]; calls [callback] when all media is ready.
     */
    private suspend fun prepareAllMedia(list: List<EntityMedia>?, callback: Runnable?) {
        if (list.isNullOrEmpty()) {
            callback?.run()
            return
        }

        for (media in list) {
            try {
                if (media.end >= media.start && media.path_ffmpeg_effect == null && media.uri != null) {
                    val mediaUri = media.uri
                    val localPath = if (mediaUri!!.startsWith("http")) {
                        AudioUtils.downloadFile(this, mediaUri!!, mTemplate?.folder_template ?: "")
                    } else {
                        AudioUtils.copyFromUri(this, Uri.parse(mediaUri), mTemplate?.folder_template ?: "")
                    }
                    if (localPath != null) {
                        media.path_ffmpeg = localPath
                        media.path_ffmpeg_effect = localPath
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        callback?.run()
    }

    /**
     * Ensure required asset files are present in the template folder.
     *
     * The FFmpeg timer overlay (`drawtext`) requires the font file to exist at
     * `${template.folder_template}/NotoNaskhArabic.ttf`.  The original Java code
     * relied on `FontUtils.copyFontToInternalStorage()` which copies the font to
     * `context.filesDir`, but the FFmpeg command references the template folder
     * path.  If the font is missing, the timer pre-render fails, `timerPath`
     * becomes null, an empty `-i ""` argument is added, and the entire export
     * fails with an FFmpeg error.
     *
     * This method copies the font (and any other required assets) into the
     * template folder so the FFmpeg command can find them.
     */
    private fun ensureTemplateAssets(template: Template) {
        val folder = template.folder_template ?: return
        val fontTarget = File(folder, Constants.FONT_NUMBER)
        if (fontTarget.exists()) return

        try {
            // Try copying from the app's internal files dir first (may have been
            // placed there by FontUtils.copyFontToInternalStorage during save)
            val internalFont = File(filesDir, Constants.FONT_NUMBER)
            if (internalFont.exists()) {
                internalFont.copyTo(fontTarget, overwrite = false)
                return
            }

            // Fallback: copy directly from assets
            assets.open("fonts/arabic/${Constants.FONT_NUMBER}").use { input ->
                FileOutputStream(fontTarget).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Set up the complete FFmpeg export command.
     *
     * Delegates to [ExportCommandBuilder.buildCommand] which builds
     * the complete filter_complex with all entity overlays.
     * Then waits for all pre-render steps to complete before executing.
     */
    private fun setupCommand(codecInfo: FfmpegCodecChecker.CodecInfo) {
        val template = mTemplate ?: return

        val command = ExportCommandBuilder.buildCommand(template, codecInfo, this)

        if (command != null) {
            // Execute the command on a worker thread
            workerThread = Thread {
                try {
                    export(command)
                } catch (e: Exception) {
                    e.printStackTrace()
                    lifecycleScope.launch(Dispatchers.Main) {
                        toStudio()
                    }
                }
            }.also { it.start() }
        } else {
            // buildCommand returned null — one or more pre-render steps failed
            // (e.g. timer overlay failed because font file was missing).
            // Show the error UI instead of silently navigating back.
            showBuildCommandError()
        }
    }

    /**
     * Show error UI when [ExportCommandBuilder.buildCommand] returns null.
     *
     * This happens when a required pre-render step fails (e.g. the timer font
     * file is missing from the template folder).  Previously the export would
     * silently navigate back to the editor; now the user sees the same error
     * layout that FFmpeg failures produce.
     */
    private fun showBuildCommandError() {
        if (isDestroy) return
        try {
            val errorLayout = binding.root.findViewById<LinearLayout>(R.id.layout_error)
            if (errorLayout != null) {
                errorLayout.visibility = View.VISIBLE
            }
            val errorText = binding.root.findViewById<android.widget.TextView>(R.id.tv_error)
            val supportBtn = binding.root.findViewById<android.widget.Button>(R.id.btn_support_team)
            val isArabic = LocaleHelper.getLanguage(this) == "ar"

            if (isArabic) {
                supportBtn?.text = "فريق الدعم"
                errorText?.text = "فشل تجهيز الفيديو للتصدير. تحقق من وجود جميع الملفات المطلوبة."
            } else {
                supportBtn?.text = "Support Team"
                errorText?.text = "Failed to prepare video for export. Please check that all required files are available."
            }

            supportBtn?.setOnClickListener {
                Feadback.reportBug(this@ProgressViewActivity,
                    "buildCommand returned null. folder_template=${mTemplate?.folder_template}, " +
                    "mTimeModel=${mTemplate?.mTimeModel != null}, " +
                    "entityProgressTemplate=${mTemplate?.entityProgressTemplate != null}, " +
                    "uri_bg_ffmpeg=${mTemplate?.uri_bg_ffmpeg}",
                    supportBtn.text.toString())
            }
        } catch (e: Exception) {
            toStudio()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Pre-render executor — implements ExportCommandBuilder.PreRenderExecutor
    // ════════════════════════════════════════════════════════════════════

    override fun executePreRenderMaskRounded(
        model: SquareBitmapModel, durationMs: Int,
        latch: CountDownLatch, semaphore: Semaphore
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            val (args, outputPath, _) = ExportCommandBuilder.preRenderMaskRoundedArgs(
                template, model, durationMs, filesDir
            )

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: Exception) {
            return null
        } finally {
            updateNext(latch, semaphore)
        }
    }

    override fun executePreRenderMaskCircle(
        model: SquareBitmapModel, durationMs: Int,
        latch: CountDownLatch, semaphore: Semaphore
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            val (args, outputPath) = ExportCommandBuilder.preRenderMaskCircleArgs(
                template, model, durationMs, filesDir
            )

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: Exception) {
            return null
        } finally {
            updateNext(latch, semaphore)
        }
    }

    override fun executePreRenderNoMask(
        model: SquareBitmapModel, durationMs: Int,
        latch: CountDownLatch, semaphore: Semaphore, codec: String?
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            val (args, outputPath) = ExportCommandBuilder.preRenderNoMaskArgs(
                template, model, durationMs, codec
            )

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: Exception) {
            return null
        } finally {
            updateNext(latch, semaphore)
        }
    }

    override fun executePreRenderVideo(
        durationMs: Int, latch: CountDownLatch,
        semaphore: Semaphore, codec: String?
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            val (args, outputPath) = ExportCommandBuilder.preRenderVideoArgs(template, durationMs, codec)
            if (args == null) return null.also { updateNext(latch, semaphore) }

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: Exception) {
            return null
        } finally {
            updateNext(latch, semaphore)
        }
    }

    override fun executePreRenderVideoHue(
        durationMs: Int, latch: CountDownLatch,
        semaphore: Semaphore, codec: String?
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            val (args, outputPath) = ExportCommandBuilder.preRenderVideoHueArgs(template, durationMs, codec)
            if (args == null) return null.also { updateNext(latch, semaphore) }

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: Exception) {
            return null
        } finally {
            updateNext(latch, semaphore)
        }
    }

    override fun executeGenerateTimer(
        durationMs: Int, latch: CountDownLatch,
        semaphore: Semaphore
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            val (args, outputPath) = ExportCommandBuilder.generateVideoTimerArgs(template, durationMs)

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: InterruptedException) {
            renderManager.nextTask()
            latch.countDown()
            return null
        } catch (e: Exception) {
            return null
        } finally {
            renderManager.nextTask()
            semaphore.release()
        }
    }

    override fun executeGenerateVideoSegment(
        entityFile: String, outputName: String, filter: String,
        durationSec: Int, index: Int,
        latch: CountDownLatch, semaphore: Semaphore
    ): String? {
        val template = mTemplate ?: return null.also { updateNext(latch, semaphore) }

        try {
            renderManager.addTask("anim prerender", durationSec)
            val (args, outputPath) = ExportCommandBuilder.generateVideoSegmentArgs(
                template, entityFile, outputName, filter, durationSec, index
            )

            val syncLatch = CountDownLatch(1)
            semaphore.acquire()
            val session = FFmpegKit.executeWithArgumentsAsync(args) { _ ->
                syncLatch.countDown()
            }
            id_ffmpeg.add(session.sessionId)

            syncLatch.await()
            return if (File(outputPath).exists()) outputPath else null
        } catch (e: InterruptedException) {
            renderManager.nextTask()
            latch.countDown()
            return null
        } catch (e: Exception) {
            return null
        } finally {
            updateNext(latch, semaphore)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  FFmpeg execution
    // ════════════════════════════════════════════════════════════════════

    /**
     * Execute the final FFmpeg command with progress tracking.
     */
    private fun export(command: Array<String>) {
        if (isCancel || isDestroy) return

        val session: FFmpegSession = FFmpegKit.executeWithArgumentsAsync(
            command,
            FFmpegSessionCompleteCallback { session ->
                if (session == null || isDestroy) return@FFmpegSessionCompleteCallback

                dismissCancelDialog()
                renderManager.nextTask()

                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    // Export complete — animate to 100% then navigate
                    completeProgress()
                } else {
                    // Export failed or cancelled
                    if (!isCancel && !ReturnCode.isCancel(returnCode)) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            showError(session)
                        }
                    }
                }
            },
            null, // Log callback
            StatisticsCallback { stats ->
                if (stats != null) {
                    statistics = stats
                    lifecycleScope.launch(Dispatchers.Main) {
                        updateProgressDialog(stats)
                    }
                }
            }
        )

        id_ffmpeg.add(session.sessionId)
    }

    /**
     * Update the progress indicator from FFmpeg [Statistics].
     */
    private fun updateProgressDialog(stats: Statistics) {
        if (isDestroy) return

        try {
            val time = stats.time
            if (time <= 0) return

            val template = mTemplate ?: return
            val totalDurationMs = max(template.duration, 500)
            val currentStepDuration = renderManager.getCurrentStepDuration()
            if (currentStepDuration <= 0f) return

            var localProgress = (time.toFloat() / 1000f) / currentStepDuration.toFloat()
            if (localProgress > 1f) localProgress = 1f

            targetProgress = renderManager.updateLocalProgress(localProgress) * 100f

            if (!isAnimating) {
                startSmoothAnimation()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Smoothly animate the progress indicator toward [targetProgress].
     */
    private fun startSmoothAnimation() {
        isAnimating = true

        lifecycleScope.launch(Dispatchers.Main) {
            while (!isDestroy) {
                displayedProgress += (targetProgress - displayedProgress) * 0.1f
                val progress = displayedProgress.coerceIn(0f, 100f)
                binding.progressHorizontal.progress = progress.toInt().coerceIn(0, binding.progressHorizontal.max)
                binding.tvProgress.visibility = View.VISIBLE
                binding.tvProgress.text = "${progress.toInt()} %"

                if (abs(displayedProgress - targetProgress) > 0.1f) {
                    delay(16)
                } else {
                    isAnimating = false
                    break
                }
            }
        }
    }

    /**
     * Animate progress to 100% on successful export completion,
     * then insert the video into the gallery and navigate to share.
     */
    private fun completeProgress() {
        isDestroy = true

        lifecycleScope.launch(Dispatchers.Main) {
            displayedProgress += (100.0f - displayedProgress) * 0.45f
            binding.progressHorizontal.progress = minOf(
                maxOf(displayedProgress.toInt(), 0),
                binding.progressHorizontal.max
            )

            val isComplete = binding.progressHorizontal.progress >= 100
            val isNearComplete = abs(displayedProgress - 100.0f) < 0.1f

            if (isComplete || isNearComplete) {
                binding.progressHorizontal.progress = 100
                displayedProgress = 100f
                targetProgress = 100f

                // Insert to gallery
                val uri = mUri
                if (uri != null) {
                    insertToGallery(Uri.parse(uri))
                }

                // Navigate to share/video view
                toShare()
            } else {
                delay(16)
                completeProgress()
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  FFmpeg session management
    // ════════════════════════════════════════════════════════════════════

    private fun clearFFmpeg() {
        for (id in id_ffmpeg) {
            try {
                FFmpegKit.cancel(id)
            } catch (_: Exception) {
            }
        }
        id_ffmpeg.clear()
        try {
            FFmpegKit.cancel()
        } catch (_: Exception) {
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Cancel dialog
    // ════════════════════════════════════════════════════════════════════

    /**
     * Show a confirmation dialog before cancelling the export.
     * Uses the original layout_dialog with custom font buttons.
     */
    private fun showCancelDialog() {
        if (cancelDialog?.isShowing == true) return

        val isArabic = LocaleHelper.getLanguage(this) == "ar"

        val title = if (isArabic) "خروج..." else "Exit..."
        val message = if (isArabic)
            "هل أنت متأكد من مغادرة هذا العمل؟"
        else
            "Are you sure want to leave this work ?"
        val positiveBtn = if (isArabic) "مغادرة" else "Leave"
        val negativeBtn = if (isArabic) "متابعة" else "Continue"

        val dialog = Dialog(this)
        cancelDialog = dialog
        dialog.setCancelable(true)
        dialog.requestWindowFeature(1)
        dialog.window?.setLayout(-1, -2)
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val inflate = LayoutInflater.from(this).inflate(R.layout.layout_dialog, null as ViewGroup?)
        dialog.setContentView(inflate)

        // Try to find custom font views; fall back to AlertDialog if not found
        try {
            val tvTitle = inflate.findViewById<View>(R.id.dialog_title)
            val tvMessage = inflate.findViewById<View>(R.id.dialog_message)
            val btnNo = inflate.findViewById<View>(R.id.dialog_no)
            val btnYes = inflate.findViewById<View>(R.id.dialog_yes)

            if (tvTitle is android.widget.TextView) tvTitle.text = title
            if (tvMessage is android.widget.TextView) tvMessage.text = message
            if (btnNo is android.widget.Button) {
                btnNo.text = positiveBtn
                btnNo.setOnClickListener {
                    isCancel = true
                    clearFFmpeg()
                    toStudio()
                }
            }
            if (btnYes is android.widget.Button) {
                btnYes.text = negativeBtn
                btnYes.setOnClickListener {
                    dismissCancelDialog()
                }
            }
        } catch (e: Exception) {
            // Fallback to AlertDialog if custom layout fails
            dismissCancelDialog()
            cancelDialog = android.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveBtn) { _, _ ->
                    isCancel = true
                    clearFFmpeg()
                    toStudio()
                }
                .setNegativeButton(negativeBtn) { d, _ ->
                    d.dismiss()
                }
                .setCancelable(false)
                .create()
                .also { it.show() }
            return
        }

        dialog.show()
    }

    private fun dismissCancelDialog() {
        val dialog = cancelDialog
        if (dialog != null && dialog.isShowing) {
            dialog.dismiss()
        }
        cancelDialog = null
    }

    // ════════════════════════════════════════════════════════════════════
    //  Error handling
    // ════════════════════════════════════════════════════════════════════

    /**
     * Show error UI when FFmpeg export fails.
     *
     * Collects the full overlay filter chain + FFmpeg session output for bug reporting.
     * Shows the error layout with a "Support Team" button that triggers
     * [Feadback.reportBug] — matching the original Java's RunnableC200010 inner class.
     */
    private fun showError(session: FFmpegSession) {
        try {
            // Build the bug report string: overlay filter chain + session output
            val reportBuilder = StringBuilder()
            val overlayFilter = ExportCommandBuilder.lastOverlayFilter
            if (overlayFilter.isNotEmpty()) {
                reportBuilder.append(overlayFilter).append("\n")
            }
            val sessionOutput = session.output
            if (sessionOutput != null) {
                reportBuilder.append(sessionOutput)
            }
            val reportText = reportBuilder.toString()

            val errorLayout = binding.root.findViewById<LinearLayout>(R.id.layout_error)
            if (errorLayout != null) {
                errorLayout.visibility = View.VISIBLE
            }

            val errorText = binding.root.findViewById<android.widget.TextView>(R.id.tv_error)
            val supportBtn = binding.root.findViewById<android.widget.Button>(R.id.btn_support_team)
            val isArabic = LocaleHelper.getLanguage(this) == "ar"

            if (isArabic) {
                supportBtn?.text = "فريق الدعم"
                errorText?.text = "يوجد مشكلة في هذا التصميم ، لن يتم حفظ هذا الفيديو أخبر فريق الدعم "
            } else {
                supportBtn?.text = "Support Team"
                errorText?.text = "There is a problem with this design, this video won't be saved. Tell the support team."
            }

            // Wire the Support Team button to the bug report email
            supportBtn?.setOnClickListener {
                Feadback.reportBug(this@ProgressViewActivity, reportText, supportBtn.text.toString())
            }
        } catch (e: Exception) {
            toStudio()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  AAC encoder check
    // ════════════════════════════════════════════════════════════════════

    /**
     * Check if the AAC encoder is available in this FFmpeg build.
     */
    @Suppress("unused")
    fun checkAacEncoder() {
        try {
            val tempFile = File.createTempFile("aac_test", ".m4a", cacheDir)
            tempFile.deleteOnExit()
            val args = ExportCommandBuilder.buildAacTestArgs(tempFile.absolutePath)
            FFmpegKit.executeAsync(args.joinToString(" "), FFmpegSessionCompleteCallback { session ->
                if (ReturnCode.isSuccess(session?.returnCode)) {
                    android.util.Log.e("AAC", "AAC encoder is available!")
                } else {
                    android.util.Log.e("AAC", "AAC encoder NOT supported in this build!")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("AAC", "Error checking AAC: ${e.message}")
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  Navigation
    // ════════════════════════════════════════════════════════════════════

    /**
     * Navigate back to the engine/studio screen.
     *
     * Original Java navigates to EngineActivity with the template ID
     * and FLAG_ACTIVITY_CLEAR_TOP to ensure a clean back stack.
     */
    private fun toStudio() {
        if (isDestroy) return
        val intent = Intent(this, EngineActivity::class.java)
        val template = mTemplate
        if (template != null) {
            intent.putExtra(Common.TEMPLATE, template.idTemplate)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // 65536 = 0x10000
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    /**
     * Insert the exported video into the Android Gallery/MediaStore.
     */
    private fun insertToGallery(uri: Uri) {
        if (uri.path == null) return
        try {
            val file = File(uri.path!!)
            if (file.exists()) {
                FileMediaScanner(this, file)
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Navigate to the VideoViewActivity for sharing/previewing the exported video.
     */
    private fun toShare() {
        val template = mTemplate ?: return

        val intent = Intent(this, VideoViewActivity::class.java)

        val surahTemplate = template.entitySurahTemplate
        if (surahTemplate != null) {
            intent.putExtra(Common.SURAH, surahTemplate.name)
            intent.putExtra(Common.READER, surahTemplate.reader)
        } else {
            intent.putExtra(Common.SURAH, "")
            intent.putExtra(Common.READER, "")
        }

        intent.putExtra(Common.TEMPLATE, template.idTemplate)
        intent.data = Uri.parse(template.uri_video)
        intent.addFlags(0x10000) // FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        finish()
    }

    // ════════════════════════════════════════════════════════════════════
    //  RenderManager coordination helpers
    // ════════════════════════════════════════════════════════════════════

    private fun updateNext(latch: CountDownLatch, semaphore: Semaphore) {
        renderManager.nextTask()
        latch.countDown()
        semaphore.release()
    }

    // ════════════════════════════════════════════════════════════════════
    //  File helpers
    // ════════════════════════════════════════════════════════════════════

    /**
     * Delete a folder using Apache Commons IO on API 26+ for better reliability,
     * falling back to manual recursive deletion on older APIs.
     *
     * Matches the original Java's deleteFolderWithCommonsIO() + deleteDirectoryManually().
     */
    private fun deleteFolderWithCommonsIO(file: File?) {
        if (file == null || !file.exists()) return
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                FileUtils.deleteDirectory(file)
                return
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }
        }
        deleteDirectoryManually(file)
    }

    /**
     * Recursively delete a directory and its contents (fallback for API < 26).
     */
    private fun deleteDirectoryManually(file: File) {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    if (child.isDirectory) {
                        deleteDirectoryManually(child)
                    } else {
                        child.delete()
                    }
                }
            }
            file.delete()
        } else {
            file.delete()
        }
    }

    /**
     * Concatenate multiple video segments into a single output file.
     */
    @Suppress("unused")
    private fun concatVideoSegments(segments: List<String>): String? {
        if (segments.isEmpty()) return null
        if (segments.size == 1) return segments[0]

        val template = mTemplate ?: return null
        val dir = template.folder_template ?: return null
        val outputFile = "$dir/final_video.mp4"

        try {
            val concatList = File(dir, "file_list.txt")
            concatList.writeText(segments.joinToString("\n") { "file '$it'" })

            val args = arrayOf(
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatList.absolutePath,
                "-c", "copy",
                outputFile
            )

            FFmpegKit.executeWithArguments(args)
            concatList.delete()

            return if (File(outputFile).exists()) outputFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Create a transparent background PNG for FFmpeg overlay operations.
     */
    @Suppress("unused")
    private fun createTransparentBg(width: Int, height: Int): File {
        val template = mTemplate ?: return File(cacheDir, "transparent.png")
        val dir = template.folder_template ?: cacheDir.absolutePath
        val file = File(dir, "transparent_${width}x${height}.png")
        if (file.exists()) return file

        return ExportCommandBuilder.createTransparentBg(width, height, dir)
    }

    // ════════════════════════════════════════════════════════════════════
    //  Fade / slide helpers — preserved for backward compatibility
    // ════════════════════════════════════════════════════════════════════

    @Suppress("unused")
    fun mFadeFilter(startTime: Float, duration: Float, isIn: Boolean): String {
        return ExportCommandBuilder.mFadeFilter(startTime, duration, isIn)
    }

    @Suppress("unused")
    fun fadeInOut(endTime: Float, fadeInDur: Float, fadeOutDur: Float): String {
        return ExportCommandBuilder.fadeInOut(endTime, fadeInDur, fadeOutDur, mTemplate?.fps ?: 60)
    }

    @Suppress("unused")
    fun slideX(start: Float, duration: Float, offset: Float, scale: Float, from: Float, to: Float): String {
        return ExportCommandBuilder.slideX(start, duration, offset, scale, from, to)
    }

    @Suppress("unused")
    fun mSlideX(start: Float, duration: Float, offset: Float, scale: Float, from: Float, to: Float): String {
        return ExportCommandBuilder.mSlideX(start, duration, offset, scale, from, to)
    }

    // ════════════════════════════════════════════════════════════════════
    //  H.264 codec detection
    // ════════════════════════════════════════════════════════════════════

    /**
     * Detect the best available H.264 encoder based on device API level.
     *
     * Checks for `h264_mediacodec` and `libx264` encoders in the FFmpeg build,
     * then selects the optimal one:
     * - API ≤ 30: prefer `libx264` (more reliable software encoder)
     * - API ≥ 31: prefer `h264_mediacodec` (hardware-accelerated, stable on newer APIs)
     *
     * Matches the original Java's getBestH264Codec() (lines 2881–2924).
     *
     * @return The best codec name, or null if no H.264 encoder is available.
     */
    private fun getBestH264Codec(): String? {
        return try {
            val session = FFmpegKit.execute("-hide_banner -encoders")
            if (!ReturnCode.isSuccess(session.returnCode)) return null
            val output = session.output ?: return null
            val lower = output.lowercase()
            val hasMediaCodec = lower.contains(" h264_mediacodec ")
            val hasLibX264 = lower.contains(" libx264 ")

            when {
                // If only mediacodec is available, use it
                !hasLibX264 && hasMediaCodec -> "h264_mediacodec"
                // API 29 and below: prefer libx264
                Build.VERSION.SDK_INT <= 29 -> {
                    when {
                        hasLibX264 -> "libx264"
                        hasMediaCodec -> "h264_mediacodec"
                        else -> null
                    }
                }
                // API 30: prefer libx264
                Build.VERSION.SDK_INT == 30 -> {
                    when {
                        hasLibX264 -> "libx264"
                        hasMediaCodec -> "h264_mediacodec"
                        else -> null
                    }
                }
                // API 31+: prefer h264_mediacodec (hardware-accelerated)
                else -> {
                    when {
                        hasMediaCodec -> "h264_mediacodec"
                        hasLibX264 -> "libx264"
                        else -> null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private const val TAG = "ProgressViewActivity"
    }
}
