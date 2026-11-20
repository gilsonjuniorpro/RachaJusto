package com.rachajusto.domain.repository

import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment

interface GroupRepository {
    suspend fun listGroups(): List<Group>
    suspend fun getGroup(id: String): Group?
    suspend fun createGroup(group: Group): Group
    suspend fun updateGroupInfo(
        groupId: String,
        name: String,
        description: String?,
        emoji: String
    ): Group
    suspend fun deleteGroup(groupId: String)
    suspend fun updateParticipants(groupId: String, participants: List<Participant>): Group
    suspend fun addExpense(groupId: String, expense: Expense): Group
    suspend fun updateExpense(groupId: String, expense: Expense): Group
    suspend fun deleteExpense(groupId: String, expenseId: String): Group
    suspend fun recordSettlement(groupId: String, payment: SettlementPayment): Group
    suspend fun undoSettlement(groupId: String, settlementId: String): Group
    suspend fun resetDemoData()
}
