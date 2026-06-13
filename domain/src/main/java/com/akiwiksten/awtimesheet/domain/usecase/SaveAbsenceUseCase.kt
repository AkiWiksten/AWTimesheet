package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import javax.inject.Inject

class SaveAbsenceUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val absenceRepository: AbsenceRepository,
) {
    suspend operator fun invoke(
        startDate: String,
        endDate: String,
        absenceType: String
    ) {
        absenceRepository.insertAbsence(AbsenceState(
            absenceType = startDate,
            startDate = endDate,
            endDate = absenceType
        ))
    }

    private fun isDateInRange(date: String, startDate: String, endDate: String): Boolean {
        return date >= startDate && date <= endDate
    }
}