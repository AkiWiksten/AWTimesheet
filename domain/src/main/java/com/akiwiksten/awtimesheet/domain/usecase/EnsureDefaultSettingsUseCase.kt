package com.akiwiksten.awtimesheet.domain.usecase

import android.content.Context
import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.DEFAULT_WORK_TYPES
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class EnsureDefaultSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        val existing = settingsRepository.getSettings()
        if (existing != null) return

        settingsRepository.insertSettings(
            SettingsState(
                dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                dailyLunchTimeEstimate = ZERO_TIME,
                initialFlexTimeTotal = ZERO_TIME
            )
        )

        // Insert default work types
        DEFAULT_WORK_TYPES.forEach { workTypeResId ->
            settingsRepository.insertWorkType(context.getString(workTypeResId))
        }
    }
}
