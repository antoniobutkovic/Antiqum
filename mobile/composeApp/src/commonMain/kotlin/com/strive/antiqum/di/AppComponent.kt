package com.strive.antiqum.di

import com.strive.antiqum.categories.data.CategoriesRepository
import com.strive.antiqum.categories.data.CategoriesRepositoryImpl
import com.strive.antiqum.categories.data.CategoriesService
import com.strive.antiqum.categories.data.CategoriesServiceImpl
import com.strive.antiqum.categories.ui.CategoriesViewModel
import com.strive.antiqum.museums.data.MuseumsRepository
import com.strive.antiqum.museums.data.MuseumsRepositoryImpl
import com.strive.antiqum.museums.data.MuseumsService
import com.strive.antiqum.museums.data.MuseumsServiceImpl
import com.strive.antiqum.museums.ui.MuseumsViewModel
import com.strive.antiqum.network.createHttpClient
import com.strive.antiqum.profile.data.PlatformPreferencesStore
import com.strive.antiqum.profile.data.ProfileRepository
import com.strive.antiqum.profile.data.ProfileRepositoryImpl
import io.ktor.client.HttpClient
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
annotation class AppScope

interface AppComponent {
    val categoriesViewModel: CategoriesViewModel
    val museumsViewModel: MuseumsViewModel

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

    @AppScope
    @Provides
    fun provideMuseumsService(httpClient: HttpClient): MuseumsService = MuseumsServiceImpl(httpClient)

    @AppScope
    @Provides
    fun provideMuseumsRepository(service: MuseumsService): MuseumsRepository = MuseumsRepositoryImpl(service)

    @AppScope
    @Provides
    fun providePlatformPreferencesStore(): PlatformPreferencesStore = PlatformPreferencesStore()

    @AppScope
    @Provides
    fun provideProfileRepository(preferences: PlatformPreferencesStore): ProfileRepository = ProfileRepositoryImpl(preferences)

    @Provides
    fun provideMuseumsViewModel(
        repository: MuseumsRepository,
        profileRepository: ProfileRepository
    ): MuseumsViewModel = MuseumsViewModel(repository, profileRepository)
}
