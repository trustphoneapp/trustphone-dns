package com.yashwanthsurabhi.shielddns.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.yashwanthsurabhi.shielddns.ui.navigation.NavRoutes

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val primaryNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, "Dashboard", Icons.Default.Dashboard),
    BottomNavItem(NavRoutes.LISTS, "Shield", Icons.AutoMirrored.Filled.List),
    BottomNavItem(NavRoutes.ACTIVITY, "Activity", Icons.Default.Timeline),
    BottomNavItem(NavRoutes.MORE, "More", Icons.Default.MoreHoriz),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val isSubScreen = NavRoutes.isSubScreen(currentRoute)
    val selectedRoute = when (currentRoute) {
        NavRoutes.RULES, NavRoutes.ALLOWLIST, NavRoutes.APPS, NavRoutes.STATS,
        NavRoutes.SETTINGS, NavRoutes.ABOUT -> NavRoutes.MORE
        else -> currentRoute
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSubScreen) {
                TopAppBar(
                    title = { Text(NavRoutes.title(currentRoute)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    primaryNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = selectedRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding: PaddingValues ->
        content(Modifier.padding(padding))
    }
}
