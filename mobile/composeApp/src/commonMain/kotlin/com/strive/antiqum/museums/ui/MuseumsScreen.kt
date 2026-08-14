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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumCard
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumDimens
import com.strive.antiqum.designsystem.AntiqumFilterChip
import com.strive.antiqum.designsystem.AntiqumSearchField
import com.strive.antiqum.designsystem.AntiqumSectionLabel
import com.strive.antiqum.designsystem.MuseumImage
import com.strive.antiqum.museums.data.Museum
import com.strive.antiqum.museums.data.MuseumCategory

@Composable
fun MuseumsScreen(
    uiState: MuseumsUiState,
    appState: AntiqumAppState,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (MuseumCategory) -> Unit,
    onPersonalFilterSelected: (PersonalFilter) -> Unit,
    onSortSelected: (MuseumSort) -> Unit,
    onToggleFilters: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectMuseum: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        MuseumsUiState.Loading -> MuseumsLoading(modifier)
        is MuseumsUiState.Error -> MuseumsError(uiState.message, onRetry, modifier)
        is MuseumsUiState.Success -> Column(modifier.fillMaxSize()) {
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
                            "${uiState.allMuseums.size} places near Zagreb",
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

            if (appState.showFilters) {
                AdvancedFilters(
                    personalFilter = appState.personalFilter,
                    sort = appState.sort,
                    onPersonalFilterSelected = onPersonalFilterSelected,
                    onSortSelected = onSortSelected,
                    modifier = Modifier.padding(horizontal = AntiqumDimens.ScreenPadding, vertical = 12.dp)
                )
            }

            if (uiState.visibleMuseums.isEmpty()) {
                MuseumsEmptyState(
                    appState = appState,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = AntiqumDimens.ScreenPadding,
                        top = 18.dp,
                        end = AntiqumDimens.ScreenPadding,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            when (appState.sort) {
                                MuseumSort.Distance -> "Nearby · Wikidata"
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
            }
        }
    }
}

@Composable
private fun AdvancedFilters(
    personalFilter: PersonalFilter,
    sort: MuseumSort,
    onPersonalFilterSelected: (PersonalFilter) -> Unit,
    onSortSelected: (MuseumSort) -> Unit,
    modifier: Modifier = Modifier
) {
    AntiqumCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
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
                    museum.locationLabel.ifBlank { "Near Zagreb, Croatia" },
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
