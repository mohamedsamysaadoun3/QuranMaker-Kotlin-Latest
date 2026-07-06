package hazem.nurmontage.videoquran.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import hazem.nurmontage.videoquran.core.common.Common
import hazem.nurmontage.videoquran.model.FreeElement
import java.io.File
import java.io.FileOutputStream

/**
 * FreeLayerActivity — Free-form image layer overlay editor.
 *
 * Allows the user to add, drag, and delete decorative image elements
 * as free-form layers on top of the video frame canvas.
 *
 * Flow:
 *   1. Activity opens with a dark canvas background
 *   2. User taps "Add Image" to pick an image from the gallery
 *   3. The image is copied to local storage and added as a draggable ImageView
 *   4. Dragging moves the element; tapping selects it (purple border highlight)
 *   5. "Delete" removes the currently selected element
 *   6. "Done" saves all element positions back and returns result
 *
 * Converted from FreeLayerActivity.java — all logic preserved exactly.
 */
class FreeLayerActivity : AppCompatActivity(), View.OnTouchListener, View.OnClickListener {

    companion object {
        private const val PICK_IMAGE = 1
        private const val TAG = "FreeLayer"
        private const val ID_BTN_ADD = 1001
        private const val ID_BTN_DONE = 1002
        private const val ID_BTN_DELETE = 1003
    }

    private lateinit var container: FrameLayout
    private var selectedView: View? = null
    private val elements = mutableListOf<FreeElement>()
    private val viewMap = HashMap<View, FreeElement>()
    private var isMoving = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // ═══════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(1024) // FLAG_KEEP_SCREEN_ON

