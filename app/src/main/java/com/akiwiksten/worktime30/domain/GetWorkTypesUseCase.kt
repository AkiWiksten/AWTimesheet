package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import javax.inject.Inject

class GetWorkTypesUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(): List<WorkTypeEntity> = repository.getWorkTypes()
}
