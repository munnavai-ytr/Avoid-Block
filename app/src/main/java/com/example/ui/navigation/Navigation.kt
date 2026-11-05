package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.BlockedPageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PinSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.MainViewModel

object PureLockRoutes {
    const val HOME = "home"
    const val PIN_SETUP = "pin_setup"
    const val BLOCKED_PAGE = "blocked_page"
    const val SETTINGS = "settings"
}

@Composable
fun PureLockNavHost(
    viewModel: MainViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PureLockRoutes.HOME
    ) {
        composable(PureLockRoutes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(PureLockRoutes.SETTINGS) },
                onNavigateToBlockedPage = { navController.navigate(PureLockRoutes.BLOCKED_PAGE) },
                onNavigateToPinSetup = { navController.navigate(PureLockRoutes.PIN_SETUP) }
            )
        }

        composable(PureLockRoutes.PIN_SETUP) {
            PinSetupScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(PureLockRoutes.BLOCKED_PAGE) {
            BlockedPageScreen(
                viewModel = viewModel,
                onReturnToSafety = { navController.popBackStack() }
            )
        }

        composable(PureLockRoutes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPinSetup = { navController.navigate(PureLockRoutes.PIN_SETUP) }
            )
        }
    }
}
