package dev.vadzimv.pagination.example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.vadzimv.pagination.example.pagination.BasePagedAdapter
import dev.vadzimv.pagination.example.pagination.ListItemViewHolder

class ExamplePagedAdapter(
    retry: () -> Unit,
    userReached: (index: Int) -> Unit
) : BasePagedAdapter<ViewObject>(retry, userReached) {

    override fun bindCustomViewHolder(holder: ListItemViewHolder, item: ViewObject) {
        val viewObjectViewHolder = holder as ViewObjectViewHolder
        viewObjectViewHolder.description.text = item.id.toString()
    }

    override fun createCustomViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        return ViewObjectViewHolder.create(parent)
    }

}

class ViewObjectViewHolder(view: View) : ListItemViewHolder(view) {

    val description = view.findViewById<TextView>(R.id.objectDescription)

    companion object {
        fun create(parent: ViewGroup) =
            ViewObjectViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_object, parent, false)
            )
    }
}

