package com.civic.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The app-wide snackbar host, so screens with controls at the bottom (the map's quick-report bar) can move them
 * up while a confirmation is showing instead of having it cover them (a second quick report must stay one tap).
 */
val LocalAppSnackbarHost = staticCompositionLocalOf { SnackbarHostState() }
