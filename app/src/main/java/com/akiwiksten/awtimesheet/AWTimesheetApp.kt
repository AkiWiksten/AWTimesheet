package com.akiwiksten.awtimesheet

import android.app.Application
import com.akiwiksten.awtimesheet.core.DEFAULT_WORK_TYPES
import com.akiwiksten.awtimesheet.domain.usecase.EnsureDefaultSettingsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AWTimesheetApp : Application() {

    @Inject
    lateinit var ensureDefaultSettingsUseCase: EnsureDefaultSettingsUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            ensureDefaultSettingsUseCase(resolveDefaultWorkTypeLabels())
        }
    }

    private fun resolveDefaultWorkTypeLabels(): List<String> {
        return DEFAULT_WORK_TYPES.map { getString(it) }
    }
}
