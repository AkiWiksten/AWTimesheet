package com.akiwiksten.awtimesheet.feature.absence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.usecase.SaveAbsenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AbsenceViewModel @Inject constructor(
    private val getCalendarDataUseCase: SaveAbsenceUseCase,
    private val absenceRepository: AbsenceRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AbsenceUiState())
    val uiState: StateFlow<AbsenceUiState> = _uiState.asStateFlow()

    init {
        initData()
    }

    fun addAbsence(absenceType: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            getCalendarDataUseCase(absenceType, startDate, endDate)
        }
        val newAbsence = SavedAbsence(
            absenceType = absenceType,
            startDate = startDate,
            endDate = endDate
        )
        _uiState.value = _uiState.value.copy(
            savedAbsences = _uiState.value.savedAbsences + newAbsence
        )
    }

    fun initData() {
        viewModelScope.launch {
            val absences = absenceRepository.getAll().map {
                SavedAbsence(
                    absenceType = it.absenceType,
                    startDate = it.startDate,
                    endDate = it.endDate
                )
            }
            _uiState.value = _uiState.value.copy(
                savedAbsences = absences
            )
        }
    }
}

data class AbsenceUiState(
    val savedAbsences: List<SavedAbsence> = emptyList()
)

data class SavedAbsence(
    val absenceType: String,
    val startDate: String,
    val endDate: String,
)
