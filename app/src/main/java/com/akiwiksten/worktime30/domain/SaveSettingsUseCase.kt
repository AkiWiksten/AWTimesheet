package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.settings.SettingsState
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(name: String, employer: String, workTypes: List<String>) {
        repository.clearWorkTypes()
        workTypes.forEach { workType ->
            repository.insertWorkType(WorkTypeEntity(workType = workType))
        }
        repository.insertSettings(SettingsState(name = name, employer = employer))
    }
}
