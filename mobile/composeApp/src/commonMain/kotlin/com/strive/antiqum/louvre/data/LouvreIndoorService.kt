package com.strive.antiqum.louvre.data

import com.strive.antiqum.network.Response
import com.strive.antiqum.network.safeResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface LouvreIndoorService {
    suspend fun getBootstrap(): Response<LouvreIndoorBootstrap>

    suspend fun navigate(request: LouvreRouteRequest): Response<LouvreRouteResult>

    suspend fun optimizeTour(request: LouvreTourRequest): Response<LouvreTourResult>
}

class LouvreIndoorServiceImpl(private val httpClient: HttpClient) : LouvreIndoorService {
    override suspend fun getBootstrap(): Response<LouvreIndoorBootstrap> = safeResponse {
        httpClient.get("api/museums/$LOUVRE_MUSEUM_ID/indoor")
    }

    override suspend fun navigate(request: LouvreRouteRequest): Response<LouvreRouteResult> = safeResponse {
        httpClient.post("api/museums/$LOUVRE_MUSEUM_ID/indoor/navigate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun optimizeTour(request: LouvreTourRequest): Response<LouvreTourResult> = safeResponse {
        httpClient.post("api/museums/$LOUVRE_MUSEUM_ID/indoor/tour") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
