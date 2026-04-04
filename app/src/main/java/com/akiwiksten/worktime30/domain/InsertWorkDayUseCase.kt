package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import javax.inject.Inject

class InsertWorkDayUseCase @Inject constructor(
    private val repository: WorkDayRepository
) {
    suspend operator fun invoke(workDay: WorkDayEntity) = repository.insertWorkDay(workDay)
}
