package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSettingsUseCaseTest {

    @Test
    fun invoke_returnsMappedValuesAndSortedWorkTypes() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = SettingsState(
                name = "Aki",
                employer = "WorkTime",
                dailyWorkTimeEstimate = "08:00",
                dailyLunchTimeEstimate = "00:45",
                initialFlexTimeTotal = "+01:30"
            )
            effectiveSettings = SettingsState(
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

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsState? = null
        var effectiveSettings: SettingsState? = null
        var workTypes: List<String> = emptyList()
        var calculatedFlexTimeTotal: String = ZERO_TIME

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = effectiveSettings

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit

        override suspend fun getCalculatedFlextimeTotal(): String = calculatedFlexTimeTotal

        override suspend fun insertCalculatedFlextimeTotal(flexTime: String) {
            calculatedFlexTimeTotal = flexTime
        }
    }
}
