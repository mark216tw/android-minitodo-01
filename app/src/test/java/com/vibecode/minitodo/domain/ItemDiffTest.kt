package com.vibecode.minitodo.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemDiffTest {

    private data class Item(val id: Long)

    private fun diff(existingIds: Set<Long>, incomingIds: List<Long>): ItemDiff<Item> =
        diffItems(existingIds, incomingIds.map(::Item)) { it.id }

    @Test
    fun `new items are inserted`() {
        val result = diff(emptySet(), listOf(0L, 0L))

        assertEquals(listOf(0L, 0L), result.toInsert.map { it.id })
        assertEquals(emptyList<Item>(), result.toUpdate)
        assertEquals(emptyList<Long>(), result.toDeleteIds)
    }

    @Test
    fun `removed items are deleted and kept items are updated`() {
        val result = diff(setOf(1L, 2L), listOf(2L, 0L))

        assertEquals(listOf(0L), result.toInsert.map { it.id })
        assertEquals(listOf(2L), result.toUpdate.map { it.id })
        assertEquals(listOf(1L), result.toDeleteIds)
    }

    @Test
    fun `unchanged items are not recreated`() {
        val result = diff(setOf(1L, 2L), listOf(1L, 2L))

        assertEquals(emptyList<Item>(), result.toInsert)
        assertEquals(listOf(1L, 2L), result.toUpdate.map { it.id })
        assertEquals(emptyList<Long>(), result.toDeleteIds)
    }

    @Test
    fun `unknown ids are treated as inserts`() {
        val result = diff(setOf(1L), listOf(99L))

        assertEquals(listOf(99L), result.toInsert.map { it.id })
        assertEquals(emptyList<Item>(), result.toUpdate)
        assertEquals(listOf(1L), result.toDeleteIds)
    }

    @Test
    fun `reordering keeps all existing items`() {
        val result = diff(setOf(1L, 2L, 3L), listOf(3L, 1L, 2L))

        assertEquals(emptyList<Item>(), result.toInsert)
        assertEquals(listOf(3L, 1L, 2L), result.toUpdate.map { it.id })
        assertEquals(emptyList<Long>(), result.toDeleteIds)
    }
}
