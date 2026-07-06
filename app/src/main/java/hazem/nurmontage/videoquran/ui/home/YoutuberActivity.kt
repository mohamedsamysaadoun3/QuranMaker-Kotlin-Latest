package hazem.nurmontage.videoquran.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.YoutuberAdabter
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityYoutuberBinding
import hazem.nurmontage.videoquran.model.YoutuberModel
import hazem.nurmontage.videoquran.utils.AppUtils
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.ScreenUtils
import hazem.nurmontage.videoquran.views.TextCustumFont

class YoutuberActivity : BaseActivity() {

    private lateinit var binding: ActivityYoutuberBinding

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    private val iYoutuber = object : YoutuberAdabter.IYoutuber {
        override fun onClick(link: String) {
            // Try YouTube app intent first, fallback to web
            val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$link"))
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/$link"))
            try {
                try {
                    startActivity(youtubeIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (_: ActivityNotFoundException) {
                startActivity(webIntent)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityYoutuberBinding.inflate(layoutInflater)
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

        // Back button
        binding.btnOnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // Setup RecyclerView with original YouTuber data
        init()

        // Send link button — opens email
        binding.btnSendLnk.setOnClickListener {
            youtuberLnk(this)
        }

        // Set tutorial label
        (binding.tvTutorial as? TextCustumFont)?.text = resources.getString(R.string.my_tutorial)
    }

    private fun init() {
        // Original 7 YouTubers with YouTube video IDs + drawable thumbnails
        val youtubers = ArrayList<YoutuberModel>()
        youtubers.add(YoutuberModel("AjFCfILaEI8", R.drawable.hilal_ytb))
        youtubers.add(YoutuberModel("vMgFSEE2hmg", R.drawable.gasadi_ytb))
        youtubers.add(YoutuberModel("dr1LTEvCEHk", R.drawable.hicham_ytb))
        youtubers.add(YoutuberModel("cRNG62W8ZLk", R.drawable.pakestain))
        youtubers.add(YoutuberModel("tkPEq4qz2OQ", R.drawable.sajad_ytb))
        youtubers.add(YoutuberModel("5IQzSF0wqJE", R.drawable.noor_ytb))
        youtubers.add(YoutuberModel("E9cVRHeDzeU", R.drawable.ytb_yesser))

        val recyclerView = binding.rv
        val adapter = YoutuberAdabter(
            iYoutuber,
            youtubers,
            AppUtils.getAppVersionName(this),
            ScreenUtils.getScreenWidth(this),
            (ScreenUtils.getScreenHeight(this) * 0.35f).toInt()
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
    }

    // ── Email link for YouTubers ─────────────────────────────────────

    private fun isGmailAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "message/rfc822"
        intent.setPackage("com.google.android.gm")
        return !context.packageManager.queryIntentActivities(intent, 0).isEmpty()
    }

    private fun youtuberLnk(context: Context) {
        val subject = resources.getString(R.string.i_m_youtuber)
        val emailAddresses = arrayOf("hazemourari08@gmail.com")

        if (isGmailAvailable(context)) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_EMAIL, emailAddresses)
            intent.putExtra(Intent.EXTRA_BCC, emailAddresses)
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.link))
            intent.type = "message/rfc822"
            intent.setPackage("com.google.android.gm")
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_EMAIL, emailAddresses)
            intent.putExtra(Intent.EXTRA_BCC, emailAddresses)
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.link))
            intent.type = "message/rfc822"
            startActivity(Intent.createChooser(intent, "Send email using"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
