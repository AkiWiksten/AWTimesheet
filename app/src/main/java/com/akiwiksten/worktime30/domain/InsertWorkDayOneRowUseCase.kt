package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import com.akiwiksten.worktime30.data.database.entity.WorkDayOneRowEntity
import javax.inject.Inject

class InsertWorkDayOneRowUseCase @Inject constructor(
    private val repository: WorkDayRepository
) {
    suspend operator fun invoke(workDayOneRow: WorkDayOneRowEntity) =
        repository.insertWorkDayOneRow(workDayOneRow)
}
