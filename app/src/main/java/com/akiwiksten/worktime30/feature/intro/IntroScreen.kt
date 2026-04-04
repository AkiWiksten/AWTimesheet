package com.akiwiksten.worktime30.feature.intro

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akiwiksten.worktime30.R

private const val ANIMATION_DURATION = 3000
private const val SCREEN_FILL_RATIO = 0.9f
private const val BUTTON_SCALE_DIVIDER = 4f
private const val DEFAULT_FALLBACK_SCALE = 3.1f

@Suppress("LongMethod", "FunctionNaming")
@Composable
fun IntroScreen(onItemClick: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val appName = remember(context) { getApplicationName(context) }
    val textStyle = LocalTextStyle.current.copy(
        textMotion = TextMotion.Animated,
        fontSize = 20.sp
    )

    // Calculate the target scale based on screen width to ensure text fits
    val targetScaleFactor = remember(appName, configuration.screenWidthDp, density) {
        val textLayoutResult = textMeasurer.measure(appName, textStyle)
        val textWidthPx = textLayoutResult.size.width
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        
        if (textWidthPx > 0) {
            (screenWidthPx / textWidthPx) * SCREEN_FILL_RATIO
        } else {
            DEFAULT_FALLBACK_SCALE
        }
    }

    var animateToScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(targetScaleFactor) {
        animateToScale = targetScaleFactor
    }

    val currentScale by animateFloatAsState(
        targetValue = animateToScale,
        animationSpec = tween(durationMillis = ANIMATION_DURATION, easing = FastOutSlowInEasing),
        label = "IntroScaleAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
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

        Spacer(modifier = Modifier.padding(80.dp))

        Button(
            onClick = onItemClick,
            modifier = Modifier
                .graphicsLayer {
                    val btnScale = currentScale / BUTTON_SCALE_DIVIDER
                    scaleX = btnScale
                    scaleY = btnScale
                    transformOrigin = TransformOrigin.Center
                }
        ) {
            Text(
                text = stringResource(R.string.continueFromIntro),
                fontSize = 40.sp,
                modifier = Modifier.padding(10.dp),
                style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated)
            )
        }
    }
}

private fun getApplicationName(context: Context): String {
    val applicationInfo = context.applicationInfo
    return if (applicationInfo.labelRes == 0) {
        applicationInfo.nonLocalizedLabel?.toString() ?: "WorkTime 3.0"
    } else {
        context.getString(applicationInfo.labelRes)
    }
}
