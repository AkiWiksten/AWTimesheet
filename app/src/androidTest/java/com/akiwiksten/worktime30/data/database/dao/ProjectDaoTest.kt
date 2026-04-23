package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toProjectNameEntity
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var projectNameDao: ProjectNameDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDao = db.projectDao()
        projectNameDao = db.projectNameDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getProjectsByDateRange_returnsOnlyInclusiveRange() = runBlocking {
        projectNameDao.insertProjectName("Alpha".toProjectNameEntity())

        projectDao.insertProject(
            SingleProjectState(date = "2026-04-01", projectName = "Alpha", projectTime = "01:00").toEntity()
        )
        projectDao.insertProject(
            SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00").toEntity()
        )
        projectDao.insertProject(
            SingleProjectState(date = "2026-04-30", projectName = "Alpha", projectTime = "03:00").toEntity()
        )
        projectDao.insertProject(
            SingleProjectState(date = "2026-05-01", projectName = "Alpha", projectTime = "04:00").toEntity()
        )

        val result = projectDao.getProjectsByDateRange("2026-04-01", "2026-04-30")

        assertEquals(3, result.size)
        assertTrue(result.any { it.date == "2026-04-01" })
        assertTrue(result.any { it.date == "2026-04-30" })
        assertFalse(result.any { it.date == "2026-05-01" })
    }

    @Test
    fun getProjectsByDateRange_withSingleDayBounds_returnsOnlyThatDay() = runBlocking {
        projectNameDao.insertProjectName("Alpha".toProjectNameEntity())

        projectDao.insertProject(
            SingleProjectState(date = "2026-04-09", projectName = "Alpha", projectTime = "01:00").toEntity()
        )
        projectDao.insertProject(
            SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00").toEntity()
        )

        val result = projectDao.getProjectsByDateRange("2026-04-10", "2026-04-10")

        assertEquals(1, result.size)
        assertEquals("2026-04-10", result.first().date)
    }

    @Test
    fun isProjectNameUsed_reflectsRowsInProjectTable() = runBlocking {
        projectNameDao.insertProjectName("Alpha".toProjectNameEntity())

        assertFalse(projectDao.isProjectNameUsed("Alpha"))

        projectDao.insertProject(
            SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "02:00").toEntity()
        )

        assertTrue(projectDao.isProjectNameUsed("Alpha"))
        assertFalse(projectDao.isProjectNameUsed("Beta"))
    }
}
