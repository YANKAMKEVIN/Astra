package com.kevin.astra.core.navigation

sealed class AstraDestination(
    val id: String,
    val label: String,
    val shortLabel: String,
    val showsNavigationBar: Boolean = true,
) {
    data object Splash : AstraDestination(
        id = "splash",
        label = "Splash",
        shortLabel = "Splash",
        showsNavigationBar = false
    )

    data object Dashboard : AstraDestination(
        id = "dashboard",
        label = "Dashboard",
        shortLabel = "Home"
    )

    data object Demo : AstraDestination(
        id = "demo",
        label = "Demo Mode",
        shortLabel = "Demo"
    )

    data object ProjectOverview : AstraDestination(
        id = "overview",
        label = "Project Overview",
        shortLabel = "Overview"
    )

    data object Assistant : AstraDestination(
        id = "assistant",
        label = "Assistant",
        shortLabel = "AI"
    )

    data object Documents : AstraDestination(
        id = "documents",
        label = "Documents",
        shortLabel = "Docs"
    )

    data object Benchmark : AstraDestination(
        id = "benchmark",
        label = "Benchmark",
        shortLabel = "Bench"
    )

    data object Settings : AstraDestination(
        id = "settings",
        label = "Settings",
        shortLabel = "Settings"
    )

    data object History : AstraDestination(
        id = "history",
        label = "History",
        shortLabel = "History"
    )

    data object VoiceAssistant : AstraDestination(
        id = "voice",
        label = "Voice",
        shortLabel = "Voice"
    )

    data object VisionAssistant : AstraDestination(
        id = "vision",
        label = "Vision",
        shortLabel = "Vision"
    )

    companion object {
        val all = listOf(
            Splash, Dashboard, Demo, ProjectOverview,
            Assistant, Documents, Benchmark, Settings, History, VoiceAssistant, VisionAssistant
        )
        
        val primaryDestinations = all.filter(AstraDestination::showsNavigationBar)
        
        fun fromId(id: String?): AstraDestination? = all.find { it.id == id }
    }
}
