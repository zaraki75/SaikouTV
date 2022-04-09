package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.RowPresenter
import ani.saikou.databinding.TvHeaderRowBinding

class HeaderRowPresenter: RowPresenter() {
    override fun createRowViewHolder(parent: ViewGroup?): ViewHolder {
        return HeaderRowViewholder(TvHeaderRowBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun isUsingDefaultSelectEffect(): Boolean {
        return false
    }

    inner class HeaderRowViewholder(val binding: TvHeaderRowBinding) : ViewHolder(binding.root) {
        init {
            binding.title.text = "No episodes found, try another source"
        }
    }
}