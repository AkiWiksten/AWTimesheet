package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import javax.inject.Inject

class GetProjectsByDateRangeUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(start: String, end: String): List<ProjectEntity> =
        repository.getProjectsByDateRange(start, end)
}
