package hazem.nurmontage.videoquran.ui.editor.text

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.WordAyaAdabter
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityTextEditBinding
import hazem.nurmontage.videoquran.model.WordModel
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.WordProcessor
import hazem.nurmontage.videoquran.views.ButtonCustumFont

class TextEditActivity : BaseActivity() {

    private lateinit var binding: ActivityTextEditBinding

    private var startIndex: Int = 0
    private var endIndex: Int = 0
    private var wordAyaAdabter: WordAyaAdabter? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            setResult(RESULT_OK, null)
            finish()
        }
    }

    companion object {
        fun findFirstDigitIndex(text: String?): Int {
            if (text != null && text.isNotEmpty()) {
                for (i in text.indices) {
                    if (Character.isDigit(text[i])) {
                        return i
                    }
                }
            }
            return -1
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityTextEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setStatusBarColor(-13421771)
        setNavigationBarColor(-13421771)

        val insetsController = ViewCompat.getWindowInsetsController(window.decorView)
        insetsController?.isAppearanceLightStatusBars = false
        insetsController?.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        // Cancel button
        binding.btnCancel.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // Done button
        val btnDone = binding.btnDone as ButtonCustumFont
        btnDone.text = resources.getString(R.string.done)
        btnDone.setOnClickListener {
            val intent = Intent()
            val selectedAya = getSelectedAya()
            val firstDigitIndex = findFirstDigitIndex(selectedAya)
            intent.putExtra("start_index", startIndex)
            intent.putExtra("end_index", endIndex)
            if (firstDigitIndex == -1) {
                intent.putExtra("aya", selectedAya)
            } else {
                val substring = selectedAya.substring(0, firstDigitIndex)
                try {
                    var parsedInt = selectedAya.substring(firstDigitIndex).toInt()
                    if (parsedInt > 286) {
                        parsedInt = 286
                    }
                    intent.putExtra("number", parsedInt)
                    intent.putExtra("index", firstDigitIndex)
                    intent.putExtra("aya", substring + " نص")
                } catch (e: Exception) {
                    intent.putExtra("aya", substring)
                }
            }
            setResult(RESULT_OK, intent)
            finish()
        }

        // Read intent extras
        val aya = intent.getStringExtra("aya")
        val completeAya = intent.getStringExtra("complete_aya")
        startIndex = intent.getIntExtra("start_index", -1)
        endIndex = intent.getIntExtra("end_index", -1)

        if (completeAya != null) {
            init(aya, completeAya)
        }
    }

    private fun getSelectedAya(): String {
        val sb = StringBuilder()
        val list = wordAyaAdabter?.getList() ?: return ""
        startIndex = -1
        var count = 0
        for (i in list.indices) {
            val wordModel = list[i]
            if (wordModel.isSelected) {
                if (startIndex == -1) {
                    startIndex = i
                }
                count++
                sb.append(wordModel.w).append(" ")
            }
        }
        endIndex = count + 1 + (if (startIndex != -1) startIndex else 0)
        if (startIndex != -1) {
            startIndex = WordProcessor.mapIndexAfterGroupReverse(startIndex, 4, list.size)
            endIndex = WordProcessor.mapIndexAfterGroupReverse(endIndex, 4, list.size)
        }
        return sb.toString().trim()
    }

    private fun init(aya: String?, completeAya: String) {
        val wordProcessor = WordProcessor()
        val split = completeAya.trim().split("\\s+".toRegex())
        val arrayList = mutableListOf<WordModel>()

        if (startIndex == endIndex) {
            // Single index: find and highlight the matching phrase
            val split2 = aya?.split("\\s+".toRegex()) ?: emptyList()
            val indexOf = completeAya.indexOf(aya ?: "")
            var isPremium = indexOf == 0
            var charIndex = 0
            var wordIndex = 0

            for (textValue in split) {
                if (!isPremium) {
                    if (charIndex == indexOf) {
                        isPremium = true
                    }
                    charIndex += textValue.length + 1
                }
                if (isPremium && wordIndex < split2.size) {
                    val equals = textValue == split2[wordIndex]
                    arrayList.add(WordModel(textValue, equals))
                    if (equals) {
                        wordIndex++
                    }
                } else {
                    arrayList.add(WordModel(textValue, false))
                }
            }
        } else {
            // Range selection: mark words in the range
            for (i in split.indices) {
                arrayList.add(WordModel(split[i], i >= startIndex && i < endIndex))
            }
        }

        wordAyaAdabter = WordAyaAdabter(wordProcessor.reverseInGroupsOfFour(arrayList))

        val recyclerView = binding.rv
        recyclerView.layoutManager = GridLayoutManager(this, 4)
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = null
        recyclerView.adapter = wordAyaAdabter
    }
}
