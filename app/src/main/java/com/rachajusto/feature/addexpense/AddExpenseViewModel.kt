package com.rachajusto.feature.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rachajusto.domain.engine.ItemizedCalculationResult
import com.rachajusto.domain.engine.SplitCalculator
import com.rachajusto.domain.model.DemoReceiptPreset
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.ExpenseLineItem
import com.rachajusto.domain.model.ExpenseSplit
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SplitMode
import com.rachajusto.domain.repository.GroupRepository
import com.rachajusto.domain.service.ReceiptScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class LineItemDraft(
    val id: String = "item_${UUID.randomUUID().toString().take(6)}",
    val name: String = "",
    val amountCents: Long = 0L,
    val participantIds: Set<String> = emptySet()
)

enum class AddExpenseValidationError {
    TITLE_REQUIRED,
    AMOUNT_REQUIRED,
    NO_PARTICIPANTS_SELECTED,
    EXACT_SUM_MISMATCH,
    SHARES_EMPTY,
    ITEMS_INVALID
}

data class AddExpenseUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isScanningReceipt: Boolean = false,
    val loadError: Boolean = false,
    val saveError: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val participants: List<Participant> = emptyList(),
    val title: String = "",
    val totalAmountCents: Long = 0L,
    val paidById: String = "",
    val date: LocalDate = LocalDate.now(),
    val category: ExpenseCategory = ExpenseCategory.FOOD,
    val splitMode: SplitMode = SplitMode.EQUAL,
    val equalSelectedIds: Set<String> = emptySet(),
    val exactAmountsByParticipant: Map<String, Long> = emptyMap(),
    val shareWeightsByParticipant: Map<String, Int> = emptyMap(),
    val lineItems: List<LineItemDraft> = emptyList(),
    val serviceFeePercent: Int = 0,
    val discountCents: Long = 0L,
    val validationError: AddExpenseValidationError? = null
) {
    val exactRemainingCents: Long
        get() = totalAmountCents - exactAmountsByParticipant.values.sum()

    val itemizedCalculation: ItemizedCalculationResult?
        get() {
            val validItems = lineItems
                .filter { it.amountCents > 0L && it.participantIds.isNotEmpty() }
                .map {
                    ExpenseLineItem(
                        id = it.id,
                        name = it.name.trim().ifEmpty { "Item" },
                        amountCents = it.amountCents,
                        participantIds = it.participantIds.toList()
                    )
                }
            if (validItems.isEmpty()) return null
            return runCatching {
                SplitCalculator.calculateItemizedSplits(
                    items = validItems,
                    serviceFeePercent = serviceFeePercent,
                    discountCents = discountCents
                )
            }.getOrNull()
        }

    val effectiveTotalCents: Long
        get() = if (splitMode == SplitMode.ITEMIZED) {
            itemizedCalculation?.grandTotalCents ?: 0L
        } else {
            totalAmountCents
        }

    val previewSplitsByParticipant: Map<String, Long>
        get() = when (splitMode) {
            SplitMode.EQUAL -> {
                if (totalAmountCents <= 0L || equalSelectedIds.isEmpty()) {
                    emptyMap()
                } else {
                    SplitCalculator.calculateEqualSplits(
                        totalAmountCents,
                        equalSelectedIds.toList()
                    ).associate { it.participantId to it.amountCents }
                }
            }

            SplitMode.EXACT -> exactAmountsByParticipant

            SplitMode.SHARES -> {
                val positive = shareWeightsByParticipant.filterValues { it > 0 }
                if (totalAmountCents <= 0L || positive.isEmpty()) {
                    emptyMap()
                } else {
                    SplitCalculator.calculateShareSplits(
                        totalAmountCents,
                        positive
                    ).associate { it.participantId to it.amountCents }
                }
            }

            SplitMode.ITEMIZED -> {
                itemizedCalculation?.splits?.associate { it.participantId to it.amountCents }
                    ?: emptyMap()
            }
        }

    val canSave: Boolean
        get() {
            if (isLoading || isSaving || isScanningReceipt) return false
            if (title.isBlank() || paidById.isBlank()) return false
            return when (splitMode) {
                SplitMode.EQUAL -> totalAmountCents > 0L && equalSelectedIds.isNotEmpty()
                SplitMode.EXACT -> totalAmountCents > 0L && exactRemainingCents == 0L
                SplitMode.SHARES -> totalAmountCents > 0L && shareWeightsByParticipant.values.any { it > 0 }
                SplitMode.ITEMIZED -> {
                    val calc = itemizedCalculation
                    calc != null && calc.grandTotalCents > 0L &&
                        lineItems.all { it.name.isNotBlank() && it.amountCents > 0L && it.participantIds.isNotEmpty() }
                }
            }
        }
}

