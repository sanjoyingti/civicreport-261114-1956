package com.arin.civicreport

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.arin.civicreport.data.CivicDatabase
import com.arin.civicreport.data.Repository
import com.arin.civicreport.ui.navigation.AppNavigation
import com.arin.civicreport.ui.screens.report.ReportFlowViewModel
import com.arin.civicreport.ui.theme.CivicReportTheme
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current.applicationContext
            val repo = remember { Repository(CivicDatabase.getInstance(context)) }
            val prefs = remember { Prefs(context) }
            val permissionManager = remember { PermissionManager(this@MainActivity) }
            val reportViewModel: ReportFlowViewModel = viewModel()

            CivicReportTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        navController = rememberNavController(),
                        repo = repo,
                        prefs = prefs,
                        permissionManager = permissionManager,
                        reportViewModel = reportViewModel
                    )
                }
            }
        }
    }
}
