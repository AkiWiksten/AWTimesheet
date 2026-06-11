package com.akiwiksten.awtimesheet.feature.singleproject.model

import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectUiState

data class SingleProjectScreenArgs(
    val initialSingleProjectState: SingleProjectState,
    val initialProjectDetails: ProjectDetailsState? = null,
    val initialSettings: SettingsState? = null
)

data class SingleProjectRouteArgs(
    val projectName: String,
    val projectTime: String,
    val isAddMode: Boolean,
    val listIndex: Int
)

data class SingleProjectNavigationActions(
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: (SingleProjectState) -> Unit
)

data class SingleProjectDerivedState(
    val hasUnsavedChanges: Boolean,
    val isDuplicate: Boolean,
    val isConfirmEnabled: Boolean
)

data class SingleProjectScreenState(
    val date: String,
    val editedProjectIndex: Int,
    val state: SingleProjectState,
    val isAddMode: Boolean,
    val isProjectNameEditable: Boolean,
    val uiState: SingleProjectUiState,
    val isConfirmEnabled: Boolean,
    val isDuplicateProjectName: Boolean
)

data class SingleProjectActions(
    val onStateChange: (SingleProjectState) -> Unit,
    val onOpenProjectDetails: () -> Unit,
    val onSave: () -> Unit
)
