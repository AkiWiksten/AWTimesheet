package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.junit.Test

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
        val alpha = ProjectDetailsState(
            date = "2026-04-10",
            projectName = "Alpha",
            projectTime = "07:00"
        )
        val beta = ProjectDetailsState(
            date = "2026-04-10",
            projectName = "Beta",
            projectTime = "05:00"
        )

        projectDetailsDao.insertProjectDetails(
            alpha.toEntity()
        )
        projectDetailsDao.insertProjectDetails(
            beta.toEntity()
        )

        val loadedAlpha = projectDetailsDao.loadProjectDetails("2026-04-10", "Alpha")?.toDomain()
        val loadedBeta = projectDetailsDao.loadProjectDetails("2026-04-10", "Beta")?.toDomain()
        val missing = projectDetailsDao.loadProjectDetails("2026-04-11", "Alpha")?.toDomain()

        assertEquals("07:00", loadedAlpha?.projectTime)
        assertEquals("05:00", loadedBeta?.projectTime)
        assertNull(missing)
    }

    @Test
    fun getProjectDetailsByDateRange_returnsOnlyInclusiveRange() = runBlocking {
        val first = ProjectDetailsState(
            date = "2026-04-01",
            projectName = "Alpha",
            projectTime = "01:00"
        )
        val inside = ProjectDetailsState(
            date = "2026-04-10",
            projectName = "Alpha",
            projectTime = "02:00"
        )
        val last = ProjectDetailsState(
            date = "2026-04-30",
            projectName = "Alpha",
            projectTime = "03:00"
        )
        val outside = ProjectDetailsState(
            date = "2026-05-01",
            projectName = "Alpha",
            projectTime = "04:00"
        )

        projectDetailsDao.insertProjectDetails(
            first.toEntity()
        )
        projectDetailsDao.insertProjectDetails(
            inside.toEntity()
        )
        projectDetailsDao.insertProjectDetails(
            last.toEntity()
        )
        projectDetailsDao.insertProjectDetails(
            outside.toEntity()
        )

        val result = projectDetailsDao
            .getProjectDetailsByDateRange("2026-04-01", "2026-04-30")
            .map { it.toDomain() }

        assertEquals(3, result.size)
        assertTrue(result.any { it.date == "2026-04-01" })
        assertTrue(result.any { it.date == "2026-04-30" })
        assertTrue(result.any { it.date == "2026-04-10" })
        assertTrue(result.none { it.date == "2026-05-01" })
    }
}

