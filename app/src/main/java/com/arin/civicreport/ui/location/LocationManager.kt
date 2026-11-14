package com.arin.civicreport.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint

data class CurrentLocation(
    val latitude: Double, val longitude: Double,
    val address: String, val city: String, val district: String,
    val state: String, val pincode: String
)

/** Simple point passed to the map overlay. Mirrors osmdroid's GeoPoint so the rest of the app stays agnostic. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

class LocationManager(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, java.util.Locale.getDefault())

    private val _state = MutableStateFlow<LocationState>(LocationState.Idle)
    val state: StateFlow<LocationState> = _state.asStateFlow()

    sealed class LocationState {
        object Idle : LocationState()
        data class Loading(val message: String) : LocationState()
        data class Ready(val loc: CurrentLocation) : LocationState()
        data class Error(val message: String) : LocationState()
    }

    private fun setAddress(addr: Address?): CurrentLocation {
        val addressText = addr?.getAddressLine(0) ?: ""
        return CurrentLocation(
            addr?.latitude ?: 0.0, addr?.longitude ?: 0.0,
            addressText,
            addr?.locality ?: "", addr?.adminArea ?: "",
            addr?.countryName ?: "", addr?.postalCode ?: ""
        )
    }

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    fun hasCoarseLocation(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    suspend fun getCurrentLocation(): CurrentLocation = withContext(Dispatchers.IO) {
        _state.value = LocationState.Loading("Getting your location...")
        val hasFine = hasFineLocation()
        val hasCoarse = hasCoarseLocation()

        if (!hasFine && !hasCoarse) {
            _state.value = LocationState.Error("Location permission denied. Tap the map to place a pin manually.")
            return@withContext CurrentLocation(0.0, 0.0, "", "", "", "", "")
        }

        try {
            val location = if (hasFine) {
                fusedClient.lastLocation.await()
            } else {
                val coarse = fusedClient.lastLocation.await()
                if (coarse != null && coarse.provider?.contains("network") == true) coarse else fusedClient.lastLocation.await()
            }

            if (location == null) {
                _state.value = LocationState.Error("No GPS signal. Tap the map to place a pin manually.")
                return@withContext CurrentLocation(0.0, 0.0, "", "", "", "", "")
            }

            _state.value = LocationState.Loading("Finding your address...")
            val addr = try {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            } catch (e: Exception) { null }
            val resolvedLoc = setAddress(addr)
            _state.value = LocationState.Ready(resolvedLoc)
            resolvedLoc
        } catch (e: Exception) {
            _state.value = LocationState.Error("Could not get location. Tap the map to place a pin manually.")
            CurrentLocation(0.0, 0.0, "", "", "", "", "")
        }
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): CurrentLocation = withContext(Dispatchers.IO) {
        _state.value = LocationState.Loading("Finding your address...")
        val addr = try {
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        } catch (e: Exception) { null }
        val resolved = setAddress(addr)
        _state.value = LocationState.Ready(resolved)
        resolved
    }
}

private suspend fun FusedLocationProviderClient.lastLocation(): android.location.Location? {
    return try {
        val pendingResult = getLocationAvailability()
        val location = getLastLocation()
        location.await()
    } catch (e: Exception) {
        null
    }
}

private suspend fun <T> java.util.concurrent.CompletableFuture<T>.await(): T? =
    try { kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        whenComplete { value, ex ->
            if (ex != null) { cont.resume(null); return@whenComplete }
            cont.resume(value);
        }
    } } catch (e: Exception) { null }
