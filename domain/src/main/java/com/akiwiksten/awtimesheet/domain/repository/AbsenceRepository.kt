package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.domain.model.AbsenceState

interface AbsenceRepository {
    suspend fun getAll(): List<AbsenceState>
    suspend fun insertAbsence(absence: AbsenceState)
    suspend fun delete(absence: AbsenceState)
}
