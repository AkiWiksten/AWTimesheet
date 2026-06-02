package com.akiwiksten.awtimesheet.feature.workday.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.feature.workday.R
import com.akiwiksten.awtimesheet.feature.workday.WorkdayActions
import com.akiwiksten.awtimesheet.feature.workday.WorkdayUiState

@Composable
internal fun WorkdayLoadingContent(
    showLoadingIndicator: Boolean,
    cachedState: WorkdayUiState.Success?,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    if (showLoadingIndicator) {
        CenteredLoadingBox()
        return
    }

    cachedState?.let {
        WorkdaySuccessContent(
            state = it,
            selectedItemIndex = selectedItemIndex,
            actions = actions
        )
    }
}

@Composable
internal fun WorkdayErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(all = 32.dp)
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}
