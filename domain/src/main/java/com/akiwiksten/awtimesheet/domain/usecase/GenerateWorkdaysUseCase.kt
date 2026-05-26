package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.random.Random
import javax.inject.Inject

enum class WorkdayGenerationScope {
    MONTH,
    YEAR
}

enum class WorkdayGenerationMode {
    INSERT_MISSING,
    UPSERT_ALL_WEEKDAYS
}

data class WorkdayGenerationResult(
    val insertedWorkdays: Int,
    val updatedWorkdays: Int,
    val weekdayCandidates: Int,
    val startDate: String,
    val endDate: String
)

data class GeneratedAllowanceLabels(
    val noAllowance: String,
    val fullAllowance: String,
    val halfDayAllowance: String
)

internal val GENERATED_WORKDAY_PROJECT_NAMES = listOf(
    "Car",
    "Roadworks",
    "Office",
    "Client A",
    "Client B",
    "Maintenance",
    "Research",
    "Planning",
    "Training",
    "Support"
)
internal val GENERATED_WORKDAY_PROJECT_WORK_TYPES = listOf(
    "Other",
    "Design",
    "Implementation",
    "Testing",
    "Meeting",
    "Exercise",
    "Architecture",
    "Review"
)
internal val GENERATED_WORKDAY_PROJECT_TIMES = listOf(
    "00:30",
    "01:00",
    "01:30",
    "02:00",
    "02:30",
    "03:00",
    "03:30",
    "04:00"
)
internal val GENERATED_WORKDAY_ALLOWANCES = listOf(
    "No allowance",
    "Full allowance",
    "Half-day allowance"
)
private const val GENERATED_WORKDAY_ESTIMATE = "07:30"
private const val GENERATED_PROJECTS_PER_DAY_VARIATION = 3
private const val GENERATED_PROJECTS_PER_DAY_MIN = 1
private const val GENERATED_TIME_OFFSET_DIVISOR = 10
private const val GENERATED_PROJECTS_PER_DAY_MAX =
    GENERATED_PROJECTS_PER_DAY_VARIATION + GENERATED_PROJECTS_PER_DAY_MIN - 1
private const val GENERATED_KILOMETRES_MIN = 0
private const val GENERATED_KILOMETRES_MAX = 200

