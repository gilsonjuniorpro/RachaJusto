package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Expense
import java.text.Normalizer
import java.util.Locale

enum class ExpenseSortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC,
    TITLE_ASC
}

object ExpenseSorter {

    fun filterAndSort(
        expenses: List<Expense>,
        query: String = "",
        sortOption: ExpenseSortOption = ExpenseSortOption.DATE_DESC
    ): List<Expense> {
        val normalizedQuery = normalizeText(query)
        val filtered = if (normalizedQuery.isEmpty()) {
            expenses
        } else {
            expenses.filter { expense ->
                normalizeText(expense.title).contains(normalizedQuery) ||
                    expense.items.any { normalizeText(it.name).contains(normalizedQuery) }
            }
        }

        return when (sortOption) {
            ExpenseSortOption.DATE_DESC -> filtered.sortedWith(
                compareByDescending<Expense> { it.date }.thenByDescending { it.createdAtIso }
            )

            ExpenseSortOption.DATE_ASC -> filtered.sortedWith(
                compareBy<Expense> { it.date }.thenBy { it.createdAtIso }
            )

            ExpenseSortOption.AMOUNT_DESC -> filtered.sortedWith(
                compareByDescending<Expense> { it.totalAmountCents }.thenByDescending { it.date }
            )

            ExpenseSortOption.AMOUNT_ASC -> filtered.sortedWith(
                compareBy<Expense> { it.totalAmountCents }.thenByDescending { it.date }
            )

            ExpenseSortOption.TITLE_ASC -> filtered.sortedBy { normalizeText(it.title) }
        }
    }

    private fun normalizeText(value: String): String {
        val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
    }
}
