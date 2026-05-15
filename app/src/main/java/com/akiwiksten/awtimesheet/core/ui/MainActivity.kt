package com.akiwiksten.awtimesheet.core.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.akiwiksten.awtimesheet.core.theme.WorkTime30Theme
import com.akiwiksten.awtimesheet.navigation.WorkTime30App
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkTime30Theme {
                WorkTime30App()
            }
        }
    }
}
