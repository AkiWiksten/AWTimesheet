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
    val originalProjectName: String = "",
    val projectTime: String,
    val isAddMode: Boolean,
    val listIndex: Int,
    val kilometres: String? = null,
    val allowance: String? = null,
    val workType: String? = null,
    val comment: String? = null,
    val projectDetails: ProjectDetailsState? = null
)

data class SingleProjectNavigationActions(
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit,
    val onNavigateToLocationPicker: (SingleProjectState) -> Unit
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
    val isDuplicateProjectName: Boolean,
    val isTimePickerDisabled: Boolean
)

data class SingleProjectActions(
    val onStateChange: (SingleProjectState) -> Unit,
    val onOpenProjectDetails: () -> Unit,
    val onSave: () -> Unit,
    val onNavigateToLocationPicker: () -> Unit
)

data class SingleProjectConfiguration(
    val absencePrefix: String,
    val flexDayWorkType: String
)

data class SingleProjectScreenParams(
    val screenState: SingleProjectScreenState,
    val actions: SingleProjectActions,
    val config: SingleProjectConfiguration
)
