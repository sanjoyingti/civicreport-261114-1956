package com.arin.civicreport.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arin.civicreport.data.Repository
import com.arin.civicreport.ui.screens.AuthorityFinderScreen
import com.arin.civicreport.ui.screens.CaptureScreen
import com.arin.civicreport.ui.screens.HomeScreen
import com.arin.civicreport.ui.screens.IncidentSelectorScreen
import com.arin.civicreport.ui.screens.LocationDetailsScreen
import com.arin.civicreport.ui.screens.ReportViewScreen
import com.arin.civicreport.ui.screens.ReviewAndSendScreen
import com.arin.civicreport.ui.screens.SettingsScreen
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs

object Routes {
    const val HOME = "home"
    const val INCIDENT = "incident"
    const val CAPTURE = "capture"
    const val LOCATION = "location"
    const val AUTHORITIES = "authorities"
    const val REVIEW = "review"
    const val REPORT_VIEW = "report"
    const val SETTINGS = "settings"

    fun report(id: Long) = "$REPORT_VIEW/$id"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    repo: Repository,
    prefs: Prefs,
    permissionManager: PermissionManager,
    reportViewModel: ReportFlowViewModel
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                repo = repo,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onReportNew = {
                    reportViewModel.reset()
                    navController.navigate(Routes.INCIDENT)
                },
                onOpenReport = { id -> navController.navigate(Routes.report(id)) }
            )
        }
        composable(Routes.INCIDENT) {
            IncidentSelectorScreen(
                onBack = { navController.popBackStack() },
                onContinue = { types ->
                    reportViewModel.start(types)
                    navController.navigate(Routes.CAPTURE)
                }
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(
                reportViewModel = reportViewModel,
                permissionManager = permissionManager,
                onBack = { navController.popBackStack() },
                onCaptured = {
                    navController.navigate(Routes.LOCATION) {
                        popUpTo(Routes.CAPTURE) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.LOCATION) {
            LocationDetailsScreen(
                reportViewModel = reportViewModel,
                permissionManager = permissionManager,
                onBack = { navController.popBackStack() },
                onConfirmed = { navController.navigate(Routes.AUTHORITIES) }
            )
        }
        composable(Routes.AUTHORITIES) {
            AuthorityFinderScreen(
                repo = repo,
                reportViewModel = reportViewModel,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Routes.REVIEW) }
            )
        }
        composable(Routes.REVIEW) {
            ReviewAndSendScreen(
                repo = repo,
                prefs = prefs,
                reportViewModel = reportViewModel,
                onBack = { navController.popBackStack() },
                onSent = { id ->
                    navController.navigate(Routes.report(id)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
        composable(
            route = "${Routes.REPORT_VIEW}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            ReportViewScreen(
                reportId = entry.arguments?.getLong("id") ?: 0L,
                repo = repo,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                repo = repo,
                prefs = prefs,
                permissionManager = permissionManager,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
