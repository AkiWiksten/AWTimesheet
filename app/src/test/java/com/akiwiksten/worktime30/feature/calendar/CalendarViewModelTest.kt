package com.akiwiksten.worktime30.feature.calendar

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.GetCalendarDataUseCase
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onDateSelected_updatesUiStateWithCalculatedSums() = runTest {
        val workdayRepository = FakeWorkdayRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                WorkdayEntity(date = "2026-04-10", workTimeToday = "02:00")
            )
        }
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00")
            )
        }
        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(workdayRepository, projectRepository),
            dateRepository = DateRepository()
        )

        viewModel.onDateSelected("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CalendarUiState.Success)
        state as CalendarUiState.Success
        assertEquals("2026-04-10", state.date)
        assertEquals("02:00", state.timePerMonth)
        assertEquals("02:00", state.timePerDay)
    }

    @Test
    fun repositoryFailure_setsErrorState() = runTest {
        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(ThrowingWorkdayRepository(), FakeProjectRepository()),
            dateRepository = DateRepository()
        )

        viewModel.onDateSelected("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CalendarUiState.Error)
    }

    private class ThrowingWorkdayRepository : WorkdayRepository {
        override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? = null

        override suspend fun insertWorkday(workday: WorkdayEntity) = Unit

        override suspend fun deleteWorkday(workday: WorkdayEntity) = Unit

        override suspend fun getWorkStats(): WorkStatsEntity? = null

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> {
            throw IllegalStateException("boom")
        }
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        val dataByRange = mutableMapOf<String, List<WorkdayEntity>>()

        override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? = null

        override suspend fun insertWorkday(workday: WorkdayEntity) = Unit

        override suspend fun deleteWorkday(workday: WorkdayEntity) = Unit

        override suspend fun getWorkStats(): WorkStatsEntity? = null

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> {
            return dataByRange["$start|$end"] ?: emptyList()
        }
    }

    private class FakeProjectRepository : ProjectRepository {
        val dataByRange = mutableMapOf<String, List<ProjectEntity>>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> {
            return dataByRange["$start|$end"] ?: emptyList()
        }

        override suspend fun insertProject(project: ProjectEntity) = Unit

        override suspend fun deleteProject(project: ProjectEntity) = Unit

        override suspend fun getProjectNames(): List<ProjectNameEntity> = emptyList()

        override suspend fun insertProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }
}

