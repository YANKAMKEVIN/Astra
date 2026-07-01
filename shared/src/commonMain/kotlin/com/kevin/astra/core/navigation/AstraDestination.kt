package com.kevin.astra.core.navigation

sealed class AstraDestination(
    val id: String,
    val label: String,
    val shortLabel: String,
    val showsNavigationBar: Boolean = true,
    /** Shown directly in the bottom bar. False = accessible via the "More" sheet. */
    val isPrimaryNav: Boolean = false,
) {
    data object Splash : AstraDestination(
        id = "splash",
        label = "Splash",
        shortLabel = "Splash",
        showsNavigationBar = false
    )

    data object Onboarding : AstraDestination(
        id = "onboarding",
        label = "Onboarding",
        shortLabel = "Welcome",
        showsNavigationBar = false
    )

    data object Demo : AstraDestination(
        id = "demo",
        label = "Demo Mode",
        shortLabel = "Demo"
    )

    data object ProjectOverview : AstraDestination(
        id = "overview",
        label = "Home",
        shortLabel = "Home",
        isPrimaryNav = true
    )

    data object Assistant : AstraDestination(
        id = "assistant",
        label = "Assistant",
        shortLabel = "Chat",
        isPrimaryNav = true
    )

    data object Documents : AstraDestination(
        id = "documents",
        label = "Documents",
        shortLabel = "Docs",
        isPrimaryNav = true
    )

    data object Benchmark : AstraDestination(
        id = "benchmark",
        label = "Benchmark",
        shortLabel = "Bench",
        isPrimaryNav = true
    )

    data object Settings : AstraDestination(
        id = "settings",
        label = "Settings",
        shortLabel = "Config",
        isPrimaryNav = true
    )

    data object VoiceAssistant : AstraDestination(
        id = "voice",
        label = "Voice Assistant",
        shortLabel = "Voice"
    )

    data object VisionAssistant : AstraDestination(
        id = "vision",
        label = "Vision Assistant",
        shortLabel = "Vision"
    )

    data object History : AstraDestination(
        id = "history",
        label = "History",
        shortLabel = "History"
    )

    companion object {
        val all: List<AstraDestination>
            get() = listOf(
                Splash, Onboarding,
                ProjectOverview, Assistant, Documents, Benchmark, Settings,
                VoiceAssistant, VisionAssistant, History, Demo,
            )

        val primaryDestinations: List<AstraDestination>
            get() = all.filter { it.showsNavigationBar }

        val primaryNavDestinations: List<AstraDestination>
            get() = all.filter { it.isPrimaryNav }

        /** Destinations reachable via the "More" sheet (in the nav bar but not primary). */
        val secondaryNavDestinations: List<AstraDestination>
            get() = all.filter { it.showsNavigationBar && !it.isPrimaryNav }

        fun fromId(id: String?): AstraDestination? = all.find { it.id == id }
    }
}
