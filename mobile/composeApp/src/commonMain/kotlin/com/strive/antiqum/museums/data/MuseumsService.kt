package com.strive.antiqum.museums.data

import com.strive.antiqum.network.Response
import com.strive.antiqum.network.safeResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter

interface MuseumsService {
    suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Response<MuseumPage>

    suspend fun getMuseumsPage(
        cursor: String?,
        limit: Int,
        latitude: Double,
        longitude: Double,
        searchQuery: String,
        category: MuseumCategory,
        sort: String
    ): Response<MuseumPage>
}

class MuseumsServiceImpl(private val httpClient: HttpClient) : MuseumsService {
    override suspend fun getNearbyMuseums(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Response<MuseumPage> = safeResponse {
        httpClient.get("api/museums") {
            parameter("limit", 100)
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("radiusKm", radiusKm)
            parameter("sort", "distance")
        }
    }

    override suspend fun getMuseumsPage(
        cursor: String?,
        limit: Int,
        latitude: Double,
        longitude: Double,
        searchQuery: String,
        category: MuseumCategory,
        sort: String
    ): Response<MuseumPage> = safeResponse {
        httpClient.get("api/museums") {
            parameter("limit", limit)
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("sort", sort)
            cursor?.let { parameter("cursor", it) }
            searchQuery.trim().takeIf(String::isNotEmpty)?.let { parameter("query", it) }
            category.takeUnless { it == MuseumCategory.All }?.let { parameter("category", it.name) }
        }
    }
}
