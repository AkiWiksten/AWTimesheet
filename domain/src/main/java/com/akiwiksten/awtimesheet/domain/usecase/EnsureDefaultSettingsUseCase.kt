package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.DEFAULT_WORK_TYPES
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import javax.inject.Inject

class EnsureDefaultSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(defaultWorkTypeLabels: List<String>, currentLanguage: String) {
        val existing = settingsRepository.getSettings()

        if (existing == null) {
            settingsRepository.insertSettings(
                SettingsState(
                    dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                    dailyLunchTimeEstimate = ZERO_TIME,
                    initialFlexTimeTotal = ZERO_TIME,
                    language = currentLanguage
                )
            )
            insertWorkTypes(defaultWorkTypeLabels)
        } else if (existing.language != currentLanguage) {
            settingsRepository.insertSettings(existing.copy(language = currentLanguage))
            settingsRepository.deleteAllWorkTypes()
            insertWorkTypes(defaultWorkTypeLabels)
        }
    }

    private suspend fun insertWorkTypes(defaultWorkTypeLabels: List<String>) {
        DEFAULT_WORK_TYPES.zip(defaultWorkTypeLabels).forEach { (_, label) ->
            settingsRepository.insertWorkType(label)
        }
    }
}
