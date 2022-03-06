package ani.saikou.anime

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.*
import ani.saikou.anime.source.Sources
import ani.saikou.databinding.ItemAnimeWatchBinding
import ani.saikou.databinding.ItemChipBinding
import ani.saikou.media.Media
import ani.saikou.media.SourceSearchDialogFragment
import com.google.android.material.chip.Chip

class AnimeWatchAdapter(private val media: Media, private val fragment: AnimeWatchFragment,private val sources: Sources): RecyclerView.Adapter<AnimeWatchAdapter.ViewHolder>() {

    private var _binding: ItemAnimeWatchBinding?=null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val bind = ItemAnimeWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(bind)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        _binding = binding

        //Timer
        countDown(media,binding.animeSourceContainer)

        //Youtube
        if (media.anime!!.youtube != null) {
            binding.animeSourceYT.visibility = View.VISIBLE
            binding.animeSourceYT.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(media.anime.youtube))
                fragment.requireContext().startActivity(intent)
            }
        }

        //Source Selection
        binding.animeSource.setText(sources.names[media.selected!!.source])
        sources[media.selected!!.source]!!.live.observe(fragment.viewLifecycleOwner){ binding.animeSourceTitle.text = it }
        binding.animeSource.setAdapter(ArrayAdapter(fragment.requireContext(), R.layout.item_dropdown, sources.names))
        binding.animeSourceTitle.isSelected = true
        binding.animeSource.setOnItemClickListener { _, _, i, _ ->
            binding.animeSourceTitle.text = ""
            fragment.onSourceChange(i).observe(fragment.viewLifecycleOwner){ binding.animeSourceTitle.text = it }
        }

        //Wrong Title
        binding.animeSourceSearch.setOnClickListener {
            SourceSearchDialogFragment().show(fragment.requireActivity().supportFragmentManager, null)
        }

        //Icons
        var reversed = media.selected!!.recyclerReversed
        var style = media.selected!!.recyclerStyle
        binding.animeSourceTop.rotation = if (!reversed) 90f else -90f
        binding.animeSourceTop.setOnClickListener {
            binding.animeSourceTop.rotation = if (reversed) 90f else -90f
            reversed = !reversed
            fragment.onIconPressed(style,reversed)
        }
        var selected = when (media.selected!!.recyclerStyle) {
            0 -> binding.animeSourceList
            1 -> binding.animeSourceGrid
            2 -> binding.animeSourceCompact
            else -> binding.animeSourceList
        }
        selected.alpha = 1f
        fun selected(it:ImageView){
            selected.alpha=0.33f
            selected = it
            selected.alpha = 1f
        }
        binding.animeSourceList.setOnClickListener {
            selected(it as ImageView)
            style = 0
            fragment.onIconPressed(style,reversed)
        }
        binding.animeSourceGrid.setOnClickListener {
            selected(it as ImageView)
            style = 1
            fragment.onIconPressed(style,reversed)
        }
        binding.animeSourceCompact.setOnClickListener {
            selected(it as ImageView)
            style = 2
            fragment.onIconPressed(style,reversed)
        }

        //Episode Handling
        handleEpisodes()
    }
    //Chips
    @SuppressLint("SetTextI18n")
    fun updateChips(limit:Int, names : Array<String>, arr: Array<Int>, selected:Int=0){
        val binding = _binding
        if(binding!=null) {
            val screenWidth = fragment.screenWidth.px
            var select: Chip?=null
            for (position in arr.indices) {
                val last = if (position + 1 == arr.size) names.size else (limit * (position + 1))
                val chip = ItemChipBinding.inflate(LayoutInflater.from(fragment.context), binding.animeSourceChipGroup, false).root
                chip.isCheckable = true
                fun selected() {
                    chip.isChecked = true
                    binding.animeWatchChipScroll.smoothScrollTo((chip.left - screenWidth / 2) + (chip.width / 2), 0)
                }
                chip.text = "${names[limit * (position)]} - ${names[last - 1]}"

                chip.setOnClickListener {
                    selected()
                    fragment.onChipClicked(position, limit * (position), last - 1)
                }
                binding.animeSourceChipGroup.addView(chip)
                if (selected == position) {
                    selected()
                    select = chip
                }
            }
            if(select!=null)
                binding.animeWatchChipScroll.apply { post{ scrollTo((select.left - screenWidth / 2) + (select.width / 2), 0) } }
        }
    }

    fun clearChips(){
        _binding?.animeSourceChipGroup?.removeAllViews()
    }

    @SuppressLint("SetTextI18n")
    fun handleEpisodes(){
        val binding = _binding
        if(binding!=null){
            if(media.anime?.episodes!=null) {
                val episodes = media.anime.episodes!!.keys.toTypedArray()
                var continueEp = loadData<String>("${media.id}_current_ep") ?:media.userProgress?.plus(1).toString()
                if(episodes.contains(continueEp)) {
                    binding.animeSourceContinue.visibility = View.VISIBLE
                    handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,continueEp)
                    if((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight>0.8f){
                        val  e = episodes.indexOf(continueEp)
                        if (e != - 1 && e+1 < episodes.size) {
                            continueEp = episodes[e + 1]
                            handleProgress(binding.itemEpisodeProgressCont,binding.itemEpisodeProgress,binding.itemEpisodeProgressEmpty,media.id,continueEp)
                        }
                    }
                    val ep = media.anime.episodes!![continueEp]!!
                    binding.itemEpisodeImage.loadImage(ep.thumb?:media.banner?:media.cover)
                    if(ep.filler) binding.itemEpisodeFillerView.visibility = View.VISIBLE
                    binding.animeSourceContinueText.text = "Continue : Episode ${ep.number}${if(ep.filler) " - Filler" else ""}${if(ep.title!=null) "\n${ep.title}" else ""}"
                    binding.animeSourceContinue.setOnClickListener {
                        fragment.onEpisodeClick(continueEp)
                    }
                    if(fragment.continueEp) {
                        if((binding.itemEpisodeProgress.layoutParams as LinearLayout.LayoutParams).weight<0.8f) {
                            binding.animeSourceContinue.performClick()
                            fragment.continueEp = false
                        }

                    }
                }
                binding.animeSourceProgressBar.visibility = View.GONE
                if(media.anime.episodes!!.isNotEmpty())
                    binding.animeSourceNotFound.visibility = View.GONE
                else
                    binding.animeSourceNotFound.visibility = View.VISIBLE
            }
            else{
                binding.animeSourceContinue.visibility = View.GONE
                binding.animeSourceNotFound.visibility = View.GONE
                clearChips()
                binding.animeSourceProgressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = 1

    inner class ViewHolder(val binding: ItemAnimeWatchBinding) : RecyclerView.ViewHolder(binding.root)
}