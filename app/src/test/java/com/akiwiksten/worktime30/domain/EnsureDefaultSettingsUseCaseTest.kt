package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.EnsureDefaultSettingsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnsureDefaultSettingsUseCaseTest {

    @Test
    fun invoke_insertsDefaultSettings_whenMissing() = runBlocking {
        val repository = FakeSettingsRepository()
        val useCase = EnsureDefaultSettingsUseCase(repository)

        useCase()

        assertEquals(
            SettingsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME
            ),
            repository.settings
        )
    }

    @Test
    fun invoke_doesNotOverrideExistingSettings() = runBlocking {
        val existing = SettingsState(
            dailyWorkTimeEstimate = "08:00",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:20"
        )
        val repository = FakeSettingsRepository().apply {
            settings = existing
        }
        val useCase = EnsureDefaultSettingsUseCase(repository)

        useCase()

        assertEquals(existing, repository.settings)
        assertNull(repository.insertedSettings)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsState? = null
        var insertedSettings: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) {
            insertedSettings = settings
            this.settings = settings
        }

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = null

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }
}

