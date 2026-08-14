package com.strive.antiqum.museums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strive.antiqum.designsystem.ThemeMode
import com.strive.antiqum.museums.data.Museum
import com.strive.antiqum.museums.data.MuseumCategory
import com.strive.antiqum.museums.data.MuseumsRepository
import com.strive.antiqum.network.Response
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MainTab {
    Map,
    Museums,
    Settings
}

enum class PersonalFilter(val label: String) {
    All("All museums"),
    Favorites("Favorites"),
    Visited("Visited"),
    NotVisited("Not visited")
}

enum class MuseumSort(val label: String) {
    Distance("Distance"),
    Alphabetical("A–Z")
}

data class AntiqumAppState(
    val hasEnteredApp: Boolean = false,
    val selectedTab: MainTab = MainTab.Map,
    val selectedMuseumId: String? = null,
    val mapSelectedMuseumId: String? = null,
    val searchQuery: String = "",
    val selectedCategory: MuseumCategory = MuseumCategory.All,
    val personalFilter: PersonalFilter = PersonalFilter.All,
    val sort: MuseumSort = MuseumSort.Distance,
    val showFilters: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val visitedIds: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.System
)

sealed interface MuseumsUiState {
    data object Loading : MuseumsUiState

    data class Success(
        val allMuseums: List<Museum>,
        val visibleMuseums: List<Museum>
    ) : MuseumsUiState

    data class Error(val message: String) : MuseumsUiState
}

class MuseumsViewModel(private val repository: MuseumsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<MuseumsUiState>(MuseumsUiState.Loading)
    val uiState: StateFlow<MuseumsUiState> = _uiState.asStateFlow()

    private val _appState = MutableStateFlow(AntiqumAppState())
    val appState: StateFlow<AntiqumAppState> = _appState.asStateFlow()

    init {
        loadMuseums()
    }

    fun enterApp() {
        _appState.value = _appState.value.copy(hasEnteredApp = true)
    }

    fun selectTab(tab: MainTab) {
        _appState.value = _appState.value.copy(
            selectedTab = tab,
            selectedMuseumId = null,
            mapSelectedMuseumId = null
        )
    }

    fun selectMuseum(id: String?) {
        _appState.value = _appState.value.copy(selectedMuseumId = id)
    }

    fun selectMapMuseum(id: String?) {
        _appState.value = _appState.value.copy(mapSelectedMuseumId = id)
    }

    fun updateSearch(query: String) {
        _appState.value = _appState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun selectCategory(category: MuseumCategory) {
        _appState.value = _appState.value.copy(selectedCategory = category)
        applyFilters()
    }

    fun selectPersonalFilter(filter: PersonalFilter) {
        _appState.value = _appState.value.copy(personalFilter = filter)
        applyFilters()
    }

    fun selectSort(sort: MuseumSort) {
        _appState.value = _appState.value.copy(sort = sort)
        applyFilters()
    }

    fun setFiltersVisible(visible: Boolean) {
        _appState.value = _appState.value.copy(showFilters = visible)
    }

    fun toggleFavorite(id: String) {
        val favorites = _appState.value.favoriteIds.toggle(id)
        _appState.value = _appState.value.copy(favoriteIds = favorites)
        applyFilters()
    }

    fun toggleVisited(id: String) {
        val visited = _appState.value.visitedIds.toggle(id)
        _appState.value = _appState.value.copy(visitedIds = visited)
        applyFilters()
    }

    fun setThemeMode(themeMode: ThemeMode) {
        _appState.value = _appState.value.copy(themeMode = themeMode)
    }

    fun reload() {
        loadMuseums()
    }

    private fun loadMuseums() {
        viewModelScope.launch {
            _uiState.value = MuseumsUiState.Loading
            _uiState.value = when (
                val response = repository.getNearbyMuseums(
                    latitude = ZAGREB_LATITUDE,
                    longitude = ZAGREB_LONGITUDE
                )
            ) {
                is Response.Success -> MuseumsUiState.Success(
                    allMuseums = response.data,
                    visibleMuseums = response.data
                )
                is Response.HttpError -> MuseumsUiState.Error(
                    "Wikidata could not be reached (HTTP ${response.statusCode})."
                )
                is Response.Error -> MuseumsUiState.Error(
                    response.e.message ?: "Wikidata could not be reached."
                )
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        val current = _uiState.value as? MuseumsUiState.Success ?: return
        val controls = _appState.value
        val query = controls.searchQuery.trim().lowercase()
        val filtered = current.allMuseums
            .asSequence()
            .filter { museum ->
                controls.selectedCategory == MuseumCategory.All || museum.category == controls.selectedCategory
            }
            .filter { museum ->
                when (controls.personalFilter) {
                    PersonalFilter.All -> true
                    PersonalFilter.Favorites -> museum.id in controls.favoriteIds
                    PersonalFilter.Visited -> museum.id in controls.visitedIds
                    PersonalFilter.NotVisited -> museum.id !in controls.visitedIds
                }
            }
            .filter { museum ->
                query.isBlank() ||
                    listOf(
                        museum.name,
                        museum.city,
                        museum.country,
                        museum.category.label,
                        museum.description
                    ).any { value -> query in value.lowercase() }
            }
            .let { museums ->
                when (controls.sort) {
                    MuseumSort.Distance -> museums.sortedBy(Museum::distanceKm)
                    MuseumSort.Alphabetical -> museums.sortedBy { it.name.lowercase() }
                }
            }
            .toList()
        _uiState.value = current.copy(visibleMuseums = filtered)
    }

    private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

    companion object {
        const val ZAGREB_LATITUDE = 45.8150
        const val ZAGREB_LONGITUDE = 15.9819
    }
}
