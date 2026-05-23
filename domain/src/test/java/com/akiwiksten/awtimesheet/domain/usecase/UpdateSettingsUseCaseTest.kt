package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.settingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UpdateSettingsUseCaseTest {

    @Test
    fun invoke_saveToday_onlyUpdatesWorkdayStats() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(
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
            settings = settingsState(
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
            settings = settingsState(
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
}
