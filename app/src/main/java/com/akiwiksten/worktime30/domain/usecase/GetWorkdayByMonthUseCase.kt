package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import java.time.LocalDate
import javax.inject.Inject

class GetWorkdayByMonthUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(date: String): List<SingleProjectState> {
        val initial = LocalDate.parse(date)
        val startMonth = initial.withDayOfMonth(1).toString()
        val endMonth = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
        return projectRepository.getProjectsByDateRange(startMonth, endMonth)
    }
}
