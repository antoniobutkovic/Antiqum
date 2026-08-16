package com.strive.antiqum.museums.data

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
data class MuseumPage(
    val museums: List<Museum>,
    val nextCursor: String? = null,
    val hasMore: Boolean
)

@Serializable
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
