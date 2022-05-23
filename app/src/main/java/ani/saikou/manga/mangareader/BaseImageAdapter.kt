package ani.saikou.manga.mangareader

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.GesturesListener
import ani.saikou.R
import ani.saikou.manga.MangaChapter
import ani.saikou.px
import ani.saikou.settings.CurrentReaderSettings

abstract class BaseImageAdapter(
    private val activity: MangaReaderActivity,
    chapter: MangaChapter
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val settings = activity.settings.default
    val uiSettings = activity.uiSettings
    val images = chapter.images!!

    @SuppressLint("ClickableViewAccessibility")
    fun applyChangesTo(holder: RecyclerView.ViewHolder){
        val view = holder.itemView
        if (settings.layout != CurrentReaderSettings.Layouts.PAGED) {
            if (settings.padding) {
                when (settings.direction) {
                    CurrentReaderSettings.Directions.TOP_TO_BOTTOM -> view.setPadding(0, 0, 0, 16f.px)
                    CurrentReaderSettings.Directions.LEFT_TO_RIGHT -> view.setPadding(0, 0, 16f.px, 0)
                    CurrentReaderSettings.Directions.BOTTOM_TO_TOP -> view.setPadding(0, 16f.px, 0, 0)
                    CurrentReaderSettings.Directions.RIGHT_TO_LEFT -> view.setPadding(16f.px, 0, 0, 0)
                }
            }
        } else {
            val detector = GestureDetectorCompat(view.context, object : GesturesListener() {
                override fun onSingleClick(event: MotionEvent?) = activity.handleController()
            })
            view.findViewById<View>(R.id.imgProgCover).apply {
                setOnTouchListener { _, event ->
                    detector.onTouchEvent(event)
                    false
                }
                setOnLongClickListener {
                    loadImage(holder.bindingAdapterPosition,view)
                }
            }
        }
        loadImage(holder.bindingAdapterPosition,view)
    }
    
    abstract fun loadImage(position: Int, parent: View) : Boolean
}