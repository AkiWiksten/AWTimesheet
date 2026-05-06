package com.akiwiksten.worktime30.feature.projects.single

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.worktime30.core.ui.sharedActivityViewModel
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.workday.WorkdayViewModel

@Composable
fun SingleProjectRoute(
    args: SingleProjectScreenArgs,
    navigationActions: SingleProjectNavigationActions,
    onSavedAndNavigateBack: () -> Unit
) {
    val workdayViewModel: WorkdayViewModel = sharedActivityViewModel()
    val projectsUiState = workdayViewModel.uiState.collectAsStateWithLifecycle().value

    SingleProjectScreen(
        args = args,
        navigationActions = navigationActions,
        projectsUiState = projectsUiState,
        onSave = { state: SingleProjectState, details: ProjectDetailsState?, settings: SettingsState? ->
            workdayViewModel.saveProject(state, details, settings)
            onSavedAndNavigateBack()
        }
    )
}


