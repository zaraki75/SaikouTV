package ani.saikou.tv.presenters

import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.TvAnimeDetailBinding
import ani.saikou.media.Media

class DetailsDescriptionPresenter: Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return AnimeDetailViewHolder(TvAnimeDetailBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder as AnimeDetailViewHolder)?.let { vh ->
            (item as? Media)?.let {
                vh.binding.title.text = it.getMainName()
                vh.binding.altTitle.text = it.nameRomaji
                vh.binding.status.text = it.status
                val desc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(it.description?: "", Html.FROM_HTML_MODE_COMPACT)
                } else {
                    Html.fromHtml(it.description?:"")
                }
                vh.binding.overview.text = desc
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class AnimeDetailViewHolder(val binding: TvAnimeDetailBinding) : Presenter.ViewHolder(binding.root) {}
}