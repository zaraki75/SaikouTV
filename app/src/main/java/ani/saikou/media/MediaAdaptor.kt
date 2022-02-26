package ani.saikou.media

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.saikou.R
import ani.saikou.databinding.ItemMediaCompactBinding
import ani.saikou.databinding.ItemMediaLargeBinding
import ani.saikou.databinding.ItemMediaPageBinding
import ani.saikou.loadImage
import ani.saikou.setAnimation
import java.io.Serializable


class MediaAdaptor(
    var type: Int,
    private val mediaList: ArrayList<Media>?,
    private val activity: FragmentActivity,
    private val matchParent:Boolean=false,
    private val viewPager: ViewPager2?=null
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (type){
            0-> MediaViewHolder(ItemMediaCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            1-> MediaLargeViewHolder(ItemMediaLargeBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            2-> MediaPageViewHolder(ItemMediaPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException()
        }

    }
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (type){
            0->{
                val b = (holder as MediaViewHolder).binding
                setAnimation(activity,b.root)
                val media = mediaList?.get(position)
                if(media!=null) {
                    loadImage(media.cover, b.itemCompactImage)
                    b.itemCompactOngoing.visibility = if (media.status == "RELEASING") View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text = ((if (media.userScore == 0) (media.meanScore ?: 0) else media.userScore) / 10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(b.root.context, (if (media.userScore != 0) R.drawable.item_user_score else R.drawable.item_score))
                    b.itemCompactUserProgress.text = (media.userProgress ?: "~").toString()
                    if (media.relation != null) {
                        b.itemCompactRelation.text = "${media.relation}  "
                        b.itemCompactType.visibility = View.VISIBLE
                    }
                    if (media.anime != null) {
                        if (media.relation != null) b.itemCompactTypeImage.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_round_movie_filter_24))
                        b.itemCompactTotal.text = " | ${if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes ?: "~").toString()) else (media.anime.totalEpisodes ?: "~").toString()}"
                    }
                    else if (media.manga != null) {
                        if (media.relation != null) b.itemCompactTypeImage.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_round_import_contacts_24))
                        b.itemCompactTotal.text = " | ${media.manga.totalChapters ?: "~"}"
                    }
                }
            }
            1->{
                val b = (holder as MediaLargeViewHolder).binding
                setAnimation(activity,b.root)
                val media = mediaList?.get(position)
                if(media!=null) {
                    loadImage(media.cover,b.itemCompactImage)
                    loadImage(media.banner?:media.cover,b.itemCompactBanner)
                    b.itemCompactOngoing.visibility = if (media.status=="RELEASING")  View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text = ((if(media.userScore==0) (media.meanScore?:0) else media.userScore)/10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(b.root.context,(if (media.userScore!=0) R.drawable.item_user_score else R.drawable.item_score))
                    if (media.anime!=null){
                        b.itemTotal.text = "Episodes"
                        b.itemCompactTotal.text = if (media.anime.nextAiringEpisode!=null) (media.anime.nextAiringEpisode.toString()+" / "+(media.anime.totalEpisodes?:"~").toString()) else (media.anime.totalEpisodes?:"~").toString()
                    }
                    else if(media.manga!=null){
                        b.itemTotal.text = "Chapters"
                        b.itemCompactTotal.text = "${media.manga.totalChapters?:"~"}"
                    }
                    @SuppressLint("NotifyDataSetChanged")
                    if (position == mediaList!!.size-2 && viewPager!=null) viewPager.post {
                        mediaList.addAll(mediaList)
                        notifyDataSetChanged()
                    }
                }
            }
            2->{
                val b = (holder as MediaPageViewHolder).binding
                val media = mediaList?.get(position)
                if(media!=null) {
                    loadImage(media.cover,b.itemCompactImage)
                    loadImage(media.banner?:media.cover,b.itemCompactBanner)
                    b.itemCompactOngoing.visibility = if (media.status=="RELEASING")  View.VISIBLE else View.GONE
                    b.itemCompactTitle.text = media.userPreferredName
                    b.itemCompactScore.text = ((if(media.userScore==0) (media.meanScore?:0) else media.userScore)/10.0).toString()
                    b.itemCompactScoreBG.background = ContextCompat.getDrawable(b.root.context,(if (media.userScore!=0) R.drawable.item_user_score else R.drawable.item_score))
                    if (media.anime!=null){
                        b.itemTotal.text = "Episodes"
                        b.itemCompactTotal.text = if (media.anime.nextAiringEpisode!=null) (media.anime.nextAiringEpisode.toString()+" / "+(media.anime.totalEpisodes?:"~").toString()) else (media.anime.totalEpisodes?:"~").toString()
                    }
                    else if(media.manga!=null){
                        b.itemTotal.text = "Chapters"
                        b.itemCompactTotal.text = "${media.manga.totalChapters?:"~"}"
                    }
                    @SuppressLint("NotifyDataSetChanged")
                    if (position == mediaList!!.size-2 && viewPager!=null) viewPager.post {
                        mediaList.addAll(mediaList)
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun getItemCount() = mediaList!!.size

    override fun getItemViewType(position: Int): Int {
        return type
    }

    inner class MediaViewHolder(val binding: ItemMediaCompactBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            if (matchParent) itemView.updateLayoutParams { width=-1 }
            itemView.setOnClickListener { clicked(bindingAdapterPosition,binding.itemCompactImage) }
            itemView.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    inner class MediaLargeViewHolder(val binding: ItemMediaLargeBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { clicked(bindingAdapterPosition,binding.itemCompactImage) }
            itemView.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    inner class MediaPageViewHolder(val binding: ItemMediaPageBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener { clicked(bindingAdapterPosition,binding.itemCompactImage) }
            itemView.setOnLongClickListener { longClicked(bindingAdapterPosition) }
        }
    }

    fun clicked(position:Int,animate:View){
        val media = mediaList?.get(position)
        ContextCompat.startActivity(
            activity,
            Intent(activity, MediaDetailsActivity::class.java).putExtra("media",media as Serializable),
            ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                Pair.create(animate,ViewCompat.getTransitionName(animate)!!)
            ).toBundle()
        )
    }

    fun longClicked(position:Int):Boolean{
        val media = mediaList?.get(position)?:return false
        MediaListDialogSmallFragment.newInstance(media).show(activity.supportFragmentManager, "list")
        return true
    }
}