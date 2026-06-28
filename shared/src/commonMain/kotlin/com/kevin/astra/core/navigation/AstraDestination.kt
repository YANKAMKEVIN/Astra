package com.kevin.astra.core.navigation

enum class AstraDestination(
    val label: String,
    val shortLabel: String,
    val showsNavigationBar: Boolean = true,
) {
    Splash(label = "Splash", shortLabel = "Splash", showsNavigationBar = false),
    Dashboard(label = "Dashboard", shortLabel = "Home"),
    Demo(label = "Demo Mode", shortLabel = "Demo"),
    ProjectOverview(label = "Project Overview", shortLabel = "Overview"),
    Assistant(label = "Assistant", shortLabel = "AI"),
    Documents(label = "Documents", shortLabel = "Docs"),
    Benchmark(label = "Benchmark", shortLabel = "Bench"),
    Settings(label = "Settings", shortLabel = "Settings"),
    ;

    companion object {
        val primaryDestinations = entries.filter(AstraDestination::showsNavigationBar)
    }
}
