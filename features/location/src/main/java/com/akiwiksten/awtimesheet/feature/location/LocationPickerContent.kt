package com.akiwiksten.awtimesheet.feature.location

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
internal fun LocationPickerScaffold(
    topBarState: LocationPickerTopBarState,
    screenState: LocationPickerScreenState,
    actions: LocationPickerScaffoldActions
) {
    Scaffold(
        floatingActionButton = {
            LocationPickerConfirmButton(
                selectedPlace = screenState.selectedPlace,
                selectedLatLng = screenState.selectedLatLng,
                onLocationSelected = actions.onLocationSelected,
                onNavigateBack = actions.onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LocationPickerSearchField(
                context = topBarState.context,
                launcher = topBarState.launcher,
                selectedPlace = screenState.selectedPlace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Box(modifier = Modifier.fillMaxSize()) {
                LocationPickerMapContent(
                    padding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    cameraPositionState = screenState.cameraPositionState,
                    mapState = screenState.mapState,
                    onMapClick = actions.onMapClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun LocationPickerSearchField(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    selectedPlace: Place?,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = selectedPlace?.displayName ?: "",
        onValueChange = {},
        modifier = modifier,
        label = { Text("Search location") },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = {
                    val fields = listOf(
                        Place.Field.ID,
                        Place.Field.DISPLAY_NAME,
                        Place.Field.FORMATTED_ADDRESS,
                        Place.Field.LOCATION
                    )

                    val intent = Autocomplete.IntentBuilder(
                        AutocompleteActivityMode.OVERLAY,
                        fields
                    ).build(context)

                    launcher.launch(intent)
                    focusManager.clearFocus()
                }
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.DISPLAY_NAME,
                    Place.Field.FORMATTED_ADDRESS,
                    Place.Field.LOCATION
                )

                val intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.OVERLAY,
                    fields
                ).build(context)

                launcher.launch(intent)
                focusManager.clearFocus()
            }
        )
    )
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
    cameraPositionState: CameraPositionState,
    mapState: LocationPickerMapState,
    onMapClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier
) {
    val mapUiSettings = remember(mapState.isMyLocationEnabled) {
        MapUiSettings(myLocationButtonEnabled = mapState.isMyLocationEnabled)
    }
    val mapProperties = remember(mapState.isMyLocationEnabled) {
        MapProperties(isMyLocationEnabled = mapState.isMyLocationEnabled)
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize()
            .padding(padding),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = mapUiSettings,
        onMapClick = onMapClick,
    ) {
        mapState.selectedLatLng?.let { latLng ->
            Marker(
                state = rememberUpdatedMarkerState(position = latLng),
                title = mapState.selectedPlaceName
            )
        }
    }
}
