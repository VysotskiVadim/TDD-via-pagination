package dev.vadzimv.pagination.example.pagination

sealed interface PagedListItem<in T>

data class ListSpecificItem<T>(val content: T) : PagedListItem<T>
object PageLoadingItem : PagedListItem<Any>
object PageLoadingError : PagedListItem<Any>
object LastPageMarker : PagedListItem<Any>