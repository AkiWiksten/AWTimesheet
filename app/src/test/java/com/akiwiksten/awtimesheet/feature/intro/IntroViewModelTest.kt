package com.akiwiksten.awtimesheet.feature.intro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroViewModelTest {

    @Test
    fun init_setsSuccessStateWithProvidedAppName() {
        val viewModel = IntroViewModel(appNameStr = "WorkTime30")

        val state = viewModel.uiState.value

        assertTrue(state is IntroUiState.Success)
        assertEquals("WorkTime30", (state as IntroUiState.Success).appName)
    }
}
