package hazem.nurmontage.videoquran.fragment

import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.SpinnerAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.adapter.IconQuranAdabters
import hazem.nurmontage.videoquran.databinding.FragmentAddQuranBinding
import hazem.nurmontage.videoquran.model.RecitersModel
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.MyPreferences
import hazem.nurmontage.videoquran.utils.NetworkUtils
import hazem.nurmontage.videoquran.utils.QuranPreference
import hazem.nurmontage.videoquran.utils.QuranReader
import hazem.nurmontage.videoquran.views.CheckboxCustumFont
import hazem.nurmontage.videoquran.views.TextCustumFont

class AddQuranFragment : Fragment {

    companion object {
        @JvmStatic var instance: AddQuranFragment? = null

        fun getInstance(
            iAddQuran: IAddQuran?,
            resources: Resources?,
            uri: Uri?,
            pathVideoCopy: String?,
            readerName: String?
        ): AddQuranFragment {
            if (instance == null) {
                instance = AddQuranFragment(iAddQuran, resources, uri, pathVideoCopy, readerName)
            }
            return instance!!
        }

        fun getInstance(
            iAddQuran: IAddQuran?,
            resources: Resources?
        ): AddQuranFragment {
            if (instance == null) {
                instance = AddQuranFragment(iAddQuran, resources)
            }
            return instance!!
        }
    }

    interface IAddQuran {
        fun onAdd(str: String, str2: String, str3: String?, str4: String?, i: Int, i2: Int, str5: String, i3: Int, i4: Int)
        fun onAddReaderName(str: String?, str2: String?, uri: Uri?)
        fun onAddTranslation(str: String, i: Int, z: Boolean)
        fun onBismilah()
        fun onCancel()
        fun onDone(str: String, i: Int, str2: String?, uri: Uri?, str3: String?)
        fun onDone(str: String, i: Int, str2: String?, list: List<RecitersModel>?)
        fun onErrorLimitation()
        fun onSearch()
        fun onVuCopyRight()
        fun progress()
        fun uploadRecitation()
    }

    private var adapterFromAyah: ArrayAdapter<String>? = null
    private var adapterToAyah: ArrayAdapter<String>? = null
    private var arrayCount: IntArray? = null
    private var arrayIdentifier: Array<String>? = null
    private var arrayReciters: Array<String>? = null
    private var arraySurah: Array<String>? = null
    private var arrayTranslation: Array<String>? = null
    private var fragmentBinding: FragmentAddQuranBinding? = null
    private var iAddQuran: IAddQuran? = null
    private var iconQuranAdabters: IconQuranAdabters? = null
    private var includeBismilah: CheckboxCustumFont? = null
    private var isFromSearch: Boolean = false
    private var isFromSelectReciters: Boolean = false
    private var iv_done_upload: ImageView? = null
    private var layoutConnection: LinearLayout? = null
    private var path_video_copy: String? = null
    private var quranPreference: QuranPreference? = null
    private var quranReader: QuranReader? = null
    private var reader_name: String? = null
    private var resources: Resources? = null
    private var spinnerFrom: Spinner? = null
    private var spinnerReciters: Spinner? = null
    private var spinnerSurah: Spinner? = null
    private var spinnerTo: Spinner? = null
    private var spinnerTranslation: Spinner? = null
    private var surah_hint: String? = null
    private var tv_reader_name: TextCustumFont? = null
    private var uri_recitation: Uri? = null
    private var icon: String = "hafes"
    private var recitersModels: MutableList<RecitersModel> = mutableListOf()
    private var current_pos: Int = -1
    private val translation_name: Array<String> = arrayOf(
        "en.hilali.txt", "fr.hamidullah.txt", "ur.maududi.txt",
        "tr.ozturk.txt", "de.bubenheim.txt", "id.indonesian.txt",
        "fa.fooladvand.txt", "bn.bengali.txt"
    )
    private var isInit: Boolean = true
    private var isFromSelect: Boolean = true

    private var iconQuranCallback: IconQuranAdabters.IIconQuranCallback? =
        object : IconQuranAdabters.IIconQuranCallback {
            override fun onIcon(str: String) {
                icon = str
            }
        }

