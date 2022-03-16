package ani.saikou

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LayoutAnimationController
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistHomeViewModel
import ani.saikou.databinding.FragmentHomeBinding
import ani.saikou.media.Media
import ani.saikou.media.MediaAdaptor
import ani.saikou.settings.SettingsDialogFragment
import ani.saikou.user.ListActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    val model: AnilistHomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val scope = lifecycleScope
        fun load(){
            if(activity!=null && _binding!=null) lifecycleScope.launch(Dispatchers.Main) {
                binding.homeUserName.text = Anilist.username
                binding.homeUserEpisodesWatched.text = Anilist.episodesWatched.toString()
                binding.homeUserChaptersRead.text = Anilist.chapterRead.toString()
                binding.homeUserAvatar.loadImage(Anilist.avatar)
                binding.homeUserBg.loadImage(Anilist.bg)
                binding.homeUserAvatar.scaleType = ImageView.ScaleType.FIT_CENTER
                binding.homeUserDataProgressBar.visibility = View.GONE

                binding.homeUserAvatarContainer.setSafeOnClickListener {
                    SettingsDialogFragment().show(parentFragmentManager, "dialog")
                }

                binding.homeAnimeList.setOnClickListener {
                    ContextCompat.startActivity(
                        requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                            .putExtra("anime", true)
                            .putExtra("userId", Anilist.userid)
                            .putExtra("username", Anilist.username), null
                    )
                }
                binding.homeMangaList.setOnClickListener {
                    ContextCompat.startActivity(
                        requireActivity(), Intent(requireActivity(), ListActivity::class.java)
                            .putExtra("anime", false)
                            .putExtra("userId", Anilist.userid)
                            .putExtra("username", Anilist.username), null
                    )
                }

                binding.homeUserAvatarContainer.startAnimation(setSlideUp)
                binding.homeUserDataContainer.visibility = View.VISIBLE
                binding.homeUserDataContainer.layoutAnimation = LayoutAnimationController(setSlideUp, 0.25f)
                binding.homeAnimeList.visibility = View.VISIBLE
                binding.homeMangaList.visibility = View.VISIBLE
                binding.homeListContainer.layoutAnimation = LayoutAnimationController(setSlideIn,0.25f)
            }
            else{
                toastString("Please Reload.")
            }
        }


        binding.homeContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBarHeight
        }
        binding.homeUserBg.updateLayoutParams { height += statusBarHeight }

        var reached = false
        binding.homeScroll.setOnScrollChangeListener { _, _, _, _, _ ->
            if (!binding.homeScroll.canScrollVertically(1)) {
                reached = true
                bottomBar.animate().translationZ(0f).setDuration(200).start()
                ObjectAnimator.ofFloat(bottomBar, "elevation", 4f, 0f).setDuration(200).start()
            }
            else{
                if (reached){
                    bottomBar.animate().translationZ(12f).setDuration(200).start()
                    ObjectAnimator.ofFloat(bottomBar, "elevation", 0f, 4f).setDuration(200).start()
                }
            }
        }
        var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size>0) {
                    height = max(statusBarHeight,min(displayCutout.boundingRects[0].width(),displayCutout.boundingRects[0].height()))
                }
            }
        }
        binding.homeRefresh.setSlingshotDistance(height+128)
        binding.homeRefresh.setProgressViewEndTarget(false, height+128)
        binding.homeRefresh.setOnRefreshListener {
            Refresh.activity[1]!!.postValue(true)
        }

        //UserData
        binding.homeUserDataProgressBar.visibility = View.VISIBLE
        binding.homeUserDataContainer.visibility = View.GONE
        if(model.loaded){
            load()
        }
        //List Images
        model.getListImages().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                binding.homeAnimeListImage.loadImage(it[0] ?: "https://bit.ly/31bsIHq")
                binding.homeMangaListImage.loadImage(it[1] ?: "https://bit.ly/2ZGfcuG")
            }
        }

        //Function For Recycler Views
        fun initRecyclerView(mode: LiveData<ArrayList<Media>>, recyclerView: RecyclerView, progress: View, empty: View, title:View) {
            progress.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            empty.visibility = View.GONE
            title.visibility = View.INVISIBLE

            mode.observe(viewLifecycleOwner) {
                recyclerView.visibility = View.GONE
                empty.visibility = View.GONE
                if (it != null) {
                    if (it.isNotEmpty()) {
                        recyclerView.adapter = MediaAdaptor(0,it, requireActivity())
                        recyclerView.layoutManager = LinearLayoutManager(
                            requireContext(),
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.layoutAnimation = LayoutAnimationController(setSlideIn, 0.25f)

                    } else {
                        empty.visibility = View.VISIBLE
                    }
                    title.visibility = View.VISIBLE
                    title.startAnimation(setSlideUp)
                    progress.visibility = View.GONE
                }
            }

        }

        // Recycler Views
        initRecyclerView(
            model.getAnimeContinue(),
            binding.homeWatchingRecyclerView,
            binding.homeWatchingProgressBar,
            binding.homeWatchingEmpty,
            binding.homeContinueWatch
        )
        binding.homeWatchingBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(0)
        }

        initRecyclerView(
            model.getMangaContinue(),
            binding.homeReadingRecyclerView,
            binding.homeReadingProgressBar,
            binding.homeReadingEmpty,
            binding.homeContinueRead
        )
        binding.homeReadingBrowseButton.setOnClickListener {
            bottomBar.selectTabAt(2)
        }

        initRecyclerView(
            model.getRecommendation(),
            binding.homeRecommendedRecyclerView,
            binding.homeRecommendedProgressBar,
            binding.homeRecommendedEmpty,
            binding.homeRecommended
        )

        binding.homeUserAvatarContainer.startAnimation(setSlideUp)

        val live = Refresh.activity.getOrPut(1) { MutableLiveData(false) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        //Get userData First
                        if (Anilist.userid == null)
                            if (Anilist.query.getUserData()) load() else logger("Error loading data")
                        else load()
                        model.loaded = true
                        model.setListImages()
                        model.setAnimeContinue()
                        model.setMangaContinue()
                        model.setRecommendation()
                    }
                    live.postValue(false)
                    _binding?.homeRefresh?.isRefreshing = false
                }
            }
        }
    }

    override fun onResume() {
        if(!model.loaded) Refresh.activity[1]!!.postValue(true)
        super.onResume()
    }
}