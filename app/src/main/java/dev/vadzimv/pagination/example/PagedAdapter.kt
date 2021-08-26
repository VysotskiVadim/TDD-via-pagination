package dev.vadzimv.pagination.example

import android.media.browse.MediaBrowser
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

class PagedAdapter(
    private val retry: () -> Unit,
    private val userReached: (index: Int) -> Unit
) : RecyclerView.Adapter<ListItemViewHolder>() {

    private val differ =
        AsyncListDiffer(this, object : DiffUtil.ItemCallback<ListItem>() {
            override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
                return oldItem == newItem
            }
        })

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            LastPageMarker -> 1
            PageLoadingError -> 2
            PageLoadingItem -> 3
            is ViewObject -> 4
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        return when (viewType) {
            1 -> LastPageMarkerViewHolder.create(parent)
            3 -> PageLoadingViewHolder.create(parent)
            2 -> LoadingErrorItemViewHolder.create(parent)
            4 -> ViewObjectViewHolder.create(parent)
            else -> error("this should never happen")
        }
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        userReached(position)
        when (holder) {
            is LastPageMarkerViewHolder -> {
            }
            is LoadingErrorItemViewHolder -> {
                holder.retryButton.setOnClickListener { retry() }
            }
            is PageLoadingViewHolder -> {
            }
            is ViewObjectViewHolder -> {
                val model = differ.currentList[position] as ViewObject
                holder.description.text = model.id.toString()
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submit(pagedList: PagedList) {
        differ.submitList(pagedList)
    }
}

class LastPageMarkerViewHolder(view: View) : ListItemViewHolder(view) {
    companion object {
        fun create(parent: ViewGroup) =
            LastPageMarkerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_last_page, parent, false)
            )
    }
}

class PageLoadingViewHolder(view: View) : ListItemViewHolder(view) {
    companion object {
        fun create(parent: ViewGroup) =
            PageLoadingViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_page_loading, parent, false)
            )
    }
}

class LoadingErrorItemViewHolder(view: View) : ListItemViewHolder(view) {

    val retryButton = view.findViewById<Button>(R.id.retryButton)

    companion object {
        fun create(parent: ViewGroup) =
            LoadingErrorItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_page_loading_error, parent, false)
            )
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

sealed class ListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)