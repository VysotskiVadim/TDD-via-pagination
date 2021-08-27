package dev.vadzimv.pagination.example

import dev.vadzimv.pagination.example.pagination.ListSpecificItem
import dev.vadzimv.pagination.example.pagination.PageDataSource
import dev.vadzimv.pagination.example.pagination.PageDataSourceResult
import dev.vadzimv.pagination.example.pagination.PagedList
import dev.vadzimv.pagination.example.pagination.PagedList.Companion.loadNextPage
import dev.vadzimv.pagination.example.pagination.PagedList.Companion.userReached
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

const val PAGE_SIZE = 25

class ExampleViewModel(
    private val screenScope: CoroutineScope,
    private val pageDataSource: PageDataSource<Model>
) {
    private val mappedDataSource = object : PageDataSource<ListSpecificItem<ViewObject>> {
        override suspend fun getPage(request: PageDataSource.GetPageRequest): PageDataSourceResult<ListSpecificItem<ViewObject>> {
            return when (val pageResult = pageDataSource.getPage(request)) {
                PageDataSourceResult.Error -> PageDataSourceResult.Error
                is PageDataSourceResult.Success -> PageDataSourceResult.Success(
                    pageResult.page.map<Model, ListSpecificItem<ViewObject>>(
                        ::map
                    )
                )
            }
        }

    }

    private val _state = MutableStateFlow<State>(State.FirstPageLoading)
    val state: StateFlow<State> by lazy {
        screenScope.launch {
            loadFirstPage()
        }
        _state
    }


    sealed class State {
        object FirstPageLoading : State()
        data class Ready(
            val pages: PagedList<ViewObject>
        ) : State()

        object FirstPageLoadingError : State()
    }

    private suspend fun loadFirstPage() {
        _state.value = State.FirstPageLoading
        val firstPageLoadingResult =
            pageDataSource.getPage(PageDataSource.GetPageRequest(0, PAGE_SIZE))
        _state.value = when (firstPageLoadingResult) {
            PageDataSourceResult.Error -> State.FirstPageLoadingError
            is PageDataSourceResult.Success -> State.Ready(
                PagedList.createFirstPage(
                    firstPageLoadingResult.page.map<Model, ListSpecificItem<ViewObject>> {
                        map(it)
                    })
            )
        }
    }

    fun retryFirstPageLoading() {
        screenScope.launch {
            loadFirstPage()
        }
    }

    fun userReached(index: Int) {
        val initialState = state.value as State.Ready
        screenScope.launch {
            initialState.pages.userReached(index, mappedDataSource).collect {
                _state.value = initialState.copy(pages = it)
            }
        }
    }

    private fun map(model: Model) = ListSpecificItem(ViewObject(model.id))

    fun retryNextPageLoading() {
        val initialState = state.value as State.Ready
        screenScope.launch {
            initialState.pages.loadNextPage(mappedDataSource).collect {
                _state.value = initialState.copy(pages = it)
            }
        }
    }
}

data class ViewObject(val id: Int)

data class Model(val id: Int)