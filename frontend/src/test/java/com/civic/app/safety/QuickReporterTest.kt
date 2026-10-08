package com.civic.app.safety

import com.civic.app.data.local.CommentDao
import com.civic.app.data.local.CommentEntity
import com.civic.app.data.local.ReportDao
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.remote.ReportApi
import com.civic.app.data.repository.ReportRepository
import com.civic.app.location.LocationSource
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickReporterTest {

    private val dao = FakeReportDao()
    private val repository = ReportRepository(ReportApi(HttpClient()), dao, FakeCommentDao())
    private val here = GeoLocation(28.6139, 77.2090)
    private val better = GeoLocation(28.6140, 77.2091)

    private fun reporter(location: FakeLocation) =
        QuickReporter(repository, location, CoroutineScope(Dispatchers.Unconfined), clock = { 1_000_000L })

    private fun events(reporter: QuickReporter, count: Int) =
        runBlocking { withTimeout(2_000) { reporter.events.take(count).toList() } }

    @Test
    fun savesInstantlyWithCachedFixThenRefines() {
        val reporter = reporter(FakeLocation(cached = here, fresh = better))
        reporter.report(IssueCategory.UNSAFE_WOMEN, hasLocationPermission = true)

        val saved = events(reporter, 1).single() as QuickReportEvent.Saved
        assertTrue(saved.located)
        assertEquals(IssueCategory.UNSAFE_WOMEN, saved.category)
        val report = dao.rows.value.single()
        assertEquals(IssueCategory.UNSAFE_WOMEN.name, report.category)
        assertNull("quick reports have no photo", report.localImagePath)
        assertEquals(1_000_000L, report.capturedAt)
        assertEquals("refined by the fresh fix", better.latitude, report.latitude!!, 1e-9)
    }

    @Test
    fun withoutCachedFixUsesFreshFix() {
        val reporter = reporter(FakeLocation(cached = null, fresh = here))
        reporter.report(IssueCategory.UNSAFE_CHILDREN, hasLocationPermission = true)

        val saved = events(reporter, 1).single() as QuickReportEvent.Saved
        assertFalse(saved.located)
        assertEquals(here.latitude, dao.rows.value.single().latitude!!, 1e-9)
    }

    @Test
    fun keepsReportWhenNoFixAtAll() {
        val reporter = reporter(FakeLocation(cached = null, fresh = null))
        reporter.report(IssueCategory.UNSAFE_GENERAL, hasLocationPermission = true)

        val events = events(reporter, 2)
        assertTrue(events[0] is QuickReportEvent.Saved)
        assertTrue(events[1] is QuickReportEvent.NoLocation)
        assertNull(dao.rows.value.single().latitude)
    }

    @Test
    fun withoutPermissionNeverAsksForLocation() {
        val location = FakeLocation(cached = here, fresh = here)
        val reporter = reporter(location)
        reporter.report(IssueCategory.UNSAFE_WOMEN, hasLocationPermission = false)

        val events = events(reporter, 2)
        assertTrue(events[1] is QuickReportEvent.NoLocation)
        assertEquals(0, location.calls)
        assertEquals(1, dao.rows.value.size)
    }

    @Test
    fun undoDeletesTheReport() {
        val reporter = reporter(FakeLocation(cached = here, fresh = null))
        reporter.report(IssueCategory.UNSAFE_WOMEN, hasLocationPermission = true)
        val saved = events(reporter, 1).single()

        reporter.undo(saved.reportId)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCivicCategories() {
        reporter(FakeLocation(null, null)).report(IssueCategory.POTHOLE, hasLocationPermission = true)
    }
}

private class FakeLocation(private val cached: GeoLocation?, private val fresh: GeoLocation?) : LocationSource {
    var calls = 0

    override suspend fun freshLocation(timeoutMs: Long): GeoLocation? = fresh.also { calls++ }

    override suspend fun lastKnown(maxAgeMs: Long): GeoLocation? = cached.also { calls++ }
}

/** In-memory ReportDao with just enough behaviour for the repository calls the reporter makes. */
private class FakeReportDao : ReportDao {
    val rows = MutableStateFlow<List<ReportEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<ReportEntity>> = rows

    override fun observeFiltered(category: String?, status: String?, excluded: List<String>) =
        rows.map { list -> list.filter { it.category !in excluded } }

    override fun observeInCategories(categories: List<String>) = rows.map { list -> list.filter { it.category in categories } }

    override fun observeById(id: Long) = rows.map { list -> list.firstOrNull { it.localId == id } }

    override suspend fun getUnsynced() = rows.value.filter { !it.isSynced }

    override suspend fun upvote(id: Long) = edit(id) { it.copy(upvotes = it.upvotes + 1) }

    override suspend fun setStatus(id: Long, status: String) = edit(id) { it.copy(status = status) }

    override suspend fun updateDetails(id: Long, category: String, description: String, timeOfDay: String?, tags: String?) =
        edit(id) { it.copy(category = category, description = description, timeOfDay = timeOfDay, tags = tags) }

    override suspend fun setLocation(id: Long, latitude: Double, longitude: Double) =
        edit(id) { it.copy(latitude = latitude, longitude = longitude) }

    override suspend fun setPhoto(id: Long, path: String?) = edit(id) { it.copy(localImagePath = path) }

    override suspend fun getById(id: Long) = rows.value.firstOrNull { it.localId == id }

    override suspend fun insert(report: ReportEntity): Long {
        val id = nextId++
        rows.value = rows.value + report.copy(localId = id)
        return id
    }

    override suspend fun update(report: ReportEntity) = edit(report.localId) { report }

    override suspend fun delete(report: ReportEntity) {
        rows.value = rows.value.filterNot { it.localId == report.localId }
    }

    private fun edit(id: Long, change: (ReportEntity) -> ReportEntity) {
        rows.value = rows.value.map { if (it.localId == id) change(it) else it }
    }
}

private class FakeCommentDao : CommentDao {
    override fun observeForReport(reportId: Long): Flow<List<CommentEntity>> = MutableStateFlow(emptyList())

    override suspend fun insert(comment: CommentEntity): Long = 0
}
