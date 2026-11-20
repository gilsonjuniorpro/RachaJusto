package com.rachajusto.feature.addexpense

import com.rachajusto.data.local.FileJsonGroupStorage
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.network.SimulatedLatencyMode
import com.rachajusto.data.repository.MockGroupRepository
import com.rachajusto.data.service.MockReceiptScannerService
import com.rachajusto.domain.model.DemoReceiptPreset
import com.rachajusto.domain.model.SplitMode
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
class AddExpenseViewModelTest {

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

    private fun createEnv(groupId: String = "grp_republica_402"): Pair<AddExpenseViewModel, MockGroupRepository> {
        val simulator = MockNetworkSimulator(SimulatedLatencyMode.INSTANT)
        val repo = MockGroupRepository(
            storage = FileJsonGroupStorage(File(tempFolder.root, "add_expense_vm_test.json")),
            networkSimulator = simulator
        )
        val scanner = MockReceiptScannerService(scanDelayMs = 0L)
        return AddExpenseViewModel(groupId, repo, scanner) to repo
    }

    @Test
    fun exactSplit_blocksSaveUntilRemainingIsZero() = runTest {
        val (vm, repo) = createEnv()
        advanceUntilIdle()

        vm.updateTitle("Jantar Especial")
        vm.selectSplitMode(SplitMode.EXACT)
        vm.updateTotalAmountCents(10000L)

        val ids = vm.uiState.value.participants.map { it.id }
        vm.updateExactAmount(ids[0], 5000L)
        vm.updateExactAmount(ids[1], 3000L)

        assertEquals(2000L, vm.uiState.value.exactRemainingCents)
        assertFalse(vm.uiState.value.canSave)

        vm.updateExactAmount(ids[2], 2000L)
        assertEquals(0L, vm.uiState.value.exactRemainingCents)
        assertTrue(vm.uiState.value.canSave)

        vm.saveExpense()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        val saved = repo.getGroup("grp_republica_402")!!.expenses.first()
        assertEquals(10000L, saved.splits.sumOf { it.amountCents })
    }

    @Test
    fun itemizedDemoScanner_populatesItemsAndSavesWithServiceFee() = runTest {
        val (vm, repo) = createEnv("grp_campos_do_jordao")
        advanceUntilIdle()

        vm.scanDemoReceipt(DemoReceiptPreset.BAR_COMANDA)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(SplitMode.ITEMIZED, state.splitMode)
        assertEquals(10, state.serviceFeePercent)
        assertEquals(4, state.lineItems.size)
        assertTrue(state.canSave)

        vm.saveExpense()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        val saved = repo.getGroup("grp_campos_do_jordao")!!.expenses.first()
        assertEquals(SplitMode.ITEMIZED, saved.splitMode)
        assertEquals(saved.totalAmountCents, saved.splits.sumOf { it.amountCents })
    }

    @Test
    fun sharesSplit_computesExactProportionalSumOnSave() = runTest {
        val (vm, repo) = createEnv()
        advanceUntilIdle()

        vm.updateTitle("Condomínio")
        vm.selectSplitMode(SplitMode.SHARES)
        vm.updateTotalAmountCents(100000L)

        val ids = vm.uiState.value.participants.map { it.id }
        vm.adjustShareWeight(ids[0], +2) // 3x
        vm.adjustShareWeight(ids[1], +1) // 2x
        // ids[2] stays 1x

        vm.saveExpense()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        val saved = repo.getGroup("grp_republica_402")!!.expenses.first()
        assertEquals(SplitMode.SHARES, saved.splitMode)
        assertEquals(100000L, saved.splits.sumOf { it.amountCents })
    }

    @Test
    fun quickAmountAndDistributeExactEqually_balancesRemainingToZero() = runTest {
        val (vm, _) = createEnv()
        advanceUntilIdle()

        vm.addQuickAmountCents(5000L)
        vm.addQuickAmountCents(5000L)
        assertEquals(10000L, vm.uiState.value.totalAmountCents)

        vm.selectSplitMode(SplitMode.EXACT)
        vm.distributeExactEqually()
        assertEquals(0L, vm.uiState.value.exactRemainingCents)
    }
}
