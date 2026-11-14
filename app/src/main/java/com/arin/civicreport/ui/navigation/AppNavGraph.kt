package com.arin.civicreport.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arin.civicreport.data.Report
import com.arin.civicreport.data.Repository
import com.arin.civicreport.model.Report as ReportModel
import com.arin.civicreport.ui.screens.HomeScreen
import com.arin.civicreport.ui.screens.IncidentSelectorScreen
import com.arin.civicreport.ui.screens.CaptureScreen
import com.arin.civicreport.ui.screens.LocationDetailsScreen
import com.arin.civicreport.ui.screens.AuthorityFinderScreen
import com.arin.civicreport.ui.screens.ReviewAndSendScreen
import com.arin.civicreport.ui.screens.SettingsScreen
import com.arin.civicreport.ui.screens.report.ReportViewModel
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs

@Composable
fun AppNavigation(
    navController: NavHostController,
    repo: Repository,
    prefs: Prefs,
    permissionManager: PermissionManager,
    onGetContentUri: (android.net.Uri) -> android.net.Uri,
    onUriSaved: (java.io.File, String) -> Unit,
    mediaTypeFromUri: (android.net.Uri) -> String
) {
    NavHost(navController = navController, startDestination = NavigationRoute.HOME) {
        composable(NavigationRoute.HOME) {
            HomeScreen(
                repo = repo,
                prefs = prefs,
                onOpenSettings = { navController.navigate(NavigationRoute.SETTINGS) },
                onReportNew = {
                    navController.navigate(NavigationRoute.INCIDENT_SELECTOR)
                    repo.seedAuthorities().let {}
                },
                onOpenReport = { id ->
                    navController.navigate("${NavigationRoute.REPORT_VIEW}$id")
                }
            )
        }
        composable(NavigationRoute.INCIDENT_SELECTOR) {
            IncidentSelectorScreen(
                onContinue = { selected ->
                    navController.navigate("${NavigationRoute.CAPTURE}$selected.joinToString("-"))
                }
            )
        }
        composable(
            route = "${NavigationRoute.CAPTURE}/{types}",
            arguments = listOf(navArgument("types") { type = NavType.StringType })
        ) { backStackEntry ->
            val types = backStackEntry.arguments?.getString("types")?.split("-") ?: emptyList()
            CaptureScreen(
                repo = repo,
                incidentTypes = types,
                permissionManager = permissionManager,
                onCaptureSuccess = { uri, mediaType ->
                    val lat = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0
                    val lon = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0
                    val address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: ""
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaUri", uri.toString())
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaType", mediaType)
                    navController.previousBackStackEntry?.savedStateHandle?.set("lat", lat)
                    navController.previousBackStackEntry?.savedStateHandle?.set("lon", lon)
                    navController.previousBackStackEntry?.savedStateHandle?.set("address", address)
                    navController.navigate(NavigationRoute.LOCATION_DETAILS)
                },
                onChooseGallery = { uri, mediaType ->
                    val lat = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0
                    val lon = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0
                    val address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: ""
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaUri", uri.toString())
                    navController.previousBackStackEntry?.savedStateHandle?.set("mediaType", mediaType)
                    navController.previousBackStackEntry?.savedStateHandle?.set("lat", lat)
                    navController.previousBackStackEntry?.savedStateHandle?.set("lon", lon)
                    navController.previousBackStackEntry?.savedStateHandle?.set("address", address)
                    navController.navigate(NavigationRoute.LOCATION_DETAILS)
                },
                onCameraError = { message -> },
                onRetryCamera = { }
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
            AuthorityFinderScreen(
                incidentTypes = navController.previousBackStackEntry?.savedStateHandle?.getString("types")?.split("-") ?: emptyList(),
                latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                onContinue = { authorities ->
                    vm.updateAuthorities(authorities)
                    navController.navigate(NavigationRoute.REVIEW_SEND)
                }
            )
        }
        composable(NavigationRoute.REVIEW_SEND) {
            ReviewAndSendScreen(
                incidentTypes = navController.previousBackStackEntry?.savedStateHandle?.getString("types")?.split("-") ?: emptyList(),
                mediaUri = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaUri"),
                mediaType = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaType") ?: "photo",
                latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                authorities = androidx.lifecycle.viewmodel.compose.viewModel<ReportViewModel>().selectedAuthorities(),
                sendMethod = prefs.defaultSendMethod,
                onBack = { navController.popBackStack(NavigationRoute.AUTHORITY, inclusive = false) },
                onSaveAndSend = { selectedAuths, method ->
                    vm.updateAuthorities(selectedAuths)
                    navController.navigate("${NavigationRoute.REPORT_VIEW}$vm.reportId")
                    repo.saveReport(
                        ReportModel(
                            id = vm.reportId, incidentTypes = navController.previousBackStackEntry?.savedStateHandle?.getString("types")?.split("-") ?: emptyList(),
                            mediaUris = listOf(navController.previousBackStackEntry?.savedStateHandle?.getString("mediaUri") ?: ""),
                            mediaType = navController.previousBackStackEntry?.savedStateHandle?.getString("mediaType") ?: "photo",
                            timestamp = System.currentTimeMillis(),
                            latitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lat") ?: 0.0,
                            longitude = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("lon") ?: 0.0,
                            address = navController.previousBackStackEntry?.savedStateHandle?.getString("address") ?: "",
                            city = "", district = "", state = "", pincode = "",
                            description = "", status = ReportModel.IncidentStatus.Sent,
                            sentAuthorities = selectedAuths, lastUpdated = System.currentTimeMillis(),
                            sendMethod = method
                        )
                    )
                }
            )
        }
        composable(NavigationRoute.REPORT_VIEW) {
            HomeScreen(
                repo = repo,
                prefs = prefs,
                onOpenSettings = { navController.navigate(NavigationRoute.SETTINGS) },
                onReportNew = { navController.navigate(NavigationRoute.INCIDENT_SELECTOR) },
                onOpenReport = { it }
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
                onAddAuthority = { }
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
