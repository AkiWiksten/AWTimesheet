package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSettingsUseCaseTest {

    @Test
    fun invoke_returnsMappedValuesAndSortedWorkTypes() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "WorkTime")
            effectiveSettings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
            workTypes = listOf("Remote", "Office")
        }
        val useCase = GetSettingsUseCase(repository)

        val result = useCase("2026-04-10")

        assertEquals("Aki", result.name)
        assertEquals("WorkTime", result.employer)
        assertEquals("07:30", result.dailyWorkTimeEstimate)
        assertEquals("00:30", result.dailyLunchTimeEstimate)
        assertEquals("01:00", result.initialFlexTimeTotal)
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

        val result = useCase("2026-04-10")

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

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = effectiveSettings

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }
}
