package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CenteredLoadingBox(
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = true
) {
    Box(
        modifier = modifier.then(
            if (fillMaxSize) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun CenteredErrorBox(
    errorMessage: String,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = true
) {
    Box(
        modifier = modifier.then(
            if (fillMaxSize) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $errorMessage",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(all = 32.dp)
        )
    }
}
