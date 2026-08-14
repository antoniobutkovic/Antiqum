package com.strive.antiqum.categories.data

import com.strive.antiqum.network.Response
import com.strive.antiqum.network.safeResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get

interface CategoriesService {
    suspend fun getCategories(): Response<CategoriesResponse>
}

class CategoriesServiceImpl(private val httpClient: HttpClient) : CategoriesService {
    override suspend fun getCategories(): Response<CategoriesResponse> = safeResponse {
        httpClient.get("api/categories")
    }
}
