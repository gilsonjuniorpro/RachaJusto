package com.rachajusto.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rachajusto.R
import com.rachajusto.core.di.AppContainer
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
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.add_expense_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}
