package com.akiwiksten.awtimesheet.feature.absence

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AbsenceViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(AbsenceUiState())
    val uiState: StateFlow<AbsenceUiState> = _uiState.asStateFlow()

    // Note: In a real app, this would be loaded from a repository.
    // For now, it matches the screen's initial empty state.
    fun addAbsence(workType: String, startDate: String, endDate: String) {
        val newAbsence = SavedAbsence(workType, startDate, endDate)
        _uiState.value = _uiState.value.copy(
            savedAbsences = _uiState.value.savedAbsences + newAbsence
        )
    }
}

data class AbsenceUiState(
    val savedAbsences: List<SavedAbsence> = emptyList()
)

data class SavedAbsence(
    val workType: String,
    val startDate: String,
    val endDate: String,
)
