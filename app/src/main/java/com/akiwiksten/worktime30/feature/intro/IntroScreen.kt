package com.akiwiksten.worktime30.feature.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R

private const val ANIMATION_DURATION = 3000
private const val SCREEN_FILL_RATIO = 0.9f
private const val BUTTON_SCALE_DIVIDER = 2.4f
private const val DEFAULT_FALLBACK_SCALE = 3.1f

@Composable
fun IntroScreen(
    onItemClick: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel()
) {
    val appName by viewModel.appName.collectAsState()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val windowInfo = LocalWindowInfo.current

    val textStyle = LocalTextStyle.current.copy(
        textMotion = TextMotion.Animated,
        fontSize = 20.sp
    )

    // Ensure we have valid dimensions before calculating the target scale
    val containerSize = windowInfo.containerSize
    val hasValidDimensions = containerSize.width > 0 && containerSize.height > 0

    val targetScaleFactor = if (hasValidDimensions) {
        calculateTargetScale(
            appName = appName,
            textStyle = textStyle,
            textMeasurer = textMeasurer,
            windowInfo = windowInfo,
            density = density
        )
    } else {
        DEFAULT_FALLBACK_SCALE
    }

    val currentScale = remember { Animatable(initialValue = 1f) }

    LaunchedEffect(key1 = targetScaleFactor, key2 = hasValidDimensions) {
        if (hasValidDimensions) {
            currentScale.animateTo(
                targetValue = targetScaleFactor,
                animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
            )
        }
    }

    IntroContent(
        appName = appName,
        textStyle = textStyle,
        currentScale = currentScale.value,
        onItemClick = onItemClick
    )
}

@Composable
private fun calculateTargetScale(
    appName: String,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer,
    windowInfo: WindowInfo,
    density: Density
): Float = remember(appName, windowInfo.containerSize, density) {
    val textLayoutResult = textMeasurer.measure(text = appName, style = textStyle)
    val textWidthPx = textLayoutResult.size.width
    val screenWidthPx = windowInfo.containerSize.width

    if (textWidthPx > 0 && screenWidthPx > 0) {
        (screenWidthPx / textWidthPx) * SCREEN_FILL_RATIO
    } else {
        DEFAULT_FALLBACK_SCALE
    }
}

@Composable
private fun IntroContent(
    appName: String,
    textStyle: TextStyle,
    currentScale: Float,
    onItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.secondary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = appName,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    transformOrigin = TransformOrigin.Center
                },
            style = textStyle,
            color = Color.White
        )

        Spacer(modifier = Modifier.padding(all = 80.dp))

        Button(
            onClick = onItemClick,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
            modifier = Modifier
                .graphicsLayer {
                    val btnScale = currentScale / BUTTON_SCALE_DIVIDER
                    scaleX = btnScale
                    scaleY = btnScale
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            Text(
                text = stringResource(id = R.string.continueFromIntro),
                fontSize = 24.sp,
                modifier = Modifier.padding(all = 4.dp),
                style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated)
            )
        }
    }
}
