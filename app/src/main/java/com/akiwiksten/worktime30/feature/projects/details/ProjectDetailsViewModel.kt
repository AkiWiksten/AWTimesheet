package com.akiwiksten.worktime30.feature.projects.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.ProjectDetailsTimeUpdateCalculator
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.hasOnlyProjectTime
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for managing the project details screen.
 */
@HiltViewModel
class ProjectDetailsViewModel @Inject constructor(
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val settingsRepository: SettingsRepository,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectDetailsUiState>(ProjectDetailsUiState.Loading)
    val uiState: StateFlow<ProjectDetailsUiState> = _uiState.asStateFlow()
    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete: StateFlow<Boolean> = _isInitialLoadComplete.asStateFlow()
    private val selectedDate = MutableStateFlow("")
    private val selectedProjectName = MutableStateFlow("")

    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)

    fun observeDateRepository(args: ProjectDetailsInitialData) {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                setDate(date = date)
                loadProjectDetails(
                    projectDetailsArg = args.projectDetails?.copy(date = date),
                    settingsArg = args.settings
                )
            }
        }
    }

    fun setDate(date: String) {
        selectedDate.value = date
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> {
                    currentState.copy(
                        details = currentState.details.copy(date = date)
                    )
                }

                else -> {
                    ProjectDetailsUiState.Success(
                        ProjectDetailsState(date = date)
                    )
                }
            }
        }
    }

    fun setProjectName(projectName: String) {
        selectedProjectName.value = projectName
        _uiState.update { currentState ->
            when (currentState) {
                is ProjectDetailsUiState.Success -> {
                    currentState.copy(
                        details = currentState.details.copy(projectName = projectName)
                    )
                }

                else -> {
                    ProjectDetailsUiState.Success(
                        details = ProjectDetailsState(projectName = projectName)
                    )
                }
            }
        }
    }

    fun observeSelectionChanges() {
        viewModelScope.launch {
            combine(selectedDate, selectedProjectName) { date, projectName -> date to projectName }
                .distinctUntilChanged()
                .collect { (date, projectName) ->
                    if (date.isNotEmpty()) {
                        val currentState = _uiState.value
                        if (isSameSelection(currentState, date, projectName)) {
                            return@collect
                        }

                        loadProjectDetailsInternal(
                            baseState = getBaseState(date, projectName),
                            showLoading = false
                        )
                    }
                }
        }
    }

    private fun isSameSelection(state: ProjectDetailsUiState, date: String, projectName: String): Boolean {
        return state is ProjectDetailsUiState.Success &&
            state.details.date == date &&
            state.details.projectName == projectName &&
            !state.details.isNewDayForProject()
    }

    private fun getBaseState(date: String, projectName: String): ProjectDetailsUiState.Success {
        val currentSuccess = uiState.value as? ProjectDetailsUiState.Success
        return ProjectDetailsUiState.Success(
            details = (currentSuccess?.details ?: ProjectDetailsState()).copy(
                date = date,
                projectName = projectName
            )
        )
    }

    private data class ProjectDetailsSelection(
        val date: String,
        val projectName: String
    )

    val setStartTime: (String) -> Unit = { startTime ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldStart = WorkTimeCalculator.stringToLocalTime(successState.details.startTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime),
                    dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        successState.settings.dailyWorkTimeEstimate
                    ),
                    dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        successState.details.lunchTimeEstimate
                    ),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                    oldStartTime = oldStart,
                    isNewDay = successState.details.isNewDayForProject()
                )
            )
            val nextState = successState.copy(
                details = successState.details.copy(
                    startTime = startTime
                )
            )
            applyUpdateToState(nextState, update)
        }
    }

    val currentStartTime: () -> Unit = {
        setStartTime(LocalTime.now().format(timeFormatter))
    }

    val setEndTime: (String) -> Unit = { endTime ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldEnd = WorkTimeCalculator.stringToLocalTime(successState.details.endTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(successState.details.startTime),
                    end = WorkTimeCalculator.stringToLocalTime(endTime),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(successState.details.breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd),
                    projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                    oldEndTime = oldEnd
                )
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(endTime = endTime)), update)
        }
    }

    val currentEndTime: () -> Unit = {
        setEndTime(LocalTime.now().format(timeFormatter))
    }

    val setLunchStart: (String) -> Unit = { lunchStart0 ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart0),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    successState.details.lunchTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldLunchStart = oldLunchStart,
                currentLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd)
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(lunchStart = lunchStart0)), update)
        }
    }

    val currentLunchStart: () -> Unit = {
        setLunchStart(LocalTime.now().format(timeFormatter))
    }

    val setLunchEnd: (String) -> Unit = { lunchEnd ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldLunchEnd = WorkTimeCalculator.stringToLocalTime(successState.details.lunchEnd)
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldLunchEnd = oldLunchEnd
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(lunchEnd = lunchEnd)), update)
        }
    }

    val currentLunchEnd: () -> Unit = {
        setLunchEnd(LocalTime.now().format(timeFormatter))
    }

    val setLunchTime: (String) -> Unit = { dailyLunchTimeEstimate ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldDailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                successState.details.lunchTimeEstimate
            )
            val update = ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(successState.details.lunchStart),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(dailyLunchTimeEstimate),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldDailyLunchTimeEstimate = oldDailyLunchTimeEstimate
            )
            applyUpdateToState(
                successState.copy(
                    details = successState.details.copy(
                        lunchTimeEstimate = dailyLunchTimeEstimate
                    )
                ),
                update
            )
        }
    }

    val currentLunchTime: () -> Unit = {
        setLunchTime(LocalTime.now().format(timeFormatter))
    }

    val setDailyWorkTime: (String) -> Unit = { dailyWorkTimeEstimate ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val updatedState = successState.copy(
                settings = successState.settings.copy(dailyWorkTimeEstimate = dailyWorkTimeEstimate)
            )
            // flexTimeByDate is now calculated on-the-fly in the UI, not stored in state
            updatedState
        }
    }

    val setBreakStart: (String) -> Unit = { breakStart0 ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldBreakStart = WorkTimeCalculator.stringToLocalTime(successState.details.breakStart)
            val update = ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(breakStart0),
                breakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                oldBreakStart = oldBreakStart
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(breakStart = breakStart0)), update)
        }
    }

    val currentBreakStart: () -> Unit = {
        setBreakStart(LocalTime.now().format(timeFormatter))
    }

    val setBreakEnd: (String) -> Unit = { breakEnd0 ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val oldBreakEnd = WorkTimeCalculator.stringToLocalTime(successState.details.breakEnd)
            val update = ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd0),
                oldBreakEnd = oldBreakEnd
            )
            applyUpdateToState(successState.copy(details = successState.details.copy(breakEnd = breakEnd0)), update)
        }
    }

    val currentBreakEnd: () -> Unit = {
        setBreakEnd(LocalTime.now().format(timeFormatter))
    }

    private fun applyUpdateToState(
        state: ProjectDetailsUiState.Success,
        result: WorkTimeCalculator.TimeUpdateResult
    ): ProjectDetailsUiState.Success {
        var nextState = state.copy(
            details = state.details.copy(
                endTime = result.end ?: state.details.endTime,
                lunchStart = result.lunchStart ?: state.details.lunchStart,
                lunchEnd = result.lunchEnd ?: state.details.lunchEnd,
                breakStart = result.breakStart ?: state.details.breakStart,
                breakEnd = result.breakEnd ?: state.details.breakEnd,
            )
        )

        result.projectTime?.let {
            nextState = nextState.copy(details = nextState.details.copy(projectTime = it))
        }
        return nextState
    }

    val getProjectDetailsState: () -> ProjectDetailsState = {
        (uiState.value as ProjectDetailsUiState.Success).details
    }

    val getSettingsEstimatesState: () -> SettingsState = {
        (uiState.value as ProjectDetailsUiState.Success).settings
    }

    fun loadProjectDetails(projectDetailsArg: ProjectDetailsState? = null, settingsArg: SettingsState? = null) {
        _isInitialLoadComplete.value = false
        val currentState = _uiState.value
        val showLoading = currentState !is ProjectDetailsUiState.Success && projectDetailsArg == null
        val baseState = (currentState as? ProjectDetailsUiState.Success)
            ?: ProjectDetailsUiState.Success(details = ProjectDetailsState())
        viewModelScope.launch {
            loadProjectDetailsInternal(
                baseState = baseState,
                projectDetailsArg = projectDetailsArg,
                settingsArg = settingsArg,
                showLoading = showLoading
            )
        }
    }

    private suspend fun loadProjectDetailsInternal(
        baseState: ProjectDetailsUiState.Success,
        projectDetailsArg: ProjectDetailsState? = null,
        settingsArg: SettingsState? = null,
        showLoading: Boolean
    ) {
        if (showLoading && _uiState.value !is ProjectDetailsUiState.Success) {
            _uiState.value = ProjectDetailsUiState.Loading
        }

        try {
            val selection = resolveSelection(baseState, projectDetailsArg)
            if (selection.date.isEmpty()) return

            val loadedProjectDetails = projectDetailsRepository.getProjectDetails(
                selection.date,
                selection.projectName
            ) ?: projectDetailsArg
            val settings = resolveSettings(settingsArg, loadedProjectDetails, selection.date)
            val normalizedProjectDetails = normalizeProjectDetails(loadedProjectDetails, settings)

            _uiState.value = createNextState(
                baseState = baseState,
                selection = selection,
                projectDetails = normalizedProjectDetails,
                settings = settings
            )
            syncSelectedValuesFromDetails(normalizedProjectDetails)
        } catch (e: IllegalArgumentException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid argument")
        } catch (e: IllegalStateException) {
            _uiState.value = ProjectDetailsUiState.Error(e.message ?: "Invalid state")
        } finally {
            _isInitialLoadComplete.value = true
        }
    }

    private fun resolveSelection(
        baseState: ProjectDetailsUiState.Success,
        projectDetailsArg: ProjectDetailsState?
    ): ProjectDetailsSelection {
        val date = baseState.details.date.ifEmpty { projectDetailsArg?.date ?: selectedDate.value }
        val projectName = baseState.details.projectName.ifEmpty {
            projectDetailsArg?.projectName ?: selectedProjectName.value
        }
        return ProjectDetailsSelection(date = date, projectName = projectName)
    }

    private suspend fun resolveSettings(
        settingsArg: SettingsState?,
        projectDetails: ProjectDetailsState?,
        date: String
    ): SettingsState? {
        return when {
            settingsArg != null -> settingsArg
            projectDetails == null -> settingsRepository.getEffectiveSettingsForDate(date)
            else -> settingsRepository.getSettings()
        }
    }

    private fun normalizeProjectDetails(
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsState? {
        if (projectDetails == null || !projectDetails.hasOnlyProjectTime()) {
            return projectDetails
        }

        val update = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
            projectTime = WorkTimeCalculator.stringToLocalTime(projectDetails.projectTime),
            dailyLunchTimeEstimate = WorkTimeCalculator
                .stringToLocalTime(settings?.dailyLunchTimeEstimate ?: ZERO_TIME)
        )
        return projectDetails.copy(
            startTime = ZERO_TIME,
            endTime = update.end?.format(timeFormatter) ?: ZERO_TIME,
            lunchStart = update.lunchStart?.format(timeFormatter) ?: ZERO_TIME,
            lunchEnd = update.lunchEnd?.format(timeFormatter) ?: ZERO_TIME,
            breakStart = update.breakStart?.format(timeFormatter) ?: ZERO_TIME,
            breakEnd = update.breakEnd?.format(timeFormatter) ?: ZERO_TIME
        )
    }

    private fun createNextState(
        baseState: ProjectDetailsUiState.Success,
        selection: ProjectDetailsSelection,
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsUiState.Success {
        return ProjectDetailsUiMapper.applyEntitiesToState(
            baseState.copy(
                details = baseState.details.copy(
                    date = selection.date,
                    projectName = selection.projectName,
                )
            ),
            projectDetails,
            settings
        )
    }

    private fun syncSelectedValuesFromDetails(projectDetails: ProjectDetailsState?) {
        projectDetails?.date
            ?.takeIf { it.isNotBlank() }
            ?.let { setDate(date = it) }
        projectDetails?.projectName
            ?.takeIf { it.isNotBlank() }
            ?.let { setProjectName(projectName = it) }
    }

    val clearDetails: () -> Unit = {
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success

            successState.copy(
                details = successState.details.copy(
                    startTime = ZERO_TIME,
                    endTime = ZERO_TIME,
                    lunchStart = ZERO_TIME,
                    lunchEnd = ZERO_TIME,
                    breakStart = ZERO_TIME,
                    breakEnd = ZERO_TIME,
                    projectTime = ZERO_TIME,
                    lunchTimeEstimate = successState.settings.dailyLunchTimeEstimate
                )
            )
        }
    }

    val setProjectTime: (String) -> Unit = { projectTime ->
        _uiState.update { currentState ->
            val successState = currentState as ProjectDetailsUiState.Success
            val nextDetails = successState.details.copy(projectTime = projectTime)

            if (nextDetails.hasOnlyProjectTime()) {
                return@update successState.copy(
                    details = normalizeProjectDetails(nextDetails, successState.settings) ?: nextDetails
                )
            }

            val oldProjectTime = WorkTimeCalculator.stringToLocalTime(successState.details.projectTime)
            val update = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(successState.details.endTime),
                dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    successState.settings.dailyWorkTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldProjectTime = oldProjectTime
            )
            applyUpdateToState(
                successState.copy(
                    details = nextDetails
                ),
                update
            )
        }
    }

    val currentProjectTime: () -> Unit = {
        setProjectTime(LocalTime.now().format(timeFormatter))
    }
}
