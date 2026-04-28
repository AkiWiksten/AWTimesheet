package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import java.time.LocalDate
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository,
    private val projectRepository: ProjectRepository,
    private val dateRepository: DateRepository
) {
    suspend operator fun invoke(
        name: String,
        employer: String,
        workTypes: List<String>,
        dailyWorkTimeEstimate: String = "",
        lunchTimeEstimate: String = ZERO_TIME
    ) {
        settingsRepository.clearWorkTypes()
        workTypes.forEach { workType ->
            settingsRepository.insertWorkType(workType)
        }
        settingsRepository.insertSettings(SettingsState(name = name, employer = employer))

        if (dailyWorkTimeEstimate.isNotEmpty()) {
            // Persist global estimates to merged settings-backed work stats.
            val existingGlobalStats = settingsRepository.getWorkStats()
            settingsRepository.insertWorkStats(
                WorkStatsState(
                    dailyWorkTimeEstimate = dailyWorkTimeEstimate,
                    dailyLunchTimeEstimate = lunchTimeEstimate,
                    initialFlexTimeTotal = existingGlobalStats?.initialFlexTimeTotal ?: ZERO_TIME
                )
            )

            val selectedDate = dateRepository.selectedDate.value
            val isCurrentDay = selectedDate == LocalDate.now().toString()

            val workTimeToday = if (selectedDate.isNotEmpty()) {
                projectRepository.getProjectTimeSumByDate(selectedDate)
            } else {
                ZERO_TIME
            }

            if (isCurrentDay && workTimeToday == ZERO_TIME && selectedDate.isNotEmpty()) {
                val existingWorkStats = settingsRepository.getWorkStatsByDate(selectedDate)
                workdayRepository.upsertWorkdayStats(
                    date = selectedDate,
                    workStats = WorkStatsState(
                        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
                        dailyLunchTimeEstimate = lunchTimeEstimate,
                        initialFlexTimeTotal = existingWorkStats?.initialFlexTimeTotal ?: ZERO_TIME
                    )
                )
            }
        }
    }
}
