package com.akiwiksten.worktime30.core.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner

@Composable
inline fun <reified VM : ViewModel> sharedActivityViewModel(): VM {
    val activity = LocalActivity.current
        ?: error("No Activity available for activity-scoped ViewModel")
    val owner = activity as? ViewModelStoreOwner
        ?: error("Activity is not a ViewModelStoreOwner")
    return hiltViewModel(viewModelStoreOwner = owner)
}

