package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.EnsureDefaultWorkStatsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnsureDefaultWorkStatsUseCaseTest {

    @Test
    fun invoke_insertsDefaultWorkStats_whenMissing() = runBlocking {
        val repository = FakeSettingsRepository()
        val useCase = EnsureDefaultWorkStatsUseCase(repository)

        useCase()

        assertEquals(
            SettingsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME
            ),
            repository.workStats
        )
    }

    @Test
    fun invoke_doesNotOverrideExistingWorkStats() = runBlocking {
        val existing = SettingsState(
            dailyWorkTimeEstimate = "08:00",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:20"
        )
        val repository = FakeSettingsRepository().apply {
            workStats = existing
        }
        val useCase = EnsureDefaultWorkStatsUseCase(repository)

        useCase()

        assertEquals(existing, repository.workStats)
        assertNull(repository.insertedWorkStats)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workStats: SettingsState? = null
        var insertedWorkStats: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getGlobalSettingsEstimates(): SettingsState? = workStats

        override suspend fun saveGlobalSettingsEstimates(estimates: SettingsState) {
            insertedWorkStats = estimates
            this.workStats = estimates
        }

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = null


        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }
}
