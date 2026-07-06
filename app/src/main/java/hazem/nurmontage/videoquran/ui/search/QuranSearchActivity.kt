package hazem.nurmontage.videoquran.ui.search

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.AyaAdapter
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.model.ItemQuranSearch
import hazem.nurmontage.videoquran.utils.JavaBM
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.QuranPreference
import hazem.nurmontage.videoquran.utils.RemoveTashkeel
import hazem.nurmontage.videoquran.utils.Utils
import hazem.nurmontage.videoquran.views.ButtonCustumFont
import hazem.nurmontage.videoquran.views.TextCustumFont
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Activity for searching Quran verses by Arabic text or Surah name.
 *
 * Supports two search modes:
 * 1. **Surah search**: Typing a Surah name (e.g. "البقرة" or "البقرة 5") loads
 *    all verses from that Surah (or a specific verse).
 * 2. **Full-text search**: Typing any Arabic phrase triggers a Boyer-Moore
 *    search across the entire Quran text file.
 *
 * Architecture decisions:
 * - Background thread search with [Handler] for UI updates (no coroutines yet,
 *   to preserve behavioral parity with the original).
 * - [JavaBM] (Boyer-Moore) provides O(n/m) pattern matching on the Quran text.
 * - [RemoveTashkeel] normalizes Arabic text before matching.
 * - Stream cleanup uses [closeQuranStreams] with try-catch for guaranteed cleanup.
 * - [isRun] volatile flag enables cooperative cancellation of background threads.
 * - Search results are cached via [QuranPreference] for quick re-launch.
 * - [OnBackPressedCallback] handles back navigation with proper result codes.
 *
 * Result codes:
 * - `RESULT_OK (-1)`: User selected a verse range
 * - `RESULT_CANCELED (0)`: User cancelled without selection
 *
 * Converted from QuranSearchActivity.java.
 */
class QuranSearchActivity : BaseActivity() {

    // ── Views ────────────────────────────────────────────────────────
    private var btnDone: ButtonCustumFont? = null
    private var countAya: TextView? = null
    private var editText: EditText? = null
    private var recyclerView: RecyclerView? = null
    private var searchProgressBar: ProgressBar? = null

    // ── Search state ─────────────────────────────────────────────────
    @Volatile private var indexAya: Int = 0
    @Volatile private var indexSurah: Int = 0
    private var isFullSurah: Boolean = false
    @Volatile private var isRun: Boolean = false
    private var javaBM: JavaBM? = null
    private var lastKey: String? = null
    private var lastSearchKey: String? = null
    private var mFrom: Int = -1
    private var mTo: Int = -1
    private var searchQuranAdapter: AyaAdapter? = null
    private var surahNames: Array<String>? = null
    private var thread: Thread? = null
    private var runnableBySurah: Runnable? = null

    // ── Quran file streams (managed manually for bg thread) ──────────
    private var inQuran: InputStream? = null
    private var bufferedReaderQuran: BufferedReader? = null

    private val handler = Handler(Looper.getMainLooper())

    // ── Back press handler ───────────────────────────────────────────
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            closeKeyboard()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    // ── Adapter callback ─────────────────────────────────────────────
    private val iSearchQuranCallback = object : AyaAdapter.ISearchQuranCallback {
        override fun onClick(from: Int, to: Int, item: ItemQuranSearch) {
            indexSurah = item.surahIndex
            if (!isFullSurah) {
                // Single verse selection: auto-confirm
                val verseTo = item.to - 1
                mTo = verseTo
                mFrom = verseTo
                btnDone?.performClick()
            } else {
                // Range selection: update the done button text
                mFrom = from
                mTo = to
                btnDone?.let { btn ->
                    if (btn.visibility != View.VISIBLE) {
                        btn.visibility = View.VISIBLE
                    }
                    btn.setText(getString(R.string.from_to, from + 1, to + 1))
                }
            }
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        setContentView(R.layout.activity_quran_search)
        setStatusBarColor(-15658732)
        setNavigationBarColor(-15658732)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        surahNames = resources.getStringArray(R.array.sura_names)

        // Back button
        findViewById<View>(R.id.btn_onBack).setOnClickListener {
            closeKeyboard()
            setResult(RESULT_CANCELED)
            finish()
        }

        // Done button: save selection and return result
        btnDone = findViewById(R.id.btn_done) as ButtonCustumFont
        btnDone?.setOnClickListener {
            if (mFrom == -1) return@setOnClickListener
            QuranPreference.savePreferencesSearch(
                this@QuranSearchActivity,
                indexSurah, mFrom, mTo,
                editText?.text?.toString() ?: ""
            )
            setResult(RESULT_OK)
            finish()
        }

        searchProgressBar = findViewById(R.id.progress)
        countAya = findViewById(R.id.tv_count_aya)

        (findViewById(R.id.tv_tittle) as TextCustumFont)
            .setText(getString(R.string.search))

        @Suppress("DEPRECATION")
        recyclerView = findViewById<RecyclerView>(R.id.rv_search_quran).apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(applicationContext)
            setItemViewCacheSize(20)
            isDrawingCacheEnabled = true
            itemAnimator = null
            drawingCacheQuality = View.DRAWING_CACHE_QUALITY_HIGH
        }

        searchQuranAdapter = AyaAdapter(iSearchQuranCallback)
        recyclerView?.adapter = searchQuranAdapter

        editText = findViewById<EditText>(R.id.edt_search_quran).apply {
            hint = getString(R.string.hint_search_quran)
            typeface = Typeface.createFromAsset(resources.assets, "fonts/ReadexPro_Medium.ttf")
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
                closeKeyboard()
                try {
                    performSearch()
                } catch (_: InterruptedException) {
                }
                true
            }
        }

