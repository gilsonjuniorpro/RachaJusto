package com.rachajusto.data.repository

import com.rachajusto.data.local.FileJsonGroupStorage
import com.rachajusto.data.network.MockNetworkSimulator
import com.rachajusto.data.network.SimulatedLatencyMode
import com.rachajusto.data.network.SimulatedNetworkException
import com.rachajusto.data.seed.SeedDataFactory
import com.rachajusto.domain.engine.CashFlowMinimizer
import com.rachajusto.domain.engine.SplitCalculator
import com.rachajusto.domain.model.Expense
import com.rachajusto.domain.model.ExpenseCategory
import com.rachajusto.domain.model.SettlementPayment
import com.rachajusto.domain.model.SplitMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class MockGroupRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(file: File): Pair<MockGroupRepository, MockNetworkSimulator> {
        val simulator = MockNetworkSimulator(SimulatedLatencyMode.INSTANT)
        val storage = FileJsonGroupStorage(file)
        val repo = MockGroupRepository(
            storage = storage,
            networkSimulator = simulator,
            seedProvider = { SeedDataFactory.createSeedGroups(LocalDate.of(2026, 11, 20)) }
        )
        return repo to simulator
    }

    @Test
    fun initialLoad_seedsTwoGroupsWithZeroSumBalanceInvariant() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo, _) = createRepository(file)

        val groups = repo.listGroups()
        assertEquals(2, groups.size)
        assertTrue(file.exists())

        for (group in groups) {
            val summary = CashFlowMinimizer.calculateSummary(group)
            assertEquals(0L, summary.balances.sumOf { it.netBalanceCents })
        }

        val campos = groups.first { it.id == "grp_campos_do_jordao" }
        assertEquals(6, campos.participants.size)
        assertEquals(7, campos.expenses.size)
        assertEquals(1, campos.settlements.size)

        val republica = groups.first { it.id == "grp_republica_402" }
        assertEquals(3, republica.participants.size)
        assertEquals(4, republica.expenses.size)
    }

    @Test
    fun persistenceRoundTrip_survivesRepositoryRecreation() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo1, _) = createRepository(file)

        val group = repo1.getGroup("grp_republica_402")!!
        val newExpense = Expense(
            id = "exp_new_1",
            groupId = group.id,
            title = "Gás Encanado",
            category = ExpenseCategory.OTHER,
            date = LocalDate.of(2026, 11, 20),
            paidById = group.participants.first().id,
            totalAmountCents = 9000L,
            splitMode = SplitMode.EQUAL,
            splits = SplitCalculator.calculateEqualSplits(9000L, group.participants.map { it.id }),
            createdAtIso = "2026-11-20T12:00:00Z"
        )
        repo1.addExpense(group.id, newExpense)

        // New repository reading same file simulates app restart
        val (repo2, _) = createRepository(file)
        val reloaded = repo2.getGroup("grp_republica_402")
        assertNotNull(reloaded)
        assertEquals(5, reloaded!!.expenses.size)
        assertTrue(reloaded.expenses.any { it.id == "exp_new_1" })
    }

    @Test
    fun recordAndUndoSettlement_recalculatesBalancesAndProgress() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo, _) = createRepository(file)

        val before = repo.getGroup("grp_republica_402")!!
        val summaryBefore = CashFlowMinimizer.calculateSummary(before)
        val firstTransfer = summaryBefore.optimizedTransfers.first()

        val payment = SettlementPayment(
            id = "stl_test_1",
            groupId = before.id,
            fromParticipantId = firstTransfer.fromParticipantId,
            toParticipantId = firstTransfer.toParticipantId,
            amountCents = firstTransfer.amountCents,
            paidAtIso = "2026-11-20T13:00:00Z"
        )
        val afterPay = repo.recordSettlement(before.id, payment)
        val summaryAfterPay = CashFlowMinimizer.calculateSummary(afterPay)

        assertTrue(summaryAfterPay.progressPercent > summaryBefore.progressPercent)
        assertEquals(0L, summaryAfterPay.balances.sumOf { it.netBalanceCents })

        val afterUndo = repo.undoSettlement(before.id, payment.id)
        val summaryAfterUndo = CashFlowMinimizer.calculateSummary(afterUndo)
        assertEquals(summaryBefore.progressPercent, summaryAfterUndo.progressPercent, 0.01f)
    }

    @Test
    fun resetDemoData_restoresInitialSeedState() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo, _) = createRepository(file)

        repo.deleteExpense("grp_republica_402", "exp_rep_1")
        assertEquals(3, repo.getGroup("grp_republica_402")!!.expenses.size)

        repo.resetDemoData()
        assertEquals(4, repo.getGroup("grp_republica_402")!!.expenses.size)
    }

    @Test
    fun networkSimulator_simulatesSingleFailureThenRecovers() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo, simulator) = createRepository(file)

        simulator.setSimulateNextError(true)
        try {
            repo.listGroups()
            fail("Expected SimulatedNetworkException")
        } catch (_: SimulatedNetworkException) {
            // expected
        }

        // Next call succeeds automatically
        val groups = repo.listGroups()
        assertEquals(2, groups.size)
    }

    @Test
    fun updateGroupInfoAndDeleteGroup_persistsChanges() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo, _) = createRepository(file)

        val updated = repo.updateGroupInfo(
            groupId = "grp_republica_402",
            name = "Apê 402 Atualizado",
            description = "Nova descrição",
            emoji = "🏢"
        )
        assertEquals("Apê 402 Atualizado", updated.name)
        assertEquals("🏢", updated.emoji)

        repo.deleteGroup("grp_republica_402")
        assertEquals(1, repo.listGroups().size)
    }

    @Test
    fun updateExpense_replacesTargetExpenseInPlace() = runTest {
        val file = File(tempFolder.root, "rachajusto_v1.json")
        val (repo, _) = createRepository(file)

        val group = repo.getGroup("grp_republica_402")!!
        val original = group.expenses.first { it.id == "exp_rep_3" }
        val edited = original.copy(
            title = "Internet Fibra 1Gbps",
            totalAmountCents = 15000L,
            splits = SplitCalculator.calculateEqualSplits(15000L, group.participants.map { it.id })
        )

        val updatedGroup = repo.updateExpense(group.id, edited)
        val saved = updatedGroup.expenses.first { it.id == "exp_rep_3" }
        assertEquals("Internet Fibra 1Gbps", saved.title)
        assertEquals(15000L, saved.totalAmountCents)
    }
}
