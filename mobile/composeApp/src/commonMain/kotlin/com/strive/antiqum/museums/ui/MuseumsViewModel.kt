package com.strive.antiqum.museums.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.strive.antiqum.designsystem.ThemeMode
import com.strive.antiqum.museums.data.Museum
import com.strive.antiqum.museums.data.MuseumCategory
import com.strive.antiqum.museums.data.MuseumsRepository
import com.strive.antiqum.network.Response
import com.strive.antiqum.profile.data.AntiqumProfile
import com.strive.antiqum.profile.data.ProfileRepository
import com.strive.antiqum.profile.data.SignInProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

enum class MuseumProfileAction {
    Save,
    MarkVisited
}

enum class MapLocationAccess {
    Unknown,
    Granted,
    Unavailable
}

data class MuseumSignInPrompt(
    val museumId: String,
    val action: MuseumProfileAction
)

data class AntiqumAppState(
    val isOnboardingVisible: Boolean = true,
    val profile: AntiqumProfile? = null,
    val selectedTab: MainTab = MainTab.Map,
    val selectedMuseumId: String? = null,
    val mapSelectedMuseumId: String? = null,
    val mapCameraRequestId: Int = 0,
    val mapUserLocation: MuseumMapLocation? = null,
    val mapLocationAccess: MapLocationAccess = MapLocationAccess.Unknown,
    val searchQuery: String = "",
    val selectedCategory: MuseumCategory = MuseumCategory.All,
    val personalFilter: PersonalFilter = PersonalFilter.All,
    val sort: MuseumSort = MuseumSort.Distance,
    val showFilters: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),
    val visitedIds: Set<String> = emptySet(),
    val signInPrompt: MuseumSignInPrompt? = null,
    val themeMode: ThemeMode = ThemeMode.System
)

sealed interface MuseumsUiState {
    data object Loading : MuseumsUiState

    data class Success(
        val nearbyMuseums: List<Museum>,
        val allMuseums: List<Museum>,
        val visibleMuseums: List<Museum>,
        val hasMoreMuseums: Boolean,
        val isLoadingMoreMuseums: Boolean = false,
        val paginationError: String? = null
    ) : MuseumsUiState

    data class Error(val message: String) : MuseumsUiState
}

