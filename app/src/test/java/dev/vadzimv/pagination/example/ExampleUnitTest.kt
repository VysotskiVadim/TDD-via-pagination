package dev.vadzimv.pagination.example

import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleUnitTest {

    val fakeDataSource = FakeDataSource(52)
    val loadingProxy = DataSourceLoadingProxy(fakeDataSource)
    val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
    val exampleViewModel = ExampleViewModel(scope, loadingProxy)


    @Test
    fun `first page`() {
        loadingProxy.stopLoading()
        val loadingState = exampleViewModel.state.value
        assertTrue(loadingState is ExampleViewModel.State.FirstPageLoading)
        loadingProxy.resumeLoading()

        val state = exampleViewModel.state.value
        assertTrue(state is ExampleViewModel.State.Ready)
        val viewObject = state.pages[0]
        assertEquals(0, viewObject.id)
    }

    @Test
    fun `retry error`() {
        fakeDataSource.setLoadingError()
        val state = exampleViewModel.state.value
        assertTrue(state is ExampleViewModel.State.FirstPageLoadingError, "state is $state")

        fakeDataSource.setCorrectResult()
        loadingProxy.stopLoading()
        exampleViewModel.retryFirstPageLoading()
        val stateWhenRetryInProgress = exampleViewModel.state.value
        assertTrue(
            stateWhenRetryInProgress is ExampleViewModel.State.FirstPageLoading,
            "state is $stateWhenRetryInProgress"
        )
        loadingProxy.resumeLoading()
        val stateAfterRetry = exampleViewModel.state.value
        assertTrue(stateAfterRetry is ExampleViewModel.State.Ready, "state is $stateAfterRetry")
    }

    @Test
    fun `cancel first page loading`() {
        loadingProxy.stopLoading()
        exampleViewModel.state.value
        scope.cancel() // user leave screen
        loadingProxy.resumeLoading()
        val state = exampleViewModel.state.value
        assertTrue(state is ExampleViewModel.State.FirstPageLoading, "state is $state")
    }

    @Test
    fun `load second page`() {
        val firstPageLoaded = exampleViewModel.state.value
        assertTrue(firstPageLoaded is ExampleViewModel.State.Ready)
        firstPageLoaded.pages
    }
}

class FakeDataSource(itemsCount: Int) : DataSource {

    private var error = false

    private val content = (0 until itemsCount).map {
        Model(it)
    }

    override suspend fun getPage(request: DataSource.GetPageRequest): DataSourceResult {
        if (error) {
            return DataSourceResult.Error
        }
        val page = content.getPage(request.offset, request.pageSize)
        return DataSourceResult.Success(page)
    }

    fun setLoadingError() {
        error = true
    }

    fun setCorrectResult() {
        error = false
    }
}

class DataSourceLoadingProxy(private val wrapped: DataSource) : DataSource {

    private var waitHandle: CompletableDeferred<Unit>? = null

    fun stopLoading() {
        waitHandle = CompletableDeferred()
    }

    override suspend fun getPage(request: DataSource.GetPageRequest): DataSourceResult {
        waitHandle?.await()
        return wrapped.getPage(request)
    }

    fun resumeLoading() {
        waitHandle?.complete(Unit)
    }
}