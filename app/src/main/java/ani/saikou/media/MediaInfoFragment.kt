package ani.saikou.media

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ani.saikou.*
import ani.saikou.databinding.FragmentMediaInfoBinding
import ani.saikou.databinding.ItemChipBinding
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import java.io.Serializable
import java.net.URLEncoder


@SuppressLint("SetTextI18n")
class MediaInfoFragment : Fragment() {
    private var _binding: FragmentMediaInfoBinding? = null
    private val binding get() = _binding!!
    private var timer: CountDownTimer? = null
    private var loaded = false
    private var type = "ANIME"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMediaInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView();_binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val screenWidth = resources.displayMetrics.run { widthPixels / density }
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.visibility = if (loaded) View.VISIBLE else View.GONE
        binding.mediaInfoContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin += 128f.px + navBarHeight }

        val model : MediaDetailsViewModel by activityViewModels()
        model.getMedia().observe(viewLifecycleOwner) { media ->
            if (media != null) {
                loaded = true
                binding.mediaInfoProgressBar.visibility = View.GONE
                binding.mediaInfoContainer.visibility = View.VISIBLE
                binding.mediaInfoName.text = "\t\t\t" + media.getMainName()
                if (media.name != "null")
                    binding.mediaInfoNameRomajiContainer.visibility = View.VISIBLE
                binding.mediaInfoNameRomaji.text = "\t\t\t" + media.nameRomaji
                binding.mediaInfoMeanScore.text =
                    if (media.meanScore != null) (media.meanScore / 10.0).toString() else "??"
                binding.mediaInfoStatus.text = media.status
                binding.mediaInfoFormat.text = media.format
                binding.mediaInfoSource.text = media.source
                binding.mediaInfoStart.text =
                    if (media.startDate.toString() != "") media.startDate.toString() else "??"
                binding.mediaInfoEnd.text =
                    if (media.endDate.toString() != "") media.endDate.toString() else "??"
                if (media.anime != null) {
                    binding.mediaInfoDuration.text =
                        if (media.anime.episodeDuration != null) media.anime.episodeDuration.toString() else "??"
                    binding.mediaInfoDurationContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeasonContainer.visibility = View.VISIBLE
                    binding.mediaInfoSeason.text =
                        media.anime.season ?: "??" + " " + media.anime.seasonYear
                    if (media.anime.mainStudio != null) {
                        binding.mediaInfoStudioContainer.visibility = View.VISIBLE
                        binding.mediaInfoStudio.text = media.anime.mainStudio!!.name
                        binding.mediaInfoStudioContainer.setOnClickListener {
                            ContextCompat.startActivity(
                                requireActivity(),
                                Intent(activity, StudioActivity::class.java).putExtra(
                                    "studio",
                                    media.anime.mainStudio!! as Serializable
                                ),
                                null
                            )
                        }
                    }
                    binding.mediaInfoTotalTitle.setText(R.string.total_eps)
                    binding.mediaInfoTotal.text =
                        if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes
                            ?: "~").toString()) else (media.anime.totalEpisodes ?: "~").toString()
                    val markWon = Markwon.builder(requireContext()).usePlugin(SoftBreakAddsNewLinePlugin.create()).build()

                    fun makeLink(a:String):String{
                        val first = a.indexOf('"').let{ if(it!=-1) it else return a}+1
                        val end = a.indexOf('"',first).let{ if(it!=-1) it else return a}
                        val name = a.subSequence(first,end).toString()
                        return "${a.subSequence(0,first)}[$name](https://www.youtube.com/results?search_query=${URLEncoder.encode(name, "utf-8")})${a.subSequence(end,a.length)}"
                    }

                    if(media.anime.op.isNotEmpty()){
                        binding.mediaInfoOpening.visibility = View.VISIBLE
                        binding.mediaInfoOpeningText.visibility = View.VISIBLE
                        var desc =  ""
                        media.anime.op.forEach{
                            desc+="\n"
                            desc+=makeLink(it)
                        }
                        desc = desc.removePrefix("\n")

                        markWon.setMarkdown(binding.mediaInfoOpening,desc)
                    }


                    if(media.anime.ed.isNotEmpty()){
                        binding.mediaInfoEnding.visibility = View.VISIBLE
                        binding.mediaInfoEndingText.visibility = View.VISIBLE
                        var desc =  ""
                        media.anime.ed.forEach{
                            desc+="\n"
                            desc+=makeLink(it)
                        }
                        desc = desc.removePrefix("\n")

                        markWon.setMarkdown(binding.mediaInfoEnding,desc)
                    }
                }
                else if (media.manga != null) {
                    type = "MANGA"
                    binding.mediaInfoTotalTitle.setText(R.string.total_chaps)
                    binding.mediaInfoTotal.text = (media.manga.totalChapters ?: "~").toString()
                }

                val desc = HtmlCompat.fromHtml(
                    (media.description ?: "null").replace("\\n", "<br>").replace("\\\"", "\""),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                binding.mediaInfoDescription.text =
                    "\t\t\t" + if (desc.toString() != "null") desc else "No Description Available"
                binding.mediaInfoDescription.setOnClickListener {
                    if (binding.mediaInfoDescription.maxLines == 5) {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 100)
                            .setDuration(950).start()
                    } else {
                        ObjectAnimator.ofInt(binding.mediaInfoDescription, "maxLines", 5)
                            .setDuration(400).start()
                    }
                }
                if(!media.relations.isNullOrEmpty()) {
                    binding.mediaInfoRelationText.visibility = View.VISIBLE
                    binding.mediaInfoRelationRecyclerView.visibility = View.VISIBLE
                    binding.mediaInfoRelationRecyclerView.adapter =
                        MediaAdaptor(0, media.relations!!, requireActivity())
                    binding.mediaInfoRelationRecyclerView.layoutManager =
                        LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    if(media.sequel!=null){
                        binding.mediaInfoQuelContainer.visibility=View.VISIBLE
                        binding.mediaInfoSequel.visibility=View.VISIBLE
                        binding.mediaInfoSequelImage.loadImage(media.sequel!!.banner?:media.sequel!!.cover)
                        binding.mediaInfoSequel.setSafeOnClickListener {
                            ContextCompat.startActivity(
                                requireContext(),
                                Intent(requireContext(), MediaDetailsActivity::class.java).putExtra(
                                    "media",
                                    media.sequel as Serializable
                                ),null
                            )
                        }
                    }
                    if(media.prequel!=null){
                        binding.mediaInfoQuelContainer.visibility=View.VISIBLE
                        binding.mediaInfoPrequel.visibility=View.VISIBLE
                        binding.mediaInfoPrequelImage.loadImage(media.prequel!!.banner?:media.prequel!!.cover)
                        binding.mediaInfoPrequel.setSafeOnClickListener {
                            ContextCompat.startActivity(
                                requireContext(),
                                Intent(requireContext(), MediaDetailsActivity::class.java).putExtra(
                                    "media",
                                    media.prequel as Serializable
                                ),null
                            )
                        }
                    }
                }
                if(media.genres.isNotEmpty()) {
                    binding.mediaInfoGenresText.visibility=View.VISIBLE
                    binding.mediaInfoGenresRecyclerView.visibility=View.VISIBLE
                    binding.mediaInfoGenresRecyclerView.adapter =
                        GenreAdapter(media.genres, type, requireActivity())
                    binding.mediaInfoGenresRecyclerView.layoutManager =
                        GridLayoutManager(requireContext(), (screenWidth / 156f).toInt())
                }
                if(!media.characters.isNullOrEmpty()) {
                    binding.mediaInfoCharacterText.visibility = View.VISIBLE
                    binding.mediaInfoCharacterRecyclerView.visibility = View.VISIBLE
                    binding.mediaInfoCharacterRecyclerView.adapter = CharacterAdapter(media.characters!!, requireActivity())
                    binding.mediaInfoCharacterRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                }
                if(!media.recommendations.isNullOrEmpty()) {
                    binding.mediaInfoRecommendedText.visibility = View.VISIBLE
                    binding.mediaInfoRecommendedRecyclerView.visibility = View.VISIBLE
                    binding.mediaInfoRecommendedRecyclerView.adapter = MediaAdaptor(0, media.recommendations!!, requireActivity())
                    binding.mediaInfoRecommendedRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                }

                if(media.tags.isNotEmpty()){
                    binding.mediaInfoTags.visibility = View.VISIBLE
                    binding.mediaInfoTagsText.visibility = View.VISIBLE
                    for (position in media.tags.indices) {
                        val chip = ItemChipBinding.inflate(LayoutInflater.from(context), binding.mediaInfoTags, false).root
                        chip.text = media.tags[position]
                        chip.setOnLongClickListener { copyToClipboard(media.tags[position]);true }
                        binding.mediaInfoTags.addView(chip)
                    }
                }

                if(media.synonyms.isNotEmpty()){
                    binding.mediaInfoSynonyms.visibility = View.VISIBLE
                    binding.mediaInfoSynonymsText.visibility = View.VISIBLE
                    for (position in media.synonyms.indices) {
                        val chip = ItemChipBinding.inflate(LayoutInflater.from(context), binding.mediaInfoSynonyms, false).root
                        chip.text = media.synonyms[position]
                        chip.setOnLongClickListener { copyToClipboard(media.synonyms[position]);true }
                        binding.mediaInfoSynonyms.addView(chip)
                    }
                }

                @Suppress("DEPRECATION")
                class MyChrome : WebChromeClient() {
                    private var mCustomView: View? = null
                    private var mCustomViewCallback: CustomViewCallback? = null
                    private var mOriginalSystemUiVisibility = 0

                    override fun onHideCustomView() {
                        (requireActivity().window.decorView as FrameLayout).removeView(mCustomView)
                        mCustomView = null
                        requireActivity().window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
                        mCustomViewCallback!!.onCustomViewHidden()
                        mCustomViewCallback = null
                    }

                    override fun onShowCustomView(paramView: View, paramCustomViewCallback: CustomViewCallback) {
                        if (mCustomView != null) {
                            onHideCustomView()
                            return
                        }
                        mCustomView = paramView
                        mOriginalSystemUiVisibility = requireActivity().window.decorView.systemUiVisibility
                        mCustomViewCallback = paramCustomViewCallback
                        (requireActivity().window.decorView as FrameLayout).addView(mCustomView, FrameLayout.LayoutParams(-1, -1))
                        requireActivity().window.decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    }
                }

                if(media.trailer!=null){
                    binding.mediaInfoTrailerText.visibility = View.VISIBLE
                    binding.mediaInfoTrailerContainer.visibility = View.VISIBLE
                    binding.mediaInfoTrailer.apply {
                        visibility = View.VISIBLE
                        settings.javaScriptEnabled = true
                        isSoundEffectsEnabled = true
                        webChromeClient = MyChrome()
                        loadUrl(media.trailer!!)
                    }
                }

                countDown(media,binding.mediaInfoContainer)
            }
        }
        super.onViewCreated(view, null)
    }

    override fun onResume() {
        binding.mediaInfoProgressBar.visibility = if (!loaded) View.VISIBLE else View.GONE
        super.onResume()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
