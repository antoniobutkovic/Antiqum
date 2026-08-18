package com.strive.antiqum.louvre.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strive.antiqum.louvre.data.LouvreIndoorBootstrap
import com.strive.antiqum.louvre.data.LouvreIndoorRepository
import com.strive.antiqum.louvre.data.LouvreLocalRouting
import com.strive.antiqum.louvre.data.LouvreNode
import com.strive.antiqum.louvre.data.LouvreRouteRequest
import com.strive.antiqum.louvre.data.LouvreRouteResult
import com.strive.antiqum.louvre.data.LouvreSight
import com.strive.antiqum.louvre.data.LouvreTourRequest
import com.strive.antiqum.network.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LouvreIndoorTab(val label: String) {
    Map("Map"),
    Sights("Sights"),
    Route("Route")
}

data class LouvreIndoorUiState(
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val bootstrap: LouvreIndoorBootstrap? = null,
    val error: String? = null,
    val message: String? = null,
    val selectedTab: LouvreIndoorTab = LouvreIndoorTab.Map,
    val selectedLevel: String = "-2",
    val currentNodeId: String? = null,
    val locationQuery: String = "",
    val sightQuery: String = "",
    val accessible: Boolean = false,
    val finishTourAtExit: Boolean = true,
    val favoriteSightIds: Set<String> = emptySet(),
    val activeRoute: LouvreRouteResult? = null,
    val orderedTourSightIds: List<String> = emptyList()
)

class LouvreIndoorViewModel(private val repository: LouvreIndoorRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LouvreIndoorUiState(favoriteSightIds = repository.favoriteSightIds())
    )
    val uiState: StateFlow<LouvreIndoorUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val response = repository.getBootstrap()) {
                is Response.Success -> {
                    val data = response.data
                    val current = _uiState.value.currentNodeId?.takeIf { id -> data.nodes.any { it.id == id } }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        bootstrap = data,
                        currentNodeId = current,
                        selectedLevel = data.nodes.firstOrNull { it.id == current }?.level ?: _uiState.value.selectedLevel,
                        favoriteSightIds = repository.favoriteSightIds(),
                        error = null
                    )
                }
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = response.message("The Louvre guide could not be loaded.")
                )
            }
        }
    }

    fun selectTab(tab: LouvreIndoorTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun selectLevel(level: String) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
    }

    fun updateLocationQuery(value: String) {
        _uiState.value = _uiState.value.copy(locationQuery = value)
    }

    fun updateSightQuery(value: String) {
        _uiState.value = _uiState.value.copy(sightQuery = value)
    }

    fun selectCurrentLocation(nodeId: String) {
        val node = _uiState.value.bootstrap?.nodes?.firstOrNull { it.id == nodeId } ?: return
        _uiState.value = _uiState.value.copy(
            currentNodeId = node.id,
            selectedLevel = node.level,
            locationQuery = "",
            activeRoute = null,
            orderedTourSightIds = emptyList(),
            message = "Starting point set to ${node.shortName}."
        )
    }

    fun setAccessible(value: Boolean) {
        _uiState.value = _uiState.value.copy(accessible = value, activeRoute = null)
    }

    fun setFinishTourAtExit(value: Boolean) {
        _uiState.value = _uiState.value.copy(finishTourAtExit = value)
    }

    fun toggleFavorite(sightId: String) {
        val current = _uiState.value.favoriteSightIds
        val updated = if (sightId in current) current - sightId else current + sightId
        repository.setFavoriteSightIds(updated)
        _uiState.value = _uiState.value.copy(favoriteSightIds = updated)
    }

    fun navigateToSight(sightId: String) {
        val state = _uiState.value
        val start = currentStartOrPrompt() ?: return
        requestRoute(LouvreRouteRequest(fromNodeId = start, sightId = sightId, accessible = state.accessible))
    }

    fun navigateToNode(nodeId: String) {
        val state = _uiState.value
        val start = currentStartOrPrompt() ?: return
        requestRoute(LouvreRouteRequest(fromNodeId = start, toNodeId = nodeId, accessible = state.accessible))
    }

    fun findNearestExit() {
        val state = _uiState.value
        val start = currentStartOrPrompt() ?: return
        requestRoute(LouvreRouteRequest(fromNodeId = start, nearestVisitorExit = true, accessible = state.accessible))
    }

    fun optimizeFavorites() {
        val state = _uiState.value
        val start = currentStartOrPrompt() ?: return
        if (state.favoriteSightIds.isEmpty()) {
            _uiState.value = state.copy(message = "Favorite at least one sight to build a tour.", selectedTab = LouvreIndoorTab.Sights)
            return
        }
        _uiState.value = state.copy(isWorking = true, message = null)
        viewModelScope.launch {
            when (
                val response = repository.optimizeTour(
                    LouvreTourRequest(
                        fromNodeId = start,
                        sightIds = state.favoriteSightIds.toList(),
                        accessible = state.accessible,
                        finishAtVisitorExit = state.finishTourAtExit
                    )
                )
            ) {
                is Response.Success -> showRoute(
                    route = response.data.route,
                    orderedSightIds = response.data.orderedSightIds,
                    message = if (response.data.skippedSightIds.isEmpty()) null else "Some unavailable sights were skipped."
                )
                else -> _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    message = response.message("A tour could not be calculated for these sights.")
                )
            }
        }
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(activeRoute = null, orderedTourSightIds = emptyList())
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun locationMatches(): List<LouvreNode> = _uiState.value.bootstrap?.let {
        LouvreLocalRouting.searchNodes(it, _uiState.value.locationQuery)
    }.orEmpty()

    fun sightMatches(): List<LouvreSight> = _uiState.value.bootstrap?.let {
        LouvreLocalRouting.searchSights(it, _uiState.value.sightQuery)
    }.orEmpty()

    private fun requestRoute(request: LouvreRouteRequest) {
        _uiState.value = _uiState.value.copy(isWorking = true, message = null)
        viewModelScope.launch {
            when (val response = repository.navigate(request)) {
                is Response.Success -> showRoute(response.data)
                else -> _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    message = response.message("No suitable route is currently available.")
                )
            }
        }
    }

    private fun currentStartOrPrompt(): String? {
        val state = _uiState.value
        return state.currentNodeId ?: run {
            _uiState.value = state.copy(
                selectedTab = LouvreIndoorTab.Map,
                message = "Set your current location first: enter a room number, room name, or nearby artwork."
            )
            null
        }
    }

    private fun showRoute(
        route: LouvreRouteResult,
        orderedSightIds: List<String> = emptyList(),
        message: String? = null
    ) {
        val level = route.segments.firstOrNull()?.level ?: _uiState.value.selectedLevel
        _uiState.value = _uiState.value.copy(
            isWorking = false,
            activeRoute = route,
            orderedTourSightIds = orderedSightIds,
            selectedLevel = level,
            selectedTab = LouvreIndoorTab.Route,
            message = message
        )
    }

    private fun Response<*>.message(fallback: String): String = when (this) {
        is Response.Error -> e.message?.takeIf(String::isNotBlank) ?: fallback
        is Response.HttpError -> if (statusCode == 422) "No route is available with the selected accessibility option." else fallback
        is Response.Success -> fallback
    }
}
