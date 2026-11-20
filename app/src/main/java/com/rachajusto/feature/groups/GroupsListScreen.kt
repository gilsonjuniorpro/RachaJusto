package com.rachajusto.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rachajusto.R
import com.rachajusto.core.designsystem.components.EmptyStateCard
import com.rachajusto.core.designsystem.components.ParticipantAvatar
import com.rachajusto.core.designsystem.components.ShimmerBox
import com.rachajusto.core.designsystem.theme.LocalSemanticColors
import com.rachajusto.core.util.CurrencyFormatter
import com.rachajusto.domain.engine.GroupStatusFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (String) -> Unit,
    topBarActions: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val loadErrorMsg = stringResource(R.string.error_loading_groups)
    val createErrorMsg = stringResource(R.string.error_creating_group)
    val retryLabel = stringResource(R.string.action_retry)
    val groupCreatedMsg = stringResource(R.string.group_created_success)

    LaunchedEffect(uiState.errorType) {
        val error = uiState.errorType ?: return@LaunchedEffect
        val message = when (error) {
            GroupsErrorType.LOAD_FAILED -> loadErrorMsg
            GroupsErrorType.CREATE_FAILED -> createErrorMsg
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Long
        )
        viewModel.consumeError()
        if (result == SnackbarResult.ActionPerformed) {
            if (error == GroupsErrorType.LOAD_FAILED) {
                viewModel.loadGroups()
            } else {
                viewModel.submitCreateGroup()
            }
        }
    }

    LaunchedEffect(uiState.groupCreatedEvent) {
        if (uiState.groupCreatedEvent) {
            snackbarHostState.showSnackbar(groupCreatedMsg)
            viewModel.consumeGroupCreatedEvent()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.groups_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = { topBarActions() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openCreateGroupSheet,
                modifier = Modifier.testTag("fab_new_group"),
                icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.groups_fab_new)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading && uiState.groups.isEmpty() -> {
                GroupsLoadingSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            !uiState.isLoading && uiState.groups.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Groups,
                        title = stringResource(R.string.groups_empty_title),
                        subtitle = stringResource(R.string.groups_empty_subtitle),
                        actionLabel = stringResource(R.string.groups_fab_new),
                        onAction = viewModel::openCreateGroupSheet
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.isLoading) {
                        item(key = "refresh_bar") {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    item(key = "portfolio_overview") {
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
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.groups_overview_total_tracked),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatBrl(uiState.totalTrackedCents),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = stringResource(R.string.groups_overview_pending_label),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                    Text(
                                        text = uiState.totalPendingTransfers.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    item(key = "search_and_filter") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                placeholder = { Text(stringResource(R.string.groups_search_placeholder)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.action_close)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GroupStatusFilter.entries.forEach { filter ->
                                    val labelRes = when (filter) {
                                        GroupStatusFilter.ALL -> R.string.groups_filter_all
                                        GroupStatusFilter.PENDING -> R.string.groups_filter_pending
                                        GroupStatusFilter.SETTLED -> R.string.groups_filter_settled
                                    }
                                    FilterChip(
                                        selected = uiState.statusFilter == filter,
                                        onClick = { viewModel.selectStatusFilter(filter) },
                                        label = { Text(stringResource(labelRes)) }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.filteredGroups.isEmpty()) {
                        item(key = "empty_filtered") {
                            EmptyStateCard(
                                icon = Icons.Rounded.Search,
                                title = stringResource(R.string.groups_search_empty_title),
                                subtitle = stringResource(R.string.groups_search_empty_subtitle),
                                actionLabel = stringResource(R.string.filter_clear),
                                onAction = viewModel::clearSearchAndFilter
                            )
                        }
                    } else {
                        items(uiState.filteredGroups, key = { it.id }) { group ->
                            GroupSummaryCard(
                                group = group,
                                onClick = { onGroupClick(group.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isCreateSheetVisible) {
        CreateGroupBottomSheet(
            form = uiState.createForm,
            isSaving = uiState.isSavingGroup,
            onDismiss = viewModel::dismissCreateGroupSheet,
            onNameChange = viewModel::updateGroupName,
            onDescriptionChange = viewModel::updateGroupDescription,
            onEmojiSelect = viewModel::selectEmoji,
            onAddParticipant = viewModel::addParticipantDraft,
            onRemoveParticipant = viewModel::removeParticipantDraft,
            onParticipantNameChange = viewModel::updateParticipantName,
            onParticipantPixKeyChange = viewModel::updateParticipantPixKey,
            onParticipantColorChange = viewModel::updateParticipantColor,
            onSubmit = viewModel::submitCreateGroup
        )
    }
}

@Composable
private fun GroupSummaryCard(
    group: GroupCardUiModel,
    onClick: () -> Unit
) {
    val semanticColors = LocalSemanticColors.current
    val progressRatio = (group.progressPercent / 100f).coerceIn(0f, 1f)
    val isFullySettled = group.expenseCount > 0 && group.pendingTransfersCount == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = group.emoji, fontSize = 26.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!group.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.groups_card_total_spent),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatBrl(group.totalSpentCents),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Overlapping participant avatars preview
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        val visibleParticipants = group.participants.take(4)
                        visibleParticipants.forEachIndexed { idx, participant ->
                            ParticipantAvatar(
                                name = participant.name,
                                colorHex = participant.avatarColorHex,
                                size = 26.dp,
                                modifier = Modifier.offset(x = (-6 * idx).dp)
                            )
                        }
                        if (group.participants.size > 4) {
                            Box(
                                modifier = Modifier
                                    .offset(x = (-6 * visibleParticipants.size).dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${group.participants.size - 4}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.groups_card_participants_count,
                            group.participants.size
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Settlement progress section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.groups_card_settlement_progress,
                            group.progressPercent.roundToInt()
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isFullySettled) semanticColors.positive else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isFullySettled) {
                            semanticColors.positiveContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullySettled) {
                                    Icons.Rounded.CheckCircleOutline
                                } else {
                                    Icons.Rounded.SyncAlt
                                },
                                contentDescription = null,
                                tint = if (isFullySettled) {
                                    semanticColors.positive
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isFullySettled) {
                                    stringResource(R.string.groups_card_settled)
                                } else if (group.expenseCount == 0) {
                                    stringResource(R.string.groups_card_no_expenses)
                                } else {
                                    stringResource(
                                        R.string.groups_card_pending_transfers,
                                        group.pendingTransfersCount
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFullySettled) {
                                    semanticColors.positive
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (isFullySettled) semanticColors.positive else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }
    }
}

@Composable
private fun GroupsLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ShimmerBox(modifier = Modifier.size(52.dp), cornerRadius = 16.dp)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(20.dp))
                            ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShimmerBox(modifier = Modifier.width(120.dp).height(26.dp))
                        ShimmerBox(modifier = Modifier.width(88.dp).height(26.dp))
                    }
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(8.dp), cornerRadius = 999.dp)
                }
            }
        }
    }
}
