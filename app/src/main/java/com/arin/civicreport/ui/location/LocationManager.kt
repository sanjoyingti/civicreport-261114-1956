package com.arin.civicreport.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.arin.civicreport.ui.util.NetworkChecker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CurrentLocation(
    val latitude: Double, val longitude: Double,
    val address: String, val city: String, val district: String,
    val state: String, val pincode: String
)

class LocationManager(context: Context) {
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationClient(context)
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

    fun observeState() = _state.catch { e ->
        _state.value = LocationState.Error("Location error: ${e.message}")
        throw e
    }

    suspend fun getCurrentLocation(): CurrentLocation = withContext(Dispatchers.IO) {
        _state.value = LocationState.Loading("Getting your location...")
        val ctx = ApplicationLifecycle.context()
        val hasFine = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        var pos = if (hasFine || hasCoarse) {
            try {
                fused.lastLocation.await()
            } catch (e: Exception) { null }
        } else {
            null
        }

        if (pos == null) {
            val network = NetworkChecker.isOnline(ctx)
            if (network) {
                _state.value = LocationState.Error("No GPS signal and no network to find your address. Tap the map to place the pin manually.")
            } else {
                _state.value = LocationState.Error("No internet connection. Please tap the map to place the pin manually.")
            }
            CurrentLocation(0.0, 0.0, "", "", "", "", "")
        } else {
            _state.value = LocationState.Loading("Finding your address...")
            val addr = try {
                geocoder.getFromLocation(pos.latitude, pos.longitude, 1)?.firstOrNull()
            } catch (e: Exception) { null }
            val resolvedLoc = setAddress(addr)
            _state.value = LocationState.Ready(resolvedLoc)
            resolvedLoc
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

    object ApplicationLifecycle {
        @Volatile private var context: android.content.Context? = null
        fun register(appCtx: android.content.Context) { context = appCtx }
        fun context(): android.content.Context = context ?: throw IllegalStateException("context not set")
    }
}
