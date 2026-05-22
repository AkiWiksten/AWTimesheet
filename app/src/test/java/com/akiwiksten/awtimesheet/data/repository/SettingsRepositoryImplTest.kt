package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.data.database.entity.WorkdayEntity
import com.akiwiksten.awtimesheet.test.FakeCalculatedFlexTimeTotalDao
import com.akiwiksten.awtimesheet.test.FakeSettingsDao
import com.akiwiksten.awtimesheet.test.FakeWorkTypeDao
import com.akiwiksten.awtimesheet.test.FakeWorkdayDao
import com.akiwiksten.awtimesheet.test.settingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryImplTest {
    private val settingsDao = FakeSettingsDao()
    private val workdayDao = FakeWorkdayDao()
    private val workTypeDao = FakeWorkTypeDao()
    private val calculatedFlexTimeTotalDao = FakeCalculatedFlexTimeTotalDao()
    private val repository = SettingsRepositoryImpl(
        settingsDao = settingsDao,
        workdayDao = workdayDao,
        workTypeDao = workTypeDao,
        calculatedFlextimeTotalDao = calculatedFlexTimeTotalDao
    )

    @Test
    fun getSettings_returnsDataFromDao() = runBlocking {
        val expected = settingsState(name = "Aki", employer = "Company")
        settingsDao.settingsResult = expected

        val result = repository.getSettings()

        assertEquals(expected, result)
    }

    @Test
    fun insertSettings_callsDaoInsert() = runBlocking {
        val settings = settingsState(name = "Aki", employer = "Company")

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
    fun getCalculatedFlextimeTotal_withoutStoredRow_returnsZeroTime() = runBlocking {
        assertEquals(ZERO_TIME, repository.getCalculatedFlextimeTotal())
    }

    @Test
    fun insertCalculatedFlextimeTotal_callsDaoInsert() = runBlocking {
        repository.insertCalculatedFlextimeTotal("01:30")

        assertEquals("01:30", calculatedFlexTimeTotalDao.insertedFlexTime)
    }

    @Test
    fun getEffectiveSettingsForDate_withoutWorkday_returnsGlobalSettings() = runBlocking {
        settingsDao.settingsResult = settingsState(
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
        settingsDao.settingsResult = settingsState(dailyWorkTimeEstimate = "08:00")
        workdayDao.workdayResult = WorkdayEntity(
            date = "2026-04-10",
            workTimeByDateEstimate = "07:45"
        )

        val result = repository.getEffectiveSettingsForDate("2026-04-10")

        assertEquals("07:45", result?.dailyWorkTimeEstimate)
    }

    @Test
    fun getEffectiveSettingsForDate_withEmptyWorkdayEstimate_fallsBackToGlobalEstimate() = runBlocking {
        settingsDao.settingsResult = settingsState(dailyWorkTimeEstimate = "08:00")
        workdayDao.workdayResult = WorkdayEntity(
            date = "2026-04-10",
            workTimeByDateEstimate = ""
        )

        val result = repository.getEffectiveSettingsForDate("2026-04-10")

        assertEquals("08:00", result?.dailyWorkTimeEstimate)
    }
}
