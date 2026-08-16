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
    val foundedYear: String?,
    val images: List<MuseumImage> = emptyList(),
    val museumTypes: List<String> = emptyList(),
    val regularOpeningHours: List<String> = emptyList(),
    val closureStatus: String? = null,
    val admission: String? = null,
    val ticketUrl: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val accessibility: List<String> = emptyList(),
    val architecturalStyles: List<String> = emptyList(),
    val heritageDesignations: List<String> = emptyList(),
    val operators: List<String> = emptyList(),
    val owners: List<String> = emptyList(),
    val parentOrganizations: List<String> = emptyList(),
    val socialLinks: List<MuseumSocialLink> = emptyList(),
    val currentExhibitions: List<MuseumExhibition> = emptyList(),
    val directionsUrl: String? = null
) {
    val locationLabel: String
        get() = listOf(city, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

@Serializable
data class MuseumImage(
    val url: String,
    val source: String,
    val title: String? = null,
    val license: String? = null,
    val photographer: String? = null
)

@Serializable
data class MuseumSocialLink(
    val platform: String,
    val url: String
)

@Serializable
data class MuseumExhibition(
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val website: String? = null
)

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
