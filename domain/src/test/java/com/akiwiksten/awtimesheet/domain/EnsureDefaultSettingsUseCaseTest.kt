package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.DEFAULT_WORK_TYPES
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.usecase.EnsureDefaultSettingsUseCase
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.settingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EnsureDefaultSettingsUseCaseTest {

    @Test
    fun invoke_insertsDefaultSettings_whenMissing() = runBlocking {
        val repository = FakeSettingsRepository()
        val useCase = EnsureDefaultSettingsUseCase(repository)

        useCase(defaultWorkTypeLabels(), "en")

        assertEquals(
            settingsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME,
                language = "en"
            ),
            repository.settings
        )
    }

    @Test
    fun invoke_doesNotOverrideExistingSettings_whenLanguageSame() = runBlocking {
        val existing = settingsState(
            dailyWorkTimeEstimate = "08:00",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:20",
            language = "en"
        )
        val repository = FakeSettingsRepository().apply {
            settings = existing
        }
        val useCase = EnsureDefaultSettingsUseCase(repository)

        useCase(defaultWorkTypeLabels(), "en")

        assertEquals(existing, repository.settings)
        assertNull(repository.insertedSettings)
    }

    @Test
    fun invoke_updatesLanguageAndResetsWorkTypes_whenLanguageChanged() = runBlocking {
        val existing = settingsState(
            dailyWorkTimeEstimate = "08:00",
            language = "en"
        )
        val repository = FakeSettingsRepository().apply {
            settings = existing
        }
        val useCase = EnsureDefaultSettingsUseCase(repository)

        useCase(listOf("Muu", "Loma"), "fi")

        assertEquals("fi", repository.settings?.language)
        assertEquals(2, repository.workTypes.size)
        assertEquals("Muu", repository.workTypes[0])
    }

    private fun defaultWorkTypeLabels(): List<String> {
        return DEFAULT_WORK_TYPES.map { it.toString() }
    }
}
