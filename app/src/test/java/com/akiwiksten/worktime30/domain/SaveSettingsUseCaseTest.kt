package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveSettingsUseCaseTest {

    @Test
    fun invoke_clearsWorkTypes_insertsNewTypes_andSavesSettings() = runBlocking {
        val repository = FakeSettingsRepository()
        val useCase = SaveSettingsUseCase(repository)

        useCase(
            name = "Aki",
            employer = "WorkTime",
            workTypes = listOf("Office", "Remote")
        )

        assertEquals(
            listOf(
                "clearWorkTypes",
                "insertWorkType:Office",
                "insertWorkType:Remote",
                "insertSettings"
            ),
            repository.operations
        )
        assertEquals(
            SettingsEntity(name = "Aki", employer = "WorkTime"),
            repository.savedSettings
        )
    }

    @Test
    fun invoke_withEmptyWorkTypes_stillSavesSettings() = runBlocking {
        val repository = FakeSettingsRepository()
        val useCase = SaveSettingsUseCase(repository)

        useCase(name = "Aki", employer = "WorkTime", workTypes = emptyList())

        assertEquals(listOf("clearWorkTypes", "insertSettings"), repository.operations)
        assertEquals(SettingsEntity(name = "Aki", employer = "WorkTime"), repository.savedSettings)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val operations = mutableListOf<String>()
        val insertedWorkTypes = mutableListOf<WorkTypeEntity>()
        var savedSettings: SettingsEntity? = null

        override suspend fun getSettings(): SettingsEntity? = null

        override suspend fun insertSettings(settings: SettingsEntity) {
            operations += "insertSettings"
            savedSettings = settings
        }

        override suspend fun getWorkTypes(): List<WorkTypeEntity> = emptyList()

        override suspend fun insertWorkType(workType: WorkTypeEntity) {
            operations += "insertWorkType:${workType.workType}"
            insertedWorkTypes += workType
        }

        override suspend fun deleteWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun clearWorkTypes() {
            operations += "clearWorkTypes"
        }
    }
}

