package ani.saikou.manga.mangareader

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ani.saikou.BottomSheetDialogFragment
import ani.saikou.R
import ani.saikou.databinding.BottomSheetSelectorBinding
import ani.saikou.hideSystemBars
import ani.saikou.manga.MangaChapter
import ani.saikou.media.Media
import ani.saikou.media.MediaDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChapterLoaderDialog : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSelectorBinding? = null
    private val binding get() = _binding!!

    val model: MediaDetailsViewModel by activityViewModels()
    var chapter: MangaChapter? = null
    var media: Media? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var loaded = false
        binding.selectorAutoListContainer.visibility = View.VISIBLE
        binding.selectorListContainer.visibility = View.GONE

        binding.selectorTitle.text = getString(R.string.loading_next_chap)
        binding.selectorCancel.setOnClickListener {
            dismiss()
        }

        model.getMedia().observe(viewLifecycleOwner) { m ->
            media = m
            if (media != null && !loaded) {
                loaded = true
                binding.selectorAutoText.text = media!!.manga!!.selectedChapter
                lifecycleScope.launch(Dispatchers.IO) {
                    val chp = media!!.manga!!.chapters!![media!!.manga!!.selectedChapter]!!
                    if(model.loadMangaChapterImages(chp, media!!.selected!!, false)) chapter = chp
                    dismiss()
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.hideSystemBars()
        model.mangaChapter.postValue(chapter)
        super.onDismiss(dialog)
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    companion object {
        fun newInstance(prev: MangaChapter) = ChapterLoaderDialog().apply {
            arguments = bundleOf("prev" to prev)
        }
    }
}