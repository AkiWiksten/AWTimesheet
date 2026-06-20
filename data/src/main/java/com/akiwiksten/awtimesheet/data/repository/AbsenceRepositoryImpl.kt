package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.data.database.dao.AbsenceDao
import com.akiwiksten.awtimesheet.data.mapper.toDomain
import com.akiwiksten.awtimesheet.data.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AbsenceRepositoryImpl @Inject constructor(
    private val absenceDao: AbsenceDao
) : AbsenceRepository {
    override fun getAll(): Flow<List<AbsenceState>> =
        absenceDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insertAbsence(absence: AbsenceState) {
        absenceDao.insertAbsence(absence.toEntity())
    }

    override suspend fun delete(absence: AbsenceState) {
        absenceDao.delete(absence.toEntity())
    }
}
