package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.domain.model.SettingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {
    private val settingsDao = FakeSettingsDao()
    private val workdayDao = FakeWorkdayDao()
    private val workTypeDao = FakeWorkTypeDao()
    private val repository = SettingsRepositoryImpl(settingsDao, workdayDao, workTypeDao)

    @Test
    fun getSettings_returnsDataFromDao() = runBlocking {
        val expected = SettingsState(name = "Aki", employer = "Company")
        settingsDao.settingsResult = expected

        val result = repository.getSettings()

        assertEquals(expected, result)
    }

    @Test
    fun insertSettings_callsDaoInsert() = runBlocking {
        val settings = SettingsState(name = "Aki", employer = "Company")

        repository.insertSettings(settings)

        assertEquals(settings, settingsDao.insertedSettings)
    }

    @Test
    fun getWorkTypes_returnsDataFromDao() = runBlocking {
        val expected = listOf("Office", "Remote")
        workTypeDao.workTypesResult = expected

        val result = repository.getWorkTypes()

        assertEquals(expected, result)
    }

    @Test
    fun insertWorkType_callsDaoInsert() = runBlocking {
        val workType = "Office"

        repository.insertWorkType(workType)

        assertEquals(workType, workTypeDao.insertedWorkType)
    }

    @Test
    fun deleteWorkType_callsDaoDelete() = runBlocking {
        val workType = "Office"

        repository.deleteWorkType(workType)

        assertEquals(workType, workTypeDao.deletedWorkType)
    }

    @Test
    fun deleteAllWorkTypes_callsDaoDeleteAllWorkTypes() = runBlocking {
        repository.deleteAllWorkTypes()

        assertEquals(1, workTypeDao.deleteAllCallCount)
    }

    @Test
    fun getEffectiveSettingsForDate_withoutWorkday_returnsGlobalSettings() = runBlocking {
        settingsDao.settingsResult = SettingsState(
            dailyWorkTimeEstimate = "08:00",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:00"
        )
        workdayDao.workdayResult = null

        val result = repository.getEffectiveSettingsForDate("2026-04-10")

        assertEquals("08:00", result?.dailyWorkTimeEstimate)
        assertEquals("00:30", result?.dailyLunchTimeEstimate)
        assertEquals("+01:00", result?.initialFlexTimeTotal)
    }

    @Test
    fun getEffectiveSettingsForDate_withWorkdayOverride_returnsPerDayEstimate() = runBlocking {
        settingsDao.settingsResult = SettingsState(dailyWorkTimeEstimate = "08:00")
        workdayDao.workdayResult = WorkdayEntity(
            date = "2026-04-10",
            workTimeTodayEstimate = "07:45"
        )

        val result = repository.getEffectiveSettingsForDate("2026-04-10")

        assertEquals("07:45", result?.dailyWorkTimeEstimate)
    }

    @Test
    fun getEffectiveSettingsForDate_withEmptyWorkdayEstimate_fallsBackToGlobalEstimate() = runBlocking {
        settingsDao.settingsResult = SettingsState(dailyWorkTimeEstimate = "08:00")
        workdayDao.workdayResult = WorkdayEntity(
            date = "2026-04-10",
            workTimeTodayEstimate = ""
        )

        val result = repository.getEffectiveSettingsForDate("2026-04-10")

        assertEquals("08:00", result?.dailyWorkTimeEstimate)
    }

    private class FakeSettingsDao : SettingsDao {
        var settingsResult: SettingsState? = null
        var insertedSettings: SettingsState? = null

        override suspend fun anyRecord(): Boolean = false

        override suspend fun insertSettings(settings: com.akiwiksten.worktime30.data.database.entity.SettingsEntity) {
            insertedSettings = settings.toDomain()
        }

        override suspend fun loadSettings(): com.akiwiksten.worktime30.data.database.entity.SettingsEntity? =
            settingsResult?.toEntity()
    }

    private class FakeWorkTypeDao : WorkTypeDao {
        var workTypesResult: List<String> = emptyList()
        var insertedWorkType: String? = null
        var deletedWorkType: String? = null
        var deleteAllCallCount: Int = 0

        override suspend fun anyRecords(): Boolean = false

        override suspend fun insertWorkType(workType: com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity) {
            insertedWorkType = workType.workType
        }

        override suspend fun loadWorkTypes(): List<com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity> =
            workTypesResult.map { com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity(workType = it) }

        override suspend fun delete(workType: com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity) {
            deletedWorkType = workType.workType
        }

        override suspend fun deleteAllWorkTypes() {
            deleteAllCallCount += 1
        }
    }

    private class FakeWorkdayDao : WorkdayDao {
        var workdayResult: WorkdayEntity? = null

        override suspend fun loadWorkday(date: String): WorkdayEntity? = workdayResult

        override suspend fun insertWorkday(workday: WorkdayEntity) = Unit

        override suspend fun getWorkdaysByDateRange(
            start: String,
            end: String
        ): List<com.akiwiksten.worktime30.data.database.entity.WorkdayEntity> = emptyList()
    }
}
