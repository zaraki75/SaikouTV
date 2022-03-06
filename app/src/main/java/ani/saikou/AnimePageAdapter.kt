package ani.saikou

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.ItemAnimePageBinding
import ani.saikou.media.MediaAdaptor
import ani.saikou.media.SearchActivity

class AnimePageAdapter: RecyclerView.Adapter<AnimePageAdapter.AnimePageViewHolder>() {
    val ready = MutableLiveData(false)
    lateinit var binding:ItemAnimePageBinding
    private var trendHandler: Handler? = null
    private lateinit var trendRun: Runnable
    var trendingViewPager:ViewPager2?=null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimePageViewHolder {
        val binding = ItemAnimePageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnimePageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimePageViewHolder, position: Int) {
        binding = holder.binding
        trendingViewPager = binding.animeTrendingViewPager

        binding.animeTitleContainer.updatePadding(top = statusBarHeight)

        updateAvatar()

        binding.animeSearchBar.hint = "ANIME"
        binding.animeSearchBarText.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, SearchActivity::class.java).putExtra("type", "ANIME"),
                null
            )
        }

        binding.animeSearchBar.setEndIconOnClickListener {
            binding.animeSearchBarText.performClick()
        }

        binding.animeGenreImage.loadImage("https://bit.ly/31bsIHq")
        binding.animeTopScoreImage.loadImage("https://bit.ly/2ZGfcuG")

        binding.animeGenre.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, GenreActivity::class.java).putExtra("type", "ANIME"),
                null
            )
        }
        binding.animeTopScore.setOnClickListener {
            ContextCompat.startActivity(
                it.context,
                Intent(it.context, SearchActivity::class.java).putExtra("type", "ANIME")
                    .putExtra("sortBy", "Score"),
                null
            )
        }
        if(ready.value==false)
            ready.postValue(true)
    }

    override fun getItemCount(): Int = 1

    fun updateHeight(){
        trendingViewPager!!.updateLayoutParams { height += statusBarHeight }
    }

    fun updateTrending(adaptor: MediaAdaptor) {
        binding.animeTrendingProgressBar.visibility = View.GONE
        binding.animeTrendingViewPager.adapter = adaptor
        binding.animeTrendingViewPager.offscreenPageLimit = 3
        binding.animeTrendingViewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        binding.animeTrendingViewPager.setPageTransformer(MediaPageTransformer())
        trendHandler = Handler(Looper.getMainLooper())
        trendRun = Runnable {
            binding.animeTrendingViewPager.currentItem = binding.animeTrendingViewPager.currentItem + 1
        }
        binding.animeTrendingViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    trendHandler!!.removeCallbacks(trendRun)
                    trendHandler!!.postDelayed(trendRun, 4000)
                }
            }
        )
    }

    fun updateRecent(adaptor: MediaAdaptor){
        binding.animeUpdatedProgressBar.visibility = View.GONE
        binding.animeUpdatedRecyclerView.adapter = adaptor
        binding.animeUpdatedRecyclerView.layoutManager = LinearLayoutManager(binding.animeUpdatedRecyclerView.context, LinearLayoutManager.HORIZONTAL, false)
        binding.animeUpdatedRecyclerView.visibility = View.VISIBLE
    }

    fun updateAvatar(){
        if (Anilist.avatar != null) {
            binding.animeUserAvatar.loadImage(Anilist.avatar)
            binding.animeUserAvatar.scaleType = ImageView.ScaleType.FIT_CENTER
        }
    }

    inner class AnimePageViewHolder(val binding: ItemAnimePageBinding) : RecyclerView.ViewHolder(binding.root)
}
