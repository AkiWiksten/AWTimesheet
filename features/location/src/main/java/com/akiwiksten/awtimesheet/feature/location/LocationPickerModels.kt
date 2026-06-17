package com.akiwiksten.awtimesheet.feature.location

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.maps.android.compose.CameraPositionState

internal data class LocationPickerMapState(
    val selectedPlaceName: String?,
    val selectedLatLng: LatLng?,
    val isMyLocationEnabled: Boolean
)

internal data class LocationPickerScreenState(
    val selectedPlace: Place?,
    val selectedLatLng: LatLng?,
    val cameraPositionState: CameraPositionState,
    val mapState: LocationPickerMapState
)

internal data class LocationPickerScaffoldActions(
    val onLocationSelected: (LocationPickerResult) -> Unit,
    val onNavigateBack: () -> Unit,
    val onMapClick: (LatLng) -> Unit
)

internal data class LocationPickerTopBarState(
    val context: Context,
    val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>
)
