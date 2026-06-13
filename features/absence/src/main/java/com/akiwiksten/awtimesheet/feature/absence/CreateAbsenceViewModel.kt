package com.akiwiksten.awtimesheet.feature.absence

import androidx.lifecycle.ViewModel
import com.akiwiksten.awtimesheet.domain.usecase.SaveAbsenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateAbsenceViewModel @Inject constructor(
    private val getCalendarDataUseCase: SaveAbsenceUseCase,
) : ViewModel() {

}