package dev.vadzimv.pagination.example

import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeDataSource(itemsCount: Int) : DataSource {

    private val content = (0 until itemsCount).map {
        Model(it)
    }

    override suspend fun getPage(request: DataSource.GetPageRequest): DataSourceResult {
        delay(1000)
        return if (Random.Default.nextBoolean()) {
            val page = content.getPage(request.offset, request.pageSize)
            DataSourceResult.Success(page)
        } else {
            DataSourceResult.Error
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