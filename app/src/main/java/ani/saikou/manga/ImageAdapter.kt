package ani.saikou.manga

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemImageBinding
import ani.saikou.setAnimation
import ani.saikou.toastString
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.File

class ImageAdapter(
private val arr: ArrayList<String>,
private val headers:MutableMap<String,String>?=null
): RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val binding = holder.binding
        setAnimation(binding.root.context,binding.root)
        Glide.with(binding.imgProgImage)
            .download(GlideUrl(arr[position]){headers?: mutableMapOf()})
            .listener(object : RequestListener<File> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean {
                    toastString(e.toString())
                    return false
                }

                override fun onResourceReady(resource: File?, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    binding.imgProgProgress.visibility= View.GONE
                    return false
                }
            })
            .into(SubsamplingScaleImageViewTarget(binding.imgProgImage))
    }

    override fun getItemCount(): Int = arr.size

    inner class ImageViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    class SubsamplingScaleImageViewTarget(view: SubsamplingScaleImageView) : CustomViewTarget<SubsamplingScaleImageView, File>(view) {
        override fun onLoadFailed(errorDrawable: Drawable?) {}
        override fun onResourceCleared(placeholder: Drawable?) {}

        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
            view.setImage(ImageSource.uri(Uri.fromFile(resource)))
        }
    }
}
