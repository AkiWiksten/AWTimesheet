package com.akiwiksten.awtimesheet.feature.absence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteAbsenceUseCase
import com.akiwiksten.awtimesheet.domain.usecase.SaveAbsenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AbsenceViewModel @Inject constructor(
    private val saveAbsenceUseCase: SaveAbsenceUseCase,
    private val absenceRepository: AbsenceRepository,
    private val deleteAbsenceUseCase: DeleteAbsenceUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AbsenceUiState())
    val uiState: StateFlow<AbsenceUiState> = _uiState.asStateFlow()

    init {
        initData()
    }

    fun addAbsence(absence: AbsenceState) {
        viewModelScope.launch {
            saveAbsenceUseCase(
                startDate = absence.startDate,
                endDate = absence.endDate,
                absenceType = absence.absenceType,
                isFlexDay = absence.isFlexDay,
                includeWeekends = absence.includeWeekends
            )
            initData()
        }
    }

    fun initData() {
        viewModelScope.launch {
            val absences = absenceRepository.getAll().map {
                SavedAbsence(
                    id = it.id,
                    absenceType = it.absenceType,
                    startDate = it.startDate,
                    endDate = it.endDate,
                    includeWeekends = it.includeWeekends,
                    isFlexDay = it.isFlexDay
                )
            }
            _uiState.update { it.copy(savedAbsences = absences) }
        }
    }

    fun selectAbsence(id: Int?) {
        _uiState.update { it.copy(selectedAbsenceId = id) }
    }

    fun deleteSelectedAbsence() {
        val selectedId = _uiState.value.selectedAbsenceId ?: return
        val selected = _uiState.value.savedAbsences.find { it.id == selectedId } ?: return
        viewModelScope.launch {
            deleteAbsenceUseCase(
                AbsenceState(
                    id = selected.id,
                    absenceType = selected.absenceType,
                    startDate = selected.startDate,
                    endDate = selected.endDate,
                    includeWeekends = selected.includeWeekends,
                    isFlexDay = selected.isFlexDay
                )
            )
            initData()
            selectAbsence(null)
        }
    }
}

data class AbsenceUiState(
    val savedAbsences: List<SavedAbsence> = emptyList(),
    val selectedAbsenceId: Int? = null
)

data class SavedAbsence(
    val id: Int,
    val absenceType: String,
    val startDate: String,
    val endDate: String,
    val includeWeekends: Boolean,
    val isFlexDay: Boolean,
)
