package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.content.SharedPreferences

class QuranPreference(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "QuranPrefs_"
        private const val KEY_FROM = "from"
        private const val KEY_TO = "to"
        private const val KEY_SURAH = "surah"
        private const val KEY_SEARCH = "search"
        private const val KEY_NAME_READER = "name_reader_"
        private const val KEY_TRANSLATION = "translation_select"

        fun savePreferencesSearch(
            context: Context,
            surah: Int,
            from: Int,
            to: Int,
            searchQuery: String
        ) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_FROM, from)
                .putInt(KEY_TO, to)
                .putInt(KEY_SURAH, surah)
                .putString(KEY_SEARCH, searchQuery)
                .apply()
        }

        fun savePreferencesSearch(context: Context, surah: Int, ayah: Int) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_FROM, ayah)
                .putInt(KEY_TO, ayah)
                .putInt(KEY_SURAH, surah)
                .apply()
        }

        fun saveLastSearch(context: Context, query: String?) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SEARCH, query)
                .apply()
        }

        fun getLastSearch(context: Context): String {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SEARCH, "") ?: ""
        }
    }

    fun savePreferences(surah: Int, from: Int, to: Int, nameReader: Int, translation: Int) {
        sharedPreferences.edit()
            .putInt(KEY_FROM, from)
            .putInt(KEY_TO, to)
            .putInt(KEY_SURAH, surah)
            .putInt(KEY_NAME_READER, nameReader)
            .putInt(KEY_TRANSLATION, translation)
            .apply()
    }

    fun getSurah(): Int = sharedPreferences.getInt(KEY_SURAH, 0)

    fun getTranslation(): Int = sharedPreferences.getInt(KEY_TRANSLATION, 0)

    fun getFrom(): Int = sharedPreferences.getInt(KEY_FROM, 0)

    fun getTo(): Int = sharedPreferences.getInt(KEY_TO, 0)

    fun getNameReader(): Int {
        return try {
            sharedPreferences.getInt(KEY_NAME_READER, 0)
        } catch (_: Exception) {
            0
        }
    }
}
