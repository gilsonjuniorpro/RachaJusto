package com.rachajusto.feature.addexpense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.rachajusto.R
import com.rachajusto.core.designsystem.components.MoneyInputField
import com.rachajusto.core.designsystem.components.ParticipantAvatar
import com.rachajusto.core.designsystem.theme.LocalSemanticColors
import com.rachajusto.core.util.CurrencyFormatter
import com.rachajusto.domain.model.DemoReceiptPreset
import com.rachajusto.domain.model.Participant

@Composable
fun EqualSplitEditor(
    participants: List<Participant>,
    selectedIds: Set<String>,
    previewByParticipant: Map<String, Long>,
    onToggleParticipant: (String) -> Unit,
    onSelectAll: () -> Unit
) {
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_expense_equal_header, selectedIds.size),
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.add_expense_select_all))
                }
            }

            participants.forEach { participant ->
                val checked = participant.id in selectedIds
                val shareCents = previewByParticipant[participant.id] ?: 0L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleParticipant(participant.id) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggleParticipant(participant.id) }
                    )
                    ParticipantAvatar(
                        name = participant.name,
                        colorHex = participant.avatarColorHex,
                        size = 32.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (checked) CurrencyFormatter.formatBrl(shareCents) else "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExactSplitEditor(
    participants: List<Participant>,
    amountsByParticipant: Map<String, Long>,
    remainingCents: Long,
    onAmountChange: (String, Long) -> Unit,
    onDistributeEqually: () -> Unit = {}
) {
    val semanticColors = LocalSemanticColors.current
    val isBalanced = remainingCents == 0L

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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isBalanced) semanticColors.positiveContainer else semanticColors.negativeContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remainingCents >= 0L) {
                            stringResource(
                                R.string.add_expense_exact_remaining,
                                CurrencyFormatter.formatBrl(remainingCents)
                            )
                        } else {
                            stringResource(
                                R.string.add_expense_exact_exceeded,
                                CurrencyFormatter.formatBrl(-remainingCents)
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isBalanced) semanticColors.positive else semanticColors.negative
                    )
                    TextButton(onClick = onDistributeEqually) {
                        Text(stringResource(R.string.add_expense_distribute_equally))
                    }
                }
            }

            participants.forEach { participant ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ParticipantAvatar(
                        name = participant.name,
                        colorHex = participant.avatarColorHex,
                        size = 36.dp
                    )
                    MoneyInputField(
                        amountCents = amountsByParticipant[participant.id] ?: 0L,
                        onAmountCentsChange = { onAmountChange(participant.id, it) },
                        label = participant.name,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SharesSplitEditor(
    participants: List<Participant>,
    weightsByParticipant: Map<String, Int>,
    previewByParticipant: Map<String, Long>,
    onAdjustWeight: (String, Int) -> Unit
) {
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.add_expense_shares_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            participants.forEach { participant ->
                val weight = weightsByParticipant[participant.id] ?: 1
                val preview = previewByParticipant[participant.id] ?: 0L
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ParticipantAvatar(
                        name = participant.name,
                        colorHex = participant.avatarColorHex,
                        size = 36.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = CurrencyFormatter.formatBrl(preview),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onAdjustWeight(participant.id, -1) },
                            enabled = weight > 0
                        ) {
                            Icon(imageVector = Icons.Rounded.Remove, contentDescription = null)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${weight}x",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        IconButton(
                            onClick = { onAdjustWeight(participant.id, +1) },
                            enabled = weight < 20
                        ) {
                            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemizedSplitEditor(
    uiState: AddExpenseUiState,
    onAddLineItem: () -> Unit,
    onRemoveLineItem: (String) -> Unit,
    onUpdateItemName: (String, String) -> Unit,
    onUpdateItemAmount: (String, Long) -> Unit,
    onToggleItemParticipant: (String, String) -> Unit,
    onServiceFeeChange: (Int) -> Unit,
    onDiscountChange: (Long) -> Unit,
    onScanPreset: (DemoReceiptPreset) -> Unit
) {
    var showPresets by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Demo Receipt Scanner card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.add_expense_scan_demo_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.add_expense_scan_demo_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }

                if (uiState.isScanningReceipt) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.add_expense_scanning_receipt),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = { showPresets = !showPresets },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.add_expense_scan_demo_button))
                    }

                    if (showPresets) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DemoReceiptPreset.entries.forEach { preset ->
                                val labelRes = when (preset) {
                                    DemoReceiptPreset.BAR_COMANDA -> R.string.receipt_preset_bar
                                    DemoReceiptPreset.CHURRASCO_MERCADO -> R.string.receipt_preset_bbq
                                    DemoReceiptPreset.RESTAURANTE_FONDUE -> R.string.receipt_preset_fondue
                                }
                                OutlinedButton(
                                    onClick = {
                                        showPresets = false
                                        onScanPreset(preset)
                                    }
                                ) {
                                    Text(stringResource(labelRes))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Line items
        uiState.lineItems.forEachIndexed { index, item ->
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.add_expense_item_number, index + 1),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (uiState.lineItems.size > 1) {
                            IconButton(
                                onClick = { onRemoveLineItem(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { onUpdateItemName(item.id, it) },
                            label = { Text(stringResource(R.string.add_expense_item_name_label)) },
                            placeholder = { Text(stringResource(R.string.add_expense_item_name_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        MoneyInputField(
                            amountCents = item.amountCents,
                            onAmountCentsChange = { onUpdateItemAmount(item.id, it) },
                            label = stringResource(R.string.add_expense_amount_short),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = stringResource(R.string.add_expense_item_consumers_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.participants.forEach { participant ->
                            val selected = participant.id in item.participantIds
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleItemParticipant(item.id, participant.id) },
                                label = { Text(participant.name) }
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onAddLineItem,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.add_expense_add_item_button))
        }

        // Service fee & discount card
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
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_expense_service_fee_label, uiState.serviceFeePercent),
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 10, 12, 15).forEach { pct ->
                        FilterChip(
                            selected = uiState.serviceFeePercent == pct,
                            onClick = { onServiceFeeChange(pct) },
                            label = { Text("$pct%") }
                        )
                    }
                }
                MoneyInputField(
                    amountCents = uiState.discountCents,
                    onAmountCentsChange = onDiscountChange,
                    label = stringResource(R.string.add_expense_discount_label),
                    modifier = Modifier.fillMaxWidth()
                )

                uiState.itemizedCalculation?.let { calc ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.add_expense_subtotal_label), style = MaterialTheme.typography.bodySmall)
                        Text(CurrencyFormatter.formatBrl(calc.subtotalCents), style = MaterialTheme.typography.bodySmall)
                    }
                    if (calc.serviceFeeCents > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.add_expense_service_fee_label, uiState.serviceFeePercent),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "+${CurrencyFormatter.formatBrl(calc.serviceFeeCents)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (calc.discountCents > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.add_expense_discount_label), style = MaterialTheme.typography.bodySmall)
                            Text(
                                "-${CurrencyFormatter.formatBrl(calc.discountCents)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.add_expense_grand_total_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            CurrencyFormatter.formatBrl(calc.grandTotalCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.expense_splits_breakdown),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    calc.splits.forEach { split ->
                        val name = uiState.participants.firstOrNull { it.id == split.participantId }?.name ?: "?"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, style = MaterialTheme.typography.bodySmall)
                            Text(
                                CurrencyFormatter.formatBrl(split.amountCents),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
