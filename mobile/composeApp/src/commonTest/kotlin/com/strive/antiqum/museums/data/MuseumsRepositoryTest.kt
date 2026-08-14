package com.strive.antiqum.museums.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MuseumsRepositoryTest {
    @Test
    fun parsesWikidataPointAsLongitudeAndLatitude() {
        assertEquals(15.9818579 to 45.7788131, parseCoordinates("Point(15.9818579 45.7788131)"))
        assertNull(parseCoordinates("not-a-point"))
    }

    @Test
    fun mapsAndDeduplicatesWikidataBindings() {
        val first = museumBinding(
            website = null,
            image = "http://commons.wikimedia.org/example.jpg"
        )
        val richerDuplicate = museumBinding(
            website = "https://example.org",
            image = "http://commons.wikimedia.org/example.jpg"
        )

        val museums = listOf(first, richerDuplicate).toMuseums()

        assertEquals(1, museums.size)
        assertEquals("Q123", museums.single().id)
        assertEquals("https://commons.wikimedia.org/example.jpg", museums.single().imageUrl)
        assertEquals("https://example.org", museums.single().website)
        assertEquals(MuseumCategory.Art, museums.single().category)
    }

    private fun museumBinding(website: String?, image: String?) = WikidataBinding(
        museum = WikidataValue(value = "http://www.wikidata.org/entity/Q123"),
        museumLabel = WikidataValue(value = "City Art Museum"),
        museumDescription = WikidataValue(value = "An art museum"),
        location = WikidataValue(value = "Point(15.98 45.81)"),
        distance = WikidataValue(value = "1.25"),
        image = image?.let { WikidataValue(value = it) },
        website = website?.let { WikidataValue(value = it) },
        cityLabel = WikidataValue(value = "Zagreb"),
        countryLabel = WikidataValue(value = "Croatia"),
        typeLabel = WikidataValue(value = "art museum")
    )
}
