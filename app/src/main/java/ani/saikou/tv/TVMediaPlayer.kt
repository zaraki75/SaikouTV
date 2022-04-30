package ani.saikou.tv

import android.app.Dialog
import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.DisplayMetrics
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.media.session.MediaButtonReceiver
import ani.saikou.*
import ani.saikou.anilist.Anilist
import ani.saikou.anime.Episode
import ani.saikou.anime.VideoCache
import ani.saikou.anime.source.AnimeSources
import ani.saikou.anime.source.HAnimeSources
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.settings.PlayerSettings
import ani.saikou.settings.UserInterfaceSettings
import ani.saikou.tv.utils.VideoPlayerGlue
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.video.VideoSize
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import java.lang.Runnable
import kotlin.math.max
import kotlin.math.roundToInt

class TVMediaPlayer(var media: Media): VideoSupportFragment(), VideoPlayerGlue.OnActionClickedListener, Player.Listener {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var cacheFactory : CacheDataSource.Factory
    private lateinit var playbackParameters: PlaybackParameters
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var mediaItem : MediaItem
    private var interacted = false
    private var currentEpisodeIndex = 0
    private var isInitialized = false
    private lateinit var episodeArr: List<String>

    private var originalAspectRatio: Float? = null
    private var screenHeight: Int = 0
    private var screenWidth: Int = 0
    private var currentViewMode: Int = 0
    private var playbackPosition: Long = 0

    private lateinit var episode: Episode
    private lateinit var episodes: MutableMap<String,Episode>
    private lateinit var playerGlue : VideoPlayerGlue

    private val model: MediaDetailsViewModel by viewModels()
    private var episodeLength: Float = 0f

    private var settings = PlayerSettings()
    private var uiSettings = UserInterfaceSettings()

