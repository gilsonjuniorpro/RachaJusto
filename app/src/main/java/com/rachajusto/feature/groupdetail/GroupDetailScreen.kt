package com.rachajusto.feature.groupdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rachajusto.R
import com.rachajusto.core.designsystem.components.EmptyStateCard
import com.rachajusto.core.designsystem.components.ShimmerBox
import com.rachajusto.core.designsystem.theme.AvatarColorPalette
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SuggestedTransfer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDataChanged: () -> Unit = {},
    topBarActions: @Composable () -> Unit = {},
    pixSheetContent: @Composable (
        transfer: SuggestedTransfer,
        from: Participant,
        to: Participant,
        onDismiss: () -> Unit,
        onConfirmPaid: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val feedbackMessages = mapOf(
        GroupDetailFeedback.GROUP_UPDATED to stringResource(R.string.feedback_group_updated),
        GroupDetailFeedback.EXPENSE_DELETED to stringResource(R.string.feedback_expense_deleted),
        GroupDetailFeedback.SETTLEMENT_RECORDED to stringResource(R.string.feedback_settlement_recorded),
        GroupDetailFeedback.SETTLEMENT_UNDONE to stringResource(R.string.feedback_settlement_undone),
        GroupDetailFeedback.PARTICIPANT_ADDED to stringResource(R.string.feedback_participant_added),
        GroupDetailFeedback.PARTICIPANT_UPDATED to stringResource(R.string.feedback_participant_updated),
        GroupDetailFeedback.PARTICIPANT_REMOVED to stringResource(R.string.feedback_participant_removed),
        GroupDetailFeedback.PARTICIPANT_CANNOT_REMOVE_LINKED to stringResource(R.string.feedback_participant_cannot_remove),
        GroupDetailFeedback.ACTION_FAILED to stringResource(R.string.error_generic_action)
    )

    LaunchedEffect(uiState.groupDeleted) {
        if (uiState.groupDeleted) {
            onDataChanged()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.feedback) {
        val feedback = uiState.feedback ?: return@LaunchedEffect
        if (feedback != GroupDetailFeedback.ACTION_FAILED &&
            feedback != GroupDetailFeedback.PARTICIPANT_CANNOT_REMOVE_LINKED
        ) {
            onDataChanged()
        }
        snackbarHostState.showSnackbar(feedbackMessages[feedback].orEmpty())
        viewModel.consumeFeedback()
    }

    val shareLabels = ShareSummaryLabels(
        totalSpentLabel = stringResource(R.string.detail_total_spent),
        pendingTransfersHeader = stringResource(R.string.share_pending_transfers_header),
        transferLineFormat = stringResource(R.string.share_transfer_line_format),
        pixKeyPrefix = stringResource(R.string.share_pix_key_prefix),
        pixKeyNotRegistered = stringResource(R.string.participants_no_pix_key),
        allSettledMessage = stringResource(R.string.settlement_all_clear_banner),
        footerLabel = stringResource(R.string.share_footer)
    )
    val shareCopiedMessage = stringResource(R.string.share_summary_copied)
    val shareChooserTitle = stringResource(R.string.participants_share_button)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val titleText = uiState.group?.let { "${it.emoji} ${it.name}" }
                        ?: stringResource(R.string.group_detail_title)
                    Text(
                        text = titleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_close)
                        )
                    }
                },
                actions = {
                    topBarActions()
                    if (uiState.group != null) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = stringResource(R.string.group_menu_more)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.group_action_edit)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Rounded.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.openEditGroupDialog()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.group_action_delete),
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.openDeleteGroupDialog()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState.group != null && uiState.selectedTab == GroupDetailTab.EXPENSES) {
                ExtendedFloatingActionButton(
                    onClick = onAddExpenseClick,
                    icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.detail_fab_new_expense)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isMutating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when {
                uiState.isLoading && uiState.group == null -> {
                    GroupDetailLoadingSkeleton()
                }

                uiState.loadError || uiState.group == null || uiState.summary == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            icon = Icons.Rounded.ErrorOutline,
                            title = stringResource(R.string.error_loading_group_detail),
                            subtitle = stringResource(R.string.error_loading_groups),
                            actionLabel = stringResource(R.string.action_retry),
                            onAction = viewModel::loadGroup
                        )
                    }
                }

                else -> {
                    val group = uiState.group!!
                    val summary = uiState.summary!!

                    PrimaryScrollableTabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        edgePadding = 12.dp
                    ) {
                        GroupDetailTab.entries.forEach { tab ->
                            val labelRes = when (tab) {
                                GroupDetailTab.EXPENSES -> R.string.tab_expenses
                                GroupDetailTab.BALANCES -> R.string.tab_balances
                                GroupDetailTab.SETTLEMENT -> R.string.tab_settlement
                                GroupDetailTab.PARTICIPANTS -> R.string.tab_participants
                            }
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = { Text(stringResource(labelRes)) }
                            )
                        }
                    }

                    when (uiState.selectedTab) {
                        GroupDetailTab.EXPENSES -> ExpensesTabContent(
                            uiState = uiState,
                            onTogglePayerFilter = viewModel::togglePayerFilter,
                            onToggleCategoryFilter = viewModel::toggleCategoryFilter,
                            onSearchQueryChange = viewModel::updateExpenseSearchQuery,
                            onSelectSortOption = viewModel::selectSortOption,
                            onClearFilters = viewModel::clearFilters,
                            onRequestDeleteExpense = viewModel::requestDeleteExpense,
                            onAddExpenseClick = onAddExpenseClick
                        )

                        GroupDetailTab.BALANCES -> BalancesTabContent(
                            group = group,
                            summary = summary,
                            analytics = uiState.analytics
                        )

                        GroupDetailTab.SETTLEMENT -> SettlementTabContent(
                            group = group,
                            summary = summary,
                            isMutating = uiState.isMutating,
                            onPayViaPix = viewModel::openPixSheet,
                            onMarkAsPaid = { transfer -> viewModel.markTransferAsPaid(transfer) },
                            onUndoSettlement = viewModel::undoSettlement
                        )

                        GroupDetailTab.PARTICIPANTS -> ParticipantsTabContent(
                            group = group,
                            onShareSummary = {
                                val summaryText = ShareSummaryFormatter.formatWhatsAppSummary(
                                    group = group,
                                    summary = summary,
                                    labels = shareLabels
                                )
                                copyAndShareSummary(context, summaryText, shareChooserTitle)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(shareCopiedMessage)
                                }
                            },
                            onAddParticipantClick = viewModel::openAddParticipantDialog,
                            onEditPixKeyClick = viewModel::openEditParticipantPix,
                            onRemoveParticipantClick = viewModel::attemptRemoveParticipant
                        )
                    }
                }
            }
        }
    }

    // Delete Expense Confirmation Dialog
    uiState.pendingDeleteExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteExpense,
            title = { Text(stringResource(R.string.dialog_delete_expense_title)) },
            text = {
                Text(stringResource(R.string.dialog_delete_expense_message, expense.title))
            },
            confirmButton = {
                Button(onClick = viewModel::confirmDeleteExpense) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteExpense) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Edit Participant PIX Key Dialog
    uiState.editingParticipant?.let { participant ->
        EditParticipantPixDialog(
            participant = participant,
            onDismiss = viewModel::dismissEditParticipantPix,
            onSave = { pixKey, city ->
                viewModel.updateParticipantPixKey(participant.id, pixKey, city)
            }
        )
    }

    // Add Participant Dialog
    if (uiState.isAddParticipantDialogOpen) {
        AddParticipantDialog(
            onDismiss = viewModel::dismissAddParticipantDialog,
            onAdd = { name, colorHex, pixKey ->
                viewModel.addParticipant(name, colorHex, pixKey)
            }
        )
    }

    // Blocked Participant Removal Warning Dialog (RF05)
    uiState.blockedRemovalParticipantName?.let { name ->
        AlertDialog(
            onDismissRequest = viewModel::dismissBlockedRemovalDialog,
            title = { Text(stringResource(R.string.dialog_blocked_removal_title)) },
            text = {
                Text(stringResource(R.string.dialog_blocked_removal_message, name))
            },
            confirmButton = {
                Button(onClick = viewModel::dismissBlockedRemovalDialog) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Edit Group Info Dialog
    val currentGroup = uiState.group
    if (uiState.isEditGroupDialogOpen && currentGroup != null) {
        var name by remember(currentGroup.id) { mutableStateOf(currentGroup.name) }
        var description by remember(currentGroup.id) { mutableStateOf(currentGroup.description.orEmpty()) }
        var emoji by remember(currentGroup.id) { mutableStateOf(currentGroup.emoji) }

        AlertDialog(
            onDismissRequest = viewModel::dismissEditGroupDialog,
            title = { Text(stringResource(R.string.group_action_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { if (it.length <= 4) emoji = it },
                        label = { Text(stringResource(R.string.create_group_emoji_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.create_group_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.create_group_description_label)) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.updateGroupInfo(name, description, emoji.ifBlank { currentGroup.emoji }) },
                    enabled = name.isNotBlank()
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditGroupDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Delete Group Confirmation Dialog
    if (uiState.isDeleteGroupDialogOpen && currentGroup != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteGroupDialog,
            title = { Text(stringResource(R.string.dialog_delete_group_title)) },
            text = {
                Text(stringResource(R.string.dialog_delete_group_message, currentGroup.name))
            },
            confirmButton = {
                Button(onClick = viewModel::confirmDeleteGroup) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteGroupDialog) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Active PIX Sheet slot
    val activeTransfer = uiState.activePixTransfer
    val group = uiState.group
    if (activeTransfer != null && group != null) {
        val from = group.participants.firstOrNull { it.id == activeTransfer.fromParticipantId }
        val to = group.participants.firstOrNull { it.id == activeTransfer.toParticipantId }
        if (from != null && to != null) {
            pixSheetContent(
                activeTransfer,
                from,
                to,
                viewModel::dismissPixSheet,
                { viewModel.markTransferAsPaid(activeTransfer, note = "PIX") }
            )
        }
    }
}

private fun copyAndShareSummary(context: Context, text: String, chooserTitle: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("RachaJusto Summary", text))

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(chooser) }
}

@Composable
private fun EditParticipantPixDialog(
    participant: Participant,
    onDismiss: () -> Unit,
    onSave: (pixKey: String, city: String) -> Unit
) {
    var pixKey by remember(participant.id) { mutableStateOf(participant.pixKey.orEmpty()) }
    var city by remember(participant.id) { mutableStateOf(participant.pixCity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.participants_edit_pix_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = participant.name,
                    style = MaterialTheme.typography.titleSmall
                )
                OutlinedTextField(
                    value = pixKey,
                    onValueChange = { pixKey = it },
                    label = { Text(stringResource(R.string.participant_pix_optional_label)) },
                    placeholder = { Text(stringResource(R.string.participant_pix_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text(stringResource(R.string.participant_city_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(pixKey, city) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun AddParticipantDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, colorHex: Long, pixKey: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pixKey by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(AvatarColorPalette.first()) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.participants_add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.participant_name_label)) },
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pixKey,
                    onValueChange = { pixKey = it },
                    label = { Text(stringResource(R.string.participant_pix_optional_label)) },
                    placeholder = { Text(stringResource(R.string.participant_pix_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarColorPalette.take(6).forEach { colorHex ->
                        val selected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onAdd(name, selectedColor, pixKey.ifBlank { null })
                    }
                }
            ) {
                Text(stringResource(R.string.create_group_add_participant))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun GroupDetailLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(92.dp), cornerRadius = 20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                ShimmerBox(modifier = Modifier.weight(1f).height(34.dp), cornerRadius = 999.dp)
            }
        }
        repeat(4) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(74.dp), cornerRadius = 18.dp)
        }
    }
}
