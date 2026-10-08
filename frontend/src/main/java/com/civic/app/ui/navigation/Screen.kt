package com.civic.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Feed : Screen("feed", "Feed", Icons.Filled.Home)
    data object Map : Screen("map", "Map", Icons.Filled.Map)
    data object Capture : Screen("capture", "Report", Icons.Filled.AddAPhoto)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)

    /** Not in the bottom bar: reached after taking a photo. */
    data object CreateReport : Screen("create_report", "New report")

    /** Not in the bottom bar: opened from a feed card or a map pin. */
    data object ReportDetail : Screen("report/{id}", "Report") {
        fun route(id: Long) = "report/$id"
    }

    companion object {
        val bottomBarItems = listOf(Feed, Map, Capture, Profile)
    }
}
