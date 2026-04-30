package com.akiwiksten.worktime30

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(name = "Screenshot Smoke", showBackground = true)
@Composable
fun SmokeSurfacePreview() {
    WorkTime30Theme(dynamicColor = false) {
        Surface {
            Text(text = "Compose screenshot smoke test")
        }
    }
}
