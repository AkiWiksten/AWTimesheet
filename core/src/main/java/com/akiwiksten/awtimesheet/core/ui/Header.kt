package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun Header(
    title: String,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = modifier
            .then(other = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier)
            .wrapContentSize(align = Alignment.Center)
    )
}
