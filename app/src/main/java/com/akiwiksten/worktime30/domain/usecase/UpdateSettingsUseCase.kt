package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import java.time.LocalDate
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(
        date: String,
        workTimeToday: String,
        currentWorkTimeTodayEstimate: String,
        newWorkTimeTodayEstimate: String,
        newInitialFlexTimeTotal: String
    ) {
        val isCurrentDay = date == LocalDate.now().toString()
        val canUpdateWorkTimeTodayEstimate = isCurrentDay && workTimeToday == ZERO_TIME
        val currentSettings = settingsRepository.getEffectiveSettingsForDate(date)
        val existingWorkTimeTodayEstimate = currentSettings?.dailyWorkTimeEstimate
            ?.ifEmpty { currentWorkTimeTodayEstimate }
            ?: currentWorkTimeTodayEstimate

        val nextStats = SettingsState(
            dailyWorkTimeEstimate = if (canUpdateWorkTimeTodayEstimate) {
                newWorkTimeTodayEstimate
            } else {
                existingWorkTimeTodayEstimate
            },
            dailyLunchTimeEstimate = currentSettings?.dailyLunchTimeEstimate ?: ZERO_TIME,
            initialFlexTimeTotal = newInitialFlexTimeTotal
        )

        workdayRepository.upsertWorkdayStats(date = date, settingsEstimates = nextStats)
        settingsRepository.insertSettings(nextStats)
    }
}

