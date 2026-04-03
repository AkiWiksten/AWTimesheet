package com.akiwiksten.worktime30.core.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Suppress("FunctionNaming", "MagicNumber")
@Composable
fun Header( title: String ){
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val textHeight = screenHeight * 0.10f
    Text(
        text = title,
        fontSize = 30.sp,
        modifier = Modifier.fillMaxWidth()
            .height(textHeight.dp)
            .wrapContentSize(Alignment.Center)
            .padding(start = 0.dp, top = 10.dp, end = 0.dp, bottom = 10.dp)
    )
}
