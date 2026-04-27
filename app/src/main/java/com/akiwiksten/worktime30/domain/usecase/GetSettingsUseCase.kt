package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): SettingsData {
        val settings = repository.getSettings()
        val workTypes = repository.getWorkTypes().sorted()
        return SettingsData(
            name = settings?.name ?: "",
            employer = settings?.employer ?: "",
            workTypes = workTypes
        )
    }
}

data class SettingsData(
    val name: String,
    val employer: String,
    val workTypes: List<String>
)
