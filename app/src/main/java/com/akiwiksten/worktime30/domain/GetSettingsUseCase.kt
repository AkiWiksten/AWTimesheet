package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import javax.inject.Inject

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): SettingsEntity? = repository.getSettings()
}
