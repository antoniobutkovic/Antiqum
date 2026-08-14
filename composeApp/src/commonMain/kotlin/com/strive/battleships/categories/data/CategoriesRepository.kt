package com.strive.battleships.categories.data

import com.strive.battleships.network.Response

interface CategoriesRepository {
    suspend fun getCategories(): Response<CategoriesResponse>
}

class CategoriesRepositoryImpl(private val service: CategoriesService) : CategoriesRepository {
    override suspend fun getCategories(): Response<CategoriesResponse> = service.getCategories()
}
