package com.civic.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.civic.app.ui.navigation.CivicNavHost
import com.civic.app.ui.theme.CivicTheme
import com.civic.app.ui.theme.ThemePreference
import com.civic.shared.model.IssueCategory

class MainActivity : ComponentActivity() {

    /** A one-tap report requested from a launcher shortcut, waiting for the UI to file it. */
    private var pendingQuickReport by mutableStateOf<IssueCategory?>(null)

    /** Dark by default; the header switch flips it and [ThemePreference] keeps the choice. */
    private var darkTheme by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch: after a rotation the same intent must not file a second report.
        if (savedInstanceState == null) pendingQuickReport = quickReportCategory(intent)
        darkTheme = ThemePreference.isDark(this)
        setContent {
            // Status-bar icons follow the chosen mode, not the system one.
            DisposableEffect(darkTheme) {
                val bars = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            CivicTheme(darkTheme = darkTheme) {
                CivicNavHost(
                    pendingQuickReport = pendingQuickReport,
                    onPendingQuickReportHandled = { pendingQuickReport = null },
                    darkTheme = darkTheme,
                    onDarkThemeChange = {
                        darkTheme = it
                        ThemePreference.setDark(this, it)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        quickReportCategory(intent)?.let { pendingQuickReport = it }
    }

    private fun quickReportCategory(intent: Intent?): IssueCategory? {
        if (intent?.action != ACTION_QUICK_UNSAFE) return null
        val name = intent.getStringExtra(EXTRA_CATEGORY)
        return IssueCategory.SAFETY.firstOrNull { it.name == name }
    }

    companion object {
        /** Used by the launcher shortcuts in res/xml/shortcuts.xml. */
        const val ACTION_QUICK_UNSAFE = "com.civic.app.action.QUICK_UNSAFE"
        const val EXTRA_CATEGORY = "category"
    }
}
