package com.rachajusto.feature.addexpense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rachajusto.R
import com.rachajusto.core.designsystem.components.EmptyStateCard
import com.rachajusto.core.designsystem.components.MoneyInputField
import com.rachajusto.core.designsystem.components.ShimmerBox
import com.rachajusto.core.i18n.LocalAppLanguage
import com.rachajusto.core.util.CurrencyFormatter
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.SplitMode
import com.rachajusto.feature.groupdetail.icon
import com.rachajusto.feature.groupdetail.labelRes
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel,
    onNavigateBack: () -> Unit,
    onExpenseSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val locale = LocalAppLanguage.current.locale
    var showDatePicker by remember { mutableStateOf(false) }

    val saveErrorMsg = stringResource(R.string.error_saving_expense)
    val retryLabel = stringResource(R.string.action_retry)

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onExpenseSaved()
        }
    }

    LaunchedEffect(uiState.saveError) {
        if (uiState.saveError) {
            val result = snackbarHostState.showSnackbar(
                message = saveErrorMsg,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Long
            )
            viewModel.consumeSaveError()
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.saveExpense()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_expense_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_close)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading && !uiState.loadError) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.add_expense_grand_total_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatBrl(uiState.effectiveTotalCents),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = viewModel::saveExpense,
                            enabled = uiState.canSave,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(stringResource(R.string.add_expense_save_button))
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    repeat(5) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(56.dp))
                    }
                }
            }

            uiState.loadError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        icon = Icons.Rounded.ErrorOutline,
                        title = stringResource(R.string.error_loading_group_detail),
                        subtitle = stringResource(R.string.error_loading_groups),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = viewModel::loadGroupParticipants
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text(stringResource(R.string.add_expense_description_label)) },
                        placeholder = { Text(stringResource(R.string.add_expense_description_placeholder)) },
                        singleLine = true,
                        isError = uiState.validationError == AddExpenseValidationError.TITLE_REQUIRED,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Split Mode selector (4 modes)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.add_expense_split_mode_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                SplitMode.EQUAL,
                                SplitMode.ITEMIZED,
                                SplitMode.EXACT,
                                SplitMode.SHARES
                            ).forEach { mode ->
                                FilterChip(
                                    selected = uiState.splitMode == mode,
                                    onClick = { viewModel.selectSplitMode(mode) },
                                    label = { Text(stringResource(mode.labelRes())) }
                                )
                            }
                        }
                    }

                    // Total amount field (auto-computed in ITEMIZED mode)
                    if (uiState.splitMode != SplitMode.ITEMIZED) {
                        MoneyInputField(
                            amountCents = uiState.totalAmountCents,
                            onAmountCentsChange = viewModel::updateTotalAmountCents,
                            label = stringResource(R.string.add_expense_total_amount_label),
                            isError = uiState.validationError == AddExpenseValidationError.AMOUNT_REQUIRED,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Payer selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.add_expense_paid_by_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.participants.forEach { participant ->
                                FilterChip(
                                    selected = uiState.paidById == participant.id,
                                    onClick = { viewModel.selectPayer(participant.id) },
                                    label = { Text(participant.name) }
                                )
                            }
                        }
                    }

                    // Date selector (Material 3 DatePicker)
                    OutlinedCard(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.add_expense_date_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatDate(uiState.date, locale),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Category selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.add_expense_category_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpenseCategory.entries.forEach { category ->
                                FilterChip(
                                    selected = uiState.category == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = category.icon(),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(stringResource(category.labelRes())) }
                                )
                            }
                        }
                    }

                    // Mode-specific split editor
                    when (uiState.splitMode) {
                        SplitMode.EQUAL -> EqualSplitEditor(
                            participants = uiState.participants,
                            selectedIds = uiState.equalSelectedIds,
                            previewByParticipant = uiState.previewSplitsByParticipant,
                            onToggleParticipant = viewModel::toggleEqualParticipant,
                            onSelectAll = viewModel::selectAllEqualParticipants
                        )

                        SplitMode.ITEMIZED -> ItemizedSplitEditor(
                            uiState = uiState,
                            onAddLineItem = viewModel::addLineItem,
                            onRemoveLineItem = viewModel::removeLineItem,
                            onUpdateItemName = viewModel::updateLineItemName,
                            onUpdateItemAmount = viewModel::updateLineItemAmount,
                            onToggleItemParticipant = viewModel::toggleLineItemParticipant,
                            onServiceFeeChange = viewModel::updateServiceFeePercent,
                            onDiscountChange = viewModel::updateDiscountCents,
                            onScanPreset = viewModel::scanDemoReceipt
                        )

                        SplitMode.EXACT -> ExactSplitEditor(
                            participants = uiState.participants,
                            amountsByParticipant = uiState.exactAmountsByParticipant,
                            remainingCents = uiState.exactRemainingCents,
                            onAmountChange = viewModel::updateExactAmount
                        )

                        SplitMode.SHARES -> SharesSplitEditor(
                            participants = uiState.participants,
                            weightsByParticipant = uiState.shareWeightsByParticipant,
                            previewByParticipant = uiState.previewSplitsByParticipant,
                            onAdjustWeight = viewModel::adjustShareWeight
                        )
                    }

                    uiState.validationError?.let { error ->
                        val msgRes = when (error) {
                            AddExpenseValidationError.TITLE_REQUIRED -> R.string.error_expense_title_required
                            AddExpenseValidationError.AMOUNT_REQUIRED -> R.string.error_expense_amount_required
                            AddExpenseValidationError.NO_PARTICIPANTS_SELECTED -> R.string.error_expense_no_participants
                            AddExpenseValidationError.EXACT_SUM_MISMATCH -> R.string.error_expense_exact_mismatch
                            AddExpenseValidationError.SHARES_EMPTY -> R.string.error_expense_shares_empty
                            AddExpenseValidationError.ITEMS_INVALID -> R.string.error_expense_items_invalid
                        }
                        Text(
                            text = stringResource(msgRes),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.updateDate(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
