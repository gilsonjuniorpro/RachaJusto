package com.rachajusto.domain.engine

import com.rachajusto.domain.model.ExpenseLineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun equalSplit_100ReaisBetween3Participants_distributesRemainderDeterministically() {
        val splits = SplitCalculator.calculateEqualSplits(
            totalCents = 10000L,
            participantIds = listOf("p3", "p1", "p2")
        )

        assertEquals(3, splits.size)
        assertEquals("p1", splits[0].participantId)
        assertEquals(3334L, splits[0].amountCents)
        assertEquals("p2", splits[1].participantId)
        assertEquals(3333L, splits[1].amountCents)
        assertEquals("p3", splits[2].participantId)
        assertEquals(3333L, splits[2].amountCents)
        assertEquals(10000L, splits.sumOf { it.amountCents })
    }

    @Test
    fun equalSplit_tolls8650Between6Participants_preservesExactSum() {
        val ids = listOf("ana", "bia", "carla", "lucas", "pedro", "rafa")
        val splits = SplitCalculator.calculateEqualSplits(8650L, ids)

        assertEquals(8650L, splits.sumOf { it.amountCents })
        // 8650 / 6 = 1441 remainder 4 -> first 4 sorted get 1442, last 2 get 1441
        assertEquals(listOf(1442L, 1442L, 1442L, 1442L, 1441L, 1441L), splits.map { it.amountCents })
    }

    @Test
    fun exactSplit_validatesExactTotalAndRejectsMismatch() {
        val splits = SplitCalculator.calculateExactSplits(
            totalCents = 15000L,
            amountsByParticipant = mapOf("p2" to 10000L, "p1" to 5000L, "p3" to 0L)
        )

        assertEquals(2, splits.size)
        assertEquals(15000L, splits.sumOf { it.amountCents })
        assertEquals(5000L, splits.first { it.participantId == "p1" }.amountCents)
        assertEquals(10000L, splits.first { it.participantId == "p2" }.amountCents)

        assertThrows(IllegalArgumentException::class.java) {
            SplitCalculator.calculateExactSplits(
                totalCents = 15000L,
                amountsByParticipant = mapOf("p1" to 5000L, "p2" to 9999L)
            )
        }
    }

    @Test
    fun shareSplit_rent360000WithWeights3_2_2_usesLargestRemainderMethod() {
        val splits = SplitCalculator.calculateShareSplits(
            totalCents = 360000L,
            weightsByParticipant = mapOf("gabriel" to 3, "matheus" to 2, "tiago" to 2)
        )

        assertEquals(360000L, splits.sumOf { it.amountCents })
        // 360000 * 3 / 7 = 154285 rem 5; 360000 * 2 / 7 = 102857 rem 1 (x2)
        // leftover = 360000 - (154285 + 102857 + 102857) = 1 -> goes to gabriel (highest remainder 5/7)
        assertEquals(154286L, splits.first { it.participantId == "gabriel" }.amountCents)
        assertEquals(102857L, splits.first { it.participantId == "matheus" }.amountCents)
        assertEquals(102857L, splits.first { it.participantId == "tiago" }.amountCents)
        assertEquals(3, splits.first { it.participantId == "gabriel" }.shareWeight)
    }

    @Test
    fun itemizedSplit_with10PercentServiceFee_distributesProportionallyWithExactCents() {
        val items = listOf(
            ExpenseLineItem("i1", "Fondue Queijo", 18000L, listOf("ana", "bia", "carla")),
            ExpenseLineItem("i2", "Vinho Tinto", 14000L, listOf("carla", "lucas")),
            ExpenseLineItem("i3", "Suco Natural", 2500L, listOf("ana", "bia"))
        )

        val result = SplitCalculator.calculateItemizedSplits(
            items = items,
            serviceFeePercent = 10
        )

        assertEquals(34500L, result.subtotalCents)
        assertEquals(3450L, result.serviceFeeCents)
        assertEquals(37950L, result.grandTotalCents)
        assertEquals(result.grandTotalCents, result.splits.sumOf { it.amountCents })

        // Subtotals: ana=7250, bia=7250, carla=13000, lucas=7000
        // 10% fee: ana=725, bia=725, carla=1300, lucas=700
        assertEquals(7975L, result.splits.first { it.participantId == "ana" }.amountCents)
        assertEquals(7975L, result.splits.first { it.participantId == "bia" }.amountCents)
        assertEquals(14300L, result.splits.first { it.participantId == "carla" }.amountCents)
        assertEquals(7700L, result.splits.first { it.participantId == "lucas" }.amountCents)
    }

    @Test
    fun itemizedSplit_withOddServiceFeeAndDiscount_maintainsExactGrandTotal() {
        val items = listOf(
            ExpenseLineItem("i1", "Picanha", 10000L, listOf("p1", "p2", "p3")),
            ExpenseLineItem("i2", "Cerveja", 7777L, listOf("p1", "p2"))
        )

        val result = SplitCalculator.calculateItemizedSplits(
            items = items,
            serviceFeePercent = 13,
            discountCents = 1000L
        )

        assertEquals(17777L, result.subtotalCents)
        assertEquals(2311L, result.serviceFeeCents) // round(17777 * 0.13) = 2311.01 -> 2311
        assertEquals(19088L, result.grandTotalCents)
        assertEquals(19088L, result.splits.sumOf { it.amountCents })
    }
}
