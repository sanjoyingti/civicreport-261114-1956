package com.arin.civicreport.ui.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.arin.civicreport.ui.util.Prefs

class PermissionManager {

    private var activity: Activity? = null
    private val locationLauncher = ActivityResultContracts.RequestMultiplePermissions()
    private val storageLauncher = ActivityResultContracts.RequestMultiplePermissions()

    fun setActivity(a: Activity?) { activity = a }

    fun hasCamera(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasReadMedia(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

    fun hasSendSms(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasCallPhone(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun hasPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

    fun requestPermission(permission: String) {
        activity?.let {
            locationLauncher.launch(arrayOf(permission))
        }
    }

    fun launchRationaleOrRequest(context: Context, permission: String, rationale: String) {
        val activity = (context as? Activity) ?: return
        if (hasPermission(context, permission)) return
        androidx.lifecycle.viewmodel.compose.viewModel<androidx.lifecycle.ViewModel>().let {}
        androidx.compose.runtime.SideEffect {
            android.app.AlertDialog.Builder(activity).setTitle("Permission needed").setMessage(rationale)
                .setPositiveButton("Grant") { _, _ ->
                    locationLauncher.launch(arrayOf(permission))
                }.setNegativeButton("Cancel", null).show()
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
