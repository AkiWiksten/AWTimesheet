package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(name: String, employer: String, workTypes: List<String>) {
        repository.clearWorkTypes()
        workTypes.forEach { workType ->
            repository.insertWorkType(WorkTypeEntity(workType = workType))
        }
        repository.insertSettings(SettingsEntity(name = name, employer = employer))
    }
}
