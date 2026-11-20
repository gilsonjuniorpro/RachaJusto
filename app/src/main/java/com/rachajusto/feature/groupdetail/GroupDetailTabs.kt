package com.rachajusto.feature.groupdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rachajusto.R
import com.rachajusto.core.designsystem.components.EmptyStateCard
import com.rachajusto.core.designsystem.components.ParticipantAvatar
import com.rachajusto.core.designsystem.theme.LocalSemanticColors
import com.rachajusto.core.i18n.LocalAppLanguage
import com.rachajusto.core.util.CurrencyFormatter
import com.rachajusto.domain.engine.GroupValidationRules
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.ParticipantBalance
import com.rachajusto.domain.model.SettlementSummary
import com.rachajusto.domain.model.SuggestedTransfer
import kotlin.math.roundToInt

@Composable
fun ExpensesTabContent(
    uiState: GroupDetailUiState,
    onTogglePayerFilter: (String?) -> Unit,
    onToggleCategoryFilter: (ExpenseCategory?) -> Unit,
    onClearFilters: () -> Unit,
    onRequestDeleteExpense: (Expense) -> Unit,
    onAddExpenseClick: () -> Unit
) {
    val group = uiState.group ?: return
    val participantsById = remember(group.participants) { group.participants.associateBy { it.id } }
    val locale = LocalAppLanguage.current.locale

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary top banner
        item(key = "expenses_summary") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.detail_total_spent),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = CurrencyFormatter.formatBrl(uiState.totalSpentCents),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.detail_average_per_person),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = CurrencyFormatter.formatBrl(uiState.averagePerPersonCents),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Filters section
        item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedPayerFilterId == null,
                        onClick = { onTogglePayerFilter(null) },
                        label = { Text(stringResource(R.string.filter_all_payers)) }
                    )
                    group.participants.forEach { participant ->
                        FilterChip(
                            selected = uiState.selectedPayerFilterId == participant.id,
                            onClick = { onTogglePayerFilter(participant.id) },
                            label = { Text(participant.name) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedCategoryFilter == null,
                        onClick = { onToggleCategoryFilter(null) },
                        label = { Text(stringResource(R.string.filter_all_categories)) }
                    )
                    ExpenseCategory.entries.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryFilter == category,
                            onClick = { onToggleCategoryFilter(category) },
                            label = { Text(stringResource(category.labelRes())) }
                        )
                    }
                }
            }
        }

        val expenses = uiState.filteredExpenses
        if (expenses.isEmpty()) {
            item(key = "empty_expenses") {
                val hasActiveFilters = uiState.selectedPayerFilterId != null ||
                    uiState.selectedCategoryFilter != null
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = if (hasActiveFilters) {
                        stringResource(R.string.expenses_empty_filtered_title)
                    } else {
                        stringResource(R.string.expenses_empty_title)
                    },
                    subtitle = if (hasActiveFilters) {
                        stringResource(R.string.expenses_empty_filtered_subtitle)
                    } else {
                        stringResource(R.string.expenses_empty_subtitle)
                    },
                    actionLabel = if (hasActiveFilters) {
                        stringResource(R.string.filter_clear)
                    } else {
                        stringResource(R.string.detail_fab_new_expense)
                    },
                    onAction = if (hasActiveFilters) onClearFilters else onAddExpenseClick
                )
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    payer = participantsById[expense.paidById],
                    participantsById = participantsById,
                    dateFormatted = CurrencyFormatter.formatShortDate(expense.date, locale),
                    onDeleteClick = { onRequestDeleteExpense(expense) }
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    payer: Participant?,
    participantsById: Map<String, Participant>,
    dateFormatted: String,
    onDeleteClick: () -> Unit
) {
    var expanded by remember(expense.id) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = expense.category.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            R.string.expense_paid_by_date,
                            payer?.name ?: "?",
                            dateFormatted
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.formatBrl(expense.totalAmountCents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(expense.splitMode.labelRes()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()

                    if (expense.items.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.expense_items_breakdown),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        expense.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${item.name} (${item.participantIds.size})",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = CurrencyFormatter.formatBrl(item.amountCents),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (expense.serviceFeePercent > 0) {
                            Text(
                                text = stringResource(
                                    R.string.expense_service_fee_applied,
                                    expense.serviceFeePercent
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }

                    Text(
                        text = stringResource(R.string.expense_splits_breakdown),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    expense.splits.forEach { split ->
                        val name = participantsById[split.participantId]?.name ?: "?"
                        val weightSuffix = split.shareWeight?.let { " (${it}x)" } ?: ""
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$name$weightSuffix",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = CurrencyFormatter.formatBrl(split.amountCents),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.expense_delete_action),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BalancesTabContent(
    group: Group,
    summary: SettlementSummary
) {
    val participantsById = remember(group.participants) { group.participants.associateBy { it.id } }
    val sortedBalances = remember(summary.balances) {
        summary.balances.sortedByDescending { it.netBalanceCents }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(sortedBalances, key = { it.participantId }) { balance ->
            val participant = participantsById[balance.participantId] ?: return@items
            ParticipantBalanceCard(
                participant = participant,
                balance = balance
            )
        }
    }
}

@Composable
private fun ParticipantBalanceCard(
    participant: Participant,
    balance: ParticipantBalance
) {
    val semanticColors = LocalSemanticColors.current
    val (statusText, statusColor, statusBg) = when {
        balance.netBalanceCents > 0L -> Triple(
            stringResource(R.string.balance_status_receive),
            semanticColors.positive,
            semanticColors.positiveContainer
        )
        balance.netBalanceCents < 0L -> Triple(
            stringResource(R.string.balance_status_owe),
            semanticColors.negative,
            semanticColors.negativeContainer
        )
        else -> Triple(
            stringResource(R.string.balance_status_settled),
            semanticColors.neutral,
            semanticColors.neutralContainer
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParticipantAvatar(
                    name = participant.name,
                    colorHex = participant.avatarColorHex,
                    size = 42.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = CurrencyFormatter.formatBrl(balance.netBalanceCents, showPlusSign = true),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.balance_paid_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatBrl(balance.totalPaidCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.balance_consumed_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatBrl(balance.totalConsumedCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (balance.totalSentCents > 0L || balance.totalReceivedCents > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (balance.totalSentCents > 0L) {
                        Text(
                            text = stringResource(
                                R.string.balance_sent_settlements,
                                CurrencyFormatter.formatBrl(balance.totalSentCents)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (balance.totalReceivedCents > 0L) {
                        Text(
                            text = stringResource(
                                R.string.balance_received_settlements,
                                CurrencyFormatter.formatBrl(balance.totalReceivedCents)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementTabContent(
    group: Group,
    summary: SettlementSummary,
    isMutating: Boolean,
    onPayViaPix: (SuggestedTransfer) -> Unit,
    onMarkAsPaid: (SuggestedTransfer) -> Unit,
    onUndoSettlement: (String) -> Unit
) {
    val participantsById = remember(group.participants) { group.participants.associateBy { it.id } }
    val semanticColors = LocalSemanticColors.current
    var showCompleted by remember { mutableStateOf(true) }

    val savedTransfers = (summary.naiveTransferCount - summary.optimizedTransfers.size).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optimization banner
        item(key = "optimization_banner") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settlement_optimization_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = if (summary.optimizedTransfers.isEmpty()) {
                            stringResource(R.string.settlement_all_clear_banner)
                        } else {
                            stringResource(
                                R.string.settlement_optimization_comparison,
                                summary.naiveTransferCount,
                                summary.optimizedTransfers.size
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (savedTransfers > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.settlement_saved_badge, savedTransfers),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.groups_card_settlement_progress,
                                summary.progressPercent.roundToInt()
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (summary.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Pending transfers
        if (summary.optimizedTransfers.isEmpty()) {
            item(key = "all_settled_empty") {
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = stringResource(R.string.settlement_empty_title),
                    subtitle = stringResource(R.string.settlement_empty_subtitle)
                )
            }
        } else {
            item(key = "pending_header") {
                Text(
                    text = stringResource(
                        R.string.settlement_pending_header,
                        summary.optimizedTransfers.size
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(
                items = summary.optimizedTransfers,
                key = { "${it.fromParticipantId}_${it.toParticipantId}_${it.amountCents}" }
            ) { transfer ->
                val from = participantsById[transfer.fromParticipantId]
                val to = participantsById[transfer.toParticipantId]
                PendingTransferCard(
                    from = from,
                    to = to,
                    transfer = transfer,
                    enabled = !isMutating,
                    onPayViaPix = { onPayViaPix(transfer) },
                    onMarkAsPaid = { onMarkAsPaid(transfer) }
                )
            }
        }

        // Collapsible completed payments section
        if (group.settlements.isNotEmpty()) {
            item(key = "completed_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCompleted = !showCompleted }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.settlement_completed_header,
                            group.settlements.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = semanticColors.positive
                    )
                    Icon(
                        imageVector = if (showCompleted) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            if (showCompleted) {
                items(group.settlements, key = { it.id }) { payment ->
                    val fromName = participantsById[payment.fromParticipantId]?.name ?: "?"
                    val toName = participantsById[payment.toParticipantId]?.name ?: "?"
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(
                                        R.string.settlement_completed_line,
                                        fromName,
                                        CurrencyFormatter.formatBrl(payment.amountCents),
                                        toName
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!payment.note.isNullOrBlank()) {
                                    Text(
                                        text = payment.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = { onUndoSettlement(payment.id) },
                                enabled = !isMutating
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Undo,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.settlement_undo_action))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingTransferCard(
    from: Participant?,
    to: Participant?,
    transfer: SuggestedTransfer,
    enabled: Boolean,
    onPayViaPix: () -> Unit,
    onMarkAsPaid: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ParticipantAvatar(
                    name = from?.name ?: "?",
                    colorHex = from?.avatarColorHex ?: 0xFF0F766E,
                    size = 36.dp
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                ParticipantAvatar(
                    name = to?.name ?: "?",
                    colorHex = to?.avatarColorHex ?: 0xFF2563EB,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.settlement_transfer_sentence,
                            from?.name ?: "?",
                            CurrencyFormatter.formatBrl(transfer.amountCents),
                            to?.name ?: "?"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = to?.pixKey?.let {
                            stringResource(R.string.settlement_pix_key_preview, it)
                        } ?: stringResource(R.string.settlement_pix_demo_fallback),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPayViaPix,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settlement_pay_via_pix))
                }

                OutlinedButton(
                    onClick = onMarkAsPaid,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.settlement_mark_paid))
                }
            }
        }
    }
}

@Composable
fun ParticipantsTabContent(
    group: Group,
    onShareSummary: () -> Unit,
    onAddParticipantClick: () -> Unit,
    onEditPixKeyClick: (Participant) -> Unit,
    onRemoveParticipantClick: (Participant) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Share Summary Card
        item(key = "share_summary_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.participants_share_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.participants_share_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                    Button(
                        onClick = onShareSummary,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.participants_share_button))
                    }
                }
            }
        }

        item(key = "participants_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.create_group_participants_label,
                        group.participants.size
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
                FilledTonalButton(onClick = onAddParticipantClick) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.create_group_add_participant))
                }
            }
        }

        items(group.participants, key = { it.id }) { participant ->
            val canRemove = GroupValidationRules.canRemoveParticipant(group, participant.id)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ParticipantAvatar(
                        name = participant.name,
                        colorHex = participant.avatarColorHex,
                        size = 42.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = participant.pixKey?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.participants_no_pix_key),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = { onEditPixKeyClick(participant) }) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.participants_edit_pix_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { onRemoveParticipantClick(participant) }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = if (canRemove) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }
                }
            }
        }
    }
}
