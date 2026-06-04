package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable

@Composable
fun ScrollableScreenColumn(
    state: ScrollableScreenColumnState,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = state.modifier.verticalScrollbar(scrollState = state.scrollState)) {
        Column(
            modifier = state.columnModifier.verticalScroll(state = state.scrollState),
            horizontalAlignment = state.horizontalAlignment,
            verticalArrangement = state.verticalArrangement,
            content = content
        )
    }
}
