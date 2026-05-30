package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.usecase.GENERATED_WORKDAY_ALLOWANCES
import com.akiwiksten.awtimesheet.domain.usecase.GENERATED_WORKDAY_PROJECT_NAMES
import com.akiwiksten.awtimesheet.domain.usecase.GENERATED_WORKDAY_PROJECT_TIMES
import com.akiwiksten.awtimesheet.domain.usecase.GENERATED_WORKDAY_PROJECT_WORK_TYPES
import com.akiwiksten.awtimesheet.domain.usecase.GenerateWorkdaysUseCase
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationMode
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationScope
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateWorkdaysUseCaseTest {

    @Test
    fun invoke_insertMissing_insertsVariedGeneratedProjectsForMonth() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val settingsRepository = FakeSettingsRepository()
        val workdayRepository = FakeWorkdayRepository()
        val useCase = GenerateWorkdaysUseCase(
            workdayRepository = workdayRepository,
            settingsRepository = settingsRepository,
            projectRepository = projectRepository
        )

        useCase(
            selectedDate = "2026-02-10",
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.INSERT_MISSING
        )

        assertTrue(settingsRepository.workTypes.containsAll(GENERATED_WORKDAY_PROJECT_WORK_TYPES))

        val allProjects = projectRepository.getProjectsByDateRange("2026-02-01", "2026-02-28")
        assertTrue(allProjects.isNotEmpty())
        assertTrue(allProjects.all { it.workType in GENERATED_WORKDAY_PROJECT_WORK_TYPES })
        assertTrue(allProjects.all { it.projectName in GENERATED_WORKDAY_PROJECT_NAMES })
        assertTrue(allProjects.all { it.projectTime in GENERATED_WORKDAY_PROJECT_TIMES })
        assertTrue(allProjects.all { it.allowance in GENERATED_WORKDAY_ALLOWANCES })
        assertTrue(allProjects.all { (it.kilometres.toIntOrNull() ?: -1) in 0..200 })
        val workTypeCounts = allProjects.groupingBy { it.workType }.eachCount()
        assertTrue(workTypeCounts.keys.containsAll(GENERATED_WORKDAY_PROJECT_WORK_TYPES))
        assertTrue(
            (workTypeCounts.maxOf { it.value } - workTypeCounts.minOf { it.value }) <= 1
        )
        allProjects.groupBy { it.date }.values.forEach { dayProjects ->
            assertEquals(dayProjects.size, dayProjects.map { it.projectName }.distinct().size)
            val generatedTotalMinutes = dayProjects.sumOf { project ->
                timeToMinutes(project.projectTime)
            }
            assertTrue(generatedTotalMinutes >= timeToMinutes("06:00"))
        }
        assertTrue(workdayRepository.workdayStatsRows.all { it.workTimeByDateEstimate == "07:30" })
        assertEquals(
            expectedGeneratedFlexTime(allProjects),
            settingsRepository.calculatedFlexTimeTotal
        )

        useCase(
            selectedDate = "2026-02-10",
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.INSERT_MISSING
        )

        assertEquals(
            allProjects.size,
            projectRepository.getProjectsByDateRange("2026-02-01", "2026-02-28").size
        )
        assertEquals(
            GENERATED_WORKDAY_PROJECT_WORK_TYPES.size,
            settingsRepository.insertedWorkTypeCalls.size
        )

        val distinctDatesWithProjects = allProjects.map { it.date }.distinct()
        assertEquals(
            20,
            distinctDatesWithProjects.size
        ) // February 2026 has 20 weekdays; generator now guarantees 1..3 projects/weekday.
    }

    @Test
    fun invoke_insertMissing_skipsGenerationForDateWhenRealProjectExists() = runBlocking {
        val projectRepository = FakeProjectRepository()
        projectRepository.insertProject(
            projectState(
                date = "2026-02-10",
                projectName = "Alpha",
                projectTime = "04:00"
            )
        )

        val useCase = GenerateWorkdaysUseCase(
            workdayRepository = FakeWorkdayRepository(),
            settingsRepository = FakeSettingsRepository(),
            projectRepository = projectRepository
        )

        useCase(
            selectedDate = "2026-02-10",
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.INSERT_MISSING
        )

        val projectsForDate = projectRepository.getProjectsByDateRange("2026-02-10", "2026-02-10")
        assertEquals(listOf("Alpha"), projectsForDate.map { it.projectName })
        assertFalse(projectsForDate.any { it.projectName in GENERATED_WORKDAY_PROJECT_NAMES })
    }

    @Test
    fun invoke_upsertAllWeekdays_replacesLegacyGeneratedRowsForDate() = runBlocking {
        val projectRepository = FakeProjectRepository()
        projectRepository.insertProject(
            projectState(
                date = "2026-02-10",
                projectName = "Car",
                projectTime = "01:00",
                kilometres = "10",
                allowance = "No allowance",
                workType = "Design"
            )
        )

        val useCase = GenerateWorkdaysUseCase(
            workdayRepository = FakeWorkdayRepository(),
            settingsRepository = FakeSettingsRepository(),
            projectRepository = projectRepository
        )

        useCase(
            selectedDate = "2026-02-10",
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS
        )

        val projectsForDate = projectRepository.getProjectsByDateRange("2026-02-10", "2026-02-10")
        assertFalse(
            projectsForDate.any {
                it.projectName == "Car" &&
                    it.projectTime == "01:00" &&
                    it.workType == "Design"
            }
        )
        assertTrue(projectsForDate.all { it.workType in GENERATED_WORKDAY_PROJECT_WORK_TYPES })
        assertTrue(projectsForDate.all { it.projectName in GENERATED_WORKDAY_PROJECT_NAMES })
    }

    @Test
    fun invoke_insertsGeneratedWorkTypesOnlyWhenMissing() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf(GENERATED_WORKDAY_PROJECT_WORK_TYPES.first(), "Legacy")
        }
        val useCase = GenerateWorkdaysUseCase(
            workdayRepository = FakeWorkdayRepository(),
            settingsRepository = settingsRepository,
            projectRepository = projectRepository
        )

        useCase(
            selectedDate = "2026-02-10",
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.INSERT_MISSING
        )

        assertTrue(settingsRepository.workTypes.containsAll(GENERATED_WORKDAY_PROJECT_WORK_TYPES))
        assertTrue(settingsRepository.workTypes.contains("Legacy"))
        assertEquals(
            GENERATED_WORKDAY_PROJECT_WORK_TYPES.drop(1).size,
            settingsRepository.insertedWorkTypeCalls.size
        )
    }

    @Test
    fun invoke_removesGeneratedRowsWhenRealProjectAlreadyExistsForDate() = runBlocking {
        val projectRepository = FakeProjectRepository()
        projectRepository.insertProject(
            projectState(
                date = "2026-02-10",
                projectName = "Car",
                projectTime = "01:00",
                kilometres = "10",
                allowance = "No allowance",
                workType = "Design"
            )
        )
        projectRepository.insertProject(
            projectState(
                date = "2026-02-10",
                projectName = "Alpha",
                projectTime = "02:00"
            )
        )

        val useCase = GenerateWorkdaysUseCase(
            workdayRepository = FakeWorkdayRepository(),
            settingsRepository = FakeSettingsRepository(),
            projectRepository = projectRepository
        )

        useCase(
            selectedDate = "2026-02-10",
            scope = WorkdayGenerationScope.MONTH,
            mode = WorkdayGenerationMode.INSERT_MISSING
        )

        val projectsForDate = projectRepository.getProjectsByDateRange("2026-02-10", "2026-02-10")
        assertEquals(listOf("Alpha"), projectsForDate.map { it.projectName })
        val deletedGeneratedForDate = projectRepository.deletedProjects.any { deleted ->
            deleted.date == "2026-02-10" &&
                deleted.projectName == "Car" &&
                deleted.projectTime == "01:00" &&
                deleted.workType == "Design"
        }
        assertTrue(
            deletedGeneratedForDate
        )
    }

    private fun expectedGeneratedFlexTime(projects: List<SingleProjectState>): String {
        val byDate = projects.groupBy { it.date }
        return byDate.values.fold(ZERO_TIME) { total, dayProjects ->
            val workTimeByDate = dayProjects.fold(ZERO_TIME) { dayTotal, project ->
                WorkTimeCalculator.calculateFlexTime(dayTotal, project.projectTime)
            }
            val dayFlexTime = if (workTimeByDate == ZERO_TIME) {
                ZERO_TIME
            } else {
                WorkTimeCalculator.calculateFlexTime(workTimeByDate, "-07:30")
            }
            WorkTimeCalculator.calculateFlexTime(total, dayFlexTime)
        }
    }

    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return (parts[0].toInt() * 60) + parts[1].toInt()
    }
}

