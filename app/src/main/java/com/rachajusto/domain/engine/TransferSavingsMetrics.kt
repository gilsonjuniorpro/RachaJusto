package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.SettlementSummary

data class TransferEfficiencyReport(
    val naiveTransfers: Int,
    val optimizedTransfers: Int,
    val savedTransfers: Int,
    val reductionPercent: Int,
    val grossExpenseVolumeCents: Long,
    val remainingSettlementVolumeCents: Long,
    val topCreditorId: String?,
    val topDebtorId: String?
)

object TransferSavingsMetrics {

    fun evaluate(group: Group, summary: SettlementSummary = CashFlowMinimizer.calculateSummary(group)): TransferEfficiencyReport {
        val naive = summary.naiveTransferCount
        val optimized = summary.optimizedTransfers.size
        val saved = (naive - optimized).coerceAtLeast(0)
        val reductionPct = if (naive > 0) {
            ((saved.toDouble() / naive.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val topCreditor = summary.balances
            .filter { it.netBalanceCents > 0L }
            .maxByOrNull { it.netBalanceCents }
            ?.participantId

        val topDebtor = summary.balances
            .filter { it.netBalanceCents < 0L }
            .minByOrNull { it.netBalanceCents }
            ?.participantId

        return TransferEfficiencyReport(
            naiveTransfers = naive,
            optimizedTransfers = optimized,
            savedTransfers = saved,
            reductionPercent = reductionPct,
            grossExpenseVolumeCents = group.expenses.sumOf { it.totalAmountCents },
            remainingSettlementVolumeCents = summary.optimizedTransfers.sumOf { it.amountCents },
            topCreditorId = topCreditor,
            topDebtorId = topDebtor
        )
    }
}
