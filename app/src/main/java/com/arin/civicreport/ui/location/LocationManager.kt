package com.arin.civicreport.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow

data class CurrentLocation(
    val latitude: Double, val longitude: Double,
    val address: String, val city: String, val district: String,
    val state: String, val pincode: String
)

class LocationManager(private val context: Context) {
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

    suspend fun getCurrentLocation(): CurrentLocation = withContext(Dispatchers.IO) {
        _state.value = LocationState.Loading("Getting your location...")
        val ctx = context.applicationContext
        val hasFine = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            _state.value = LocationState.Error("Location permission denied. Tap the map to place a pin manually.")
            return@withContext CurrentLocation(0.0, 0.0, "", "", "", "", "")
        }

        try {
            val mgr = ctx.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val pos: Location? = if (hasFine) {
                mgr.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            } else {
                mgr.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            }
            if (pos == null) {
                _state.value = LocationState.Error("No GPS signal. Tap the map to place a pin manually.")
                return@withContext CurrentLocation(0.0, 0.0, "", "", "", "", "")
            }
            _state.value = LocationState.Loading("Finding your address...")
            val addr = try {
                geocoder.getFromLocation(pos.latitude, pos.longitude, 1)?.firstOrNull()
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
