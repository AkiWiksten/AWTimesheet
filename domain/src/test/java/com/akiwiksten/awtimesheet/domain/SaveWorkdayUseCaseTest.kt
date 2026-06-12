package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectDetailsRepository
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveWorkdayUseCaseTest {

    @Test
    fun invoke_savesProjectsAndProjectDetails() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveWorkdayUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(
            projectToSave = projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "02:00"
            ),
            projectDetailsToSave = null
        )

        assertEquals(listOf("Alpha"), projectRepository.insertedProjectNames)
        assertEquals(1, projectRepository.insertedProjects.size)
        assertEquals("2026-04-10", workdayRepository.upsertedWorkdayDate)
    }

    @Test
    fun invoke_doesNotInsertProjectDetails_whenProjectDetailsIsNull() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveWorkdayUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(
            projectToSave = projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "01:00"
            ),
            projectDetailsToSave = null
        )

        assertEquals("2026-04-10", workdayRepository.upsertedWorkdayDate)
    }

    @Suppress("LongMethod")
    @Test
    fun invoke_absenceFlexDay_clearsOtherProjectsForSameDate() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            insertProject(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            )
            insertProject(
                projectState(
                    date = "2026-04-10",
                    projectName = "Beta",
                    projectTime = "01:30"
                )
            )
            insertProject(
                projectState(
                    date = "2026-04-11",
                    projectName = "Gamma",
                    projectTime = "03:00"
                )
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveWorkdayUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(
            projectToSave = projectState(
                date = "2026-04-10",
                projectName = "Absence-Flex day",
                projectTime = "07:30",
                workType = "Absence-Flex day"
            ),
            projectDetailsToSave = null,
            localizedFlexDayWorkType = "Absence-Flex day"
        )

        assertTrue(
            projectRepository.deletedProjects.any { it.date == "2026-04-10" && it.projectName == "Alpha" }
        )
        assertTrue(
            projectRepository.deletedProjects.any { it.date == "2026-04-10" && it.projectName == "Beta" }
        )
        assertTrue(
            projectRepository.deletedProjects.none { it.date == "2026-04-11" && it.projectName == "Gamma" }
        )
    }
}
