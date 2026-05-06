package com.akiwiksten.worktime30.feature.projects.single

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.SaveWorkdayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SingleProjectViewModel @Inject constructor(
    private val saveWorkdayUseCase: SaveWorkdayUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    fun saveProject(
        state: SingleProjectState,
        projectDetails: ProjectDetailsState? = null,
        settings: SettingsState? = null
    ) {
        viewModelScope.launch {
            try {
                val date = dateRepository.selectedDate.first()
                val projectToSave = state.copy(date = date)

                val projectDetailsToSave = projectDetails?.copy(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                ) ?: ProjectDetailsState(
                    date = date,
                    projectName = state.projectName,
                    projectTime = state.projectTime
                )

                saveWorkdayUseCase(
                    projectsToSave = listOf(projectToSave),
                    projectDetailsToSave = projectDetailsToSave
                )

                settings?.let { settingsRepository.insertSettings(it) }
            } catch (e: IllegalArgumentException) {
                Log.e("SingleProjectViewModel", "saveProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("SingleProjectViewModel", "saveProject: ", e)
            }
        }
    }
}
