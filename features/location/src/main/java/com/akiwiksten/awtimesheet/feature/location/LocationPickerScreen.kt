@file:Suppress("FunctionNaming")

package com.akiwiksten.awtimesheet.feature.location

import android.app.Activity
import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.parcelize.Parcelize

private const val DEFAULT_ZOOM = 15f

@Composable
fun LocationPickerScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedPlace by remember {
        mutableStateOf<Place?>(null)
    }

    var selectedLatLng by remember {
        mutableStateOf<LatLng?>(null)
    }

    val cameraPositionState =
        rememberCameraPositionState()

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
                navController = navController
            )
        }
    ) { padding ->
        LocationPickerMapContent(
            padding = padding,
            cameraPositionState = cameraPositionState,
            selectedPlace = selectedPlace,
            selectedLatLng = selectedLatLng
        ) {
            selectedLatLng = it
        }
    }
}

@Composable
private fun LocationPickerTopBar(
    context: android.content.Context,
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
                    AutocompleteActivityMode.OVERLAY,
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
    navController: NavController
) {
    FloatingActionButton(
        onClick = {
            val place = selectedPlace ?: return@FloatingActionButton
            val latLng = selectedLatLng ?: return@FloatingActionButton

            navController
                .previousBackStackEntry
                ?.savedStateHandle
                ?.set(
                    "selected_location",
                    LocationPickerResult(
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        address = place.formattedAddress ?: ""
                    )
                )

            navController.popBackStack()
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
    onMapClick: (LatLng) -> Unit
) {
    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        cameraPositionState = cameraPositionState,
        onMapClick = onMapClick,
    ) {
        selectedLatLng?.let { latLng ->
            Marker(
                state = MarkerState(latLng),
                title = selectedPlace?.displayName
            )
        }
    }
}

@Composable
fun rememberPlacesLauncher(
    onPlaceSelected: (Place) -> Unit,
): ManagedActivityResultLauncher<Intent, ActivityResult> {

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            result.data?.let { intent ->

                val place =
                    Autocomplete.getPlaceFromIntent(intent)

                onPlaceSelected(place)
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
