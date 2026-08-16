package com.strive.antiqum.museums.data

import kotlin.test.Test
import kotlin.test.assertEquals

class MuseumModelsTest {
    @Test
    fun locationLabelCombinesDistinctLocationParts() {
        assertEquals("Zagreb, Croatia", museum(city = "Zagreb", country = "Croatia").locationLabel)
        assertEquals("Zagreb", museum(city = "Zagreb", country = "Zagreb").locationLabel)
        assertEquals("Croatia", museum(city = "", country = "Croatia").locationLabel)
    }

    @Test
    fun pageCarriesOpaqueBackendCursor() {
        val page = MuseumPage(
            museums = listOf(museum(city = "Zagreb", country = "Croatia")),
            nextCursor = "opaque-cursor",
            hasMore = true
        )

        assertEquals("opaque-cursor", page.nextCursor)
        assertEquals(true, page.hasMore)
    }

    private fun museum(city: String, country: String) = Museum(
        id = "Q123",
        name = "Museum",
        description = "Description",
        category = MuseumCategory.Other,
        city = city,
        country = country,
        latitude = 45.815,
        longitude = 15.9819,
        distanceKm = 0.0,
        imageUrl = null,
        website = null,
        address = null,
        foundedYear = null
    )
}
