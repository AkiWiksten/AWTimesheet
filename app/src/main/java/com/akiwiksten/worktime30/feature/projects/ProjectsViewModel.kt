package com.akiwiksten.worktime30.feature.projects

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Projects screen, managing project logs and work types.
 */
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjectsScreenDataUseCase: GetProjectsScreenDataUseCase,
    private val saveProjectsUseCase: SaveProjectsUseCase
) : ViewModel() {
    
    private val _items = MutableStateFlow<List<ProjectListItemUiState>>(emptyList())
    val items: StateFlow<List<ProjectListItemUiState>> = _items.asStateFlow()

    var index0 = 0
    
    private val _workTimeBalance = MutableStateFlow<String?>(ZERO_TIME)
    val workTimeBalance = _workTimeBalance.asStateFlow()
    
    private val _workTimeTodayDb = MutableStateFlow(ZERO_TIME)
    
    private val _selectedIndex = MutableStateFlow(-1)
    val selectedIndex = _selectedIndex.asStateFlow()
    
    private val deletedProjects = mutableListOf<String>()
    
    private var _dropDownWorkTypes = MutableStateFlow<List<String>>(listOf())
    val dropDownWorkTypes = _dropDownWorkTypes.asStateFlow()
    
    private val _date = MutableStateFlow(ZERO_TIME)
    val date: StateFlow<String> = _date.asStateFlow()

    val totalWorkTime: StateFlow<String> = _items.map { list ->
        list.fold(ZERO_TIME) { acc, item ->
            val duration = WorkTimeCalculator.calculateWorkTimeBalance(
                item.projectEndTime, "-" + item.projectStartTime
            )
            WorkTimeCalculator.calculateWorkTimeBalance(acc, duration)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZERO_TIME)

    fun setDate(date: String) {
        _date.value = date
    }

    fun setSelectedIndex(index: Int) {
        _selectedIndex.value = index
    }

    fun saveProjects() {
        viewModelScope.launch {
            val dateVal = _date.value

            val projectsToSave = _items.value.map { project ->
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
        if (_items.value.any { it.projectName == uiState.projectName }) {
            return false
        }
        val newItem = uiState.copy(
            index = index0++,
            projectName = uiState.projectName.trim()
        )
        _items.update { (it + newItem).sortedByDescending { item -> item.projectStartTime } }
        return true
    }

    fun editItem(uiState: ProjectListItemUiState) {
        var balance = uiState.initBalance
        _items.value.forEach { item ->
            val timeToUse = if (item.index == uiState.index) uiState.projectTime else item.projectTime
            balance = WorkTimeCalculator.calculateWorkTimeBalance(balance, timeToUse)
        }
        _workTimeBalance.value = balance
        
        _items.update { list ->
            list.map { item ->
                if (item.index == uiState.index) {
                    item.copy(
                        projectName = uiState.projectName,
                        projectStartTime = uiState.projectStartTime,
                        projectEndTime = uiState.projectEndTime,
                        kilometres = uiState.kilometres,
                        allowance = uiState.allowance,
                        workType = uiState.workType
                    )
                } else {
                    item
                }
            }.sortedByDescending { it.projectStartTime }
        }
        _selectedIndex.value = -1
    }

    fun deleteItem(index: Int) {
        val item = _items.value.find { it.index == index } ?: return
        deletedProjects.add(item.projectName)
        _items.update { it.filter { it.index != index } }
        _selectedIndex.value = -1
    }

    fun selectedItem(index: Int): ProjectListItemUiState {
        return _items.value.find { it.index == index } ?: ProjectListItemUiState()
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

            _workTimeTodayDb.value = data.workTimeToday
            _workTimeBalance.value = "-${data.workTimeToday}"
            _dropDownWorkTypes.value = data.workTypes

            val loadedItems = if (data.projects.isEmpty()) {
                data.projectNames.map { project ->
                    ProjectListItemUiState(index = index0++, projectName = project.name)
                }
            } else {
                data.projects.map { project ->
                    ProjectListItemUiState(
                        index = index0++,
                        projectName = project.projectName,
                        projectStartTime = project.projectStartTime,
                        projectEndTime = project.projectEndTime,
                        projectTime = project.projectTime,
                        kilometres = project.kilometres,
                        allowance = project.allowance,
                        workType = project.workType
                    )
                }
            }
            _items.value = loadedItems.sortedByDescending { it.projectStartTime }
        }
    }

    fun areItemsOverlapping(newStartTime: String, newEndTime: String): Boolean {
        return _items.value.any { item ->
            (newStartTime > item.projectStartTime && newStartTime < item.projectEndTime) ||
            (newEndTime > item.projectStartTime && newEndTime < item.projectEndTime)
        }
    }
}

data class ProjectListItemUiState(
    val index: Int = 0,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val projectStartTime: String = ZERO_TIME,
    val projectEndTime: String = ZERO_TIME,
    val kilometres: Int = 0,
    val allowance: String = "",
    val workType: String = "",
    val titleId: Int = -1,
    val leftOvers: String = "",
    val initBalance: String = "",
    val id: Int = 0
)
