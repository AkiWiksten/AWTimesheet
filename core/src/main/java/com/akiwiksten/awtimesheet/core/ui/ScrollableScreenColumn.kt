package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Suppress("LongParameterList")
@Composable
fun ScrollableScreenColumn(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.verticalScrollbar(scrollState = scrollState)) {
        Column(
            modifier = columnModifier.verticalScroll(state = scrollState),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}
