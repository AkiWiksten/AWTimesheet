package com.akiwiksten.worktime30.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.Project
import com.akiwiksten.worktime30.data.database.Settings
import com.akiwiksten.worktime30.data.database.WorkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject


@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    private val _ctx = MutableStateFlow<Context?>(null)
    val ctx = _ctx.asStateFlow()
    private var _name = MutableStateFlow("")
    val name = _name.asStateFlow()
    private var _employer = MutableStateFlow("")
    val employer = _employer.asStateFlow()
    private var _endMonthDate = MutableStateFlow("")
    val endMonthDate = _endMonthDate.asStateFlow()
    private var _dropDownWorkTypes = MutableStateFlow<MutableList<String>>(mutableListOf())
    val dropDownWorkTypes = _dropDownWorkTypes.asStateFlow()
    var projectsByMonth = mutableListOf<Project>()

    fun setWorkType(workType: String) {
        if(_dropDownWorkTypes.value.isEmpty()) {
            _dropDownWorkTypes.value.add(workType)
        }
    }

    fun setEndMonthDate(selectedDate: String) {
        val initial = LocalDate.parse(selectedDate)
        _endMonthDate.value = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
    }

    fun setCtx(ctx: Context) {
        _ctx.value = ctx
    }

    fun setName(name0: String) {
        _name.value = name0
    }

    fun setEmployer(employer0: String) {
        _employer.value = employer0
    }

    fun loadProjectsByMonth(date: String) {
        viewModelScope.launch {
            val initial = LocalDate.parse(date)
            val startMonth = initial.withDayOfMonth(1).toString()
            _endMonthDate.value =
                initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
            projectsByMonth = AppDatabase.getInstance(ctx.value!!).projectDao()
                .getProjectsByDateRange(startMonth, _endMonthDate.value)
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            _dropDownWorkTypes.value.clear()
            val workTypes = AppDatabase.getInstance(_ctx.value!!).workTypeDao().loadWorkTypes()
            for (workType in workTypes) {
                _dropDownWorkTypes.value.add(workType.workType)
            }
            val report = AppDatabase.getInstance(_ctx.value!!).settingsDao().loadSettings()
            _name.value = report?.name ?: ""
            _employer.value = report?.employer ?: ""
            _dropDownWorkTypes.value.sort()
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            AppDatabase.getInstance(_ctx.value!!).workTypeDao().deleteAll()
            for (workType in _dropDownWorkTypes.value) {
                val workTypeDb = WorkType(workType = workType)
                AppDatabase.getInstance(_ctx.value!!).workTypeDao().insertWorkType(workTypeDb)
            }
            val settings = Settings(name = _name.value, employer = _employer.value)
            AppDatabase.getInstance(_ctx.value!!).settingsDao().insertSettings(settings)
        }
    }

    fun deleteWorkType(workType: String) {
        viewModelScope.launch {
            val workTypeDb = WorkType(workType = workType)
            AppDatabase.getInstance(_ctx.value!!).workTypeDao().delete(workTypeDb)
        }
    }
}
