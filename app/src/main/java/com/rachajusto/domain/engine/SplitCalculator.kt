package com.rachajusto.domain.engine

import com.rachajusto.domain.model.ExpenseLineItem
import com.rachajusto.domain.model.ExpenseSplit
import java.math.BigInteger

data class ItemizedCalculationResult(
    val subtotalCents: Long,
    val serviceFeeCents: Long,
    val discountCents: Long,
    val grandTotalCents: Long,
    val splits: List<ExpenseSplit>
)

object SplitCalculator {

    /**
     * Splits [totalCents] equally across [participantIds].
     * Any division remainder is distributed deterministically (+1 cent each)
     * following ascending participant ID order so the sum always equals [totalCents].
     */
    fun calculateEqualSplits(
        totalCents: Long,
        participantIds: List<String>
    ): List<ExpenseSplit> {
        require(totalCents >= 0L) { "totalCents must be non-negative" }
        val sortedIds = participantIds.distinct().sorted()
        require(sortedIds.isNotEmpty()) { "At least one participant is required" }

        val count = sortedIds.size.toLong()
        val baseAmount = totalCents / count
        val remainder = (totalCents % count).toInt()

        return sortedIds.mapIndexed { index, id ->
            val extraCent = if (index < remainder) 1L else 0L
            ExpenseSplit(
                participantId = id,
                amountCents = baseAmount + extraCent
            )
        }
    }

    /**
     * Validates and constructs splits for exact per-participant amounts.
     */
    fun calculateExactSplits(
        totalCents: Long,
        amountsByParticipant: Map<String, Long>
    ): List<ExpenseSplit> {
        require(totalCents >= 0L) { "totalCents must be non-negative" }
        require(amountsByParticipant.isNotEmpty()) { "At least one participant is required" }
        require(amountsByParticipant.values.all { it >= 0L }) { "Amounts must be non-negative" }

        val sum = amountsByParticipant.values.sum()
        require(sum == totalCents) {
            "Exact splits sum ($sum) does not match totalAmountCents ($totalCents)"
        }

        return amountsByParticipant.entries
            .sortedBy { it.key }
            .filter { it.value > 0L }
            .map { (participantId, amount) ->
                ExpenseSplit(
                    participantId = participantId,
                    amountCents = amount
                )
            }
    }

    /**
     * Splits [totalCents] proportionally according to positive integer weights using
     * the Largest Remainder Method (Hamilton apportionment) in exact integer arithmetic.
     */
    fun calculateShareSplits(
        totalCents: Long,
        weightsByParticipant: Map<String, Int>
    ): List<ExpenseSplit> {
        require(totalCents >= 0L) { "totalCents must be non-negative" }
        val activeWeights = weightsByParticipant
            .filterValues { it > 0 }
            .mapValues { it.value.toLong() }
        require(activeWeights.isNotEmpty()) { "At least one participant must have a positive share weight" }

        val allocated = allocateProportionally(totalCents, activeWeights)

        return activeWeights.keys
            .sorted()
            .map { participantId ->
                ExpenseSplit(
                    participantId = participantId,
                    amountCents = allocated.getValue(participantId),
                    shareWeight = activeWeights.getValue(participantId).toInt()
                )
            }
    }

