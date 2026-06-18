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

    init {
        observeRouteHistory()
    }

    fun insertRoute(distanceKm: String, startAddress: String, destinationAddress: String) {
        viewModelScope.launch {
            routeRepository.insertRoute(
                route = RouteState(
                    distance = "$distanceKm km",
                    start = startAddress,
                    destination = destinationAddress,
                    timestamp = System.currentTimeMillis().toString(),
                )
            )
        }
    }

    fun clearRouteHistory() {
        viewModelScope.launch {
            routeRepository.clearAll()
        }
    }

    private fun observeRouteHistory() {
        viewModelScope.launch {
            routeRepository.observeAll().collectLatest { routes ->
                _routeHistory.update {
                    routes.sortedByDescending { route -> route.timestamp.toLongOrNull() ?: Long.MIN_VALUE }
                }
            }
        }
    }
}

