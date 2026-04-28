package com.akiwiksten.worktime30.feature.calendar

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.usecase.GetCalendarDataUseCase
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
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00", 0, "", "")
            )
        }
        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(projectRepository),
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
    fun init_startsLoadingAndEmitsInitialDate() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-01-01") }
        val projectRepository = FakeProjectRepository()

        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(projectRepository),
            dateRepository = dateRepository
        )

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is CalendarUiState.Success)
        assertEquals("2026-01-01", (state as CalendarUiState.Success).date)
    }

    @Test
    fun convertMillisToDate_returnsCorrectIsoFormat() {
        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(FakeProjectRepository()),
            dateRepository = DateRepository()
        )

        // 2026-04-10 00:00:00 UTC (The formatter uses systemDefault, but this is a stable timestamp)
        // We'll use a local date to millis conversion to be environment independent in the test itself
        val localDate = java.time.LocalDate.of(2026, 4, 10)
        val millis = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val result = viewModel.convertMillisToDate(millis)

        assertEquals("2026-04-10", result)
    }

    private open class FakeProjectRepository : ProjectRepository {
        val dataByRange = mutableMapOf<String, List<ProjectEntity>>()

        override suspend fun anyRecords(): Boolean = false

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return (dataByRange["$start|$end"] ?: emptyList()).map { entity ->
                SingleProjectState(
                    date = entity.date,
                    projectName = entity.projectName,
                    projectTime = entity.projectTime,
                    kilometres = entity.kilometres.toString(),
                    allowance = entity.allowance,
                    workType = entity.workType
                )
            }
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false

        override suspend fun getProjectTimeSumByDate(date: String): String = ZERO_TIME
    }
}
