package hu.nova.mobile.navigation

sealed class NovaDestination(val route: String) {
    object Home : NovaDestination("home")
    object Chat : NovaDestination("chat")
    object Memory : NovaDestination("memory")
    object Settings : NovaDestination("settings")
}
