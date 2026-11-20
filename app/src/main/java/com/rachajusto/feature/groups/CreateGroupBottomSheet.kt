package com.rachajusto.feature.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rachajusto.R
import com.rachajusto.core.designsystem.theme.AvatarColorPalette
import com.rachajusto.core.i18n.WithAppLanguage

private val GroupEmojiOptions = listOf(
    "🏔️", "🏠", "🍻", "🏖️", "🍖", "✈️",
    "🍕", "🎉", "🚗", "⚽", "🏕️", "🎶"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    form: CreateGroupFormState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onEmojiSelect: (String) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onParticipantNameChange: (String, String) -> Unit,
    onParticipantPixKeyChange: (String, String) -> Unit,
    onParticipantColorChange: (String, Long) -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        WithAppLanguage {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.create_group_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss, enabled = !isSaving) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }

            // Emoji picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.create_group_emoji_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GroupEmojiOptions.forEach { emoji ->
                        val selected = form.emoji == emoji
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onEmojiSelect(emoji) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.create_group_name_label)) },
                placeholder = { Text(stringResource(R.string.create_group_name_placeholder)) },
                singleLine = true,
                isError = form.validationError == CreateGroupValidationError.GROUP_NAME_REQUIRED,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.create_group_description_label)) },
                placeholder = { Text(stringResource(R.string.create_group_description_placeholder)) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.create_group_participants_label, form.participants.size),
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(onClick = onAddParticipant, enabled = !isSaving) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.create_group_add_participant))
                }
            }

            form.participants.forEachIndexed { index, draft ->
                ParticipantDraftCard(
                    index = index,
                    draft = draft,
                    canRemove = form.participants.size > 2,
                    hasNameError = form.validationError == CreateGroupValidationError.PARTICIPANT_NAME_REQUIRED &&
                        draft.name.isBlank(),
                    onNameChange = { onParticipantNameChange(draft.draftId, it) },
                    onPixKeyChange = { onParticipantPixKeyChange(draft.draftId, it) },
                    onColorSelect = { onParticipantColorChange(draft.draftId, it) },
                    onRemove = { onRemoveParticipant(draft.draftId) }
                )
            }

            form.validationError?.let { error ->
                val message = when (error) {
                    CreateGroupValidationError.GROUP_NAME_REQUIRED ->
                        stringResource(R.string.error_group_name_required)
                    CreateGroupValidationError.MIN_TWO_PARTICIPANTS ->
                        stringResource(R.string.error_min_two_participants)
                    CreateGroupValidationError.PARTICIPANT_NAME_REQUIRED ->
                        stringResource(R.string.error_participant_name_required)
                }
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onSubmit,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.create_group_submit))
                }
            }
        }
        }
    }
}

@Composable
private fun ParticipantDraftCard(
    index: Int,
    draft: ParticipantDraft,
    canRemove: Boolean,
    hasNameError: Boolean,
    onNameChange: (String) -> Unit,
    onPixKeyChange: (String) -> Unit,
    onColorSelect: (Long) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
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
                    text = stringResource(R.string.create_group_participant_number, index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
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

            OutlinedTextField(
                value = draft.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.participant_name_label)) },
                singleLine = true,
                isError = hasNameError,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = draft.pixKey,
                onValueChange = onPixKeyChange,
                label = { Text(stringResource(R.string.participant_pix_optional_label)) },
                placeholder = { Text(stringResource(R.string.participant_pix_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarColorPalette.forEach { colorHex ->
                    val selected = draft.avatarColorHex == colorHex
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
                            .clickable { onColorSelect(colorHex) },
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
    }
}
