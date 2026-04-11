package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetSettingsUseCaseTest {

    @Test
    fun invoke_returnsMappedValuesAndSortedWorkTypes() = runBlocking {
        val repository = FakeSettingsRepository().apply {
            settings = SettingsEntity(name = "Aki", employer = "WorkTime")
            workTypes = listOf(
                WorkTypeEntity(workType = "Remote"),
                WorkTypeEntity(workType = "Office")
            )
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
            workTypes = listOf(WorkTypeEntity(workType = "Field"))
        }
        val useCase = GetSettingsUseCase(repository)

        val result = useCase()

        assertEquals("", result.name)
        assertEquals("", result.employer)
        assertEquals(listOf("Field"), result.workTypes)
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsEntity? = null
        var workTypes: List<WorkTypeEntity> = emptyList()

        override suspend fun getSettings(): SettingsEntity? = settings

        override suspend fun insertSettings(settings: SettingsEntity) = Unit

        override suspend fun getWorkTypes(): List<WorkTypeEntity> = workTypes

        override suspend fun insertWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun deleteWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun clearWorkTypes() = Unit
    }
}

