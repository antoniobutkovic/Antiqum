package com.strive.antiqum.louvre.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumFilterChip
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.designsystem.AntiqumSearchField
import com.strive.antiqum.designsystem.AntiqumSecondaryButton
import com.strive.antiqum.louvre.data.LouvreIndoorBootstrap
import com.strive.antiqum.louvre.data.LouvreNode
import com.strive.antiqum.louvre.data.LouvreRouteResult
import com.strive.antiqum.louvre.data.LouvreSight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun LouvreIndoorScreen(
    viewModel: LouvreIndoorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        LouvreGuideHeader(onBack)
        when {
            state.isLoading -> LouvreGuideLoading()
            state.error != null || state.bootstrap == null -> LouvreGuideError(
                message = state.error ?: "The Louvre guide could not be loaded.",
                onRetry = viewModel::load
            )
            else -> {
                val data = state.bootstrap ?: return@Column
                GuideNotice(data.emergencyNotice)
                state.message?.let { message ->
                    StatusMessage(message = message, onDismiss = viewModel::clearMessage)
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LouvreIndoorTab.entries) { tab ->
                        AntiqumFilterChip(
                            label = tab.label,
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) }
                        )
                    }
                }
                when (state.selectedTab) {
                    LouvreIndoorTab.Map -> LouvreMapPanel(
                        data = data,
                        state = state,
                        locationMatches = if (state.locationQuery.isBlank()) emptyList() else viewModel.locationMatches(),
                        onLocationQueryChange = viewModel::updateLocationQuery,
                        onSelectLevel = viewModel::selectLevel,
                        onSetLocation = viewModel::selectCurrentLocation,
                        onFavorite = viewModel::toggleFavorite,
                        onNavigate = viewModel::navigateToSight,
                        onFindExit = viewModel::findNearestExit
                    )
                    LouvreIndoorTab.Sights -> LouvreSightsPanel(
                        data = data,
                        state = state,
                        matches = viewModel.sightMatches(),
                        onQueryChange = viewModel::updateSightQuery,
                        onFavorite = viewModel::toggleFavorite,
                        onNavigate = viewModel::navigateToSight,
                        onOptimize = viewModel::optimizeFavorites
                    )
                    LouvreIndoorTab.Route -> LouvreRoutePanel(
                        data = data,
                        state = state,
                        locationMatches = if (state.locationQuery.isBlank()) emptyList() else viewModel.locationMatches(),
                        onLocationQueryChange = viewModel::updateLocationQuery,
                        onSetLocation = viewModel::selectCurrentLocation,
                        onSelectLevel = viewModel::selectLevel,
                        onAccessibleChange = viewModel::setAccessible,
                        onFinishAtExitChange = viewModel::setFinishTourAtExit,
                        onFindExit = viewModel::findNearestExit,
                        onOptimize = viewModel::optimizeFavorites,
                        onClearRoute = viewModel::clearRoute
                    )
                }
            }
        }
    }
}

