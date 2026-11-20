package com.rachajusto.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rachajusto.feature.groupdetail.GroupDetailScreen
import com.rachajusto.feature.groupdetail.GroupDetailViewModel
import com.rachajusto.feature.groups.GroupsListScreen
import com.rachajusto.feature.groups.GroupsViewModel

@Composable
fun RachaJustoNavHost(
    appContainer: AppContainer,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = RachaJustoRoutes.GROUPS
    ) {
        composable(RachaJustoRoutes.GROUPS) { backStackEntry ->
            val groupsViewModel: GroupsViewModel = viewModel(
                factory = GroupsViewModel.factory(appContainer.groupRepository)
            )
            val shouldRefresh = backStackEntry.savedStateHandle.get<Boolean>("refresh_groups") == true
            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh) {
                    backStackEntry.savedStateHandle["refresh_groups"] = false
                    groupsViewModel.loadGroups()
                }
            }
            GroupsListScreen(
                viewModel = groupsViewModel,
                onGroupClick = { groupId ->
                    navController.navigate(RachaJustoRoutes.groupDetail(groupId))
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
            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh) {
                    backStackEntry.savedStateHandle["refresh_detail"] = false
                    detailViewModel.loadGroup()
                    navController.previousBackStackEntry?.savedStateHandle?.set("refresh_groups", true)
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
}
