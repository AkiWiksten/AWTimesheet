package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.test.InMemoryDateRepository
import com.akiwiksten.awtimesheet.domain.usecase.SaveSettingsUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.projectState
import com.akiwiksten.awtimesheet.test.settingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SaveSettingsUseCaseTest {

    @Test
    fun invoke_clearsWorkTypes_insertsNewTypes_andSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val projectRepository = FakeProjectRepository()
        val dateRepository = InMemoryDateRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            settings = settingsState(
                name = "Aki",
                employer = "WorkTime",
                workTypes = listOf("Office", "Remote")
            )
        )

        assertEquals(
            listOf("clearWorkTypes", "insertWorkType:Office", "insertWorkType:Remote", "insertSettings"),
            settingsRepository.operations
        )
        assertEquals(settingsState(name = "Aki", employer = "WorkTime"), settingsRepository.savedSettings)
    }

    @Test
    fun invoke_withEmptyWorkTypes_stillSavesSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository(),
            projectRepository = FakeProjectRepository(),
            dateRepository = InMemoryDateRepository()
        )

        useCase(settings = settingsState(name = "Aki", employer = "WorkTime", workTypes = emptyList()))

        assertEquals(listOf("clearWorkTypes", "insertSettings"), settingsRepository.operations)
        assertEquals(settingsState(name = "Aki", employer = "WorkTime"), settingsRepository.savedSettings)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_currentDayAndZeroWorkTime_savesDailyWorkTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply { updateDate(LocalDate.now().toString()) }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository(),
            projectRepository = FakeProjectRepository(),
            dateRepository = dateRepository
        )

        useCase(
            settings = settingsState(
                name = "Aki",
                employer = "WorkTime",
                workTypes = emptyList(),
                dailyWorkTimeEstimate = "07:30"
            )
        )

        assertEquals("07:30", settingsRepository.insertedSettings?.dailyWorkTimeEstimate)
        assertEquals("00:00", settingsRepository.insertedSettings?.dailyLunchTimeEstimate)
    }

    @Test
    fun invoke_withLunchTimeEstimate_currentDayAndZeroWorkTime_savesLunchTime() = runBlocking {
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply { updateDate(LocalDate.now().toString()) }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository(),
            projectRepository = FakeProjectRepository(),
            dateRepository = dateRepository
        )

        useCase(
            settings = settingsState(
                name = "Aki",
                employer = "WorkTime",
                workTypes = emptyList(),
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30"
            )
        )

        assertEquals("00:30", settingsRepository.insertedSettings?.dailyLunchTimeEstimate)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_nonCurrentDay_updatesGlobalStatsButNotWorkday() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val dateRepository = InMemoryDateRepository().apply { updateDate("2000-01-01") }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectRepository = FakeProjectRepository(),
            dateRepository = dateRepository
        )

        useCase(
            settings = settingsState(
                name = "Aki",
                employer = "WorkTime",
                workTypes = emptyList(),
                dailyWorkTimeEstimate = "08:00"
            )
        )

        assertEquals("08:00", settingsRepository.insertedSettings?.dailyWorkTimeEstimate)
        assertNull(workdayRepository.upsertedWorkdayDate)
    }

    @Test
    fun invoke_withDailyWorkTimeEstimate_currentDayAndNonZeroWorkTime_updatesGlobalStatsButNotWorkday() = runBlocking {
        val today = LocalDate.now().toString()
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(projectState(date = today, projectName = "Alpha", projectTime = "01:00"))
        }
        val dateRepository = InMemoryDateRepository().apply { updateDate(today) }
        val useCase = SaveSettingsUseCase(
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        useCase(
            settings = settingsState(
                name = "Aki",
                employer = "WorkTime",
                workTypes = emptyList(),
                dailyWorkTimeEstimate = "08:00"
            )
        )

        assertEquals("08:00", settingsRepository.insertedSettings?.dailyWorkTimeEstimate)
        assertNull(workdayRepository.upsertedWorkdayDate)
    }
}
