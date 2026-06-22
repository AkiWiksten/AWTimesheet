@file:Suppress("FunctionNaming", "MatchingDeclarationName", "LongMethod")

package com.akiwiksten.awtimesheet.feature.location

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.AwtCenterAlignedTopAppBar
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.android.tools.screenshot.PreviewTest
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistanceCalculatorScreen(
    state: DistanceCalculatorScreenState,
    viewModel: DistanceCalculatorViewModel = hiltViewModel()
) {
    val routeHistory by viewModel.routeHistory.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()

    BackHandler(onBack = state.onNavigateBack)
    Scaffold(
        topBar = {
            AwtCenterAlignedTopAppBar(
                title = stringResource(id = R.string.distance_calculation_title),
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
        DistanceCalculatorScreenContent(
            state = state.copy(
                routeHistory = routeHistory,
                selectedRoute = selectedRoute
            ),
            padding = padding
        )
    }
}

@Composable
private fun DistanceCalculatorScreenContent(
    state: DistanceCalculatorScreenState,
    padding: PaddingValues,
) {
    val baseDistance = state.distanceKm ?: 0.0
    val effectiveDistance = if (state.isRoundTrip) baseDistance * 2 else baseDistance
    val distanceText =
        if (state.distanceKm != null) "${effectiveDistance.roundToInt()} km" else stringResource(R.string.not_available)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(PADDING_SPACING)
            .padding(bottom = LocalContentBottomPadding.current),
    ) {
        DistanceCalculatorInputCard(state = state, distanceText = distanceText)

        if (state.routeHistory.isNotEmpty()) {
            val canDeleteSelectedRoute = state.selectedRoute?.let { selected ->
                state.routeHistory.any { routeItem ->
                    ((routeItem.start == selected.start) && (routeItem.destination == selected.destination))
                }
            } == true

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PADDING_SPACING_SMALL),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AwtButton(
                    onClick = state.onReturnDistance,
                    enabled = state.selectedRoute != null,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(text = stringResource(R.string.return_distance))
                    }
                }
                IconButton(
                    onClick = state.onDeleteSelectedRoute,
                    enabled = canDeleteSelectedRoute,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete)
                    )
                }
                AwtButton(onClick = state.onClearRouteHistory) {
                    Text(text = stringResource(R.string.clear_route_history))
                }
            }

            CalculatedRouteList(
                items = state.routeHistory,
                selectedRoute = state.selectedRoute,
                onRouteSelected = state.onRouteSelected,
                modifier = Modifier.padding(top = PADDING_SPACING_SMALL)
            )
        } else {
            EmptyHistoryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = PADDING_SPACING)
            )
        }
    }
}

@Composable
private fun DistanceCalculatorInputCard(
    state: DistanceCalculatorScreenState,
    distanceText: String,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = DEFAULT_ELEVATION,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(PADDING_SPACING)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PADDING_SPACING_SMALL),
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
                horizontalArrangement = Arrangement.spacedBy(PADDING_SPACING_SMALL),
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

            TripTypeSelector(
                isRoundTrip = state.isRoundTrip,
                onTripTypeChange = state.onTripTypeChange
            )

            val baseDistance = state.distanceKm ?: 0.0
            val roundedDistance = if (state.isRoundTrip) (baseDistance * 2).roundToInt() else baseDistance.roundToInt()
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
                    onClick = { state.onAddToList(roundedDistance.toString()) },
                    enabled = roundedDistance > 0,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.add_to_list))
                        Spacer(modifier = Modifier.size(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatedRouteList(
    items: List<RouteState>,
    selectedRoute: RouteState?,
    onRouteSelected: (RouteState) -> Unit,
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
            itemsIndexed(
                items = items,
                key = { _, routeItem ->
                    "${routeItem.start}_${routeItem.destination}"
                }
            ) { _, routeItem ->
                CalculatedRouteListItem(
                    item = routeItem,
                    isSelected = selectedRoute?.let { selected ->
                        ((selected.start == routeItem.start) && (selected.destination == routeItem.destination))
                    } == true
                ) {
                    onRouteSelected(routeItem)
                }
            }
        }
    }
}

@Composable
private fun CalculatedRouteListItem(
    item: RouteState,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING_SMALL),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.distance, fontWeight = FontWeight.Bold)
                val tripTypeResId = if (item.distance.contains("*2")) R.string.round_trip else R.string.one_way
                Text(
                    text = stringResource(tripTypeResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(text = item.start)
            Text(text = item.destination)
        }
    }
}

@Composable
private fun TripTypeSelector(
    isRoundTrip: Boolean,
    onTripTypeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PADDING_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onTripTypeChange(false) }
        ) {
            RadioButton(
                selected = !isRoundTrip,
                onClick = { onTripTypeChange(false) }
            )
            Text(text = stringResource(id = R.string.one_way))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onTripTypeChange(true) }
        ) {
            RadioButton(
                selected = isRoundTrip,
                onClick = { onTripTypeChange(true) }
            )
            Text(text = stringResource(id = R.string.round_trip))
        }
    }
}

@Composable
private fun EmptyHistoryCard(
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_history_items),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val PreviewRouteHistory = listOf(
    RouteState(
        start = "Office",
        destination = "Client A",
        distance = "24 km"
    ),
    RouteState(
        start = "Client A",
        destination = "Office",
        distance = "24 km"
    )
)

@PreviewTest
@Preview(showBackground = true, name = "Distance Calculator - Empty")
@Composable
fun PreviewDistanceCalculatorEmpty() {
    DistanceCalculatorPreviewContent(
        state = DistanceCalculatorScreenState(
            startAddress = null,
            destinationAddress = null,
            distanceKm = null,
            onClearRouteHistory = {},
            onRouteSelected = {},
            onSelectStartPoint = {},
            onSelectDestinationPoint = {},
            onTripTypeChange = {},
            onAddToList = {},
            onReturnDistance = {},
            onDeleteSelectedRoute = {},
            onNavigateBack = {}
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Distance Calculator - With History")
@Composable
fun PreviewDistanceCalculatorWithHistory() {
    val selectedRoute = PreviewRouteHistory.first()
    DistanceCalculatorPreviewContent(
        state = DistanceCalculatorScreenState(
            startAddress = selectedRoute.start,
            destinationAddress = selectedRoute.destination,
            distanceKm = 24.2,
            routeHistory = PreviewRouteHistory,
            selectedRoute = selectedRoute,
            onClearRouteHistory = {},
            onRouteSelected = {},
            onSelectStartPoint = {},
            onSelectDestinationPoint = {},
            onTripTypeChange = {},
            onAddToList = {},
            onReturnDistance = {},
            onDeleteSelectedRoute = {},
            onNavigateBack = {}
        )
    )
}

@Composable
private fun DistanceCalculatorPreviewContent(state: DistanceCalculatorScreenState) {
    AWTimesheetTheme(dynamicColor = false) {
        DistanceCalculatorScreen(state = state)
    }
}
