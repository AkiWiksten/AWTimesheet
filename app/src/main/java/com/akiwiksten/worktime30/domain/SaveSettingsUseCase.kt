package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import javax.inject.Inject

class SaveSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(
        name: String,
        employer: String,
        workTypes: List<String>,
        dailyWorkTimeEstimate: String = ""
    ) {
        settingsRepository.clearWorkTypes()
        workTypes.forEach { workType ->
            settingsRepository.insertWorkType(workType)
        }
        settingsRepository.insertSettings(SettingsState(name = name, employer = employer))

        // Save dailyWorkTimeEstimate to WorkStats
        if (dailyWorkTimeEstimate.isNotEmpty()) {
            val existingWorkStats = projectDetailsRepository.getWorkStats()
            projectDetailsRepository.insertWorkStats(
                WorkStatsState(
                    dailyWorkTime = dailyWorkTimeEstimate,
                    lunchTime = existingWorkStats?.lunchTime ?: "",
                    initialFlexTimeTotal = existingWorkStats?.initialFlexTimeTotal ?: ""
                )
            )
        }
    }
}
