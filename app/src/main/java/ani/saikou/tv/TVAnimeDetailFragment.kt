package ani.saikou.tv

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.app.DetailsSupportFragmentBackgroundController
import androidx.leanback.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import ani.saikou.R
import ani.saikou.Refresh
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HAnimeSources
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.saveData
import ani.saikou.settings.UserInterfaceSettings
import ani.saikou.tv.components.CustomListRowPresenter
import ani.saikou.tv.components.HeaderOnlyRow
import ani.saikou.tv.presenters.DetailActionsPresenter
import ani.saikou.tv.presenters.DetailsDescriptionPresenter
import ani.saikou.tv.presenters.HeaderRowPresenter
import ani.saikou.tv.presenters.EpisodePresenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class TVAnimeDetailFragment : DetailsSupportFragment() {

    private val model: MediaDetailsViewModel by activityViewModels()
    private val scope = lifecycleScope
    val actions = ArrayObjectAdapter(DetailActionsPresenter())

    private lateinit var media: Media
    lateinit var uiSettings: UserInterfaceSettings
    var loaded = false

    private lateinit var detailsBackground: DetailsSupportFragmentBackgroundController

    private lateinit var rowsAdapter: ArrayObjectAdapter
    var episodePresenters = mutableListOf<ArrayObjectAdapter>()
    private lateinit var detailsOverview: DetailsOverviewRow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        detailsBackground = DetailsSupportFragmentBackgroundController(this)

        uiSettings = loadData("ui_settings", toast = false)
            ?: UserInterfaceSettings().apply { saveData("ui_settings", this) }
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
        val mediaObject = requireActivity().intent.getSerializableExtra("media")
        if (mediaObject is Int) {
            //TODO null check this monster
            val name = requireActivity().intent.getStringExtra("name")
            val nameRomanji = requireActivity().intent.getStringExtra("nameRomaji")
            val userPreferredName = requireActivity().intent.getStringExtra("userPreferredName")
            val isAdult = requireActivity().intent.getBooleanExtra("isAdult", false)
            media = Media(
                id = mediaObject,
                name = name!!,
                nameRomaji = nameRomanji!!,
                userPreferredName = userPreferredName!!,
                isAdult = isAdult!!
            )
        } else {
            media = mediaObject as Media
        }

        media.selected = model.loadSelected(media)

        val selector = ClassPresenterSelector().apply {
            FullWidthDetailsOverviewRowPresenter(DetailsDescriptionPresenter()).also {
                it.backgroundColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
                it.actionsBackgroundColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
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
            val presenter = CustomListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM, false)
            presenter.shadowEnabled = false
            addClassPresenter(ListRow::class.java, presenter)
            addClassPresenter(HeaderOnlyRow::class.java, HeaderRowPresenter())
        }

        rowsAdapter = ArrayObjectAdapter(selector)

        detailsOverview = DetailsOverviewRow(media)
        detailsOverview.actionsAdapter = actions

        adapter = rowsAdapter

        initializeBackground()
        initializeCover()
    }

    fun observeData() {
        model.getKitsuEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.kitsuEpisodes = i
        }

        model.getFillerEpisodes().observe(viewLifecycleOwner) { i ->
            if (i != null)
                media.anime?.fillerEpisodes = i
        }

        model.getMedia().observe(viewLifecycleOwner) {
            if (it != null) {
                media = it
                media.selected = model.loadSelected(media)

                if (!loaded) {
                    finishLoadingRows()
                    model.watchAnimeWatchSources =
                        if (media.isAdult) HAnimeSources else AnimeSources

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
            loadedEpisodes?.let { epMap ->
                epMap[media.selected!!.source]?.let { ep ->
                    val episodes = ep
                    media.anime?.episodes = ep

                    clearEpisodes()

                    episodes.forEach { (i, episode) ->
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

                    //CHIP GROUP
                    val total = episodes.size
                    val divisions = total.toDouble() / 10
                    val limit = when {
                        (divisions < 25) -> 25
                        (divisions < 50) -> 50
                        else -> 100
                    }

                    if (total == 0) {
                        rowsAdapter.removeItems(1, rowsAdapter.size() - 1)
                        rowsAdapter.add(HeaderOnlyRow("No episodes found, try another source"))
                    } else if (total > limit) {
                        val arr = episodes.keys.toList()
                        val numberOfChips = ceil((total).toDouble() / limit).toInt()


                        for (index in 0..numberOfChips - 1) {
                            val last =
                                if (index + 1 == numberOfChips) total else (limit * (index + 1))
                            val startIndex = limit * (index)
                            val start = arr[startIndex]
                            val end = arr[last - 1]
                            createEpisodePresenter("Episodes ${start} - ${end}").addAll(
                                0,
                                episodes.values.toList().subList(startIndex, min(startIndex+limit,episodes.values.size))
                            )
                        }

                    } else {
                        createEpisodePresenter("Episodes").addAll(
                            0,
                            episodes.values.toList()
                        )
                    }
                }
            }
        }

        val live = Refresh.activity.getOrPut(this.hashCode()) { MutableLiveData(true) }
        live.observe(requireActivity()) {
            if (it) {
                scope.launch(Dispatchers.IO) {
                    model.loadMedia(media)
                    live.postValue(false)
                }
            }
        }
    }

    fun onEpisodeClick(i: String) {
        model.continueMedia = false

        if (media.anime?.episodes?.get(i) != null) {
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

    private fun createEpisodePresenter(title: String): ArrayObjectAdapter {
        val adapter = ArrayObjectAdapter(EpisodePresenter(1, media, this))
        episodePresenters.add(adapter)
        rowsAdapter.add(ListRow(HeaderItem(1, title), adapter))
        return adapter
    }

    private fun clearEpisodes() {
        rowsAdapter.removeItems(1, rowsAdapter.size() - 1)
        episodePresenters.clear()
    }

    private fun finishLoadingRows() {
        rowsAdapter.add(detailsOverview)
        rowsAdapter.add(HeaderOnlyRow(null))
        requireActivity().findViewById<ProgressBar>(R.id.progress).visibility = View.GONE
    }

    private fun initializeBackground() {
        detailsBackground.solidColor = ContextCompat.getColor(requireContext(), R.color.bg_black)
        detailsBackground.enableParallax()
        Glide.with(this)
            .asBitmap()
            .centerInside()
            .load(media.banner)
            .into(object : CustomTarget<Bitmap>() {
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
            .into(object : CustomTarget<Bitmap>() {
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
        val selectedSourceName: String? = model.watchAnimeWatchSources?.names?.get(
            media!!.selected?.source
                ?: 0
        )
        selectedSourceName?.let {
            actions.add(Action(0, "Source: " + it))
        } ?: kotlin.run {
            actions.add(Action(0, "Select Source"))
        }
    }

}
