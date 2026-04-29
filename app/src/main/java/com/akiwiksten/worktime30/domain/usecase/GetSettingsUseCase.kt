package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(_date: String): SettingsState {
        val settings = repository.getSettings()
        val workTypes = repository.getWorkTypes().sorted()
        return SettingsState(
            name = settings?.name ?: "",
            employer = settings?.employer ?: "",
            // Settings screen should show global defaults only.
            dailyWorkTimeEstimate = settings?.dailyWorkTimeEstimate ?: "",
            dailyLunchTimeEstimate = settings?.dailyLunchTimeEstimate ?: ZERO_TIME,
            initialFlexTimeTotal = settings?.initialFlexTimeTotal ?: ZERO_TIME,
            workTypes = workTypes
        )
    }
}
