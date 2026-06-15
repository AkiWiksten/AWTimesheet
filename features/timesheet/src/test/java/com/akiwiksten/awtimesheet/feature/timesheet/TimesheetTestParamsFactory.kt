package com.akiwiksten.awtimesheet.feature.timesheet

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import org.robolectric.RuntimeEnvironment

internal object TimesheetTestParamsFactory {
    fun create(
        projects: List<SingleProjectState>,
        totalFlexTimeTotal: String = "00:00"
    ): GenerateTimesheetParams {
        val ctx = RuntimeEnvironment.getApplication()
        return GenerateTimesheetParams(
            ctx = ctx,
            projectsByMonth = projects,
            endOfMonthDate = "2026-05-31",
            name = "Aki Wiksten",
            employer = "AJVW Inc.",
            defaultWorkTypeLabel = "Other",
            noAllowanceSourceLabel = "No allowance",
            halfDayAllowanceSourceLabel = "Half-day allowance",
            fullAllowanceSourceLabel = "Full allowance",
            noAllowanceExportLabel = "No",
            halfDayAllowanceExportLabel = "Half-day",
            fullAllowanceExportLabel = "Full",
            dayOfMonthLabel = "Day of Month",
            projectNameLabel = "Project name",
            workTimeByDateLabel = "Work time by date",
            allowanceLabel = "Allowance",
            workTypeLabel = "Work type",
            commentLabel = "Comment",
            employerLabel = "Employer",
            nameLabel = "Name",
            totalSumLabel = "TOTAL SUM",
            startDateLabel = "Start date",
            titleLabel = "Timesheet",
            endDateLabel = "End date",
            projectTimeLabel = "Project time",
            totalLabel = "Total",
            generalLabel = "General",
            workTimeTotalLabel = "Work time total",
            kilometresLabel = "Kilometres",
            flexTimeTotalLabel = "Flex time total",
            totalFlexTimeTotal = totalFlexTimeTotal
        )
    }
}
