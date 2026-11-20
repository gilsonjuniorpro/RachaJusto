package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.ParticipantBalance
import com.rachajusto.domain.model.SettlementSummary
import com.rachajusto.domain.model.SuggestedTransfer
import kotlin.math.abs
import kotlin.math.min

object CashFlowMinimizer {

    /**
     * Computes each participant's financial balance, minimizes the remaining
     * transfers using exact-pair matching followed by greedy largest-debtor/creditor
     * settlement, and calculates naive transfer count and settlement progress.
     */
    fun calculateSummary(group: Group): SettlementSummary {
        val paidMap = mutableMapOf<String, Long>()
        val consumedMap = mutableMapOf<String, Long>()
        val sentMap = mutableMapOf<String, Long>()
        val receivedMap = mutableMapOf<String, Long>()

        for (participant in group.participants) {
            paidMap[participant.id] = 0L
            consumedMap[participant.id] = 0L
            sentMap[participant.id] = 0L
            receivedMap[participant.id] = 0L
        }

        var rawObligationCount = 0
        for (expense in group.expenses) {
            paidMap[expense.paidById] = (paidMap[expense.paidById] ?: 0L) + expense.totalAmountCents
            for (split in expense.splits) {
                consumedMap[split.participantId] =
                    (consumedMap[split.participantId] ?: 0L) + split.amountCents
                if (split.participantId != expense.paidById && split.amountCents > 0L) {
                    rawObligationCount++
                }
            }
        }

        for (payment in group.settlements) {
            sentMap[payment.fromParticipantId] =
                (sentMap[payment.fromParticipantId] ?: 0L) + payment.amountCents
            receivedMap[payment.toParticipantId] =
                (receivedMap[payment.toParticipantId] ?: 0L) + payment.amountCents
        }

        val balances = group.participants.map { participant ->
            val paid = paidMap[participant.id] ?: 0L
            val consumed = consumedMap[participant.id] ?: 0L
            val sent = sentMap[participant.id] ?: 0L
            val received = receivedMap[participant.id] ?: 0L
            val net = paid - consumed + sent - received

            ParticipantBalance(
                participantId = participant.id,
                totalPaidCents = paid,
                totalConsumedCents = consumed,
                totalSentCents = sent,
                totalReceivedCents = received,
                netBalanceCents = net
            )
        }

        val balanceSum = balances.sumOf { it.netBalanceCents }
        check(balanceSum == 0L) {
            "Zero-sum balance invariant violated: sum=$balanceSum"
        }

        val optimizedTransfers = minimizeTransfers(balances)

        val naiveTransferCount = if (optimizedTransfers.isEmpty()) {
            0
        } else {
            maxOf(rawObligationCount - group.settlements.size, optimizedTransfers.size)
        }

        val totalInitialDebtCents = balances.sumOf { balance ->
            val initialNet = balance.totalPaidCents - balance.totalConsumedCents
            if (initialNet > 0L) initialNet else 0L
        }
        val remainingDebtCents = optimizedTransfers.sumOf { it.amountCents }

        val progressPercent = when {
            group.expenses.isEmpty() -> 0f
            totalInitialDebtCents == 0L -> 100f
            remainingDebtCents <= 0L -> 100f
            else -> {
                val settled = (totalInitialDebtCents - remainingDebtCents).coerceIn(0L, totalInitialDebtCents)
                ((settled.toDouble() / totalInitialDebtCents.toDouble()) * 100.0)
                    .toFloat()
                    .coerceIn(0f, 100f)
            }
        }

        return SettlementSummary(
            balances = balances,
            optimizedTransfers = optimizedTransfers,
            naiveTransferCount = naiveTransferCount,
            progressPercent = progressPercent
        )
    }

    internal fun minimizeTransfers(balances: List<ParticipantBalance>): List<SuggestedTransfer> {
        data class Node(val participantId: String, var amountCents: Long)

        val debtors = balances
            .filter { it.netBalanceCents < 0L }
            .map { Node(it.participantId, abs(it.netBalanceCents)) }
            .sortedWith(compareByDescending<Node> { it.amountCents }.thenBy { it.participantId })
            .toMutableList()

        val creditors = balances
            .filter { it.netBalanceCents > 0L }
            .map { Node(it.participantId, it.netBalanceCents) }
            .sortedWith(compareByDescending<Node> { it.amountCents }.thenBy { it.participantId })
            .toMutableList()

        val transfers = mutableListOf<SuggestedTransfer>()

        // Pass 1: Match exact equal-magnitude debtor/creditor pairs first
        val debtorIterator = debtors.iterator()
        while (debtorIterator.hasNext()) {
            val debtor = debtorIterator.next()
            val matchingCreditorIndex = creditors.indexOfFirst { it.amountCents == debtor.amountCents }
            if (matchingCreditorIndex >= 0) {
                val creditor = creditors.removeAt(matchingCreditorIndex)
                transfers.add(
                    SuggestedTransfer(
                        fromParticipantId = debtor.participantId,
                        toParticipantId = creditor.participantId,
                        amountCents = debtor.amountCents
                    )
                )
                debtorIterator.remove()
            }
        }

        // Pass 2: Greedy settlement between largest remaining debtor and largest remaining creditor
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortWith(compareByDescending<Node> { it.amountCents }.thenBy { it.participantId })
            creditors.sortWith(compareByDescending<Node> { it.amountCents }.thenBy { it.participantId })

            val debtor = debtors.first()
            val creditor = creditors.first()
            val settledAmount = min(debtor.amountCents, creditor.amountCents)

            if (settledAmount > 0L) {
                transfers.add(
                    SuggestedTransfer(
                        fromParticipantId = debtor.participantId,
                        toParticipantId = creditor.participantId,
                        amountCents = settledAmount
                    )
                )
            }

            debtor.amountCents -= settledAmount
            creditor.amountCents -= settledAmount

            if (debtor.amountCents == 0L) debtors.removeAt(0)
            if (creditor.amountCents == 0L) creditors.removeAt(0)
        }

        return transfers.sortedWith(
            compareByDescending<SuggestedTransfer> { it.amountCents }
                .thenBy { it.fromParticipantId }
                .thenBy { it.toParticipantId }
        )
    }
}
