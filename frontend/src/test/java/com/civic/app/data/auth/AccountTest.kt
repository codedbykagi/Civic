package com.civic.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {

    /** The handle has to satisfy the username check constraint in infra/supabase/schema.sql. */
    private val serverRule = Regex("^[a-z0-9_]{3,20}$")

    @Test
    fun `derived usernames always satisfy the server constraint`() {
        val names = listOf(
            "Aditya Srivastava",
            "A",
            "  ",
            "!!!",
            "Zoë O'Brien-Smith",
            "a_very_long_display_name_that_runs_past_twenty",
            "राधा",
            "user@example.com",
            "123",
        )
        names.forEach { name ->
            val username = name.toUsername()
            assertTrue("'$name' produced '$username'", serverRule.matches(username))
        }
    }

    @Test
    fun `usernames are lowercased and punctuation collapses to one underscore`() {
        assertEquals("aditya_srivastava", "Aditya Srivastava".toUsername())
        assertEquals("zo_o_brien_smith", "Zoë O'Brien-Smith".toUsername())
    }

    @Test
    fun `a name with nothing usable falls back rather than producing an invalid handle`() {
        assertEquals("civic_user", "!!!".toUsername())
        assertEquals("civic_user", "".toUsername())
        // A fully non-ASCII name has no valid handle to derive, so it falls back instead of being rejected
        // by the server's check constraint.
        assertEquals("civic_user", "राधा".toUsername())
    }

    @Test
    fun `initials take at most two letters and survive an empty name`() {
        assertEquals("AS", Account("1", "a", "Aditya Srivastava").initials)
        assertEquals("A", Account("1", "a", "Aditya").initials)
        assertEquals("?", Account("1", "a", "  ").initials)
    }

    @Test
    fun `a session counts as expiring a minute before it actually expires`() {
        val now = 1_000_000L
        val session = CloudSession("u", "access", "refresh", expiresAtMillis = now + 90_000)
        assertFalse(session.isExpiring(now))
        // 30s of life left: refresh now rather than start a request with a token that dies in flight.
        assertTrue(session.isExpiring(now + 60_000))
    }
}
