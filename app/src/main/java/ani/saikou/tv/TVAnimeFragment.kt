package ani.saikou.tv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.saikou.R
import ani.saikou.Refresh
import ani.saikou.anilist.AnilistAnimeViewModel
import ani.saikou.anilist.SearchResults
import ani.saikou.media.Media
import ani.saikou.tv.presenters.AnimePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVAnimeFragment: BrowseSupportFragment()  {

    val model: AnilistAnimeViewModel by activityViewModels()
    lateinit var rowAdapter: ArrayObjectAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUIElements()

        val scope = viewLifecycleOwner.lifecycleScope

        /*var height = statusBarHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = activity?.window?.decorView?.rootWindowInsets?.displayCutout
            if (displayCutout != null) {
                if (displayCutout.boundingRects.size > 0) {
                    height = max(
                        statusBarHeight,
                        min(
                            displayCutout.boundingRects[0].width(),
                            displayCutout.boundingRects[0].height()
                        )
                    )
                }
            }
        }
        binding.animeRefresh.setSlingshotDistance(height + 128)
        binding.animeRefresh.setProgressViewEndTarget(false, height + 128)
        binding.animeRefresh.setOnRefreshListener {
            Refresh.activity[this.hashCode()]!!.postValue(true)
        }*/

        //binding.animePageRecyclerView.updatePaddingRelative(bottom = navBarHeight+160f.px)

        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults("ANIME", isAdult = false, onList = false, results = arrayListOf(), hasNextPage = true, sort = "Popular")
        }
        /*val popularAdaptor = MediaAdaptor(1, model.searchResults.results ,requireActivity())
        val progressAdaptor = ProgressAdapter(searched = model.searched)
        val adapter = ConcatAdapter(animePageAdapter,popularAdaptor,progressAdaptor)
        binding.animePageRecyclerView.adapter = adapter
        val layout =  LinearLayoutManager(requireContext())
        binding.animePageRecyclerView.layoutManager = layout*/

        /*var visible=false
        fun animate(){
            val start = if(visible) 0f else 1f
            val end = if(!visible) 0f else 1f
            ObjectAnimator.ofFloat(binding.animePageScrollTop,"scaleX",start,end).apply {
                duration=300
                interpolator = OvershootInterpolator(2f)
                start()
            }
            ObjectAnimator.ofFloat(binding.animePageScrollTop,"scaleY",start,end).apply {
                duration=300
                interpolator = OvershootInterpolator(2f)
                start()
            }
        }*/

        /*binding.animePageScrollTop.setOnClickListener{
            binding.animePageRecyclerView.scrollToPosition(4)
            binding.animePageRecyclerView.smoothScrollToPosition(0)
        }*/


        /*binding.animePageRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
                if (!v.canScrollVertically(1)) {
                    if (model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading) {
                        scope.launch(Dispatchers.IO) {
                            loading=true
                            model.loadNextPage(model.searchResults)
                        }
                    }
                }
                if(layout.findFirstVisibleItemPosition()>1 && !visible){
                    binding.animePageScrollTop.visibility = View.VISIBLE
                    visible = true
                    animate()
                }

                if(!v.canScrollVertically(-1)){
                    visible = false
                    animate()
                    scope.launch{
                        delay(300)
                        binding.animePageScrollTop.visibility = View.GONE
                    }
                }

                super.onScrolled(v, dx, dy)
            }
        })*/
        val presenter = ListRowPresenter()
        presenter.shadowEnabled = false
        rowAdapter = ArrayObjectAdapter(presenter)
        adapter = rowAdapter

        model.getUpdated().observe(viewLifecycleOwner) {
                    if (it != null) {
                        val listRowAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
                        listRowAdapter.addAll(0, it)
                        rowAdapter.add(ListRow(HeaderItem("Recent"), listRowAdapter))

                    }
                }

        model.getTrending().observe(viewLifecycleOwner) {
                if (it != null) {
                    val listRowAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
                    listRowAdapter.addAll(0, it)
                    rowAdapter.add(ListRow(HeaderItem("Trending"), listRowAdapter))
                }
        }
        /*model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {

            }
        }*/

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        model.loaded = true
                        model.loadTrending()
                        model.loadUpdated()
                        //model.loadPopular("ANIME", sort = "Popular")
                    }
                    live.postValue(false)
                }
            }
        }

    }

    private fun setupUIElements() {
        title = getString(R.string.app_name)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // Set fastLane (or headers) background color
        //brandColor =

        // Set search icon color.
        searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.lb_default_search_color)
        /*setHeaderPresenterSelector(object : PresenterSelector() {
            override fun getPresenter(o: Any): Presenter {
                return AnimePresenter(0, requireActivity())
            }
        })*/

        setOnSearchClickedListener {
            val intent = Intent(requireActivity().applicationContext, TVSearchActivity::class.java)
            startActivity(intent)
        }

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->

            val intent = Intent(requireActivity().applicationContext, TVAnimeDetailActivity::class.java)
            intent.putExtra("media", item as Media)
            startActivity(intent)
        }

        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            //Pagination here??
        }
    }


    private inner class GridItemPresenter : Presenter() {
        override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
            val view = TextView(parent.context)
            view.layoutParams = ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.cardview_dark_background))
            view.setTextColor(Color.WHITE)
            view.gravity = Gravity.CENTER
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
            (viewHolder.view as TextView).text = item as String
        }

        override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {}
    }

    companion object {
        private val GRID_ITEM_WIDTH = 200
        private val GRID_ITEM_HEIGHT = 200
    }
}