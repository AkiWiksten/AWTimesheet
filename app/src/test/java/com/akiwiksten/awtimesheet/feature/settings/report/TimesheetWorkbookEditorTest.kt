package com.akiwiksten.awtimesheet.feature.settings.report

import org.junit.Assert.assertTrue
import org.junit.Test

class TimesheetWorkbookEditorTest {

    @Test
    fun replaceSharedStrings_updatesOnlyRequestedEntries() {
        val sharedStringsXml = """
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <si><t>Day of Month</t></si>
                 <si><t>Project name</t></si>
                 <si><t>Work time by date</t></si>
                 <si><t>Allowance</t></si>
                 <si><t>Work type</t></si>
                 <si><t>Kilometres</t></si>
                <si><t>Employer</t></si>
                <si><t>Name</t></si>
                <si><t>Project 1</t></si>
                <si><t>Project 2</t></si>
                <si><t>Project 3</t></si>
                <si><t>Work time total</t></si>
                <si><t>Design</t></si>
                <si><t>Sum</t></si>
                <si><t>No</t></si>
                <si><t>Half-day</t></si>
                <si><t>Full</t></si>
                <si><t>Start date</t></si>
                <si><t>TIMESHEET</t></si>
                <si><t>End date</t></si>
                <si><t>General</t></si>
                <si><t>Flex time total</t></si>
                <si><t>Project time</t></si>
                <si><t>Flex time total</t></si>
                <si><t>Extra</t></si>
            </sst>
        """.trimIndent().toByteArray()

        val updatedXml = replaceSharedStrings(
            sharedStringsXml = sharedStringsXml,
            replacements = mapOf(
                0 to "Dag i månaden",
                7 to "Namn",
                17 to "Nej",
                20 to "Startdatum",
                22 to "Slutdatum",
                24 to "Total flex"
            )
        ).decodeToString()

        assertTrue(updatedXml.contains("Dag i månaden"))
        assertTrue(updatedXml.contains("Namn"))
        assertTrue(updatedXml.contains("Nej"))
        assertTrue(updatedXml.contains("Startdatum"))
        assertTrue(updatedXml.contains("Slutdatum"))
        assertTrue(updatedXml.contains("Total flex"))
    }
}

