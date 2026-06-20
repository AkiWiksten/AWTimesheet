package com.akiwiksten.awtimesheet.feature.absence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.usecase.SaveAbsenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateAbsenceViewModel @Inject constructor(
    private val saveAbsenceUseCase: SaveAbsenceUseCase,
    private val absenceRepository: AbsenceRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateAbsenceUiState())
    val uiState: StateFlow<CreateAbsenceUiState> = _uiState.asStateFlow()

    init {
        observeExistingAbsences()
    }

    private fun observeExistingAbsences() {
        absenceRepository.getAll()
            .onEach { absences ->
                val savedAbsences = absences.map {
                    SavedAbsence(
                        id = it.id,
                        absenceType = it.absenceType,
                        startDate = it.startDate,
                        endDate = it.endDate,
                        includeWeekends = it.includeWeekends,
                        isFlexDay = it.isFlexDay
                    )
                }
                _uiState.update { it.copy(existingAbsences = savedAbsences) }
            }
            .launchIn(viewModelScope)
    }

    fun addAbsence(absence: AbsenceState, onComplete: () -> Unit) {
        viewModelScope.launch {
            saveAbsenceUseCase(
                startDate = absence.startDate,
                endDate = absence.endDate,
                absenceType = absence.absenceType,
                isFlexDay = absence.isFlexDay,
                includeWeekends = absence.includeWeekends
            )
            onComplete()
        }
    }
}

data class CreateAbsenceUiState(
    val existingAbsences: List<SavedAbsence> = emptyList(),
)
