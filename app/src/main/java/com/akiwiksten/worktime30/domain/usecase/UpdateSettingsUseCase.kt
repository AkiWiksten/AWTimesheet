package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import java.time.LocalDate
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
        val isCurrentDay = params.date == LocalDate.now().toString()
        val canUpdateWorkTimeByDateEstimate = isCurrentDay && params.workTimeByDate == ZERO_TIME
        val currentSettings = settingsRepository.getEffectiveSettingsForDate(params.date)
        val existingWorkTimeByDateEstimate = currentSettings?.dailyWorkTimeEstimate
            ?.ifEmpty { params.currentWorkTimeByDateEstimate }
            ?: params.currentWorkTimeByDateEstimate

        val localNextStats = SettingsState(
            dailyWorkTimeEstimate = if (canUpdateWorkTimeByDateEstimate) {
                params.newWorkTimeByDateEstimate
            } else {
                existingWorkTimeByDateEstimate
            },
            dailyLunchTimeEstimate = currentSettings?.dailyLunchTimeEstimate ?: ZERO_TIME,
            initialFlexTimeTotal = params.newInitialFlexTimeTotal
        )

        workdayRepository.upsertWorkdayStats(date = params.date,
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
