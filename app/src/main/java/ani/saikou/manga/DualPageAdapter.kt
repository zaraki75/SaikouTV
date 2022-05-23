package ani.saikou.manga

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
import ani.saikou.databinding.ItemDualPageBinding
import ani.saikou.manga.mangareader.BaseImageAdapter
import ani.saikou.manga.mangareader.MangaReaderActivity
import ani.saikou.px
import ani.saikou.settings.CurrentReaderSettings
import ani.saikou.settings.CurrentReaderSettings.Layouts.PAGED
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.File

class DualPageAdapter(
    activity: MangaReaderActivity,
    chapter: MangaChapter
) : BaseImageAdapter(activity,chapter) {

    private val pages = chapter.dualPages()
    override fun loadImage(position: Int, parent: View): Boolean {
        val imageView1 = parent.findViewById<SubsamplingScaleImageView>(
            if(settings.layout!=PAGED) R.id.imgProgImageNoGestures1 else R.id.imgProgImageGestures1
        ) ?: return false
        val progress1 = parent.findViewById<View>(R.id.imgProgProgress1) ?: return false

        val imageView2 = parent.findViewById<SubsamplingScaleImageView>(
            if(settings.layout!=PAGED) R.id.imgProgImageNoGestures2 else R.id.imgProgImageGestures2
        ) ?: return false
        val progress2 = parent.findViewById<View>(R.id.imgProgProgress2) ?: return false

        fun apply(position: Int,imageView: SubsamplingScaleImageView,progress:View):Boolean {
            imageView.recycle()
            imageView.visibility = View.GONE

            if (settings.layout != PAGED) {
                parent.updateLayoutParams {
                    if (settings.direction != CurrentReaderSettings.Directions.LEFT_TO_RIGHT && settings.direction != CurrentReaderSettings.Directions.RIGHT_TO_LEFT) {
                        width = ViewGroup.LayoutParams.MATCH_PARENT
                        height = 480f.px
                    } else {
                        width = 480f.px
                        height = ViewGroup.LayoutParams.MATCH_PARENT
                    }
                }
            }

            val link = images.getOrNull(position)?.url ?: return true
            val trans = images[position].transformation

            if (link.url.isEmpty()) return true
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
                                    if (settings.direction != CurrentReaderSettings.Directions.LEFT_TO_RIGHT && settings.direction != CurrentReaderSettings.Directions.RIGHT_TO_LEFT)
                                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                                    else
                                        width = ViewGroup.LayoutParams.WRAP_CONTENT

                                }
                            view.setImage(ImageSource.uri(Uri.fromFile(resource)))
                            ObjectAnimator.ofFloat(parent, "alpha", 0f, 1f)
                                .setDuration((400 * uiSettings.animationSpeed).toLong())
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

        return apply(position*2,imageView1,progress1) && apply(position*2+1,imageView2,progress2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DualImageViewHolder {
        val binding = ItemDualPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DualImageViewHolder(binding)
    }

    inner class DualImageViewHolder(binding: ItemDualPageBinding) : RecyclerView.ViewHolder(binding.root)

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        applyChangesTo(holder)
    }

    override fun getItemCount(): Int = pages.size
}