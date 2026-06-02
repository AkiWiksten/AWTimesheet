package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import javax.inject.Inject

data class UpdateSettingsParams(
    val date: String,
    val workTimeByDate: String,
    val currentWorkTimeByDateEstimate: String,
    val newWorkTimeByDateEstimate: String,
    val newInitialFlexTimeTotal: String,
    val updateGlobalSettings: Boolean = false
)

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(params: UpdateSettingsParams) {
        val currentSettings = settingsRepository.getEffectiveSettingsForDate(params.date)

        val localNextStats = SettingsState(
            dailyWorkTimeEstimate = params.newWorkTimeByDateEstimate,
            dailyLunchTimeEstimate = currentSettings?.dailyLunchTimeEstimate ?: ZERO_TIME,
            initialFlexTimeTotal = params.newInitialFlexTimeTotal
        )

        workdayRepository.upsertWorkdayStats(
            date = params.date,
            workTimeByDateEstimate = localNextStats.dailyWorkTimeEstimate
        )
        if (params.updateGlobalSettings) {
            val globalNextStats = localNextStats.copy(
                dailyWorkTimeEstimate = params.newWorkTimeByDateEstimate
            )
            settingsRepository.insertSettings(globalNextStats)
        }
    }
}
