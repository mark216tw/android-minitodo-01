package com.vibecode.minitodo.domain

data class ItemDiff<T>(
    val toInsert: List<T>,
    val toUpdate: List<T>,
    val toDeleteIds: List<Long>,
)

fun <T> diffItems(
    existingIds: Set<Long>,
    incoming: List<T>,
    idOf: (T) -> Long,
): ItemDiff<T> {
    val toInsert = incoming.filter { item ->
        val id = idOf(item)
        id == 0L || id !in existingIds
    }
    val toUpdate = incoming.filter { item ->
        val id = idOf(item)
        id != 0L && id in existingIds
    }
    val keptIds = toUpdate.map(idOf).toSet()
    val toDeleteIds = existingIds.filterNot { it in keptIds }
    return ItemDiff(
        toInsert = toInsert,
        toUpdate = toUpdate,
        toDeleteIds = toDeleteIds,
    )
}
