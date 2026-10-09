package com.civic.app.safety

import com.civic.app.data.auth.Account
import com.civic.app.data.local.ReportEntity
import com.civic.app.data.repository.FakeCommentDao
import com.civic.app.data.repository.FakeReportDao
import com.civic.app.data.repository.ReportRepository
import com.civic.app.location.LocationSource
import com.civic.shared.model.GeoLocation
import com.civic.shared.model.IssueCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    private val account = Account("user-1", "aditya", "Aditya", isCloud = true)
    private val repository = ReportRepository(dao, FakeCommentDao()) { account }
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
        val report = dao.all.single()
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
        assertEquals(here.latitude, dao.all.single().latitude!!, 1e-9)
    }

    @Test
    fun keepsReportWhenNoFixAtAll() {
        val reporter = reporter(FakeLocation(cached = null, fresh = null))
        reporter.report(IssueCategory.UNSAFE_GENERAL, hasLocationPermission = true)

        val events = events(reporter, 2)
        assertTrue(events[0] is QuickReportEvent.Saved)
        assertTrue(events[1] is QuickReportEvent.NoLocation)
        assertNull(dao.all.single().latitude)
    }

    @Test
    fun withoutPermissionNeverAsksForLocation() {
        val location = FakeLocation(cached = here, fresh = here)
        val reporter = reporter(location)
        reporter.report(IssueCategory.UNSAFE_WOMEN, hasLocationPermission = false)

        val events = events(reporter, 2)
        assertTrue(events[1] is QuickReportEvent.NoLocation)
        assertEquals(0, location.calls)
        assertEquals(1, dao.all.size)
    }

    @Test
    fun undoDeletesTheReport() {
        val reporter = reporter(FakeLocation(cached = here, fresh = null))
        reporter.report(IssueCategory.UNSAFE_WOMEN, hasLocationPermission = true)
        val saved = events(reporter, 1).single()

        reporter.undo(saved.reportId)
        assertTrue(dao.all.isEmpty())
    }

    /** A one-tap report is the fastest path into the app, so it is the easiest one to get privacy wrong on. */
    @Test
    fun oneTapReportsArePrivateAndAttributed() {
        val reporter = reporter(FakeLocation(cached = here, fresh = null))
        reporter.report(IssueCategory.UNSAFE_WOMEN, hasLocationPermission = true)
        events(reporter, 1)

        val report = dao.all.single()
        assertEquals(ReportEntity.VISIBILITY_PRIVATE, report.visibility)
        assertEquals("user-1", report.authorId)
        assertEquals("Aditya", report.authorName)
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
