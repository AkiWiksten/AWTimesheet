package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwtCenterAlignedTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = DEFAULT_ELEVATION,
        shadowElevation = DEFAULT_ELEVATION
    ) {
        val density = LocalDensity.current
        var navIconWidth by remember { mutableStateOf(0.dp) }
        var actionsWidth by remember { mutableStateOf(0.dp) }

        CenterAlignedTopAppBar(
            title = {
                Header(
                    title = title,
                    modifier = Modifier.padding(top = 0.dp)
                )
            },
            navigationIcon = {
                Box(
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                        with(density) {
                            navIconWidth = layoutCoordinates.size.width.toDp()
                        }
                    }
                ) {
                    navigationIcon()
                }
            },
            actions = {
                // Actions container
                Row(
                    modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                        with(density) {
                            actionsWidth = layoutCoordinates.size.width.toDp()
                        }
                    }
                ) {
                    actions()
                }

                // Balance the title: if navigation icon is wider than actions, add spacer after actions
                val balanceSpacerWidth = if (navIconWidth > actionsWidth) {
                    navIconWidth - actionsWidth
                } else {
                    0.dp
                }

                if (balanceSpacerWidth > 0.dp) {
                    Spacer(modifier = Modifier.width(balanceSpacerWidth))
                }
            },
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    }
}
