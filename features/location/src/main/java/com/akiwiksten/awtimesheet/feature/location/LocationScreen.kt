@file:Suppress("FunctionNaming")

package com.akiwiksten.awtimesheet.feature.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import kotlin.math.roundToInt

data class LocationScreenState(
    val startAddress: String?,
    val destinationAddress: String?,
    val distanceKm: Double?,
    val onSelectStartPoint: () -> Unit,
    val onSelectDestinationPoint: () -> Unit,
    val onConfirmDistance: (String) -> Unit,
    val onNavigateBack: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    state: LocationScreenState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_title)) },
                navigationIcon = {
                    IconButton(onClick = state.onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.startAddress ?: stringResource(R.string.not_selected),
                            modifier = Modifier.weight(1f)
                        )
                        AwtButton(onClick = state.onSelectStartPoint, modifier = Modifier.width(140.dp)) {
                            Text(stringResource(R.string.start))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.destinationAddress ?: stringResource(R.string.not_selected),
                            modifier = Modifier.weight(1f)
                        )
                        AwtButton(
                            onClick = state.onSelectDestinationPoint,
                            modifier = Modifier.width(140.dp)
                        ) {
                            Text(stringResource(R.string.destination))
                        }
                    }

                    val distanceText =
                        state.distanceKm?.let { "${it.roundToInt()} km" } ?: stringResource(R.string.not_available)
                    val roundedDistance = state.distanceKm?.roundToInt() ?: 0
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.distance), fontWeight = FontWeight.SemiBold)
                            AwtButton(
                                onClick = { state.onConfirmDistance(roundedDistance.toString()) },
                                enabled = roundedDistance > 0
                            ) {
                                Text(stringResource(R.string.confirm))
                            }
                        }
                        Text(distanceText)
                    }
                }
            }
        }
    }
}