    private var onFromAyaSelectedListener: AdapterView.OnItemSelectedListener? =
        object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) {
                if (isFromSearch) {
                    spinnerTo?.setSelection(quranPreference?.getTo() ?: 0)
                    isFromSearch = false
                } else {
                    if (!isFromSelect) {
                        if (spinnerTo?.selectedItemPosition != pos) {
                            spinnerTo?.setSelection(pos)
                        }
                    } else {
                        isFromSelect = false
                    }
                }
            }
        }

    private var onSurahSelectedListener: AdapterView.OnItemSelectedListener? =
        object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) {
                if (pos == current_pos) return

                val ayahCount = if (isInit) {
                    arrayCount?.get(quranPreference?.getSurah() ?: 0) ?: 0
                } else {
                    arrayCount?.get(pos) ?: 0
                }

                val ayahList = mutableListOf<String>()
                for (i in 1..ayahCount) {
                    ayahList.add(i.toString())
                }
                adapterFromAyah?.clear()
                adapterFromAyah?.addAll(ayahList)
                adapterToAyah?.clear()
                adapterToAyah?.addAll(ayahList)

                if (isInit) {
                    try {
                        spinnerSurah?.setSelection(quranPreference?.getSurah() ?: 0, true)
                        spinnerFrom?.setSelection(quranPreference?.getFrom() ?: 0, false)
                        spinnerTo?.setSelection(quranPreference?.getTo() ?: 0, false)
                        spinnerReciters?.setSelection(quranPreference?.getNameReader() ?: 0, false)
                        spinnerTranslation?.setSelection(quranPreference?.getTranslation() ?: 0, false)
                    } catch (_: Exception) {}
                    isInit = false
                } else {
                    spinnerTo?.setSelection(0, false)
                    spinnerFrom?.setSelection(0, false)
                }
                current_pos = spinnerSurah?.selectedItemPosition ?: -1
            }
        }

    constructor()

    constructor(iAddQuran: IAddQuran?, resources: Resources?) {
        this.iAddQuran = iAddQuran
        this.resources = resources
    }

    constructor(
        iAddQuran: IAddQuran?,
        resources: Resources?,
        uri: Uri?,
        pathVideoCopy: String?,
        readerName: String?
    ) {
        this.iAddQuran = iAddQuran
        this.resources = resources
        this.uri_recitation = uri
        this.path_video_copy = pathVideoCopy
        this.reader_name = readerName
    }

    private fun setSystemBarsColorBlack() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        setSystemBarsColorBlack()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val bind = FragmentAddQuranBinding.inflate(inflater, container, false)
        fragmentBinding = bind
        val root: RelativeLayout = bind.root

        if (resources != null && iAddQuran != null) {
            quranPreference = QuranPreference(requireContext())
            quranReader = QuranReader(requireContext())
            surah_hint = if (LocaleHelper.getLanguage(requireContext()) == "ar") "سورة " else "Surah "

            val ivDone = root.findViewById<ImageView>(R.id.iv_done)
            iv_done_upload = ivDone
            if (uri_recitation != null) {
                ivDone.visibility = View.VISIBLE
            }

            root.findViewById<TextCustumFont>(R.id.tv_surah)
                .setText(resources?.getString(R.string.tv_surah))
            root.findViewById<TextCustumFont>(R.id.tv_icon)
                .setText(resources?.getString(R.string.quran_icon))
            root.findViewById<TextCustumFont>(R.id.tv_add_bismilah)
                .setText(resources?.getString(R.string.add_bismilah))
            root.findViewById<TextCustumFont>(R.id.tv_end_ayah)
                .setText(resources?.getString(R.string.to))
            root.findViewById<TextCustumFont>(R.id.tv_hint_reader)
                .setText(resources?.getString(R.string.tv_hint_reader))
            root.findViewById<TextCustumFont>(R.id.tv_translation)
                .setText(resources?.getString(R.string.translation))

            arraySurah = resources?.getStringArray(R.array.surah_names_merged)
            arrayCount = resources?.getIntArray(R.array.surah_count)
            arrayIdentifier = resources?.getStringArray(R.array.identifier)
            arrayReciters = resources?.getStringArray(R.array.reciters)
            arrayTranslation = resources?.getStringArray(R.array.translation_name)

            val checkbox = root.findViewById<CheckboxCustumFont>(R.id.checkbox)
            includeBismilah = checkbox
            checkbox.isChecked = MyPreferences.isIncludeBismilah(requireContext())

            root.findViewById<View>(R.id.add_bismilah).setOnClickListener {
                includeBismilah?.isChecked = !(includeBismilah?.isChecked ?: false)
            }

            spinnerSurah = root.findViewById(R.id.sura_name)
            val surahAdapter = ArrayAdapter(
                requireContext(), R.layout.row_spinner_aya, arraySurah!!
            )
            surahAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSurah?.onItemSelectedListener = onSurahSelectedListener
            spinnerSurah?.adapter = surahAdapter as SpinnerAdapter
            spinnerSurah?.let {
                it.dropDownVerticalOffset = it.height * (-10)
            }

            spinnerFrom = root.findViewById(R.id.aya_from)
            val fromAdapter = ArrayAdapter<String>(
                requireContext(), R.layout.row_spinner_aya
            )
            adapterFromAyah = fromAdapter
            fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerFrom?.onItemSelectedListener = onFromAyaSelectedListener
            spinnerFrom?.adapter = adapterFromAyah as SpinnerAdapter

            spinnerTo = root.findViewById(R.id.aya_to)
            val toAdapter = ArrayAdapter<String>(
                requireContext(), R.layout.row_spinner_aya
            )
            adapterToAyah = toAdapter
            toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTo?.adapter = adapterToAyah as SpinnerAdapter

            spinnerReciters = root.findViewById(R.id.spinner_reciters)
            val recitersAdapter = ArrayAdapter(
                requireContext(), R.layout.row_spinner_aya, arrayReciters!!
            )
            recitersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerReciters?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, pos: Int, id: Long
                ) {
                    if (isFromSelectReciters) {
                        goneReaderNameUpload()
                    }
                    isFromSelectReciters = true
                }
            }
            spinnerReciters?.adapter = recitersAdapter as SpinnerAdapter

            spinnerTranslation = root.findViewById(R.id.spinner_translation)
            val translationAdapter = ArrayAdapter(
                requireContext(), R.layout.row_spinner_aya, arrayTranslation!!
            )
            translationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTranslation?.adapter = translationAdapter as SpinnerAdapter

            layoutConnection = root.findViewById(R.id.hint_no_internet)

            root.findViewById<View>(R.id.btn_done).setOnClickListener {
                if (iAddQuran != null) {
                    val fromPos = spinnerFrom?.selectedItemPosition ?: 0
                    val toPos = spinnerTo?.selectedItemPosition ?: 0
                    val surahPos = spinnerSurah?.selectedItemPosition ?: 0
                    val selectedItemPosition = fromPos + 1
                    val selectedItemPosition2 = toPos + 1
                    val selectedItemPosition3 = surahPos + 1
                    Thread {
                        try {
                            iAddQuran?.progress()
                            if (includeBismilah != null && includeBismilah?.isChecked == true) {
                                iAddQuran?.onBismilah()
                            }
                            addAyaEntityRecursive(
                                selectedItemPosition, selectedItemPosition2, selectedItemPosition3
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            iAddQuran?.onCancel()
                        }
                    }.start()
                }
            }

            root.findViewById<View>(R.id.btn_cancel).setOnClickListener {
                if (iAddQuran != null) {
                    iAddQuran?.onCancel()
                }
            }

            root.findViewById<View>(R.id.btn_search).setOnClickListener {
                savePreference()
                if (iAddQuran != null) {
                    iAddQuran?.onSearch()
                }
            }

            root.findViewById<View>(R.id.btn_upload).setOnClickListener {
                if (iAddQuran != null) {
                    iAddQuran?.uploadRecitation()
                }
                iAddQuran = null
            }

            val tvReader = root.findViewById<TextCustumFont>(R.id.tv_reader)
            tv_reader_name = tvReader
            tvReader.setOnClickListener {
                if (iAddQuran == null || uri_recitation == null) return@setOnClickListener
                iAddQuran?.onAddReaderName(reader_name, path_video_copy, uri_recitation)
            }

            if (reader_name.isNullOrEmpty()) {
                reader_name = "-"
                tv_reader_name?.setTextColor(-1)
            } else {
                tv_reader_name?.paint?.isUnderlineText = true
                tv_reader_name?.text = reader_name
            }

            initIconRv(root)
        }
        return root
    }

    private fun initIconRv(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recyclerView.itemAnimator = null
        recyclerView.setHasFixedSize(true)

        val iconList = arrayListOf("hafes", "shamerli", "nour_hode", "amiri")
        val adapter = IconQuranAdabters(
            iconQuranCallback, iconList, MyPreferences.getLastIconIndex(requireContext())
        )
        iconQuranAdabters = adapter
        icon = iconList[adapter.getSelect()]
        recyclerView.adapter = adapter
    }

    private fun goneReaderNameUpload() {
        uri_recitation = null
        iv_done_upload?.visibility = View.GONE
        tv_reader_name?.text = "-"
        tv_reader_name?.paint?.isUnderlineText = false
        tv_reader_name?.setOnClickListener(null)
    }

    override fun onResume() {
        super.onResume()
        try {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                spinnerReciters?.visibility = View.VISIBLE
                spinnerReciters?.isEnabled = true
                layoutConnection?.visibility = View.GONE
            } else {
                spinnerReciters?.isEnabled = false
                spinnerReciters?.visibility = View.INVISIBLE
                layoutConnection?.visibility = View.VISIBLE
            }
        } catch (_: Exception) {}
    }

    fun addAyaIndex() {
        try {
            isFromSearch = true
            val surah = quranPreference?.getSurah() ?: 0
            current_pos = surah
            spinnerSurah?.setSelection(surah, false)
            val ayahCount = arrayCount?.get(quranPreference?.getSurah() ?: 0) ?: 0
            val ayahList = mutableListOf<String>()
            for (i in 1..ayahCount) {
                ayahList.add(i.toString())
            }
            adapterFromAyah?.clear()
            adapterFromAyah?.addAll(ayahList)
            adapterToAyah?.clear()
            adapterToAyah?.addAll(ayahList)
            spinnerFrom?.setSelection(quranPreference?.getFrom() ?: 0, false)
            spinnerReciters?.setSelection(quranPreference?.getNameReader() ?: 0, false)
        } catch (_: Exception) {}
    }

    fun setNameReader(str: String?, uri: Uri?, str2: String?) {
        uri_recitation = uri
        path_video_copy = str2
        if (uri != null) {
            iv_done_upload?.visibility = View.VISIBLE
        }
        var name = str
        if (name.isNullOrEmpty()) {
            tv_reader_name?.paint?.isUnderlineText = false
            name = "-"
        } else {
            tv_reader_name?.paint?.isUnderlineText = true
        }
        reader_name = name
        tv_reader_name?.text = name
    }

    fun splitAya(str: String, str2: String?, ayaNum: Int) {
        val trim = str.trim()
        val split = trim.replace("\\s*([\u06D6-\u06ED])".toRegex(), "$1")
            .trim().split("\\s+".toRegex()).toTypedArray()
        val split2 = str2?.split(",")?.toTypedArray()

        val space = " "
        val nasMark = " نص"

        if (split.size <= 4) {
            iAddQuran?.onAdd(
                str + nasMark, trim,
                str2?.replace(",", " "),
                str2, str.length, ayaNum, icon, 0, split.size
            )
            return
        }

        val sb = StringBuilder()
        var longWordThreshold = 1
        var lastIndex = split.size - 1
        var longWordCount = 0
        var totalWordCount = 0
        var loopIndex = 0
        var chunkStart = 0

        while (loopIndex < split.size) {
            val word = split[loopIndex]
            sb.append(word).append(space)
            if (word.length > longWordThreshold) {
                longWordCount++
            }
            val newTotalCount = totalWordCount + 1

            if (longWordCount == 5) {
                val endIndex = (chunkStart + longWordCount) - (newTotalCount - longWordCount)

                if (loopIndex == lastIndex) {
                    val chunkText = sb.toString().trim()
                    iAddQuran?.onAdd(
                        chunkText + nasMark, trim,
                        if (split2 != null) getWords(split2, chunkStart, endIndex) else null,
                        str2, chunkText.length, ayaNum, icon, chunkStart, endIndex
                    )
                } else {
                    val chunkText = sb.toString().trim()
                    val transWords = if (split2 != null)
                        getWords(split2, chunkStart, endIndex) else null
                    iAddQuran?.onAdd(
                        chunkText, trim, transWords, str2,
                        -1, -1, icon, chunkStart, chunkStart + newTotalCount
                    )
                }

                chunkStart += newTotalCount
                sb.setLength(0)
                longWordCount = 0
                totalWordCount = 0
            } else {
                totalWordCount = newTotalCount
            }
            loopIndex++
        }

        if (sb.isNotEmpty()) {
            val chunkText = sb.toString().trim()
            val transWords = if (split2 != null)
                getWords(split2, split2.size - totalWordCount, split2.size) else null
            iAddQuran?.onAdd(
                chunkText + nasMark, trim, transWords, str2,
                chunkText.length, ayaNum, icon, chunkStart, chunkStart + totalWordCount
            )
        }
    }

    fun getWords(strArr: Array<String>?, start: Int, end: Int): String {
        if (strArr == null || strArr.isEmpty()) return ""
        var s = start
        var e = end
        if (s < 0) s = 0
        if (e > strArr.size) e = strArr.size
        if (s >= e) return ""
        return strArr.copyOfRange(s, e).joinToString(" ")
    }

    fun addAyaEntityRecursive(from: Int, to: Int, surahNumber: Int) {
        try {
            val ayahText = quranReader?.getAyahText(surahNumber, from) ?: run {
                iAddQuran?.onCancel()
                return
            }
            val translationPos = spinnerTranslation?.selectedItemPosition ?: 0
            val translationAyahText = if (translationPos > 0) {
                quranReader?.getTranslationAyahText(
                    translation_name[translationPos - 1], surahNumber, from
                )
            } else null
            splitAya(ayahText, null, from)
            if (translationAyahText != null) {
                iAddQuran?.onAddTranslation(translationAyahText, from, translationPos == 1)
            }
            if (iAddQuran != null) {
                if (spinnerReciters?.isEnabled == true) {
                    val identifier = arrayIdentifier?.get(spinnerReciters?.selectedItemPosition ?: 0) ?: run {
                        iAddQuran?.onCancel()
                        return
                    }
                    recitersModels.add(RecitersModel(identifier, surahNumber, from))
                }
                if (from >= to) {
                    val surahName = arraySurah?.get(spinnerSurah?.selectedItemPosition ?: 0) ?: ""
                    val surahIndex = (spinnerSurah?.selectedItemPosition ?: 0) + 1
                    if (uri_recitation != null) {
                        iAddQuran?.onDone(
                            surah_hint + surahName, surahIndex,
                            reader_name, uri_recitation, path_video_copy
                        )
                    } else {
                        val reciterName = arrayReciters?.get(spinnerReciters?.selectedItemPosition ?: 0) ?: ""
                        iAddQuran?.onDone(
                            surah_hint + surahName, surahIndex,
                            reciterName, recitersModels
                        )
                    }
                    return
                }
            }
            addAyaEntityRecursive(from + 1, to, surahNumber)
        } catch (e: Exception) {
            e.printStackTrace()
            iAddQuran?.onCancel()
        }
    }

    private fun savePreference() {
        quranPreference?.savePreferences(
            spinnerSurah?.selectedItemPosition ?: 0,
            spinnerFrom?.selectedItemPosition ?: 0,
            spinnerTo?.selectedItemPosition ?: 0,
            spinnerReciters?.selectedItemPosition ?: 0,
            spinnerTranslation?.selectedItemPosition ?: 0
        )
        try {
            MyPreferences.putIndexLastIcon(requireContext(), iconQuranAdabters?.getSelect() ?: 0)
        } catch (_: Exception) {}
        try {
            MyPreferences.putIncludeBismilah(requireContext(), includeBismilah?.isChecked ?: false)
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        savePreference()
        QuranPreference.saveLastSearch(requireContext(), null)
        iAddQuran?.onCancel()
        onFromAyaSelectedListener = null
        onSurahSelectedListener = null
        fragmentBinding = null
        instance = null
        iconQuranCallback = null
    }
}
