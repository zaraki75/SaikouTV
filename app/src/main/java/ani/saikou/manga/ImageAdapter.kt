package ani.saikou.manga

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemImageBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.io.File

class ImageAdapter(
private val arr: ArrayList<String>,
private val headers:MutableMap<String,String>?=null,
private val transformation: Transformation<File>?=null
): RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val binding = holder.binding
        binding.imgProgImage.recycle()
        binding.imgProgProgress.visibility= View.VISIBLE
        Glide.with(binding.imgProgImage)
            .download(GlideUrl(arr[position]){headers?: mutableMapOf()}).override(Target.SIZE_ORIGINAL).apply{
                val target = object : CustomViewTarget<SubsamplingScaleImageView, File>(binding.imgProgImage) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {}
                    override fun onResourceCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                        view.setImage(ImageSource.uri(Uri.fromFile(resource)))
                        binding.imgProgProgress.visibility= View.GONE
                    }
                }
                if(transformation!=null)
                    transform(File("").javaClass, transformation).into(target)
                else
                    into(target)
            }

    }

    override fun getItemCount(): Int = arr.size

    inner class ImageViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)
}
