package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class DeleteAbsenceUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val absenceRepository: AbsenceRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository,
) {
    suspend operator fun invoke(
        id: Int,
        startDate: String,
        endDate: String,
        absenceType: String,
        isFlexDay: Boolean = false,
        includeWeekends: Boolean = false
    ) {
        absenceRepository.delete(
            AbsenceState(
                id = id,
                absenceType = absenceType,
                startDate = startDate,
                endDate = endDate,
                includeWeekends = includeWeekends
            )
        )

        var date = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        while (!date.isAfter(end)) {
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            if (includeWeekends || !isWeekend) {
                val dateString = date.toString()
                projectRepository.deleteProject(SingleProjectState(
                    projectName = absenceType,
                    date = dateString
                ))
            }

            date = date.plusDays(1)
        }
    }
}