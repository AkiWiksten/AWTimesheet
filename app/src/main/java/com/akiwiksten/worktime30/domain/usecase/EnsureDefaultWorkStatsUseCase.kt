package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
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
