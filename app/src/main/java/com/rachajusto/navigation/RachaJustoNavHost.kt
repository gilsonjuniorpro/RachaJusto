package com.rachajusto.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rachajusto.core.di.AppContainer
import com.rachajusto.feature.addexpense.AddExpenseScreen
import com.rachajusto.feature.addexpense.AddExpenseViewModel
import com.rachajusto.feature.demo.DemoModeBottomSheet
import com.rachajusto.feature.demo.DemoModeViewModel
import com.rachajusto.feature.demo.TopBarUtilityActions
import com.rachajusto.feature.groupdetail.GroupDetailScreen
import com.rachajusto.feature.groupdetail.GroupDetailViewModel
import com.rachajusto.feature.groups.GroupsListScreen
import com.rachajusto.feature.groups.GroupsViewModel
import com.rachajusto.feature.pix.PixPaymentBottomSheet

@Composable
fun RachaJustoNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    val currentLanguage by appContainer.languageManager.currentLanguage.collectAsStateWithLifecycle()
    val currentThemeMode by appContainer.themePreferenceManager.themeMode.collectAsStateWithLifecycle()
    val isErrorArmed by appContainer.networkSimulator.simulateNextError.collectAsStateWithLifecycle()
    var showDemoSheet by remember { mutableStateOf(false) }
    var dataResetEpoch by remember { mutableIntStateOf(0) }

    val demoModeViewModel: DemoModeViewModel = viewModel(
        factory = DemoModeViewModel.factory(
            repository = appContainer.groupRepository,
            networkSimulator = appContainer.networkSimulator
        )
    )

    NavHost(
        navController = navController,
        startDestination = RachaJustoRoutes.GROUPS
    ) {
        composable(RachaJustoRoutes.GROUPS) { backStackEntry ->
            val groupsViewModel: GroupsViewModel = viewModel(
                factory = GroupsViewModel.factory(appContainer.groupRepository)
            )
            val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_groups") == true
            LaunchedEffect(shouldRefresh, dataResetEpoch) {
                if (shouldRefresh) {
                    backStackEntry.savedStateHandle["refresh_groups"] = false
                    groupsViewModel.loadGroups()
                } else if (dataResetEpoch > 0) {
                    groupsViewModel.loadGroups()
                }
            }
            GroupsListScreen(
                viewModel = groupsViewModel,
                onGroupClick = { groupId ->
                    navController.navigate(RachaJustoRoutes.groupDetail(groupId))
                },
                topBarActions = {
                    TopBarUtilityActions(
                        currentLanguage = currentLanguage,
                        isErrorSimulationArmed = isErrorArmed,
                        onSelectLanguage = appContainer.languageManager::setLanguage,
                        onOpenDemoSheet = { showDemoSheet = true }
                    )
                }
            )
        }

        composable(
            route = RachaJustoRoutes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val detailViewModel: GroupDetailViewModel = viewModel(
                key = "group_detail_$groupId",
                factory = GroupDetailViewModel.factory(groupId, appContainer.groupRepository)
            )
            val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_detail") == true
            LaunchedEffect(shouldRefresh, dataResetEpoch) {
                if (shouldRefresh) {
                    backStackEntry.savedStateHandle["refresh_detail"] = false
                    detailViewModel.loadGroup()
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_groups", true)
                } else if (dataResetEpoch > 0) {
                    detailViewModel.loadGroup()
                }
            }

            GroupDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onAddExpenseClick = {
                    navController.navigate(RachaJustoRoutes.addExpense(groupId))
                },
                onDataChanged = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_groups", true)
                },
                topBarActions = {
                    TopBarUtilityActions(
                        currentLanguage = currentLanguage,
                        isErrorSimulationArmed = isErrorArmed,
                        onSelectLanguage = appContainer.languageManager::setLanguage,
                        onOpenDemoSheet = { showDemoSheet = true }
                    )
                },
                pixSheetContent = { transfer, from, to, groupName, onDismiss, onConfirmPaid ->
                    PixPaymentBottomSheet(
                        transfer = transfer,
                        fromParticipant = from,
                        toParticipant = to,
                        groupName = groupName,
                        onDismiss = onDismiss,
                        onConfirmPayment = onConfirmPaid
                    )
                }
            )
        }

        composable(
            route = RachaJustoRoutes.ADD_EXPENSE,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val addExpenseViewModel: AddExpenseViewModel = viewModel(
                key = "add_expense_$groupId",
                factory = AddExpenseViewModel.factory(
                    groupId = groupId,
                    repository = appContainer.groupRepository,
                    receiptScannerService = appContainer.receiptScannerService
                )
            )

            AddExpenseScreen(
                viewModel = addExpenseViewModel,
                onNavigateBack = { navController.popBackStack() },
                onExpenseSaved = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_detail", true)
                    navController.popBackStack()
                }
            )
        }
    }

    if (showDemoSheet) {
        DemoModeBottomSheet(
            viewModel = demoModeViewModel,
            currentLanguage = currentLanguage,
            currentThemeMode = currentThemeMode,
            onSelectLanguage = appContainer.languageManager::setLanguage,
            onSelectThemeMode = appContainer.themePreferenceManager::setThemeMode,
            onDataReset = {
                dataResetEpoch++
                navController.popBackStack(RachaJustoRoutes.GROUPS, inclusive = false)
            },
            onDismiss = { showDemoSheet = false }
        )
    }
}
