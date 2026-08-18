package com.strive.antiqum.louvre.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LouvreLocalRoutingTest {
    @Test
    fun searchReturnsAllNumberedSightsAndFindsCurrentRoomByArtwork() {
        val data = fixture()

        assertEquals(listOf("painting", "sculpture"), LouvreLocalRouting.searchSights(data, "").map { it.id })
        assertEquals("room-1", LouvreLocalRouting.searchNodes(data, "Painting").first().id)
    }

    @Test
    fun accessibleRouteUsesLiftAndReachesVisitorExit() {
        val route = LouvreLocalRouting.calculateRoute(
            data = fixture(),
            request = LouvreRouteRequest(
                fromNodeId = "room-1",
                nearestVisitorExit = true,
                accessible = true
            )
        )

        assertEquals("exit", route.destinationNodeId)
        assertTrue(route.steps.any { it.kind == "lift" })
        assertFalse(route.steps.any { it.kind == "stairs" })
    }

    @Test
    fun optimizedTourVisitsEveryFavoriteAndCanFinishAtExit() {
        val tour = LouvreLocalRouting.optimizeTour(
            data = fixture(),
            request = LouvreTourRequest(
                fromNodeId = "hall",
                sightIds = listOf("painting", "sculpture"),
                finishAtVisitorExit = true
            )
        )

        assertEquals(setOf("painting", "sculpture"), tour.orderedSightIds.toSet())
        assertEquals("exit", tour.route.destinationNodeId)
    }

    private fun fixture() = LouvreIndoorBootstrap(
        museumId = LOUVRE_MUSEUM_ID,
        datasetVersion = "test",
        generatedAt = "2026-08-17T00:00:00Z",
        levels = listOf(
            LouvreLevel("0", "Level 0", "Museum"),
            LouvreLevel("1", "Level 1", "Museum")
        ),
        nodes = listOf(
            node("exit", "Exit", "0", "visitor_exit"),
            node("hall", "Hall", "0", "junction"),
            node("lift-0", "Lift 0", "0", "lift"),
            node("lift-1", "Lift 1", "1", "lift"),
            node("room-1", "Room 1", "1", "room"),
            node("room-2", "Room 2", "0", "room")
        ),
        edges = listOf(
            edge("exit", "hall", 10.0),
            edge("hall", "lift-0", 5.0),
            edge("lift-0", "lift-1", 8.0, kind = "lift"),
            edge("lift-1", "room-1", 5.0),
            edge("hall", "room-2", 7.0)
        ),
        sights = listOf(
            sight("painting", "Painting", "room-1", "1"),
            sight("sculpture", "Sculpture", "room-2", "0")
        ),
        notices = emptyList(),
        defaultStartNodeId = "hall",
        officialClosuresUrl = "https://example.com",
        emergencyNotice = "Follow staff.",
        attribution = "Test"
    )

    private fun node(id: String, name: String, level: String, kind: String) = LouvreNode(
        id = id,
        name = name,
        shortName = name,
        level = level,
        wing = "Pyramid",
        kind = kind,
        x = 50.0,
        y = 50.0
    )

    private fun edge(from: String, to: String, distance: Double, kind: String = "walk") = LouvreEdge(
        from = from,
        to = to,
        distanceMeters = distance,
        accessible = true,
        kind = kind
    )

    private fun sight(id: String, title: String, nodeId: String, level: String) = LouvreSight(
        id = id,
        mapNumber = if (id == "painting") 1 else 2,
        title = title,
        subtitle = "Artist",
        nodeId = nodeId,
        room = nodeId,
        wing = "Pyramid",
        level = level,
        category = "Art",
        sourceUrl = "https://example.com/$id",
        featured = true
    )
}
