package dev.vadzimv.pagination.example.pagination

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.vadzimv.pagination.example.R

abstract class BasePagedAdapter<T>(
    private val retry: () -> Unit,
    private val userReached: (index: Int) -> Unit
) : RecyclerView.Adapter<ListItemViewHolder>() {
    private val differ =
        AsyncListDiffer(this, object : DiffUtil.ItemCallback<PagedListItem<T>>() {
            override fun areItemsTheSame(
                oldItem: PagedListItem<T>,
                newItem: PagedListItem<T>
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: PagedListItem<T>,
                newItem: PagedListItem<T>
            ): Boolean {
                return oldItem == newItem
            }
        })

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            LastPageMarker -> 1
            PageLoadingError -> 2
            PageLoadingItem -> 3
            is ListSpecificItem -> getItemType(item.content)
        }
    }

    protected open fun getItemType(content: T): Int {
        return 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        return when (viewType) {
            1 -> LastPageMarkerViewHolder.create(parent)
            3 -> PageLoadingViewHolder.create(parent)
            2 -> LoadingErrorItemViewHolder.create(parent)
            else -> createCustomViewHolder(parent, viewType)
        }
    }

    protected abstract fun createCustomViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListItemViewHolder

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
            else -> bindCustomViewHolder(
                holder,
                (differ.currentList[position] as ListSpecificItem).content
            )
        }
    }

    protected abstract fun bindCustomViewHolder(holder: ListItemViewHolder, item: T)

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submit(pagedList: PagedList<T>) {
        differ.submitList(pagedList)
    }
}

abstract class ListItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

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