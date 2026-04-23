package com.akiwiksten.worktime30.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
            .heightIn(min = 44.dp)
            .padding(vertical = 8.dp, horizontal = 8.dp)
            .wrapContentSize(align = Alignment.Center)
            .padding(start = 0.dp, top = 4.dp, end = 0.dp, bottom = 4.dp)
    )
}
