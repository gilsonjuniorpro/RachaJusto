package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Expense
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

object ExpenseDuplicator {

    fun duplicateForDate(
        source: Expense,
        targetDate: LocalDate = LocalDate.now(),
        createdAtIso: String = Instant.now().toString()
    ): Expense {
        val newExpenseId = "exp_${UUID.randomUUID().toString().take(8)}"
        val clonedItems = source.items.mapIndexed { idx, item ->
            item.copy(id = "item_${UUID.randomUUID().toString().take(6)}_$idx")
        }
        return source.copy(
            id = newExpenseId,
            date = targetDate,
            items = clonedItems,
            createdAtIso = createdAtIso
        )
    }
}
