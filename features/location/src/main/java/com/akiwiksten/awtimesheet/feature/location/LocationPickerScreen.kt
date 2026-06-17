@file:Suppress("FunctionNaming")

package com.akiwiksten.awtimesheet.feature.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Parcelable
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.parcelize.Parcelize

private const val DEFAULT_ZOOM = 15f

@Composable
fun LocationPickerScreen(
    onLocationSelected: (LocationPickerResult) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedPlace by remember {
        mutableStateOf<Place?>(null)
    }

    var selectedLatLng by remember {
        mutableStateOf<LatLng?>(null)
    }

    var hasFineLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPositionState =
        rememberCameraPositionState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasFineLocationPermission = isGranted
        if (isGranted) {
            moveCameraToCurrentLocation(
                context = context,
                cameraPositionState = cameraPositionState
            )
        }
    }

    LaunchedEffect(Unit) {
        hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocationPermission) {
            moveCameraToCurrentLocation(
                context = context,
                cameraPositionState = cameraPositionState
            )
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val launcher =
        rememberPlacesLauncher { place ->

            selectedPlace = place

            place.location?.let { latLng ->

                selectedLatLng = latLng

                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(
                        latLng,
                        DEFAULT_ZOOM
                    )
            }
        }

    Scaffold(
        topBar = {
            LocationPickerTopBar(
                context = context,
                launcher = launcher
            )
        },
        floatingActionButton = {
            LocationPickerConfirmButton(
                selectedPlace = selectedPlace,
                selectedLatLng = selectedLatLng,
                onLocationSelected = onLocationSelected,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        LocationPickerMapContent(
            padding = padding,
            cameraPositionState = cameraPositionState,
            selectedPlace = selectedPlace,
            selectedLatLng = selectedLatLng,
            isMyLocationEnabled = hasFineLocationPermission
        ) {
            selectedLatLng = it
        }
    }
}

@Suppress("MissingPermission")
private fun moveCameraToCurrentLocation(
    context: Context,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState
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
            DEFAULT_ZOOM
        )
    }
}

@Composable
private fun LocationPickerTopBar(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = {
                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.DISPLAY_NAME,
                    Place.Field.FORMATTED_ADDRESS,
                    Place.Field.LOCATION
                )

                val intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN,
                    fields
                ).build(context)

                launcher.launch(intent)
            }
        ) {
            Text("Search Place")
        }
    }
}

@Composable
private fun LocationPickerConfirmButton(
    selectedPlace: Place?,
    selectedLatLng: LatLng?,
    onLocationSelected: (LocationPickerResult) -> Unit,
    onNavigateBack: () -> Unit
) {
    FloatingActionButton(
        onClick = {
            val place = selectedPlace ?: return@FloatingActionButton
            val latLng = selectedLatLng ?: return@FloatingActionButton

            onLocationSelected(
                LocationPickerResult(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    address = place.formattedAddress ?: ""
                )
            )

            onNavigateBack()
        }
    ) {
        Icon(
            Icons.Default.Check,
            null
        )
    }
}

@Composable
private fun LocationPickerMapContent(
    padding: androidx.compose.foundation.layout.PaddingValues,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    selectedPlace: Place?,
    selectedLatLng: LatLng?,
    isMyLocationEnabled: Boolean,
    onMapClick: (LatLng) -> Unit
) {
    val mapUiSettings = remember(isMyLocationEnabled) {
        MapUiSettings(myLocationButtonEnabled = isMyLocationEnabled)
    }
    val mapProperties = remember(isMyLocationEnabled) {
        MapProperties(isMyLocationEnabled = isMyLocationEnabled)
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapClick = onMapClick,
    ) {
        selectedLatLng?.let { latLng ->
            Marker(
                state = rememberUpdatedMarkerState(position = latLng),
                title = selectedPlace?.displayName
            )
        }
    }
}

@Composable
fun rememberPlacesLauncher(
    onPlaceSelected: (Place) -> Unit,
): ManagedActivityResultLauncher<Intent, ActivityResult> {

    val currentOnPlaceSelected by rememberUpdatedState(onPlaceSelected)

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { intent ->
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    currentOnPlaceSelected(place)
                }
            }
            else -> {
                // RESULT_ERROR or RESULT_CANCELED
                result.data?.let { intent ->
                    runCatching { Autocomplete.getStatusFromIntent(intent) }
                        .onSuccess { status ->
                            android.util.Log.e(
                                "LocationPicker",
                                "Places autocomplete error: ${status.statusMessage}"
                            )
                        }
                }
            }
        }
    }
}

@Parcelize
data class LocationPickerResult(
    val latitude: Double,
    val longitude: Double,
    val address: String
) : Parcelable
