package dev.vadzimv.pagination.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ExampleViewModel(dataSource: DataSource) {

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> by lazy {
        GlobalScope.launch(Dispatchers.Unconfined) {
            val firstPage = dataSource.getPage()
            _state.value = State.Ready(PagedList(firstPage))
        }
        _state
    }

    sealed class State {
        object Loading : State()
        data class Ready(
            val pages: PagedList
        ) : State()
    }
}

interface DataSource {
    suspend fun getPage(): List<Model>
}

data class Model(val id: Int)

data class ViewObject(val id: Int)

class PagedList(val content: List<Model>) {
    operator fun get(index: Int): ViewObject {
        return ViewObject(content[index].id)
    }
}