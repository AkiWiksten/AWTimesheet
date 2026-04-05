package com.akiwiksten.worktime30.feature.intro

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class IntroViewModel @Inject constructor(
    @Named("app_name") appNameStr: String
) : ViewModel() {

    private val _appName = MutableStateFlow(appNameStr)
    val appName: StateFlow<String> = _appName.asStateFlow()
}
