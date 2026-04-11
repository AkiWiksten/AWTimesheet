package com.akiwiksten.worktime30.feature.workday

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkdayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun setDate_andProjectName_loadsWorkdayData() = runTest {
        val repository = FakeWorkdayRepository().apply {
            workday = WorkdayEntity(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                workTimeToday = "08:00",
                balanceToday = "00:30"
            )
            workStats = WorkStatsEntity(
                dailyWorkTime = "07:30",
                lunchTime = "00:30",
                workTimeTotal = "20:00",
                balanceTotal = "01:00"
            )
        }
        val viewModel = WorkdayViewModel(repository)

        viewModel.setProjectName("Alpha")
        viewModel.setDate("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is WorkdayUiState.Success)
        state as WorkdayUiState.Success
        assertEquals("2026-04-10", state.date)
        assertEquals("Alpha", state.projectName)
        assertEquals("08:00", state.startTime)
        assertEquals("20:00", state.workTimeTotal)
    }

    @Test
    fun loadWorkday_withArgs_mapsEntities_andClearDayResetsDailyFields() = runTest {
        val viewModel = WorkdayViewModel(FakeWorkdayRepository())

        viewModel.setDate("2026-04-10")
        viewModel.setProjectName("Alpha")
        viewModel.loadWorkday(
            workdayArg = WorkdayEntity(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                workTimeToday = "08:00",
                balanceToday = "00:30"
            ),
            workStatsArg = WorkStatsEntity(
                dailyWorkTime = "07:30",
                lunchTime = "00:30",
                workTimeTotal = "12:00",
                balanceTotal = "02:00"
            )
        )
        advanceUntilIdle()

        val workdayEntity = viewModel.getWorkdayEntity()
        val workStatsEntity = viewModel.getWorkStatsEntity()
        assertEquals("Alpha", workdayEntity.projectName)
        assertEquals("12:00", workStatsEntity.workTimeTotal)

        viewModel.clearDay()

        val cleared = viewModel.uiState.value as WorkdayUiState.Success
        assertEquals(ZERO_TIME, cleared.startTime)
        assertEquals(ZERO_TIME, cleared.endTime)
        assertEquals(ZERO_TIME, cleared.workTimeToday)
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        var workday: WorkdayEntity? = null
        var workStats: WorkStatsEntity? = null

        override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? = workday

        override suspend fun insertWorkday(workday: WorkdayEntity) {
            this.workday = workday
        }

        override suspend fun deleteWorkday(workday: WorkdayEntity) {
            this.workday = null
        }

        override suspend fun getWorkStats(): WorkStatsEntity? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) {
            this.workStats = workStats
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> = emptyList()
    }
}

