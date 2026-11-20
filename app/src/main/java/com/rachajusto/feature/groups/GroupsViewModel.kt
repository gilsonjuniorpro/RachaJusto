package com.rachajusto.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rachajusto.core.designsystem.theme.AvatarColorPalette
import com.rachajusto.domain.engine.CashFlowMinimizer
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

data class GroupCardUiModel(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String?,
    val totalSpentCents: Long,
    val participants: List<Participant>,
    val expenseCount: Int,
    val pendingTransfersCount: Int,
    val progressPercent: Float
)

data class ParticipantDraft(
    val draftId: String = UUID.randomUUID().toString(),
    val name: String = "",
    val avatarColorHex: Long = AvatarColorPalette.first(),
    val pixKey: String = ""
)

data class CreateGroupFormState(
    val name: String = "",
    val description: String = "",
    val emoji: String = "🎉",
    val participants: List<ParticipantDraft> = defaultParticipantDrafts(),
    val validationError: CreateGroupValidationError? = null
)

enum class CreateGroupValidationError {
    GROUP_NAME_REQUIRED,
    MIN_TWO_PARTICIPANTS,
    PARTICIPANT_NAME_REQUIRED
}

enum class GroupsErrorType {
    LOAD_FAILED,
    CREATE_FAILED
}

data class GroupsUiState(
    val isLoading: Boolean = true,
    val groups: List<GroupCardUiModel> = emptyList(),
    val errorType: GroupsErrorType? = null,
    val isCreateSheetVisible: Boolean = false,
    val isSavingGroup: Boolean = false,
    val createForm: CreateGroupFormState = CreateGroupFormState(),
    val groupCreatedEvent: Boolean = false
)

private fun defaultParticipantDrafts(): List<ParticipantDraft> = listOf(
    ParticipantDraft(avatarColorHex = AvatarColorPalette[0]),
    ParticipantDraft(avatarColorHex = AvatarColorPalette[1])
)

class GroupsViewModel(
    private val repository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorType = null) }
            runCatching { repository.listGroups() }
                .onSuccess { groups ->
                    val mapped = groups.map { it.toCardUiModel() }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groups = mapped,
                            errorType = null
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorType = GroupsErrorType.LOAD_FAILED
                        )
                    }
                }
        }
    }

    fun openCreateGroupSheet() {
        _uiState.update {
            it.copy(
                isCreateSheetVisible = true,
                createForm = CreateGroupFormState()
            )
        }
    }

    fun dismissCreateGroupSheet() {
        _uiState.update { it.copy(isCreateSheetVisible = false) }
    }

    fun updateGroupName(name: String) {
        _uiState.update {
            it.copy(createForm = it.createForm.copy(name = name, validationError = null))
        }
    }

    fun updateGroupDescription(description: String) {
        _uiState.update {
            it.copy(createForm = it.createForm.copy(description = description))
        }
    }

    fun selectEmoji(emoji: String) {
        _uiState.update {
            it.copy(createForm = it.createForm.copy(emoji = emoji))
        }
    }

    fun addParticipantDraft() {
        _uiState.update { state ->
            val nextColor = AvatarColorPalette[state.createForm.participants.size % AvatarColorPalette.size]
            val updated = state.createForm.participants + ParticipantDraft(avatarColorHex = nextColor)
            state.copy(createForm = state.createForm.copy(participants = updated, validationError = null))
        }
    }

    fun removeParticipantDraft(draftId: String) {
        _uiState.update { state ->
            if (state.createForm.participants.size <= 2) return@update state
            val updated = state.createForm.participants.filterNot { it.draftId == draftId }
            state.copy(createForm = state.createForm.copy(participants = updated, validationError = null))
        }
    }

    fun updateParticipantName(draftId: String, name: String) {
        _uiState.update { state ->
            val updated = state.createForm.participants.map {
                if (it.draftId == draftId) it.copy(name = name) else it
            }
            state.copy(createForm = state.createForm.copy(participants = updated, validationError = null))
        }
    }

    fun updateParticipantPixKey(draftId: String, pixKey: String) {
        _uiState.update { state ->
            val updated = state.createForm.participants.map {
                if (it.draftId == draftId) it.copy(pixKey = pixKey) else it
            }
            state.copy(createForm = state.createForm.copy(participants = updated))
        }
    }

    fun updateParticipantColor(draftId: String, colorHex: Long) {
        _uiState.update { state ->
            val updated = state.createForm.participants.map {
                if (it.draftId == draftId) it.copy(avatarColorHex = colorHex) else it
            }
            state.copy(createForm = state.createForm.copy(participants = updated))
        }
    }

    fun submitCreateGroup() {
        val form = _uiState.value.createForm
        val trimmedName = form.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update {
                it.copy(createForm = form.copy(validationError = CreateGroupValidationError.GROUP_NAME_REQUIRED))
            }
            return
        }

        if (form.participants.size < 2) {
            _uiState.update {
                it.copy(createForm = form.copy(validationError = CreateGroupValidationError.MIN_TWO_PARTICIPANTS))
            }
            return
        }

        if (form.participants.any { it.name.isBlank() }) {
            _uiState.update {
                it.copy(createForm = form.copy(validationError = CreateGroupValidationError.PARTICIPANT_NAME_REQUIRED))
            }
            return
        }

        val newGroup = Group(
            id = "grp_${UUID.randomUUID().toString().take(8)}",
            name = trimmedName,
            description = form.description.trim().ifEmpty { null },
            emoji = form.emoji,
            participants = form.participants.mapIndexed { idx, draft ->
                Participant(
                    id = "p_${UUID.randomUUID().toString().take(6)}_$idx",
                    name = draft.name.trim(),
                    avatarColorHex = draft.avatarColorHex,
                    pixKey = draft.pixKey.trim().ifEmpty { null }
                )
            },
            expenses = emptyList(),
            settlements = emptyList(),
            createdAtIso = Instant.now().toString()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGroup = true, errorType = null) }
            runCatching {
                repository.createGroup(newGroup)
                repository.listGroups()
            }.onSuccess { groups ->
                _uiState.update {
                    it.copy(
                        isSavingGroup = false,
                        isCreateSheetVisible = false,
                        groups = groups.map { g -> g.toCardUiModel() },
                        groupCreatedEvent = true
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isSavingGroup = false,
                        errorType = GroupsErrorType.CREATE_FAILED
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorType = null) }
    }

    fun consumeGroupCreatedEvent() {
        _uiState.update { it.copy(groupCreatedEvent = false) }
    }

    private fun Group.toCardUiModel(): GroupCardUiModel {
        val summary = CashFlowMinimizer.calculateSummary(this)
        return GroupCardUiModel(
            id = id,
            emoji = emoji,
            name = name,
            description = description,
            totalSpentCents = expenses.sumOf { it.totalAmountCents },
            participants = participants,
            expenseCount = expenses.size,
            pendingTransfersCount = summary.optimizedTransfers.size,
            progressPercent = summary.progressPercent
        )
    }

    companion object {
        fun factory(repository: GroupRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return GroupsViewModel(repository) as T
                }
            }
    }
}
