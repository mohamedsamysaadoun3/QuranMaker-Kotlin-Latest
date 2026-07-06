package hazem.nurmontage.videoquran.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import hazem.nurmontage.videoquran.model.Template

object LocalPersistence {

    private const val PREFS_NAME = "MTemplate"

    fun readObjectFromFile(context: Context, key: String): Any? {
        return try {
            val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(key, "") ?: ""
            GsonBuilder().create().fromJson(json, Template::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writeTemplate(context: Context, obj: Any?, oldKey: String, newKey: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(obj)
            prefs.edit()
                .remove(oldKey)
                .putString(newKey, json)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun duplicateTemplate(context: Context, obj: Any?, key: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = Gson().toJson(obj)
            prefs.edit().putString(key, json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteTemplate(context: Context, key: String) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(key)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
