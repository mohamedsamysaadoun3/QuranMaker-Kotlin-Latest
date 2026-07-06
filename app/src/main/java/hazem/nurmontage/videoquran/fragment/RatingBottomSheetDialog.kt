package hazem.nurmontage.videoquran.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.views.TextCustumFont
import hazem.nurmontage.videoquran.views.ButtonCustumFont

class RatingBottomSheetDialog : BottomSheetDialogFragment {

    companion object {
        private const val KEY_NEVER_ASK_AGAIN = "never_ask_again_new"
        private const val PREFS_NAME = "app_prefs_new_mars"

        fun setNeverAskAgain(context: Context, neverAsk: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NEVER_ASK_AGAIN, neverAsk)
                .apply()
        }

        fun shouldShowRatingDialog(context: Context): Boolean {
            return !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NEVER_ASK_AGAIN, false)
        }
    }

    private var res: android.content.res.Resources? = null

    constructor()

    constructor(resources: android.content.res.Resources) : this() {
        this.res = resources
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.rating_bottom_sheet, container, false)

        val resources = res ?: return view

        val rateButton: ButtonCustumFont = view.findViewById(R.id.rateButton)
        val laterButton: ButtonCustumFont = view.findViewById(R.id.laterButton)
        val neverButton: ButtonCustumFont = view.findViewById(R.id.neverButton)
        val tvTitle: TextCustumFont = view.findViewById(R.id.tv_tittle)
        val tvSubtitle: TextCustumFont = view.findViewById(R.id.tv_subtittle)

        rateButton.text = resources.getString(R.string.rate_now)
        laterButton.text = resources.getString(R.string.later)
        neverButton.text = resources.getString(R.string.no_thanks)

        tvTitle.text = resources.getString(R.string.enjoying_the_app)
        tvSubtitle.text = resources.getString(R.string.moment_to_rate)

        rateButton.setOnClickListener {
            context?.let { ctx ->
                openPlayStore(ctx)
                setNeverAskAgain(ctx, true)
            }
            dismiss()
        }

        laterButton.setOnClickListener {
            dismiss()
        }

        neverButton.setOnClickListener {
            context?.let { ctx -> setNeverAskAgain(ctx, true) }
            dismiss()
        }

        return view
    }

    private fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }
}
