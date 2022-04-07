package ani.saikou.tv

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.saikou.*
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HAnimeSources
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.settings.UserInterfaceSettings
import ani.saikou.tv.presenters.EpisodePresenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.*

class TVAnimeDetailFragment: DetailsSupportFragment() {

    private val model: MediaDetailsViewModel by activityViewModels()
    private val scope = lifecycleScope
    val actions = ArrayObjectAdapter()

    private lateinit var media: Media
    lateinit var uiSettings : UserInterfaceSettings
    var loaded = false

    private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController

    private lateinit var rowsAdapter: ArrayObjectAdapter

    private lateinit var episodesAdapter: ArrayObjectAdapter
    private lateinit var detailsOverview: DetailsOverviewRow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detailsBackground = DetailsSupportFragmentBackgroundController(this)

        uiSettings = loadData("ui_settings", toast = false) ?:UserInterfaceSettings().apply { saveData("ui_settings",this) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildDetails()
        observeData()
    }

    override fun onPause() {
        super.onPause()
        loaded = false
    }

    private fun buildDetails() {
        media = activity?.intent?.getSerializableExtra("media") as Media
        media.selected = model.loadSelected(media)

        initializeBackground()
        val selector = ClassPresenterSelector().apply {
            // Attach your media item details presenter to the row presenter:
            FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter()).also {
                it.setOnActionClickedListener {
                    if (it.id.toInt() == 0) {
                        parentFragmentManager.beginTransaction().addToBackStack(null)
                            .replace(
                                R.id.main_detail_fragment,
                                TVSourceSelectorFragment.newInstance()
                            ).commit()
                    }
                }
                addClassPresenter(DetailsOverviewRow::class.java, it)
            }
            val presenter = ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
            presenter.shadowEnabled = false
            addClassPresenter(ListRow::class.java, presenter)
        }

        rowsAdapter = ArrayObjectAdapter(selector)

        detailsOverview = DetailsOverviewRow(media)
        detailsOverview.actionsAdapter = actions

        val episodePresenter = EpisodePresenter(1, media, this)
        episodesAdapter = ArrayObjectAdapter(episodePresenter)

        adapter = rowsAdapter

        initializeBackground()
        initializeCover()
    }

    fun observeData() {
        model.getKitsuEpisodes().observe(viewLifecycleOwner) { i ->
            if(i!=null)
                media.anime?.kitsuEpisodes = i
        }

        model.getFillerEpisodes().observe(viewLifecycleOwner) { i ->
            if(i!=null)
                media.anime?.fillerEpisodes = i
        }

        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                media.selected = model.loadSelected(media)

                finishLoadingRows()

                if(!loaded) {
                    model.watchAnimeWatchSources = if (media.isAdult) HAnimeSources else AnimeSources

                    setupActions()

                    lifecycleScope.launch(Dispatchers.IO) {
                        awaitAll(
                            async { model.loadKitsuEpisodes(media) },
                            async { model.loadFillerEpisodes(media) }
                        )
                        model.loadEpisodes(media, media.selected!!.source)
                    }
                    loaded = true
                }
            }
        }

        model.getEpisodes().observe(viewLifecycleOwner) { loadedEpisodes ->
            if (loadedEpisodes != null) {
                val episodes = loadedEpisodes[media.selected!!.source]
                if (episodes != null) {
                    episodesAdapter.clear()
                    episodes.forEach { (i, episode) ->
                        episodesAdapter.add(episode)
                        if (media.anime?.fillerEpisodes != null) {
                            if (media.anime!!.fillerEpisodes!!.containsKey(i)) {
                                episode.title = media.anime!!.fillerEpisodes!![i]?.title
                                episode.filler =
                                    media.anime!!.fillerEpisodes!![i]?.filler ?: false
                            }
                        }
                        if (media.anime?.kitsuEpisodes != null) {
                            if (media.anime!!.kitsuEpisodes!!.containsKey(i)) {
                                episode.desc = media.anime!!.kitsuEpisodes!![i]?.desc
                                episode.title = media.anime!!.kitsuEpisodes!![i]?.title
                                episode.thumb =
                                    media.anime!!.kitsuEpisodes!![i]?.thumb ?: media.cover
                            }
                        }
                    }
                    media.anime?.episodes = episodes
                    rowsAdapter.notifyArrayItemRangeChanged(0,rowsAdapter.size())


                }
            }
        }

        val live = Refresh.activity.getOrPut(this.hashCode()){ MutableLiveData(true) }
        live.observe(requireActivity()){
            if(it){
                scope.launch(Dispatchers.IO) {
                    model.loadMedia(media)
                    live.postValue(false)
                }
            }
        }
    }

    fun onEpisodeClick(i: String) {
        model.continueMedia = false

        if (media.anime?.episodes?.get(i)!=null) {
            media.anime!!.selectedEpisode = i
        } else {
            return
        }

        media.selected = model.loadSelected(media)
        media.selected?.let {
            val selector = TVSelectorFragment.newInstance(it.stream, true)
            parentFragmentManager.beginTransaction().addToBackStack(null)
                .replace(R.id.main_detail_fragment, selector)
                .commit()
        }

    }

    private fun finishLoadingRows() {
        rowsAdapter.add(detailsOverview)
        rowsAdapter.add(ListRow(HeaderItem(1, "Episodes"), episodesAdapter))
    }

    private fun initializeBackground() {
        detailsBackground.enableParallax()
        Glide.with(this)
            .asBitmap()
            .centerInside()
            .load(media.banner)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    detailsBackground.coverBitmap = resource
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun initializeCover() {
        Glide.with(this)
            .asBitmap()
            .load(media.cover)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    detailsOverview.apply {
                        imageDrawable = resource.toDrawable(requireActivity().resources)
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setupActions() {
        actions.clear()
        val selectedSourceName: String? = model.watchAnimeWatchSources?.names?.get(media!!.selected?.source
            ?: 0)
        selectedSourceName?.let {
            actions.add(Action(0,"Source: "+ it))
        }?: kotlin.run {
            actions.add(Action(0,"Select Source"))
        }
    }

}

class DetailsDescriptionPresenter : AbstractDetailsDescriptionPresenter() {

    override fun onBindDescription(viewHolder: ViewHolder, itemData: Any) {
        val details = itemData as Media
        viewHolder.apply {
            title.text = details.name
            subtitle.text = details.userPreferredName
            body.text = details.description
        }
    }
}
