package hazem.nurmontage.videoquran.ui.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arthenica.ffmpegkit.StreamInformation
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.ColorAdapter
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.databinding.ActivityEditTrslBinding
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.views.EditTextCustumFont

/**
 * Activity for editing translation text with style options.
 * Faithful port of original EditTrslTxtActivity.java.
 *
 * Input (via intent extras — matching Java keys):
 *   - "reader_name" (String) — current translation text
 *   - "surah_name" (String) — surah name (unused in this activity)
 *   - "style" (int) — style
 *   - "index" (String key from StreamInformation.KEY_INDEX) — surah index
 *   - "isBg" (boolean) — whether background is enabled
 *   - "clrBg" (int) — background color
 *
 * Output (via result intent extras — matching Java keys):
 *   - Common.READER (String) — updated translation text
 *   - "style" (int) — style
 *   - "index" (String key) — surah index
 *   - "isBg" (boolean) — whether background is enabled
 *   - "clrBg" (int) — background color
 */
class EditTrslTxtActivity : BaseActivity() {

    private lateinit var binding: ActivityEditTrslBinding

    private var colorAdapter: ColorAdapter? = null
    private var clrBg: Int = 0
    private var style: Int = 0
    private var indexSurah: Int = 0
    private lateinit var editText: EditTextCustumFont

    /** Background colors array — exact values from Java */
    private val BG_COLORS = intArrayOf(-8388608, -1, ViewCompat.MEASURED_STATE_MASK, -2838729, -16777088, -16694239, -13220529, -9404272)

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            closeKeyboard()
            setResult(RESULT_CANCELED, null)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private val iColor = object : ColorAdapter.IColor {
        override fun onColor(color: Int, position: Int) {
            clrBg = color
            scrollToSelectedPosition()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditTrslBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        // Exact values from Java
        setStatusBarColor(-15658735)
        setNavigationBarColor(-14935010)

        val mResources = resources
        if (mResources == null) {
            finish()
            return
        }

        binding.tvTittle.text = mResources.getString(R.string.edit)
        binding.tvAddBg.text = mResources.getString(R.string.add_bg)

        editText = binding.edtReader
        editText.requestFocus()

        // Read intent extras — matching Java keys
        // Also accept "txt" key for compatibility with current Kotlin callers
        val stringExtra = intent.getStringExtra("reader_name") ?: intent.getStringExtra("txt")
        intent.getStringExtra("surah_name") // unused in this activity
        style = intent.getIntExtra("style", 0)
        clrBg = intent.getIntExtra("clrBg", ViewCompat.MEASURED_STATE_MASK)
        binding.checkboxBg.isChecked = intent.getBooleanExtra("isBg", false)
        editText.setText(stringExtra)

        showKeyboard()

        binding.btnOnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        binding.btnDone.setOnClickListener {
            val intent = Intent().apply {
                putExtra(Common.READER, editText.text.toString())
                putExtra("style", style)
                putExtra(StreamInformation.KEY_INDEX, indexSurah)
                putExtra("isBg", binding.checkboxBg.isChecked)
                putExtra("clrBg", clrBg)
            }
            setResult(RESULT_OK, intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
        }

        binding.tvAddBg.setOnClickListener {
            binding.checkboxBg.isChecked = !binding.checkboxBg.isChecked
        }

        initRv()

        binding.checkboxBg.setOnCheckedChangeListener { _, isChecked ->
            updateColorUI(isChecked)
        }
        updateColorUI(binding.checkboxBg.isChecked)
    }

    private fun initRv() {
        val recyclerView: RecyclerView = binding.rvColor
        colorAdapter = ColorAdapter(iColor, BG_COLORS, Utils.indexOf(BG_COLORS, clrBg))
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, LocaleHelper.getLanguage(this) == "ar")
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = colorAdapter
        scrollToSelectedPosition()
    }

    private fun updateColorUI(isChecked: Boolean) {
        binding.rvColor.isEnabled = isChecked
        binding.rvColor.animate().alpha(if (isChecked) 1.0f else 0.4f).setDuration(180L).start()
        colorAdapter?.enabled = isChecked
    }

    private fun scrollToSelectedPosition() {
        val linearLayoutManager = binding.rvColor.layoutManager as? LinearLayoutManager ?: return
        val posSelect = colorAdapter?.getPosSelect() ?: return
        linearLayoutManager.scrollToPositionWithOffset(posSelect, binding.rvColor.width / 2 - 50)
    }

    override fun onPause() {
        closeKeyboard()
        super.onPause()
    }

    private fun showKeyboard() {
        try {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(editText, 1)
        } catch (_: Exception) {
        }
    }

    private fun closeKeyboard() {
        try {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(editText.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
