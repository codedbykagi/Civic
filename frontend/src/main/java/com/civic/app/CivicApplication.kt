package com.civic.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.civic.app.data.DraftStore
import com.civic.app.data.local.CivicDatabase
import com.civic.app.data.remote.ApiClient
import com.civic.app.data.remote.ReportApi
import com.civic.app.data.repository.ReportRepository
import com.civic.app.location.LocationProvider
import com.civic.app.safety.QuickReporter
import com.civic.app.safety.routing.KtorHttpTransport
import com.civic.app.safety.routing.RoutingConfig
import com.civic.app.safety.routing.SafeRoutePlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class CivicApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Simple manual dependency container. Swap for Hilt/Koin if the app grows. */
class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(application, CivicDatabase::class.java, "civic.db")
        .addMigrations(*CivicDatabase.ALL_MIGRATIONS)
        .build()
    private val reportApi by lazy { ReportApi(ApiClient.httpClient) }

    /** Work that must outlive any one screen (e.g. finishing a quick report after navigating away). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val reportRepository = ReportRepository(reportApi, database.reportDao(), database.commentDao())
    val locationProvider = LocationProvider(application)
    val draftStore = DraftStore()
    val quickReporter = QuickReporter(reportRepository, locationProvider, appScope)

    /** Walking routes with fewer reported zones. Lazy: no HTTP client until someone plans a route. */
    val routePlanner by lazy {
        SafeRoutePlanner(
            transport = KtorHttpTransport(
                // FOSSGIS requires an app-identifying User-Agent; Valhalla asks apps to send an X-Client-Id.
                userAgent = "CivicSafetyPrototype/${BuildConfig.VERSION_NAME} (Android; non-commercial prototype)",
                clientId = "civic-android-prototype",
            ),
            config = RoutingConfig(valhallaUrl = BuildConfig.VALHALLA_URL, osrmFootBaseUrl = BuildConfig.OSRM_FOOT_URL),
        )
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CivicApplication).container
