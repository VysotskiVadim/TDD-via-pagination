package dev.vadzimv.pagination.example

import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleUnitTest {

    val fakeDataSource = FakeDataSource()
    val loadingProxy = DataSourceLoadingProxy(fakeDataSource)
    val exampleViewModel = ExampleViewModel(loadingProxy)

    @Test
    fun `first page`() {
        loadingProxy.stopLoading()
        val loadingState = exampleViewModel.state.value
        assertTrue(loadingState is ExampleViewModel.State.Loading)
        loadingProxy.resumeLoading()

        val state = exampleViewModel.state.value
        assertTrue(state is ExampleViewModel.State.Ready)
        val viewObject = state.pages[0]
        assertEquals(1, viewObject.id)
    }
}

class FakeDataSource : DataSource {
    override suspend fun getPage(): List<Model> {
        return listOf(1, 2, 3).map { Model(it) }
    }
}

class DataSourceLoadingProxy(private val wrapped: DataSource) : DataSource {

    private var waitHandle: CompletableDeferred<Unit>? = null

    fun stopLoading() {
        waitHandle = CompletableDeferred()
    }

    override suspend fun getPage(): List<Model> {
        waitHandle?.await()
        return wrapped.getPage()
    }

    fun resumeLoading() {
        waitHandle?.complete(Unit)
    }
}