@Composable
private fun LouvreGuideHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
        }
        Column(Modifier.weight(1f)) {
            Text("Louvre indoor guide", style = MaterialTheme.typography.titleLarge)
            Text(
                "Original Antiqum schematic · visitor guidance",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(Icons.Outlined.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun GuideNotice(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusMessage(message: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun LouvreMapPanel(
    data: LouvreIndoorBootstrap,
    state: LouvreIndoorUiState,
    locationMatches: List<LouvreNode>,
    onLocationQueryChange: (String) -> Unit,
    onSelectLevel: (String) -> Unit,
    onSetLocation: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onFindExit: () -> Unit
) {
    val floorSights = data.sights.filter { it.level == state.selectedLevel }.sortedBy(LouvreSight::mapNumber)
    val floorRooms = data.nodes.count { it.level == state.selectedLevel && it.kind == "room" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LevelSelector(data, state.selectedLevel, onSelectLevel) }
        item {
            Text(
                "$floorRooms searchable locations · ${floorSights.size} numbered sights on this floor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            AntiqumSearchField(
                value = state.locationQuery,
                onValueChange = onLocationQueryChange,
                placeholder = "Enter your room, sight, wing or landmark"
            )
        }
        items(locationMatches.take(6), key = LouvreNode::id) { node ->
            LocationResultCard(node = node, onClick = { onSetLocation(node.id) })
        }
        item {
            CurrentLocationCard(data.nodes.firstOrNull { it.id == state.currentNodeId })
        }
        item {
            AntiqumPrimaryButton(
                label = "Directions to visitor exit",
                icon = Icons.AutoMirrored.Outlined.ExitToApp,
                onClick = onFindExit,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                "Tap any map point to set where you are. Numbered bronze markers match the sights below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            LouvreFloorMap(
                data = data,
                level = state.selectedLevel,
                currentNodeId = state.currentNodeId,
                favoriteSightIds = state.favoriteSightIds,
                route = state.activeRoute,
                onNodeTapped = onSetLocation
            )
        }
        item { MapLegend() }
        item {
            Text("Numbered sights on this floor", style = MaterialTheme.typography.headlineMedium)
        }
        if (floorSights.isEmpty()) {
            item { Text("No numbered visitor sights on this level.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(floorSights, key = LouvreSight::id) { sight ->
                LouvreSightCard(
                    sight = sight,
                    isFavorite = sight.id in state.favoriteSightIds,
                    onFavorite = { onFavorite(sight.id) },
                    onNavigate = { onNavigate(sight.id) }
                )
            }
        }
        item { AttributionCard(data) }
    }
}

@Composable
private fun LouvreSightsPanel(
    data: LouvreIndoorBootstrap,
    state: LouvreIndoorUiState,
    matches: List<LouvreSight>,
    onQueryChange: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onOptimize: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AntiqumSearchField(
                value = state.sightQuery,
                onValueChange = onQueryChange,
                placeholder = "Search Mona Lisa, room 711, wing…"
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "${state.favoriteSightIds.size} favorite${if (state.favoriteSightIds.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    AntiqumPrimaryButton(
                        label = "Build best route",
                        icon = Icons.Outlined.Route,
                        onClick = onOptimize,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        items(matches, key = LouvreSight::id) { sight ->
            LouvreSightCard(
                sight = sight,
                isFavorite = sight.id in state.favoriteSightIds,
                onFavorite = { onFavorite(sight.id) },
                onNavigate = { onNavigate(sight.id) }
            )
        }
        item {
            Text(
                "All ${data.sights.size} numbered Antiqum highlights are shown. The guide also includes ${data.nodes.count { it.kind == "room" }} searchable official Louvre locations. Rooms can close or change; verify access in the Louvre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LouvreRoutePanel(
    data: LouvreIndoorBootstrap,
    state: LouvreIndoorUiState,
    locationMatches: List<LouvreNode>,
    onLocationQueryChange: (String) -> Unit,
    onSetLocation: (String) -> Unit,
    onSelectLevel: (String) -> Unit,
    onAccessibleChange: (Boolean) -> Unit,
    onFinishAtExitChange: (Boolean) -> Unit,
    onFindExit: () -> Unit,
    onOptimize: () -> Unit,
    onClearRoute: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Where are you now?", style = MaterialTheme.typography.headlineMedium) }
        item {
            AntiqumSearchField(
                value = state.locationQuery,
                onValueChange = onLocationQueryChange,
                placeholder = "Room number, artwork, wing or landmark"
            )
        }
        items(locationMatches, key = LouvreNode::id) { node ->
            LocationResultCard(node = node, onClick = { onSetLocation(node.id) })
        }
        item { CurrentLocationCard(data.nodes.firstOrNull { it.id == state.currentNodeId }) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AntiqumFilterChip(
                        label = "Accessible route",
                        selected = state.accessible,
                        onClick = { onAccessibleChange(!state.accessible) }
                    )
                }
                item {
                    AntiqumFilterChip(
                        label = "Tour ends at exit",
                        selected = state.finishTourAtExit,
                        onClick = { onFinishAtExitChange(!state.finishTourAtExit) }
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                AntiqumPrimaryButton(
                    label = "Nearest exit",
                    icon = Icons.AutoMirrored.Outlined.ExitToApp,
                    onClick = onFindExit,
                    modifier = Modifier.weight(1f)
                )
                AntiqumSecondaryButton(
                    label = "Favorite tour",
                    icon = Icons.Outlined.Route,
                    onClick = onOptimize,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (state.isWorking) {
            item {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp))
                }
            }
        }
        state.activeRoute?.let { route ->
            item {
                RouteSummary(route = route, onClear = onClearRoute)
            }
            if (state.orderedTourSightIds.isNotEmpty()) {
                item {
                    OrderedTour(data, state.orderedTourSightIds)
                }
            }
            item { LevelSelector(data, state.selectedLevel, onSelectLevel) }
            item {
                LouvreFloorMap(
                    data = data,
                    level = state.selectedLevel,
                    currentNodeId = state.currentNodeId,
                    favoriteSightIds = state.favoriteSightIds,
                    route = route,
                    onNodeTapped = onSetLocation
                )
            }
            item { Text("Walking instructions", style = MaterialTheme.typography.headlineMedium) }
            items(route.steps, key = { "${it.index}-${it.nodeId}" }) { step ->
                RouteStepCard(
                    index = step.index,
                    title = step.title,
                    detail = step.detail,
                    isArrival = step.kind == "arrive"
                )
            }
            item {
                Text(
                    route.warning,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            val closures = data.notices.firstOrNull()
            if (closures != null) ClosureCard(closures.title, closures.detail, closures.sourceUrl)
        }
    }
}

@Composable
private fun LouvreFloorMap(
    data: LouvreIndoorBootstrap,
    level: String,
    currentNodeId: String?,
    favoriteSightIds: Set<String>,
    route: LouvreRouteResult?,
    onNodeTapped: (String) -> Unit
) {
    val nodes = data.nodes.filter { it.level == level }
    val byId = data.nodes.associateBy { it.id }
    val infrastructureEdges = data.edges.filter { edge ->
        val from = byId[edge.from]
        val to = byId[edge.to]
        from?.level == level && to?.level == level && (from.kind != "room" || to.kind != "room")
    }
    val routePairs = route?.nodeIds.orEmpty().zipWithNext().filter { (from, to) -> byId[from]?.level == level && byId[to]?.level == level }
    val floorSights = data.sights.filter { it.level == level }.sortedBy(LouvreSight::mapNumber)
    val sightsByNode = floorSights.groupBy(LouvreSight::nodeId)
    val favoriteNodeIds = data.sights.filter { it.id in favoriteSightIds }.mapTo(mutableSetOf()) { it.nodeId }
    val outline = MaterialTheme.colorScheme.outline
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val roomColor = AntiqumColors.Bronze
    val junctionColor = MaterialTheme.colorScheme.onSurfaceVariant
    val routeColor = MaterialTheme.colorScheme.primary
    val exitColor = Color(0xFF2E7D32)
    val currentColor = Color(0xFF1976D2)
    val textMeasurer = rememberTextMeasurer()
    val numberStyle = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    val wingColors = mapOf(
        "Denon" to Color(0xFF4C956C),
        "Richelieu" to Color(0xFF9C6644),
        "Sully" to Color(0xFF577590)
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = surface,
        border = BorderStroke(1.dp, outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
                .padding(12.dp)
                .pointerInput(nodes) {
                    detectTapGestures { tap ->
                        val nearest = nodes.minByOrNull { node ->
                            val dx = tap.x - node.x.toFloat() / 100f * size.width
                            val dy = tap.y - node.y.toFloat() / 100f * size.height
                            sqrt(dx * dx + dy * dy)
                        }
                        nearest?.let { node ->
                            val dx = tap.x - node.x.toFloat() / 100f * size.width
                            val dy = tap.y - node.y.toFloat() / 100f * size.height
                            if (sqrt(dx * dx + dy * dy) <= 34.dp.toPx()) onNodeTapped(node.id)
                        }
                    }
                }
        ) {
            fun point(node: LouvreNode) = Offset(node.x.toFloat() / 100f * size.width, node.y.toFloat() / 100f * size.height)

            nodes.filter { it.kind == "room" }.groupBy(LouvreNode::wing).forEach { (wing, wingNodes) ->
                if (wingNodes.isEmpty()) return@forEach
                val points = wingNodes.map(::point)
                val left = (points.minOf(Offset::x) - 8f).coerceAtLeast(0f)
                val top = (points.minOf(Offset::y) - 8f).coerceAtLeast(0f)
                val right = (points.maxOf(Offset::x) + 8f).coerceAtMost(size.width)
                val bottom = (points.maxOf(Offset::y) + 8f).coerceAtMost(size.height)
                drawRoundRect(
                    color = wingColors[wing]?.copy(alpha = 0.07f) ?: outline.copy(alpha = 0.05f),
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(16f, 16f)
                )
            }

            infrastructureEdges.forEach { edge ->
                val from = byId[edge.from] ?: return@forEach
                val to = byId[edge.to] ?: return@forEach
                drawLine(outline.copy(alpha = 0.35f), point(from), point(to), strokeWidth = 3f, cap = StrokeCap.Round)
            }
            routePairs.forEach { (fromId, toId) ->
                val from = byId[fromId] ?: return@forEach
                val to = byId[toId] ?: return@forEach
                drawLine(routeColor.copy(alpha = 0.2f), point(from), point(to), strokeWidth = 12f, cap = StrokeCap.Round)
                drawLine(routeColor, point(from), point(to), strokeWidth = 6f, cap = StrokeCap.Round)
            }
            nodes.forEach { node ->
                val center = point(node)
                val color = when {
                    node.id == currentNodeId -> currentColor
                    node.kind == "visitor_exit" -> exitColor
                    node.kind == "lift" -> currentColor.copy(alpha = 0.75f)
                    node.kind == "room" -> wingColors[node.wing]?.copy(alpha = 0.55f) ?: junctionColor.copy(alpha = 0.55f)
                    else -> junctionColor.copy(alpha = 0.7f)
                }
                if (node.id in favoriteNodeIds) drawCircle(roomColor, radius = 14f, center = center, style = Stroke(width = 4f))
                val nodeRadius = when {
                    node.id == currentNodeId || node.kind == "visitor_exit" -> 8f
                    node.kind == "room" -> 2.6f
                    else -> 5f
                }
                drawCircle(color, radius = nodeRadius, center = center)
                if (node.id == currentNodeId || node.kind == "visitor_exit") drawCircle(surface, radius = 2.5f, center = center)

                sightsByNode[node.id].orEmpty().forEachIndexed { index, sight ->
                    val count = sightsByNode[node.id].orEmpty().size
                    val angle = -PI / 2 + (2 * PI * index / count.coerceAtLeast(1))
                    val clusterRadius = if (count > 1) 13f else 0f
                    val badgeCenter = center + Offset(
                        (cos(angle) * clusterRadius).toFloat(),
                        (sin(angle) * clusterRadius).toFloat()
                    )
                    drawCircle(roomColor, radius = 10.5f, center = badgeCenter)
                    if (sight.id in favoriteSightIds) drawCircle(onSurface, radius = 13f, center = badgeCenter, style = Stroke(width = 2f))
                    val layout = textMeasurer.measure(sight.mapNumber.toString(), numberStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = badgeCenter - Offset(layout.size.width / 2f, layout.size.height / 2f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelSelector(data: LouvreIndoorBootstrap, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        items(data.levels, key = { it.id }) { level ->
            AntiqumFilterChip(label = level.label, selected = level.id == selected, onClick = { onSelect(level.id) })
        }
    }
}

@Composable
private fun MapLegend() {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item { LegendItem(Color(0xFF1976D2), "You are here / lift") }
        item { LegendItem(AntiqumColors.Bronze, "Numbered sight") }
        item { LegendItem(Color(0xFF577590), "Searchable room") }
        item { LegendItem(Color(0xFF2E7D32), "Visitor exit") }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LocationResultCard(node: LouvreNode, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(node.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${node.wing} · Level ${node.level}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CurrentLocationCard(node: LouvreNode?) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Current starting point", style = MaterialTheme.typography.labelLarge)
                Text(
                    node?.let { "${it.name} · Level ${it.level}" } ?: "Choose where you are",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LouvreSightCard(
    sight: LouvreSight,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onNavigate: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("#${sight.mapNumber}  ${sight.title}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        sight.subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Room ${sight.room} · ${sight.wing} · Level ${sight.level}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Favorite sight",
                        tint = if (isFavorite) AntiqumColors.Bronze else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AntiqumSecondaryButton(
                    label = "Navigate here",
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { uriHandler.openUri(sight.sourceUrl) }) { Text("Official details") }
            }
        }
    }
}

@Composable
private fun RouteSummary(route: LouvreRouteResult, onClear: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(route.destinationName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${route.distanceMeters} m · about ${route.estimatedMinutes} min${if (route.accessible) " · accessible" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClear) { Text("Clear") }
            }
        }
    }
}

@Composable
private fun OrderedTour(data: LouvreIndoorBootstrap, ids: List<String>) {
    val sights = data.sights.associateBy { it.id }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Optimized sight order", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            ids.forEachIndexed { index, id ->
                sights[id]?.let { sight ->
                    Text("${index + 1}. #${sight.mapNumber} ${sight.title} · room ${sight.room}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun RouteStepCard(index: Int, title: String, detail: String, isArrival: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = if (isArrival) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    index.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isArrival) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).padding(top = 2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ClosureCard(title: String, detail: String, sourceUrl: String) {
    val uriHandler = LocalUriHandler.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { uriHandler.openUri(sourceUrl) }) { Text("Check official gallery access") }
        }
    }
}

@Composable
private fun AttributionCard(data: LouvreIndoorBootstrap) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(13.dp)) {
            Text("About this map", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(data.attribution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Dataset ${data.datasetVersion}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LouvreGuideLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Preparing the Louvre floor guide…")
        }
    }
}

@Composable
private fun LouvreGuideError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Indoor guide unavailable", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(7.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            AntiqumPrimaryButton(label = "Try again", onClick = onRetry)
        }
    }
}
