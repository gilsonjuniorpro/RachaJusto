package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupCsvExporterTest {

    @Test
    fun exportExpensesCsv_includesHeaderExpensesAndSettlements() {
        val group = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).first()
        val csv = GroupCsvExporter.exportExpensesCsv(group)

        assertTrue(csv.startsWith("\uFEFFType,Date,Title,Category,SplitMode,Payer,AmountBRL,AmountCents,Details"))
        assertTrue(csv.contains("Airbnb Chalé"))
        assertTrue(csv.contains("EXPENSE"))
        assertTrue(csv.contains("SETTLEMENT"))
        assertTrue(csv.contains("240000"))
    }

    @Test
    fun escapeCsvField_quotesCommasAndDoublesInternalQuotes() {
        assertEquals("Simple", GroupCsvExporter.escapeCsvField("Simple"))
        assertEquals("\"R$ 10,00\"", GroupCsvExporter.escapeCsvField("R$ 10,00"))
        assertEquals("\"Item \"\"VIP\"\"\"", GroupCsvExporter.escapeCsvField("Item \"VIP\""))
    }
}
