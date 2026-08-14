package com.strive.antiqum.museums.data

import kotlinx.serialization.Serializable

data class Museum(
    val id: String,
    val name: String,
    val description: String,
    val category: MuseumCategory,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val imageUrl: String?,
    val website: String?,
    val address: String?,
    val foundedYear: String?
) {
    val locationLabel: String
        get() = listOf(city, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

enum class MuseumCategory(val label: String) {
    All("All"),
    Art("Art"),
    History("History"),
    Archaeology("Archaeology"),
    Science("Science"),
    NaturalHistory("Natural History"),
    Technology("Technology"),
    Military("Military"),
    Ethnography("Ethnography"),
    Maritime("Maritime"),
    Other("Other")
}

@Serializable
data class WikidataSparqlResponse(
    val results: WikidataResults = WikidataResults()
)

@Serializable
data class WikidataResults(
    val bindings: List<WikidataBinding> = emptyList()
)

@Serializable
data class WikidataBinding(
    val museum: WikidataValue? = null,
    val museumLabel: WikidataValue? = null,
    val museumDescription: WikidataValue? = null,
    val location: WikidataValue? = null,
    val distance: WikidataValue? = null,
    val image: WikidataValue? = null,
    val website: WikidataValue? = null,
    val inception: WikidataValue? = null,
    val address: WikidataValue? = null,
    val cityLabel: WikidataValue? = null,
    val countryLabel: WikidataValue? = null,
    val typeLabel: WikidataValue? = null
)

@Serializable
data class WikidataValue(
    val type: String = "",
    val value: String = ""
)