class GenerateWorkdaysUseCase @Inject constructor(
    private val workdayRepository: WorkdayRepository,
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository
) {
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

    suspend operator fun invoke(
        selectedDate: String,
        scope: WorkdayGenerationScope,
        mode: WorkdayGenerationMode = WorkdayGenerationMode.INSERT_MISSING,
        allowanceLabels: GeneratedAllowanceLabels = GeneratedAllowanceLabels(
            noAllowance = GENERATED_WORKDAY_ALLOWANCES[0],
            fullAllowance = GENERATED_WORKDAY_ALLOWANCES[1],
            halfDayAllowance = GENERATED_WORKDAY_ALLOWANCES[2]
        )
    ): WorkdayGenerationResult {
        require(selectedDate.isNotBlank()) { "Selected date is required." }

        val baseDate = LocalDate.parse(selectedDate, dateFormatter)
        val range = resolveRange(baseDate, scope)

        var insertedWorkdays = 0
        var updatedWorkdays = 0
        var weekdayCandidates = 0
        var workTypeCursor = baseDate.hashCode().absoluteValue % GENERATED_WORKDAY_PROJECT_WORK_TYPES.size

        var day = range.first
        while (!day.isAfter(range.second)) {
            if (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) {
                day = day.plusDays(1)
                continue
            }
            weekdayCandidates += 1

            val date = day.format(dateFormatter)
            val estimate = GENERATED_WORKDAY_ESTIMATE

            when (mode) {
                WorkdayGenerationMode.INSERT_MISSING -> {
                    if (workdayRepository.ensureWorkdayStats(date, estimate)) {
                        insertedWorkdays += 1
                    }
                }

                WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS -> {
                    val existed = workdayRepository.loadWorkday(date) != null
                    workdayRepository.upsertWorkdayStats(date, estimate)
                    if (existed) {
                        updatedWorkdays += 1
                    } else {
                        insertedWorkdays += 1
                    }
                }
            }

            workTypeCursor = syncGeneratedProjects(
                date = date,
                mode = mode,
                allowanceLabels = allowanceLabels,
                workTypeCursor = workTypeCursor
            )

            day = day.plusDays(1)
        }

        return WorkdayGenerationResult(
            insertedWorkdays = insertedWorkdays,
            updatedWorkdays = updatedWorkdays,
            weekdayCandidates = weekdayCandidates,
            startDate = range.first.format(dateFormatter),
            endDate = range.second.format(dateFormatter)
        )
    }

    private fun resolveRange(baseDate: LocalDate, scope: WorkdayGenerationScope): Pair<LocalDate, LocalDate> {
        return when (scope) {
            WorkdayGenerationScope.MONTH -> {
                val start = baseDate.withDayOfMonth(1)
                start to start.withDayOfMonth(start.month.length(start.isLeapYear))
            }

            WorkdayGenerationScope.YEAR -> {
                val start = baseDate.withDayOfYear(1)
                start to start.withDayOfYear(start.lengthOfYear())
            }
        }
    }


    private suspend fun syncGeneratedProjects(
        date: String,
        mode: WorkdayGenerationMode,
        allowanceLabels: GeneratedAllowanceLabels,
        workTypeCursor: Int
    ): Int {
        val projectsForDate = projectRepository.getProjectsByDateRange(date, date)
        val generatedProjects = projectsForDate.filter { project ->
            isGeneratedProject(project = project, allowanceLabels = allowanceLabels)
        }
        val hasRealProjects = projectsForDate.any { project ->
            !isGeneratedProject(project = project, allowanceLabels = allowanceLabels)
        }
        val oldGeneratedFlexTime = calculateGeneratedFlexTime(generatedProjects)

        if (hasRealProjects) {
            generatedProjects.forEach { projectRepository.deleteProject(it) }
            updateCalculatedFlexTimeTotal(
                WorkTimeCalculator.calculateFlexTime(
                    initialTime = ZERO_TIME,
                    addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$oldGeneratedFlexTime")
                )
            )
            return workTypeCursor
        }


        if (mode == WorkdayGenerationMode.UPSERT_ALL_WEEKDAYS) {
            generatedProjects.forEach { projectRepository.deleteProject(it) }
        }

        val generatedForDate = buildGeneratedProjectsForDate(
            date = date,
            allowanceLabels = allowanceLabels,
            workTypeCursor = workTypeCursor
        )
        val newGeneratedProjects = generatedForDate.projects
        newGeneratedProjects.forEach { project ->
            projectRepository.insertProjectName(project.projectName)
            projectRepository.insertProject(project)
        }

        val newGeneratedFlexTime = calculateGeneratedFlexTime(newGeneratedProjects)
        updateCalculatedFlexTimeTotal(
            WorkTimeCalculator.calculateFlexTime(
                initialTime = newGeneratedFlexTime,
                addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$oldGeneratedFlexTime")
            )
        )

        return generatedForDate.nextWorkTypeCursor
    }

    private suspend fun updateCalculatedFlexTimeTotal(delta: String) {
        if (delta == ZERO_TIME) return

        val updatedCalculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
            initialTime = settingsRepository.getCalculatedFlextimeTotal(),
            addedTime = delta
        )
        settingsRepository.insertCalculatedFlextimeTotal(updatedCalculatedFlexTimeTotal)
    }

    private fun calculateGeneratedFlexTime(projects: List<SingleProjectState>): String {
        val generatedWorkTime = projects.fold(ZERO_TIME) { total, project ->
            WorkTimeCalculator.calculateFlexTime(total, project.projectTime)
        }
        return if (generatedWorkTime == ZERO_TIME) {
            ZERO_TIME
        } else {
            WorkTimeCalculator.calculateFlexTime(
                initialTime = generatedWorkTime,
                addedTime = "-$GENERATED_WORKDAY_ESTIMATE"
            )
        }
    }

    private fun buildGeneratedProjectsForDate(
        date: String,
        allowanceLabels: GeneratedAllowanceLabels,
        workTypeCursor: Int
    ): GeneratedProjectsForDate {
        val seed = date.hashCode().absoluteValue
        val requestedCount =
            (seed % GENERATED_PROJECTS_PER_DAY_VARIATION) +
                    GENERATED_PROJECTS_PER_DAY_MIN // 1..3 generated projects per weekday
        val count = requestedCount.coerceAtMost(
            GENERATED_PROJECTS_PER_DAY_MAX.coerceAtMost(GENERATED_WORKDAY_PROJECT_NAMES.size)
        )

        val nameOffset = seed % GENERATED_WORKDAY_PROJECT_NAMES.size
        val timeOffset = (seed / GENERATED_TIME_OFFSET_DIVISOR) % GENERATED_WORKDAY_PROJECT_TIMES.size
        val random = Random(seed)
        val namesForDate = (0 until count).map { index ->
            GENERATED_WORKDAY_PROJECT_NAMES[(nameOffset + index) % GENERATED_WORKDAY_PROJECT_NAMES.size]
        }
        val allowancesForDate = listOf(
            allowanceLabels.noAllowance,
            allowanceLabels.fullAllowance,
            allowanceLabels.halfDayAllowance
        )

        val projectsForDate = namesForDate.mapIndexed { index, projectName ->
            val projectTime = GENERATED_WORKDAY_PROJECT_TIMES[
                (timeOffset + index) % GENERATED_WORKDAY_PROJECT_TIMES.size
            ]
            val kilometres = random.nextInt(
                from = GENERATED_KILOMETRES_MIN,
                until = GENERATED_KILOMETRES_MAX + 1
            ).toString()
            val allowance = allowancesForDate[random.nextInt(allowancesForDate.size)]
            val workType = GENERATED_WORKDAY_PROJECT_WORK_TYPES[
                (workTypeCursor + index) % GENERATED_WORKDAY_PROJECT_WORK_TYPES.size
            ]
            SingleProjectState(
                date = date,
                projectName = projectName,
                projectTime = projectTime,
                kilometres = kilometres,
                allowance = allowance,
                workType = workType
            )
        }

        return GeneratedProjectsForDate(
            projects = projectsForDate,
            nextWorkTypeCursor =
                (workTypeCursor + projectsForDate.size) % GENERATED_WORKDAY_PROJECT_WORK_TYPES.size
        )
    }

    private fun isGeneratedProject(
        project: SingleProjectState,
        allowanceLabels: GeneratedAllowanceLabels
    ): Boolean {
        val allowancesForDate = setOf(
            allowanceLabels.noAllowance,
            allowanceLabels.fullAllowance,
            allowanceLabels.halfDayAllowance
        )

        return project.projectName in GENERATED_WORKDAY_PROJECT_NAMES &&
            project.projectTime in GENERATED_WORKDAY_PROJECT_TIMES &&
            project.allowance in allowancesForDate &&
            project.workType in GENERATED_WORKDAY_PROJECT_WORK_TYPES &&
            (project.kilometres.toIntOrNull() ?: -1) in GENERATED_KILOMETRES_MIN..GENERATED_KILOMETRES_MAX
    }

    private data class GeneratedProjectsForDate(
        val projects: List<SingleProjectState>,
        val nextWorkTypeCursor: Int
    )
}



