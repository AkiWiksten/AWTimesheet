package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UpdateSettingsUseCaseTest {

    @Test
    fun invoke_saveToday_onlyUpdatesWorkdayStats() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                name = "Aki",
                employer = "WorkTime",
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val useCase = UpdateSettingsUseCase(settingsRepository, workdayRepository)

        useCase(
            UpdateSettingsParams(
                date = LocalDate.now().toString(),
                workTimeByDate = ZERO_TIME,
                currentWorkTimeByDateEstimate = "07:30",
                newWorkTimeByDateEstimate = "08:00",
                newInitialFlexTimeTotal = "+01:00",
                updateGlobalSettings = false
            )
        )

        assertEquals("08:00", workdayRepository.lastSaved)
        assertEquals(0, settingsRepository.insertCalls)
        assertEquals("07:30", settingsRepository.settings?.dailyWorkTimeEstimate)
    }

    @Test
    fun invoke_saveGlobally_updatesWorkdayAndGlobalSettings() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                name = "Aki",
                employer = "WorkTime",
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val useCase = UpdateSettingsUseCase(settingsRepository, workdayRepository)

        useCase(
            UpdateSettingsParams(
                date = LocalDate.now().toString(),
                workTimeByDate = ZERO_TIME,
                currentWorkTimeByDateEstimate = "07:30",
                newWorkTimeByDateEstimate = "08:00",
                newInitialFlexTimeTotal = "+01:00",
                updateGlobalSettings = true
            )
        )

        assertEquals("08:00", workdayRepository.lastSaved)
        assertEquals(1, settingsRepository.insertCalls)
        assertEquals("08:00", settingsRepository.settings?.dailyWorkTimeEstimate)
    }

    @Test
    fun invoke_saveGlobally_whenLocalUpdateBlocked_updatesGlobalOnly() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                name = "Aki",
                employer = "WorkTime",
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val workdayRepository = FakeWorkdayRepository()
        val useCase = UpdateSettingsUseCase(settingsRepository, workdayRepository)

        useCase(
            UpdateSettingsParams(
                date = "2000-01-01",
                workTimeByDate = "01:00",
                currentWorkTimeByDateEstimate = "07:30",
                newWorkTimeByDateEstimate = "08:00",
                newInitialFlexTimeTotal = "+01:00",
                updateGlobalSettings = true
            )
        )

        // Local/day estimate remains unchanged due to guard.
        assertEquals("07:30", workdayRepository.lastSaved)
        // Global save still applies requested value.
        assertEquals(1, settingsRepository.insertCalls)
        assertEquals("08:00", settingsRepository.settings?.dailyWorkTimeEstimate)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsState? = null
        var insertCalls: Int = 0

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) {
            insertCalls += 1
            this.settings = settings
        }

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = settings

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        var lastDate: String? = null
        var lastSaved: String? = null

        override suspend fun loadWorkday(date: String): String? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) {
            lastDate = date
            lastSaved = workTimeByDateEstimate
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> {
            return emptyList()
        }
    }
}
