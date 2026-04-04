package com.akiwiksten.worktime30.feature.projects

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val workDayRepository: WorkDayRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    var items = mutableStateListOf<ProjectListItemUiState>()
    var index0 = 0
    private val _workTimeBalance = MutableStateFlow<String?>(ZERO_TIME)
    val workTimeBalance = _workTimeBalance.asStateFlow()
    private val _workTimeToday = MutableStateFlow<String?>(ZERO_TIME)
    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex = _selectedIndex.asStateFlow()
    val deletedProjects: MutableList<String> = mutableListOf()
    private var _dropDownWorkTypes = MutableStateFlow<List<String>>(listOf())
    val dropDownWorkTypes = _dropDownWorkTypes.asStateFlow()
    private val _projectListItemUiState = MutableStateFlow(ProjectListItemUiState())
    val projectListItemUiState: StateFlow<ProjectListItemUiState>
        get() = _projectListItemUiState.asStateFlow()
    private val _date = MutableStateFlow<String?>(ZERO_TIME)

    fun setDate(date: String) {
        _date.value = date
    }

    fun setSelectedIndex(index: Int) {
        _selectedIndex.value = index
    }

    fun setNewState(uiState: ProjectListItemUiState) {
        _projectListItemUiState.update {
            it.copy(
                index = uiState.index,
                projectName = uiState.projectName,
                projectTime = uiState.projectTime,
                kilometres = uiState.kilometres,
                allowance = uiState.allowance,
                workType = uiState.workType,
                titleId = uiState.titleId,
                leftOvers = uiState.leftOvers,
                initBalance = uiState.initBalance,
                id = uiState.id
            )
        }
    }

    private fun loadProjectData(date: String) {
        viewModelScope.launch {
            items.clear()
            val projects = projectRepository.getProjectsByDateRange(date, date)

            if (projects.isEmpty()) {
                val projectNames = projectRepository.getProjectNames()
                for (project in projectNames) {
                    _projectListItemUiState.value = ProjectListItemUiState(projectName = project.name)
                    addItem(_projectListItemUiState.value)
                }
            } else {
                for (project in projects) {
                    _projectListItemUiState.value = ProjectListItemUiState(
                        projectName = project.projectName,
                        projectStartTime = project.projectStartTime,
                        projectEndTime = project.projectEndTime,
                        projectTime = project.projectTime,
                        kilometres = project.kilometres,
                        allowance = project.allowance,
                        workType = project.workType
                    )
                    addItem(_projectListItemUiState.value)
                }
            }
            items.sortByDescending { it.projectTime }
        }
    }

    fun saveProjects() {
        viewModelScope.launch {
            val dateVal = _date.value ?: return@launch

            for (project in items) {
                val project0 = ProjectEntity(
                    date = dateVal,
                    projectName = project.projectName,
                    projectStartTime = project.projectStartTime,
                    projectEndTime = project.projectEndTime,
                    projectTime = project.projectTime,
                    kilometres = project.kilometres,
                    allowance = project.allowance,
                    workType = project.workType
                )

                projectRepository.insertProject(project0)
                projectRepository.insertProjectName(ProjectNameEntity(project.projectName))
            }
            for (deleted in deletedProjects) {
                projectRepository.deleteProject(ProjectEntity(dateVal, deleted, ZERO_TIME))
                projectRepository.deleteProjectName(ProjectNameEntity(deleted))
            }
        }
    }

    fun addItem(uiState: ProjectListItemUiState): Boolean {
        if (items.any { it.projectName == uiState.projectName }) {
            return false
        }
        _projectListItemUiState.value = ProjectListItemUiState(
            index = index0,
            projectName = uiState.projectName.trim(),
            projectStartTime = uiState.projectStartTime,
            projectEndTime = uiState.projectEndTime,
            kilometres = uiState.kilometres,
            allowance = uiState.allowance,
            workType = uiState.workType
        )
        items.add(_projectListItemUiState.value)
        index0++
        items.sortByDescending { it.projectStartTime }
        return true
    }

    fun editItem(uiState: ProjectListItemUiState) {
        var balance = uiState.initBalance
        for (item in items) {
            balance = if (item.projectName == items.find { i -> i.index == uiState.index }?.projectName) {
                WorkTimeCalculator.calculateWorkTimeBalance(
                    balance, uiState.projectTime
                )
            } else {
                WorkTimeCalculator.calculateWorkTimeBalance(
                    balance, item.projectTime
                )
            }
        }
        _workTimeBalance.value = balance
        val item = items.find { i -> i.index == uiState.index }
        item?.projectName = uiState.projectName
        item?.projectStartTime = uiState.projectStartTime
        item?.projectEndTime = uiState.projectEndTime
        item?.kilometres = uiState.kilometres
        item?.allowance = uiState.allowance
        item?.workType = uiState.workType
        items.sortByDescending { it.projectStartTime }
        _selectedIndex.value = -1
    }

    fun deleteItem(index: Int) {
        val item = items.find { i -> i.index == index } ?: return
        deletedProjects.add(item.projectName)
        items.remove(item)
    }

    fun selectedItem(index: Int): ProjectListItemUiState {
        return items.find { i -> i.index == index } ?: ProjectListItemUiState()
    }

    fun leftOvers(projectTime: String): String {
        var leftOvers: String = ZERO_TIME
        val balance = _workTimeBalance.value ?: return leftOvers
        if (balance.isNotEmpty() && balance.startsWith("-")) {
            val balanceLocal = WorkTimeCalculator.stringToLocalTime(
                balance.substring(1)
            )
            val pTimeLocal = WorkTimeCalculator.stringToLocalTime(projectTime)
            leftOvers = balanceLocal
                .plusHours(pTimeLocal.hour.toLong())
                .plusMinutes(pTimeLocal.minute.toLong()).toString()
        }
        return leftOvers
    }

    fun loadWorkTypes() {
        viewModelScope.launch {
            val workTypes = settingsRepository.getWorkTypes()
            _dropDownWorkTypes.value = workTypes.map { it.workType }
        }
    }

    fun loadWorkTimeTodayFromDb(date: String) {
        viewModelScope.launch {
            val workDay = workDayRepository.getWorkDay(date)

            _workTimeToday.value = workDay?.workTimeToday ?: ZERO_TIME
            _workTimeBalance.value = "-${_workTimeToday.value}"

            Log.d("ProjectsViewModel", "loadWorkTimeTodayFromDb ${_workTimeBalance.value}")
            loadProjectData(date = date)
        }
    }

    fun getWorkTimeToday(): String {
        var workTimeToday = ZERO_TIME
        for (item in items) {
            val workTimeProject = WorkTimeCalculator.calculateWorkTimeBalance(
                item.projectEndTime,
                "-" + item.projectStartTime
            )
            workTimeToday = WorkTimeCalculator.calculateWorkTimeBalance(
                workTimeToday, workTimeProject
            )
        }
        return workTimeToday
    }

    @Suppress("ComplexCondition")
    fun areItemsOverlapping(newStartTime: String, newEndTime: String): Boolean {
        for (item in items) {
            if ((newStartTime > item.projectStartTime && newStartTime < item.projectEndTime) ||
                (newEndTime > item.projectStartTime && newEndTime < item.projectEndTime)
            ) {
                return true
            }
        }
        return false
    }
}

data class ProjectListItemUiState(
    var index: Int = 0,
    var projectName: String = "",
    var projectTime: String = ZERO_TIME,
    var projectStartTime: String = ZERO_TIME,
    var projectEndTime: String = ZERO_TIME,
    var kilometres: Int = 0,
    var allowance: String = "",
    var workType: String = "",
    var titleId: Int = -1,
    var leftOvers: String = "",
    var initBalance: String = "",
    var id: Int = 0
)
