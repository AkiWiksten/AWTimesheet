package com.akiwiksten.worktime30.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.GetProjectsByMonthUseCase
import com.akiwiksten.worktime30.domain.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.SaveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val saveSettingsUseCase: SaveSettingsUseCase,
    private val getProjectsByMonthUseCase: GetProjectsByMonthUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private var _name = MutableStateFlow("")
    val name = _name.asStateFlow()
    private var _employer = MutableStateFlow("")
    val employer = _employer.asStateFlow()
    private var _endMonthDate = MutableStateFlow("")
    val endMonthDate = _endMonthDate.asStateFlow()
    private var _dropDownWorkTypes = MutableStateFlow<MutableList<String>>(mutableListOf())
    val dropDownWorkTypes = _dropDownWorkTypes.asStateFlow()
    var projectsByMonth: List<ProjectEntity> = listOf()

    fun setWorkType(workType: String) {
        if (_dropDownWorkTypes.value.isEmpty()) {
            _dropDownWorkTypes.value.add(workType)
        }
    }

    fun setEndMonthDate(selectedDate: String) {
        val initial = LocalDate.parse(selectedDate)
        _endMonthDate.value = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
    }

    fun setName(name0: String) {
        _name.value = name0
    }

    fun setEmployer(employer0: String) {
        _employer.value = employer0
    }

    fun loadProjectsByMonth(date: String) {
        viewModelScope.launch {
            val parsedDate = LocalDate.parse(date)
            _endMonthDate.value = parsedDate
                .withDayOfMonth(parsedDate.month.length(parsedDate.isLeapYear))
                .toString()
            projectsByMonth = getProjectsByMonthUseCase(date)
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            val data = getSettingsUseCase()
            _name.value = data.name
            _employer.value = data.employer
            _dropDownWorkTypes.value = data.workTypes.toMutableList()
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            saveSettingsUseCase(
                name = _name.value,
                employer = _employer.value,
                workTypes = _dropDownWorkTypes.value
            )
        }
    }

    fun deleteWorkType(workType: String) {
        viewModelScope.launch {
            settingsRepository.deleteWorkType(WorkTypeEntity(workType = workType))
        }
    }
}
