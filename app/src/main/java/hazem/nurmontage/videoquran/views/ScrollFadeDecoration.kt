package hazem.nurmontage.videoquran.views

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScrollFadeDecoration : RecyclerView.ItemDecoration() {

    private val paint = Paint()
    private val fadeWidth = 50
    private val fadeColor = -2013265920

    override fun onDrawOver(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition()
        val lastVisible = layoutManager.findLastCompletelyVisibleItemPosition()
        val itemCount = layoutManager.itemCount
        val width = recyclerView.width
        val height = recyclerView.height

        if (firstVisible > 0) {
            paint.shader = LinearGradient(0f, 0f, fadeWidth.toFloat(), 0f, fadeColor, 0, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, fadeWidth.toFloat(), height.toFloat(), paint)
        }

        if (lastVisible < itemCount - 1) {
            val left = width - fadeWidth.toFloat()
            val right = width.toFloat()
            paint.shader = LinearGradient(left, 0f, right, 0f, 0, fadeColor, Shader.TileMode.CLAMP)
            canvas.drawRect(left, 0f, right, height.toFloat(), paint)
        }

        paint.shader = null
    }
}
