package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import java.time.LocalDate
import javax.inject.Inject

data class UpdateSettingsParams(
    val date: String,
    val workTimeToday: String,
    val currentWorkTimeTodayEstimate: String,
    val newWorkTimeTodayEstimate: String,
    val newInitialFlexTimeTotal: String,
    val updateGlobalSettings: Boolean = false
)

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(params: UpdateSettingsParams) {
        val isCurrentDay = params.date == LocalDate.now().toString()
        val canUpdateWorkTimeTodayEstimate = isCurrentDay && params.workTimeToday == ZERO_TIME
        val currentSettings = settingsRepository.getEffectiveSettingsForDate(params.date)
        val existingWorkTimeTodayEstimate = currentSettings?.dailyWorkTimeEstimate
            ?.ifEmpty { params.currentWorkTimeTodayEstimate }
            ?: params.currentWorkTimeTodayEstimate

        val localNextStats = SettingsState(
            dailyWorkTimeEstimate = if (canUpdateWorkTimeTodayEstimate) {
                params.newWorkTimeTodayEstimate
            } else {
                existingWorkTimeTodayEstimate
            },
            dailyLunchTimeEstimate = currentSettings?.dailyLunchTimeEstimate ?: ZERO_TIME,
            initialFlexTimeTotal = params.newInitialFlexTimeTotal
        )

        workdayRepository.upsertWorkdayStats(date = params.date, settingsEstimates = localNextStats)
        if (params.updateGlobalSettings) {
            val globalNextStats = localNextStats.copy(
                dailyWorkTimeEstimate = params.newWorkTimeTodayEstimate
            )
            settingsRepository.insertSettings(globalNextStats)
        }
    }
}
