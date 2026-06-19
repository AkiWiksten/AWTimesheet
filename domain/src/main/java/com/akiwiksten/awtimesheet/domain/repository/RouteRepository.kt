package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.domain.model.RouteState
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    suspend fun getAll(): List<RouteState>
    fun observeAll(): Flow<List<RouteState>>
    suspend fun insertRoute(route: RouteState)
    suspend fun delete(route: RouteState)
    suspend fun clearAll()
}
