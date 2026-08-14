package com.strive.antiqum.museums.data

import com.strive.antiqum.network.Response

interface MuseumsRepository {
    suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int = 50
    ): Response<List<Museum>>
}

class MuseumsRepositoryImpl(private val service: MuseumsService) : MuseumsRepository {
    override suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Response<List<Museum>> = when (val response = service.getNearbyMuseums(latitude, longitude, radiusKm)) {
        is Response.Success -> Response.Success(response.data.results.bindings.toMuseums())
        is Response.HttpError -> response
        is Response.Error -> response
    }
}

internal fun List<WikidataBinding>.toMuseums(): List<Museum> = mapNotNull { binding ->
    val itemUrl = binding.museum?.value ?: return@mapNotNull null
    val id = itemUrl.substringAfterLast('/')
    val coordinates = parseCoordinates(binding.location?.value) ?: return@mapNotNull null
    Museum(
        id = id,
        name = binding.museumLabel?.value?.takeUnless { it == id } ?: "Museum",
        description = binding.museumDescription?.value
            ?: "Discover this museum's collections, history, and cultural significance.",
        category = categoryFor(binding.typeLabel?.value.orEmpty(), binding.museumLabel?.value.orEmpty()),
        city = binding.cityLabel?.value.orEmpty(),
        country = binding.countryLabel?.value.orEmpty(),
        latitude = coordinates.second,
        longitude = coordinates.first,
        distanceKm = binding.distance?.value?.toDoubleOrNull() ?: 0.0,
        imageUrl = binding.image?.value?.replace("http://", "https://"),
        website = binding.website?.value,
        address = binding.address?.value,
        foundedYear = binding.inception?.value?.take(4)?.takeIf { year -> year.all(Char::isDigit) }
    )
}
    .groupBy(Museum::id)
    .map { (_, duplicates) ->
        duplicates.maxBy { museum ->
            listOf(
                museum.imageUrl,
                museum.website,
                museum.address,
                museum.foundedYear
            ).count { it != null }
        }
    }
    .sortedBy(Museum::distanceKm)

internal fun parseCoordinates(value: String?): Pair<Double, Double>? {
    val parts = value
        ?.removePrefix("Point(")
        ?.removeSuffix(")")
        ?.split(' ')
        ?: return null
    if (parts.size != 2) return null
    val longitude = parts[0].toDoubleOrNull() ?: return null
    val latitude = parts[1].toDoubleOrNull() ?: return null
    return longitude to latitude
}

private fun categoryFor(type: String, name: String): MuseumCategory {
    val text = "$type $name".lowercase()
    return when {
        "natural history" in text -> MuseumCategory.NaturalHistory
        "archaeolog" in text -> MuseumCategory.Archaeology
        "ethnograph" in text -> MuseumCategory.Ethnography
        "maritime" in text || "naval" in text -> MuseumCategory.Maritime
        "military" in text || "war museum" in text -> MuseumCategory.Military
        "technolog" in text || "technical museum" in text -> MuseumCategory.Technology
        "science" in text -> MuseumCategory.Science
        "histor" in text || "heritage" in text -> MuseumCategory.History
        "art" in text || "gallery" in text -> MuseumCategory.Art
        else -> MuseumCategory.Other
    }
}
