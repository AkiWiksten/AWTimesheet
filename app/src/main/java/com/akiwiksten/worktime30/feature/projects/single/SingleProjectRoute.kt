package com.akiwiksten.worktime30.feature.projects.single

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.worktime30.core.ui.sharedActivityViewModel
import com.akiwiksten.worktime30.feature.workday.WorkdayViewModel

@Composable
fun SingleProjectRoute(
    args: SingleProjectScreenArgs,
    navigationActions: SingleProjectNavigationActions
) {
    val workdayViewModel: WorkdayViewModel = sharedActivityViewModel()
    val projectsUiState = workdayViewModel.uiState.collectAsStateWithLifecycle().value

    SingleProjectScreen(
        args = args,
        navigationActions = navigationActions,
        projectsUiState = projectsUiState,
        onSaved = {
            //workdayViewModel.retryLoad()
        }
    )
}


