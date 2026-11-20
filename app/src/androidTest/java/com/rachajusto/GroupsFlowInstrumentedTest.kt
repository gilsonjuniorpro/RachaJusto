package com.rachajusto

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class GroupsFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<RachaJustoApplication>()
        app.appContainer.networkSimulator.setLatencyMode(SimulatedLatencyMode.INSTANT)
        app.appContainer.networkSimulator.setSimulateNextError(false)
        app.appContainer.languageManager.setLanguage(AppLanguage.PT_BR)
    }

    private fun waitForGroupsLoaded() {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Fim de Semana em Campos do Jordão")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeScreen_displaysSeededGroupsAndSupportsStatusFilter() {
        waitForGroupsLoaded()

        composeRule.onNodeWithText("Fim de Semana em Campos do Jordão").assertIsDisplayed()
        composeRule.onNodeWithText("República Apê 402 — Contas do Mês").assertIsDisplayed()

        // Filter to settled groups
        composeRule.onNodeWithText("Quitados").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Churrasco de Aniversário")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Churrasco de Aniversário").assertIsDisplayed()

        // Switch back to All
        composeRule.onNodeWithText("Todos").performClick()
        composeRule.onNodeWithText("Fim de Semana em Campos do Jordão").assertIsDisplayed()
    }

    @Test
    fun languageSwitcher_updatesUiBetweenPortugueseAndEnglish() {
        waitForGroupsLoaded()

        composeRule.onNodeWithText("PT").performClick()
        composeRule.onNodeWithText("English").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("My Groups").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("My Groups").assertIsDisplayed()

        // Restore Portuguese (Brazil)
        composeRule.onNodeWithText("EN").performClick()
        composeRule.onNodeWithText("Português (Brasil)").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Meus Grupos").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun createGroupFab_opensCreateGroupBottomSheet() {
        waitForGroupsLoaded()

        composeRule.onNodeWithTag("fab_new_group").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Ícone do grupo").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Ícone do grupo").assertIsDisplayed()
    }
}
