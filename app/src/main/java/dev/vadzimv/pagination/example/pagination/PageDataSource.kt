package dev.vadzimv.pagination.example.pagination

interface PageDataSource<T> {
    suspend fun getPage(request: GetPageRequest): PageDataSourceResult<T>

    data class GetPageRequest(
        val offset: Int,
        val pageSize: Int
    )
}

sealed class PageDataSourceResult<in T> {
    data class Success<T>(
        val page: Page<T>
    ) : PageDataSourceResult<T>()

    object Error : PageDataSourceResult<Any>()
}