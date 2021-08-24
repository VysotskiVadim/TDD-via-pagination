package dev.vadzimv.pagination.example

fun <T> List<T>.getPage(offset: Int, size: Int): Page<T> {
    val nextOffset = offset + size
    val offsetToReturn = if (nextOffset >= this.size) {
        null
    } else nextOffset
    return Page(this.drop(offset).take(size), offsetToReturn)
}