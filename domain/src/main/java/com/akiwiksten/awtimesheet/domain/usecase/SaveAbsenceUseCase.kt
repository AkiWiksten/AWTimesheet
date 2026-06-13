package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class SaveAbsenceUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val absenceRepository: AbsenceRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository,
) {
    suspend operator fun invoke(
        startDate: String,
        endDate: String,
        absenceType: String,
        hasWeekends: Boolean = false
    ) {
        absenceRepository.insertAbsence(AbsenceState(
            absenceType = absenceType,
            startDate = startDate,
            endDate = endDate,
            hasWeekends = hasWeekends
        ))
        
        val dailyWorkTimeEstimate = settingsRepository.getSettings()?.dailyWorkTimeEstimate
            ?.takeIf { it != ZERO_TIME && it.isNotEmpty() }
            ?: DEFAULT_DAILY_WORK_TIME

        projectRepository.insertProjectName(absenceType)

        var date = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        while (!date.isAfter(end)) {
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            if (hasWeekends || !isWeekend) {
                val dateString = date.toString()
                val workTimeByDate = workdayRepository.loadWorkday(date.toString()) ?: dailyWorkTimeEstimate
                projectRepository.insertProject(SingleProjectState(
                    projectName = absenceType,
                    projectTime = workTimeByDate,
                    workType = absenceType,
                    date = dateString
                ))

                workdayRepository.upsertWorkdayStats(
                    date = dateString,
                    workTimeByDateEstimate = workTimeByDate
                )
            }
            
            date = date.plusDays(1)
        }
    }
}
