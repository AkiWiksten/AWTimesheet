package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.domain.model.ProjectDetailsState

sealed class ProjectDetailsUiState {
    object Loading : ProjectDetailsUiState()

    data class Success(
        val data: ProjectDetailsState
    ) : ProjectDetailsUiState()

    data class Error(val message: String) : ProjectDetailsUiState()
}
