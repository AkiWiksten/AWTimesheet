@file:Suppress("MagicNumber")
package com.akiwiksten.worktime30.feature.intro

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akiwiksten.worktime30.R

@Suppress("LongMethod", "FunctionNaming", "LongParameterList")
@Composable
fun IntroScreen(onItemClick: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        scale = 3.7f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val alpha: Float by animateFloatAsState(
            targetValue = scale,
            // Configure the animation duration and easing.
            animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            label = "alpha"
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            val ctx = LocalContext.current
            Text(
                text = getApplicationName(ctx),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = alpha
                        scaleY = alpha
                        transformOrigin = TransformOrigin.Center
                    }
                    .align(Alignment.Center),
                // Text composable does not take TextMotion as a parameter.
                // Provide it via style argument but make sure that we are copying from current theme
                style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.padding(80.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Button(
                onClick = { onItemClick() },
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = alpha / 4
                        scaleY = alpha / 4
                        transformOrigin = TransformOrigin.Center
                    }
            )
            {
                Text(
                    text = stringResource(R.string.continue0),
                    fontSize = 20.sp,
                    style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated)
                )
            }
        }
    }
}

fun getApplicationName(context: Context): String {
    val applicationInfo = context.applicationInfo
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(
        stringId
    )
}
