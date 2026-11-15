package com.arin.civicreport.ui.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {

    private val singleLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val locationLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasCamera(context: Context): Boolean = hasPermission(context, Manifest.permission.CAMERA)

    fun hasAudio(context: Context): Boolean = hasPermission(context, Manifest.permission.RECORD_AUDIO)

    fun hasFineLocation(context: Context): Boolean =
        hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasCoarseLocation(context: Context): Boolean =
        hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasReadMedia(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES) &&
                hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    fun hasSendSms(context: Context): Boolean = hasPermission(context, Manifest.permission.SEND_SMS)

    fun requestCameraPermission() {
        singleLauncher.launch(Manifest.permission.CAMERA)
    }

    fun requestAudioPermission() {
        singleLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun requestSmsPermission() {
        singleLauncher.launch(Manifest.permission.SEND_SMS)
    }

    fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            singleLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestLocationPermissions() {
        val permissions = mutableListOf<String>()
        if (!hasFineLocation(activity)) permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasCoarseLocation(activity)) permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissions.isNotEmpty()) locationLauncher.launch(permissions.toTypedArray())
    }

    fun requestPermission(permission: String) {
        when (permission) {
            Manifest.permission.CAMERA -> requestCameraPermission()
            Manifest.permission.RECORD_AUDIO -> requestAudioPermission()
            Manifest.permission.SEND_SMS -> requestSmsPermission()
            Manifest.permission.POST_NOTIFICATIONS -> requestNotificationsPermission()
            else -> requestLocationPermissions()
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}
