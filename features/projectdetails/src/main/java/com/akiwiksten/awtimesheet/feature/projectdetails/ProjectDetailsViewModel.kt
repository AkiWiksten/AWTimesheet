package com.akiwiksten.awtimesheet.feature.projectdetails

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Parcelize
sealed class ProjectDetailsUiState : Parcelable {
    @Parcelize
    data object Loading : ProjectDetailsUiState()

    @Parcelize
    data class Success(
        val details: ProjectDetailsState,
        val settings: SettingsState = SettingsState()
    ) : ProjectDetailsUiState()

    @Parcelize
    data class Error(val message: String) : ProjectDetailsUiState()
}

/**
 * ViewModel for managing the project details screen.
 * Focused on loading the initial data (baseline) for the screen.
 * Form editing state is managed in the Composable layer.
 */
@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailsUiState>(ProjectDetailsUiState.Loading)
    val uiState: StateFlow<ProjectDetailsUiState> = _uiState.asStateFlow()
    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete: StateFlow<Boolean> = _isInitialLoadComplete.asStateFlow()

    private var dateObserverJob: Job? = null
    private var loadProjectDetailsJob: Job? = null

    fun observeDateRepository(detailsArgs: ProjectDetailsState) {
        dateObserverJob?.cancel()
        dateObserverJob = viewModelScope.launch {
            dateRepository.selectedDate.collectLatest { date ->
                loadProjectDetails(
                    date = date,
                    projectDetailsArg = detailsArgs.copy(date = date)
                )
            }
        }
    }

    fun loadProjectDetails(
        date: String,
        projectDetailsArg: ProjectDetailsState? = null
    ) {
        if (date.isBlank()) {
            _isInitialLoadComplete.value = true
            return
        }

        val currentState = _uiState.value
        val showLoading = currentState !is ProjectDetailsUiState.Success && projectDetailsArg == null
        if (showLoading) {
            _isInitialLoadComplete.value = false
        }

        val baseState = currentState as? ProjectDetailsUiState.Success
            ?: ProjectDetailsUiState.Success(details = ProjectDetailsState())

        val projectName = projectDetailsArg?.projectName ?: ""

        // Cancel any in-flight load so rapid date changes always keep the latest result.
        loadProjectDetailsJob?.cancel()
        loadProjectDetailsJob = viewModelScope.launch {
            loadProjectDetailsInternal(
                baseState = baseState,
                date = date,
                projectName = projectName,
                projectDetailsArg = projectDetailsArg,
                showLoading = showLoading
            )
        }
    }

    private suspend fun loadProjectDetailsInternal(
        baseState: ProjectDetailsUiState.Success,
        date: String,
        projectName: String,
        projectDetailsArg: ProjectDetailsState? = null,
        showLoading: Boolean
    ) {
        if (showLoading && _uiState.value !is ProjectDetailsUiState.Success) {
            _uiState.value = ProjectDetailsUiState.Loading
        }

        try {
            val projectDetails = projectDetailsRepository.getProjectDetails(
                date,
                projectName
            ) ?: projectDetailsArg
            val settings = settingsRepository.getEffectiveSettingsForDate(date)
            val normalizedProjectDetails = ProjectDetailsUiMapper.normalizeProjectDetails(
                projectDetails,
                settings
            )

            val nextState = createNextState(
                baseState = baseState,
                date = date,
                projectName = projectName,
                projectDetails = normalizedProjectDetails,
                settings = settings
            )
            _uiState.value = nextState
        } catch (e: IllegalArgumentException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid argument")
        } catch (e: IllegalStateException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid state")
        } finally {
            _isInitialLoadComplete.value = true
        }
    }

    private fun createNextState(
        baseState: ProjectDetailsUiState.Success,
        date: String,
        projectName: String,
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsUiState.Success {
        return ProjectDetailsUiMapper.mapEntitiesToUiState(
            baseState.copy(
                details = baseState.details.copy(
                    date = date,
                    projectName = projectName,
                )
            ),
            projectDetails,
            settings
        )
    }
}