        // Build UI programmatically — same as original Java
        container = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1A1A2E"))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#2D2D44"))
            setPadding(8, 8, 8, 8)
        }

        val btnAdd = Button(this).apply {
            text = "+ Add Image"
            setTextColor(-1)
            setBackgroundColor(Color.parseColor("#6C63FF"))
            id = ID_BTN_ADD
            setOnClickListener(this@FreeLayerActivity)
            textSize = 12f
        }
        toolbar.addView(btnAdd, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val btnDelete = Button(this).apply {
            text = "Delete"
            setTextColor(-1)
            setBackgroundColor(Color.parseColor("#FF4444"))
            id = ID_BTN_DELETE
            setOnClickListener(this@FreeLayerActivity)
            textSize = 12f
            visibility = View.GONE
        }
        toolbar.addView(btnDelete, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val btnDone = Button(this).apply {
            text = "Done"
            setTextColor(-1)
            setBackgroundColor(Color.parseColor("#4CAF50"))
            id = ID_BTN_DONE
            setOnClickListener(this@FreeLayerActivity)
            textSize = 12f
        }
        toolbar.addView(btnDone, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val hint = TextView(this).apply {
            text = "Tap + to add elements. Drag to move. Tap to select."
            setTextColor(Color.parseColor("#888888"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(toolbar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(hint, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(container, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
            ))
        }

        setContentView(root)

        // Restore previously saved free elements from Common
        Common.freeElements?.let { savedList ->
            for (element in savedList) {
                elements.add(element)
                val imageView = ImageView(this)
                val bitmap = BitmapFactory.decodeFile(element.imagePath)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                    val params = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = Gravity.CENTER }
                    imageView.layoutParams = params
                    imageView.setOnTouchListener(this@FreeLayerActivity)
                    viewMap[imageView] = element
                    container.addView(imageView)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Add element from URI — handles both file:// and content:// schemes
    // ═══════════════════════════════════════════════════════════════════

    private fun addElementFromUri(uri: Uri?) {
        if (uri == null) return
        try {
            val freeElement = FreeElement()
            freeElement.imagePath = uri.toString()

            val scheme = uri.scheme
            if ("file" == scheme) {
                freeElement.imagePath = uri.path
            } else if ("content" == scheme) {
                val inputStream = contentResolver.openInputStream(uri) ?: return
                val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "free_elements")
                dir.mkdirs()
                val outFile = File(dir, "element_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                    fos.flush()
                }
                inputStream.close()
                freeElement.imagePath = outFile.absolutePath
            }

            elements.add(freeElement)

            val imageView = ImageView(this)
            val width = (container.width * freeElement.width).toInt()
            val height = (container.height * freeElement.height).toInt()
            val decoded = BitmapFactory.decodeFile(freeElement.imagePath, BitmapFactory.Options())
            if (decoded == null || decoded.width <= 0) return

            imageView.setImageBitmap(Bitmap.createScaledBitmap(decoded, width, height, true))
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            imageView.layoutParams = params
            imageView.setOnTouchListener(this)
            viewMap[imageView] = freeElement
            container.addView(imageView)
            selectView(imageView)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error adding element", e)
            Toast.makeText(this, "Error adding image", Toast.LENGTH_SHORT).show()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Delete selected element
    // ═══════════════════════════════════════════════════════════════════

    private fun deleteSelected() {
        val view = selectedView ?: return
        val element = viewMap[view] ?: return
        elements.remove(element)
        (view.parent as? ViewGroup)?.removeView(view)
        selectedView = null
        val btnDelete = findViewById<Button>(ID_BTN_DELETE)
        btnDelete?.visibility = View.GONE
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Save and finish — updates positions from views, stores to Common
    // ═══════════════════════════════════════════════════════════════════

    private fun saveAndFinish() {
        val childCount = container.childCount
        for (i in 0 until childCount) {
            val child = container.getChildAt(i)
            if (viewMap.containsKey(child)) {
                updateElementFromView(child)
            }
        }
        Common.freeElements = elements
        setResult(RESULT_OK)
        finish()
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Select / highlight a view
    // ═══════════════════════════════════════════════════════════════════

    private fun selectView(view: View?) {
        selectedView?.let {
            it.isSelected = false
            it.background = null
        }
        selectedView = view
        if (view != null) {
            view.isSelected = true
            val gradientDrawable = GradientDrawable().apply {
                setColor(Color.parseColor("#FF6C63FF"))
                alpha = 0
                setStroke(3, Color.parseColor("#FF6C63FF"))
            }
            view.background = gradientDrawable
            val btnDelete = findViewById<Button>(ID_BTN_DELETE)
            btnDelete?.visibility = View.VISIBLE
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Update FreeElement position from view coordinates
    // ═══════════════════════════════════════════════════════════════════

    private fun updateElementFromView(view: View) {
        val freeElement = viewMap[view] ?: return
        val containerHeight = container.height
        if (container.width <= 0 || containerHeight <= 0) return
        freeElement.x = view.x / containerHeight
        freeElement.y = view.y / container.height
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Activity result — handles gallery image picker
    // ═══════════════════════════════════════════════════════════════════

    @Deprecated("Use Activity Result API in future refactor")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            data.data?.let { addElementFromUri(it) }
        } else if (resultCode != RESULT_CANCELED && data != null) {
            data.data?.let { addElementFromUri(it) }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  OnClickListener — button dispatch
    // ═══════════════════════════════════════════════════════════════════

    override fun onClick(view: View) {
        when (view.id) {
            ID_BTN_ADD -> {
                // Open gallery image picker
                val intent = Intent().apply {
                    action = Intent.ACTION_PICK
                    data = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    type = "image/*"
                }
                @Suppress("DEPRECATION")
                startActivityForResult(intent, PICK_IMAGE)
            }
            ID_BTN_DONE -> saveAndFinish()
            ID_BTN_DELETE -> deleteSelected()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  OnTouchListener — drag logic for free elements
    // ═══════════════════════════════════════════════════════════════════

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                selectView(view)
                lastTouchX = motionEvent.rawX
                lastTouchY = motionEvent.rawY
                isMoving = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isMoving) return false
                val dx = (motionEvent.rawX - lastTouchX).toInt()
                val dy = (motionEvent.rawY - lastTouchY).toInt()
                view.translationX = view.translationX + dx
                view.translationY = view.translationY + dy
                lastTouchX = motionEvent.rawX
                lastTouchY = motionEvent.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                isMoving = false
                updateElementFromView(view)
                return true
            }
        }
        return false
    }
}
