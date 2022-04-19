package ani.saikou.tv

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.PlaybackGlue
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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSource
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
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

    private lateinit var episode: Episode
    private lateinit var episodes: MutableMap<String,Episode>
    private lateinit var playerGlue : VideoPlayerGlue

    private val model: MediaDetailsViewModel by viewModels()
    private var episodeLength: Float = 0f

    private var settings = PlayerSettings()
    private var uiSettings = UserInterfaceSettings()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settings = loadData("player_settings") ?: PlayerSettings().apply { saveData("player_settings",this) }
        uiSettings = loadData("ui_settings") ?: UserInterfaceSettings().apply { saveData("ui_settings",this) }

        val episodeObserverRunnable = Runnable {
            model.getEpisode().observe(this) {
                if (it != null) {
                    episode = it
                    media.selected = model.loadSelected(media)
                    model.setMedia(media)

                    val stream = episode.streamLinks[episode.selectedStream]
                    val url = stream?.quality?.get(episode.selectedQuality)
                    mediaItem =  MediaItem.Builder().setUri(url?.url).build()
                    initVideo()
                }
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
            }

        isInitialized = true
        exoPlayer.addListener(this)
        val mediaButtonReceiver = ComponentName(requireActivity(), MediaButtonReceiver::class.java)
        MediaSessionCompat(requireContext(), "Player", mediaButtonReceiver, null).let { media ->
            val mediaSessionConnector = MediaSessionConnector(media)
            mediaSessionConnector.setPlayer(exoPlayer)
            media.isActive = true
        }

        playerGlue = VideoPlayerGlue(requireActivity(), LeanbackPlayerAdapter(requireActivity(), exoPlayer, 16), this)
        playerGlue.host = VideoSupportFragmentGlueHost(this)
        playerGlue.addPlayerCallback(object : PlaybackGlue.PlayerCallback() {
            override fun onPreparedStateChanged(glue: PlaybackGlue) {
                if (glue.isPrepared()) {
                    playerGlue.play()
                }
            }
        })
    }

    override fun onPrevious() {
        if(currentEpisodeIndex>0) {
            //change(currentEpisodeIndex - 1)
        }
    }

    override fun onNext() {
        if(isInitialized) {
            //nextEpisode{ i-> progress { change(currentEpisodeIndex + i) } }
        }
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
            //changingServer = false
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
            model.onEpisodeClick(
                media, media.anime!!.selectedEpisode!!, requireActivity().supportFragmentManager,
                false,
                prev)
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
            if(loadData<Boolean>("${media.id}_save_progress")!=false)
                updateAnilist(media.id, media.anime!!.selectedEpisode!!)
            runnable.run()
        } else {
            runnable.run()
        }
    }

    private fun updateAnilist(id: Int, number: String){
        if(Anilist.userid!=null) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                val a = number.toFloatOrNull()?.roundToInt()
                Anilist.mutation.editList(id, a, status = "CURRENT")
                Refresh.all()
            }
        }
    }
}