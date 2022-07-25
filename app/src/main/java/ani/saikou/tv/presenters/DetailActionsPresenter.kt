package ani.saikou.tv.presenters

import android.graphics.Color
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
        if(item is SourceAction) {
            (viewHolder as DetailActionsViewHolder)?.let {
                it.binding.title.text = (item as Action).label1.toString().uppercase()
            }
        } else if(item is ChangeAction) {
            (viewHolder as DetailActionsViewHolder)?.let {
                it.binding.title.text = (item as Action).label1.toString().uppercase()
                //it.binding.background.setBackgroundColor(Color.BLACK)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class DetailActionsViewHolder(val binding: TvDetailActionBinding) : Presenter.ViewHolder(binding.root) {}
}

class SourceAction(id: Long, label: String): Action(id, label) {}
class ChangeAction(id: Long, label: String): Action(id, label) {}