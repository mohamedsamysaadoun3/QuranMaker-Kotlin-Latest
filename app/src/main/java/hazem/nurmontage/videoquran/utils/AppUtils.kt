package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object AppUtils {

    private const val TAG = "AppUtils"

    @JvmStatic
    fun getAppVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.6"
        } catch (e: Exception) {
            "1.6"
        }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    fun getAppVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting app version code", e)
            -1
        }
    }
}
