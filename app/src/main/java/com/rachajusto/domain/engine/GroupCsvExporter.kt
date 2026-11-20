package com.rachajusto.domain.engine

import com.rachajusto.core.util.CurrencyFormatter
import com.rachajusto.domain.model.Group

object GroupCsvExporter {

    private const val UTF8_BOM = "\uFEFF"

    fun exportExpensesCsv(group: Group): String {
        val participantsById = group.participants.associateBy { it.id }
        return buildString {
            append(UTF8_BOM)
            appendLine("Type,Date,Title,Category,SplitMode,Payer,AmountBRL,AmountCents,Details")

            for (expense in group.expenses.sortedByDescending { it.date }) {
                val payerName = participantsById[expense.paidById]?.name ?: expense.paidById
                val splitDetails = expense.splits.joinToString(" | ") { split ->
                    val name = participantsById[split.participantId]?.name ?: split.participantId
                    "$name: ${CurrencyFormatter.formatBrl(split.amountCents)}"
                }
                appendLine(
                    listOf(
                        "EXPENSE",
                        expense.date.toString(),
                        expense.title,
                        expense.category.name,
                        expense.splitMode.name,
                        payerName,
                        CurrencyFormatter.formatBrl(expense.totalAmountCents),
                        expense.totalAmountCents.toString(),
                        splitDetails
                    ).joinToString(",") { escapeCsvField(it) }
                )
            }

            for (settlement in group.settlements) {
                val fromName = participantsById[settlement.fromParticipantId]?.name
                    ?: settlement.fromParticipantId
                val toName = participantsById[settlement.toParticipantId]?.name
                    ?: settlement.toParticipantId
                appendLine(
                    listOf(
                        "SETTLEMENT",
                        settlement.paidAtIso.take(10),
                        "$fromName -> $toName",
                        "SETTLEMENT",
                        "PIX",
                        fromName,
                        CurrencyFormatter.formatBrl(settlement.amountCents),
                        settlement.amountCents.toString(),
                        settlement.note.orEmpty()
                    ).joinToString(",") { escapeCsvField(it) }
                )
            }
        }
    }

    internal fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') ||
            value.contains('\n') || value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }
}
