package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import java.time.LocalDate
import javax.inject.Inject

data class ProjectsByMonthResult(
    val projects: List<SingleProjectState>,
    val endOfMonth: String
)

class GetProjectsByMonthUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(date: String): ProjectsByMonthResult {
        val parsedDate = LocalDate.parse(date)
        val startOfMonth = parsedDate.withDayOfMonth(1).toString()
        val endOfMonth = parsedDate
            .withDayOfMonth(parsedDate.month.length(parsedDate.isLeapYear))
            .toString()
        val projects = projectRepository.getProjectsByDateRange(startOfMonth, endOfMonth)
        return ProjectsByMonthResult(
            projects = projects,
            endOfMonth = endOfMonth
        )
    }
}
