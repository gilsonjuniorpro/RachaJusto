package com.rachajusto

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rachajusto.core.i18n.AppLanguage
import com.rachajusto.data.network.SimulatedLatencyMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupDetailAndPixInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<RachaJustoApplication>()
        app.appContainer.networkSimulator.setLatencyMode(SimulatedLatencyMode.INSTANT)
        app.appContainer.networkSimulator.setSimulateNextError(false)
        app.appContainer.languageManager.setLanguage(AppLanguage.PT_BR)
    }

    private fun openCamposDoJordaoGroup() {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Fim de Semana em Campos do Jordão")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Fim de Semana em Campos do Jordão").performClick()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Despesas").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun groupDetail_switchesAcrossAllFourTabsAndDisplaysAnalyticsAndSummary() {
        openCamposDoJordaoGroup()

        // Tab 2: Balances + Category Breakdown
        composeRule.onNodeWithText("Saldos").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Gastos por Categoria")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Gastos por Categoria").assertIsDisplayed()

        // Tab 3: Optimized Settlement
        composeRule.onNodeWithText("Acerto Otimizado").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Minimização de Transferências")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Minimização de Transferências").assertIsDisplayed()

        // Tab 4: Participants & Share / Export CSV
        composeRule.onNodeWithText("Participantes").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Compartilhar Resumo")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Exportar CSV").assertIsDisplayed()
    }

    @Test
    fun optimizedSettlement_opensPixQrCodeBottomSheet() {
        openCamposDoJordaoGroup()

        composeRule.onNodeWithText("Acerto Otimizado").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Pagar via PIX").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onAllNodesWithText("Pagar via PIX").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("PIX Copia e Cola").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("PIX Copia e Cola").assertIsDisplayed()
        composeRule.onNodeWithText("Copiar Código PIX").assertIsDisplayed()
    }

    @Test
    fun addExpenseScreen_switchesAcrossAllFourSplitModes() {
        openCamposDoJordaoGroup()

        composeRule.onNodeWithTag("fab_new_expense").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Nova Despesa").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Por Item").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Escanear Comanda (Demo)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Escanear Comanda (Demo)").assertIsDisplayed()

        composeRule.onNodeWithText("Exatos").performClick()
        composeRule.onNodeWithText("Distribuir igual").assertIsDisplayed()

        composeRule.onNodeWithText("Cotas").performClick()
        composeRule.onNodeWithText("Igualitária").performClick()
    }
}
