package com.akiwiksten.worktime30.feature.projects

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.TimeGeneratorModel
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.Project
import com.akiwiksten.worktime30.data.database.ProjectName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class ProjectsViewModel @Inject constructor() : ViewModel() {
    private val _ctx = MutableStateFlow<Context?>(null)
    val ctx = _ctx.asStateFlow()
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

    fun setCtx(ctx: Context) {
        _ctx.value = ctx
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
            val context = _ctx.value ?: return@launch
            val db = AppDatabase.getInstance(context)
            val projects = db.projectDao().loadProjectsByDate(date)

            if(projects.isEmpty()) {
                val projectNames = db.projectNameDao().loadProjectNames()
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
                        workType = project.workType)
                    addItem(_projectListItemUiState.value)
                }
            }
            items.sortByDescending { it.projectTime }
        }
    }

    fun saveProjects() {
        viewModelScope.launch {
            val context = _ctx.value ?: return@launch
            val dateVal = _date.value ?: return@launch
            val db = AppDatabase.getInstance(context)

            for (project in items) {
                val project0 = Project(
                    date = dateVal,
                    projectName = project.projectName,
                    projectStartTime = project.projectStartTime,
                    projectEndTime = project.projectEndTime,
                    projectTime = project.projectTime,
                    kilometres = project.kilometres,
                    allowance = project.allowance,
                    workType = project.workType)

                db.projectDao().insertProject(project0)
                db.projectNameDao().insertProjectName(ProjectName(project.projectName))
            }
            for (deleted in deletedProjects) {
                db.projectDao().delete(Project(dateVal, deleted, ZERO_TIME))
                db.projectNameDao().delete(ProjectName(deleted))
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
            workType = uiState.workType)
        items.add(_projectListItemUiState.value)
        index0++
        items.sortByDescending { it.projectStartTime }
        return true
    }

    fun editItem(uiState: ProjectListItemUiState) {
        var balance = uiState.initBalance
        for(item in items) {
            balance = if(item.projectName == items.find { i -> i.index == uiState.index }?.projectName) {
                TimeGeneratorModel.calculateWorkTimeBalance(
                    balance, uiState.projectTime
                )
            } else {
                TimeGeneratorModel.calculateWorkTimeBalance(
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
        if(balance.isNotEmpty() && balance.substring(0, 1) == "-") {
            val balanceLocal = TimeGeneratorModel.stringToLocalTime(
                balance.substring(1))
            val pTimeLocal = TimeGeneratorModel.stringToLocalTime(projectTime)
            leftOvers = balanceLocal
                .plusHours(pTimeLocal.hour.toLong())
                .plusMinutes(pTimeLocal.minute.toLong()).toString()
        }
        return leftOvers
    }

    fun loadWorkTypes() {
        viewModelScope.launch {
            val context = _ctx.value ?: return@launch
            val db = AppDatabase.getInstance(context)
            val workTypes = db.workTypeDao().loadWorkTypes()
            _dropDownWorkTypes.value = workTypes.map { it.workType }
        }
    }

    fun loadWorkTimeTodayFromDb(date: String) {
        viewModelScope.launch {
            val context = _ctx.value ?: return@launch
            val workDay = AppDatabase.getInstance(context).workDayDao().loadWorkDay(date)
            
            _workTimeToday.value = workDay?.workTimeToday ?: ZERO_TIME
            _workTimeBalance.value = "-${_workTimeToday.value}"
            
            Log.d("ProjectsViewModel", "loadWorkTimeTodayFromDb ${_workTimeBalance.value}")
            loadProjectData(date = date)
        }
    }


    fun getWorkTimeToday(): String {
        var workTimeToday = ZERO_TIME
        for(item in items) {
            val workTimeProject = TimeGeneratorModel.calculateWorkTimeBalance(
                item.projectEndTime, "-" + item.projectStartTime
            )
            workTimeToday = TimeGeneratorModel.calculateWorkTimeBalance(
                workTimeToday, workTimeProject
            )
        }
        return workTimeToday
    }

    @Suppress("ComplexCondition")
    fun areItemsOverlapping(newStartTime: String, newEndTime: String): Boolean {
        for(item in items) {
            if((newStartTime > item.projectStartTime && newStartTime < item.projectEndTime) ||
                (newEndTime > item.projectStartTime && newEndTime < item.projectEndTime)) {
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
