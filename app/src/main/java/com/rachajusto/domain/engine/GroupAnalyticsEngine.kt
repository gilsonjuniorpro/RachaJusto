package com.rachajusto.domain.engine

import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.Group
import java.time.LocalDate

data class CategorySpending(
    val category: ExpenseCategory,
    val totalCents: Long,
    val expenseCount: Int,
    val percentage: Float
)

data class DailySpending(
    val date: LocalDate,
    val totalCents: Long,
    val expenseCount: Int
)

data class GroupAnalytics(
    val totalSpentCents: Long,
    val averageExpenseCents: Long,
    val largestExpenseTitle: String?,
    val largestExpenseCents: Long,
    val topPayerParticipantId: String?,
    val topPayerTotalCents: Long,
    val categoryBreakdown: List<CategorySpending>,
    val dailySpending: List<DailySpending>
)

object GroupAnalyticsEngine {

    fun analyze(group: Group): GroupAnalytics {
        val expenses = group.expenses
        if (expenses.isEmpty()) {
            return GroupAnalytics(
                totalSpentCents = 0L,
                averageExpenseCents = 0L,
                largestExpenseTitle = null,
                largestExpenseCents = 0L,
                topPayerParticipantId = null,
                topPayerTotalCents = 0L,
                categoryBreakdown = emptyList(),
                dailySpending = emptyList()
            )
        }

        val totalSpent = expenses.sumOf { it.totalAmountCents }
        val avgExpense = totalSpent / expenses.size.toLong()
        val largest = expenses.maxBy { it.totalAmountCents }

        val paidByTotals = expenses.groupBy { it.paidById }
            .mapValues { (_, list) -> list.sumOf { it.totalAmountCents } }
        val topPayerEntry = paidByTotals.entries.maxWithOrNull(
            compareBy<Map.Entry<String, Long>> { it.value }.thenByDescending { it.key }
        )

        val categoryBreakdown = expenses
            .groupBy { it.category }
            .map { (category, list) ->
                val catTotal = list.sumOf { it.totalAmountCents }
                val pct = if (totalSpent > 0L) {
                    (catTotal.toDouble() / totalSpent.toDouble() * 100.0).toFloat()
                } else {
                    0f
                }
                CategorySpending(
                    category = category,
                    totalCents = catTotal,
                    expenseCount = list.size,
                    percentage = pct
                )
            }
            .sortedByDescending { it.totalCents }

        val dailySpending = expenses
            .groupBy { it.date }
            .map { (date, list) ->
                DailySpending(
                    date = date,
                    totalCents = list.sumOf { it.totalAmountCents },
                    expenseCount = list.size
                )
            }
            .sortedByDescending { it.date }

        return GroupAnalytics(
            totalSpentCents = totalSpent,
            averageExpenseCents = avgExpense,
            largestExpenseTitle = largest.title,
            largestExpenseCents = largest.totalAmountCents,
            topPayerParticipantId = topPayerEntry?.key,
            topPayerTotalCents = topPayerEntry?.value ?: 0L,
            categoryBreakdown = categoryBreakdown,
            dailySpending = dailySpending
        )
    }
}
