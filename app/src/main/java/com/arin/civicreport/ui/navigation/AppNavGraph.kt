package com.arin.civicreport.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.IncidentStatus
import com.arin.civicreport.model.Report as ReportModel
import com.arin.civicreport.ui.screens.HomeScreen
import com.arin.civicreport.ui.screens.IncidentSelectorScreen
import com.arin.civicreport.ui.screens.CaptureScreen
import com.arin.civicreport.ui.screens.LocationDetailsScreen
import com.arin.civicreport.ui.screens.AuthorityFinderScreen
import com.arin.civicreport.ui.screens.ReviewAndSendScreen
import com.arin.civicreport.ui.screens.SettingsScreen
import com.arin.civicreport.ui.screens.ReportViewScreen
import com.arin.civicreport.ui.screens.report.ReportViewModel
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs

@Composable
fun AppNavigation(
    navController: NavHostController,
    repo: Repository,
    prefs: Prefs,
    permissionManager: PermissionManager
) {
    NavHost(navController = navController, startDestination = NavigationRoute.HOME) {
        composable(NavigationRoute.HOME) {
            HomeScreen(
                repo = repo,
                prefs = prefs,
                onOpenSettings = { navController.navigate(NavigationRoute.SETTINGS) },
                onReportNew = {
                    repo.seedAuthorities().let {}
                    navController.navigate(NavigationRoute.INCIDENT_SELECTOR)
                },
                onOpenReport = { id ->
                    navController.navigate("${NavigationRoute.REPORT_VIEW}$id")
                }
            )
        }
        composable(NavigationRoute.INCIDENT_SELECTOR) {
            IncidentSelectorScreen(
                onContinue = { selected ->
                    navController.navigate("${NavigationRoute.CAPTURE}${selected.joinToString("-")}")
                }
            )
        }
        composable(
            route = "${NavigationRoute.CAPTURE}/{types}",
            arguments = listOf(navArgument("types") { type = NavType.StringType })
        ) { backStackEntry ->
            val types = backStackEntry.arguments?.getString("types")?.split("-") ?: emptyList()
            CaptureScreen(
                incidentTypes = types,
                permissionManager = permissionManager,
                onCaptureSuccess = { uri, mediaType ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaUri", uri.toString())
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaType", mediaType)
                    navController.navigate(NavigationRoute.LOCATION_DETAILS)
                },
                onChooseGallery = { uri, mediaType ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaUri", uri.toString())
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaType", mediaType)
                    navController.navigate(NavigationRoute.LOCATION_DETAILS)
                },
                onCameraError = { message -> },
                onRetryCamera = {}
            )
        }
        composable(NavigationRoute.LOCATION_DETAILS) {
            LocationDetailsScreen(
                incidentTypes = navController.previousBackStackEntry?.savedStateHandle?.getString("types")?.split("-") ?: emptyList(),
                mediaUri = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaUri"),
                mediaType = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaType") ?: "photo",
                latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                onLocationConfirmed = { lat, lon, addr ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("lat", lat)
                    navController.previousBackStackEntry?.savedStateHandle?.set("lon", lon)
                    navController.previousBackStackEntry?.savedStateHandle?.set("address", addr)
                    navController.navigate(NavigationRoute.AUTHORITY)
                },
                onRetake = { navController.popBackStack(NavigationRoute.CAPTURE, inclusive = false) }
            )
        }
        composable(NavigationRoute.AUTHORITY) {
            val vm = androidx.lifecycle.viewmodel.compose.viewModel<ReportViewModel>()
            val types = navController.previousBackStackEntry?.savedStateHandle?.getString("types")?.split("-") ?: emptyList()
            AuthorityFinderScreen(
                incidentTypes = types,
                latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                state = "Maharashtra",
                onContinue = { authorities ->
                    vm.updateAuthorities(authorities)
                    navController.navigate(NavigationRoute.REVIEW_SEND)
                }
            )
        }
        composable(NavigationRoute.REVIEW_SEND) {
            val vm = androidx.lifecycle.viewmodel.compose.viewModel<ReportViewModel>()
            val types = navController.previousBackStackEntry?.savedStateHandle?.getString("types")?.split("-") ?: emptyList()
            ReviewAndSendScreen(
                incidentTypes = types,
                mediaUri = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaUri"),
                mediaType = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaType") ?: "photo",
                latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                authorities = vm.reportSelectedAuthorities(),
                sendMethod = prefs.defaultSendMethod,
                onBack = { navController.popBackStack(NavigationRoute.AUTHORITY, inclusive = false) },
                onSaveAndSend = { selectedAuths, method ->
                    vm.updateAuthorities(selectedAuths.map { id -> vm.reportSelectedAuthorities().find { a -> a.id == id }!! })
                    val newReport = ReportModel(
                        id = vm.reportId.value,
                        incidentTypes = types,
                        mediaUris = listOf(navController.previousBackStackEntry?.savedStateHandle?.getString("mediaUri") ?: ""),
                        mediaType = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaType") ?: "photo",
                        timestamp = System.currentTimeMillis(),
                        latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                        longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                        address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                        city = navController.previousBackStackEntry?.savedStateHandle?.getString("city") ?: "",
                        district = navController.previousBackStackEntry?.savedStateHandle?.getString("district") ?: "",
                        state = "Maharashtra",
                        pincode = navController.previousBackStackEntry?.savedStateHandle?.getString("pincode") ?: "",
                        description = navController.previousBackStackEntry?.savedStateHandle?.getString("description") ?: "",
                        status = IncidentStatus.Sent,
                        sentAuthorities = selectedAuths,
                        lastUpdated = System.currentTimeMillis(),
                        sendMethod = method
                    )
                    vm.setReportId(newReport.id)
                    repo.saveReport(newReport)
                    navController.navigate("${NavigationRoute.REPORT_VIEW}${newReport.id}")
                }
            )
        }
        composable(
            route = "${NavigationRoute.REPORT_VIEW}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            ReportViewScreen(
                reportId = id,
                repo = repo,
                prefs = prefs,
                onOpenSettings = { navController.navigate(NavigationRoute.SETTINGS) },
                onBack = { navController.popBackStack() },
                permissionManager = permissionManager
            )
        }
        composable(NavigationRoute.SETTINGS) {
            SettingsScreen(
                repo = repo,
                prefs = prefs,
                onBack = { navController.popBackStack() },
                onRequestPermission = { perm ->
                    permissionManager.requestPermission(perm)
                },
                onAddAuthority = {}
            )
        }
    }
}

object NavigationRoute {
    const val HOME = "home"
    const val INCIDENT_SELECTOR = "incident_selector"
    const val CAPTURE = "capture"
    const val LOCATION_DETAILS = "location_details"
    const val AUTHORITY = "authority"
    const val REVIEW_SEND = "review_send"
    const val REPORT_VIEW = "report_view"
    const val SETTINGS = "settings"
}
