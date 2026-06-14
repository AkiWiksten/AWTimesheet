package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
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

class DeleteAbsenceUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val absenceRepository: AbsenceRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository,
) {
    suspend operator fun invoke(absenceState: AbsenceState) {
        absenceRepository.delete(absence = absenceState)

        val dailyWorkTimeEstimate = settingsRepository.getSettings()?.dailyWorkTimeEstimate
            ?.takeIf { it != ZERO_TIME && it.isNotEmpty() }
            ?: DEFAULT_DAILY_WORK_TIME

        var date = LocalDate.parse(absenceState.startDate)
        val end = LocalDate.parse(absenceState.endDate)

        var totalFlexTimeReversal = ZERO_TIME
        while (!date.isAfter(end)) {
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            if (absenceState.includeWeekends || !isWeekend) {
                val dateString = date.toString()
                projectRepository.deleteProject(SingleProjectState(
                    projectName = absenceState.absenceType,
                    date = dateString
                ))

                if (absenceState.isFlexDay) {
                    val workTimeByDate = workdayRepository.loadWorkday(dateString) ?: dailyWorkTimeEstimate
                    totalFlexTimeReversal = WorkTimeCalculator.calculateFlexTime(
                        initialTime = totalFlexTimeReversal,
                        addedTime = workTimeByDate
                    )
                }
            }

            date = date.plusDays(1)
        }

        if (absenceState.isFlexDay && totalFlexTimeReversal != ZERO_TIME) {
            val currentTotal = settingsRepository.getCalculatedFlextimeTotal()
            val newTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = currentTotal,
                addedTime = totalFlexTimeReversal
            )
            settingsRepository.insertCalculatedFlextimeTotal(newTotal)
        }
    }
}