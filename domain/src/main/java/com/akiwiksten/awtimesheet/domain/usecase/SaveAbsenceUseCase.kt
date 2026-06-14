package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
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
    private val dateRepository: DateRepository,
) {
    suspend operator fun invoke(
        startDate: String,
        endDate: String,
        absenceType: String,
        isFlexDay: Boolean = false,
        includeWeekends: Boolean = false
    ) {
        absenceRepository.insertAbsence(
            AbsenceState(
                absenceType = absenceType,
                startDate = startDate,
                endDate = endDate,
                includeWeekends = includeWeekends,
                isFlexDay = isFlexDay
            )
        )

        val dailyWorkTimeEstimate = settingsRepository.getSettings()?.dailyWorkTimeEstimate
            ?.takeIf { it != ZERO_TIME && it.isNotEmpty() }
            ?: DEFAULT_DAILY_WORK_TIME

        projectRepository.insertProjectName(absenceType)

        var date = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        var totalFlexTimeReduction = ZERO_TIME
        while (!date.isAfter(end)) {
            val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
            if (includeWeekends || !isWeekend) {
                val dateString = date.toString()
                val workTimeByDate = workdayRepository.loadWorkday(date.toString()) ?: dailyWorkTimeEstimate
                projectRepository.insertProject(
                    SingleProjectState(
                        projectName = absenceType,
                        projectTime = workTimeByDate,
                        workType = absenceType,
                        date = dateString
                    )
                )

                workdayRepository.upsertWorkdayStats(
                    date = dateString,
                    workTimeByDateEstimate = workTimeByDate
                )

                if (isFlexDay) {
                    totalFlexTimeReduction = WorkTimeCalculator.calculateFlexTime(
                        initialTime = totalFlexTimeReduction,
                        addedTime = workTimeByDate
                    )
                }
            }
            date = date.plusDays(1)
        }

        if (isFlexDay && totalFlexTimeReduction != ZERO_TIME) {
            val currentTotal = settingsRepository.getCalculatedFlextimeTotal()
            val newTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = currentTotal,
                addedTime = "-$totalFlexTimeReduction"
            )
            settingsRepository.insertCalculatedFlextimeTotal(newTotal)
        }
        dateRepository.notifyCalendarDataChanged()
    }
}
