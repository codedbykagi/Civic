package com.civic.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.civic.app.ai.AiAssistant
import com.civic.app.ai.NoAiAssistant
import com.civic.app.data.DraftStore
import com.civic.app.data.auth.AuthRepository
import com.civic.app.data.auth.AuthState
import com.civic.app.data.auth.SessionStore
import com.civic.app.data.cloud.AuthApi
import com.civic.app.data.cloud.CloudHttp
import com.civic.app.data.cloud.CloudReportApi
import com.civic.app.data.cloud.ProfileApi
import com.civic.app.data.cloud.StorageApi
import com.civic.app.data.local.CivicDatabase
import com.civic.app.data.repository.ReportRepository
import com.civic.app.data.sync.SyncManager
import com.civic.app.location.LocationProvider
import com.civic.app.safety.QuickReporter
import com.civic.app.safety.routing.KtorHttpTransport
import com.civic.app.safety.routing.RoutingConfig
import com.civic.app.safety.routing.SafeRoutePlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

class CivicApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}

/** Simple manual dependency container. Swap for Hilt/Koin if the app grows. */
class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(application, CivicDatabase::class.java, "civic.db")
        .addMigrations(*CivicDatabase.ALL_MIGRATIONS)
        .build()

    /** Work that must outlive any one screen (e.g. finishing a quick report after navigating away). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ---------- accounts ----------

    private val sessionStore = SessionStore(application)

    /**
     * One HTTP client for every Supabase call. The token lambda is what keeps auth out of each API class: it
     * hands back the current access token, refreshing it first if it is about to expire.
     */
    private val cloudHttp: CloudHttp by lazy {
        CloudHttp(CloudHttp.defaultClient()) { authRepository.accessToken() }
    }
    private val cloudReportApi: CloudReportApi by lazy { CloudReportApi(cloudHttp) }
    private val storageApi: StorageApi by lazy { StorageApi(cloudHttp) }

    val authRepository: AuthRepository = AuthRepository(
        store = sessionStore,
        authApi = AuthApi(cloudHttp),
        profileApi = ProfileApi(cloudHttp),
        storageApi = storageApi,
    )

    val reportRepository: ReportRepository = ReportRepository(
        dao = database.reportDao(),
        commentDao = database.commentDao(),
        currentAccount = { authRepository.account },
    )

    val syncManager: SyncManager = SyncManager(
        auth = authRepository,
        api = cloudReportApi,
        storage = storageApi,
        dao = database.reportDao(),
        commentDao = database.commentDao(),
    )

    /** Placeholder until the AI proxy exists; see com.civic.app.ai.AiAssistant. */
    val aiAssistant: AiAssistant = NoAiAssistant

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

    /**
     * Restores the stored identity, then keeps the database in step with it: a rename updates the author shown on
     * this user's posts, and signing in adopts whatever was written as a device profile before syncing.
     */
    fun start() {
        authRepository.restore()
        appScope.launch(Dispatchers.IO) {
            authRepository.state.filterIsInstance<AuthState.Active>().collect { active ->
                reportRepository.refreshAuthorDetails(active.account)
                if (active.isCloud) {
                    reportRepository.adoptLocalReports(active.account)
                    syncManager.syncNow()
                }
            }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CivicApplication).container
