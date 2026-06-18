package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.data.database.dao.RouteDao
import com.akiwiksten.awtimesheet.data.mapper.toDomain
import com.akiwiksten.awtimesheet.data.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.akiwiksten.awtimesheet.domain.repository.AbsenceRepository
import com.akiwiksten.awtimesheet.domain.repository.RouteRepository
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao
) : RouteRepository {
    override suspend fun getAll(): List<RouteState> = routeDao.getAll().map { it.toDomain() }

    override suspend fun insertRoute(route: RouteState) {
        routeDao.insertRoute(route.toEntity())
    }

    override suspend fun delete(route: RouteState) {
        routeDao.delete(route.toEntity())
    }
}