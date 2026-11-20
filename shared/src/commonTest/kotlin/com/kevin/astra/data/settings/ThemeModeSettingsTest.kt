package com.kevin.astra.data.settings

import com.kevin.astra.domain.settings.ThemeHolder
import com.kevin.astra.domain.settings.ThemeMode
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeSettingsTest {
    @AfterTest
    fun resetHolder() {
        ThemeHolder.set(ThemeMode.Dark)
    }

    @Test
    fun systemModeFollowsThePhone() {
        assertTrue(ThemeMode.System.isLight(systemInDarkTheme = false))
        assertFalse(ThemeMode.System.isLight(systemInDarkTheme = true))
        assertTrue(ThemeMode.Light.isLight(systemInDarkTheme = true))
        assertFalse(ThemeMode.Dark.isLight(systemInDarkTheme = false))
    }

    @Test
    fun freshInstallStaysDark() = runBlocking {
        assertEquals(ThemeMode.Dark, testAiConfigurationRepository().getConfiguration().themeMode)
    }

    @Test
    fun theOldLightThemeFlagBecomesLightMode() = runBlocking {
        val store = TestAiConfigurationKeyValueStore().apply { putBoolean("ai.light_theme_enabled", true) }

        assertEquals(ThemeMode.Light, testAiConfigurationRepository(store).getConfiguration().themeMode)
    }

    @Test
    fun chosenModeIsSavedAndPublished() = runBlocking {
        val store = TestAiConfigurationKeyValueStore()
        testAiConfigurationRepository(store).updateThemeMode(ThemeMode.System)

        assertEquals(ThemeMode.System, ThemeHolder.mode())
        assertEquals(ThemeMode.System, testAiConfigurationRepository(store).getConfiguration().themeMode)
    }
}
