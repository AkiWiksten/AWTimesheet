package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

data class ScrollableScreenColumnState(
    val scrollState: ScrollState,
    val modifier: Modifier = Modifier,
    val columnModifier: Modifier = Modifier,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    val verticalArrangement: Arrangement.Vertical = Arrangement.Top,
)
