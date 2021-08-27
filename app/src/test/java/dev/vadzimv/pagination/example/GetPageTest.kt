package dev.vadzimv.pagination.example

import dev.vadzimv.pagination.example.pagination.Page
import kotlin.test.Test
import kotlin.test.assertEquals

class GetPageTest {
    @Test
    fun `get first page`() {
        val page = (0..10).toList().getPage(0, 3)
        assertEquals(
            Page(listOf(0, 1, 2), 3),
            page
        )
    }

    @Test
    fun `get some page`() {
        val page = (0..10).toList().getPage(3, 2)
        assertEquals(
            Page(listOf(3, 4), 5),
            page
        )
    }

    @Test
    fun `get last page`() {
        val page = (0..10).toList().getPage(9, 3)
        assertEquals(
            Page(listOf(9, 10), null),
            page
        )
    }
}