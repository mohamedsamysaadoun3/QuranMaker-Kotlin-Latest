package hazem.nurmontage.videoquran.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArrowOverlayDecoration(
    context: Context,
    drawableRes: Int,
    sizeDp: Int
) : RecyclerView.ItemDecoration() {

    private val arrowDrawable: Drawable? = AppCompatResources.getDrawable(context, drawableRes)
    private val arrowSize: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        sizeDp.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    override fun onDrawOver(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = layoutManager.itemCount
        val halfHeight = recyclerView.height / 2

        if (lastVisible < itemCount - 1) {
            val size = arrowSize
            val top = halfHeight - (size / 2)
            arrowDrawable?.setBounds(0, top, size, top + size)
            arrowDrawable?.isAutoMirrored = false
            arrowDrawable?.draw(canvas)
        }

        if (firstVisible > 0) {
            val width = recyclerView.width
            val top = halfHeight - (arrowSize / 2)
            canvas.save()
            canvas.scale(-1.0f, 1.0f, width - (arrowSize / 2.0f), 0.0f)
            val size = arrowSize
            arrowDrawable?.setBounds(width - size, top, width, size + top)
            arrowDrawable?.draw(canvas)
            canvas.restore()
        }
    }
}
