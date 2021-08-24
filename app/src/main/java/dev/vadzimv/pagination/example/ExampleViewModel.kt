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
            is DataSourceResult.Success -> State.Ready(PagedList(firstPageLoadingResult.page.items))
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

data class ViewObject(val id: Int)

class PagedList(val content: List<Model>) {
    operator fun get(index: Int): ViewObject {
        return ViewObject(content[index].id)
    }
}