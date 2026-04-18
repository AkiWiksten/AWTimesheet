package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDetailsDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var projectDetailsDao: ProjectDetailsDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        projectDetailsDao = db.projectDetailsDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun loadProjectDetails_matchesCompositeKey_dateAndProjectName() = runBlocking {
        projectDetailsDao.insertProjectDetails(
            ProjectDetailsEntity(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "07:00"
            )
        )
        projectDetailsDao.insertProjectDetails(
            ProjectDetailsEntity(
                date = "2026-04-10",
                projectName = "Beta",
                projectTime = "05:00"
            )
        )

        val alpha = projectDetailsDao.loadProjectDetails("2026-04-10", "Alpha")
        val beta = projectDetailsDao.loadProjectDetails("2026-04-10", "Beta")
        val missing = projectDetailsDao.loadProjectDetails("2026-04-11", "Alpha")

        assertEquals("07:00", alpha?.projectTime)
        assertEquals("05:00", beta?.projectTime)
        assertNull(missing)
    }

    @Test
    fun getProjectDetailsByDateRange_returnsOnlyInclusiveRange() = runBlocking {
        projectDetailsDao.insertProjectDetails(
            ProjectDetailsEntity(
                date = "2026-04-01",
                projectName = "Alpha",
                projectTime = "01:00"
            )
        )
        projectDetailsDao.insertProjectDetails(
            ProjectDetailsEntity(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "02:00"
            )
        )
        projectDetailsDao.insertProjectDetails(
            ProjectDetailsEntity(
                date = "2026-04-30",
                projectName = "Alpha",
                projectTime = "03:00"
            )
        )
        projectDetailsDao.insertProjectDetails(
            ProjectDetailsEntity(
                date = "2026-05-01",
                projectName = "Alpha",
                projectTime = "04:00"
            )
        )

        val result = projectDetailsDao.getProjectDetailsByDateRange("2026-04-01", "2026-04-30")

        assertEquals(3, result.size)
        assertTrue(result.any { it.date == "2026-04-01" })
        assertTrue(result.any { it.date == "2026-04-30" })
        assertTrue(result.any { it.date == "2026-04-10" })
        assertTrue(result.none { it.date == "2026-05-01" })
    }
}

