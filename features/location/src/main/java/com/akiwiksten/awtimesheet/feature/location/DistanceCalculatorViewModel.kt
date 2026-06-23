package com.akiwiksten.awtimesheet.feature.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.akiwiksten.awtimesheet.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DistanceCalculatorViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
) : ViewModel() {
    private val _routeHistory = MutableStateFlow<List<RouteState>>(emptyList())
    val routeHistory: StateFlow<List<RouteState>> = _routeHistory.asStateFlow()
    private val _selectedRoutes = MutableStateFlow<Set<RouteState>>(emptySet())
    val selectedRoutes: StateFlow<Set<RouteState>> = _selectedRoutes.asStateFlow()

    init {
        loadInitialRouteHistory()
        observeRouteHistory()
    }

    fun insertRoute(request: InsertRouteRequest) {
        viewModelScope.launch {
            routeRepository.insertRoute(
                route = RouteState(
                    distance = "${request.distanceKm} km",
                    start = request.startAddress,
                    startLatitude = request.startLatitude,
                    startLongitude = request.startLongitude,
                    destination = request.destinationAddress,
                    destinationLatitude = request.destinationLatitude,
                    destinationLongitude = request.destinationLongitude,
                    timestamp = System.currentTimeMillis().toString(),
                )
            )
        }
    }

    fun clearRouteHistory() {
        viewModelScope.launch {
            routeRepository.clearAll()
            _selectedRoutes.value = emptySet()
        }
    }

    fun selectRoute(route: RouteState) {
        _selectedRoutes.update { currentSelection ->
            if (currentSelection.any { it.timestamp == route.timestamp }) {
                currentSelection.filterNot { it.timestamp == route.timestamp }.toSet()
            } else {
                currentSelection + route
            }
        }
    }

    fun deleteSelectedRoutes() {
        viewModelScope.launch {
            val toDelete = _selectedRoutes.value
            toDelete.forEach { routeRepository.delete(it) }
            _selectedRoutes.value = emptySet()
        }
    }

    fun deleteRoute(route: RouteState) {
        viewModelScope.launch {
            routeRepository.delete(route)
        }
    }

    fun clearSelectedRoutes() {
        _selectedRoutes.value = emptySet()
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
                _selectedRoutes.update { currentSelection ->
                    currentSelection.mapNotNull { selected ->
                        routes.firstOrNull { it.timestamp == selected.timestamp }
                    }.toSet()
                }
            }
        }
    }
}

data class InsertRouteRequest(
    val distanceKm: String,
    val startAddress: String,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val destinationAddress: String,
    val destinationLatitude: Double?,
    val destinationLongitude: Double?,
)
