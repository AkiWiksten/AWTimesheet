@file:Suppress("FunctionNaming", "MatchingDeclarationName", "LongMethod")

package com.akiwiksten.awtimesheet.feature.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import kotlin.math.roundToInt

private data class LocationRouteItem(
    val distance: String,
    val start: String,
    val destination: String,
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
        LocationScreenContent(state = state, padding = padding)
    }
}

@Composable
private fun LocationScreenContent(
    state: LocationScreenState,
    padding: PaddingValues,
) {
    val distanceText =
        state.distanceKm?.let { "${it.roundToInt()} km" } ?: stringResource(R.string.not_available)
    val routeItem = state.distanceKm?.let {
        LocationRouteItem(
            distance = distanceText,
            start = state.startAddress ?: stringResource(R.string.not_selected),
            destination = state.destinationAddress ?: stringResource(R.string.not_selected),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
    ) {
        LocationInputCard(state = state, distanceText = distanceText)

        routeItem?.let {
            CalculatedRouteList(
                item = it,
                modifier = Modifier.padding(top = PADDING_SPACING)
            )
        }
    }
}

@Composable
private fun LocationInputCard(
    state: LocationScreenState,
    distanceText: String,
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
                AwtButton(onClick = state.onSelectStartPoint, modifier = Modifier.width(120.dp)) {
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
                    modifier = Modifier.width(120.dp)
                ) {
                    Text(stringResource(R.string.destination))
                }
            }

            val roundedDistance = state.distanceKm?.roundToInt() ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.distance), fontWeight = FontWeight.SemiBold)
                    Text(distanceText)
                }
                AwtButton(
                    onClick = { state.onConfirmDistance(roundedDistance.toString()) },
                    enabled = roundedDistance > 0,
                    modifier = Modifier.width(120.dp)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun CalculatedRouteList(
    item: LocationRouteItem,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        modifier = modifier.fillMaxWidth()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            item {
                CalculatedRouteListItem(item)
            }
        }
    }
}

@Composable
private fun CalculatedRouteListItem(item: LocationRouteItem) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING_SMALL),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            Text(text = item.distance, fontWeight = FontWeight.Bold)
            Text(text = item.start)
            Text(text = item.destination)
        }
    }
}
