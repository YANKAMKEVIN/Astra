package com.kevin.astra.domain.onboarding

import com.kevin.astra.data.settings.AiConfigurationKeyValueStore

private const val KEY_ONBOARDING_COMPLETED = "onboarding.completed"

class OnboardingRepository(private val store: AiConfigurationKeyValueStore) {
    fun isOnboardingCompleted(): Boolean = store.getBoolean(KEY_ONBOARDING_COMPLETED) ?: false
    fun markOnboardingCompleted() = store.putBoolean(KEY_ONBOARDING_COMPLETED, true)
}
