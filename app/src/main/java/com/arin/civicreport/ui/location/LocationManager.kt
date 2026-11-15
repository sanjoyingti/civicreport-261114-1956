package com.arin.civicreport.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val city: String,
    val district: String,
    val state: String,
    val pincode: String
) {
    val hasFix: Boolean get() = latitude != 0.0 || longitude != 0.0
}

data class GeoPoint(val latitude: Double, val longitude: Double)

class LocationManager(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    fun hasFineLocation(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocation(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(): Boolean = hasFineLocation() || hasCoarseLocation()

    suspend fun getCurrentLocation(): CurrentLocation = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext empty()
        val location = try {
            lastKnownLocation()
        } catch (e: SecurityException) {
            null
        } ?: return@withContext empty()
        reverseGeocode(location.latitude, location.longitude)
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): CurrentLocation =
        withContext(Dispatchers.IO) {
            val address = try {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            } catch (e: Exception) {
                null
            }
            toLocation(latitude, longitude, address)
        }

    private fun toLocation(latitude: Double, longitude: Double, address: Address?): CurrentLocation =
        CurrentLocation(
            latitude = latitude,
            longitude = longitude,
            address = address?.getAddressLine(0) ?: "%.6f, %.6f".format(latitude, longitude),
            city = address?.locality ?: address?.subAdminArea ?: "",
            district = address?.subAdminArea ?: "",
            state = address?.adminArea ?: "",
            pincode = address?.postalCode ?: ""
        )

    private fun empty(): CurrentLocation = CurrentLocation(0.0, 0.0, "", "", "", "", "")

    private suspend fun lastKnownLocation(): android.location.Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
}
