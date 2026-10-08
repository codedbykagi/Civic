package com.civic.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Feed : Screen("feed", "Home", Icons.Filled.Home)
    data object Map : Screen("map", "Safety map", Icons.Filled.Map)
    data object ReportHub : Screen("report_hub", "Report", Icons.Filled.AddCircle)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)

    /**
     * Camera. [MODE] says where the photo goes: a new report, the post form's draft, or an existing report
     * ([REPORT_ID]).
     */
    data object Capture : Screen("capture?mode={mode}&reportId={reportId}", "Camera") {
        const val MODE = "mode"
        const val REPORT_ID = "reportId"
        const val MODE_NEW = "new"
        const val MODE_DRAFT = "draft"
        const val MODE_ATTACH = "attach"

        fun newReport() = "capture?mode=$MODE_NEW&reportId=-1"
        fun forDraft() = "capture?mode=$MODE_DRAFT&reportId=-1"
        fun attachTo(reportId: Long) = "capture?mode=$MODE_ATTACH&reportId=$reportId"
    }

    /** Not in the bottom bar: the post form, after a photo or "report without a photo". */
    data object CreateReport : Screen("create_report", "New report")

    /** Not in the bottom bar: opened from a feed card, a map pin or the safety-report list. */
    data object ReportDetail : Screen("report/{id}", "Report") {
        fun route(id: Long) = "report/$id"
    }

    /** The user's own private safety reports. */
    data object SafetyReports : Screen("safety_reports", "My safety reports")

    companion object {
        val bottomBarItems = listOf(Feed, Map, ReportHub, Profile)
    }
}
