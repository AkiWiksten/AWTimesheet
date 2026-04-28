package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
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
        dailyLunchTimeEstimate: String = ZERO_TIME
    ) {
        settingsRepository.deleteAllWorkTypes()
        workTypes.forEach { workType ->
            settingsRepository.insertWorkType(workType)
        }
        settingsRepository.insertSettings(SettingsState(name = name, employer = employer))

        if (dailyWorkTimeEstimate.isNotEmpty()) {
            // Persist global estimates to merged settings-backed work stats.
            val existingGlobalStats = settingsRepository.getSettings()
            settingsRepository.insertSettings(
                SettingsState(
                    dailyWorkTimeEstimate = dailyWorkTimeEstimate,
                    dailyLunchTimeEstimate = dailyLunchTimeEstimate,
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
                val existingWorkStats = settingsRepository.getEffectiveSettingsForDate(selectedDate)
                workdayRepository.upsertWorkdayStats(
                    date = selectedDate,
                    settingsEstimates = SettingsState(
                        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
                        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
                        initialFlexTimeTotal = existingWorkStats?.initialFlexTimeTotal ?: ZERO_TIME
                    )
                )
            }
        }
    }
}
