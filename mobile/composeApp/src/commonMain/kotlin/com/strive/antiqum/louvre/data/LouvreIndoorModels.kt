package com.strive.antiqum.louvre.data

import kotlinx.serialization.Serializable

const val LOUVRE_MUSEUM_ID = "Q19675"

@Serializable
data class LouvreLevel(
    val id: String,
    val label: String,
    val subtitle: String
)

@Serializable
data class LouvreNode(
    val id: String,
    val name: String,
    val shortName: String,
    val level: String,
    val wing: String,
    val kind: String,
    val x: Double,
    val y: Double,
    val searchableAliases: List<String> = emptyList()
)

@Serializable
data class LouvreEdge(
    val from: String,
    val to: String,
    val distanceMeters: Double,
    val accessible: Boolean,
    val kind: String,
    val instruction: String? = null
)

@Serializable
data class LouvreSight(
    val id: String,
    val title: String,
    val subtitle: String,
    val nodeId: String,
    val room: String,
    val wing: String,
    val level: String,
    val category: String,
    val sourceUrl: String,
    val imageUrl: String? = null,
    val imageCredit: String? = null,
    val featured: Boolean
)

@Serializable
data class LouvreClosureNotice(
    val id: String,
    val title: String,
    val detail: String,
    val status: String,
    val sourceUrl: String,
    val checkedAt: String
)

@Serializable
data class LouvreIndoorBootstrap(
    val museumId: String,
    val datasetVersion: String,
    val generatedAt: String,
    val levels: List<LouvreLevel>,
    val nodes: List<LouvreNode>,
    val edges: List<LouvreEdge>,
    val sights: List<LouvreSight>,
    val notices: List<LouvreClosureNotice>,
    val defaultStartNodeId: String,
    val officialClosuresUrl: String,
    val emergencyNotice: String,
    val attribution: String
)

@Serializable
data class LouvreRouteRequest(
    val fromNodeId: String,
    val toNodeId: String? = null,
    val sightId: String? = null,
    val nearestVisitorExit: Boolean = false,
    val accessible: Boolean = false
)

@Serializable
data class LouvreTourRequest(
    val fromNodeId: String,
    val sightIds: List<String>,
    val accessible: Boolean = false,
    val finishAtVisitorExit: Boolean = false
)

@Serializable
data class LouvreRouteStep(
    val index: Int,
    val title: String,
    val detail: String,
    val level: String,
    val nodeId: String,
    val kind: String,
    val distanceMeters: Double
)

@Serializable
data class LouvreRouteSegment(
    val level: String,
    val nodeIds: List<String>
)

@Serializable
data class LouvreRouteResult(
    val fromNodeId: String,
    val destinationNodeId: String,
    val destinationName: String,
    val accessible: Boolean,
    val distanceMeters: Int,
    val estimatedMinutes: Int,
    val nodeIds: List<String>,
    val steps: List<LouvreRouteStep>,
    val segments: List<LouvreRouteSegment>,
    val warning: String
)

@Serializable
data class LouvreTourResult(
    val orderedSightIds: List<String>,
    val orderedNodeIds: List<String>,
    val route: LouvreRouteResult,
    val skippedSightIds: List<String>
)
