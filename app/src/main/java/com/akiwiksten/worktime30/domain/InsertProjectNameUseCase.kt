package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import javax.inject.Inject

class InsertProjectNameUseCase @Inject constructor(
    private val repository: ProjectRepository
) {
    suspend operator fun invoke(projectName: ProjectNameEntity) =
        repository.insertProjectName(projectName)
}
