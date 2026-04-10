package com.akiwiksten.worktime30.feature.intro

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme

@Preview(showBackground = true, name = "Intro - Loading")
@Composable
fun PreviewIntroLoading() {
    IntroPreviewContent(uiState = IntroUiState.Loading)
}

@Preview(showBackground = true, name = "Intro - Success")
@Composable
fun PreviewIntroSuccess() {
    IntroPreviewContent(uiState = IntroUiState.Success(appName = "WorkTime 3.0"))
}

@Preview(showBackground = true, name = "Intro - Success Long Name")
@Composable
fun PreviewIntroSuccessLongName() {
    IntroPreviewContent(uiState = IntroUiState.Success(appName = "WorkTime 3.0 Professional Edition"))
}

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
