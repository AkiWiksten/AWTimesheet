package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import java.time.LocalDate
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
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
            // Always persist estimates to global WorkStatsEntity
            val existingGlobalStats = projectDetailsRepository.getWorkStats()
            projectDetailsRepository.insertWorkStats(
                WorkStatsState(
                    dailyWorkTimeEstimate = dailyWorkTimeEstimate,
                    dailyLunchTimeEstimate = lunchTimeEstimate,
                    initialFlexTimeTotal = existingGlobalStats?.initialFlexTimeTotal ?: ZERO_TIME
                )
            )

            val selectedDate = dateRepository.selectedDate.value
            val isCurrentDay = selectedDate == LocalDate.now().toString()

            val workTimeToday = if (selectedDate.isNotEmpty()) {
                projectRepository
                    .getProjectsByDateRange(selectedDate, selectedDate)
                    .fold(ZERO_TIME) { acc, project ->
                        WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
                    }
            } else {
                ZERO_TIME
            }

            if (isCurrentDay && workTimeToday == ZERO_TIME && selectedDate.isNotEmpty()) {
                val existingWorkStats = projectDetailsRepository.getWorkStatsByDate(selectedDate)
                projectDetailsRepository.upsertWorkdayStats(
                    date = selectedDate,
                    workTimeToday = workTimeToday,
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
