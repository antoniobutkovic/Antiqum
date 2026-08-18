package com.strive.antiqum.louvre.data

import com.strive.antiqum.network.Response
import com.strive.antiqum.profile.data.PlatformPreferencesStore
import com.strive.antiqum.profile.data.getStringSet
import com.strive.antiqum.profile.data.putStringSet
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LouvreIndoorRepository {
    suspend fun getBootstrap(): Response<LouvreIndoorBootstrap>

    suspend fun navigate(request: LouvreRouteRequest): Response<LouvreRouteResult>

    suspend fun optimizeTour(request: LouvreTourRequest): Response<LouvreTourResult>

    fun favoriteSightIds(): Set<String>

    fun setFavoriteSightIds(ids: Set<String>)
}

class LouvreIndoorRepositoryImpl(
    private val service: LouvreIndoorService,
    private val preferences: PlatformPreferencesStore
) : LouvreIndoorRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private var bootstrap: LouvreIndoorBootstrap? = null

    override suspend fun getBootstrap(): Response<LouvreIndoorBootstrap> {
        val network = service.getBootstrap()
        if (network is Response.Success) {
            bootstrap = network.data
            preferences.putString(BOOTSTRAP_CACHE_KEY, json.encodeToString(network.data))
            return network
        }
        val cached = preferences.getString(BOOTSTRAP_CACHE_KEY)
            ?.let { runCatching { json.decodeFromString<LouvreIndoorBootstrap>(it) }.getOrNull() }
        if (cached != null) {
            bootstrap = cached
            return Response.Success(cached)
        }
        return network
    }

    override suspend fun navigate(request: LouvreRouteRequest): Response<LouvreRouteResult> {
        val network = service.navigate(request)
        if (network is Response.Success) return network
        val data = bootstrap ?: return network
        return runCatching { LouvreLocalRouting.calculateRoute(data, request) }
            .fold(onSuccess = { Response.Success(it) }, onFailure = { Response.Error(it.asException()) })
    }

    override suspend fun optimizeTour(request: LouvreTourRequest): Response<LouvreTourResult> {
        val network = service.optimizeTour(request)
        if (network is Response.Success) return network
        val data = bootstrap ?: return network
        return runCatching { LouvreLocalRouting.optimizeTour(data, request) }
            .fold(onSuccess = { Response.Success(it) }, onFailure = { Response.Error(it.asException()) })
    }

    override fun favoriteSightIds(): Set<String> = preferences.getStringSet(FAVORITES_KEY)

    override fun setFavoriteSightIds(ids: Set<String>) = preferences.putStringSet(FAVORITES_KEY, ids)

    private fun Throwable.asException(): Exception = this as? Exception ?: Exception(message, this)

    private companion object {
        const val BOOTSTRAP_CACHE_KEY = "louvre_indoor_bootstrap_v2"
        const val FAVORITES_KEY = "louvre_favorite_sight_ids"
    }
}