    /**
     * Splits an itemized receipt where each line item is divided equally among its
     * selected participants, then distributes optional [serviceFeePercent] and [discountCents]
     * proportionally to each participant's subtotal using the Largest Remainder Method.
     */
    fun calculateItemizedSplits(
        items: List<ExpenseLineItem>,
        serviceFeePercent: Int = 0,
        discountCents: Long = 0L
    ): ItemizedCalculationResult {
        require(items.isNotEmpty()) { "At least one line item is required" }
        require(serviceFeePercent in 0..100) { "serviceFeePercent must be between 0 and 100" }
        require(discountCents >= 0L) { "discountCents must be non-negative" }

        val subtotalByParticipant = mutableMapOf<String, Long>()
        var subtotalCents = 0L

        for (item in items) {
            require(item.amountCents >= 0L) { "Line item amount must be non-negative" }
            require(item.participantIds.isNotEmpty()) {
                "Line item '${item.name}' must have at least one participant"
            }
            subtotalCents += item.amountCents
            val itemSplits = calculateEqualSplits(item.amountCents, item.participantIds)
            for (split in itemSplits) {
                subtotalByParticipant[split.participantId] =
                    (subtotalByParticipant[split.participantId] ?: 0L) + split.amountCents
            }
        }

        // Exact half-up rounding in integer arithmetic: (subtotal * percent + 50) / 100
        val serviceFeeCents = if (serviceFeePercent > 0 && subtotalCents > 0L) {
            BigInteger.valueOf(subtotalCents)
                .multiply(BigInteger.valueOf(serviceFeePercent.toLong()))
                .add(BigInteger.valueOf(50L))
                .divide(BigInteger.valueOf(100L))
                .longValueExact()
        } else {
            0L
        }

        val effectiveDiscountCents = discountCents.coerceAtMost(subtotalCents + serviceFeeCents)
        val grandTotalCents = subtotalCents + serviceFeeCents - effectiveDiscountCents

        val feeAllocation = if (serviceFeeCents > 0L) {
            allocateProportionally(serviceFeeCents, subtotalByParticipant)
        } else {
            emptyMap()
        }

        val preDiscountByParticipant = subtotalByParticipant.mapValues { (id, sub) ->
            sub + (feeAllocation[id] ?: 0L)
        }

        val discountAllocation = if (effectiveDiscountCents > 0L) {
            allocateProportionally(effectiveDiscountCents, preDiscountByParticipant)
        } else {
            emptyMap()
        }

        val splits = preDiscountByParticipant.keys
            .sorted()
            .map { participantId ->
                val finalAmount = (preDiscountByParticipant[participantId] ?: 0L) -
                    (discountAllocation[participantId] ?: 0L)
                ExpenseSplit(
                    participantId = participantId,
                    amountCents = finalAmount
                )
            }

        check(splits.sumOf { it.amountCents } == grandTotalCents) {
            "Itemized splits sum invariant violated"
        }

        return ItemizedCalculationResult(
            subtotalCents = subtotalCents,
            serviceFeeCents = serviceFeeCents,
            discountCents = effectiveDiscountCents,
            grandTotalCents = grandTotalCents,
            splits = splits
        )
    }

    /**
     * Exact integer Largest Remainder Method (Hamilton apportionment).
     * Guarantees sum(result.values) == [totalCents] with deterministic ID tie-breaking.
     */
    internal fun allocateProportionally(
        totalCents: Long,
        weightsByKey: Map<String, Long>
    ): Map<String, Long> {
        if (totalCents == 0L) {
            return weightsByKey.keys.associateWith { 0L }
        }

        val positiveWeights = weightsByKey.filterValues { it > 0L }
        if (positiveWeights.isEmpty()) {
            val equal = calculateEqualSplits(totalCents, weightsByKey.keys.toList())
            return equal.associate { it.participantId to it.amountCents }
        }

        val totalWeight = positiveWeights.values.fold(BigInteger.ZERO) { acc, w ->
            acc.add(BigInteger.valueOf(w))
        }
        val totalBig = BigInteger.valueOf(totalCents)

        data class Entry(
            val key: String,
            val floorCents: Long,
            val remainderNumerator: BigInteger
        )

        val entries = positiveWeights.entries.map { (key, weight) ->
            val numerator = totalBig.multiply(BigInteger.valueOf(weight))
            val divRem = numerator.divideAndRemainder(totalWeight)
            Entry(
                key = key,
                floorCents = divRem[0].longValueExact(),
                remainderNumerator = divRem[1]
            )
        }

        val floorSum = entries.sumOf { it.floorCents }
        val leftoverCents = (totalCents - floorSum).toInt()

        val sortedByRemainder = entries.sortedWith(
            compareByDescending<Entry> { it.remainderNumerator }
                .thenBy { it.key }
        )

        val result = weightsByKey.keys.associateWith { 0L }.toMutableMap()
        sortedByRemainder.forEachIndexed { index, entry ->
            val bonus = if (index < leftoverCents) 1L else 0L
            result[entry.key] = entry.floorCents + bonus
        }
        return result
    }
}
