package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkdayDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var workdayDao: WorkdayDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workdayDao = db.workdayDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun loadWorkday_matchesCompositeKey_dateAndProjectName() = runBlocking {
        workdayDao.insertWorkday(WorkdayEntity(date = "2026-04-10", projectName = "Alpha", workTimeToday = "07:00"))
        workdayDao.insertWorkday(WorkdayEntity(date = "2026-04-10", projectName = "Beta", workTimeToday = "05:00"))

        val alpha = workdayDao.loadWorkday("2026-04-10", "Alpha")
        val beta = workdayDao.loadWorkday("2026-04-10", "Beta")
        val missing = workdayDao.loadWorkday("2026-04-11", "Alpha")

        assertEquals("07:00", alpha?.workTimeToday)
        assertEquals("05:00", beta?.workTimeToday)
        assertNull(missing)
    }

    @Test
    fun getWorkdaysByDateRange_returnsOnlyInclusiveRange() = runBlocking {
        workdayDao.insertWorkday(WorkdayEntity(date = "2026-04-01", projectName = "Alpha", workTimeToday = "01:00"))
        workdayDao.insertWorkday(WorkdayEntity(date = "2026-04-10", projectName = "Alpha", workTimeToday = "02:00"))
        workdayDao.insertWorkday(WorkdayEntity(date = "2026-04-30", projectName = "Alpha", workTimeToday = "03:00"))
        workdayDao.insertWorkday(WorkdayEntity(date = "2026-05-01", projectName = "Alpha", workTimeToday = "04:00"))

        val result = workdayDao.getWorkdaysByDateRange("2026-04-01", "2026-04-30")

        assertEquals(3, result.size)
        assertTrue(result.any { it.date == "2026-04-01" })
        assertTrue(result.any { it.date == "2026-04-30" })
        assertTrue(result.none { it.date == "2026-05-01" })
    }
}

