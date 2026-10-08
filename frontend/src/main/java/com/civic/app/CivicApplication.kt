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
        .addMigrations(CivicDatabase.MIGRATION_1_2)
        .build()
    private val reportApi by lazy { ReportApi(ApiClient.httpClient) }

    val reportRepository = ReportRepository(reportApi, database.reportDao(), database.commentDao())
    val locationProvider = LocationProvider(application)
    val draftStore = DraftStore()
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CivicApplication).container
