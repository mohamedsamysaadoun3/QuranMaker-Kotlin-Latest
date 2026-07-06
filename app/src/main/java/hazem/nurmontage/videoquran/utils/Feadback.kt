package hazem.nurmontage.videoquran.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import hazem.nurmontage.videoquran.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Feadback {

    @JvmStatic
    fun reportBug(context: Context, cmd: String, subject: String) {
        var firstInstallTime: Long
        try {
            firstInstallTime = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            firstInstallTime = 0
        }

        val formattedDate = SimpleDateFormat("yyyy_MM_dd_HH:mm:ss", Locale.US).format(Date(firstInstallTime))
        val timeDifference = getTimeDifference(Date(firstInstallTime))

        var versionName: String
        try {
            versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            versionName = ""
        }

        var body = subject + "\n\n\n\"cmd = " + cmd +
                "\n\nFirst Install Time : " + formattedDate +
                "\nTime ago :" + timeDifference +
                "\nApp Name: " + context.getString(R.string.app_name) +
                "\nApp Version: " + versionName +
                "\nDevice Platform: Android(" + Build.MODEL + ")" +
                "\nDevice OS: " + Build.VERSION.RELEASE

        val emailAddresses = arrayOf("nurmontage.contact@gmail.com")

        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_EMAIL, emailAddresses)
            putExtra(Intent.EXTRA_BCC, emailAddresses)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            type = "message/rfc822"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        if (isGmailAvailable(context)) {
            val gmailIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_EMAIL, emailAddresses)
                putExtra(Intent.EXTRA_BCC, emailAddresses)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                type = "message/rfc822"
                setPackage("com.google.android.gm")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(gmailIntent)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Send email using"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isGmailAvailable(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            setPackage("com.google.android.gm")
        }
        return context.packageManager.queryIntentActivities(intent, 0).size > 0
    }

    @JvmStatic
    fun getSecondsDifference(date: Date): Int {
        return ((Date(System.currentTimeMillis()).time - date.time) / 1000).toInt()
    }

    @JvmStatic
    fun getReadableTime(seconds: Int): String {
        val hours = seconds / 3600
        val remainder = seconds - hours * 3600
        val minutes = remainder / 60
        val secs = remainder - minutes * 60

        var result = if (hours > 0) "$hours hour " else ""
        if (minutes > 0) {
            result += "$minutes min "
        }
        return if (secs > 0) result + "$secs sec" else result
    }

    @JvmStatic
    fun getTimeDifference(date: Date): String {
        return getReadableTime(getSecondsDifference(date))
    }
}
