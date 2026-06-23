package com.akiwiksten.awtimesheet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.feature.absence.AbsenceScreen
import com.akiwiksten.awtimesheet.feature.absence.CreateAbsenceScreen
import com.akiwiksten.awtimesheet.feature.calendar.CalendarScreen
import com.akiwiksten.awtimesheet.feature.intro.IntroScreen
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorScreen
import com.akiwiksten.awtimesheet.feature.location.LocationPickerScreen
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.settings.SettingsScreen
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
import com.akiwiksten.awtimesheet.feature.workday.WorkdayScreen

@Composable
internal fun IntroNavEntry(backStack: SnapshotStateList<Any>) {
    IntroScreen(onItemClick = { backStack.add(element = Screen.Calendar) })
}

@Composable
internal fun CalendarNavEntry() {
    CalendarScreen()
}

@Composable
internal fun AbsenceNavEntry(backStack: SnapshotStateList<Any>) {
    AbsenceScreen(
        onNavigateToCreateAbsence = { backStack.add(element = Screen.CreateAbsence) }
    )
}

@Composable
internal fun CreateAbsenceNavEntry(backStack: SnapshotStateList<Any>) {
    CreateAbsenceScreen(
        onNavigateBack = { backStack.pop() }
    )
}

@Composable
internal fun WorkdayNavEntry(backStack: SnapshotStateList<Any>) {
    WorkdayScreen(
        onNavigateToSingleProject = { project ->
            backStack.add(
                element = Screen.SingleProject(
                    listIndex = project.listIndex,
                    projectName = project.projectName,
                    originalProjectName = if (project.isAddMode) "" else project.projectName,
                    isAddMode = project.isAddMode
                )
            )
        }
    )
}

@Composable
internal fun SettingsNavEntry(settingsNavigationGuard: SettingsNavigationGuard) {
    SettingsScreen(
        onUnsavedChangesChanged = settingsNavigationGuard.onUnsavedChangesChanged,
        registerUnsavedActions = settingsNavigationGuard.registerUnsavedActions
    )
}

@Composable
internal fun ProjectDetailsNavEntry(screen: Screen.ProjectDetails, backStack: SnapshotStateList<Any>) {
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
            backStack.updateSingleProjectFromDetails(
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
internal fun SingleProjectNavEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    SingleProjectScreen(
        routeArgs = SingleProjectRouteArgs(
            projectName = screen.projectName ?: "",
            originalProjectName = screen.originalProjectName ?: "",
            projectTime = screen.projectTime ?: "",
            isAddMode = screen.isAddMode,
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
            onNavigateToDistanceCalculator = { singleProject ->
                backStack.updateSingleProjectState(singleProject)
                backStack.add(element = Screen.DistanceCalculator())
            }
        )
    )
}

@Composable
internal fun DistanceCalculatorNavEntry(
    screen: Screen.DistanceCalculator,
    backStack: SnapshotStateList<Any>,
) {
    DistanceCalculatorScreen(
        initialStartPoint = screen.startPoint?.toFeatureLocationPoint(),
        initialDestinationPoint = screen.destinationPoint?.toFeatureLocationPoint(),
        onSelectStartPoint = { start, destination ->
            navigateToLocationPicker(
                backStack = backStack,
                startPoint = start,
                destinationPoint = destination,
                target = Screen.LocationTarget.START,
            )
        },
        onSelectDestinationPoint = { start, destination ->
            navigateToLocationPicker(
                backStack = backStack,
                startPoint = start,
                destinationPoint = destination,
                target = Screen.LocationTarget.DESTINATION,
            )
        },
        onConfirmDistance = { backStack.confirmLocationDistance(it) },
        onNavigateBack = { backStack.pop() }
    )
}

@Composable
internal fun LocationPickerNavEntry(
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

