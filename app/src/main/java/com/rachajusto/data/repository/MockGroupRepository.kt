package com.rachajusto.data.repository

import com.rachajusto.data.local.GroupStorage
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.seed.SeedDataFactory
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.Group
import com.rachajusto.domain.model.Participant
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.repository.GroupRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MockGroupRepository(
    private val storage: GroupStorage,
    private val networkSimulator: MockNetworkSimulator,
    private val seedProvider: () -> List<Group> = { SeedDataFactory.createSeedGroups() }
) : GroupRepository {

    private val mutex = Mutex()
    private var cachedGroups: MutableList<Group>? = null

    private fun ensureLoaded(): MutableList<Group> {
        cachedGroups?.let { return it }
        val loaded = storage.readGroups()?.toMutableList()
            ?: seedProvider().toMutableList().also { storage.writeGroups(it) }
        cachedGroups = loaded
        return loaded
    }

    private fun persist(groups: MutableList<Group>) {
        cachedGroups = groups
        storage.writeGroups(groups)
    }

    override suspend fun listGroups(): List<Group> = networkSimulator.execute {
        mutex.withLock {
            ensureLoaded().toList()
        }
    }

    override suspend fun getGroup(id: String): Group? = networkSimulator.execute {
        mutex.withLock {
            ensureLoaded().firstOrNull { it.id == id }
        }
    }

    override suspend fun createGroup(group: Group): Group = networkSimulator.execute {
        mutex.withLock {
            val groups = ensureLoaded()
            groups.add(0, group)
            persist(groups)
            group
        }
    }

    override suspend fun updateParticipants(
        groupId: String,
        participants: List<Participant>
    ): Group = networkSimulator.execute {
        mutex.withLock {
            updateGroupLocked(groupId) { current ->
                current.copy(participants = participants)
            }
        }
    }

    override suspend fun addExpense(groupId: String, expense: Expense): Group =
        networkSimulator.execute {
            mutex.withLock {
                updateGroupLocked(groupId) { current ->
                    current.copy(expenses = listOf(expense) + current.expenses)
                }
            }
        }

    override suspend fun deleteExpense(groupId: String, expenseId: String): Group =
        networkSimulator.execute {
            mutex.withLock {
                updateGroupLocked(groupId) { current ->
                    current.copy(expenses = current.expenses.filterNot { it.id == expenseId })
                }
            }
        }

    override suspend fun recordSettlement(
        groupId: String,
        payment: SettlementPayment
    ): Group = networkSimulator.execute {
        mutex.withLock {
            updateGroupLocked(groupId) { current ->
                current.copy(settlements = listOf(payment) + current.settlements)
            }
        }
    }

    override suspend fun undoSettlement(
        groupId: String,
        settlementId: String
    ): Group = networkSimulator.execute {
        mutex.withLock {
            updateGroupLocked(groupId) { current ->
                current.copy(settlements = current.settlements.filterNot { it.id == settlementId })
            }
        }
    }

    override suspend fun resetDemoData() = networkSimulator.execute {
        mutex.withLock {
            storage.clear()
            val freshSeed = seedProvider().toMutableList()
            persist(freshSeed)
        }
    }

    private fun updateGroupLocked(groupId: String, transform: (Group) -> Group): Group {
        val groups = ensureLoaded()
        val index = groups.indexOfFirst { it.id == groupId }
        require(index >= 0) { "Group not found: $groupId" }
        val updated = transform(groups[index])
        groups[index] = updated
        persist(groups)
        return updated
    }
}
