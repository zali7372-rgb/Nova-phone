package hu.nova.mobile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import hu.nova.mobile.R
import hu.nova.mobile.ui.NovaViewModelFactory
import hu.nova.mobile.ui.chat.ChatScreen
import hu.nova.mobile.ui.home.HomeScreen
import hu.nova.mobile.ui.memory.MemoryScreen
import hu.nova.mobile.ui.settings.SettingsScreen

private data class NavItem(val destination: NovaDestination, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int)

private val navItems = listOf(
    NavItem(NovaDestination.Home, Icons.Filled.Home, R.string.nav_home),
    NavItem(NovaDestination.Chat, Icons.Filled.Chat, R.string.nav_chat),
    NavItem(NovaDestination.Memory, Icons.Filled.Psychology, R.string.nav_memory),
    NavItem(NovaDestination.Settings, Icons.Filled.Settings, R.string.nav_settings)
)

@Composable
fun NovaNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModelFactory: NovaViewModelFactory
) {
    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route

            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.destination.route,
                        onClick = {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                        label = { Text(stringResource(item.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NovaDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(NovaDestination.Home.route) { HomeScreen(viewModelFactory) }
            composable(NovaDestination.Chat.route) { ChatScreen(viewModelFactory) }
            composable(NovaDestination.Memory.route) { MemoryScreen(viewModelFactory) }
            composable(NovaDestination.Settings.route) { SettingsScreen(viewModelFactory) }
        }
    }
}
