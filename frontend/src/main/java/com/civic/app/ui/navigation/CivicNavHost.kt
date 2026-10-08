package com.civic.app.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.civic.app.ui.screens.capture.CaptureScreen
import com.civic.app.ui.screens.detail.ReportDetailScreen
import com.civic.app.ui.screens.detail.ReportDetailViewModel
import com.civic.app.ui.screens.feed.FeedScreen
import com.civic.app.ui.screens.map.MapScreen
import com.civic.app.ui.screens.profile.ProfileScreen
import com.civic.app.ui.screens.report.CreateReportScreen

@Composable
fun CivicNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.bottomBarItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                        label = { Text(screen.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            // consumeWindowInsets so imePadding() inside screens doesn't double-count the bottom bar.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            val openReport: (Long) -> Unit = { id -> navController.navigate(Screen.ReportDetail.route(id)) }
            composable(Screen.Feed.route) { FeedScreen(onOpenReport = openReport) }
            composable(Screen.Map.route) { MapScreen(onOpenReport = openReport) }
            composable(
                Screen.ReportDetail.route,
                arguments = listOf(navArgument(ReportDetailViewModel.ARG_ID) { type = NavType.LongType }),
            ) {
                ReportDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Capture.route) {
                CaptureScreen(onPhotoCaptured = { navController.navigate(Screen.CreateReport.route) })
            }
            composable(Screen.CreateReport.route) {
                CreateReportScreen(onPosted = { navController.popBackStack(Screen.Feed.route, inclusive = false) })
            }
            composable(Screen.Profile.route) { ProfileScreen() }
        }
    }
}
