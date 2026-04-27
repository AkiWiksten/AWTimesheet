package com.akiwiksten.worktime30.feature.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme

@PreviewTest
@Preview(showBackground = true, name = "Intro - Loading")
@Composable
fun PreviewIntroLoading() {
    IntroPreviewContent(uiState = IntroUiState.Loading)
}

@PreviewTest
@Preview(showBackground = true, name = "Intro - Success")
@Composable
fun PreviewIntroSuccess() {
    IntroPreviewContent(uiState = IntroUiState.Success(appName = "WorkTime 3.0"))
}

@PreviewTest
@Preview(showBackground = true, name = "Intro - Success Long Name")
@Composable
fun PreviewIntroSuccessLongName() {
    IntroPreviewContent(uiState = IntroUiState.Success(appName = "WorkTime 3.0 Professional Edition"))
}

@PreviewTest
@Preview(showBackground = true, name = "Intro - Error")
@Composable
fun PreviewIntroError() {
    IntroPreviewContent(uiState = IntroUiState.Error(message = "Failed to load intro"))
}

@Composable
private fun IntroPreviewContent(uiState: IntroUiState) {
    WorkTime30Theme(dynamicColor = false) {
        IntroStateContent(
            uiState = uiState,
            onItemClick = {}
        )
    }
}
