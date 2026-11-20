package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.model.SplitMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroupValidationRulesTest {

    @Test
    fun canRemoveParticipant_blocksParticipantsWithExpensesOrSettlementsAndAllowsUnused() {
        val p1 = Participant("p1", "Lucas", 0xFF0F766E)
        val p2 = Participant("p2", "Ana", 0xFF2563EB)
        val p3 = Participant("p3", "Pedro", 0xFF7C3AED)
        val p4 = Participant("p4", "Bia", 0xFFDB2777)

        val group = Group(
            id = "g1",
            name = "Test",
            emoji = "🏕️",
            participants = listOf(p1, p2, p3, p4),
            expenses = listOf(
                Expense(
                    id = "e1", groupId = "g1", title = "Food",
                    category = ExpenseCategory.FOOD, date = LocalDate.now(),
                    paidById = "p1", totalAmountCents = 10000L,
                    splitMode = SplitMode.EQUAL,
                    splits = SplitCalculator.calculateEqualSplits(10000L, listOf("p1", "p2")),
                    createdAtIso = "2026-01-01T00:00:00Z"
                )
            ),
            settlements = listOf(
                SettlementPayment(
                    id = "s1", groupId = "g1",
                    fromParticipantId = "p3", toParticipantId = "p1",
                    amountCents = 1000L, paidAtIso = "2026-01-02T00:00:00Z"
                )
            ),
            createdAtIso = "2026-01-01T00:00:00Z"
        )

        assertFalse(GroupValidationRules.canRemoveParticipant(group, "p1"))
        assertFalse(GroupValidationRules.canRemoveParticipant(group, "p2"))
        assertFalse(GroupValidationRules.canRemoveParticipant(group, "p3"))
        assertTrue(GroupValidationRules.canRemoveParticipant(group, "p4"))
    }
}
