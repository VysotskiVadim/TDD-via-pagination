package dev.vadzimv.pagination.example

import dev.vadzimv.pagination.example.pagination.PageDataSourceResult
import dev.vadzimv.pagination.example.pagination.PageDataSource
import dev.vadzimv.pagination.example.pagination.Page
import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeDataSource(itemsCount: Int) : PageDataSource<Model> {

    private val content = (0 until itemsCount).map {
        Model(it)
    }

    override suspend fun getPage(request: PageDataSource.GetPageRequest): PageDataSourceResult<Model> {
        delay(1000)
        return if (Random.Default.nextBoolean()) {
            val page = content.getPage(request.offset, request.pageSize)
            PageDataSourceResult.Success(page)
        } else {
            PageDataSourceResult.Error
        }
    }

    private fun <T> List<T>.getPage(offset: Int, size: Int): Page<T> {
        val nextOffset = offset + size
        val offsetToReturn = if (nextOffset >= this.size) {
            null
        } else nextOffset
        return Page(this.drop(offset).take(size), offsetToReturn)
    }
}