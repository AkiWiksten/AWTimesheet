package com.akiwiksten.awtimesheet.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.akiwiksten.awtimesheet.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DistanceCalculatorViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
) : ViewModel() {
    private val _routeHistory = MutableStateFlow<List<RouteState>>(emptyList())
    val routeHistory: StateFlow<List<RouteState>> = _routeHistory.asStateFlow()
    private val _selectedRoute = MutableStateFlow<RouteState?>(null)
    val selectedRoute: StateFlow<RouteState?> = _selectedRoute.asStateFlow()

    init {
        loadInitialRouteHistory()
        observeRouteHistory()
    }

    fun insertRoute(
        distanceKm: String,
        startAddress: String,
        startLatitude: Double?,
        startLongitude: Double?,
        destinationAddress: String,
        destinationLatitude: Double?,
        destinationLongitude: Double?,
    ) {
        viewModelScope.launch {
            routeRepository.insertRoute(
                route = RouteState(
                    distance = "$distanceKm km",
                    start = startAddress,
                    startLatitude = startLatitude,
                    startLongitude = startLongitude,
                    destination = destinationAddress,
                    destinationLatitude = destinationLatitude,
                    destinationLongitude = destinationLongitude,
                    timestamp = System.currentTimeMillis().toString(),
                )
            )
        }
    }

    fun clearRouteHistory() {
        viewModelScope.launch {
            routeRepository.clearAll()
            _selectedRoute.value = null
        }
    }

    fun selectRoute(route: RouteState) {
        _selectedRoute.update { currentSelection ->
            if (currentSelection?.start == route.start && currentSelection.destination == route.destination) {
                null
            } else {
                route
            }
        }
    }

    fun deleteRoute(route: RouteState) {
        viewModelScope.launch {
            routeRepository.delete(route)
        }
    }

    fun clearSelectedRoute() {
        _selectedRoute.value = null
    }

    private fun loadInitialRouteHistory() {
        viewModelScope.launch {
            _routeHistory.update {
                routeRepository.getAll()
                    .sortedByDescending { route -> route.timestamp.toLongOrNull() ?: Long.MIN_VALUE }
            }
        }
    }

    private fun observeRouteHistory() {
        viewModelScope.launch {
            routeRepository.observeAll().collectLatest { routes ->
                _routeHistory.update {
                    routes.sortedByDescending { route -> route.timestamp.toLongOrNull() ?: Long.MIN_VALUE }
                }
                _selectedRoute.update { currentSelection ->
                    currentSelection?.let { selected ->
                        routes.firstOrNull { it.start == selected.start && it.destination == selected.destination }
                    }
                }
            }
        }
    }
}

