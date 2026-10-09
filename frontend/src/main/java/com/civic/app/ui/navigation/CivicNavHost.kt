package com.civic.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.civic.app.appContainer
import com.civic.app.data.Draft
import com.civic.app.data.auth.AuthState
import com.civic.app.safety.QuickReportEvent
import com.civic.app.ui.components.CivicHeader
import com.civic.app.ui.components.LocalAppSnackbarHost
import com.civic.app.ui.components.rememberQuickReportAction
import com.civic.app.ui.screens.auth.SignInScreen
import com.civic.app.ui.screens.auth.SignUpScreen
import com.civic.app.ui.screens.auth.WelcomeScreen
import com.civic.app.ui.screens.capture.CaptureScreen
import com.civic.app.ui.screens.detail.ReportDetailScreen
import com.civic.app.ui.screens.detail.ReportDetailViewModel
import com.civic.app.ui.screens.feed.FeedScreen
import com.civic.app.ui.screens.map.MapScreen
import com.civic.app.ui.screens.profile.ProfileScreen
import com.civic.app.ui.screens.report.CreateReportScreen
import com.civic.app.ui.screens.report.ReportHubScreen
import com.civic.app.ui.screens.report.startReportWithoutPhoto
import com.civic.app.ui.screens.safety.SafetyReportsScreen
import com.civic.shared.model.IssueCategory
import kotlinx.coroutines.launch

/** Snackbar for a one-tap report; carries the report so its buttons can undo it or open it. */
private data class QuickReportVisuals(
    override val message: String,
    val reportId: Long,
    val canUndo: Boolean,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Long
}

/**
 * [pendingQuickReport] comes from a launcher shortcut ("Unsafe here: women" …): it is reported once, then
 * [onPendingQuickReportHandled] clears it.
 */
@Composable
fun CivicNavHost(
    pendingQuickReport: IssueCategory? = null,
    onPendingQuickReportHandled: () -> Unit = {},
    darkTheme: Boolean = true,
    onDarkThemeChange: (Boolean) -> Unit = {},
) {
    val authState by LocalContext.current.appContainer.authRepository.state.collectAsState()
    when (authState) {
        // Don't build the app graph until the stored identity is known, or the feed flashes before onboarding.
        AuthState.Loading -> Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        // No Scaffold, no bottom bar: there is nothing to navigate to until the user has an identity.
        AuthState.NeedsOnboarding -> AuthFlow()
        is AuthState.Active -> SignedInApp(pendingQuickReport, onPendingQuickReportHandled, darkTheme, onDarkThemeChange)
    }
}

/** Onboarding, in its own graph so a back press inside it can never reach the app. */
@Composable
private fun AuthFlow() {
    val navController = rememberNavController()
    // Survives the Sign up → Sign in hop; each destination has its own AuthViewModel, so its state would not.
    var confirmationNotice by remember { mutableStateOf<String?>(null) }
    // Nothing calls back on success: the repository flips to Active and this whole flow leaves the tree.
    val backToWelcome: () -> Unit = { navController.popBackStack(Screen.Welcome.route, inclusive = false) }
    // Surface, not Scaffold: these screens need the theme background but no app bars.
    Surface(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Screen.Welcome.route) {
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onSignedIn = {},
                    onSignIn = { navController.navigate(Screen.SignIn.route) },
                    onSignUp = { navController.navigate(Screen.SignUp.route) },
                )
            }
            composable(Screen.SignIn.route) {
                SignInScreen(
                    onBack = backToWelcome,
                    onSignedIn = {},
                    onSignUp = { navController.navigate(Screen.SignUp.route) },
                    incomingNotice = confirmationNotice,
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onBack = backToWelcome,
                    onSignedIn = {},
                    onSignIn = { navController.navigate(Screen.SignIn.route) },
                    onNeedsConfirmation = { email ->
                        confirmationNotice = "Check $email for a confirmation link, then sign in."
                        navController.navigate(Screen.SignIn.route)
                    },
                )
            }
        }
    }
}

