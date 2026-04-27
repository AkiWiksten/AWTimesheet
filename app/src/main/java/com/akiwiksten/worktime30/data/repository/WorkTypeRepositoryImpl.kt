package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toWorkTypeEntity
import com.akiwiksten.worktime30.domain.repository.WorkTypeRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkTypeRepositoryImpl @Inject constructor(
    private val workTypeDao: WorkTypeDao
) : WorkTypeRepository {
    override suspend fun loadWorkTypes(): List<String> =
        workTypeDao.loadWorkTypes().map { it.toDomain() }

    override suspend fun insertWorkType(workType: String) =
        workTypeDao.insertWorkType(workType.toWorkTypeEntity())

    override suspend fun deleteWorkType(workType: String) =
        workTypeDao.delete(workType.toWorkTypeEntity())

    override suspend fun deleteAll() = workTypeDao.deleteAll()
}
