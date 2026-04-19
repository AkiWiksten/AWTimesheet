package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.feature.settings.SettingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {
    private val settingsDao = FakeSettingsDao()
    private val workTypeDao = FakeWorkTypeDao()
    private val repository = SettingsRepositoryImpl(settingsDao, workTypeDao)

    @Test
    fun getSettings_returnsDataFromDao() = runBlocking {
        val expected = SettingsEntity(name = "Aki", employer = "Company")
        settingsDao.settingsResult = expected

        val result = repository.getSettings()

        assertEquals(expected.toDomain(), result)
    }

    @Test
    fun insertSettings_callsDaoInsert() = runBlocking {
        val settings = SettingsState(name = "Aki", employer = "Company")

        repository.insertSettings(settings)

        assertEquals(settings.toEntity(), settingsDao.insertedSettings)
    }

    @Test
    fun getWorkTypes_returnsDataFromDao() = runBlocking {
        val expected = listOf(WorkTypeEntity(workType = "Office"), WorkTypeEntity(workType = "Remote"))
        workTypeDao.workTypesResult = expected

        val result = repository.getWorkTypes()

        assertEquals(expected, result)
    }

    @Test
    fun insertWorkType_callsDaoInsert() = runBlocking {
        val workType = WorkTypeEntity(workType = "Office")

        repository.insertWorkType(workType)

        assertEquals(workType, workTypeDao.insertedWorkType)
    }

    @Test
    fun deleteWorkType_callsDaoDelete() = runBlocking {
        val workType = WorkTypeEntity(workType = "Office")

        repository.deleteWorkType(workType)

        assertEquals(workType, workTypeDao.deletedWorkType)
    }

    @Test
    fun clearWorkTypes_callsDaoDeleteAll() = runBlocking {
        repository.clearWorkTypes()

        assertEquals(1, workTypeDao.deleteAllCallCount)
    }

    private class FakeSettingsDao : SettingsDao {
        var settingsResult: SettingsEntity? = null
        var insertedSettings: SettingsEntity? = null

        override suspend fun anyRecord(): Boolean = false

        override suspend fun insertSettings(settings: SettingsEntity) {
            insertedSettings = settings
        }

        override suspend fun loadSettings(): SettingsEntity? = settingsResult
    }

    private class FakeWorkTypeDao : WorkTypeDao {
        var workTypesResult: List<WorkTypeEntity> = emptyList()
        var insertedWorkType: WorkTypeEntity? = null
        var deletedWorkType: WorkTypeEntity? = null
        var deleteAllCallCount: Int = 0

        override suspend fun anyRecords(): Boolean = false

        override suspend fun insertWorkType(workType: WorkTypeEntity) {
            insertedWorkType = workType
        }

        override suspend fun loadWorkTypes(): List<WorkTypeEntity> = workTypesResult

        override suspend fun delete(workType: WorkTypeEntity) {
            deletedWorkType = workType
        }

        override suspend fun deleteAll() {
            deleteAllCallCount += 1
        }
    }
}
