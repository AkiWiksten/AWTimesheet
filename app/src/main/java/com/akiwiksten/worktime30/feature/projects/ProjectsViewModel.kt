package com.akiwiksten.worktime30.feature.projects

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Projects screen, managing project logs and work types.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsScreenDataUseCase: GetProjectsScreenDataUseCase,
    private val saveProjectsUseCase: SaveProjectsUseCase
) : ViewModel() {
    var items = mutableStateListOf<ProjectListItemUiState>()
    var index0 = 0
    
    private val _workTimeBalance = MutableStateFlow<String?>(ZERO_TIME)
    val workTimeBalance = _workTimeBalance.asStateFlow()
    
    private val _workTimeToday = MutableStateFlow<String?>(ZERO_TIME)
    
    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex = _selectedIndex.asStateFlow()
    
    private val deletedProjects = mutableListOf<String>()
    
    private var _dropDownWorkTypes = MutableStateFlow<List<String>>(listOf())
    val dropDownWorkTypes = _dropDownWorkTypes.asStateFlow()
    
    private val _projectListItemUiState = MutableStateFlow(ProjectListItemUiState())
    val projectListItemUiState: StateFlow<ProjectListItemUiState> = _projectListItemUiState.asStateFlow()
    
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

    fun saveProjects() {
        viewModelScope.launch {
            val dateVal = _date.value ?: return@launch

            val projectsToSave = items.map { project ->
                ProjectEntity(
                    date = dateVal,
                    projectName = project.projectName,
                    projectStartTime = project.projectStartTime,
                    projectEndTime = project.projectEndTime,
                    projectTime = project.projectTime,
                    kilometres = project.kilometres,
                    allowance = project.allowance,
                    workType = project.workType
                )
            }

            saveProjectsUseCase(
                date = dateVal,
                projectsToSave = projectsToSave,
                projectNamesToDelete = deletedProjects
            )
            deletedProjects.clear()
        }
    }

    fun addItem(uiState: ProjectListItemUiState): Boolean {
        if (items.any { it.projectName == uiState.projectName }) {
            return false
        }
        val newItem = ProjectListItemUiState(
            index = index0++,
            projectName = uiState.projectName.trim(),
            projectStartTime = uiState.projectStartTime,
            projectEndTime = uiState.projectEndTime,
            kilometres = uiState.kilometres,
            allowance = uiState.allowance,
            workType = uiState.workType
        )
        items.add(newItem)
        items.sortByDescending { it.projectStartTime }
        return true
    }

    fun editItem(uiState: ProjectListItemUiState) {
        var balance = uiState.initBalance
        items.forEach { item ->
            val timeToUse = if (item.index == uiState.index) uiState.projectTime else item.projectTime
            balance = WorkTimeCalculator.calculateWorkTimeBalance(balance, timeToUse)
        }
        _workTimeBalance.value = balance
        
        items.find { it.index == uiState.index }?.apply {
            projectName = uiState.projectName
            projectStartTime = uiState.projectStartTime
            projectEndTime = uiState.projectEndTime
            kilometres = uiState.kilometres
            allowance = uiState.allowance
            workType = uiState.workType
        }
        items.sortByDescending { it.projectStartTime }
        _selectedIndex.value = -1
    }

    fun deleteItem(index: Int) {
        val item = items.find { it.index == index } ?: return
        deletedProjects.add(item.projectName)
        items.remove(item)
    }

    fun selectedItem(index: Int): ProjectListItemUiState {
        return items.find { it.index == index } ?: ProjectListItemUiState()
    }

    fun leftOvers(projectTime: String): String {
        val balance = _workTimeBalance.value ?: return ZERO_TIME
        if (balance.startsWith("-")) {
            val balanceLocal = WorkTimeCalculator.stringToLocalTime(balance.substring(1))
            val pTimeLocal = WorkTimeCalculator.stringToLocalTime(projectTime)
            return balanceLocal.plusHours(pTimeLocal.hour.toLong())
                .plusMinutes(pTimeLocal.minute.toLong()).toString()
        }
        return ZERO_TIME
    }

    fun loadWorkTimeTodayFromDb(date: String) {
        viewModelScope.launch {
            val data = getProjectsScreenDataUseCase(date)

            _workTimeToday.value = data.workTimeToday
            _workTimeBalance.value = "-${data.workTimeToday}"
            _dropDownWorkTypes.value = data.workTypes

            items.clear()
            if (data.projects.isEmpty()) {
                data.projectNames.forEach { project ->
                    addItem(ProjectListItemUiState(projectName = project.name))
                }
            } else {
                data.projects.forEach { project ->
                    addItem(ProjectListItemUiState(
                        projectName = project.projectName,
                        projectStartTime = project.projectStartTime,
                        projectEndTime = project.projectEndTime,
                        projectTime = project.projectTime,
                        kilometres = project.kilometres,
                        allowance = project.allowance,
                        workType = project.workType
                    ))
                }
            }
            items.sortByDescending { it.projectTime }
            Log.d("ProjectsViewModel", "loadWorkTimeTodayFromDb ${_workTimeBalance.value}")
        }
    }

    fun getWorkTimeToday(): String {
        return items.fold(ZERO_TIME) { acc, item ->
            val duration = WorkTimeCalculator.calculateWorkTimeBalance(
                item.projectEndTime, "-" + item.projectStartTime
            )
            WorkTimeCalculator.calculateWorkTimeBalance(acc, duration)
        }
    }

    @Suppress("ComplexCondition")
    fun areItemsOverlapping(newStartTime: String, newEndTime: String): Boolean {
        return items.any { item ->
            (newStartTime > item.projectStartTime && newStartTime < item.projectEndTime) ||
            (newEndTime > item.projectStartTime && newEndTime < item.projectEndTime)
        }
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