class AddExpenseViewModel(
    private val groupId: String,
    private val repository: GroupRepository,
    private val receiptScannerService: ReceiptScannerService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    init {
        loadGroupParticipants()
    }

    fun loadGroupParticipants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = false) }
            runCatching { repository.getGroup(groupId) }
                .onSuccess { group ->
                    if (group == null || group.participants.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, loadError = true) }
                    } else {
                        val allIds = group.participants.map { it.id }.toSet()
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                loadError = false,
                                participants = group.participants,
                                paidById = group.participants.first().id,
                                equalSelectedIds = allIds,
                                exactAmountsByParticipant = group.participants.associate { it.id to 0L },
                                shareWeightsByParticipant = group.participants.associate { it.id to 1 },
                                lineItems = listOf(LineItemDraft(participantIds = allIds))
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadError = true) }
                }
        }
    }

    fun updateTitle(value: String) {
        _uiState.update { it.copy(title = value, validationError = null) }
    }

    fun updateTotalAmountCents(cents: Long) {
        _uiState.update { it.copy(totalAmountCents = cents.coerceAtLeast(0L), validationError = null) }
    }

    fun addQuickAmountCents(deltaCents: Long) {
        _uiState.update {
            it.copy(
                totalAmountCents = (it.totalAmountCents + deltaCents).coerceAtLeast(0L),
                validationError = null
            )
        }
    }

    fun selectPayer(participantId: String) {
        _uiState.update { it.copy(paidById = participantId) }
    }

    fun updateDate(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun selectCategory(category: ExpenseCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun selectSplitMode(mode: SplitMode) {
        _uiState.update { state ->
            val updatedItems = if (mode == SplitMode.ITEMIZED && state.lineItems.isEmpty()) {
                listOf(LineItemDraft(participantIds = state.participants.map { it.id }.toSet()))
            } else {
                state.lineItems
            }
            state.copy(splitMode = mode, lineItems = updatedItems, validationError = null)
        }
    }

    // EQUAL mode
    fun toggleEqualParticipant(participantId: String) {
        _uiState.update { state ->
            val next = state.equalSelectedIds.toMutableSet()
            if (participantId in next) next.remove(participantId) else next.add(participantId)
            state.copy(equalSelectedIds = next, validationError = null)
        }
    }

    fun selectAllEqualParticipants() {
        _uiState.update { state ->
            state.copy(
                equalSelectedIds = state.participants.map { it.id }.toSet(),
                validationError = null
            )
        }
    }

    // EXACT mode
    fun updateExactAmount(participantId: String, amountCents: Long) {
        _uiState.update { state ->
            val updated = state.exactAmountsByParticipant.toMutableMap()
            updated[participantId] = amountCents.coerceAtLeast(0L)
            state.copy(exactAmountsByParticipant = updated, validationError = null)
        }
    }

    fun distributeExactEqually() {
        _uiState.update { state ->
            if (state.totalAmountCents <= 0L || state.participants.isEmpty()) return@update state
            val equalSplits = SplitCalculator.calculateEqualSplits(
                state.totalAmountCents,
                state.participants.map { it.id }
            )
            state.copy(
                exactAmountsByParticipant = equalSplits.associate { it.participantId to it.amountCents },
                validationError = null
            )
        }
    }

    // SHARES mode
    fun adjustShareWeight(participantId: String, delta: Int) {
        _uiState.update { state ->
            val updated = state.shareWeightsByParticipant.toMutableMap()
            val current = updated[participantId] ?: 1
            updated[participantId] = (current + delta).coerceIn(0, 20)
            state.copy(shareWeightsByParticipant = updated, validationError = null)
        }
    }

    // ITEMIZED mode
    fun addLineItem() {
        _uiState.update { state ->
            val allIds = state.participants.map { it.id }.toSet()
            state.copy(
                lineItems = state.lineItems + LineItemDraft(participantIds = allIds),
                validationError = null
            )
        }
    }

    fun removeLineItem(itemId: String) {
        _uiState.update { state ->
            if (state.lineItems.size <= 1) return@update state
            state.copy(
                lineItems = state.lineItems.filterNot { it.id == itemId },
                validationError = null
            )
        }
    }

    fun updateLineItemName(itemId: String, name: String) {
        _uiState.update { state ->
            state.copy(
                lineItems = state.lineItems.map { if (it.id == itemId) it.copy(name = name) else it },
                validationError = null
            )
        }
    }

    fun updateLineItemAmount(itemId: String, amountCents: Long) {
        _uiState.update { state ->
            state.copy(
                lineItems = state.lineItems.map {
                    if (it.id == itemId) it.copy(amountCents = amountCents.coerceAtLeast(0L)) else it
                },
                validationError = null
            )
        }
    }

    fun toggleLineItemParticipant(itemId: String, participantId: String) {
        _uiState.update { state ->
            state.copy(
                lineItems = state.lineItems.map { item ->
                    if (item.id == itemId) {
                        val next = item.participantIds.toMutableSet()
                        if (participantId in next) next.remove(participantId) else next.add(participantId)
                        item.copy(participantIds = next)
                    } else {
                        item
                    }
                },
                validationError = null
            )
        }
    }

    fun updateServiceFeePercent(percent: Int) {
        _uiState.update { it.copy(serviceFeePercent = percent.coerceIn(0, 30)) }
    }

    fun updateDiscountCents(cents: Long) {
        _uiState.update { it.copy(discountCents = cents.coerceAtLeast(0L)) }
    }

    fun scanDemoReceipt(preset: DemoReceiptPreset) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningReceipt = true, validationError = null) }
            runCatching { receiptScannerService.scanPreset(preset) }
                .onSuccess { scanned ->
                    _uiState.update { state ->
                        val allIds = state.participants.map { it.id }.toSet()
                        val scannedDrafts = scanned.items.mapIndexed { idx, item ->
                            // Assign alcoholic drink items to a subset when group has >= 4 people to highlight per-item fairness
                            val isAlcoholItem = item.name.contains("Cerveja", ignoreCase = true) ||
                                item.name.contains("Vinho", ignoreCase = true)
                            val assignedIds = if (isAlcoholItem && state.participants.size >= 4) {
                                state.participants.take(state.participants.size - 1).map { it.id }.toSet()
                            } else {
                                allIds
                            }
                            LineItemDraft(
                                id = "scan_${idx}_${UUID.randomUUID().toString().take(4)}",
                                name = item.name,
                                amountCents = item.amountCents,
                                participantIds = assignedIds
                            )
                        }
                        state.copy(
                            isScanningReceipt = false,
                            splitMode = SplitMode.ITEMIZED,
                            title = scanned.title,
                            category = scanned.category,
                            serviceFeePercent = scanned.serviceFeePercent,
                            discountCents = 0L,
                            lineItems = scannedDrafts
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isScanningReceipt = false) }
                }
        }
    }

    fun saveExpense() {
        val state = _uiState.value
        val trimmedTitle = state.title.trim()
        if (trimmedTitle.isEmpty()) {
            _uiState.update { it.copy(validationError = AddExpenseValidationError.TITLE_REQUIRED) }
            return
        }

        val buildResult = buildExpenseSplits(state)
        if (buildResult.error != null) {
            _uiState.update { it.copy(validationError = buildResult.error) }
            return
        }

        val expense = Expense(
            id = "exp_${UUID.randomUUID().toString().take(8)}",
            groupId = groupId,
            title = trimmedTitle,
            category = state.category,
            date = state.date,
            paidById = state.paidById,
            totalAmountCents = buildResult.totalCents,
            serviceFeePercent = if (state.splitMode == SplitMode.ITEMIZED) state.serviceFeePercent else 0,
            splitMode = state.splitMode,
            items = buildResult.items,
            splits = buildResult.splits,
            createdAtIso = Instant.now().toString()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveError = false) }
            runCatching { repository.addExpense(groupId, expense) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false, saveError = true) }
                }
        }
    }

    fun consumeSaveError() {
        _uiState.update { it.copy(saveError = false) }
    }

    private data class SplitBuildResult(
        val totalCents: Long = 0L,
        val items: List<ExpenseLineItem> = emptyList(),
        val splits: List<ExpenseSplit> = emptyList(),
        val error: AddExpenseValidationError? = null
    )

    private fun buildExpenseSplits(state: AddExpenseUiState): SplitBuildResult {
        return when (state.splitMode) {
            SplitMode.EQUAL -> {
                if (state.totalAmountCents <= 0L) {
                    SplitBuildResult(error = AddExpenseValidationError.AMOUNT_REQUIRED)
                } else if (state.equalSelectedIds.isEmpty()) {
                    SplitBuildResult(error = AddExpenseValidationError.NO_PARTICIPANTS_SELECTED)
                } else {
                    SplitBuildResult(
                        totalCents = state.totalAmountCents,
                        splits = SplitCalculator.calculateEqualSplits(
                            state.totalAmountCents,
                            state.equalSelectedIds.toList()
                        )
                    )
                }
            }

            SplitMode.EXACT -> {
                if (state.totalAmountCents <= 0L) {
                    SplitBuildResult(error = AddExpenseValidationError.AMOUNT_REQUIRED)
                } else if (state.exactRemainingCents != 0L) {
                    SplitBuildResult(error = AddExpenseValidationError.EXACT_SUM_MISMATCH)
                } else {
                    SplitBuildResult(
                        totalCents = state.totalAmountCents,
                        splits = SplitCalculator.calculateExactSplits(
                            state.totalAmountCents,
                            state.exactAmountsByParticipant
                        )
                    )
                }
            }

            SplitMode.SHARES -> {
                val positiveWeights = state.shareWeightsByParticipant.filterValues { it > 0 }
                if (state.totalAmountCents <= 0L) {
                    SplitBuildResult(error = AddExpenseValidationError.AMOUNT_REQUIRED)
                } else if (positiveWeights.isEmpty()) {
                    SplitBuildResult(error = AddExpenseValidationError.SHARES_EMPTY)
                } else {
                    SplitBuildResult(
                        totalCents = state.totalAmountCents,
                        splits = SplitCalculator.calculateShareSplits(
                            state.totalAmountCents,
                            positiveWeights
                        )
                    )
                }
            }

            SplitMode.ITEMIZED -> {
                val invalid = state.lineItems.isEmpty() ||
                    state.lineItems.any { it.name.isBlank() || it.amountCents <= 0L || it.participantIds.isEmpty() }
                if (invalid) {
                    SplitBuildResult(error = AddExpenseValidationError.ITEMS_INVALID)
                } else {
                    val domainItems = state.lineItems.map {
                        ExpenseLineItem(
                            id = it.id,
                            name = it.name.trim(),
                            amountCents = it.amountCents,
                            participantIds = it.participantIds.toList()
                        )
                    }
                    val calc = SplitCalculator.calculateItemizedSplits(
                        items = domainItems,
                        serviceFeePercent = state.serviceFeePercent,
                        discountCents = state.discountCents
                    )
                    if (calc.grandTotalCents <= 0L) {
                        SplitBuildResult(error = AddExpenseValidationError.AMOUNT_REQUIRED)
                    } else {
                        SplitBuildResult(
                            totalCents = calc.grandTotalCents,
                            items = domainItems,
                            splits = calc.splits
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            groupId: String,
            repository: GroupRepository,
            receiptScannerService: ReceiptScannerService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AddExpenseViewModel(groupId, repository, receiptScannerService) as T
            }
        }
    }
}
