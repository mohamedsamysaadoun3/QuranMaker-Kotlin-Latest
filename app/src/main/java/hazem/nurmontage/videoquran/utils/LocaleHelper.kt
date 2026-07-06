package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    private const val PREFS_NAME = "ActPreference"
    private const val USER_IS_CHOICE = "userIsChoice"

    fun onAttach(context: Context): Context {
        return setLocale(context, getPersistedData(context, getLanguage(context)))
    }

    fun getLanguage(context: Context): String {
        return getPersistedData(context, "en")
    }

    fun setLocale(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun setLocale(context: Context, language: String): Context {
        persist(context, language)
        return updateResources(context, language)
    }

    fun getPersistedData(context: Context, default: String): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SELECTED_LANGUAGE, default) ?: default
    }

    fun persist(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_LANGUAGE, language)
            .apply()
    }

    fun userIsChoice(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(USER_IS_CHOICE, true)
            .apply()
    }

    fun getUserIsChoice(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(USER_IS_CHOICE, false)
    }

    fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun updateResourcesLegacy(context: Context, language: String): Context {
        val locale = Locale(language)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        val newContext = context.createConfigurationContext(config)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
        return newContext
    }
}
