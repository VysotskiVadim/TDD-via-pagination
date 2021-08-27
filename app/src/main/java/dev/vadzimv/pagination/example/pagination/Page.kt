package dev.vadzimv.pagination.example.pagination

data class Page<T>(
    val items: List<T>,
    val nextOffset: Int?
) {
    fun <I, O> map(mapper: (T) -> (O)): Page<O> {
        return Page(items.map(mapper), nextOffset)
    }
}