class MuseumsViewModel(
    private val repository: MuseumsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<MuseumsUiState>(MuseumsUiState.Loading)
    val uiState: StateFlow<MuseumsUiState> = _uiState.asStateFlow()

    private val _museumDetails = MutableStateFlow<Museum?>(null)
    val museumDetails: StateFlow<Museum?> = _museumDetails.asStateFlow()

    private val initialProfile = profileRepository.getProfile()
    private val _appState = MutableStateFlow(
        AntiqumAppState(
            isOnboardingVisible = !profileRepository.hasCompletedOnboarding(),
            profile = initialProfile,
            favoriteIds = if (initialProfile == null) emptySet() else profileRepository.getFavoriteIds(),
            visitedIds = if (initialProfile == null) emptySet() else profileRepository.getVisitedIds()
        )
    )
    val appState: StateFlow<AntiqumAppState> = _appState.asStateFlow()

    private var nextMuseumCursor: String? = null
    private var catalogGeneration = 0
    private var catalogJob: Job? = null
    private var searchJob: Job? = null
    private var detailJob: Job? = null

    init {
        loadMuseums()
    }

    fun completeOnboarding() {
        profileRepository.completeOnboarding()
        _appState.value = _appState.value.copy(isOnboardingVisible = false)
    }

    fun showOnboarding() {
        _appState.value = _appState.value.copy(isOnboardingVisible = true)
    }

    fun signIn(provider: SignInProvider) {
        val pendingAction = _appState.value.signInPrompt
        val profile = profileRepository.signIn(provider)
        _appState.value = _appState.value.copy(
            profile = profile,
            favoriteIds = profileRepository.getFavoriteIds(),
            visitedIds = profileRepository.getVisitedIds(),
            signInPrompt = null
        )
        when (pendingAction?.action) {
            MuseumProfileAction.Save -> updateFavorite(pendingAction.museumId)
            MuseumProfileAction.MarkVisited -> updateVisited(pendingAction.museumId)
            null -> Unit
        }
    }

    fun signInFromOnboarding(provider: SignInProvider) {
        signIn(provider)
        completeOnboarding()
    }

    fun signOut() {
        profileRepository.signOut()
        _appState.value = _appState.value.copy(
            profile = null,
            favoriteIds = emptySet(),
            visitedIds = emptySet(),
            signInPrompt = null
        )
        applyPersonalFilters()
    }

    fun dismissSignInPrompt() {
        _appState.value = _appState.value.copy(signInPrompt = null)
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
        detailJob?.cancel()
        _museumDetails.value = null
        if (id == null) return
        detailJob = viewModelScope.launch {
            val location = _appState.value.mapUserLocation
            when (
                val response = repository.getMuseumDetails(
                    id = id,
                    referenceLatitude = location?.latitude ?: ZAGREB_LATITUDE,
                    referenceLongitude = location?.longitude ?: ZAGREB_LONGITUDE
                )
            ) {
                is Response.Success -> if (_appState.value.selectedMuseumId == id) {
                    _museumDetails.value = response.data
                }
                is Response.HttpError, is Response.Error -> Unit
            }
        }
    }

    fun selectMapMuseum(id: String?) {
        _appState.value = _appState.value.copy(mapSelectedMuseumId = id)
    }

    fun recenterMap() {
        _appState.value = _appState.value.copy(
            mapCameraRequestId = _appState.value.mapCameraRequestId + 1
        )
    }

    fun resolveMapLocation(location: MuseumMapLocation?) {
        val current = _appState.value
        if (location == null) {
            if (current.mapLocationAccess == MapLocationAccess.Unknown) {
                _appState.value = current.copy(mapLocationAccess = MapLocationAccess.Unavailable)
            }
            return
        }
        if (current.mapUserLocation == location && current.mapLocationAccess == MapLocationAccess.Granted) return

        _appState.value = current.copy(
            mapUserLocation = location,
            mapLocationAccess = MapLocationAccess.Granted,
            mapSelectedMuseumId = null,
            mapCameraRequestId = current.mapCameraRequestId + 1
        )
        reload()
    }

    fun updateSearch(query: String) {
        _appState.value = _appState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            refreshCatalog()
        }
    }

    fun selectCategory(category: MuseumCategory) {
        _appState.value = _appState.value.copy(selectedCategory = category)
        searchJob?.cancel()
        refreshCatalog()
    }

    fun selectPersonalFilter(filter: PersonalFilter) {
        _appState.value = _appState.value.copy(personalFilter = filter)
        applyPersonalFilters()
    }

    fun selectSort(sort: MuseumSort) {
        _appState.value = _appState.value.copy(sort = sort)
        searchJob?.cancel()
        refreshCatalog()
    }

    fun setFiltersVisible(visible: Boolean) {
        _appState.value = _appState.value.copy(showFilters = visible)
    }

    fun toggleFavorite(id: String) {
        if (_appState.value.profile == null) {
            _appState.value = _appState.value.copy(
                signInPrompt = MuseumSignInPrompt(id, MuseumProfileAction.Save)
            )
            return
        }
        updateFavorite(id)
    }

    private fun updateFavorite(id: String) {
        val favorites = _appState.value.favoriteIds.toggle(id)
        profileRepository.saveFavoriteIds(favorites)
        _appState.value = _appState.value.copy(favoriteIds = favorites)
        applyPersonalFilters()
    }

    fun toggleVisited(id: String) {
        if (_appState.value.profile == null) {
            _appState.value = _appState.value.copy(
                signInPrompt = MuseumSignInPrompt(id, MuseumProfileAction.MarkVisited)
            )
            return
        }
        updateVisited(id)
    }

    private fun updateVisited(id: String) {
        val visited = _appState.value.visitedIds.toggle(id)
        profileRepository.saveVisitedIds(visited)
        _appState.value = _appState.value.copy(visitedIds = visited)
        applyPersonalFilters()
    }

    fun setThemeMode(themeMode: ThemeMode) {
        _appState.value = _appState.value.copy(themeMode = themeMode)
    }

    fun reload() {
        catalogGeneration += 1
        catalogJob?.cancel()
        searchJob?.cancel()
        loadMuseums()
    }

    fun loadNextMuseumPage() {
        val current = _uiState.value as? MuseumsUiState.Success ?: return
        if (current.isLoadingMoreMuseums || !current.hasMoreMuseums) return

        _uiState.value = current.copy(
            isLoadingMoreMuseums = true,
            paginationError = null
        )
        catalogJob = viewModelScope.launch {
            val controls = _appState.value
            val generation = catalogGeneration
            when (
                val response = repository.getMuseumsPage(
                    cursor = nextMuseumCursor,
                    pageSize = MUSEUM_PAGE_SIZE,
                    referenceLatitude = controls.mapUserLocation?.latitude ?: ZAGREB_LATITUDE,
                    referenceLongitude = controls.mapUserLocation?.longitude ?: ZAGREB_LONGITUDE,
                    searchQuery = controls.searchQuery,
                    category = controls.selectedCategory,
                    sort = controls.sort.apiValue
                )
            ) {
                is Response.Success -> {
                    if (generation != catalogGeneration) return@launch
                    val latest = _uiState.value as? MuseumsUiState.Success ?: return@launch
                    val museums = (latest.allMuseums + response.data.museums).distinctBy(Museum::id)
                    nextMuseumCursor = response.data.nextCursor
                    _uiState.value = latest.copy(
                        allMuseums = museums,
                        hasMoreMuseums = response.data.hasMore,
                        isLoadingMoreMuseums = false,
                        paginationError = null
                    )
                    applyPersonalFilters()
                }
                is Response.HttpError -> setPaginationError(
                    "The museum catalog could not load the next page (HTTP ${response.statusCode})."
                )
                is Response.Error -> setPaginationError(
                    response.e.message ?: "More museums could not be loaded."
                )
            }
        }
    }

    private fun loadMuseums() {
        catalogJob = viewModelScope.launch {
            _uiState.value = MuseumsUiState.Loading
            nextMuseumCursor = null
            val controls = _appState.value

            val nearbyMuseums = when (
                val nearbyResponse = repository.getNearbyMuseums(
                    latitude = controls.mapUserLocation?.latitude ?: ZAGREB_LATITUDE,
                    longitude = controls.mapUserLocation?.longitude ?: ZAGREB_LONGITUDE
                )
            ) {
                is Response.Success -> nearbyResponse.data
                is Response.HttpError -> {
                    _uiState.value = MuseumsUiState.Error(
                        "The museum catalog is unavailable (HTTP ${nearbyResponse.statusCode})."
                    )
                    return@launch
                }
                is Response.Error -> {
                    _uiState.value = MuseumsUiState.Error(
                        nearbyResponse.e.message ?: "The museum catalog is unavailable."
                    )
                    return@launch
                }
            }

            when (
                val catalogResponse = repository.getMuseumsPage(
                    cursor = null,
                    pageSize = MUSEUM_PAGE_SIZE,
                    referenceLatitude = controls.mapUserLocation?.latitude ?: ZAGREB_LATITUDE,
                    referenceLongitude = controls.mapUserLocation?.longitude ?: ZAGREB_LONGITUDE,
                    searchQuery = controls.searchQuery,
                    category = controls.selectedCategory,
                    sort = controls.sort.apiValue
                )
            ) {
                is Response.Success -> {
                    nextMuseumCursor = catalogResponse.data.nextCursor
                    _uiState.value = MuseumsUiState.Success(
                        nearbyMuseums = nearbyMuseums,
                        allMuseums = catalogResponse.data.museums,
                        visibleMuseums = catalogResponse.data.museums,
                        hasMoreMuseums = catalogResponse.data.hasMore
                    )
                }
                is Response.HttpError -> _uiState.value = MuseumsUiState.Error(
                    "The museum catalog is unavailable (HTTP ${catalogResponse.statusCode})."
                )
                is Response.Error -> _uiState.value = MuseumsUiState.Error(
                    catalogResponse.e.message ?: "The museum catalog is unavailable."
                )
            }
            applyPersonalFilters()
        }
    }

    private fun refreshCatalog() {
        val current = _uiState.value as? MuseumsUiState.Success ?: return
        catalogGeneration += 1
        val generation = catalogGeneration
        catalogJob?.cancel()
        nextMuseumCursor = null
        _uiState.value = current.copy(
            allMuseums = emptyList(),
            visibleMuseums = emptyList(),
            hasMoreMuseums = true,
            isLoadingMoreMuseums = true,
            paginationError = null
        )
        catalogJob = viewModelScope.launch {
            val controls = _appState.value
            when (
                val response = repository.getMuseumsPage(
                    cursor = null,
                    pageSize = MUSEUM_PAGE_SIZE,
                    referenceLatitude = controls.mapUserLocation?.latitude ?: ZAGREB_LATITUDE,
                    referenceLongitude = controls.mapUserLocation?.longitude ?: ZAGREB_LONGITUDE,
                    searchQuery = controls.searchQuery,
                    category = controls.selectedCategory,
                    sort = controls.sort.apiValue
                )
            ) {
                is Response.Success -> {
                    if (generation != catalogGeneration) return@launch
                    val latest = _uiState.value as? MuseumsUiState.Success ?: return@launch
                    nextMuseumCursor = response.data.nextCursor
                    _uiState.value = latest.copy(
                        allMuseums = response.data.museums,
                        visibleMuseums = response.data.museums,
                        hasMoreMuseums = response.data.hasMore,
                        isLoadingMoreMuseums = false,
                        paginationError = null
                    )
                    applyPersonalFilters()
                }
                is Response.HttpError -> setPaginationError(
                    "The museum catalog could not apply these filters (HTTP ${response.statusCode})."
                )
                is Response.Error -> setPaginationError(
                    response.e.message ?: "The museum catalog could not apply these filters."
                )
            }
        }
    }

    private fun setPaginationError(message: String) {
        val current = _uiState.value as? MuseumsUiState.Success ?: return
        _uiState.value = current.copy(
            isLoadingMoreMuseums = false,
            paginationError = message
        )
    }

    private fun applyPersonalFilters() {
        val current = _uiState.value as? MuseumsUiState.Success ?: return
        val controls = _appState.value
        val filtered = current.allMuseums
            .asSequence()
            .filter { museum ->
                when (controls.personalFilter) {
                    PersonalFilter.All -> true
                    PersonalFilter.Favorites -> museum.id in controls.favoriteIds
                    PersonalFilter.Visited -> museum.id in controls.visitedIds
                    PersonalFilter.NotVisited -> museum.id !in controls.visitedIds
                }
            }
            .toList()
        _uiState.value = current.copy(visibleMuseums = filtered)
    }

    private fun Set<String>.toggle(id: String): Set<String> = if (id in this) this - id else this + id

    companion object {
        const val ZAGREB_LATITUDE = 45.8150
        const val ZAGREB_LONGITUDE = 15.9819
        const val MUSEUM_PAGE_SIZE = 20
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}

private val MuseumSort.apiValue: String
    get() = when (this) {
        MuseumSort.Distance -> "distance"
        MuseumSort.Alphabetical -> "alphabetical"
    }
