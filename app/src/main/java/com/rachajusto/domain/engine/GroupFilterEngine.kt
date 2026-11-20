package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Group
import java.text.Normalizer
import java.util.Locale

enum class GroupStatusFilter {
    ALL,
    PENDING,
    SETTLED
}

object GroupFilterEngine {

    fun filter(
        groups: List<Group>,
        query: String = "",
        statusFilter: GroupStatusFilter = GroupStatusFilter.ALL
    ): List<Group> {
        val normalizedQuery = normalize(query)
        return groups.filter { group ->
            val matchesQuery = normalizedQuery.isEmpty() ||
                normalize(group.name).contains(normalizedQuery) ||
                normalize(group.description.orEmpty()).contains(normalizedQuery) ||
                group.participants.any { normalize(it.name).contains(normalizedQuery) }

            if (!matchesQuery) return@filter false

            when (statusFilter) {
                GroupStatusFilter.ALL -> true
                GroupStatusFilter.PENDING -> {
                    val summary = CashFlowMinimizer.calculateSummary(group)
                    summary.optimizedTransfers.isNotEmpty()
                }
                GroupStatusFilter.SETTLED -> {
                    val summary = CashFlowMinimizer.calculateSummary(group)
                    group.expenses.isNotEmpty() && summary.optimizedTransfers.isEmpty()
                }
            }
        }
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
    }
}
