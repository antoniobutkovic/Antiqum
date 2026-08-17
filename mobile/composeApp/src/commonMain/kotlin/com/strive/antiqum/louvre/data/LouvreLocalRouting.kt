package com.strive.antiqum.louvre.data

import kotlin.math.ceil
import kotlin.math.roundToInt

object LouvreLocalRouting {
    fun searchNodes(data: LouvreIndoorBootstrap, query: String, limit: Int = 12): List<LouvreNode> {
        val value = query.trim().lowercase()
        return data.nodes
            .map { node -> node to matchScore(value, listOf(node.name, node.shortName, node.wing, node.level) + node.searchableAliases) }
            .filter { (_, score) -> if (value.isEmpty()) true else score > 0 }
            .sortedWith(compareByDescending<Pair<LouvreNode, Int>> { it.second }.thenBy { it.first.name })
            .take(limit)
            .map { it.first }
    }

    fun searchSights(data: LouvreIndoorBootstrap, query: String, limit: Int = 30): List<LouvreSight> {
        val value = query.trim().lowercase()
        return data.sights
            .map { sight -> sight to matchScore(value, listOf(sight.title, sight.subtitle, sight.room, sight.wing, sight.category)) }
            .filter { (_, score) -> if (value.isEmpty()) true else score > 0 }
            .sortedWith(compareByDescending<Pair<LouvreSight, Int>> { it.second }.thenBy { it.first.title })
            .take(limit)
            .map { it.first }
    }

    fun calculateRoute(data: LouvreIndoorBootstrap, request: LouvreRouteRequest): LouvreRouteResult {
        val destinationId = when {
            request.sightId != null -> data.sights.firstOrNull { it.id == request.sightId }?.nodeId
                ?: error("Unknown Louvre sight")
            request.nearestVisitorExit -> {
                data.nodes
                    .filter { it.kind == "visitor_exit" }
                    .mapNotNull { exit ->
                        runCatching { shortestPath(data, request.fromNodeId, exit.id, request.accessible) }.getOrNull()
                    }
                    .minByOrNull { it.distanceMeters }
                    ?.nodeIds
                    ?.lastOrNull()
            }
            else -> request.toNodeId
        } ?: error("Choose a destination sight, location, or visitor exit")
        return routeResult(data, shortestPath(data, request.fromNodeId, destinationId, request.accessible), request.accessible)
    }

    fun optimizeTour(data: LouvreIndoorBootstrap, request: LouvreTourRequest): LouvreTourResult {
        require(request.sightIds.isNotEmpty()) { "Choose at least one favorite sight" }
        val sightsById = data.sights.associateBy { it.id }
        val selected = request.sightIds.distinct().mapNotNull(sightsById::get)
        require(selected.isNotEmpty()) { "No available favorite sights were found" }
        val nodeToSights = selected.groupBy { it.nodeId }
        val targets = nodeToSights.keys.toList()
        val orderedTargets = if (targets.size <= 11) {
            exactOpenTour(data, request.fromNodeId, targets, request.accessible)
        } else {
            greedyTour(data, request.fromNodeId, targets, request.accessible)
        }
        val routeTargets = orderedTargets.toMutableList()
        if (request.finishAtVisitorExit) {
            val finalStart = routeTargets.lastOrNull() ?: request.fromNodeId
            data.nodes
                .filter { it.kind == "visitor_exit" }
                .mapNotNull { exit -> runCatching { exit.id to shortestPath(data, finalStart, exit.id, request.accessible) }.getOrNull() }
                .minByOrNull { it.second.distanceMeters }
                ?.first
                ?.let(routeTargets::add)
        }
        val combined = combinePaths(data, request.fromNodeId, routeTargets, request.accessible)
        return LouvreTourResult(
            orderedSightIds = orderedTargets.flatMap { nodeId -> nodeToSights[nodeId].orEmpty().map { it.id } },
            orderedNodeIds = orderedTargets,
            route = routeResult(data, combined, request.accessible),
            skippedSightIds = request.sightIds.filterNot(sightsById::containsKey)
        )
    }

