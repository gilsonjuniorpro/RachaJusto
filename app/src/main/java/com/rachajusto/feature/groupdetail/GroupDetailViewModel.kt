package com.rachajusto.feature.groupdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rachajusto.domain.engine.CashFlowMinimizer
import com.rachajusto.domain.engine.GroupValidationRules
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.model.SettlementSummary
import com.rachajusto.domain.model.SuggestedTransfer
import com.rachajusto.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

enum class GroupDetailTab {
    EXPENSES,
    BALANCES,
    SETTLEMENT,
    PARTICIPANTS
}

enum class GroupDetailFeedback {
    EXPENSE_DELETED,
    SETTLEMENT_RECORDED,
    SETTLEMENT_UNDONE,
    PARTICIPANT_ADDED,
    PARTICIPANT_UPDATED,
    PARTICIPANT_REMOVED,
    PARTICIPANT_CANNOT_REMOVE_LINKED,
    ACTION_FAILED
}

data class GroupDetailUiState(
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val group: Group? = null,
    val summary: SettlementSummary? = null,
    val selectedTab: GroupDetailTab = GroupDetailTab.EXPENSES,
    val selectedPayerFilterId: String? = null,
    val selectedCategoryFilter: ExpenseCategory? = null,
    val loadError: Boolean = false,
    val feedback: GroupDetailFeedback? = null,
    val pendingDeleteExpense: Expense? = null,
    val editingParticipant: Participant? = null,
    val isAddParticipantDialogOpen: Boolean = false,
    val blockedRemovalParticipantName: String? = null,
    val activePixTransfer: SuggestedTransfer? = null
) {
    val filteredExpenses: List<Expense>
        get() {
            val all = group?.expenses ?: return emptyList()
            return all
                .filter { expense ->
                    (selectedPayerFilterId == null || expense.paidById == selectedPayerFilterId) &&
                        (selectedCategoryFilter == null || expense.category == selectedCategoryFilter)
                }
                .sortedWith(
                    compareByDescending<Expense> { it.date }
                        .thenByDescending { it.createdAtIso }
                )
        }

    val totalSpentCents: Long
        get() = group?.expenses?.sumOf { it.totalAmountCents } ?: 0L

    val averagePerPersonCents: Long
        get() {
            val count = group?.participants?.size ?: 0
            return if (count > 0) totalSpentCents / count else 0L
        }
}

