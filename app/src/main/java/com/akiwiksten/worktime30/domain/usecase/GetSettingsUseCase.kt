package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(date: String): SettingsState {
        val settings = repository.getSettings()
        val effectiveSettings = repository.getEffectiveSettingsForDate(date)
        val workTypes = repository.getWorkTypes().sorted()
        return SettingsState(
            name = settings?.name ?: "",
            employer = settings?.employer ?: "",
            dailyWorkTimeEstimate = effectiveSettings?.dailyWorkTimeEstimate ?: "",
            dailyLunchTimeEstimate = effectiveSettings?.dailyLunchTimeEstimate ?: ZERO_TIME,
            initialFlexTimeTotal = effectiveSettings?.initialFlexTimeTotal ?: ZERO_TIME,
            workTypes = workTypes
        )
    }
}
