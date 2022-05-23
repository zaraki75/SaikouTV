package ani.saikou.manga.mangareader

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.R
import ani.saikou.databinding.ItemImageBinding
import ani.saikou.manga.MangaChapter
import ani.saikou.px
import ani.saikou.settings.CurrentReaderSettings.Directions.LEFT_TO_RIGHT
import ani.saikou.settings.CurrentReaderSettings.Directions.RIGHT_TO_LEFT
import ani.saikou.settings.CurrentReaderSettings.Layouts.PAGED
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.File


class ImageAdapter(
    activity: MangaReaderActivity,
    chapter: MangaChapter
) : BaseImageAdapter(activity, chapter) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    inner class ImageViewHolder(binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        applyChangesTo(holder)
    }

    override fun loadImage(position: Int, parent: View): Boolean {
        val imageView = parent.findViewById<SubsamplingScaleImageView>(
            if(settings.layout!=PAGED) R.id.imgProgImageNoGestures else R.id.imgProgImageGestures
        ) ?: return false
        val progress = parent.findViewById<View>(R.id.imgProgProgress) ?: return false
        imageView.recycle()
        imageView.visibility = View.GONE

        if (settings.layout != PAGED) {
            parent.updateLayoutParams {
                if (settings.direction != LEFT_TO_RIGHT && settings.direction != RIGHT_TO_LEFT) {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = 480f.px
                } else {
                    width = 480f.px
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }

        val link = images[position].url
        val trans = images[position].transformation

        if (link.url.isEmpty()) return false
        Glide.with(imageView).download(GlideUrl(link.url) { link.headers })
            .override(Target.SIZE_ORIGINAL)
            .apply {
                val target = object : CustomViewTarget<SubsamplingScaleImageView, File>(imageView) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        progress.visibility = View.GONE
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        imageView.visibility = View.VISIBLE
                        if (settings.layout != PAGED)
                            parent.updateLayoutParams {
                                if (settings.direction != LEFT_TO_RIGHT && settings.direction != RIGHT_TO_LEFT)
                                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                                else
                                    width = ViewGroup.LayoutParams.WRAP_CONTENT

                            }
                        view.setImage(ImageSource.uri(Uri.fromFile(resource)))
                        ObjectAnimator.ofFloat(parent, "alpha", 0f, 1f).setDuration((400 * uiSettings.animationSpeed).toLong())
                            .start()
                        progress.visibility = View.GONE
                    }
                }
                if (trans != null)
                    transform(File("").javaClass, trans).into(target)
                else
                    into(target)
            }
        return true
    }

    override fun getItemCount(): Int = images.size
}