    private var linkSelector: TVSelectorFragment? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val metrics: WindowMetrics = requireContext().getSystemService(WindowManager::class.java).currentWindowMetrics
            screenHeight = metrics.bounds.height()
            screenWidth = metrics.bounds.width()
        } else {
            val outMetrics = DisplayMetrics()
            val display = requireActivity().windowManager.defaultDisplay
            display.getMetrics(outMetrics)
            screenHeight = outMetrics.heightPixels
            screenWidth = outMetrics.widthPixels
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = loadData("player_settings") ?: PlayerSettings().apply { saveData("player_settings",this) }
        uiSettings = loadData("ui_settings") ?: UserInterfaceSettings().apply { saveData("ui_settings",this) }

        linkSelector = null

        val episodeObserverRunnable = Runnable {
            model.getEpisode().observe(viewLifecycleOwner) {
                if (it != null) {
                    episode = it
                    media.selected = model.loadSelected(media)
                    model.setMedia(media)

                    val stream = episode.streamLinks[episode.selectedStream]
                    val url = stream?.quality?.get(episode.selectedQuality)
                    if(url == null) {
                        if (linkSelector == null) {
                            linkSelector = TVSelectorFragment.newInstance(media, true)
                            linkSelector?.setStreamLinks(episode.streamLinks)
                            parentFragmentManager.beginTransaction().addToBackStack(null)
                                .replace(R.id.main_tv_fragment, linkSelector!!)
                                .commit()
                        }
                    } else {
                        if(!isInitialized) {
                            episodeArr = episodes.keys.toList()
                            currentEpisodeIndex = episodeArr.indexOf(media.anime!!.selectedEpisode!!)
                            mediaItem = MediaItem.Builder().setUri(url?.url).build()
                            initVideo()
                        }
                    }
                    }
                //}
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(isInitialized)
                    progress {
                        isEnabled = false
                        isInitialized = false
                        exoPlayer.stop()
                        exoPlayer.release()
                        VideoCache.release()
                        requireActivity().supportFragmentManager.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    }
                else {
                    isEnabled = false
                    isInitialized = false
                    requireActivity().supportFragmentManager.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE)
                }
            }
        })

        surfaceView.keepScreenOn = true

        episodeObserverRunnable.run()

        //Handle Media
        model.setMedia(media)
        episodes = media.anime!!.episodes!!

        model.watchAnimeWatchSources = if(media.isAdult) HAnimeSources else AnimeSources

        //Set Episode, to invoke getEpisode() at Start
        model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!,"invoke")
        episodeArr = episodes.keys.toList()
        currentEpisodeIndex = episodeArr.indexOf(media.anime!!.selectedEpisode!!)
    }

    fun initVideo() {
        val simpleCache = VideoCache.getInstance(requireContext())

        val stream = episode.streamLinks[episode.selectedStream]?: return
        originalAspectRatio = null
        val httpClient = OkHttpClient().newBuilder().ignoreAllSSLErrors().apply {
            followRedirects(true)
            followSslRedirects(true)
        }.build()
        val dataSourceFactory = DataSource.Factory {
            val dataSource: HttpDataSource = OkHttpDataSource.Factory(httpClient).createDataSource()
            if(stream.headers!=null)
                stream.headers.forEach {
                    dataSource.setRequestProperty(it.key, it.value)
                }
            dataSource
        }
        cacheFactory = CacheDataSource.Factory().apply {
            setCache(simpleCache)
            setUpstreamDataSourceFactory(dataSourceFactory)
        }
        trackSelector = DefaultTrackSelector(requireContext())
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .setMinVideoSize(
                    loadData("maxWidth",requireActivity()) ?:720, loadData("maxHeight",requireActivity())
                        ?:480)
                .setMaxVideoSize(1,1)
        )


        //Speed
        val speeds     =  if(settings.cursedSpeeds) arrayOf(1f , 1.25f , 1.5f , 1.75f , 2f , 2.5f , 3f , 4f, 5f , 10f , 25f, 50f) else arrayOf( 0.25f , 0.33f , 0.5f , 0.66f , 0.75f , 1f , 1.25f , 1.33f , 1.5f , 1.66f , 1.75f , 2f )
        val speedsName = speeds.map { "${it}x" }.toTypedArray()
        var curSpeed   = loadData("${media.id}_speed",requireActivity()) ?:settings.defaultSpeed

        playbackParameters = PlaybackParameters(speeds[curSpeed])

        exoPlayer = ExoPlayer.Builder(requireContext())
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
            .setTrackSelector(trackSelector)
            .build().apply {
                playWhenReady = true
                this.playbackParameters = this@TVMediaPlayer.playbackParameters
                setMediaItem(mediaItem)
                prepare()
                loadData<Long>("${media.id}_${media.anime!!.selectedEpisode}_max")?.apply {
                    if (this <= playbackPosition) playbackPosition = max(0, this - 5)
                }
                seekTo(playbackPosition)
            }

        isInitialized = true
        exoPlayer.addListener(this)
        val mediaButtonReceiver = ComponentName(requireActivity(), MediaButtonReceiver::class.java)
        MediaSessionCompat(requireContext(), "Player", mediaButtonReceiver, null).let { media ->
            val mediaSessionConnector = MediaSessionConnector(media)
            mediaSessionConnector.setPlayer(exoPlayer)
            mediaSessionConnector.setClearMediaItemsOnStop(true)
            media.isActive = true
        }

        exoPlayer.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
                if (originalAspectRatio == null) {
                    originalAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    playerGlue.shouldShowResizeAction = (screenHeight / screenWidth.toFloat()) != originalAspectRatio
                }
            }
        })

        playerGlue = VideoPlayerGlue(requireActivity(), LeanbackPlayerAdapter(requireActivity(), exoPlayer, 16), episode.streamLinks[episode.selectedStream]!!.quality[episode.selectedQuality]!!.quality == "Multi Quality",this)
        playerGlue.host = VideoSupportFragmentGlueHost(this)
        playerGlue.title = media.getMainName()

        if(!episode.title.isNullOrEmpty())
            playerGlue.subtitle = "Episode "+ episode.number + ": "+ episode.title
        else
            playerGlue.subtitle = "Episode "+ episode.number


        playerGlue.playWhenPrepared()
        playerGlue.host.setOnKeyInterceptListener { view, keyCode, event ->
            if (playerGlue.host.isControlsOverlayVisible) return@setOnKeyInterceptListener false

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                playerGlue.fastForward()
                preventControlsOverlay(playerGlue)
                return@setOnKeyInterceptListener true
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.action == KeyEvent.ACTION_DOWN) {
                playerGlue.rewind()
                preventControlsOverlay(playerGlue)
                return@setOnKeyInterceptListener true
            }

            false
        }

        playerGlue.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
            override fun onPlayCompleted(glue: PlaybackGlue?) {
                super.onPlayCompleted(glue)
                onNext()
            }
        })

        adapter = ArrayObjectAdapter(playerGlue.playbackRowPresenter).apply {
            add(playerGlue.controlsRow)
        }
    }

    override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
        playerGlue.shouldShowQualityAction = tracksInfo.trackGroupInfos.size > 2
        //playerGlue.drawSecondaryActions()
    }

    // QUALITY SELECTOR
    private fun initPopupQuality(trackSelector: DefaultTrackSelector): Dialog? {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return null
        var videoRenderer: Int? = null

        fun isVideoRenderer(mappedTrackInfo: MappingTrackSelector.MappedTrackInfo, rendererIndex: Int): Boolean {
            if (mappedTrackInfo.getTrackGroups(rendererIndex).length == 0) return false
            return C.TRACK_TYPE_VIDEO == mappedTrackInfo.getRendererType(rendererIndex)
        }

        for (i in 0 until mappedTrackInfo.rendererCount)
            if (isVideoRenderer(mappedTrackInfo, i))
                videoRenderer = i

        val trackSelectionDialogBuilder =
            TrackSelectionDialogBuilder(requireContext(), "Available Qualities", trackSelector, videoRenderer ?: return null)
        trackSelectionDialogBuilder.setTheme(R.style.TVDialogTheme)
        trackSelectionDialogBuilder.setTrackNameProvider {
            if (it.frameRate > 0f) it.height.toString() + "p" else it.height.toString() + "p (fps : N/A)"
        }
        trackSelectionDialogBuilder.setAllowAdaptiveSelections(false)
        return trackSelectionDialogBuilder.build()
    }

    private fun preventControlsOverlay(playerGlue: VideoPlayerGlue) = view?.postDelayed({
        playerGlue.host.showControlsOverlay(false)
        playerGlue.host.hideControlsOverlay(false)
    }, 10)

    override fun onQuality() {
        if(playerGlue.shouldShowQualityAction)
            initPopupQuality(trackSelector)?.show()
    }

    override fun onPrevious() {
        if(currentEpisodeIndex>0) {
            change(currentEpisodeIndex - 1)
        }
    }

    override fun onNext() {
        if(isInitialized) {
            nextEpisode{ i-> progress { change(currentEpisodeIndex + i) } }
        }
    }

    override fun onResize() {
        if(!playerGlue.shouldShowResizeAction) return

        currentViewMode++
        if (currentViewMode>2) currentViewMode = 0

        var params = surfaceView.layoutParams
        when(currentViewMode){
            0 -> {  //Original
                params.height = screenHeight
                params.width = (screenHeight*originalAspectRatio!!).toInt()
            }
            1 -> {  //FitWidth
                params.height = (screenWidth/originalAspectRatio!!).toInt()
                params.width = screenWidth
            }
            2 -> {  //FitXY
                params.height = screenHeight
                params.width = screenWidth
            }
        }
        surfaceView.layoutParams = params
    }

    private fun nextEpisode(runnable: ((Int)-> Unit) ){
        var isFiller = true
        var i=1
        while (isFiller) {
            if (episodeArr.size > currentEpisodeIndex + i) {
                isFiller = if (settings.autoSkipFiller) episodes[episodeArr[currentEpisodeIndex + i]]?.filler?:false else false
                if (!isFiller) runnable.invoke(i)
                i++
            }
            else {
                isFiller = false
            }
        }
    }

    fun change(index:Int){
        if(isInitialized) {
            exoPlayer.stop()
            exoPlayer.release()
            VideoCache.release()

            isInitialized = false
            saveData(
                "${media.id}_${episodeArr[currentEpisodeIndex]}",
                exoPlayer.currentPosition,
                requireActivity()
            )
            val prev = episodeArr[currentEpisodeIndex]
            episodeLength= 0f
            media.anime!!.selectedEpisode = episodeArr[index]
            model.setMedia(media)
            model.epChanged.postValue(false)
            model.setEpisode(episodes[media.anime!!.selectedEpisode!!]!!,"change")

            media.anime?.episodes?.get(media.anime!!.selectedEpisode!!)?.let{ ep ->
                media.selected = model.loadSelected(media)
                lifecycleScope.launch {
                    media.selected?.let {
                        model.loadEpisodeStreams(ep, it.source)
                    }
                }
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == ExoPlayer.STATE_READY) {
            exoPlayer.play()
            if (episodeLength == 0f) {
                episodeLength = exoPlayer.duration.toFloat()
            }
        }
        if(playbackState == Player.STATE_ENDED && settings.autoPlay){
            if(interacted)
                onNext()
        }
        super.onPlaybackStateChanged(playbackState)
    }

    private fun progress(runnable: Runnable){
        if (exoPlayer.currentPosition / episodeLength > settings.watchPercentage && Anilist.userid != null) {
            if(loadData<Boolean>("${media.id}_save_progress")!=false && if (media.isAdult) settings.updateForH else true)
                updateAnilist(media, media.anime!!.selectedEpisode!!)
            runnable.run()
        } else {
            runnable.run()
        }
    }

    private fun updateAnilist(media: Media, number: String){
        if(Anilist.userid!=null) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val a = number.toFloatOrNull()?.roundToInt()
                if (a != media.userProgress) {
                    Anilist.mutation.editList(
                        media.id,
                        a,
                        status = if (media.userStatus == "REPEATING") media.userStatus else "CURRENT"
                    )
                }
                media.userProgress = a
                Refresh.all()
            }
        }
    }
}