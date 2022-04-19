package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.TvGenreCardBinding
import ani.saikou.loadImage
import ani.saikou.px

class GenresPresenter(
    private val big:Boolean = false): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val binding = TvGenreCardBinding.inflate(LayoutInflater.from(parent?.context), parent, false)
        if (big) binding.genreCard.updateLayoutParams { height=72f.px }
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        val genre = item as Pair<String, String>
        val holder = viewHolder as GenreViewHolder
        val binding = holder.binding
        binding.genreTitle.text = genre.first
        binding.genreImage.loadImage(item.second)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class GenreViewHolder(val binding: TvGenreCardBinding) : Presenter.ViewHolder(binding.root) {}
}