package com.akiwiksten.worktime30.feature.projects

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
            var projects: List<Project> = emptyList()
            async {
                projects =
                    AppDatabase.getInstance(ctx.value!!).projectDao().loadProjectsByDate(date)
            }.await()
            if(projects.isEmpty()) {
                var projectNames: List<ProjectName> = emptyList()
                async {
                    projectNames =
                        AppDatabase.getInstance(ctx.value!!).projectNameDao().loadProjectNames()
                }.await()
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
            for (project in items) {
                val project0 = Project(
                    date = _date.value!!,
                    projectName = project.projectName,
                    projectStartTime = project.projectStartTime,
                    projectEndTime = project.projectEndTime,
                    projectTime = project.projectTime,
                    kilometres = project.kilometres,
                    allowance = project.allowance,
                    workType = project.workType)
                async {
                    AppDatabase.getInstance(ctx.value!!).projectDao()
                        .insertProject(project0)
                    val project1 = ProjectName(project.projectName)
                    AppDatabase.getInstance(ctx.value!!).projectNameDao()
                        .insertProjectName(project1)
                }.await()
            }
            for (deleted in deletedProjects) {
                async {
                    AppDatabase.getInstance(ctx.value!!).projectDao()
                        .delete(Project(_date.value!!, deleted, ZERO_TIME))
                    val project0 = ProjectName(deleted)
                    AppDatabase.getInstance(ctx.value!!).projectNameDao()
                        .delete(project0)
                }.await()
            }
        }
    }

    fun addItem(uiState: ProjectListItemUiState): Boolean {
        if(items.find { i -> i.projectName == uiState.projectName } != null) {
            return false
        }
        //_workTimeBalance.value = TimeGeneratorModel.calculateWorkTimeBalance(
        //    _workTimeBalance.value!!, uiState.projectTime)

        //Log.d("ProjectsViewModel", "addItem ${_workTimeBalance.value}")
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

    fun editItem(uiState: ProjectListItemUiState
    ) {
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
        val item = items.find { i -> i.index == index }
        deletedProjects.add(item!!.projectName)
        items.remove(selectedItem(index))
    }

    fun selectedItem(index: Int): ProjectListItemUiState {
        return items.find { i -> i.index == index }!!
    }

    fun leftOvers(projectTime: String): String {
        var leftOvers: String = ZERO_TIME
        if(_workTimeBalance.value!!.substring(0, 1) == "-") {
            val balanceLocal = TimeGeneratorModel.stringToLocalTime(
                _workTimeBalance.value!!.substring(1))
            val pTimeLocal = TimeGeneratorModel.stringToLocalTime(projectTime)
            leftOvers = balanceLocal
                .plusHours(pTimeLocal.hour.toLong())
                .plusMinutes(pTimeLocal.minute.toLong()).toString()
        }
        return leftOvers
    }

    fun loadWorkTypes() {
        viewModelScope.launch {
            async {
                _dropDownWorkTypes.value.clear()
                val workTypes = AppDatabase.getInstance(_ctx.value!!).workTypeDao().loadWorkTypes()
                for (workType in workTypes) {
                    _dropDownWorkTypes.value.add(workType.workType)
                }
            }.await()
        }
    }

    fun loadWorkTimeTodayFromDb(date: String) {
        viewModelScope.launch {
            var workDay: WorkDay? = null
            async {
                workDay = AppDatabase.getInstance(_ctx.value!!).workDayDao().loadWorkDay(date)
            }.await()
            if(workDay != null) {
                _workTimeToday.value = workDay!!.workTimeToday
            } else {
                _workTimeToday.value = ZERO_TIME
            }
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