        findViewById<View>(R.id.btn_search).setOnClickListener {
            closeKeyboard()
            try {
                performSearch()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                e.printStackTrace()
            }
        }

        lastSearch()
    }

    override fun onResume() {
        super.onResume()
        editText?.requestFocus()
        showKeyboard()
    }

    override fun onPause() {
        closeKeyboard()
        super.onPause()
    }

    // ── Keyboard management ──────────────────────────────────────────

    fun showKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        } catch (_: Exception) {
        }
    }

    fun closeKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText?.windowToken, 0)
        } catch (_: Exception) {
        }
    }

    // ── Search logic ─────────────────────────────────────────────────

    private fun lastSearch() {
        val last = QuranPreference.getLastSearch(this) ?: return
        if (last.isEmpty()) return
        try {
            editText?.setText(last)
            performSearch()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            e.printStackTrace()
        }
    }

    private fun updateCount() {
        countAya?.text = "الآيـــات : (${searchQuranAdapter?.size ?: 0})"
    }

    private fun resetFromTo() {
        mFrom = -1
        mTo = -1
        btnDone?.visibility = View.GONE
    }

    /**
     * Main search entry point. Parses the query to determine if it's
     * a Surah name search or a full-text Quran search.
     */
    @Throws(InterruptedException::class)
    private fun performSearch() {
        resetFromTo()
        val query = editText?.text?.toString()?.trim() ?: ""
        if (query.isEmpty() || query == "--" || !Utils.isProbablyLArabic(query)) return

        if (lastSearchKey != query) {
            lastSearchKey = query.replace("\"", "")
            searchQuranAdapter?.clear()

            val parts = lastSearchKey!!.split(" ")

            if (parts.size == 1) {
                // Single word: check if it's a Surah name
                val surahIdx = if (lastSearchKey!!.contains("\u0639\u0645\u0631\u0627\u0646")) 3
                               else getIndexSurah(parts[0])
                if (surahIdx != -1) {
                    indexSurah = surahIdx
                    indexAya = -1
                    searchBySurah()
                    return
                }
            } else if (parts.size == 2) {
                // Two words: "SurahName verseNumber"
                val surahIdx = if (lastSearchKey!!.contains("\u0639\u0645\u0631\u0627\u0646")) 3
                               else getIndexSurah(parts[0])
                try {
                    val verseNum = parts[1].toInt()
                    if (surahIdx != -1) {
                        indexSurah = surahIdx
                        indexAya = verseNum
                        searchBySurah()
                        return
                    }
                } catch (_: NumberFormatException) {
                }
            }

            // Full-text search using Boyer-Moore
            isFullSurah = false
            if (javaBM == null) {
                javaBM = JavaBM()
            }
            javaBM!!.setPattern(RemoveTashkeel.removeTashkeel(lastSearchKey) ?: "")
            searchAllQuran()
        }
    }

    /**
     * Normalize Arabic text for fuzzy matching:
     * - Remove "ال" prefix
     * - Normalize alef variants (أإآ → ا)
     * - Normalize ى → ي, ة → ه
     * - Remove tashkeel diacritics
     */
    private fun normalizeArabic(text: String?): String {
        if (text == null) return ""
        var result = text.trim()
        if (result.startsWith("\u0627\u0644")) { // "ال"
            result = result.substring(2)
        }
        return result
            .replace("\u0623", "\u0627") // أ → ا
            .replace("\u0625", "\u0627") // إ → ا
            .replace("\u0622", "\u0627") // آ → ا
            .replace("\u0649", "\u064A") // ى → ي
            .replace("\u0629", "\u0647") // ة → ه
            .replace(Regex("[\\u064B-\\u065F]"), "") // remove tashkeel
    }

    /**
     * Find the Surah index by matching the normalized query against the Surah names array.
     */
    private fun getIndexSurah(query: String): Int {
        val normalized = normalizeArabic(query)
        val names = surahNames ?: return -1
        for ((i, name) in names.withIndex()) {
            val surahName = normalizeArabic(name.split("-")[0].trim())
            if (surahName.contains(normalized)) {
                return i
            }
        }
        return -1
    }

    /**
     * Search the entire Quran text file on a background thread.
     * Results are posted to the UI thread one at a time for progressive display.
     */
    @Throws(InterruptedException::class)
    private fun searchAllQuran() {
        stopCurrentSearchThread()
        isRun = true

        thread = Thread {
            handler.post { searchProgressBar?.visibility = View.VISIBLE }

            try {
                inQuran = assets.open("quran/quran-simple.txt")
                bufferedReaderQuran = BufferedReader(InputStreamReader(inQuran))

                while (isRun) {
                    val line = bufferedReaderQuran?.readLine() ?: break
                    if (line.isEmpty()) continue

                    val parts = line.split("\\|".toRegex())
                    if (parts.size < 3) continue

                    val surahIdx = parts[0].toInt() - 1
                    val ayaIdx = parts[1].toInt() - 1
                    val ayaText = parts[2]

                    // Skip Bismillah prefix (first ayah of each surah except Al-Fatiha)
                    val textToSearch = if (surahIdx > 0 && ayaIdx == 0 &&
                        ayaText.contains("\u0628\u0651\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650")) {
                        ayaText.substring(40)
                    } else {
                        ayaText
                    }

                    val cleanText = RemoveTashkeel.removeTashkeel(textToSearch) ?: ""
                    val matchPos = javaBM?.match(cleanText) ?: -1

                    if (matchPos != -1) {
                        handler.post {
                            if (searchQuranAdapter != null) {
                                val countIndex = Utils.countIndex(
                                    Utils.countSpace(matchPos, cleanText), ayaText
                                )
                                searchQuranAdapter!!.add(
                                    ItemQuranSearch(
                                        ayaText,
                                        surahNames!![surahIdx],
                                        ayaIdx + 1,
                                        surahIdx,
                                        countIndex,
                                        Utils.countIndex(
                                            countIndex,
                                            Utils.countSpace(javaBM!!.getPattern()),
                                            ayaText
                                        )
                                    )
                                )
                                updateCount()
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                System.err.println("Error reading Quran file: ${e.message}")
                e.printStackTrace()
            } finally {
                closeQuranStreams()
                handler.post {
                    if (searchQuranAdapter?.size == 0) {
                        updateCount()
                    }
                    searchProgressBar?.visibility = View.GONE
                }
            }
        }.also { it.start() }
    }

    /**
     * Search by Surah name and optional verse number.
     * Loads all verses from the specified Surah on a background thread.
     */
    @Throws(InterruptedException::class)
    private fun searchBySurah() {
        isFullSurah = true

        if (runnableBySurah == null) {
            runnableBySurah = Runnable {
                handler.post { searchProgressBar?.visibility = View.VISIBLE }

                try {
                    inQuran = assets.open("quran/quran-simple.txt")
                    bufferedReaderQuran = BufferedReader(InputStreamReader(inQuran))

                    while (isRun) {
                        val line = bufferedReaderQuran?.readLine() ?: break
                        if (line.isEmpty()) break

                        val parts = line.split("\\|".toRegex())
                        if (parts.size < 3) break

                        val surahIdx = parts[0].toInt() - 1
                        val verseNum = parts[1].toInt()

                        if (surahIdx == indexSurah &&
                            (indexAya == -1 || indexAya == verseNum)
                        ) {
                            val names = surahNames ?: break
                            handler.post {
                                searchQuranAdapter?.add(
                                    ItemQuranSearch(
                                        parts[2],
                                        names[indexSurah],
                                        verseNum,
                                        surahIdx,
                                        -1, -1
                                    )
                                )
                                updateCount()
                            }
                            if (indexAya != -1) break
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    closeQuranStreams()
                    handler.post {
                        if (searchQuranAdapter?.size == 0) {
                            updateCount()
                        }
                        searchProgressBar?.visibility = View.GONE
                    }
                }
            }
        }

        // Wait for any running thread to finish
        thread?.let {
            try {
                isRun = false
                it.join()
            } catch (_: InterruptedException) {
            }
        }

        isRun = true
        thread = Thread(runnableBySurah).also { it.start() }
    }

    // ── Stream cleanup ───────────────────────────────────────────────

    private fun closeQuranStreams() {
        try {
            bufferedReaderQuran?.close()
            inQuran?.close()
        } catch (_: Exception) {
        }
    }

    private fun stopCurrentSearchThread() {
        thread?.let {
            try {
                isRun = false
                it.join()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                e.printStackTrace()
            }
        }
    }
}
