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

class PermissionManager(private val activity: Activity) {

    private val cameraLauncher = ActivityResultContracts.RequestPermission()
    private val locationLauncher = ActivityResultContracts.RequestMultiplePermissions()
    private val smsLauncher = ActivityResultContracts.RequestPermission()
    private val notificationsLauncher = ActivityResultContracts.RequestPermission()

    fun hasCamera(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun hasReadMedia(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

    fun hasReadMediaForLegacy(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    fun hasSendSms(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasCallPhone(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun hasPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

    fun requestCameraPermission() {
        cameraLauncher.launch(Manifest.permission.CAMERA)
    }

    fun requestLocationPermissions() {
        val perms = mutableListOf<String>()
        if (hasFineLocation(activity)) perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasCoarseLocation(activity)) perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (perms.isNotEmpty()) locationLauncher.launch(perms.toTypedArray())
    }

    fun requestSmsPermission() {
        smsLauncher.launch(Manifest.permission.SEND_SMS)
    }

    fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestPermission(permission: String) {
        when (permission) {
            Manifest.permission.CAMERA -> requestCameraPermission()
            Manifest.permission.SEND_SMS -> requestSmsPermission()
            Manifest.permission.POST_NOTIFICATIONS -> requestNotificationsPermission()
            else -> requestLocationPermissions()
        }
    }

    fun launchRationaleOrRequest(context: Context, permission: String, rationale: String) {
        val activity = context as? Activity ?: return
        if (hasPermission(context, permission)) return
        android.app.AlertDialog.Builder(activity)
            .setTitle("Permission needed")
            .setMessage(rationale)
            .setPositiveButton("Grant") { _, _ -> requestPermission(permission) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    private fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
