package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import java.time.LocalDate
import javax.inject.Inject

class GetProjectsByMonthUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(date: String): List<ProjectEntity> {
        val initial = LocalDate.parse(date)
        val startMonth = initial.withDayOfMonth(1).toString()
        val endMonth = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
        return projectRepository.getProjectsByDateRange(startMonth, endMonth)
    }
}
