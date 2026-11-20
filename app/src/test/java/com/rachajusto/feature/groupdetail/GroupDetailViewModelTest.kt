package com.rachajusto.feature.groupdetail

import com.rachajusto.data.local.FileJsonGroupStorage
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.network.SimulatedLatencyMode
import com.rachajusto.data.repository.MockGroupRepository
import com.rachajusto.domain.model.ExpenseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(groupId: String = "grp_campos_do_jordao"): GroupDetailViewModel {
        val simulator = MockNetworkSimulator(SimulatedLatencyMode.INSTANT)
        val repo = MockGroupRepository(
            storage = FileJsonGroupStorage(File(tempFolder.root, "detail_vm_test.json")),
            networkSimulator = simulator
        )
        return GroupDetailViewModel(groupId, repo)
    }

    @Test
    fun loadGroup_computesSummaryAndFiltersByPayerAndCategory() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.group)
        assertNotNull(state.summary)
        assertEquals(7, state.filteredExpenses.size)
        assertEquals(0L, state.summary!!.balances.sumOf { it.netBalanceCents })

        vm.togglePayerFilter("p_lucas")
        assertTrue(vm.uiState.value.filteredExpenses.all { it.paidById == "p_lucas" })

        vm.toggleCategoryFilter(ExpenseCategory.TRANSPORT)
        assertEquals(1, vm.uiState.value.filteredExpenses.size)
        assertEquals("Pedágios", vm.uiState.value.filteredExpenses.first().title)

        vm.clearFilters()
        assertEquals(7, vm.uiState.value.filteredExpenses.size)
    }

    @Test
    fun deleteExpense_removesExpenseAndUpdatesBalances() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val target = vm.uiState.value.group!!.expenses.first()
        vm.requestDeleteExpense(target)
        vm.confirmDeleteExpense()
        advanceUntilIdle()

        assertEquals(6, vm.uiState.value.group!!.expenses.size)
        assertEquals(GroupDetailFeedback.EXPENSE_DELETED, vm.uiState.value.feedback)
    }

    @Test
    fun participantManagement_blocksLinkedRemovalAndAllowsUnusedRemoval() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // Lucas has expenses -> blocked
        val lucas = vm.uiState.value.group!!.participants.first { it.id == "p_lucas" }
        vm.attemptRemoveParticipant(lucas)
        assertEquals("Lucas", vm.uiState.value.blockedRemovalParticipantName)
        assertEquals(GroupDetailFeedback.PARTICIPANT_CANNOT_REMOVE_LINKED, vm.uiState.value.feedback)

        // Add unused participant -> allowed to remove
        vm.addParticipant("Marina", 0xFF0F766E, "marina@pix.br")
        advanceUntilIdle()
        assertEquals(7, vm.uiState.value.group!!.participants.size)

        val marina = vm.uiState.value.group!!.participants.first { it.name == "Marina" }
        vm.attemptRemoveParticipant(marina)
        advanceUntilIdle()
        assertEquals(6, vm.uiState.value.group!!.participants.size)
        assertEquals(GroupDetailFeedback.PARTICIPANT_REMOVED, vm.uiState.value.feedback)
    }

    @Test
    fun settlementLifecycle_recordsAndUndoesPayment() = runTest {
        val vm = createViewModel("grp_republica_402")
        advanceUntilIdle()

        val initialTransfer = vm.uiState.value.summary!!.optimizedTransfers.first()
        vm.markTransferAsPaid(initialTransfer)
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.group!!.settlements.size)
        assertEquals(GroupDetailFeedback.SETTLEMENT_RECORDED, vm.uiState.value.feedback)

        val settlementId = vm.uiState.value.group!!.settlements.first().id
        vm.undoSettlement(settlementId)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.group!!.settlements.isEmpty())
        assertEquals(GroupDetailFeedback.SETTLEMENT_UNDONE, vm.uiState.value.feedback)
        assertNull(vm.uiState.value.activePixTransfer)
    }

    @Test
    fun editAndDeleteGroup_updatesMetadataAndFlagsDeletion() = runTest {
        val vm = createViewModel("grp_republica_402")
        advanceUntilIdle()

        vm.updateGroupInfo("República Renovada", "Contas atualizadas", "🏢")
        advanceUntilIdle()

        assertEquals("República Renovada", vm.uiState.value.group!!.name)
        assertEquals("🏢", vm.uiState.value.group!!.emoji)
        assertEquals(GroupDetailFeedback.GROUP_UPDATED, vm.uiState.value.feedback)

        vm.confirmDeleteGroup()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.groupDeleted)
    }
}
