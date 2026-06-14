package com.akiwiksten.awtimesheet.feature.projectdetails

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

sealed class ProjectDetailsUiState : Parcelable {
    @Parcelize
    data object Loading : ProjectDetailsUiState()

    @Parcelize
    data class Success(
        val details: ProjectDetailsState,
        val settings: SettingsState = SettingsState(),
        val persistedProjectTime: String = ZERO_TIME
    ) : ProjectDetailsUiState()

    @Parcelize
    data class Error(val message: String) : ProjectDetailsUiState()
}

@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProjectDetailsUiState>(ProjectDetailsUiState.Loading)
    val uiState: StateFlow<ProjectDetailsUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    fun observeDateRepository(detailsArgs: ProjectDetailsState) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            dateRepository.selectedDate.collectLatest { date ->
                loadProjectDetailsInternal(date, detailsArgs)
            }
        }
    }

    fun loadProjectDetails(date: String, projectDetailsArg: ProjectDetailsState) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            loadProjectDetailsInternal(date, projectDetailsArg)
        }
    }

    private suspend fun loadProjectDetailsInternal(date: String, projectDetailsArg: ProjectDetailsState) {
        val details = projectDetailsRepository.getProjectDetails(date, projectDetailsArg.projectName)
        val settings = settingsRepository.getSettings()

        _uiState.value = ProjectDetailsUiMapper.mapEntitiesToUiState(
            baseState = ProjectDetailsUiState.Success(
                details = projectDetailsArg.copy(date = date),
                persistedProjectTime = details?.projectTime ?: ZERO_TIME
            ),
            projectDetails = details,
            settings = settings
        )
    }
}
