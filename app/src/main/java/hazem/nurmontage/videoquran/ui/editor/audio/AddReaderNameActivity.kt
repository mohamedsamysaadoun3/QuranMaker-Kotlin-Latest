package hazem.nurmontage.videoquran.ui.editor.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MimeTypes
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityAddReaderNameBinding

/**
 * Activity for adding or editing a reciter/reader name overlay.
 * Faithful port of AddReaderNameActivity.java.
 *
 * Input (via intent extras):
 *   - "name" (String) — current reader name text
 *   - "audio" (String) — audio URI (MimeTypes.BASE_TYPE_AUDIO)
 *   - "path_video_copy" (String) — video copy path
 *
 * Output (via result intent extras, same keys):
 *   - "name" (String) — updated reader name (trimmed, newlines replaced with spaces)
 *   - "audio" (String) — passed through from input
 *   - "path_video_copy" (String) — passed through from input
 */
class AddReaderNameActivity : BaseActivity() {

    private lateinit var binding: ActivityAddReaderNameBinding
    private lateinit var editText: android.widget.EditText

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            closeKeyboard()
            val intent = Intent()
            intent.putExtra("name", getIntent().getStringExtra("name"))
            intent.putExtra(MimeTypes.BASE_TYPE_AUDIO, getIntent().getStringExtra(MimeTypes.BASE_TYPE_AUDIO))
            intent.putExtra("path_video_copy", getIntent().getStringExtra("path_video_copy"))
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(hazem.nurmontage.videoquran.utils.LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddReaderNameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        hideSystemBars()

        // Cancel button
        binding.btnCancel.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // Done button
        binding.btnDone.setOnClickListener {
            closeKeyboard()
            val intent = Intent()
            intent.putExtra("name", editText.text.toString().trim().replace("\n", " "))
            intent.putExtra(MimeTypes.BASE_TYPE_AUDIO, getIntent().getStringExtra(MimeTypes.BASE_TYPE_AUDIO))
            intent.putExtra("path_video_copy", getIntent().getStringExtra("path_video_copy"))
            setResult(RESULT_OK, intent)
            finish()
        }

        // Setup EditText
        val edt = binding.edtReader
        editText = edt
        editText.requestFocus()

        val nameExtra = getIntent().getStringExtra("name")
        if (nameExtra != null && nameExtra.length > 3) {
            editText.setText(nameExtra)
        }

        showKeyboard()
    }

    override fun onPause() {
        closeKeyboard()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closeKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
