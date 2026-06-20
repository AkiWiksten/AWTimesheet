package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import kotlinx.coroutines.flow.Flow

interface AbsenceRepository {
    fun getAll(): Flow<List<AbsenceState>>
    suspend fun insertAbsence(absence: AbsenceState)
    suspend fun delete(absence: AbsenceState)
}
