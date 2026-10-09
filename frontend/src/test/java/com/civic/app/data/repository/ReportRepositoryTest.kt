package com.civic.app.data.repository

import com.civic.app.data.auth.Account
import com.civic.app.data.local.ReportEntity
import com.civic.shared.model.IssueCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRepositoryTest {

    private val account = Account(
        id = "user-1",
        username = "aditya",
        displayName = "Aditya",
        avatar = "/data/avatar.jpg",
        isCloud = true,
    )

    private fun repo(
        dao: FakeReportDao = FakeReportDao(),
        comments: FakeCommentDao = FakeCommentDao(),
        signedIn: Account? = account,
    ) = ReportRepository(dao, comments) { signedIn }

    private fun report(category: IssueCategory, description: String = "") = ReportEntity(
        category = category.name,
        description = description,
        localImagePath = null,
        latitude = 1.0,
        longitude = 2.0,
        capturedAt = 0L,
    )

    @Test
    fun `a new report is attributed to the signed-in account`() = runBlocking {
        val dao = FakeReportDao()
        val id = repo(dao).addReport(report(IssueCategory.POTHOLE))

        val saved = dao.row(id)!!
        assertEquals("user-1", saved.authorId)
        assertEquals("Aditya", saved.authorName)
        assertEquals("/data/avatar.jpg", saved.authorAvatar)
    }

    /**
     * The privacy rule that must not be reachable from the UI: a safety report is private wherever it was
     * created from, so no screen can accidentally publish one by forgetting to pass a flag.
     */
    @Test
    fun `safety reports are always private and civic reports always public`() = runBlocking {
        val dao = FakeReportDao()
        val repository = repo(dao)

        IssueCategory.SAFETY.forEach { category ->
            val saved = dao.row(repository.addReport(report(category)))!!
            assertEquals(category.name, ReportEntity.VISIBILITY_PRIVATE, saved.visibility)
        }
        IssueCategory.CIVIC.forEach { category ->
            val saved = dao.row(repository.addReport(report(category)))!!
            assertEquals(category.name, ReportEntity.VISIBILITY_PUBLIC, saved.visibility)
        }
    }

    @Test
    fun `a caller cannot publish a safety report by pre-setting its visibility`() = runBlocking {
        val dao = FakeReportDao()
        val sneaky = report(IssueCategory.UNSAFE_WOMEN).copy(visibility = ReportEntity.VISIBILITY_PUBLIC)

        val saved = dao.row(repo(dao).addReport(sneaky))!!
        assertEquals(ReportEntity.VISIBILITY_PRIVATE, saved.visibility)
    }

    @Test
    fun `safety reports stay out of the civic feed`() = runBlocking {
        val dao = FakeReportDao()
        val repository = repo(dao)
        repository.addReport(report(IssueCategory.POTHOLE, "pothole"))
        repository.addReport(report(IssueCategory.UNSAFE_WOMEN, "felt unsafe"))

        val feed = repository.observeCivicFeed(category = null, status = null).first()
        assertEquals(listOf("pothole"), feed.map { it.description })

        val private = repository.observeSafetyReports().first()
        assertEquals(listOf("felt unsafe"), private.map { it.description })
    }

    @Test
    fun `with no account a report is still saved, just unattributed`() = runBlocking {
        val dao = FakeReportDao()
        val id = repo(dao, signedIn = null).addReport(report(IssueCategory.GARBAGE))

        assertNull(dao.row(id)!!.authorId)
    }

    @Test
    fun `an upvote toggles both ways and never goes negative`() = runBlocking {
        val dao = FakeReportDao()
        val repository = repo(dao)
        val id = repository.addReport(report(IssueCategory.POTHOLE))

        repository.toggleUpvote(id)
        assertEquals(1, dao.row(id)!!.upvotes)
        assertTrue(dao.row(id)!!.upvotedByMe)

        repository.toggleUpvote(id)
        assertEquals(0, dao.row(id)!!.upvotes)
        assertTrue(!dao.row(id)!!.upvotedByMe)

        // Taking back a vote that was never counted (e.g. the row arrived from the server at 0) must not
        // produce a negative count.
        repository.toggleUpvote(id)
        repository.toggleUpvote(id)
        repository.toggleUpvote(id)
        assertTrue(dao.row(id)!!.upvotes >= 0)
    }

    @Test
    fun `a comment carries the author instead of Guest`() = runBlocking {
        val comments = FakeCommentDao()
        val repository = repo(comments = comments)
        repository.addComment(reportId = 7, text = "Fixed last week")

        val saved = comments.all.single()
        assertEquals("Aditya", saved.author)
        assertEquals("user-1", saved.authorId)
        assertTrue(!saved.isSynced)
    }

    @Test
    fun `signing in adopts reports written under a device profile`() = runBlocking {
        val dao = FakeReportDao(
            listOf(
                report(IssueCategory.POTHOLE).copy(localId = 1, authorId = "local:install-1", authorName = "Guest"),
                report(IssueCategory.FIRE).copy(localId = 2, authorId = null),
                report(IssueCategory.GARBAGE).copy(localId = 3, authorId = "someone-else", isSynced = true),
            ),
        )
        repo(dao).adoptLocalReports(account)

        assertEquals("user-1", dao.row(1)!!.authorId)
        assertEquals("user-1", dao.row(2)!!.authorId)
        // Another account's report is left alone, and stays synced.
        assertEquals("someone-else", dao.row(3)!!.authorId)
        assertTrue(dao.row(3)!!.isSynced)
        // The adopted ones are queued for upload.
        assertTrue(!dao.row(1)!!.isSynced && !dao.row(2)!!.isSynced)
    }

    @Test
    fun `renaming a profile updates the name shown on existing posts`() = runBlocking {
        val dao = FakeReportDao(listOf(report(IssueCategory.POTHOLE).copy(localId = 1, authorId = "user-1")))
        val comments = FakeCommentDao()
        val repository = repo(dao, comments)
        repository.addComment(reportId = 1, text = "hi")

        repository.refreshAuthorDetails(account.copy(displayName = "Aditya S.", avatar = null))

        assertEquals("Aditya S.", dao.row(1)!!.authorName)
        assertNull(dao.row(1)!!.authorAvatar)
        assertEquals("Aditya S.", comments.all.single().author)
    }
}
