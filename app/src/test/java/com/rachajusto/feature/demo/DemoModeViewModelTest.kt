package com.rachajusto.feature.demo

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DemoModeViewModelTest {

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

    @Test
    fun controlsUpdateNetworkSimulatorAndResetRestoresSeedData() = runTest {
        val simulator = MockNetworkSimulator(SimulatedLatencyMode.INSTANT)
        val repo = MockGroupRepository(
            storage = FileJsonGroupStorage(File(tempFolder.root, "demo_vm_test.json")),
            networkSimulator = simulator
        )
        val vm = DemoModeViewModel(repo, simulator)

        vm.setLatencyMode(SimulatedLatencyMode.SLOW)
        assertEquals(SimulatedLatencyMode.SLOW, simulator.latencyMode.value)

        vm.setLatencyMode(SimulatedLatencyMode.INSTANT)
        vm.setSimulateNextError(true)
        assertTrue(simulator.simulateNextError.value)

        // Reset fails when error simulation is armed
        vm.resetDemoData()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resetFailed)
        assertFalse(simulator.simulateNextError.value)

        // Next reset succeeds
        repo.deleteExpense("grp_republica_402", "exp_rep_1")
        assertEquals(3, repo.getGroup("grp_republica_402")!!.expenses.size)

        vm.resetDemoData()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resetCompleted)
        assertEquals(4, repo.getGroup("grp_republica_402")!!.expenses.size)
    }
}
