package ani.saikou.tv

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import ani.saikou.*
import ani.saikou.R
import ani.saikou.anilist.*
import ani.saikou.media.Media
import ani.saikou.tv.components.ButtonListRow
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.login.TVLoginFragment
import ani.saikou.tv.presenters.AnimePresenter
import ani.saikou.tv.presenters.ButtonListRowPresenter
import ani.saikou.tv.presenters.GenresPresenter
import ani.saikou.tv.presenters.MainHeaderPresenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVAnimeFragment: BrowseSupportFragment()  {

    companion object {
        var shouldReload: Boolean = false
    }
    private val PAGING_THRESHOLD = 15

    val homeModel: AnilistHomeViewModel by activityViewModels()
    val model: AnilistAnimeViewModel by activityViewModels()
    val genresModel: GenresViewModel by activityViewModels()

    //TODO Sketchy handling here
    var nCallbacks: Int = 0

    lateinit var continueAdapter: ArrayObjectAdapter
    lateinit var recommendedAdapter: ArrayObjectAdapter
    lateinit var genresAdapter: ArrayObjectAdapter
    lateinit var trendingAdapter: ArrayObjectAdapter
    lateinit var updatedAdapter: ArrayObjectAdapter
    lateinit var popularAdapter: ArrayObjectAdapter
    lateinit var rowAdapter: ArrayObjectAdapter

    lateinit var continueRow: ListRow
    lateinit var recommendedRow: ListRow
    lateinit var genresRow: ListRow
    lateinit var trendingRow: ListRow
    lateinit var updatedRow: ListRow
    lateinit var popularRow: ListRow
    var loading: Boolean = false
    var viewLoaded: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUIElements()
        loading = true
        viewLoaded = false
        nCallbacks = 0

        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults("ANIME", isAdult = false, onList = false, results = arrayListOf(), hasNextPage = true, sort = "Popular")
        }

        initAdapters()

        genresModel.genres?.let {
            it.forEach {
                genresAdapter.add(it.toPair())
            }
        }

        observeData()
    }

    private fun initAdapters() {
        val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
        presenter.shadowEnabled = false
        rowAdapter = ArrayObjectAdapter(presenter)
        adapter = rowAdapter

        continueAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        recommendedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        genresAdapter = ArrayObjectAdapter(GenresPresenter(true))
        trendingAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        popularAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        updatedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))

        continueRow = ListRow(HeaderItem("Continue Watching"), continueAdapter)
        recommendedRow = ListRow(HeaderItem("Recommended"), recommendedAdapter)
        genresRow = ListRow(HeaderItem("Genres"), genresAdapter)
        trendingRow = ListRow(HeaderItem("Trending"), trendingAdapter)
        popularRow = ListRow(HeaderItem("Popular"), popularAdapter)
        updatedRow = ListRow(HeaderItem("Updated"), updatedAdapter)

        progressBarManager.initialDelay = 0
        progressBarManager.show()
    }

    private fun observeData() {
        model.getTrending().observe(viewLifecycleOwner) {
            if (it != null) {
                updateHomeTVChannel(it)
                trendingAdapter.addAll(0, it)
                checkLoadingState()
            }
        }

        model.getUpdated().observe(viewLifecycleOwner) {
            if (it != null) {
                updatedAdapter.addAll(0, it)
                checkLoadingState()
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                loading = false
                model.searchResults = it
                popularAdapter.addAll(popularAdapter.size(), it.results)
                checkLoadingState()
            }
        }

        homeModel.getAnimeContinue().observe(viewLifecycleOwner) {
            if (it != null) {
                continueAdapter.addAll(0, it)
                if(it.isEmpty()) {
                    rowAdapter.remove(continueRow)
                }
                checkLoadingState()
            }
        }

        homeModel.getRecommendation().observe(viewLifecycleOwner) {
            if (it != null) {
                recommendedAdapter.addAll(0, it.filter { it.relation == "ANIME" })
                if(it.isEmpty()) {
                    rowAdapter.remove(recommendedRow)
                }
                checkLoadingState()
            }
        }
        val scope = viewLifecycleOwner.lifecycleScope
        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(viewLifecycleOwner) {
            if (it) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        model.loaded = true
                        loading = false

                        if (Anilist.userid == null) {
                            if (Anilist.query.getUserData())
                                loadUserData()
                        }

                        model.loadTrending()
                        model.loadUpdated()
                        model.loadPopular("ANIME", sort = "Popular")
                        genresModel.loadGenres(Anilist.genres?: loadData("genres_list") ?: arrayListOf()) {
                            MainScope().launch {
                                genresAdapter.add(it)
                            }
                        }
                        homeModel.loaded = true
                        homeModel.setListImages()
                        homeModel.setAnimeContinue()
                        homeModel.setRecommendation()
                    }
                    live.postValue(false)
                }
            }
        }
    }

    fun checkLoadingState(){
        if(nCallbacks == 4 && !viewLoaded) {
            progressBarManager.hide()
            //This determines order in screen
            if (Anilist.userid == null) {
                rowAdapter.add(genresRow)
                rowAdapter.add(trendingRow)
                rowAdapter.add(popularRow)
                rowAdapter.add(updatedRow)
                rowAdapter.add(ButtonListRow("Login", object : ButtonListRow.OnClickListener {
                    override fun onClick() {
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.main_browse_fragment, TVLoginFragment()).addToBackStack(null)
                            .commit()
                    }
                }))
            } else {
                if(continueAdapter.size() > 0)
                rowAdapter.add(continueRow)
                if(recommendedAdapter.size() > 0)
                rowAdapter.add(recommendedRow)

                rowAdapter.add(genresRow)
                rowAdapter.add(trendingRow)
                rowAdapter.add(popularRow)
                rowAdapter.add(updatedRow)
                rowAdapter.add(ButtonListRow("Logout", object : ButtonListRow.OnClickListener {
                    override fun onClick() {
                        Anilist.removeSavedToken(requireContext())
                        reloadScreen()
                    }
                }))
            }
            viewLoaded = true
        } else {
            nCallbacks++
        }
    }

    fun loadUserData(){
        if(activity!=null)
            lifecycleScope.launch(Dispatchers.Main) {
                title = Anilist.username
                Glide.with(requireContext())
                    .asBitmap()
                    .centerInside()
                    .load(Anilist.avatar)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            badgeDrawable = BitmapDrawable(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
                val backgroundManager = BackgroundManager.getInstance(requireActivity())
                backgroundManager.attach(requireActivity().window)
                Glide.with(requireContext())
                    .asBitmap()
                    .centerInside()
                    .load(Anilist.bg)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            backgroundManager.drawable = BitmapDrawable(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
        }
    }

    override fun onResume() {
        super.onResume()
        if(shouldReload) {
            reloadScreen()
        }
    }

    private fun reloadScreen() {
        shouldReload = false
        nCallbacks = 0
        model.loaded = true
        loading = false
        viewLoaded = false
        genresModel.genres = null
        initAdapters()
        Refresh.all()
    }

    private fun setupUIElements() {
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        // Set fastLane (or headers) background color
        //brandColor = ContextCompat.getColor(requireActivity(), R.color.violet_700)
        // Set search icon color.
        //searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.bg_black)

        setHeaderPresenterSelector(object : PresenterSelector() {
            override fun getPresenter(o: Any): Presenter {
                if(o is ButtonListRow) {
                    return ButtonListRowPresenter()
                } else {
                    return MainHeaderPresenter()
                }
            }
        })

        setOnSearchClickedListener {
            val intent = Intent(requireActivity().applicationContext, TVSearchActivity::class.java)
            startActivity(intent)
        }

        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->

            if (item is Media) {
                val intent =
                    Intent(requireActivity().applicationContext, TVAnimeDetailActivity::class.java)
                intent.putExtra("media", item as Media)
                startActivity(intent)
            } else if (item is Pair<*,*>) {
                ContextCompat.startActivity(requireContext(), Intent(requireContext(), TVSearchActivity::class.java).putExtra("type","ANIME").putExtra("genre",item.first as String).putExtra("sortBy","Trending").also {
                    //TODO deal with this when we have settings on TV
                    /* if(item.lowercase()=="hentai") {
                        it.putExtra("hentai", true)
                    }*/
                },null)
            }
        }

        setOnItemViewSelectedListener { itemViewHolder, item, rowViewHolder, row ->
            item?.let {
                if ((row as ListRow).adapter == popularAdapter && model.searchResults.hasNextPage && model.searchResults.results.isNotEmpty() && !loading && isNearEndOfList(popularAdapter, item)) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        loading=true
                        model.loadNextPage(model.searchResults)
                    }
                }
            }
        }
    }

    private fun isNearEndOfList(adapter: ArrayObjectAdapter, item: Any): Boolean {
        var found = false
        for (i in adapter.size()-PAGING_THRESHOLD until adapter.size()-1) {
            if(adapter.get(i) == item) {
                found = true
                break
            }
        }
        return found
    }

    private fun updateHomeTVChannel(animes: List<Media>) {
        clearHomeTVChannel()
        animes.forEach {
            addMediaToHomeTVChannel(it)
        }
    }

    
    private fun clearHomeTVChannel() {
        requireContext().contentResolver.delete(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null)
    }

    private fun addMediaToHomeTVChannel(media: Media): Long {
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE) ?: return -1
        val channelID = sharedPref.getLong(TVMainActivity.defaultChannelIDKey, -1)
        if (channelID == -1L) return -1

        val intent = Intent(requireContext(), TVMainActivity::class.java)
        intent.putExtra("media", media.id)

        val builder = PreviewProgram.Builder()
        builder.setChannelId(channelID)
            .setType(TvContractCompat.PreviewPrograms.TYPE_TV_SERIES)
            .setTitle(media.name)
            .setDescription(media.description)
            .setPosterArtUri(Uri.parse(media.cover))
            .setIntent(intent)

        val programURI = requireContext().contentResolver.insert(TvContractCompat.PreviewPrograms.CONTENT_URI,
            builder.build().toContentValues())
        programURI?.let {
            return ContentUris.parseId(it)
        } ?: run {
            return -1
        }
    }
}