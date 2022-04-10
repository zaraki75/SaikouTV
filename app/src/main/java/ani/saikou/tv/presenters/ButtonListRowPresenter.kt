package ani.saikou.tv.presenters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.RowHeaderPresenter
import ani.saikou.databinding.TvHeaderButtonBinding
import ani.saikou.tv.components.ButtonListRow

class ButtonListRowPresenter(): Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        return ButtonListRowViewHolder(TvHeaderButtonBinding.inflate(LayoutInflater.from(parent?.context), parent, false))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        (viewHolder as ButtonListRowViewHolder)?.let {
            it.binding.title.text = (item as ButtonListRow).text
            it.binding.root.setOnClickListener {
                item.click()
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

    inner class ButtonListRowViewHolder(val binding: TvHeaderButtonBinding) : RowHeaderPresenter.ViewHolder(binding.root) {}
}