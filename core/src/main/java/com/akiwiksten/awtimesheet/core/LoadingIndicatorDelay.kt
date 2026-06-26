package com.akiwiksten.awtimesheet.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

@Composable
fun rememberDelayedLoadingVisibility(
    isLoading: Boolean,
    delayMillis: Long = LOADING_INDICATOR_DELAY_MS
): Boolean {
    var isVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(isLoading, delayMillis) {
        if (!isLoading) {
            isVisible = false
            return@LaunchedEffect
        }

        isVisible = false
        delay(timeMillis = delayMillis)
        isVisible = true
    }

    return isVisible
}
