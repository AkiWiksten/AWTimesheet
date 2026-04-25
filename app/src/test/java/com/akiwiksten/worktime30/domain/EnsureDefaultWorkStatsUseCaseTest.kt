package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnsureDefaultWorkStatsUseCaseTest {

    @Test
    fun invoke_insertsDefaultWorkStats_whenMissing() = runBlocking {
        val repository = FakeProjectDetailsRepository()
        val useCase = EnsureDefaultWorkStatsUseCase(repository)

        useCase()

        assertEquals(
            WorkStatsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME
            ),
            repository.workStats
        )
    }

    @Test
    fun invoke_doesNotOverrideExistingWorkStats() = runBlocking {
        val existing = WorkStatsState(
            dailyWorkTimeEstimate = "08:00",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:20"
        )
        val repository = FakeProjectDetailsRepository().apply {
            workStats = existing
        }
        val useCase = EnsureDefaultWorkStatsUseCase(repository)

        useCase()

        assertEquals(existing, repository.workStats)
        assertNull(repository.insertedWorkStats)
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var workStats: WorkStatsState? = null
        var insertedWorkStats: WorkStatsState? = null

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsState) {
            insertedWorkStats = workStats
            this.workStats = workStats
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}