    private data class Path(
        val nodeIds: List<String>,
        val edges: List<LouvreEdge>,
        val distanceMeters: Double
    )

    private fun shortestPath(data: LouvreIndoorBootstrap, startId: String, endId: String, accessible: Boolean): Path {
        val nodesById = data.nodes.associateBy { it.id }
        require(nodesById.containsKey(startId)) { "Unknown starting location" }
        require(nodesById.containsKey(endId)) { "Unknown destination" }
        if (startId == endId) return Path(listOf(startId), emptyList(), 0.0)
        val adjacency = mutableMapOf<String, MutableList<LouvreEdge>>()
        data.edges.filterNot { accessible && !it.accessible }.forEach { edge ->
            adjacency.getOrPut(edge.from, ::mutableListOf).add(edge)
            adjacency.getOrPut(edge.to, ::mutableListOf).add(edge.copy(from = edge.to, to = edge.from))
        }
        val distances = mutableMapOf(startId to 0.0)
        val previous = mutableMapOf<String, Pair<String, LouvreEdge>>()
        val pending = nodesById.keys.toMutableSet()
        while (pending.isNotEmpty()) {
            val current = pending.minByOrNull { distances[it] ?: Double.POSITIVE_INFINITY } ?: break
            val currentDistance = distances[current] ?: Double.POSITIVE_INFINITY
            if (!currentDistance.isFinite()) break
            pending.remove(current)
            if (current == endId) break
            adjacency[current].orEmpty().forEach { edge ->
                if (edge.to in pending) {
                    val proposed = currentDistance + edge.distanceMeters
                    if (proposed < (distances[edge.to] ?: Double.POSITIVE_INFINITY)) {
                        distances[edge.to] = proposed
                        previous[edge.to] = current to edge
                    }
                }
            }
        }
        require(previous.containsKey(endId)) { "No suitable route is available for these options" }
        val nodeIds = mutableListOf(endId)
        val traversed = mutableListOf<LouvreEdge>()
        var cursor = endId
        while (cursor != startId) {
            val item = previous[cursor] ?: error("Route data is incomplete")
            traversed.add(item.second)
            cursor = item.first
            nodeIds.add(cursor)
        }
        return Path(nodeIds.reversed(), traversed.reversed(), distances[endId] ?: 0.0)
    }

    private fun routeResult(data: LouvreIndoorBootstrap, path: Path, accessible: Boolean): LouvreRouteResult {
        val nodesById = data.nodes.associateBy { it.id }
        val destination = nodesById.getValue(path.nodeIds.last())
        val steps = path.edges.mapIndexed { index, edge ->
            val node = nodesById.getValue(edge.to)
            LouvreRouteStep(
                index = index + 1,
                title = edge.instruction ?: when (edge.kind) {
                    "lift" -> "Take the lift to ${node.shortName}"
                    "stairs" -> "Use the stairs to ${node.shortName}"
                    "escalator" -> "Use the escalator toward ${node.shortName}"
                    else -> "Continue to ${node.shortName}"
                },
                detail = "${edge.distanceMeters.roundToInt()} m · ${node.wing} · Level ${node.level}",
                level = node.level,
                nodeId = node.id,
                kind = edge.kind,
                distanceMeters = edge.distanceMeters
            )
        } + LouvreRouteStep(
            index = path.edges.size + 1,
            title = "Arrive at ${destination.shortName}",
            detail = destination.name,
            level = destination.level,
            nodeId = destination.id,
            kind = "arrive",
            distanceMeters = 0.0
        )
        val segments = mutableListOf<LouvreRouteSegment>()
        path.nodeIds.forEach { id ->
            val level = nodesById.getValue(id).level
            val last = segments.lastOrNull()
            if (last?.level == level) {
                segments[segments.lastIndex] = last.copy(nodeIds = last.nodeIds + id)
            } else {
                segments += LouvreRouteSegment(level, listOf(id))
            }
        }
        return LouvreRouteResult(
            fromNodeId = path.nodeIds.first(),
            destinationNodeId = destination.id,
            destinationName = destination.name,
            accessible = accessible,
            distanceMeters = path.distanceMeters.roundToInt(),
            estimatedMinutes = maxOf(1, ceil(path.distanceMeters / if (accessible) 55.0 else 70.0).toInt()),
            nodeIds = path.nodeIds,
            steps = steps,
            segments = segments,
            warning = "Visitor guidance only. Check current room closures and follow Louvre signs and staff instructions."
        )
    }

