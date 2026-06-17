@file:Suppress("FunctionNaming")

package com.akiwiksten.awtimesheet.feature.location

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.rememberCameraPositionState
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

    val cameraPositionState =
        rememberCameraPositionState()

    val hasFineLocationPermission = rememberFineLocationPermission(
        context = context,
        cameraPositionState = cameraPositionState,
        zoom = DEFAULT_ZOOM
    )

    val launcher =
        rememberPlacesLauncher { place ->
            selectedPlace = place
            selectedLatLng = updateCameraFromPlace(
                place = place,
                cameraPositionState = cameraPositionState,
                zoom = DEFAULT_ZOOM
            )
        }

    val mapState = remember(
        selectedPlace?.displayName,
        selectedLatLng,
        hasFineLocationPermission
    ) {
        LocationPickerMapState(
            selectedPlaceName = selectedPlace?.displayName,
            selectedLatLng = selectedLatLng,
            isMyLocationEnabled = hasFineLocationPermission
        )
    }

    val screenState = LocationPickerScreenState(
        selectedPlace = selectedPlace,
        selectedLatLng = selectedLatLng,
        cameraPositionState = cameraPositionState,
        mapState = mapState
    )

    LocationPickerScaffold(
        topBarState = LocationPickerTopBarState(context = context, launcher = launcher),
        screenState = screenState,
        actions = LocationPickerScaffoldActions(
            onLocationSelected = onLocationSelected,
            onNavigateBack = onNavigateBack,
            onMapClick = { selectedLatLng = it }
        )
    )
}

@Composable
fun rememberPlacesLauncher(
    onPlaceSelected: (Place) -> Unit,
): androidx.activity.compose.ManagedActivityResultLauncher<Intent, ActivityResult> {
    val currentOnPlaceSelected by rememberUpdatedState(onPlaceSelected)

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { intent ->
                    val place = com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(intent)
                    currentOnPlaceSelected(place)
                }
            }
            else -> {
                // RESULT_ERROR or RESULT_CANCELED
                result.data?.let { intent ->
                    runCatching { com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(intent) }
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
