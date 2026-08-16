package com.strive.antiqum.museums.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumCard
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumDimens
import com.strive.antiqum.designsystem.AntiqumFilterChip
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.designsystem.AntiqumSearchField
import com.strive.antiqum.designsystem.AntiqumSecondaryButton
import com.strive.antiqum.designsystem.AntiqumSectionLabel
import com.strive.antiqum.designsystem.MuseumImage
import com.strive.antiqum.museums.data.Museum
import com.strive.antiqum.museums.data.MuseumCategory
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MuseumsScreen(
    uiState: MuseumsUiState,
    appState: AntiqumAppState,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (MuseumCategory) -> Unit,
    onPersonalFilterSelected: (PersonalFilter) -> Unit,
    onSortSelected: (MuseumSort) -> Unit,
    onToggleFilters: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectMuseum: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        MuseumsUiState.Loading -> MuseumsLoading(modifier)
        is MuseumsUiState.Error -> MuseumsError(uiState.message, onRetry, modifier)
        is MuseumsUiState.Success -> {
            val museumListState = rememberLazyListState()
            LaunchedEffect(
                museumListState,
                uiState.hasMoreMuseums,
                uiState.isLoadingMoreMuseums,
                uiState.paginationError
            ) {
                snapshotFlow {
                    val layout = museumListState.layoutInfo
                    val lastVisibleIndex = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
                    layout.totalItemsCount > 0 && lastVisibleIndex >= layout.totalItemsCount - 4
                }
                    .distinctUntilChanged()
                    .collect { nearEnd ->
                        if (
                            nearEnd &&
                            uiState.hasMoreMuseums &&
                            !uiState.isLoadingMoreMuseums &&
                            uiState.paginationError == null
                        ) {
                            onLoadMore()
                        }
                    }
            }

            Column(modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.padding(
                        start = AntiqumDimens.ScreenPadding,
                        top = 24.dp,
                        end = AntiqumDimens.ScreenPadding
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Museums", style = MaterialTheme.typography.displayMedium)
                            Text(
                                "${uiState.allMuseums.size} museums loaded worldwide",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        IconButton(
                            onClick = onToggleFilters,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                        ) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "Advanced filters",
                                tint = if (appState.showFilters || appState.personalFilter != PersonalFilter.All) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    AntiqumSearchField(
                        value = appState.searchQuery,
                        onValueChange = onSearchChange
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = AntiqumDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    items(MuseumCategory.entries) { category ->
                        AntiqumFilterChip(
                            label = category.label,
                            selected = appState.selectedCategory == category,
                            onClick = { onCategorySelected(category) }
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(
                        start = AntiqumDimens.ScreenPadding,
                        top = 18.dp,
                        end = AntiqumDimens.ScreenPadding,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                    state = museumListState
                ) {
                    if (uiState.visibleMuseums.isEmpty()) {
                        item {
                            MuseumsEmptyState(appState = appState)
                        }
                    } else {
                        item {
                            Text(
                                when (appState.sort) {
                                    MuseumSort.Distance -> "Distance from Zagreb · Wikidata"
                                    MuseumSort.Alphabetical -> "Alphabetical · Wikidata"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        items(uiState.visibleMuseums, key = Museum::id) { museum ->
                            MuseumListCard(
                                museum = museum,
                                isFavorite = museum.id in appState.favoriteIds,
                                isVisited = museum.id in appState.visitedIds,
                                onToggleFavorite = { onToggleFavorite(museum.id) },
                                onClick = { onSelectMuseum(museum.id) }
                            )
                        }
                    }

                    item {
                        PaginationFooter(
                            isLoading = uiState.isLoadingMoreMuseums,
                            error = uiState.paginationError,
                            onRetry = onLoadMore
                        )
                    }
                }
            }

            if (appState.showFilters) {
                AdvancedFiltersSheet(
                    personalFilter = appState.personalFilter,
                    sort = appState.sort,
                    onPersonalFilterSelected = onPersonalFilterSelected,
                    onSortSelected = onSortSelected,
                    onDismiss = onToggleFilters
                )
            }
        }
    }
}

@Composable
private fun PaginationFooter(
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            error != null -> {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                AntiqumSecondaryButton(
                    label = "Try again",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFiltersSheet(
    personalFilter: PersonalFilter,
    sort: MuseumSort,
    onPersonalFilterSelected: (PersonalFilter) -> Unit,
    onSortSelected: (MuseumSort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            Modifier.padding(
                start = AntiqumDimens.ScreenPadding,
                end = AntiqumDimens.ScreenPadding,
                bottom = 24.dp
            )
        ) {
            Text("Filters", style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(22.dp))
            AntiqumSectionLabel("Collection")
            Spacer(Modifier.height(9.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                PersonalFilter.entries.forEach { filter ->
                    AntiqumFilterChip(
                        label = filter.label,
                        selected = personalFilter == filter,
                        onClick = { onPersonalFilterSelected(filter) }
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            AntiqumSectionLabel("Sort by")
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MuseumSort.entries.forEach { option ->
                    AntiqumFilterChip(
                        label = option.label,
                        selected = sort == option,
                        onClick = { onSortSelected(option) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            AntiqumPrimaryButton(
                label = "Done",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun MuseumListCard(
    museum: Museum,
    isFavorite: Boolean,
    isVisited: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AntiqumCard(modifier = modifier, onClick = onClick) {
        Row(modifier = Modifier.padding(10.dp)) {
            MuseumImage(
                imageUrl = museum.imageUrl,
                contentDescription = museum.name,
                modifier = Modifier
                    .size(width = 112.dp, height = 98.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        museum.name,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = AntiqumColors.Bronze,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Text(
                    museum.locationLabel.ifBlank { "Location unavailable" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${museum.category.label} · ${museum.distanceKm.formatDistance()}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isVisited) {
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Visited",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MuseumsEmptyState(appState: AntiqumAppState, modifier: Modifier = Modifier) {
    val (title, copy) = when {
        appState.searchQuery.isNotBlank() -> "No museums found" to "Try another museum, city, country, or category."
        appState.personalFilter == PersonalFilter.Favorites ->
            "No favorites yet" to "Save museums you would like to visit by tapping the heart icon."
        appState.personalFilter == PersonalFilter.Visited ->
            "No museums visited yet" to "Start exploring museums and build your personal museum history."
        else -> "No museums match" to "Try adjusting the active category or filters."
    }
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Museum,
                contentDescription = null,
                modifier = Modifier.size(58.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                copy,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

internal fun Double.formatDistance(): String = if (this < 10.0) {
    "${(this * 10).toInt() / 10.0} km"
} else {
    "${toInt()} km"
}
