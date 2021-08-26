package dev.vadzimv.pagination.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val PAGE_SIZE = 25

class ExampleViewModel(
    private val screenScope: CoroutineScope,
    private val dataSource: DataSource
) {
    private val _state = MutableStateFlow<State>(State.FirstPageLoading)
    val state: StateFlow<State> by lazy {
        screenScope.launch {
            loadFirstPage()
        }
        _state
    }

    private suspend fun loadFirstPage() {
        _state.value = State.FirstPageLoading
        val firstPageLoadingResult = dataSource.getPage(DataSource.GetPageRequest(0, PAGE_SIZE))
        _state.value = when (firstPageLoadingResult) {
            DataSourceResult.Error -> State.FirstPageLoadingError
            is DataSourceResult.Success -> State.Ready(
                PagedList(
                    firstPageLoadingResult.page.nextOffset,
                    firstPageLoadingResult.page.items.map()
                )
            )
        }
    }

    sealed class State {
        object FirstPageLoading : State()
        data class Ready(
            val pages: PagedList
        ) : State()

        object FirstPageLoadingError : State()
    }

    fun retryFirstPageLoading() {
        screenScope.launch {
            loadFirstPage()
        }
    }

    fun userReached(index: Int) {
        val initialState = state.value as State.Ready
        val existingPage = initialState.pages
        val itemsBeforeEnd = existingPage.size - index - 1
        if (existingPage.nextOffset == null || itemsBeforeEnd > PAGE_SIZE / 2 || existingPage.lastOrNull() !is ViewObject) {
            return
        }
        screenScope.launch {
            loadNextPage(initialState, existingPage.nextOffset)
        }
    }

    private suspend fun loadNextPage(
        initialState: State.Ready,
        nextPageOffset: Int
    ) {
        _state.value =
            initialState.copy(
                pages = PagedList(
                    null,
                    initialState.pages.content + listOf(
                        PageLoadingItem
                    )
                )
            )
        val secondPageLoadingResult =
            dataSource.getPage(
                DataSource.GetPageRequest(
                    offset = nextPageOffset,
                    PAGE_SIZE
                )
            )
        _state.value = when (secondPageLoadingResult) {
            DataSourceResult.Error -> {
                initialState.copy(pages = initialState.pages.updateContent {
                    it + listOf(
                        PageLoadingError
                    )
                })
            }
            is DataSourceResult.Success -> {
                val end = if (secondPageLoadingResult.page.nextOffset == null) listOf(
                    LastPageMarker
                ) else listOf()
                State.Ready(
                    PagedList(
                        nextOffset = secondPageLoadingResult.page.nextOffset,
                        content = initialState.pages.content + secondPageLoadingResult.page.items.map() + end
                    )
                )
            }
        }
    }

    private fun List<Model>.map() = this.map { map(it) }

    private fun map(model: Model): ViewObject {
        return ViewObject(model.id)
    }

    fun retryNextPageLoading() {
        screenScope.launch {
            val currentState = _state.value
            if (currentState is State.Ready && currentState.pages.nextOffset != null) {
                val stateWithoutError =
                    currentState.copy(currentState.pages.updateContent { it.dropLast(1) })
                loadNextPage(stateWithoutError, currentState.pages.nextOffset)
            }
        }
    }
}

interface DataSource {
    suspend fun getPage(request: GetPageRequest): DataSourceResult

    data class GetPageRequest(
        val offset: Int,
        val pageSize: Int
    )
}

sealed class DataSourceResult {
    data class Success(
        val page: Page<Model>
    ) : DataSourceResult()

    object Error : DataSourceResult()
}

data class Page<T>(
    val items: List<T>,
    val nextOffset: Int?
)

data class Model(val id: Int)

interface ListItem
data class ViewObject(val id: Int) : ListItem
object PageLoadingItem : ListItem
object PageLoadingError : ListItem
object LastPageMarker : ListItem


class PagedList(
    val nextOffset: Int?,
    val content: List<ListItem>
) : List<ListItem> by content {

    fun updateContent(update: (content: List<ListItem>) -> List<ListItem>): PagedList {
        return PagedList(nextOffset, update(content))
    }
}