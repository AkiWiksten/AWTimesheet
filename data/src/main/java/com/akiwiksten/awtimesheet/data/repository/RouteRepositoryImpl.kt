package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.data.database.dao.RouteDao
import com.akiwiksten.awtimesheet.data.mapper.toDomain
import com.akiwiksten.awtimesheet.data.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.akiwiksten.awtimesheet.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao
) : RouteRepository {
    override suspend fun getAll(): List<RouteState> = routeDao.getAll().map { it.toDomain() }

    override fun observeAll(): Flow<List<RouteState>> = routeDao.observeAll().map { routes ->
        routes.map { route -> route.toDomain() }
    }

    override suspend fun insertRoute(route: RouteState) {
        routeDao.insertRoute(route.toEntity())
    }

    override suspend fun delete(route: RouteState) {
        routeDao.delete(route.toEntity())
    }

    override suspend fun clearAll() {
        routeDao.clearAll()
    }
}
