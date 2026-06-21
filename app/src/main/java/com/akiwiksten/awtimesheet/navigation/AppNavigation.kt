package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorScreen
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorViewModel
import com.akiwiksten.awtimesheet.feature.location.LocationPickerScreen
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
import kotlinx.parcelize.Parcelize
import kotlin.math.min

@Parcelize
private class BackStackData(val items: ArrayList<Screen>) : Parcelable

private val BackStackSaver: Saver<SnapshotStateList<Any>, BackStackData> = Saver(
    save = { stack -> BackStackData(ArrayList(stack.filterIsInstance<Screen>())) },
    restore = { data ->
        mutableStateListOf<Any>().apply {
            addAll(data.items)
        }
    }
)

@Composable
fun AWTimesheetApp() {
    val backStack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Any>(Screen.Intro) }
    val settingsNavigationGuard = rememberSettingsNavigationGuard()
    val isIntroRoute = backStack.lastOrNull() is Screen.Intro
    val portraitWidth = rememberPortraitWidthDp()

    if (isIntroRoute) {
        WorkTimeNavDisplay(
            backStack = backStack,
            settingsNavigationGuard = settingsNavigationGuard,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        MainAppScaffold(
            backStack = backStack,
            portraitWidth = portraitWidth,
            settingsNavigationGuard = settingsNavigationGuard
        )
    }
}

@Composable
private fun rememberPortraitWidthDp(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        min(windowInfo.containerSize.width, windowInfo.containerSize.height).toDp()
    }
}

@Composable
internal fun PortraitWidthContainer(
    portraitWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(width = portraitWidth)
        ) {
            content()
        }
    }
}

@Composable
internal fun LocationEntry(
    screen: Screen.Location,
    backStack: SnapshotStateList<Any>,
    viewModel: DistanceCalculatorViewModel = hiltViewModel(),
) {
    val routeHistory by viewModel.routeHistory.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()

    val cardState = rememberLocationCardState(
        screen = screen,
        selectedRoute = selectedRoute,
    )
    val cardUiState = cardState.value.toDistanceCalculatorCardUiState()

    DistanceCalculatorScreen(
        state = createDistanceCalculatorScreenState(
            backStack = backStack,
            viewModel = viewModel,
            params = DistanceCalculatorUiParams(
                routeHistory = routeHistory,
                selectedRoute = selectedRoute,
                cardUiState = cardUiState,
            ),
            onTripTypeChange = { isRoundTrip ->
                cardState.value = reduceLocationCardState(
                    current = cardState.value,
                    event = LocationCardEvent.TripTypeChanged(isRoundTrip = isRoundTrip)
                )
            }
        )
    )
}

@Composable
internal fun LocationPickerEntry(
    screen: Screen.LocationPicker,
    backStack: SnapshotStateList<Any>,
) {
    val titleResId = when (screen.target) {
        Screen.LocationTarget.START ->
            com.akiwiksten.awtimesheet.feature.location.R.string.select_location_start_title
        Screen.LocationTarget.DESTINATION ->
            com.akiwiksten.awtimesheet.feature.location.R.string.select_location_destination_title
    }

    LocationPickerScreen(
        titleResId = titleResId,
        initialAddress = screen.initialPoint?.address,
        initialLatLng = screen.initialPoint?.let { point ->
            com.google.android.gms.maps.model.LatLng(point.latitude, point.longitude)
        },
        onLocationSelected = { result ->
            backStack.updateLocationSelection(target = screen.target, result = result)
            backStack.pop()
        },
        onNavigateBack = { backStack.pop() }
    )
}

@Composable
internal fun ProjectDetailsEntry(screen: Screen.ProjectDetails, backStack: SnapshotStateList<Any>) {
    ProjectDetailsScreen(
        detailsArgs = ProjectDetailsState(
            date = screen.date,
            projectName = screen.projectName,
            originalProjectName = screen.originalProjectName,
            projectTime = screen.projectTime,
            startTime = screen.startTime,
            endTime = screen.endTime,
            lunchStart = screen.lunchStart,
            lunchEnd = screen.lunchEnd,
            breakStart = screen.breakStart,
            breakEnd = screen.breakEnd,
        ),
        onNavigateBack = { backStack.pop() },
        onConfirm = { details ->
            backStack.updateSingleProjectWorkTime(
                details = Screen.ProjectDetails(
                    date = details.date,
                    projectName = details.projectName,
                    originalProjectName = details.originalProjectName,
                    projectTime = details.projectTime,
                    startTime = details.startTime,
                    endTime = details.endTime,
                    lunchStart = details.lunchStart,
                    lunchEnd = details.lunchEnd,
                    breakStart = details.breakStart,
                    breakEnd = details.breakEnd,
                )
            )
        }
    )
}

@Composable
internal fun SingleProjectEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    SingleProjectScreen(
        routeArgs = SingleProjectRouteArgs(
            projectName = screen.projectName ?: "",
            originalProjectName = screen.originalProjectName ?: "",
            projectTime = screen.projectTime ?: "",
            isAddMode = screen.listIndex == -1,
            listIndex = screen.listIndex,
            kilometres = screen.kilometres,
            allowance = screen.allowance,
            workType = screen.workType,
            comment = screen.comment,
            projectDetails = if (screen.details == null) {
                null
            } else {
                ProjectDetailsState(
                    date = screen.details.date,
                    projectName = screen.details.projectName,
                    originalProjectName = screen.details.originalProjectName,
                    projectTime = screen.details.projectTime,
                    startTime = screen.details.startTime,
                    endTime = screen.details.endTime,
                    lunchStart = screen.details.lunchStart,
                    lunchEnd = screen.details.lunchEnd,
                    breakStart = screen.details.breakStart,
                    breakEnd = screen.details.breakEnd
                )
            }
        ),
        navigationActions = SingleProjectNavigationActions(
            onNavigateBack = { backStack.pop() },
            onOpenProjectDetails = { singleProject, projectDetails ->
                backStack.updateSingleProjectState(singleProject)
                backStack.add(
                    element = Screen.ProjectDetails(
                        date = projectDetails?.date ?: "",
                        projectTime = singleProject.projectTime,
                        projectName = singleProject.projectName,
                        originalProjectName = projectDetails?.originalProjectName ?: "",
                        startTime = projectDetails?.startTime ?: "",
                        endTime = projectDetails?.endTime ?: "",
                        lunchStart = projectDetails?.lunchStart ?: "",
                        lunchEnd = projectDetails?.lunchEnd ?: "",
                        breakStart = projectDetails?.breakStart ?: "",
                        breakEnd = projectDetails?.breakEnd ?: ""
                    )
                )
            },
            onNavigateToLocationPicker = { singleProject ->
                backStack.updateSingleProjectState(singleProject)
                backStack.add(element = Screen.Location())
            }
        )
    )
}
