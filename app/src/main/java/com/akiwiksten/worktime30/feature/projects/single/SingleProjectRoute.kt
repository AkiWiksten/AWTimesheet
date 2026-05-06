package com.akiwiksten.worktime30.feature.projects.single

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SingleProjectRoute(
    args: SingleProjectScreenArgs,
    navigationActions: SingleProjectNavigationActions
) {
    val vm: SingleProjectViewModel = hiltViewModel()

    LifecycleResumeEffect(
        args.initialSingleProjectState.date,
        args.initialSingleProjectState.projectName,
        args.initialSingleProjectState.index
    ) {
        vm.setInitialValues(
            date = args.initialSingleProjectState.date,
            projectName = args.initialSingleProjectState.projectName,
            workTimeByDate = args.initialSingleProjectState.projectTime // or real day total source
        )
        vm.initializeState()
        onPauseOrDispose {
            // Optional cleanup
        }
    }

    val uiState by vm.uiState.collectAsStateWithLifecycle()

    SingleProjectScreen(
        args = args,
        navigationActions = navigationActions,
        uiState = uiState,
        singleProjectViewModel = vm,
    )
}


