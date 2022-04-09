package ani.saikou.tv

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import ani.saikou.R
import ani.saikou.Refresh
import ani.saikou.anilist.Anilist
import ani.saikou.anilist.AnilistAnimeViewModel
import ani.saikou.anilist.GenresViewModel
import ani.saikou.anilist.SearchResults
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.presenters.AnimePresenter
import ani.saikou.tv.presenters.GenresPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TVAnimeFragment: BrowseSupportFragment()  {

    private val PAGING_THRESHOLD = 15

    val model: AnilistAnimeViewModel by activityViewModels()
    val genresModel: GenresViewModel by viewModels()

    lateinit var genresAdapter: ArrayObjectAdapter
    lateinit var trendingAdapter: ArrayObjectAdapter
    lateinit var updatedAdapter: ArrayObjectAdapter
    lateinit var popularAdapter: ArrayObjectAdapter
    lateinit var rowAdapter: ArrayObjectAdapter
    var loading = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUIElements()

        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults("ANIME", isAdult = false, onList = false, results = arrayListOf(), hasNextPage = true, sort = "Popular")
        }

        val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
        presenter.shadowEnabled = false
        rowAdapter = ArrayObjectAdapter(presenter)
        adapter = rowAdapter

        genresAdapter = ArrayObjectAdapter(GenresPresenter("ANIME",true))
        trendingAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        popularAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))
        updatedAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))

        //This determines order in screen
        rowAdapter.add(ListRow(HeaderItem(1, "Genres"), genresAdapter))
        rowAdapter.add(ListRow(HeaderItem(0, "Trending"), trendingAdapter))
        rowAdapter.add(ListRow(HeaderItem(0, "Popular"), popularAdapter))
        rowAdapter.add(ListRow(HeaderItem(0, "Updated"), updatedAdapter))

        progressBarManager.initialDelay = 0
        progressBarManager.show()
        observeData()
    }

    private fun observeData() {
        model.getTrending().observe(viewLifecycleOwner) {
            if (it != null) {
                updateHomeTVChannel(it)
                trendingAdapter.addAll(0, it)
                progressBarManager.hide()
            }
        }

        model.getUpdated().observe(viewLifecycleOwner) {
            if (it != null) {
                updatedAdapter.addAll(0, it)
                progressBarManager.hide()
            }
        }

        model.getPopular().observe(viewLifecycleOwner) {
            if (it != null) {
                loading = false
                model.searchResults = it
                popularAdapter.addAll(popularAdapter.size(), it.results)
                progressBarManager.hide()
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
                        model.loadTrending()
                        model.loadUpdated()
                        model.loadPopular("ANIME", sort = "Popular")
                        genresModel.loadGenres(Anilist.genres?: loadData("genres_list") ?: arrayListOf()) {
                            MainScope().launch {
                                genresAdapter.add(it)
                            }
                        }
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
        //brandColor = ContextCompat.getColor(requireActivity(), R.color.violet_700)
        badgeDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_logo)

        // Set search icon color.
        //searchAffordanceColor = ContextCompat.getColor(requireActivity(), R.color.bg_black)
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