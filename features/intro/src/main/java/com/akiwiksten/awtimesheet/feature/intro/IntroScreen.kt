package com.akiwiksten.awtimesheet.feature.intro

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility

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
    IntroBackgroundContainer {
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
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showLoadingIndicator) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        lastSuccessState?.let {
                            IntroAnimatedContent(
                                appName = it.appName,
                                onItemClick = onItemClick
                            )
                        }
                    }
                }
            }
            is IntroUiState.Success -> {
                IntroAnimatedContent(
                    appName = uiState.appName,
                    onItemClick = onItemClick
                )
            }
            is IntroUiState.Error -> {
                // Show error state
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
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
private fun IntroBackgroundContainer(content: @Composable () -> Unit) {
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

        content()
    }
}

@Composable
private fun IntroAnimatedContent(
    appName: String,
    onItemClick: () -> Unit
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val textStyle = LocalTextStyle.current.copy(
        textMotion = TextMotion.Animated,
        fontSize = 20.sp
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val scaleReferenceWidthPx = with(density) { minOf(maxWidth, maxHeight).roundToPx() }
        val hasValidDimensions = scaleReferenceWidthPx > 0 && maxHeight > 0.dp

        val targetScaleFactor = if (hasValidDimensions) {
            calculateTargetScale(
                appName = appName,
                textStyle = textStyle,
                textMeasurer = textMeasurer,
                contentWidthPx = scaleReferenceWidthPx
            )
        } else {
            DEFAULT_FALLBACK_SCALE
        }

        val currentScale = remember { Animatable(initialValue = MIN_INITIAL_SCALE) }

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
            currentScale = currentScale.value,
            onItemClick = onItemClick
        )
    }
}

@Composable
private fun calculateTargetScale(
    appName: String,
    textStyle: TextStyle,
    textMeasurer: TextMeasurer,
    contentWidthPx: Int
): Float = remember(appName, textStyle, contentWidthPx) {
    val textLayoutResult = textMeasurer.measure(text = appName, style = textStyle)
    val textWidthPx = textLayoutResult.size.width

    if (textWidthPx > 0 && contentWidthPx > 0) {
        (contentWidthPx / textWidthPx) * SCREEN_FILL_RATIO
    } else {
        DEFAULT_FALLBACK_SCALE
    }
}

@Composable
private fun IntroContent(
    appName: String,
    currentScale: Float,
    onItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = true, onClick = onItemClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StrokeText(
            text = appName,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    transformOrigin = TransformOrigin.Center
                }
        )
    }
}

@Composable
private fun StrokeText(text: String, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 64f
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 10f
            color = android.graphics.Color.BLACK
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val centerX = size.width / 2f
        val centerY = size.height / 2f

        drawContext.canvas.nativeCanvas.drawText(text, centerX, centerY, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.WHITE
        drawContext.canvas.nativeCanvas.drawText(text, centerX, centerY, paint)
    }
}
