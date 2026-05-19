package com.akiwiksten.awtimesheet.feature.calendar

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.usecase.GetCalendarDataUseCase
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    fun init_withFreshFetch_doesNotDoubleCountStoredWorkTimeChange() = runTest {
        val dateRepository = DateRepository().apply {
            updateDate("2026-04-10")
            updateWorkTimeByDateChange("-02:30")
        }
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00", 0, "", "")
            )
        }

        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(projectRepository),
            dateRepository = dateRepository
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value as CalendarUiState.Success
        assertEquals("08:00", state.timePerMonth)
        assertEquals("08:00", state.timePerWeek)
        assertEquals("08:00", state.timePerDay)
        assertEquals(ZERO_TIME, dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun refresh_withCachedData_appliesStoredWorkTimeChangeOnce() = runTest {
        val dateRepository = DateRepository().apply {
            updateDate("2026-04-10")
        }
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00", 0, "", "")
            )
        }

        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(projectRepository),
            dateRepository = dateRepository
        )

        advanceUntilIdle()

        dateRepository.updateWorkTimeByDateChange("02:30")
        viewModel.refresh()
        advanceUntilIdle()

        val refreshedState = viewModel.uiState.value as CalendarUiState.Success
        assertEquals("10:30", refreshedState.timePerMonth)
        assertEquals("10:30", refreshedState.timePerWeek)
        assertEquals("10:30", refreshedState.timePerDay)
        assertEquals(ZERO_TIME, dateRepository.workTimeByDateChange.value)

        viewModel.refresh()
        advanceUntilIdle()

        val secondRefreshState = viewModel.uiState.value as CalendarUiState.Success
        assertEquals("10:30", secondRefreshState.timePerMonth)
        assertEquals("10:30", secondRefreshState.timePerWeek)
        assertEquals("10:30", secondRefreshState.timePerDay)
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
    fun onDateSelected_afterInitialSuccess_doesNotEmitLoading() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00", 0, "", "")
            )
            dataByRange["2026-05-01|2026-05-31"] = listOf(
                ProjectEntity(date = "2026-05-10", projectName = "Beta", projectTime = "03:00", 0, "", "")
            )
        }
        val viewModel = CalendarViewModel(
            getCalendarDataUseCase = GetCalendarDataUseCase(projectRepository),
            dateRepository = dateRepository
        )
        val emissions = mutableListOf<CalendarUiState>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { emissions += it }
        }

        advanceUntilIdle()
        emissions.clear()

        projectRepository.readDelayMillis = 1_000
        viewModel.onDateSelected("2026-05-10")
        advanceTimeBy(500)

        assertTrue(emissions.none { it is CalendarUiState.Loading })

        advanceUntilIdle()

        val state = viewModel.uiState.value as CalendarUiState.Success
        assertEquals("2026-05-10", state.date)
        assertEquals("03:00", state.timePerMonth)
        assertEquals("03:00", state.timePerDay)

        job.cancel()
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
        var readDelayMillis: Long = 0

        override suspend fun anyRecords(): Boolean = false

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            if (readDelayMillis > 0) {
                delay(readDelayMillis)
            }
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

        override suspend fun getProject(date: String, projectName: String): SingleProjectState? = null

        override suspend fun getWorkTimeByDate(date: String): String = ZERO_TIME
    }
}
