package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Group

enum class IntegrityIssueCode {
    DUPLICATE_PARTICIPANT_ID,
    UNKNOWN_PAYER,
    UNKNOWN_SPLIT_PARTICIPANT,
    SPLIT_SUM_MISMATCH,
    NEGATIVE_AMOUNT,
    UNKNOWN_SETTLEMENT_PARTICIPANT,
    SELF_SETTLEMENT
}

data class IntegrityIssue(
    val code: IntegrityIssueCode,
    val referenceId: String
)

data class IntegrityReport(
    val issues: List<IntegrityIssue>
) {
    val isValid: Boolean get() = issues.isEmpty()
}

object GroupIntegrityAuditor {

    fun audit(group: Group): IntegrityReport {
        val issues = mutableListOf<IntegrityIssue>()
        val participantIds = mutableSetOf<String>()

        for (participant in group.participants) {
            if (!participantIds.add(participant.id)) {
                issues.add(IntegrityIssue(IntegrityIssueCode.DUPLICATE_PARTICIPANT_ID, participant.id))
            }
        }

        for (expense in group.expenses) {
            if (expense.totalAmountCents < 0L || expense.splits.any { it.amountCents < 0L }) {
                issues.add(IntegrityIssue(IntegrityIssueCode.NEGATIVE_AMOUNT, expense.id))
            }
            if (expense.paidById !in participantIds) {
                issues.add(IntegrityIssue(IntegrityIssueCode.UNKNOWN_PAYER, expense.id))
            }
            if (expense.splits.any { it.participantId !in participantIds }) {
                issues.add(IntegrityIssue(IntegrityIssueCode.UNKNOWN_SPLIT_PARTICIPANT, expense.id))
            }
            if (expense.splits.sumOf { it.amountCents } != expense.totalAmountCents) {
                issues.add(IntegrityIssue(IntegrityIssueCode.SPLIT_SUM_MISMATCH, expense.id))
            }
        }

        for (settlement in group.settlements) {
            if (settlement.amountCents <= 0L) {
                issues.add(IntegrityIssue(IntegrityIssueCode.NEGATIVE_AMOUNT, settlement.id))
            }
            if (settlement.fromParticipantId !in participantIds ||
                settlement.toParticipantId !in participantIds
            ) {
                issues.add(IntegrityIssue(IntegrityIssueCode.UNKNOWN_SETTLEMENT_PARTICIPANT, settlement.id))
            }
            if (settlement.fromParticipantId == settlement.toParticipantId) {
                issues.add(IntegrityIssue(IntegrityIssueCode.SELF_SETTLEMENT, settlement.id))
            }
        }

        return IntegrityReport(issues)
    }
}
