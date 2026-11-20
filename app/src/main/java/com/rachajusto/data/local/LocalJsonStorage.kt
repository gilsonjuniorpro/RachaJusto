package com.rachajusto.data.local

import com.rachajusto.domain.engine.GroupIntegrityAuditor
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.ExpenseLineItem
import com.rachajusto.domain.model.ExpenseSplit
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.model.SplitMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

interface GroupStorage {
    fun readGroups(): List<Group>?
    fun writeGroups(groups: List<Group>)
    fun clear()
}

@Serializable
private data class StorageSchemaV1(
    val version: Int = 1,
    val groups: List<GroupDto>
)

@Serializable
private data class ParticipantDto(
    val id: String,
    val name: String,
    val avatarColorHex: Long,
    val pixKey: String? = null,
    val pixCity: String = "SAO PAULO"
)

@Serializable
private data class ExpenseLineItemDto(
    val id: String,
    val name: String,
    val amountCents: Long,
    val participantIds: List<String>
)

@Serializable
private data class ExpenseSplitDto(
    val participantId: String,
    val amountCents: Long,
    val shareWeight: Int? = null
)

@Serializable
private data class ExpenseDto(
    val id: String,
    val groupId: String,
    val title: String,
    val category: String,
    val dateIso: String,
    val paidById: String,
    val totalAmountCents: Long,
    val serviceFeePercent: Int = 0,
    val splitMode: String,
    val items: List<ExpenseLineItemDto> = emptyList(),
    val splits: List<ExpenseSplitDto>,
    val createdAtIso: String
)

@Serializable
private data class SettlementPaymentDto(
    val id: String,
    val groupId: String,
    val fromParticipantId: String,
    val toParticipantId: String,
    val amountCents: Long,
    val paidAtIso: String,
    val note: String? = null
)

@Serializable
private data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val emoji: String,
    val participants: List<ParticipantDto>,
    val expenses: List<ExpenseDto> = emptyList(),
    val settlements: List<SettlementPaymentDto> = emptyList(),
    val createdAtIso: String
)

class FileJsonGroupStorage(
    private val file: File
) : GroupStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun readGroups(): List<Group>? {
        if (!file.exists()) return null
        return runCatching {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return null
            val schema = json.decodeFromString<StorageSchemaV1>(content)
            schema.groups.map { it.toDomain() }
        }.getOrNull()
    }

    override fun writeGroups(groups: List<Group>) {
        file.parentFile?.mkdirs()
        val payload = json.encodeToString(
            StorageSchemaV1.serializer(),
            StorageSchemaV1(version = 1, groups = groups.map { it.toDto() })
        )
        val tempFile = File(file.parentFile, "${file.name}.tmp")
        tempFile.writeText(payload, Charsets.UTF_8)
        if (!tempFile.renameTo(file)) {
            file.writeText(payload, Charsets.UTF_8)
            tempFile.delete()
        }
    }

    override fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }

    fun exportSnapshotJson(groups: List<Group>): String {
        return json.encodeToString(
            StorageSchemaV1.serializer(),
            StorageSchemaV1(version = 1, groups = groups.map { it.toDto() })
        )
    }

    fun importSnapshotJson(rawJson: String): Result<List<Group>> = runCatching {
        val schema = json.decodeFromString<StorageSchemaV1>(rawJson)
        val parsed = schema.groups.map { it.toDomain() }
        for (group in parsed) {
            val report = GroupIntegrityAuditor.audit(group)
            require(report.isValid) {
                "Imported group ${group.id} failed integrity check: ${report.issues}"
            }
        }
        writeGroups(parsed)
        parsed
    }

    private fun ParticipantDto.toDomain() = Participant(
        id = id,
        name = name,
        avatarColorHex = avatarColorHex,
        pixKey = pixKey,
        pixCity = pixCity
    )

    private fun Participant.toDto() = ParticipantDto(
        id = id,
        name = name,
        avatarColorHex = avatarColorHex,
        pixKey = pixKey,
        pixCity = pixCity
    )

    private fun ExpenseLineItemDto.toDomain() = ExpenseLineItem(
        id = id,
        name = name,
        amountCents = amountCents,
        participantIds = participantIds
    )

    private fun ExpenseLineItem.toDto() = ExpenseLineItemDto(
        id = id,
        name = name,
        amountCents = amountCents,
        participantIds = participantIds
    )

    private fun ExpenseSplitDto.toDomain() = ExpenseSplit(
        participantId = participantId,
        amountCents = amountCents,
        shareWeight = shareWeight
    )

    private fun ExpenseSplit.toDto() = ExpenseSplitDto(
        participantId = participantId,
        amountCents = amountCents,
        shareWeight = shareWeight
    )

    private fun ExpenseDto.toDomain() = Expense(
        id = id,
        groupId = groupId,
        title = title,
        category = ExpenseCategory.entries.firstOrNull { it.name == category } ?: ExpenseCategory.OTHER,
        date = LocalDate.parse(dateIso),
        paidById = paidById,
        totalAmountCents = totalAmountCents,
        serviceFeePercent = serviceFeePercent,
        splitMode = SplitMode.entries.firstOrNull { it.name == splitMode } ?: SplitMode.EQUAL,
        items = items.map { it.toDomain() },
        splits = splits.map { it.toDomain() },
        createdAtIso = createdAtIso
    )

    private fun Expense.toDto() = ExpenseDto(
        id = id,
        groupId = groupId,
        title = title,
        category = category.name,
        dateIso = date.toString(),
        paidById = paidById,
        totalAmountCents = totalAmountCents,
        serviceFeePercent = serviceFeePercent,
        splitMode = splitMode.name,
        items = items.map { it.toDto() },
        splits = splits.map { it.toDto() },
        createdAtIso = createdAtIso
    )

    private fun SettlementPaymentDto.toDomain() = SettlementPayment(
        id = id,
        groupId = groupId,
        fromParticipantId = fromParticipantId,
        toParticipantId = toParticipantId,
        amountCents = amountCents,
        paidAtIso = paidAtIso,
        note = note
    )

    private fun SettlementPayment.toDto() = SettlementPaymentDto(
        id = id,
        groupId = groupId,
        fromParticipantId = fromParticipantId,
        toParticipantId = toParticipantId,
        amountCents = amountCents,
        paidAtIso = paidAtIso,
        note = note
    )

    private fun GroupDto.toDomain() = Group(
        id = id,
        name = name,
        description = description,
        emoji = emoji,
        participants = participants.map { it.toDomain() },
        expenses = expenses.map { it.toDomain() },
        settlements = settlements.map { it.toDomain() },
        createdAtIso = createdAtIso
    )

    private fun Group.toDto() = GroupDto(
        id = id,
        name = name,
        description = description,
        emoji = emoji,
        participants = participants.map { it.toDto() },
        expenses = expenses.map { it.toDto() },
        settlements = settlements.map { it.toDto() },
        createdAtIso = createdAtIso
    )

    companion object {
        const val FILE_NAME = "rachajusto_v1.json"
    }
}
