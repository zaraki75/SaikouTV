package ani.saikou.tv

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.ProgressBarManager
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import ani.saikou.anime.Episode
import ani.saikou.databinding.ItemUrlBinding
import ani.saikou.databinding.TvItemUrlBinding
import ani.saikou.download
import ani.saikou.loadData
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import ani.saikou.toastString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class TVSelectorFragment: VerticalGridSupportFragment() {

    val model : MediaDetailsViewModel by activityViewModels()
    private var scope: CoroutineScope = lifecycleScope
    private var media: Media? = null
    private var episode: Episode? = null
    private var prevEpisode: Episode? = null
    private var makeDefault = false
    private var selected:String?=null
    private var launch:Boolean?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            selected = it.getString("server")
            launch = it.getBoolean("launch",true)
            prevEpisode = it.getSerializable("prev") as? Episode
        }

        title = "Select quality"

        progressBarManager.initialDelay = 0
        progressBarManager.show()
        val presenter = VerticalGridPresenter()
        presenter.numberOfColumns = 1
        gridPresenter = presenter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.getMedia().observe(viewLifecycleOwner) { m ->
            media = m
            if (media != null) {
                episode = media?.anime?.episodes?.get(media?.anime?.selectedEpisode)
                if(episode!=null){
                    if (selected != null) {

                        fun fail() {
                           cancel()
                        }

                        fun load() {
                            if (episode?.streamLinks?.containsKey(selected) == true) {
                                if (episode!!.streamLinks[selected]!!.quality.size >= media!!.selected!!.quality) {
                                    media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedStream =
                                        selected
                                    media!!.anime!!.episodes?.get(media!!.anime!!.selectedEpisode!!)?.selectedQuality =
                                        media!!.selected!!.quality
                                    startExoplayer(media!!)
                                } else fail()
                            } else fail()
                        }
                        if (episode?.streamLinks?.isEmpty() == true) {
                            model.getEpisode().observe(viewLifecycleOwner) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch {
                                if (withContext(Dispatchers.IO){ !model.loadEpisodeStream(episode!!, media!!.selected!!) }) fail()
                            }
                        } else load()
                    }
                    else {
                        makeDefault = loadData("make_default") ?:true

                        fun load() {
                            media!!.anime?.episodes?.set(media!!.anime?.selectedEpisode?:"",
                                episode!!
                            )

                            val links = episode!!.streamLinks
                            val linkList = mutableListOf<Episode.StreamLinks>()

                            links.keys.toList().forEach { key ->
                                links[key]?.let { links ->
                                    links.quality.forEach {
                                        linkList.add(Episode.StreamLinks(links.server, listOf(it), links.headers, links.subtitles))
                                    }
                                }
                            }

                            val arrayAdapter = ArrayObjectAdapter(StreamAdapter())
                            arrayAdapter.addAll(0, linkList)
                            adapter = arrayAdapter
                            progressBarManager.hide()
                        }
                        if (episode!!.streamLinks.isEmpty() || !episode!!.allStreams) {
                            model.getEpisode().observe(viewLifecycleOwner) {
                                if (it != null) {
                                    episode = it
                                    load()
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                model.loadEpisodeStreams(episode!!, media!!.selected!!.source)
                            }
                        } else load()
                    }
                }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    fun startExoplayer(media: Media){
        model.epChanged.postValue(true)
        if (launch!!) {
            val intent = Intent(activity, TVVideoActivity::class.java).apply {
                putExtra("media", media)
            }
            startActivity(intent)
        }
        else{
            model.setEpisode(media.anime!!.episodes!![media.anime.selectedEpisode!!]!!,"startExo no launch")
        }
    }

    fun cancel() {
        media!!.selected!!.stream = null
        model.saveSelected(media!!.id, media!!.selected!!, requireActivity())
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {
        fun newInstance(server:String?=null,la:Boolean=true,prev:Episode?=null): TVSelectorFragment =
            TVSelectorFragment().apply {
                arguments = Bundle().apply {
                    putString("server",server)
                    putBoolean("launch",la)
                    putSerializable("prev",prev)
                }
            }
    }

    private inner class StreamAdapter : Presenter() {
        val links = episode!!.streamLinks

        private inner class UrlViewHolder(val binding: TvItemUrlBinding) : Presenter.ViewHolder(binding.root) {}

        override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder = UrlViewHolder(TvItemUrlBinding.inflate(LayoutInflater.from(parent?.context), parent, false))


        override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
            val stream = item as Episode.StreamLinks
            val server = stream.server
            val quality = stream.quality.first()
            val qualityPos = links.values.find { it?.server == server }?.quality?.indexOfFirst { it.quality == quality.quality }
            val holder = viewHolder as? UrlViewHolder
            if(server!=null && holder != null && qualityPos != null) {
                holder.view.setOnClickListener {
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedStream = server
                    media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]?.selectedQuality = qualityPos
                    if(makeDefault){
                        media!!.selected!!.stream = server
                        media!!.selected!!.quality = qualityPos
                        model.saveSelected(media!!.id,media!!.selected!!,requireActivity())
                    }
                    cancel()
                    startExoplayer(media!!)
                }

                val binding = holder.binding
                val url = quality
                binding.serverName.text = stream.server
                binding.urlQuality.text = url.quality
                binding.urlNote.text = url.note?:""
                binding.urlNote.visibility = if(url.note!=null) View.VISIBLE else View.GONE
                if(url.quality!="Multi Quality") {
                    binding.urlSize.visibility = if(url.size!=null) View.VISIBLE else View.GONE
                    binding.urlSize.text = (if (url.note!=null) " : " else "")+ DecimalFormat("#.##").format(url.size?:0).toString()+" MB"

                    //TODO Download? on TV?
                    /*binding.urlDownload.visibility = View.VISIBLE
                    binding.urlDownload.setOnClickListener {
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedStream = server
                        media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!.selectedQuality = qualityPos
                        download(requireActivity(),media!!.anime!!.episodes!![media!!.anime!!.selectedEpisode!!]!!,media!!.userPreferredName)
                    }*/
                }

            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
    }
}