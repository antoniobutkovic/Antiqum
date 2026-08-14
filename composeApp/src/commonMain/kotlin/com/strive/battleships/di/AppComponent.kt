package com.strive.battleships.di

import com.strive.battleships.categories.data.CategoriesRepository
import com.strive.battleships.categories.data.CategoriesRepositoryImpl
import com.strive.battleships.categories.data.CategoriesService
import com.strive.battleships.categories.data.CategoriesServiceImpl
import com.strive.battleships.categories.ui.CategoriesViewModel
import com.strive.battleships.network.createHttpClient
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

interface AppComponent {
    val categoriesViewModel: CategoriesViewModel

    @AppScope
    @Provides
    fun provideHttpClient(): HttpClient = createHttpClient()

    @AppScope
    @Provides
    fun provideCategoriesService(httpClient: HttpClient): CategoriesService = CategoriesServiceImpl(httpClient)

    @AppScope
    @Provides
    fun provideCategoriesRepository(service: CategoriesService): CategoriesRepository = CategoriesRepositoryImpl(service)

    @Provides
    fun provideCategoriesViewModel(repository: CategoriesRepository): CategoriesViewModel = CategoriesViewModel(repository)
}
