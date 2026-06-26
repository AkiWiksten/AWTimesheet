package com.akiwiksten.awtimesheet.feature.intro

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.rememberDelayedLoadingVisibility

private const val ANIMATION_DURATION = 3000
private const val SCREEN_FILL_RATIO = 0.6f
private const val DEFAULT_FALLBACK_SCALE = 2.4f
private const val MIN_INITIAL_SCALE = 0.01f

@Composable
fun IntroScreen(
    onItemClick: () -> Unit,
    viewModel: IntroViewModel = hiltViewModel()
) {
    val activity = LocalActivity.current

    DisposableEffect(activity) {
        val window = activity?.window
        val decorView = window?.decorView
        val previousDecorFits = true

        if (window != null && decorView != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose {
            if (window != null && decorView != null) {
                WindowInsetsControllerCompat(window, decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
                WindowCompat.setDecorFitsSystemWindows(window, previousDecorFits)
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val currentUiState = uiState

    IntroStateContent(
        uiState = currentUiState,
        onItemClick = onItemClick
    )
}

@Composable
internal fun IntroStateContent(
    uiState: IntroUiState,
    onItemClick: () -> Unit
) {
    IntroBackgroundContainer { maxWidth, maxHeight ->
        val showLoadingIndicator = rememberDelayedLoadingVisibility(
            isLoading = uiState is IntroUiState.Loading
        )
        var lastSuccessState by remember { mutableStateOf<IntroUiState.Success?>(value = null) }

        LaunchedEffect(uiState) {
            if (uiState is IntroUiState.Success) {
                lastSuccessState = uiState
            }
        }

        when (uiState) {
            is IntroUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showLoadingIndicator) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        lastSuccessState?.let {
                            IntroAnimatedContent(
                                appName = it.appName,
                                onItemClick = onItemClick,
                                maxWidth = maxWidth,
                                maxHeight = maxHeight
                            )
                        }
                    }
                }
            }
            is IntroUiState.Success -> {
                IntroAnimatedContent(
                    appName = uiState.appName,
                    onItemClick = onItemClick,
                    maxWidth = maxWidth,
                    maxHeight = maxHeight
                )
            }
            is IntroUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.error_message, uiState.message),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroBackgroundContainer(content: @Composable (Dp, Dp) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val backgroundRes = if (isLandscape) {
            R.drawable.aw_timesheet_background_landscape
        } else {
            R.drawable.aw_timesheet_background_portrait
        }

        Image(
            painter = painterResource(id = backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        content(maxWidth, maxHeight)
    }
}

@Composable
private fun IntroAnimatedContent(
    appName: String,
    onItemClick: () -> Unit,
    maxWidth: Dp,
    maxHeight: Dp
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val baseStyle = TextStyle(
        fontSize = 64.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        textMotion = TextMotion.Animated
    )

    val targetScaleFactor = remember(appName, maxWidth, maxHeight) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val textWidth = textMeasurer.measure(appName, baseStyle).size.width
        if (textWidth > 0) (maxWidthPx * SCREEN_FILL_RATIO) / textWidth else DEFAULT_FALLBACK_SCALE
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val currentScale by animateFloatAsState(
        targetValue = if (startAnimation) targetScaleFactor else MIN_INITIAL_SCALE,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
        label = "IntroScale"
    )

    IntroContent(
        appName = appName,
        currentScale = currentScale,
        showTapMe = currentScale >= targetScaleFactor * 0.99f,
        onItemClick = onItemClick,
        baseStyle = baseStyle
    )
}

@Composable
private fun IntroContent(
    appName: String,
    currentScale: Float,
    showTapMe: Boolean,
    onItemClick: () -> Unit,
    baseStyle: TextStyle
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = true, onClick = onItemClick),
        contentAlignment = Alignment.Center
    ) {
        StrokeText(
            text = appName,
            style = baseStyle,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    transformOrigin = TransformOrigin.Center
                }
                .height(80.dp)
                .fillMaxWidth()
        )

        if (showTapMe) {
            StrokeText(
                text = stringResource(R.string.tap_me),
                style = baseStyle.copy(fontSize = 16.sp),
                strokeWidth = 4f,
                modifier = Modifier
                    .padding(top = 160.dp)
                    .height(40.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StrokeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 16f
) {
    val textMeasurer = rememberTextMeasurer()
    val textLayoutResult = remember(text, style) {
        textMeasurer.measure(text, style)
    }

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f - (textLayoutResult.size.height / 2f)
        val topLeft = androidx.compose.ui.geometry.Offset(
            centerX - (textLayoutResult.size.width / 2f),
            centerY
        )

        // Draw Stroke
        drawText(
            textLayoutResult = textLayoutResult,
            color = Color.Black,
            topLeft = topLeft,
            drawStyle = Stroke(width = strokeWidth)
        )

        // Draw Fill
        drawText(
            textLayoutResult = textLayoutResult,
            color = Color.White,
            topLeft = topLeft,
            drawStyle = Fill
        )
    }
}
