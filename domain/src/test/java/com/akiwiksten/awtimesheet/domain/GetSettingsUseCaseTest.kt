package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.usecase.GetSettingsUseCase
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.settingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSettingsUseCaseTest {

    @Test
    fun invoke_returnsMappedValuesAndSortedWorkTypes() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = settingsState(
                name = "Aki",
                employer = "WorkTime",
                dailyWorkTimeEstimate = "08:00",
                dailyLunchTimeEstimate = "00:45",
                initialFlexTimeTotal = "+01:30"
            )
            effectiveSettings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
            workTypes = listOf("Remote", "Office")
        }
        val useCase = GetSettingsUseCase(repository)

        val result = useCase()

        assertEquals("Aki", result.name)
        assertEquals("WorkTime", result.employer)
        assertEquals("08:00", result.dailyWorkTimeEstimate)
        assertEquals("00:45", result.dailyLunchTimeEstimate)
        assertEquals("+01:30", result.initialFlexTimeTotal)
        assertEquals(listOf("Office", "Remote"), result.workTypes)
    }

    @Test
    fun invoke_returnsDefaultsWhenSettingsMissing() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = null
            effectiveSettings = null
            workTypes = listOf("Field")
        }
        val useCase = GetSettingsUseCase(repository)

        val result = useCase()

        assertEquals("", result.name)
        assertEquals("", result.employer)
        assertEquals("", result.dailyWorkTimeEstimate)
        assertEquals(ZERO_TIME, result.dailyLunchTimeEstimate)
        assertEquals(ZERO_TIME, result.initialFlexTimeTotal)
        assertEquals(listOf("Field"), result.workTypes)
    }
}
