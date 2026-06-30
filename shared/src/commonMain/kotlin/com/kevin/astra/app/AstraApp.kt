package com.kevin.astra.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraNavigationBar
import com.kevin.astra.core.design.AstraTheme
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.presentation.assistant.AssistantScreen
import com.kevin.astra.presentation.assistant.AssistantViewModel
import com.kevin.astra.presentation.benchmark.BenchmarkScreen
import com.kevin.astra.presentation.benchmark.BenchmarkViewModel
import com.kevin.astra.presentation.dashboard.DashboardScreen
import com.kevin.astra.presentation.dashboard.DashboardViewModel
import com.kevin.astra.presentation.demo.DemoScreen
import com.kevin.astra.presentation.demo.DemoViewModel
import com.kevin.astra.presentation.documents.DocumentsScreen
import com.kevin.astra.presentation.documents.DocumentsViewModel
import com.kevin.astra.presentation.history.ConversationHistoryScreen
import com.kevin.astra.presentation.history.ConversationHistoryViewModel
import com.kevin.astra.presentation.vision.VisionAssistantScreen
import com.kevin.astra.presentation.vision.VisionAssistantViewModel
import com.kevin.astra.presentation.voice.VoiceAssistantScreen
import com.kevin.astra.presentation.voice.VoiceAssistantViewModel
import com.kevin.astra.presentation.overview.ProjectOverviewScreen
import com.kevin.astra.presentation.overview.ProjectOverviewViewModel
import com.kevin.astra.presentation.settings.SettingsScreen
import com.kevin.astra.presentation.settings.SettingsViewModel
import com.kevin.astra.presentation.splash.SplashScreen

@Composable
fun AstraApp(
    navigator: AstraNavigator,
    dashboardViewModel: DashboardViewModel,
    demoViewModel: DemoViewModel,
    projectOverviewViewModel: ProjectOverviewViewModel,
    assistantViewModel: AssistantViewModel,
    benchmarkViewModel: BenchmarkViewModel,
    documentsViewModel: DocumentsViewModel,
    settingsViewModel: SettingsViewModel,
    historyViewModel: ConversationHistoryViewModel,
    voiceViewModel: VoiceAssistantViewModel,
    visionViewModel: VisionAssistantViewModel,
) {
    val currentDestination by navigator.currentDestination.collectAsStateWithLifecycle()

    AstraTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(AstraColors.Background),
            containerColor = AstraColors.Background,
            bottomBar = {
                if (currentDestination.showsNavigationBar) {
                    AstraNavigationBar(
                        selectedDestination = currentDestination,
                        onDestinationSelected = navigator::navigateTo,
                    )
                }
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        fadeIn(tween(250)) togetherWith fadeOut(tween(150))
                    },
                    label = "astra-destination",
                ) { destination ->
                    when (destination) {
                        AstraDestination.Splash -> SplashScreen(
                            contentPadding = contentPadding,
                            onFinished = { navigator.navigateTo(AstraDestination.Dashboard) },
                        )

                        AstraDestination.Dashboard -> DashboardScreen(
                            contentPadding = contentPadding,
                            viewModel = dashboardViewModel,
                        )
                        AstraDestination.Demo -> DemoScreen(
                            contentPadding = contentPadding,
                            viewModel = demoViewModel,
                        )
                        AstraDestination.ProjectOverview -> ProjectOverviewScreen(
                            contentPadding = contentPadding,
                            viewModel = projectOverviewViewModel,
                        )
                        AstraDestination.Assistant -> AssistantScreen(
                            contentPadding = contentPadding,
                            viewModel = assistantViewModel,
                        )
                        AstraDestination.Documents -> DocumentsScreen(
                            contentPadding = contentPadding,
                            viewModel = documentsViewModel,
                        )
                        AstraDestination.Benchmark -> BenchmarkScreen(
                            contentPadding = contentPadding,
                            viewModel = benchmarkViewModel,
                        )
                        AstraDestination.Settings -> SettingsScreen(
                            contentPadding = contentPadding,
                            viewModel = settingsViewModel,
                        )
                        AstraDestination.History -> ConversationHistoryScreen(
                            contentPadding = contentPadding,
                            viewModel = historyViewModel,
                        )
                        AstraDestination.VoiceAssistant -> VoiceAssistantScreen(
                            contentPadding = contentPadding,
                            viewModel = voiceViewModel,
                        )
                        AstraDestination.VisionAssistant -> VisionAssistantScreen(
                            contentPadding = contentPadding,
                            viewModel = visionViewModel,
                        )
                    }
                }
            }
        }
    }
}
