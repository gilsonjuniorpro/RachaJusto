package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseSorterTest {

    @Test
    fun filterAndSort_supportsAccentInsensitiveSearchAndAllSortOrders() {
        val expenses = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).first().expenses

        val byAmountDesc = ExpenseSorter.filterAndSort(expenses, sortOption = ExpenseSortOption.AMOUNT_DESC)
        assertEquals("Airbnb Chalé", byAmountDesc.first().title)

        val byAmountAsc = ExpenseSorter.filterAndSort(expenses, sortOption = ExpenseSortOption.AMOUNT_ASC)
        assertEquals("Pedágios", byAmountAsc.first().title)

        // Accent-insensitive search matches "Café" via "cafe" and line item "Filé" via "file"
        val cafeMatches = ExpenseSorter.filterAndSort(expenses, query = "cafe")
        assertEquals(1, cafeMatches.size)
        assertEquals("Café da Manhã Padaria", cafeMatches.first().title)

        val itemMatches = ExpenseSorter.filterAndSort(expenses, query = "file mignon")
        assertTrue(itemMatches.any { it.title == "Restaurante Fondue" })
    }
}
