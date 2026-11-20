package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransferSavingsMetricsTest {

    @Test
    fun evaluate_camposDoJordaoGroup_reportsSignificantTransferReduction() {
        val group = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).first()
        val report = TransferSavingsMetrics.evaluate(group)

        assertTrue(report.savedTransfers > 0)
        assertTrue(report.reductionPercent >= 50)
        assertTrue(report.remainingSettlementVolumeCents < report.grossExpenseVolumeCents)
        assertNotNull(report.topCreditorId)
        assertNotNull(report.topDebtorId)
    }
}
