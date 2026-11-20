package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

class ExpenseDuplicatorTest {

    @Test
    fun duplicateForDate_generatesFreshIdsAndPreservesExactSplits() {
        val group = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).first()
        val fondue = group.expenses.first { it.items.isNotEmpty() }
        val targetDate = LocalDate.of(2026, 12, 1)

        val copy = ExpenseDuplicator.duplicateForDate(fondue, targetDate = targetDate)

        assertNotEquals(fondue.id, copy.id)
        assertEquals(targetDate, copy.date)
        assertEquals(fondue.totalAmountCents, copy.totalAmountCents)
        assertEquals(fondue.splits, copy.splits)
        assertEquals(fondue.items.size, copy.items.size)
        assertNotEquals(fondue.items.first().id, copy.items.first().id)
    }
}
