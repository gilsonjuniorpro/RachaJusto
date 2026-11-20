package com.rachajusto.feature.groupdetail

import com.rachajusto.core.util.CurrencyFormatter
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.SettlementSummary

data class ShareSummaryLabels(
    val totalSpentLabel: String,
    val pendingTransfersHeader: String,
    val transferLineFormat: String, // e.g. "• %1$s paga *%2$s* para %3$s"
    val pixKeyPrefix: String,
    val pixKeyNotRegistered: String,
    val allSettledMessage: String,
    val footerLabel: String
)

object ShareSummaryFormatter {

    fun formatWhatsAppSummary(
        group: Group,
        summary: SettlementSummary,
        labels: ShareSummaryLabels
    ): String {
        val participantsById = group.participants.associateBy { it.id }
        val totalSpentCents = group.expenses.sumOf { it.totalAmountCents }

        return buildString {
            appendLine("${group.emoji} *${group.name}*")
            appendLine("${labels.totalSpentLabel}: *${CurrencyFormatter.formatBrl(totalSpentCents)}*")
            appendLine()

            if (summary.optimizedTransfers.isEmpty()) {
                appendLine("✅ ${labels.allSettledMessage}")
            } else {
                appendLine("💸 *${labels.pendingTransfersHeader} (${summary.optimizedTransfers.size}):*")
                for (transfer in summary.optimizedTransfers) {
                    val fromName = participantsById[transfer.fromParticipantId]?.name ?: "?"
                    val toParticipant = participantsById[transfer.toParticipantId]
                    val toName = toParticipant?.name ?: "?"
                    val amountText = CurrencyFormatter.formatBrl(transfer.amountCents)
                    val pixInfo = toParticipant?.pixKey?.takeIf { it.isNotBlank() }
                        ?: labels.pixKeyNotRegistered

                    appendLine(String.format(labels.transferLineFormat, fromName, amountText, toName))
                    appendLine("   ↳ ${labels.pixKeyPrefix}: $pixInfo")
                }
            }
            appendLine()
            append(labels.footerLabel)
        }
    }
}
