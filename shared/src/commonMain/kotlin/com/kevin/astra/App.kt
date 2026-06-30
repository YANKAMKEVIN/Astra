package com.kevin.astra

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kevin.astra.app.AstraApp
import com.kevin.astra.app.di.initializeKoin

@Composable
@Preview
fun App() {
    val koin = initializeKoin().koin
    AstraApp(
        navigator = koin.get(),
        dashboardViewModel = koin.get(),
        demoViewModel = koin.get(),
        projectOverviewViewModel = koin.get(),
        assistantViewModel = koin.get(),
        benchmarkViewModel = koin.get(),
        documentsViewModel = koin.get(),
        settingsViewModel = koin.get(),
        historyViewModel = koin.get(),
        voiceViewModel = koin.get(),
        visionViewModel = koin.get(),
    )
}
