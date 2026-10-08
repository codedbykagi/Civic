package com.civic.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.civic.app.ui.navigation.CivicNavHost
import com.civic.app.ui.theme.CivicTheme
import com.civic.shared.model.IssueCategory

class MainActivity : ComponentActivity() {

    /** A one-tap report requested from a launcher shortcut, waiting for the UI to file it. */
    private var pendingQuickReport by mutableStateOf<IssueCategory?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch: after a rotation the same intent must not file a second report.
        if (savedInstanceState == null) pendingQuickReport = quickReportCategory(intent)
        setContent {
            CivicTheme {
                CivicNavHost(
                    pendingQuickReport = pendingQuickReport,
                    onPendingQuickReportHandled = { pendingQuickReport = null },
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
