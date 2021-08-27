package dev.vadzimv.pagination.example.pagination

import dev.vadzimv.pagination.example.PAGE_SIZE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class PagedList<T>(
    val nextOffset: Int?,
    val content: List<PagedListItem<T>>
) : List<PagedListItem<T>> by content {

    fun updateContent(update: (content: List<PagedListItem<T>>) -> List<PagedListItem<T>>): PagedList<T> {
        return PagedList(nextOffset, update(content))
    }

    companion object {
        fun <T> createFirstPage(page: Page<ListSpecificItem<T>>): PagedList<T> {
            return PagedList(
                page.nextOffset,
                page.items.map { it }
            )
        }


        fun <T : Any> PagedList<T>.userReached(
            index: Int,
            pageDataSource: PageDataSource<ListSpecificItem<T>>
        ): Flow<PagedList<T>> {
            val itemsBeforeEnd = this.size - index - 1
            if (itemsBeforeEnd > PAGE_SIZE / 2 || this.lastOrNull() !is ListSpecificItem) {
                return flowOf(this)
            }

            return loadNextPage(pageDataSource)
        }

        fun <T : Any> PagedList<T>.loadNextPage(
            pageDataSource: PageDataSource<ListSpecificItem<T>>
        ): Flow<PagedList<T>> {
            val initialState =
                if (this.last() is PageLoadingError) this.updateContent { it.dropLast(1) } else this
            if (initialState.nextOffset == null) return flowOf(this)
            return flow {
                emit(initialState.updateContent { it + listOf<PagedListItem<T>>(PageLoadingItem) })

                val nextPageLoadingResult = pageDataSource.getPage(
                    PageDataSource.GetPageRequest(
                        offset = initialState.nextOffset,
                        PAGE_SIZE
                    )
                )

                val toEmit: PagedList<T> = when (nextPageLoadingResult) {
                    PageDataSourceResult.Error -> {
                        initialState.updateContent {
                            it + listOf(
                                PageLoadingError
                            )
                        }
                    }
                    is PageDataSourceResult.Success -> {
                        val end = if (nextPageLoadingResult.page.nextOffset == null) listOf(
                            LastPageMarker
                        ) else listOf()
                        PagedList(
                            nextOffset = nextPageLoadingResult.page.nextOffset,
                            content = initialState.content + nextPageLoadingResult.page.items + end
                        )
                    }

                }
                emit(toEmit)
            }
        }
    }
}