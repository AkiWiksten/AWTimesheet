package com.akiwiksten.awtimesheet.core

import java.text.Normalizer

/**
 * Shared matcher for Absence-Flex day labels used across modules.
 */
object AbsenceFlexDayMatcher {
    fun isAbsenceFlexDay(
        workType: String,
        projectName: String,
        localizedFlexDayWorkType: String
    ): Boolean {
        val normalizedLocalized = normalizeLabel(localizedFlexDayWorkType)
        if (normalizedLocalized.isBlank()) return false

        val normalizedWorkType = normalizeLabel(workType)
        val normalizedProjectName = normalizeLabel(projectName)
        return normalizedWorkType == normalizedLocalized || normalizedProjectName == normalizedLocalized
    }

    private fun normalizeLabel(value: String): String {
        val withoutDiacritics = Normalizer
            .normalize(value.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return withoutDiacritics
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
    }
}


