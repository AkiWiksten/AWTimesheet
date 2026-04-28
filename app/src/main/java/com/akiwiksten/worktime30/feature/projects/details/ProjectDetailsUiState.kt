package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState

sealed class ProjectDetailsUiState {
    object Loading : ProjectDetailsUiState()

    data class Success(
        val data: ProjectDetailsState,
        val workStats: SettingsState = SettingsState()
    ) : ProjectDetailsUiState()

    data class Error(val message: String) : ProjectDetailsUiState()
}
