package com.akiwiksten.awtimesheet.feature.singleproject

import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenArgs
import com.akiwiksten.awtimesheet.feature.singleproject.model.resolveFullInitialSingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.withAbsenceLogic
import com.akiwiksten.awtimesheet.test.projectState
import com.akiwiksten.awtimesheet.test.settingsState
import org.junit.Assert
import org.junit.Test

class SingleProjectStateResolverTest {

    @Test
    fun withAbsenceLogic_whenWorkTypeChangesToAbsence_updatesProjectTimeAndName() {
        val previousState = projectState(workType = "Work", projectName = "Old Project")
        val currentState = previousState.copy(workType = "Absence-Sick")
        val settings = settingsState(dailyWorkTimeEstimate = "07:30")

        val result = currentState.withAbsenceLogic(
            previousState = previousState,
            settings = settings,
            absencePrefix = "Absence"
        )

        Assert.assertEquals("07:30", result.projectTime)
        Assert.assertEquals("Absence-Sick", result.projectName)
        Assert.assertEquals("", result.kilometres)
        Assert.assertEquals("", result.allowance)
    }

    @Test
    fun withAbsenceLogic_whenWorkTypeChangesToFinnishAbsence_updatesProjectTimeAndName() {
        val previousState = projectState(workType = "Työ", projectName = "Vanha Projekti")
        val currentState = previousState.copy(workType = "Poissaolo-Sairas")
        val settings = settingsState(dailyWorkTimeEstimate = "07:30")

        val result = currentState.withAbsenceLogic(
            previousState = previousState,
            settings = settings,
            absencePrefix = "Poissaolo"
        )

        Assert.assertEquals("07:30", result.projectTime)
        Assert.assertEquals("Poissaolo-Sairas", result.projectName)
        Assert.assertEquals("", result.kilometres)
        Assert.assertEquals("", result.allowance)
    }

    @Test
    fun withAbsenceLogic_whenWorkTypeDoesNotChange_doesNotUpdateProjectTime() {
        val previousState = projectState(workType = "Absence-Sick", projectTime = "02:00")
        val currentState = previousState.copy(projectTime = "02:00")
        val settings = settingsState(dailyWorkTimeEstimate = "07:30")

        val result = currentState.withAbsenceLogic(
            previousState = previousState,
            settings = settings,
            absencePrefix = "Absence"
        )

        Assert.assertEquals("02:00", result.projectTime)
    }

    @Test
    fun resolveFullInitialSingleProjectState_inAddMode_withAbsenceWorkType_setsInitialTimeAndName() {
        val args = SingleProjectScreenArgs(
            initialSingleProjectState = projectState(listIndex = -1, workType = "Absence-Sick")
        )
        val settings = settingsState(dailyWorkTimeEstimate = "07:30")
        val argsWithSettings = args.copy(initialSettings = settings)

        val result = resolveFullInitialSingleProjectState(
            uiState = SingleProjectUiState.Success(
                data = argsWithSettings.initialSingleProjectState,
                workTimeByDate = "00:00",
                settings = argsWithSettings.initialSettings,
                workTypes = emptyList(),
                otherProjectNames = emptyList()
            ),
            noAllowanceText = "None",
            defaultWorkTypeText = "Other",
            absencePrefix = "Absence"
        )

        Assert.assertEquals("07:30", result.projectTime)
        Assert.assertEquals("Absence-Sick", result.projectName)
        Assert.assertEquals("", result.kilometres)
        Assert.assertEquals("", result.allowance)
    }
}
