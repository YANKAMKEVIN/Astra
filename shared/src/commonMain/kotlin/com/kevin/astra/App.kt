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
        assistantViewModel = koin.get(),
        settingsViewModel = koin.get(),
    )
}
