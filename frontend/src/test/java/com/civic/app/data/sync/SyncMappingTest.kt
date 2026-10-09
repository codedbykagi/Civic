package com.civic.app.data.sync

import com.civic.app.data.cloud.ProfileDto
import com.civic.app.data.local.ReportEntity
import com.civic.shared.model.IssueCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room stores epoch millis and comma-separated tags; Postgres stores timestamptz and a text[]. These tests pin
 * the translation, because getting it wrong shows up as reports dated 1970 rather than as a crash.
 */
class SyncMappingTest {

    private val entity = ReportEntity(
        localId = 42,
        category = IssueCategory.POTHOLE.name,
        description = "Deep pothole",
        localImagePath = "/data/photo.jpg",
        latitude = 28.6139,
        longitude = 77.2090,
        capturedAt = 1_700_000_000_000,
        upvotes = 3,
        tags = "POORLY_LIT,ISOLATED",
    )

    @Test
    fun `capturedAt becomes an ISO timestamp Postgres accepts`() {
        val dto = entity.toDto(authorId = "user-1", photoUrl = null, installId = "install-9")
        assertEquals("2023-11-14T22:13:20Z", dto.capturedAt)
    }

    @Test
    fun `the client id is the install plus the local row, so a retry updates instead of duplicating`() {
        val dto = entity.toDto(authorId = "user-1", photoUrl = null, installId = "install-9")
        assertEquals("install-9:42", dto.clientId)
    }

    @Test
    fun `tags cross the wire as an array and come back as a comma-separated string`() {
        val dto = entity.toDto(authorId = "user-1", photoUrl = null, installId = "i")
        assertEquals(listOf("POORLY_LIT", "ISOLATED"), dto.tags)

        val roundTripped = dto.copy(id = "uuid-1").toEntity("uuid-1", upvotedByMe = false)
        assertEquals("POORLY_LIT,ISOLATED", roundTripped.tags)
    }

    @Test
    fun `no tags means an empty array out and a null column back`() {
        val dto = entity.copy(tags = null).toDto(authorId = "u", photoUrl = null, installId = "i")
        assertTrue(dto.tags.isEmpty())
        assertNull(dto.copy(id = "x").toEntity("x", upvotedByMe = false).tags)
    }

    @Test
    fun `a round trip preserves what the server owns and does not invent a local photo`() {
        val dto = entity.toDto(authorId = "user-1", photoUrl = "https://example.test/p.jpg", installId = "i")
            .copy(id = "uuid-1", upvotes = 7, profiles = ProfileDto("user-1", "aditya", "Aditya"))

        val back = dto.toEntity("uuid-1", upvotedByMe = true)
        assertEquals("uuid-1", back.remoteId)
        assertEquals(1_700_000_000_000, back.capturedAt)
        assertEquals(7, back.upvotes)
        assertEquals("Aditya", back.authorName)
        assertEquals("https://example.test/p.jpg", back.imageUrl)
        assertTrue(back.isSynced)
        assertTrue(back.upvotedByMe)
        // The photo on the server is not a file on this phone.
        assertNull(back.localImagePath)
    }

    @Test
    fun `a private report stays private across the round trip`() {
        val safety = entity.copy(
            category = IssueCategory.UNSAFE_WOMEN.name,
            visibility = ReportEntity.VISIBILITY_PRIVATE,
        )
        val dto = safety.toDto(authorId = "u", photoUrl = null, installId = "i")
        assertEquals("private", dto.visibility)
        assertEquals(
            ReportEntity.VISIBILITY_PRIVATE,
            dto.copy(id = "x").toEntity("x", upvotedByMe = false).visibility,
        )
    }

    @Test
    fun `timestamps Postgres might return are all parsed`() {
        assertEquals(1_700_000_000_000, "2023-11-14T22:13:20Z".toEpochMillis())
        assertEquals(1_700_000_000_000, "2023-11-14T22:13:20.000Z".toEpochMillis())
        // PostgREST can render timestamptz with a space and no zone designator.
        assertEquals(1_700_000_000_000, "2023-11-14 22:13:20".toEpochMillis())
    }

    @Test
    fun `an unparseable timestamp falls back to now rather than to 1970`() {
        val before = System.currentTimeMillis()
        val parsed = "not a date".toEpochMillis()
        assertTrue("got $parsed", parsed >= before)
    }

    @Test
    fun `a null timestamp also falls back to now`() {
        val before = System.currentTimeMillis()
        assertTrue(null.toEpochMillis() >= before)
    }
}
