package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object AppSettingsHelper {

    @JvmStatic
    fun openAppSettings(context: Context) {
        val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
        intent.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(intent)
    }
}
