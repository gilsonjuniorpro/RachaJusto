package com.rachajusto.domain.model

import java.time.LocalDate

enum class SplitMode {
    EQUAL,
    EXACT,
    SHARES,
    ITEMIZED
}

enum class ExpenseCategory {
    FOOD,
    DRINKS,
    LODGING,
    TRANSPORT,
    MARKET,
    OTHER
}

data class Participant(
    val id: String,
    val name: String,
    val avatarColorHex: Long,
    val pixKey: String? = null,
    val pixCity: String = "SAO PAULO"
)

data class ExpenseLineItem(
    val id: String,
    val name: String,
    val amountCents: Long,
    val participantIds: List<String>
)

data class ExpenseSplit(
    val participantId: String,
    val amountCents: Long,
    val shareWeight: Int? = null
)

data class Expense(
    val id: String,
    val groupId: String,
    val title: String,
    val category: ExpenseCategory,
    val date: LocalDate,
    val paidById: String,
    val totalAmountCents: Long,
    val serviceFeePercent: Int = 0,
    val splitMode: SplitMode,
    val items: List<ExpenseLineItem> = emptyList(),
    val splits: List<ExpenseSplit>,
    val createdAtIso: String
)

data class SettlementPayment(
    val id: String,
    val groupId: String,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountCents: Long,
    val paidAtIso: String,
    val note: String? = null
)

data class Group(
    val id: String,
    val name: String,
    val description: String? = null,
    val emoji: String,
    val participants: List<Participant>,
    val expenses: List<Expense> = emptyList(),
    val settlements: List<SettlementPayment> = emptyList(),
    val createdAtIso: String
)

data class ParticipantBalance(
    val participantId: String,
    val totalPaidCents: Long,
    val totalConsumedCents: Long,
    val totalSentCents: Long,
    val totalReceivedCents: Long,
    val netBalanceCents: Long
)

data class SuggestedTransfer(
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountCents: Long
)

data class SettlementSummary(
    val balances: List<ParticipantBalance>,
    val optimizedTransfers: List<SuggestedTransfer>,
    val naiveTransferCount: Int,
    val progressPercent: Float
)

enum class DemoReceiptPreset {
    BAR_COMANDA,
    CHURRASCO_MERCADO,
    RESTAURANTE_FONDUE
}

data class ScannedReceiptItem(
    val name: String,
    val amountCents: Long
)

data class ScannedReceiptResult(
    val title: String,
    val category: ExpenseCategory,
    val serviceFeePercent: Int,
    val items: List<ScannedReceiptItem>
)
