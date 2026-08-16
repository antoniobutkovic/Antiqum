package com.strive.antiqum.profile.data

enum class SignInProvider(val label: String) {
    Google("Google"),
    Apple("Apple")
}

data class AntiqumProfile(
    val provider: SignInProvider,
    val displayName: String = "Antiqum explorer"
)

interface ProfileRepository {
    fun hasCompletedOnboarding(): Boolean

    fun completeOnboarding()

    fun getProfile(): AntiqumProfile?

    fun signIn(provider: SignInProvider): AntiqumProfile

    fun signOut()

    fun getFavoriteIds(): Set<String>

    fun saveFavoriteIds(ids: Set<String>)

    fun getVisitedIds(): Set<String>

    fun saveVisitedIds(ids: Set<String>)
}

class ProfileRepositoryImpl(
    private val preferences: PlatformPreferencesStore
) : ProfileRepository {
    override fun hasCompletedOnboarding(): Boolean = preferences.getBoolean(ONBOARDING_COMPLETED_KEY, defaultValue = false)

    override fun completeOnboarding() {
        preferences.putBoolean(ONBOARDING_COMPLETED_KEY, true)
    }

    override fun getProfile(): AntiqumProfile? = preferences
        .getString(SIGN_IN_PROVIDER_KEY)
        ?.let { storedProvider -> SignInProvider.entries.firstOrNull { it.name == storedProvider } }
        ?.let(::AntiqumProfile)

    override fun signIn(provider: SignInProvider): AntiqumProfile {
        preferences.putString(SIGN_IN_PROVIDER_KEY, provider.name)
        return AntiqumProfile(provider)
    }

    override fun signOut() {
        preferences.remove(SIGN_IN_PROVIDER_KEY)
    }

    override fun getFavoriteIds(): Set<String> = preferences.getStringSet(FAVORITE_IDS_KEY)

    override fun saveFavoriteIds(ids: Set<String>) {
        preferences.putStringSet(FAVORITE_IDS_KEY, ids)
    }

    override fun getVisitedIds(): Set<String> = preferences.getStringSet(VISITED_IDS_KEY)

    override fun saveVisitedIds(ids: Set<String>) {
        preferences.putStringSet(VISITED_IDS_KEY, ids)
    }

    private companion object {
        const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
        const val SIGN_IN_PROVIDER_KEY = "sign_in_provider"
        const val FAVORITE_IDS_KEY = "favorite_ids"
        const val VISITED_IDS_KEY = "visited_ids"
    }
}
