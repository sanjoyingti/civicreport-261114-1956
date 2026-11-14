package com.arin.civicreport

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.arin.civicreport.data.CivicDatabase
import com.arin.civicreport.data.Repository
import com.arin.civicreport.ui.navigation.AppNavigation
import com.arin.civicreport.ui.theme.CivicReportTheme
import com.arin.civicreport.ui.util.PermissionManager
import com.arin.civicreport.ui.util.Prefs

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newContext: Context) {
        super.attachBaseContext(newContext)
        ApplicationLifecycle.register(newContext.applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current.applicationContext
            val db = CivicDatabase.getInstance(context)
            val repo = Repository(db)
            val prefs = Prefs(context)
            val pm = rememberPermissionManager()

            CivicReportTheme(darkTheme = remember {
                val view = LocalView.current
                val ctx = view.context
                androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.ui.platform.LocalConfiguration.current
                false // system in dark is already handled by CivicReportTheme
            }) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(
                        navController = rememberNavController(),
                        repo = repo,
                        prefs = prefs,
                        permissionManager = pm,
                        onGetContentUri = { uri: android.net.Uri -> uri },
                        onUriSaved = { file: java.io.File, kind: String -> },
                        mediaTypeFromUri = { uri: android.net.Uri -> if (uri.toString().endsWith(".mp4", ignoreCase = true)) "video" else "photo" }
                    )
                }
            }
        }
    }

    @Composable
    private fun rememberPermissionManager(): PermissionManager = PermissionManager()
}

object ApplicationLifecycle {
    private var context: android.content.Context? = null
    fun register(appCtx: android.content.Context) { context = appCtx }
    fun context(): android.content.Context = context ?: throw IllegalStateException("context not set")
}
