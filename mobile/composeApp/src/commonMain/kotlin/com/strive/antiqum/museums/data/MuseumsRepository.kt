package com.strive.antiqum.museums.data

import com.strive.antiqum.network.Response

interface MuseumsRepository {
    suspend fun getMuseumDetails(
        id: String,
        referenceLatitude: Double,
        referenceLongitude: Double
    ): Response<Museum>

    suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int = 50
    ): Response<List<Museum>>

    suspend fun getMuseumsPage(
        cursor: String?,
        pageSize: Int,
        referenceLatitude: Double,
        referenceLongitude: Double,
        searchQuery: String,
        category: MuseumCategory,
        sort: String
    ): Response<MuseumPage>
}

class MuseumsRepositoryImpl(private val service: MuseumsService) : MuseumsRepository {
    override suspend fun getMuseumDetails(
        id: String,
        referenceLatitude: Double,
        referenceLongitude: Double
    ): Response<Museum> = service.getMuseumDetails(id, referenceLatitude, referenceLongitude)

    override suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Response<List<Museum>> = when (
        val response = service.getNearbyMuseums(latitude, longitude, radiusKm)
    ) {
        is Response.Success -> Response.Success(response.data.museums)
        is Response.HttpError -> response
        is Response.Error -> response
    }

    override suspend fun getMuseumsPage(
        cursor: String?,
        pageSize: Int,
        referenceLatitude: Double,
        referenceLongitude: Double,
        searchQuery: String,
        category: MuseumCategory,
        sort: String
    ): Response<MuseumPage> = service.getMuseumsPage(
        cursor = cursor,
        limit = pageSize,
        latitude = referenceLatitude,
        longitude = referenceLongitude,
        searchQuery = searchQuery,
        category = category,
        sort = sort
    )
}
