package com.akiwiksten.awtimesheet.feature.timesheet.mapper

import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetAllowanceType
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetDisplayData
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetEntry
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetEntryAggregates
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetExportData
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.buildAllowanceRows
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections.buildWorkTypeRows
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.MAX_SUMMARY_PROJECTS
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toMinutesOrNull
import java.time.LocalDate

internal object TimesheetExportDataBuilder {
    fun build(params: GenerateTimesheetParams): TimesheetExportData {
        val labels = params.toTimesheetLabels()
        val endDate = LocalDate.parse(params.endOfMonthDate)
        val startDate = endDate.withDayOfMonth(1)
        val entries = params.projectsByMonth.toSortedTimesheetEntries(labels)

        val allProjectNames = entries.allDistinctProjectNames()
        val summaryProjectNames = allProjectNames.take(MAX_SUMMARY_PROJECTS)
        val displayData = createDisplayData(entries)
        val aggregates = aggregateEntries(entries, allProjectNames)

        val allowanceRows = buildAllowanceRows(
            allProjectNames = allProjectNames,
            labels = labels,
            allowanceCountsByProjectAndType = aggregates.allowanceCountsByProjectAndType,
            allowanceTotalCountsByType = aggregates.allowanceTotalCountsByType
        )
        val workTypeRows = buildWorkTypeRows(
            allProjectNames = allProjectNames,
            workTypeTimeByProjectAndType = aggregates.workTypeTimeByProjectAndType,
            workTypeTotalTime = aggregates.workTypeTotalTime,
            displayedEntriesByDay = displayData.displayedEntriesByDay
        )

        return TimesheetExportData(
            name = params.name,
            employer = params.employer,
            startDate = startDate,
            endDate = endDate,
            totalLabel = params.totalLabel,
            generalLabel = params.generalLabel,
            workTimeTotalLabel = params.workTimeTotalLabel,
            kilometresLabel = params.kilometresLabel,
            summaryProjectNames = summaryProjectNames,
            summaryProjectTimes = aggregates.summaryProjectTimes,
            summaryProjectKilometres = aggregates.summaryProjectKilometres,
            totalWorkTime = aggregates.totalWorkTime,
            totalKilometres = aggregates.totalKilometres,
            dayOfMonthLabel = labels.dayOfMonthLabel,
            projectNameLabel = labels.projectNameLabel,
            workTimeByDateLabel = labels.workTimeByDateLabel,
            allowanceLabel = labels.allowanceLabel,
            workTypeLabel = labels.workTypeLabel,
            employerLabel = labels.employerLabel,
            nameLabel = labels.nameLabel,
            totalSumLabel = labels.totalSumLabel,
            startDateLabel = labels.startDateLabel,
            titleLabel = labels.titleLabel,
            endDateLabel = labels.endDateLabel,
            projectTimeLabel = labels.projectTimeLabel,
            allowanceRows = allowanceRows,
            workTypeRows = workTypeRows,
            displayedEntriesByDay = displayData.displayedEntriesByDay,
            overflowedDays = displayData.overflowedDays,
            hiddenProjectNames = allProjectNames.drop(MAX_SUMMARY_PROJECTS),
            hiddenWorkTypes = emptyList(),
            flexTimeTotalLabel = params.flexTimeTotalLabel,
            totalFlexTimeTotal = params.totalFlexTimeTotal
        )
    }

    private fun createDisplayData(entries: List<TimesheetEntry>): TimesheetDisplayData {
        val entriesByDay = entries.groupBy { it.dayOfMonth }
        return TimesheetDisplayData(
            displayedEntriesByDay = entriesByDay,
            overflowedDays = emptyList()
        )
    }

    private fun aggregateEntries(
        entries: List<TimesheetEntry>,
        allProjectNames: List<String>
    ): TimesheetEntryAggregates {
        val summaryProjectTimes = allProjectNames.associateWith { 0L }.toMutableMap()
        val summaryProjectKilometres = allProjectNames.associateWith { 0L }.toMutableMap()
        val allowanceCountsByProjectAndType = mutableMapOf<Pair<String, TimesheetAllowanceType>, Int>()
        val allowanceTotalCountsByType = mutableMapOf<TimesheetAllowanceType, Int>()
        val workTypeTimeByProjectAndType = mutableMapOf<Pair<String, String>, Long>()
        val workTypeTotalTime = mutableMapOf<String, Long>()
        var totalWorkTime = 0L
        var totalKilometres = 0L

        entries.forEach { entry ->
            val projectTimeMinutes = entry.projectTime.toMinutesOrNull()
            if (projectTimeMinutes != null) {
                val kilometres = entry.kilometres.toLongOrNull() ?: 0L
                if (entry.projectName in summaryProjectTimes) {
                    summaryProjectTimes.compute(entry.projectName) { _, current ->
                        (current ?: 0L) + projectTimeMinutes
                    }
                    summaryProjectKilometres.compute(entry.projectName) { _, current ->
                        (current ?: 0L) + kilometres
                    }
                }
                allowanceCountsByProjectAndType.compute(entry.projectName to entry.allowanceType) { _, current ->
                    (current ?: 0) + 1
                }
                allowanceTotalCountsByType.compute(entry.allowanceType) { _, current ->
                    (current ?: 0) + 1
                }
                workTypeTimeByProjectAndType.compute(entry.projectName to entry.workType) { _, current ->
                    (current ?: 0L) + projectTimeMinutes
                }
                workTypeTotalTime.compute(entry.workType) { _, current ->
                    (current ?: 0L) + projectTimeMinutes
                }
                totalWorkTime += projectTimeMinutes
                totalKilometres += kilometres
            }
        }

        return TimesheetEntryAggregates(
            summaryProjectTimes = summaryProjectTimes,
            summaryProjectKilometres = summaryProjectKilometres,
            allowanceCountsByProjectAndType = allowanceCountsByProjectAndType,
            allowanceTotalCountsByType = allowanceTotalCountsByType,
            workTypeTimeByProjectAndType = workTypeTimeByProjectAndType,
            workTypeTotalTime = workTypeTotalTime,
            totalWorkTime = totalWorkTime,
            totalKilometres = totalKilometres
        )
    }
}