    private fun combinePaths(
        data: LouvreIndoorBootstrap,
        startId: String,
        targets: List<String>,
        accessible: Boolean
    ): Path {
        val nodes = mutableListOf(startId)
        val edges = mutableListOf<LouvreEdge>()
        var distance = 0.0
        var cursor = startId
        targets.forEach { target ->
            val path = shortestPath(data, cursor, target, accessible)
            nodes += path.nodeIds.drop(1)
            edges += path.edges
            distance += path.distanceMeters
            cursor = target
        }
        return Path(nodes, edges, distance)
    }

    private data class TourState(val cost: Double, val previous: Int?)

    private fun exactOpenTour(
        data: LouvreIndoorBootstrap,
        startId: String,
        targets: List<String>,
        accessible: Boolean
    ): List<String> {
        val count = targets.size
        val startDistances = targets.map { shortestPath(data, startId, it, accessible).distanceMeters }
        val between = List(count) { from ->
            List(count) { to -> if (from == to) 0.0 else shortestPath(data, targets[from], targets[to], accessible).distanceMeters }
        }
        val states = mutableMapOf<Pair<Int, Int>, TourState>()
        repeat(count) { index -> states[(1 shl index) to index] = TourState(startDistances[index], null) }
        for (mask in 1 until (1 shl count)) {
            repeat(count) { last ->
                val current = states[mask to last] ?: return@repeat
                repeat(count) { next ->
                    if (mask and (1 shl next) == 0) {
                        val nextMask = mask or (1 shl next)
                        val cost = current.cost + between[last][next]
                        val key = nextMask to next
                        if (cost < (states[key]?.cost ?: Double.POSITIVE_INFINITY)) {
                            states[key] = TourState(cost, last)
                        }
                    }
                }
            }
        }
        val fullMask = (1 shl count) - 1
        var last = (0 until count).minBy { states[fullMask to it]?.cost ?: Double.POSITIVE_INFINITY }
        var mask = fullMask
        val reversed = mutableListOf<Int>()
        while (true) {
            reversed += last
            val previous = states[mask to last]?.previous ?: break
            mask = mask xor (1 shl last)
            last = previous
        }
        return reversed.reversed().map(targets::get)
    }

    private fun greedyTour(
        data: LouvreIndoorBootstrap,
        startId: String,
        targets: List<String>,
        accessible: Boolean
    ): List<String> {
        val remaining = targets.toMutableSet()
        val ordered = mutableListOf<String>()
        var cursor = startId
        while (remaining.isNotEmpty()) {
            val next = remaining.minBy { shortestPath(data, cursor, it, accessible).distanceMeters }
            ordered += next
            remaining -= next
            cursor = next
        }
        return ordered
    }

    private fun matchScore(query: String, values: List<String>): Int {
        if (query.isEmpty()) return 1
        val normalized = values.map(String::lowercase)
        return when {
            normalized.any { it == query } -> 100
            normalized.any { it.startsWith(query) } -> 70
            normalized.any { it.contains(query) } -> 40
            query.split(' ').filter(String::isNotBlank).let { tokens -> normalized.any { value -> tokens.all(value::contains) } } -> 20
            else -> 0
        }
    }
}
