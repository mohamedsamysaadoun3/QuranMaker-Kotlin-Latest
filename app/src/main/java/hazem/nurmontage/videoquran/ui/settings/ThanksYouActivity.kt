package hazem.nurmontage.videoquran.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import hazem.nurmontage.videoquran.R
import hazem.nurmontage.videoquran.core.base.BaseActivity
import hazem.nurmontage.videoquran.databinding.ActivityThanksYouBinding
import hazem.nurmontage.videoquran.utils.LocaleHelper
import hazem.nurmontage.videoquran.utils.MyVibrationHelper
import nl.dionsegijn.konfetti.core.PartyFactory
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Shape
import java.util.Arrays
import java.util.concurrent.TimeUnit

class ThanksYouActivity : BaseActivity() {

    private lateinit var binding: ActivityThanksYouBinding

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityThanksYouBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, windowInsets ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        setStatusBarColor(-1)

        val mResources = resources
        if (intent != null) {
            binding.tvPriceDonate.text = String.format(
                mResources.getString(R.string.donate_hint),
                intent.getStringExtra("price")
            )
            binding.tvThnksDonate.text = mResources.getString(R.string.thanks_hint)
        }

        explode()

        binding.btnOnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }
    }

    private fun explode() {
        val favoriteDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.favorite_24px)
        val shapes = mutableListOf<Shape>()
        shapes.add(Shape.Square)
        shapes.add(Shape.Circle)
        if (favoriteDrawable != null) {
            shapes.add(Shape.DrawableShape(favoriteDrawable, true, true))
        }

        binding.konfettiView.start(
            PartyFactory(
                Emitter(2800L, TimeUnit.MILLISECONDS).max(512)
            )
                .spread(Spread.ROUND)
                .shapes(shapes)
                .colors(Arrays.asList(16572810, 16740973, 16003181, 11832815))
                .setSpeedBetween(0.0f, 30.0f)
                .position(Position.Relative(0.5, 0.3))
                .build()
        )
    }

    override fun onResume() {
        super.onResume()
        playVibration()
    }

    private fun playVibration() {
        MyVibrationHelper(this).vibrate(250L)
    }
}
