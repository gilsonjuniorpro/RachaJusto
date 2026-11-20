package com.rachajusto.domain.engine

import com.rachajusto.data.seed.SeedDataFactory
import com.rachajusto.domain.model.SettlementPayment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupIntegrityAuditorTest {

    @Test
    fun audit_allSeedGroupsPassWithZeroIssues() {
        val seedGroups = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20))
        for (group in seedGroups) {
            val report = GroupIntegrityAuditor.audit(group)
            assertTrue("Expected valid group ${group.id}, got ${report.issues}", report.isValid)
        }
    }

    @Test
    fun audit_detectsSplitMismatchAndSelfSettlement() {
        val group = SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)).last()
        val brokenExpense = group.expenses.first().copy(totalAmountCents = 999999L)
        val selfSettlement = SettlementPayment(
            id = "stl_self",
            groupId = group.id,
            fromParticipantId = group.participants.first().id,
            toParticipantId = group.participants.first().id,
            amountCents = 1000L,
            paidAtIso = "2026-11-20T00:00:00Z"
        )

        val brokenGroup = group.copy(
            expenses = listOf(brokenExpense),
            settlements = listOf(selfSettlement)
        )
        val report = GroupIntegrityAuditor.audit(brokenGroup)

        assertFalse(report.isValid)
        assertTrue(report.issues.any { it.code == IntegrityIssueCode.SPLIT_SUM_MISMATCH })
        assertTrue(report.issues.any { it.code == IntegrityIssueCode.SELF_SETTLEMENT })
    }
}
