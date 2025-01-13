/*
 * Copyright (C) 2024, Scrolless
 * All rights reserved.
 */
package com.scrolless.framework.extensions

/**
 * Adds items to this mutable list if they don't already exist based on a unique identifier.
 * @param newItems The items to add to the list.
 * @param idSelector A function that extracts the identifier from an item.
 */
fun <T, ID> MutableList<T>.addUniqueItemsById(
    newItems: List<T>,
    idSelector: (T) -> ID
) {
    newItems.forEach { newItem ->
        val newItemId = idSelector(newItem)
        val exists = this.any { existingItem -> idSelector(existingItem) == newItemId }
        if (!exists) {
            this.add(newItem)
        }
    }
}

fun <T, ID> MutableList<T>.addOrReplaceItemsById(
    newItems: List<T>,
    idSelector: (T) -> ID
) {
    newItems.forEach { newItem ->
        val newItemId = idSelector(newItem)
        // Find the index of the existing item with the same ID
        val index = this.indexOfFirst { existingItem -> idSelector(existingItem) == newItemId }
        if (index != -1) {
            // Replace the existing item if found
            this[index] = newItem
        } else {
            // Add the new item if not found
            this.add(newItem)
        }
    }
}
