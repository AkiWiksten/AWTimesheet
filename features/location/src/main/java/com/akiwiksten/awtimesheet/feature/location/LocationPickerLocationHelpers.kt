package com.akiwiksten.awtimesheet.feature.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.CameraPositionState
import java.util.Locale

@Composable
internal fun rememberFineLocationPermission(
    context: Context,
    cameraPositionState: CameraPositionState,
    zoom: Float,
    shouldMoveCameraToCurrentLocation: Boolean = true,
): Boolean {
    var hasFineLocationPermission by remember {
        mutableStateOf(hasFineLocationPermission(context = context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasFineLocationPermission = isGranted
        if (isGranted && shouldMoveCameraToCurrentLocation) {
            moveCameraToCurrentLocation(
                context = context,
                cameraPositionState = cameraPositionState,
                zoom = zoom
            )
        }
    }

    LaunchedEffect(Unit) {
        hasFineLocationPermission = hasFineLocationPermission(context = context)
        if (hasFineLocationPermission && shouldMoveCameraToCurrentLocation) {
            moveCameraToCurrentLocation(
                context = context,
                cameraPositionState = cameraPositionState,
                zoom = zoom
            )
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    return hasFineLocationPermission
}
internal fun updateCameraFromPlace(
    place: Place,
    cameraPositionState: CameraPositionState,
    zoom: Float
): LatLng? {
    return place.location?.also { latLng ->
        cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, zoom)
    }
}

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

internal fun getCurrentLocationLatLng(context: Context): LatLng? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return if (hasFineLocationPermission(context) && locationManager != null) {
        latestKnownLocation(locationManager)?.let { LatLng(it.latitude, it.longitude) }
    } else {
        null
    }
}

internal fun resolveAddressFromLatLng(
    context: Context,
    latLng: LatLng,
    onResolved: (String?) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())
    if (!Geocoder.isPresent()) {
        onResolved(null)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        onResolved(addresses.firstOrNull()?.getAddressLine(0))
                    }

                    override fun onError(errorMessage: String?) {
                        onResolved(null)
                    }
                }
            )
        }.onFailure {
            onResolved(null)
        }
        return
    }

    val address = runCatching {
        @Suppress("DEPRECATION")
        geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            ?.firstOrNull()
            ?.getAddressLine(0)
    }.getOrNull()

    onResolved(address)
}

internal fun resolveLatLngFromAddress(
    context: Context,
    address: String,
    onResolved: (LatLng?) -> Unit
) {
    val geocoder = Geocoder(context, Locale.getDefault())
    if (!Geocoder.isPresent()) {
        onResolved(null)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            geocoder.getFromLocationName(
                address,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val resolved = addresses.firstOrNull()
                        onResolved(
                            if (resolved != null) LatLng(resolved.latitude, resolved.longitude) else null
                        )
                    }

                    override fun onError(errorMessage: String?) {
                        onResolved(null)
                    }
                }
            )
        }.onFailure {
            onResolved(null)
        }
        return
    }

    val latLng = runCatching {
        @Suppress("DEPRECATION")
        geocoder.getFromLocationName(address, 1)
            ?.firstOrNull()
            ?.let { LatLng(it.latitude, it.longitude) }
    }.getOrNull()

    onResolved(latLng)
}

@Suppress("MissingPermission")
private fun moveCameraToCurrentLocation(
    context: Context,
    cameraPositionState: CameraPositionState,
    zoom: Float
) {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

    val latestLocation = latestKnownLocation(locationManager)

    latestLocation?.let { location ->
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(location.latitude, location.longitude),
            zoom
        )
    }
}

@Suppress("MissingPermission")
private fun latestKnownLocation(locationManager: LocationManager): Location? {
    return locationManager
        .getProviders(true)
        .asSequence()
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
}
