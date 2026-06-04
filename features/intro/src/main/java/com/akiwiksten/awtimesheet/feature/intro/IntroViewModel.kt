package com.akiwiksten.awtimesheet.feature.intro

import androidx.lifecycle.ViewModel
import com.akiwiksten.awtimesheet.core.APP_NAME_QUALIFIER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named

sealed class IntroUiState {
    object Loading : IntroUiState()

    data class Success(
        val appName: String = ""
    ) : IntroUiState()

    data class Error(val message: String) : IntroUiState()
}

@HiltViewModel
class IntroViewModel @Inject constructor(
    @Named(APP_NAME_QUALIFIER) appNameStr: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<IntroUiState>(IntroUiState.Loading)
    val uiState: StateFlow<IntroUiState> = _uiState.asStateFlow()

    init {
        // Simulate loading and then transition to success
        _uiState.value = IntroUiState.Success(appName = appNameStr)
    }
}
