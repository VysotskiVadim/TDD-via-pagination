package dev.vadzimv.pagination.example

import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExampleUnitTest {

    private val availableItems = PAGE_SIZE * 3 + PAGE_SIZE / 2
    val fakeDataSource = FakeDataSource(availableItems)
    val loadingProxy = DataSourceLoadingProxy(fakeDataSource)
    val scope = CoroutineScope(Job() + Dispatchers.Unconfined)
    val exampleViewModel = ExampleViewModel(scope, loadingProxy)


    @Test
    fun `first page`() {
        loadingProxy.stopLoading()
        exampleViewModel.loadFirstPage()
        val loadingState = exampleViewModel.state.value
        assertTrue(loadingState is ExampleViewModel.State.FirstPageLoading)
        loadingProxy.resumeLoading()

        val state = exampleViewModel.state.value
        assertTrue(state is ExampleViewModel.State.Ready)
        assertEquals(PAGE_SIZE, state.pages.size)
        val viewObject = state.pages[0] as ViewObject
        assertEquals(0, viewObject.id)
    }

    @Test
    fun `retry error`() {
        fakeDataSource.setLoadingError()
        exampleViewModel.loadFirstPage()
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
        exampleViewModel.loadFirstPage()
        scope.cancel() // user leave screen
        loadingProxy.resumeLoading()
        val state = exampleViewModel.state.value
        assertTrue(state is ExampleViewModel.State.FirstPageLoading, "state is $state")
    }

    @Test
    fun `load second page`() {
        exampleViewModel.loadFirstPage()
        val firstPageLoaded = exampleViewModel.assertReady()
        loadingProxy.stopLoading()
        exampleViewModel.scrollToTheNextPage()

        exampleViewModel.assertLastItemIsLoading()
        loadingProxy.resumeLoading()

        exampleViewModel.assertSecondPageLoaded(firstPageLoaded)
    }

    @Test
    fun `second page loading error with retry`() {
        exampleViewModel.loadFirstPage()
        val firstPageLoaded = exampleViewModel.assertReady()
        fakeDataSource.setLoadingError()

        exampleViewModel.scrollToTheNextPage()

        val errorState = exampleViewModel.assertReady()
        assertEquals(PageLoadingError, errorState.pages.last())

        fakeDataSource.setCorrectResult()
        loadingProxy.stopLoading()
        exampleViewModel.retryNextPageLoading()
        exampleViewModel.assertLastItemIsLoading()
        loadingProxy.resumeLoading()
        exampleViewModel.assertSecondPageLoaded(firstPageLoaded)
    }


    @Test
    fun `load third page`() {
        with(exampleViewModel) {
            loadFirstPage()
            scrollToTheNextPage()
            scrollToTheNextPage()
        }
        val items = exampleViewModel.assertReady().pages.map { (it as? ViewObject)?.id }
        assertEquals(
            (0 until (PAGE_SIZE * 3)).toList(),
            items
        )
    }

    @Test
    fun `load last page`() {
        with(exampleViewModel) {
            loadFirstPage()
            scrollToTheNextPage()
            scrollToTheNextPage()
            scrollToTheNextPage()
        }
        val fourPagesLoaded = exampleViewModel.assertReady()
        val items = fourPagesLoaded.pages.map { (it as? ViewObject)?.id }
        assertEquals(
            (0 until availableItems).toList() + listOf(null),
            items
        )
        assertTrue(fourPagesLoaded.pages.last() is LastPageMarker)
    }
}

private fun ExampleViewModel.loadFirstPage() {
    state.value // trigger lazy loading
}

private fun ExampleViewModel.assertSecondPageLoaded(firstPageLoaded: ExampleViewModel.State.Ready) {
    val secondPageLoaded = assertReady()
    val itemFromSecondPage =
        secondPageLoaded.pages[firstPageLoaded.pages.size + 1] as ViewObject
    assertEquals(PAGE_SIZE + 1, itemFromSecondPage.id)
    assertEquals(PAGE_SIZE * 2, secondPageLoaded.pages.size)
}

private fun ExampleViewModel.assertLastItemIsLoading() {
    val nextPageLoadingState = this.assertReady()
    val lastItem = nextPageLoadingState.pages[nextPageLoadingState.pages.size - 1]
    assertTrue(lastItem is PageLoadingItem, "last item isn't loading: $lastItem")
}

private fun ExampleViewModel.assertReady(): ExampleViewModel.State.Ready {
    val state = this.state.value
    assertTrue(
        state is ExampleViewModel.State.Ready,
        "state is expected to be Ready, but it $state"
    )
    return state
}

private fun ExampleViewModel.scrollToTheNextPage() {
    val somethingIsLoaded = assertReady()
    val currentPage = somethingIsLoaded.pages
    for (i in 0 until currentPage.size) {
        currentPage[i]
        this.userReached(i)
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