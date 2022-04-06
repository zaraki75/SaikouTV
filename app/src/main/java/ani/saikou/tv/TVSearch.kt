package ani.saikou.tv

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.lifecycle.lifecycleScope
import ani.saikou.anilist.AnilistSearch
import ani.saikou.anilist.SearchResults
import ani.saikou.media.Media
import ani.saikou.tv.presenters.AnimePresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TVSearch: SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    lateinit var rowAdapter: ArrayObjectAdapter
    private val scope = lifecycleScope

    val model: AnilistSearch by viewModels()
    var type = "ANIME"

    var searchText: String? = null
    var genre: String? = null
    var sortBy: String? = null
    var _tag: String? = null
    var adult = false
    var listOnly :Boolean?= null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //AnimePresenter(0, requireActivity())
        rowAdapter = ArrayObjectAdapter(ListRowPresenter())

        val notSet = model.notSet
        if(model.notSet) {
            model.notSet = false
            model.searchResults = SearchResults(type,
                isAdult = false,
                onList = null,
                results = arrayListOf(),
                hasNextPage = false)
        }

        model.getSearch().observe(this) {
            if (it != null) {
                model.searchResults.apply {
                    onList = it.onList
                    isAdult = it.isAdult
                    perPage = it.perPage
                    search = it.search
                    sort = it.sort
                    genres = it.genres
                    tags = it.tags
                    format = it.format
                    page = it.page
                    hasNextPage = it.hasNextPage
                }

                val prev = model.searchResults.results.size
                model.searchResults.results.addAll(it.results)
                val listRowAdapter = ArrayObjectAdapter(AnimePresenter(0, requireActivity()))

                listRowAdapter.addAll(0,it.results)
                rowAdapter.add(ListRow(HeaderItem("Results"), listRowAdapter))
            }
        }


        setSearchResultProvider(this)
        setOnItemViewClickedListener { itemViewHolder, item, rowViewHolder, row ->
            val intent = Intent(requireActivity().applicationContext, TVAnimeDetailActivity::class.java)
            intent.putExtra("media", item as Media)
            startActivity(intent)
        }
    }

    private var searchTimer = Timer()
    private var loading = false
    fun search(
        search: String? = null,
        genre: String? = null,
        tag: String? = null,
        sort: String? = null,
        adult: Boolean = false,
        listOnly: Boolean? = null
    ) {
        val size = model.searchResults.results.size
        model.searchResults.results.clear()
        rowAdapter.clear()

        this.genre = genre
        this.sortBy = sort
        this.searchText = search
        this.adult = adult
        this._tag = tag
        this.listOnly = listOnly

        searchTimer.cancel()
        searchTimer.purge()
        val timerTask: TimerTask = object : TimerTask() {
            override fun run() {
                scope.launch(Dispatchers.IO) {
                    loading = true
                    model.loadSearch(
                        type,
                        search,
                        if (genre != null) arrayListOf(genre) else null,
                        if (tag != null) arrayListOf(tag) else null,
                        sort,
                        adult,
                        listOnly
                    )
                    loading = false
                }
            }
        }
        searchTimer = Timer()
        searchTimer.schedule(timerTask, 500)
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        search(query)
        return true
    }


}