package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Action
import androidx.leanback.widget.Presenter
import ani.saikou.databinding.TvDetailActionBinding
import ani.saikou.tv.components.ButtonListRow

class DetailActionsPresenter(): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return DetailActionsViewHolder(TvDetailActionBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder as DetailActionsViewHolder)?.let {
            it.binding.title.text = (item as Action).label1.toString().uppercase()
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class DetailActionsViewHolder(val binding: TvDetailActionBinding) : Presenter.ViewHolder(binding.root) {}
}