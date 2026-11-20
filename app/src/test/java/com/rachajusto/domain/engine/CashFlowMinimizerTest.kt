package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.model.SplitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CashFlowMinimizerTest {

    private fun participant(id: String) = Participant(
        id = id,
        name = id.uppercase(),
        avatarColorHex = 0xFF0F766E
    )

    @Test
    fun calculateSummary_matchesExactPairsBeforeGreedyAndMinimizesTransfers() {
        // A paid 100 split equally A,B -> A=+50, B=-50
        // C paid 60 split equally C,D -> C=+30, D=-30
        // Exact pair matching should produce exactly 2 transfers: B->A (50), D->C (30)
        val group = Group(
            id = "g1",
            name = "Exact Pair Test",
            emoji = "🧪",
            participants = listOf(participant("a"), participant("b"), participant("c"), participant("d")),
            expenses = listOf(
                Expense(
                    id = "e1",
                    groupId = "g1",
                    title = "Dinner",
                    category = ExpenseCategory.FOOD,
                    date = LocalDate.now(),
                    paidById = "a",
                    totalAmountCents = 10000L,
                    splitMode = SplitMode.EQUAL,
                    splits = SplitCalculator.calculateEqualSplits(10000L, listOf("a", "b")),
                    createdAtIso = "2026-01-01T00:00:00Z"
                ),
                Expense(
                    id = "e2",
                    groupId = "g1",
                    title = "Drinks",
                    category = ExpenseCategory.DRINKS,
                    date = LocalDate.now(),
                    paidById = "c",
                    totalAmountCents = 6000L,
                    splitMode = SplitMode.EQUAL,
                    splits = SplitCalculator.calculateEqualSplits(6000L, listOf("c", "d")),
                    createdAtIso = "2026-01-01T00:00:00Z"
                )
            ),
            createdAtIso = "2026-01-01T00:00:00Z"
        )

        val summary = CashFlowMinimizer.calculateSummary(group)

        assertEquals(0L, summary.balances.sumOf { it.netBalanceCents })
        assertEquals(2, summary.optimizedTransfers.size)
        assertEquals(5000L, summary.optimizedTransfers[0].amountCents)
        assertEquals("b", summary.optimizedTransfers[0].fromParticipantId)
        assertEquals("a", summary.optimizedTransfers[0].toParticipantId)
        assertEquals(3000L, summary.optimizedTransfers[1].amountCents)
        assertEquals("d", summary.optimizedTransfers[1].fromParticipantId)
        assertEquals("c", summary.optimizedTransfers[1].toParticipantId)
        assertEquals(0f, summary.progressPercent, 0.01f)
    }

    @Test
    fun calculateSummary_complexMultiPayerGroup_reducesNaiveTransfersSignificantly() {
        val participants = listOf("p1", "p2", "p3", "p4", "p5", "p6").map { participant(it) }
        val allIds = participants.map { it.id }

        val expenses = listOf(
            Expense(
                id = "e1", groupId = "g2", title = "House", category = ExpenseCategory.LODGING,
                date = LocalDate.now(), paidById = "p1", totalAmountCents = 240000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(240000L, allIds),
                createdAtIso = "2026-01-01T00:00:00Z"
            ),
            Expense(
                id = "e2", groupId = "g2", title = "Market", category = ExpenseCategory.MARKET,
                date = LocalDate.now(), paidById = "p2", totalAmountCents = 60000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(60000L, allIds),
                createdAtIso = "2026-01-01T00:00:00Z"
            ),
            Expense(
                id = "e3", groupId = "g2", title = "Drinks", category = ExpenseCategory.DRINKS,
                date = LocalDate.now(), paidById = "p3", totalAmountCents = 42000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(42000L, listOf("p1", "p3", "p4", "p5")),
                createdAtIso = "2026-01-01T00:00:00Z"
            ),
            Expense(
                id = "e4", groupId = "g2", title = "Fuel", category = ExpenseCategory.TRANSPORT,
                date = LocalDate.now(), paidById = "p5", totalAmountCents = 24000L,
                splitMode = SplitMode.EQUAL,
                splits = SplitCalculator.calculateEqualSplits(24000L, listOf("p1", "p5", "p6")),
                createdAtIso = "2026-01-01T00:00:00Z"
            )
        )

        val group = Group(
            id = "g2", name = "Trip", emoji = "🏔️",
            participants = participants, expenses = expenses,
            createdAtIso = "2026-01-01T00:00:00Z"
        )

        val summary = CashFlowMinimizer.calculateSummary(group)

        assertEquals(0L, summary.balances.sumOf { it.netBalanceCents })
        assertTrue(
            "Expected optimized transfers (${summary.optimizedTransfers.size}) < naive (${summary.naiveTransferCount})",
            summary.optimizedTransfers.size < summary.naiveTransferCount
        )
        assertTrue(summary.optimizedTransfers.size <= participants.size - 1)
    }

    @Test
    fun calculateSummary_fullySettledGroup_hasZeroTransfersAnd100PercentProgress() {
        val group = Group(
            id = "g3",
            name = "Settled",
            emoji = "✅",
            participants = listOf(participant("a"), participant("b")),
            expenses = listOf(
                Expense(
                    id = "e1", groupId = "g3", title = "Pizza", category = ExpenseCategory.FOOD,
                    date = LocalDate.now(), paidById = "a", totalAmountCents = 10000L,
                    splitMode = SplitMode.EQUAL,
                    splits = SplitCalculator.calculateEqualSplits(10000L, listOf("a", "b")),
                    createdAtIso = "2026-01-01T00:00:00Z"
                )
            ),
            settlements = listOf(
                SettlementPayment(
                    id = "s1", groupId = "g3",
                    fromParticipantId = "b", toParticipantId = "a",
                    amountCents = 5000L, paidAtIso = "2026-01-02T00:00:00Z"
                )
            ),
            createdAtIso = "2026-01-01T00:00:00Z"
        )

        val summary = CashFlowMinimizer.calculateSummary(group)

        assertEquals(0L, summary.balances.sumOf { it.netBalanceCents })
        assertTrue(summary.balances.all { it.netBalanceCents == 0L })
        assertTrue(summary.optimizedTransfers.isEmpty())
        assertEquals(0, summary.naiveTransferCount)
        assertEquals(100f, summary.progressPercent, 0.01f)
    }

    @Test
    fun calculateSummary_partialSettlement_tracksAccurateProgressPercent() {
        val group = Group(
            id = "g4",
            name = "Partial",
            emoji = "⏳",
            participants = listOf(participant("a"), participant("b")),
            expenses = listOf(
                Expense(
                    id = "e1", groupId = "g4", title = "Rent", category = ExpenseCategory.LODGING,
                    date = LocalDate.now(), paidById = "a", totalAmountCents = 20000L,
                    splitMode = SplitMode.EQUAL,
                    splits = SplitCalculator.calculateEqualSplits(20000L, listOf("a", "b")),
                    createdAtIso = "2026-01-01T00:00:00Z"
                )
            ),
            settlements = listOf(
                SettlementPayment(
                    id = "s1", groupId = "g4",
                    fromParticipantId = "b", toParticipantId = "a",
                    amountCents = 4000L, paidAtIso = "2026-01-02T00:00:00Z"
                )
            ),
            createdAtIso = "2026-01-01T00:00:00Z"
        )

        val summary = CashFlowMinimizer.calculateSummary(group)
        assertEquals(40f, summary.progressPercent, 0.01f)
        assertEquals(1, summary.optimizedTransfers.size)
        assertEquals(6000L, summary.optimizedTransfers.first().amountCents)
    }
}
