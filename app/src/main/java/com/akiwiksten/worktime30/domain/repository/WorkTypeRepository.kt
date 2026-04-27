package com.akiwiksten.worktime30.domain.repository

interface WorkTypeRepository {
    suspend fun loadWorkTypes(): List<String>
    suspend fun insertWorkType(workType: String)
    suspend fun deleteWorkType(workType: String)
    suspend fun deleteAll()
}
