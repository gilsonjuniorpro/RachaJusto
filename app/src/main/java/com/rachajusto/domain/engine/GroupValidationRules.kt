package com.rachajusto.domain.engine

import com.rachajusto.domain.model.Group

object GroupValidationRules {

    /**
     * Prevents removing any participant referenced in group expenses or recorded settlements.
     */
    fun canRemoveParticipant(group: Group, participantId: String): Boolean {
        val usedInExpenses = group.expenses.any { expense ->
            expense.paidById == participantId ||
                expense.splits.any { it.participantId == participantId } ||
                expense.items.any { participantId in it.participantIds }
        }
        if (usedInExpenses) return false

        val usedInSettlements = group.settlements.any { settlement ->
            settlement.fromParticipantId == participantId ||
                settlement.toParticipantId == participantId
        }
        return !usedInSettlements
    }
}
