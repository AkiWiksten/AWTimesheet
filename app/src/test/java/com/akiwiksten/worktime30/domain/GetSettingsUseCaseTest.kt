package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.settings.SettingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSettingsUseCaseTest {

    @Test
    fun invoke_returnsMappedValuesAndSortedWorkTypes() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "WorkTime")
            workTypes = listOf("Remote", "Office")
        }
        val useCase = GetSettingsUseCase(repository)

        val result = useCase()

        assertEquals("Aki", result.name)
        assertEquals("WorkTime", result.employer)
        assertEquals(listOf("Office", "Remote"), result.workTypes)
    }

    @Test
    fun invoke_returnsEmptyNameAndEmployerWhenSettingsMissing() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = null
            workTypes = listOf("Field")
        }
        val useCase = GetSettingsUseCase(repository)

        val result = useCase()

        assertEquals("", result.name)
        assertEquals("", result.employer)
        assertEquals(listOf("Field"), result.workTypes)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsState? = null
        var workTypes: List<String> = emptyList()

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun clearWorkTypes() = Unit
    }
}
