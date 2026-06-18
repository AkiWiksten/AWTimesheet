@file:Suppress("FunctionNaming")

package com.akiwiksten.awtimesheet.feature.location

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.ui.AwtButton
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerScaffold(
    topBarState: LocationPickerTopBarState,
    screenState: LocationPickerScreenState,
    actions: LocationPickerScaffoldActions
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_location_title)) },
                navigationIcon = {
                    IconButton(onClick = topBarState.onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
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
                searchText = screenState.searchText,
                isResolvingAddress = screenState.isResolvingAddress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            if (screenState.isPrefillCenteringFailed) {
                Text(
                    text = stringResource(R.string.prefill_location_not_found),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
            AwtButton(
                onClick = actions.onUseCurrentLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.use_current_address))
            }
            LocationPickerConfirmButton(
                selectedAddress = screenState.selectedAddress,
                isResolvingAddress = screenState.isResolvingAddress,
                selectedLatLng = screenState.selectedLatLng,
                onLocationSelected = actions.onLocationSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            Box(modifier = Modifier.fillMaxSize()) {
                LocationPickerMapContent(
                    padding = PaddingValues(0.dp),
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
    searchText: String,
    isResolvingAddress: Boolean,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val initialQuery = searchText.takeIf { it.isNotBlank() }

    fun buildSearchIntent(): Intent {
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )

        val builder = Autocomplete.IntentBuilder(
            AutocompleteActivityMode.OVERLAY,
            fields
        )

        if (initialQuery != null) {
            builder.setInitialQuery(initialQuery)
        }

        return builder.build(context)
    }

    OutlinedTextField(
        value = searchText,
        onValueChange = {},
        modifier = modifier,
        label = { Text(stringResource(R.string.search_location)) },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isResolvingAddress) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                IconButton(
                    onClick = {
                        launcher.launch(buildSearchIntent())
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                launcher.launch(buildSearchIntent())
                focusManager.clearFocus()
            }
        )
    )
}

@Composable
private fun LocationPickerConfirmButton(
    selectedAddress: String?,
    isResolvingAddress: Boolean,
    selectedLatLng: LatLng?,
    onLocationSelected: (LocationPickerResult) -> Unit,
    modifier: Modifier = Modifier
) {
    AwtButton(
        onClick = {
            val latLng = selectedLatLng ?: return@AwtButton

            onLocationSelected(
                LocationPickerResult(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    address = selectedAddress ?: "${latLng.latitude}, ${latLng.longitude}"
                )
            )
        },
        enabled = selectedLatLng != null && !isResolvingAddress,
        modifier = modifier,
    ) {
        Icon(Icons.Default.Check, contentDescription = null)
        Text(text = stringResource(R.string.confirm), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun LocationPickerMapContent(
    padding: PaddingValues,
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
