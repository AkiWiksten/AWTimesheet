package com.akiwiksten.awtimesheet.feature.workday

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.AbsenceFlexDayMatcher
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteProjectUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsParams
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsUseCase
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("detekt.TooManyFunctions")
@HiltViewModel
class WorkdayViewModel @Inject constructor(
    private val getWorkdayScreenDataUseCase: GetWorkdayScreenDataUseCase,
    private val deleteProjectUseCase: DeleteProjectUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {
    private var localizedFlexDayWorkType = ""

    private val refreshTrigger = MutableStateFlow(value = 0)

    val uiState: StateFlow<WorkdayUiState> = combine(
        refreshTrigger,
        dateRepository.selectedDate,
        dateRepository.calendarRefreshVersion
    ) { _, date, _ -> date }
        .map { date ->
            val data = getWorkdayScreenDataUseCase(date)
            val recordedNames = data.projects.map { it.projectName }.toSet()

            val unrecordedProjects = data.projectNames
                .filter { it !in recordedNames }
                .map { name ->
                    SingleProjectState(projectName = name)
                }

            val allProjects = (data.projects + unrecordedProjects)
                .sortedBy { it.projectName }
                .mapIndexed { listIndex, project -> project.copy(listIndex = listIndex) }

            val workTypes = settingsRepository.getWorkTypes()
            val flexByDateResult = calculateFlexTimeByDate(
                projects = data.projects,
                workTimeByDate = data.workTimeByDate,
                workTimeByDateEstimate = data.workTimeByDateEstimate
            )

            WorkdayUiState.Success(
                date = date,
                workTimeByDate = data.workTimeByDate,
                workTimeByDateEstimate = data.workTimeByDateEstimate,
                flexTimeByDate = flexByDateResult.value,
                isFlexTimeByDateSpecialRuleApplied = flexByDateResult.isSpecialRuleApplied,
                initialFlexTimeTotal = data.initialFlexTimeTotal,
                flexTimeTotal = data.flexTimeTotal,
                projects = allProjects,
                workTypes = workTypes
            ) as WorkdayUiState
        }
        .onStart { emit(value = WorkdayUiState.Loading) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WorkdayUiState.Loading
        )

    fun retryLoad() {
        requestReload()
    }

    @Suppress("unused")
    fun setLocalizedFlexDayWorkType(workType: String) {
        val normalized = normalizeLabel(workType)
        if (normalized.isNotEmpty() && normalized != localizedFlexDayWorkType) {
            localizedFlexDayWorkType = normalized
            requestReload()
        }
    }

    private fun requestReload() {
        refreshTrigger.value += 1
    }

    fun reconcileFlexTimeTotalAfterProjectEditorReturn(oldFlexTimeByDate: String, oldWorkTimeByDate: String) {
        viewModelScope.launch {
            try {
                reconcileFlexTimeTotal(
                    oldFlexTimeByDate = oldFlexTimeByDate,
                    oldWorkTimeByDate = oldWorkTimeByDate
                )
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "reconcileFlexTimeTotalAfterProjectEditorReturn: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "reconcileFlexTimeTotalAfterProjectEditorReturn: ", e)
            }
        }
    }

    fun deleteProject(state: SingleProjectState) {
        viewModelScope.launch {
            try {
                val currentState = uiState.value as? WorkdayUiState.Success ?: return@launch
                val date = currentState.date
                val oldFlexTimeByDate = currentState.flexTimeByDate
                val oldWorkTimeByDate = currentState.workTimeByDate

                deleteProjectUseCase(date = date, projectName = state.projectName, projectTime = state.projectTime)
                if (state.projectTime != ZERO_TIME) {
                    dateRepository.addWorkTimeByDateChange("-${state.projectTime}")
                }
                reconcileFlexTimeTotal(
                    oldFlexTimeByDate = oldFlexTimeByDate,
                    oldWorkTimeByDate = oldWorkTimeByDate
                )
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "deleteProject: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "deleteProject: ", e)
            }
        }
    }

    fun updateSettings(workTimeByDateEstimate: String, updateGlobalSettings: Boolean = false) {
        if (!WorkTimeCalculator.isValidTimeInput(workTimeByDateEstimate)) {
            return
        }

        viewModelScope.launch {
            try {
                val currentUiState = uiState.value as? WorkdayUiState.Success ?: return@launch
                val oldFlexTimeByDate = currentUiState.flexTimeByDate
                val oldWorkTimeByDate = currentUiState.workTimeByDate
                updateSettingsUseCase(
                    UpdateSettingsParams(
                        date = currentUiState.date,
                        workTimeByDate = currentUiState.workTimeByDate,
                        currentWorkTimeByDateEstimate = currentUiState.workTimeByDateEstimate,
                        newWorkTimeByDateEstimate = workTimeByDateEstimate,
                        newInitialFlexTimeTotal = currentUiState.initialFlexTimeTotal,
                        updateGlobalSettings = updateGlobalSettings
                    )
                )
                reconcileFlexTimeTotalAfterEstimateUpdate(
                    oldFlexTimeByDate = oldFlexTimeByDate,
                    workTimeByDate = oldWorkTimeByDate,
                    newWorkTimeByDateEstimate = workTimeByDateEstimate
                )
            } catch (e: IllegalArgumentException) {
                Log.e("WorkdayViewModel", "updateSettings: ", e)
            } catch (e: IllegalStateException) {
                Log.e("WorkdayViewModel", "updateSettings: ", e)
            }
        }
    }

    private suspend fun reconcileFlexTimeTotalAfterEstimateUpdate(
        oldFlexTimeByDate: String,
        workTimeByDate: String,
        newWorkTimeByDateEstimate: String
    ) {
        val date = dateRepository.selectedDate.value
        val latestData = getWorkdayScreenDataUseCase(date)
        val latestWorkTimeByDate = latestData.workTimeByDate
        val newFlexTimeByDate = calculateFlexTimeByDate(
            projects = latestData.projects,
            workTimeByDate = latestWorkTimeByDate,
            workTimeByDateEstimate = newWorkTimeByDateEstimate
        ).value
        val oldFlexContribution = WorkTimeCalculator.resolveFlexContribution(
            flexTimeByDate = oldFlexTimeByDate,
            workTimeByDate = workTimeByDate
        )
        val newFlexContribution = WorkTimeCalculator.resolveFlexContribution(
            flexTimeByDate = newFlexTimeByDate,
            workTimeByDate = latestWorkTimeByDate
        )
        val flexTimeByDateDelta = WorkTimeCalculator.calculateFlexTime(
            initialTime = newFlexContribution,
            addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$oldFlexContribution")
        )

        if (flexTimeByDateDelta != ZERO_TIME) {
            val persistedCalculatedFlexTimeTotal = settingsRepository.getCalculatedFlextimeTotal()
            val updatedCalculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = persistedCalculatedFlexTimeTotal,
                addedTime = flexTimeByDateDelta
            )
            settingsRepository.insertCalculatedFlextimeTotal(updatedCalculatedFlexTimeTotal)
        }

        requestReload()
    }

    private suspend fun reconcileFlexTimeTotal(
        oldFlexTimeByDate: String,
        oldWorkTimeByDate: String
    ) {
        val date = dateRepository.selectedDate.value
        if (date.isBlank()) {
            requestReload()
            return
        }

        val latestData = getWorkdayScreenDataUseCase(date)
        val newWorkTimeByDate = latestData.workTimeByDate
        val newFlexTimeByDate = calculateFlexTimeByDate(
            projects = latestData.projects,
            workTimeByDate = latestData.workTimeByDate,
            workTimeByDateEstimate = latestData.workTimeByDateEstimate
        ).value
        val oldFlexContribution = WorkTimeCalculator.resolveFlexContribution(
            flexTimeByDate = oldFlexTimeByDate,
            workTimeByDate = oldWorkTimeByDate
        )
        val newFlexContribution = WorkTimeCalculator.resolveFlexContribution(
            flexTimeByDate = newFlexTimeByDate,
            workTimeByDate = newWorkTimeByDate
        )
        val flexTimeByDateDelta = WorkTimeCalculator.calculateFlexTime(
            initialTime = newFlexContribution,
            addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$oldFlexContribution")
        )

        if (flexTimeByDateDelta != ZERO_TIME) {
            val persistedCalculatedFlexTimeTotal = settingsRepository.getCalculatedFlextimeTotal()
            val updatedCalculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = persistedCalculatedFlexTimeTotal,
                addedTime = flexTimeByDateDelta
            )
            settingsRepository.insertCalculatedFlextimeTotal(updatedCalculatedFlexTimeTotal)
        }

        requestReload()
    }

    private fun calculateFlexTimeByDate(
        projects: List<SingleProjectState>,
        workTimeByDate: String,
        workTimeByDateEstimate: String
    ): FlexByDateResult {
        val flexDayProjectTime = projects
            .filter { project -> isAbsenceFlexDay(project) }
            .fold(ZERO_TIME) { total, project ->
                WorkTimeCalculator.calculateFlexTime(
                    initialTime = total,
                    addedTime = project.projectTime
                )
            }

        if (flexDayProjectTime != ZERO_TIME) {
            return FlexByDateResult(
                value = WorkTimeCalculator.normalizeDuplicateMinus("-$flexDayProjectTime"),
                isSpecialRuleApplied = true
            )
        }

        return FlexByDateResult(
            value = WorkTimeCalculator.calculateFlexTimeByDate(
                workTimeByDate = workTimeByDate,
                workTimeByDateEstimate = workTimeByDateEstimate
            )
        )
    }

    private data class FlexByDateResult(
        val value: String,
        val isSpecialRuleApplied: Boolean = false
    )

    private fun isAbsenceFlexDay(project: SingleProjectState): Boolean {
        return AbsenceFlexDayMatcher.isAbsenceFlexDay(
            workType = project.workType,
            projectName = project.projectName,
            localizedFlexDayWorkType = localizedFlexDayWorkType
        )
    }

    private fun normalizeLabel(value: String): String {
        val withoutDiacritics = Normalizer
            .normalize(value.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return withoutDiacritics
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }
}
