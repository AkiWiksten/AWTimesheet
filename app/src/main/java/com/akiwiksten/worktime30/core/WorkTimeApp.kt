package com.akiwiksten.worktime30.core

import android.app.Application
import com.akiwiksten.worktime30.domain.EnsureDefaultWorkStatsUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WorkTimeApp : Application() {

	@Inject
	lateinit var ensureDefaultWorkStatsUseCase: EnsureDefaultWorkStatsUseCase

	private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	override fun onCreate() {
		super.onCreate()

		applicationScope.launch {
			ensureDefaultWorkStatsUseCase()
		}
	}
}
