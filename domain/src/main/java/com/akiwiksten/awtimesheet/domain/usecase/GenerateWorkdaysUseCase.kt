package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class WorkdayGenerationScope {
    MONTH,
    YEAR
}

enum class WorkdayGenerationMode {
    INSERT_MISSING,
    UPSERT_ALL_WEEKDAYS
}

data class WorkdayGenerationResult(
    val insertedWorkdays: Int,
    val updatedWorkdays: Int,
    val weekdayCandidates: Int,
    val startDate: String,
    val endDate: String
)

class GenerateWorkdaysUseCase @Inject constructor(
    private val workdayRepository: WorkdayRepository,
    private val settingsRepository: SettingsRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

    suspend operator fun invoke(
        selectedDate: String,
        scope: WorkdayGenerationScope,
        mode: WorkdayGenerationMode = WorkdayGenerationMode.INSERT_MISSING
    ): WorkdayGenerationResult {
        require(selectedDate.isNotBlank()) { "Selected date is required." }

        val baseDate = LocalDate.parse(selectedDate, dateFormatter)
        val range = when (scope) {
            WorkdayGenerationScope.MONTH -> {
                val start = baseDate.withDayOfMonth(1)
                start to start.withDayOfMonth(start.month.length(start.isLeapYear))
            }

            WorkdayGenerationScope.YEAR -> {
                val start = baseDate.withDayOfYear(1)
                start to start.withDayOfYear(start.lengthOfYear())
            }
        }

        val globalSettings = settingsRepository.getSettings()
            ?: SettingsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME
            )

        var insertedWorkdays = 0
        var updatedWorkdays = 0
        var weekdayCandidates = 0

        var day = range.first
        while (!day.isAfter(range.second)) {
            if (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) {
                day = day.plusDays(1)
                continue
            }
            weekdayCandidates += 1

            val date = day.format(dateFormatter)
            val effectiveSettings = settingsRepository.getEffectiveSettingsForDate(date)
            val estimate = effectiveSettings?.dailyWorkTimeEstimate
                ?.takeIf { it.isNotBlank() }
                ?: globalSettings.dailyWorkTimeEstimate

            when (mode) {
                WorkdayGenerationMode.INSERT_MISSING -> {
                    if (workdayRepository.ensureWorkdayStats(date, estimate)) {
                        insertedWorkdays += 1
                    }
                }

                WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS -> {
                    val existed = workdayRepository.loadWorkday(date) != null
                    workdayRepository.upsertWorkdayStats(date, estimate)
                    if (existed) {
                        updatedWorkdays += 1
                    } else {
                        insertedWorkdays += 1
                    }
                }
            }

            day = day.plusDays(1)
        }

        return WorkdayGenerationResult(
            insertedWorkdays = insertedWorkdays,
            updatedWorkdays = updatedWorkdays,
            weekdayCandidates = weekdayCandidates,
            startDate = range.first.format(dateFormatter),
            endDate = range.second.format(dateFormatter)
        )
    }
}