class GroupDetailViewModel(
    private val groupId: String,
    private val repository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        loadGroup()
    }

    fun loadGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = false) }
            runCatching { repository.getGroup(groupId) }
                .onSuccess { group ->
                    if (group == null) {
                        _uiState.update { it.copy(isLoading = false, loadError = true) }
                    } else {
                        applyGroup(group)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadError = true) }
                }
        }
    }

    fun selectTab(tab: GroupDetailTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun togglePayerFilter(participantId: String?) {
        _uiState.update { state ->
            val next = if (state.selectedPayerFilterId == participantId) null else participantId
            state.copy(selectedPayerFilterId = next)
        }
    }

    fun toggleCategoryFilter(category: ExpenseCategory?) {
        _uiState.update { state ->
            val next = if (state.selectedCategoryFilter == category) null else category
            state.copy(selectedCategoryFilter = next)
        }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(selectedPayerFilterId = null, selectedCategoryFilter = null)
        }
    }

    fun requestDeleteExpense(expense: Expense) {
        _uiState.update { it.copy(pendingDeleteExpense = expense) }
    }

    fun dismissDeleteExpense() {
        _uiState.update { it.copy(pendingDeleteExpense = null) }
    }

    fun confirmDeleteExpense() {
        val expense = _uiState.value.pendingDeleteExpense ?: return
        _uiState.update { it.copy(pendingDeleteExpense = null) }
        runMutation(GroupDetailFeedback.EXPENSE_DELETED) {
            repository.deleteExpense(groupId, expense.id)
        }
    }

    fun markTransferAsPaid(transfer: SuggestedTransfer, note: String? = null) {
        val payment = SettlementPayment(
            id = "stl_${UUID.randomUUID().toString().take(8)}",
            groupId = groupId,
            fromParticipantId = transfer.fromParticipantId,
            toParticipantId = transfer.toParticipantId,
            amountCents = transfer.amountCents,
            paidAtIso = Instant.now().toString(),
            note = note
        )
        _uiState.update { it.copy(activePixTransfer = null) }
        runMutation(GroupDetailFeedback.SETTLEMENT_RECORDED) {
            repository.recordSettlement(groupId, payment)
        }
    }

    fun undoSettlement(settlementId: String) {
        runMutation(GroupDetailFeedback.SETTLEMENT_UNDONE) {
            repository.undoSettlement(groupId, settlementId)
        }
    }

    fun openPixSheet(transfer: SuggestedTransfer) {
        _uiState.update { it.copy(activePixTransfer = transfer) }
    }

    fun dismissPixSheet() {
        _uiState.update { it.copy(activePixTransfer = null) }
    }

    fun openAddParticipantDialog() {
        _uiState.update { it.copy(isAddParticipantDialogOpen = true) }
    }

    fun dismissAddParticipantDialog() {
        _uiState.update { it.copy(isAddParticipantDialogOpen = false) }
    }

    fun addParticipant(name: String, avatarColorHex: Long, pixKey: String?) {
        val currentGroup = _uiState.value.group ?: return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        val newParticipant = Participant(
            id = "p_${UUID.randomUUID().toString().take(8)}",
            name = trimmedName,
            avatarColorHex = avatarColorHex,
            pixKey = pixKey?.trim()?.ifEmpty { null }
        )
        _uiState.update { it.copy(isAddParticipantDialogOpen = false) }
        runMutation(GroupDetailFeedback.PARTICIPANT_ADDED) {
            repository.updateParticipants(groupId, currentGroup.participants + newParticipant)
        }
    }

    fun openEditParticipantPix(participant: Participant) {
        _uiState.update { it.copy(editingParticipant = participant) }
    }

    fun dismissEditParticipantPix() {
        _uiState.update { it.copy(editingParticipant = null) }
    }

    fun updateParticipantPixKey(participantId: String, newPixKey: String, newCity: String) {
        val currentGroup = _uiState.value.group ?: return
        val updated = currentGroup.participants.map { p ->
            if (p.id == participantId) {
                p.copy(
                    pixKey = newPixKey.trim().ifEmpty { null },
                    pixCity = newCity.trim().ifEmpty { "SAO PAULO" }
                )
            } else {
                p
            }
        }
        _uiState.update { it.copy(editingParticipant = null) }
        runMutation(GroupDetailFeedback.PARTICIPANT_UPDATED) {
            repository.updateParticipants(groupId, updated)
        }
    }

    fun attemptRemoveParticipant(participant: Participant) {
        val currentGroup = _uiState.value.group ?: return
        if (!GroupValidationRules.canRemoveParticipant(currentGroup, participant.id)) {
            _uiState.update {
                it.copy(
                    blockedRemovalParticipantName = participant.name,
                    feedback = GroupDetailFeedback.PARTICIPANT_CANNOT_REMOVE_LINKED
                )
            }
            return
        }
        val updated = currentGroup.participants.filterNot { it.id == participant.id }
        runMutation(GroupDetailFeedback.PARTICIPANT_REMOVED) {
            repository.updateParticipants(groupId, updated)
        }
    }

    fun dismissBlockedRemovalDialog() {
        _uiState.update { it.copy(blockedRemovalParticipantName = null) }
    }

    fun consumeFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }

    private fun runMutation(
        successFeedback: GroupDetailFeedback,
        action: suspend () -> Group
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            runCatching { action() }
                .onSuccess { updatedGroup ->
                    applyGroup(updatedGroup, feedback = successFeedback)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            feedback = GroupDetailFeedback.ACTION_FAILED
                        )
                    }
                }
        }
    }

    private fun applyGroup(group: Group, feedback: GroupDetailFeedback? = null) {
        val summary = CashFlowMinimizer.calculateSummary(group)
        _uiState.update {
            it.copy(
                isLoading = false,
                isMutating = false,
                group = group,
                summary = summary,
                loadError = false,
                feedback = feedback ?: it.feedback
            )
        }
    }

    companion object {
        fun factory(groupId: String, repository: GroupRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GroupDetailViewModel(groupId, repository) as T
                }
            }
    }
}
