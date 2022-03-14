package `in`.windrunner.android_toolbox.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BindableHolder<T : FilterableItem>(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(item: T)
}