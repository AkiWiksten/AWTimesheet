package com.akiwiksten.awtimesheet.feature.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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

@Composable
internal fun rememberFineLocationPermission(
    context: Context,
    cameraPositionState: CameraPositionState,
    zoom: Float
): Boolean {
    var hasFineLocationPermission by remember {
        mutableStateOf(hasFineLocationPermission(context = context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasFineLocationPermission = isGranted
        if (isGranted) {
            moveCameraToCurrentLocation(
                context = context,
                cameraPositionState = cameraPositionState,
                zoom = zoom
            )
        }
    }

    LaunchedEffect(Unit) {
        hasFineLocationPermission = hasFineLocationPermission(context = context)
        if (hasFineLocationPermission) {
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

@Suppress("MissingPermission")
private fun moveCameraToCurrentLocation(
    context: Context,
    cameraPositionState: CameraPositionState,
    zoom: Float
) {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

    val latestLocation = locationManager
        .getProviders(true)
        .asSequence()
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)

    latestLocation?.let { location ->
        cameraPositionState.position = CameraPosition.fromLatLngZoom(
            LatLng(location.latitude, location.longitude),
            zoom
        )
    }
}
