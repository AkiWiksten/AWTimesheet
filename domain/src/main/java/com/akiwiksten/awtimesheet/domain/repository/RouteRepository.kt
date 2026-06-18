package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.domain.model.RouteState

interface RouteRepository {
    suspend fun getAll(): List<RouteState>
    suspend fun insertRoute(route: RouteState)
    suspend fun delete(route: RouteState)
}