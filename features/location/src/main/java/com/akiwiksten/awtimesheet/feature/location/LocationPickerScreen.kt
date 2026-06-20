package com.akiwiksten.awtimesheet.feature.location

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.parcelize.Parcelize

private const val DEFAULT_ZOOM = 15f

@Composable
fun LocationPickerScreen(
    onLocationSelected: (LocationPickerResult) -> Unit,
    onNavigateBack: () -> Unit,
    titleResId: Int = R.string.select_location_title,
    initialAddress: String? = null,
    initialLatLng: LatLng? = null,
) {
    BackHandler(onBack = onNavigateBack)
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState()
    val state = rememberLocationPickerState(initialAddress, initialLatLng)

    val hasFineLocationPermission = rememberFineLocationPermission(
        context = context,
        cameraPositionState = cameraPositionState,
        zoom = DEFAULT_ZOOM,
        shouldMoveCameraToCurrentLocation = initialAddress.isNullOrBlank() && initialLatLng == null,
    )

    LocationPickerInitialLocationEffect(
        context = context,
        initialAddress = initialAddress,
        initialLatLng = initialLatLng,
        cameraPositionState = cameraPositionState,
        onLocationResolved = { latLng, address, resolving, failed ->
            state.selectedLatLng = latLng
            state.selectedAddress = address ?: state.selectedAddress
            state.isResolvingAddress = resolving
            state.isPrefillCenteringFailed = failed
        }
    )

    val launcher = rememberPlacesLauncher { place ->
        state.selectedPlace = place
        state.selectedAddress = place.formattedAddress ?: place.displayName
        state.isResolvingAddress = false
        state.isPrefillCenteringFailed = false
        state.selectedLatLng = updateCameraFromPlace(
            place = place,
            cameraPositionState = cameraPositionState,
            zoom = DEFAULT_ZOOM,
        )
    }

    LocationPickerScaffold(
        topBarState = LocationPickerTopBarState(context, titleResId, launcher, onNavigateBack),
        screenState = state.toScreenState(hasFineLocationPermission, cameraPositionState),
        actions = rememberLocationPickerActions(
            context = context,
            cameraPositionState = cameraPositionState,
            state = state,
            onLocationSelected = onLocationSelected,
            onNavigateBack = onNavigateBack,
        ),
    )
}

@Composable
private fun rememberLocationPickerState(
    initialAddress: String?,
    initialLatLng: LatLng?,
): LocationPickerInternalState = remember(initialAddress, initialLatLng) {
    LocationPickerInternalState(initialAddress, initialLatLng)
}

private class LocationPickerInternalState(
    initialAddress: String?,
    initialLatLng: LatLng?,
) {
    var selectedPlace by mutableStateOf<Place?>(null)
    var selectedAddress by mutableStateOf(initialAddress)
    var isResolvingAddress by mutableStateOf(initialAddress != null && initialLatLng == null)
    var isPrefillCenteringFailed by mutableStateOf(false)
    var selectedLatLng by mutableStateOf(initialLatLng)

    fun toScreenState(
        hasFineLocationPermission: Boolean,
        cameraPositionState: com.google.maps.android.compose.CameraPositionState
    ) = LocationPickerScreenState(
        searchText = selectedAddress ?: selectedPlace?.displayName ?: "",
        selectedAddress = selectedAddress,
        isResolvingAddress = isResolvingAddress,
        isPrefillCenteringFailed = isPrefillCenteringFailed,
        selectedLatLng = selectedLatLng,
        cameraPositionState = cameraPositionState,
        mapState = LocationPickerMapState(
            selectedPlaceName = selectedPlace?.displayName ?: selectedAddress,
            selectedLatLng = selectedLatLng,
            isMyLocationEnabled = hasFineLocationPermission,
        )
    )
}

@Composable
private fun rememberLocationPickerActions(
    context: android.content.Context,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    state: LocationPickerInternalState,
    onLocationSelected: (LocationPickerResult) -> Unit,
    onNavigateBack: () -> Unit,
): LocationPickerScaffoldActions {
    return remember(context, cameraPositionState, state) {
        LocationPickerScaffoldActions(
            onLocationSelected = onLocationSelected,
            onNavigateBack = onNavigateBack,
            onMapClick = { latLng ->
                state.selectedPlace = null
                state.selectedLatLng = latLng
                state.selectedAddress = "${latLng.latitude}, ${latLng.longitude}"
                state.isResolvingAddress = true
                state.isPrefillCenteringFailed = false
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, DEFAULT_ZOOM)
                resolveAddressFromLatLng(context, latLng) { resolvedAddress ->
                    if (state.selectedLatLng == latLng) {
                        state.isResolvingAddress = false
                        state.selectedAddress = resolvedAddress ?: "${latLng.latitude}, ${latLng.longitude}"
                    }
                }
            },
            onUseCurrentLocation = {
                getCurrentLocationLatLng(context)?.let { currentLatLng ->
                    state.selectedPlace = null
                    state.selectedLatLng = currentLatLng
                    state.selectedAddress = "${currentLatLng.latitude}, ${currentLatLng.longitude}"
                    state.isResolvingAddress = true
                    state.isPrefillCenteringFailed = false
                    resolveAddressFromLatLng(context, currentLatLng) { resolvedAddress ->
                        if (state.selectedLatLng == currentLatLng) {
                            state.isResolvingAddress = false
                            state.selectedAddress = resolvedAddress
                                ?: "${currentLatLng.latitude}, ${currentLatLng.longitude}"
                        }
                    }
                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(currentLatLng, DEFAULT_ZOOM)
                } ?: run { state.isResolvingAddress = false }
            },
        )
    }
}

@Composable
private fun LocationPickerInitialLocationEffect(
    context: android.content.Context,
    initialAddress: String?,
    initialLatLng: LatLng?,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onLocationResolved: (LatLng?, String?, Boolean, Boolean) -> Unit
) {
    LaunchedEffect(initialAddress, initialLatLng) {
        if (initialLatLng != null) {
            onLocationResolved(initialLatLng, null, false, false)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(initialLatLng, DEFAULT_ZOOM)
            return@LaunchedEffect
        }

        if (initialAddress.isNullOrBlank()) {
            onLocationResolved(null, null, false, false)
            return@LaunchedEffect
        }

        resolveLatLngFromAddress(context, initialAddress) { latLng ->
            onLocationResolved(latLng, null, false, latLng == null)
            if (latLng != null) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, DEFAULT_ZOOM)
            }
        }
    }
}

@Composable
fun rememberPlacesLauncher(
    onPlaceSelected: (Place) -> Unit,
): androidx.activity.compose.ManagedActivityResultLauncher<Intent, ActivityResult> {
    val currentOnPlaceSelected by rememberUpdatedState(onPlaceSelected)

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { intent ->
                    val place = com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(intent)
                    currentOnPlaceSelected(place)
                }
            }

            else -> {
                result.data?.let { intent ->
                    runCatching {
                        com.google.android.libraries.places.widget.Autocomplete.getStatusFromIntent(intent)
                    }.onSuccess { status ->
                        android.util.Log.e(
                            "LocationPicker",
                            "Places autocomplete error: ${status.statusMessage}",
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
    val address: String,
) : Parcelable
