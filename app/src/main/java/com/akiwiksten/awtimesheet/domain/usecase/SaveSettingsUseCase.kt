package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import java.time.LocalDate
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository,
    private val projectRepository: ProjectRepository,
    private val dateRepository: DateRepository
) {
    suspend operator fun invoke(settings: SettingsState) {
        settingsRepository.deleteAllWorkTypes()
        settings.workTypes.forEach { workType ->
            settingsRepository.insertWorkType(workType)
        }
        settingsRepository.insertSettings(SettingsState(name = settings.name, employer = settings.employer))

        val existingGlobalStats = settingsRepository.getSettings()
        val resolvedInitialFlexTimeTotal = settings.initialFlexTimeTotal
            .takeIf { it != ZERO_TIME }
            ?: existingGlobalStats?.initialFlexTimeTotal
            ?: ZERO_TIME

        if (
            settings.dailyWorkTimeEstimate.isNotEmpty() ||
            settings.dailyLunchTimeEstimate != ZERO_TIME ||
            resolvedInitialFlexTimeTotal != ZERO_TIME
        ) {
            // Persist global estimates to merged settings-backed work stats.
            settingsRepository.insertSettings(
                SettingsState(
                    dailyWorkTimeEstimate = settings.dailyWorkTimeEstimate,
                    dailyLunchTimeEstimate = settings.dailyLunchTimeEstimate,
                    initialFlexTimeTotal = resolvedInitialFlexTimeTotal
                )
            )

            val selectedDate = dateRepository.selectedDate.value
            val isCurrentDay = selectedDate == LocalDate.now().toString()

            val workTimeByDate = if (selectedDate.isNotEmpty()) {
                projectRepository.getWorkTimeByDate(selectedDate)
            } else {
                ZERO_TIME
            }

            if (isCurrentDay && workTimeByDate == ZERO_TIME && selectedDate.isNotEmpty()) {
                workdayRepository.upsertWorkdayStats(
                    date = selectedDate,
                    workTimeByDateEstimate = settings.dailyWorkTimeEstimate
                )
            }
        }
    }
}
