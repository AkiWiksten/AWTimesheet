package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import javax.inject.Inject

class EnsureDefaultWorkStatsUseCase @Inject constructor(
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke() {
        val existing = projectDetailsRepository.getWorkStats()
        if (existing != null) return

        projectDetailsRepository.insertWorkStats(
            WorkStatsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME
            )
        )
    }
}

