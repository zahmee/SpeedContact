package com.speedcontact.app.domain

import com.speedcontact.app.model.SortOrder
import java.text.Collator

object ContactSorter {
    /** Items named in [order] come first in that order; any others keep their place after them. */
    fun <T> applyCustomOrder(items: List<T>, order: List<String>, key: (T) -> String): List<T> {
        if (order.isEmpty()) return items
        val positions = order.withIndex().associate { (index, value) -> value to index }
        return items.sortedBy { positions[key(it)] ?: Int.MAX_VALUE }
    }

    /** [items] must already be in the user's custom order. */
    fun <T> sort(
        items: List<T>,
        order: SortOrder,
        key: (T) -> String,
        name: (T) -> String,
        lastCalled: Map<String, Long>,
        collator: Collator = Collator.getInstance(),
    ): List<T> {
        val byName = compareBy(collator, name)
        return when (order) {
            SortOrder.CUSTOM -> items
            SortOrder.ALPHABETICAL -> items.sortedWith(byName)
            SortOrder.RECENT -> items.sortedWith(
                compareByDescending<T> { lastCalled[key(it)] ?: 0L }.then(byName),
            )
        }
    }
}
