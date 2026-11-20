package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.Group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupAnalyticsEngineTest {

    @Test
    fun analyze_emptyGroup_returnsZeroedAnalytics() {
        val empty = Group(
            id = "g_empty",
            name = "Empty",
            emoji = "🧾",
            participants = emptyList(),
            createdAtIso = "2026-11-20T00:00:00Z"
        )
        val result = GroupAnalyticsEngine.analyze(empty)
        assertEquals(0L, result.totalSpentCents)
        assertNull(result.largestExpenseTitle)
        assertTrue(result.categoryBreakdown.isEmpty())
    }

    @Test
    fun analyze_camposDoJordaoSeed_computesCategoryPercentagesAndTopPayer() {
        val group = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).first()
        val result = GroupAnalyticsEngine.analyze(group)

        assertEquals(group.expenses.sumOf { it.totalAmountCents }, result.totalSpentCents)
        assertEquals("Airbnb Chalé", result.largestExpenseTitle)
        assertEquals(240000L, result.largestExpenseCents)
        assertEquals("p_lucas", result.topPayerParticipantId)
        assertEquals(ExpenseCategory.LODGING, result.categoryBreakdown.first().category)
        assertEquals(100f, result.categoryBreakdown.sumOf { it.percentage.toDouble() }.toFloat(), 0.1f)
        assertTrue(result.dailySpending.isNotEmpty())
    }
}
