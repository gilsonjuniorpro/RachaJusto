package com.rachajusto.feature.groups

import com.rachajusto.data.local.FileJsonGroupStorage
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.network.SimulatedLatencyMode
import com.rachajusto.data.repository.MockGroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

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

    private fun createViewModel(): Pair<GroupsViewModel, MockNetworkSimulator> {
        val simulator = MockNetworkSimulator(SimulatedLatencyMode.INSTANT)
        val repo = MockGroupRepository(
            storage = FileJsonGroupStorage(File(tempFolder.root, "groups_vm_test.json")),
            networkSimulator = simulator
        )
        return GroupsViewModel(repo) to simulator
    }

    @Test
    fun init_loadsSeedGroupsWithComputedTotalsAndProgress() = runTest {
        val (vm, _) = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorType)
        assertEquals(3, state.groups.size)
        assertTrue(state.groups.all { it.totalSpentCents > 0L })
    }

    @Test
    fun submitCreateGroup_validatesRequiredFieldsThenCreatesGroup() = runTest {
        val (vm, _) = createViewModel()
        advanceUntilIdle()

        vm.openCreateGroupSheet()
        vm.submitCreateGroup()
        assertEquals(CreateGroupValidationError.GROUP_NAME_REQUIRED, vm.uiState.value.createForm.validationError)

        vm.updateGroupName("Churrasco da Firma")
        vm.submitCreateGroup()
        assertEquals(CreateGroupValidationError.PARTICIPANT_NAME_REQUIRED, vm.uiState.value.createForm.validationError)

        val drafts = vm.uiState.value.createForm.participants
        vm.updateParticipantName(drafts[0].draftId, "João")
        vm.updateParticipantPixKey(drafts[0].draftId, "joao@pix.br")
        vm.updateParticipantName(drafts[1].draftId, "Maria")

        vm.submitCreateGroup()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isCreateSheetVisible)
        assertTrue(state.groupCreatedEvent)
        assertEquals(4, state.groups.size)
        assertEquals("Churrasco da Firma", state.groups.first().name)
    }

    @Test
    fun simulatedNetworkError_setsLoadErrorState() = runTest {
        val (vm, simulator) = createViewModel()
        advanceUntilIdle()

        simulator.setSimulateNextError(true)
        vm.loadGroups()
        advanceUntilIdle()

        assertEquals(GroupsErrorType.LOAD_FAILED, vm.uiState.value.errorType)
    }
}