@Composable
private fun SignedInApp(
    pendingQuickReport: IssueCategory?,
    onPendingQuickReportHandled: () -> Unit,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val container = LocalContext.current.appContainer
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val quickReport = rememberQuickReportAction()
    // Hoisted for the same reason as in AuthFlow: it has to outlive the Sign up destination.
    var confirmationNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingQuickReport) {
        pendingQuickReport ?: return@LaunchedEffect
        quickReport(pendingQuickReport)
        onPendingQuickReportHandled()
    }

    // Confirm every one-tap report: quick feedback plus Undo (a mis-tap must be easy to take back).
    LaunchedEffect(container) {
        container.quickReporter.events.collect { event ->
            val visuals = when (event) {
                is QuickReportEvent.Saved -> QuickReportVisuals(
                    message = "Saved: ${event.category.displayName} · ${event.timeOfDay.displayName}" +
                        if (event.located) "" else " · finding your location…",
                    reportId = event.reportId,
                    canUndo = true,
                )
                is QuickReportEvent.NoLocation -> QuickReportVisuals(
                    message = "No GPS fix: saved without a location, so it can't appear on the map.",
                    reportId = event.reportId,
                    canUndo = false,
                )
            }
            scope.launch {
                snackbarHost.currentSnackbarData?.dismiss()
                snackbarHost.showSnackbar(visuals)
            }
        }
    }

    Scaffold(
        // Main tabs get the header with the theme switch; pushed screens keep their own back-arrow bar.
        topBar = {
            if (Screen.bottomBarItems.any { it.route == currentRoute }) {
                CivicHeader(darkTheme = darkTheme, onDarkThemeChange = onDarkThemeChange)
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                val visuals = data.visuals as? QuickReportVisuals
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = visuals?.let {
                        {
                            Row {
                                if (it.canUndo) {
                                    TextButton(onClick = {
                                        container.quickReporter.undo(it.reportId)
                                        data.dismiss()
                                    }) { Text("Undo") }
                                }
                                TextButton(onClick = {
                                    data.dismiss()
                                    navController.navigate(Screen.ReportDetail.route(it.reportId))
                                }) { Text("Add details") }
                            }
                        }
                    },
                ) { Text(data.visuals.message) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, tonalElevation = 0.dp) {
                Screen.bottomBarItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = { navController.navigateToTab(screen.route) },
                        icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                        label = { Text(screen.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalAppSnackbarHost provides snackbarHost) {
            NavHost(
                navController = navController,
                startDestination = Screen.Feed.route,
                // consumeWindowInsets so imePadding() inside screens doesn't double-count the bottom bar.
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
            ) {
                val openReport: (Long) -> Unit = { id -> navController.navigate(Screen.ReportDetail.route(id)) }
                val openSafetyReports: () -> Unit = { navController.navigate(Screen.SafetyReports.route) }
                composable(Screen.Feed.route) { FeedScreen(onOpenReport = openReport) }
                composable(Screen.Map.route) { MapScreen(onOpenReport = openReport) }
                composable(Screen.ReportHub.route) {
                    ReportHubScreen(
                        onTakePhoto = { navController.navigate(Screen.Capture.newReport()) },
                        onReportWithoutPhoto = {
                            startReportWithoutPhoto(container.draftStore, IssueCategory.UNSAFE_GENERAL)
                            navController.navigate(Screen.CreateReport.route)
                        },
                        onMySafetyReports = openSafetyReports,
                    )
                }
                composable(
                    Screen.ReportDetail.route,
                    arguments = listOf(navArgument(ReportDetailViewModel.ARG_ID) { type = NavType.LongType }),
                ) {
                    ReportDetailScreen(
                        onBack = { navController.popBackStack() },
                        onAddPhoto = { id -> navController.navigate(Screen.Capture.attachTo(id)) },
                    )
                }
                composable(
                    Screen.Capture.route,
                    arguments = listOf(
                        navArgument(Screen.Capture.MODE) { type = NavType.StringType; defaultValue = Screen.Capture.MODE_NEW },
                        navArgument(Screen.Capture.REPORT_ID) { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    val mode = entry.arguments?.getString(Screen.Capture.MODE) ?: Screen.Capture.MODE_NEW
                    val reportId = entry.arguments?.getLong(Screen.Capture.REPORT_ID) ?: -1L
                    CaptureScreen(
                        // An existing report already has its position; don't make the user wait for GPS.
                        needsLocation = mode != Screen.Capture.MODE_ATTACH,
                        onPhotoCaptured = { path, capturedAt, location ->
                            when (mode) {
                                Screen.Capture.MODE_ATTACH -> {
                                    container.reportRepository.setPhoto(reportId, path)
                                    navController.popBackStack()
                                }
                                Screen.Capture.MODE_DRAFT -> {
                                    container.draftStore.setPhoto(path, location)
                                    navController.popBackStack()
                                }
                                else -> {
                                    container.draftStore.replace(Draft(path, location, capturedAt))
                                    navController.navigate(Screen.CreateReport.route)
                                }
                            }
                        },
                    )
                }
                composable(Screen.CreateReport.route) {
                    CreateReportScreen(
                        onPosted = { isSafety ->
                            if (isSafety) {
                                // Private reports aren't in the feed: show where they went instead.
                                navController.navigate(Screen.SafetyReports.route) {
                                    popUpTo(Screen.ReportHub.route)
                                }
                            } else {
                                // Pop (not navigateToTab): saving this stack would reopen the form on the next Report tap.
                                navController.popBackStack(Screen.Feed.route, inclusive = false)
                            }
                        },
                        onTakePhoto = { navController.navigate(Screen.Capture.forDraft()) },
                    )
                }
                composable(Screen.SafetyReports.route) {
                    SafetyReportsScreen(onBack = { navController.popBackStack() }, onOpenReport = openReport)
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onMySafetyReports = { navController.navigate(Screen.SafetyReports.route) },
                        onSignIn = { navController.navigate(Screen.SignIn.route) },
                        onSignUp = { navController.navigate(Screen.SignUp.route) },
                    )
                }
                // Also in the main graph: someone using the app on a device profile can sign in from Profile.
                composable(Screen.SignIn.route) {
                    SignInScreen(
                        onBack = { navController.popBackStack() },
                        onSignedIn = { navController.popBackStack() },
                        onSignUp = { navController.navigate(Screen.SignUp.route) },
                        incomingNotice = confirmationNotice,
                        // The user already has an identity here; creating one would rename them to "Neighbour".
                        onSkip = { navController.popBackStack() },
                    )
                }
                composable(Screen.SignUp.route) {
                    SignUpScreen(
                        onBack = { navController.popBackStack() },
                        onSignedIn = { navController.popBackStack() },
                        onSignIn = { navController.navigate(Screen.SignIn.route) },
                        onNeedsConfirmation = { email ->
                            confirmationNotice = "Check $email for a confirmation link, then sign in."
                            navController.navigate(Screen.SignIn.route)
                        },
                        onSkip = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
