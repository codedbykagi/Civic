package com.civic.app.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The messages in [friendlyMessage] are shown to users verbatim, so they are worth pinning down. */
class CloudErrorTest {

    @Test
    fun `a bad password reads as a bad password, not as an HTTP code`() {
        assertEquals("Wrong email or password.", friendlyMessage(400, "Invalid login credentials", null))
    }

    @Test
    fun `an unconfirmed email tells the user where to look`() {
        val message = friendlyMessage(400, "Email not confirmed", null)
        assertTrue(message, message.contains("inbox", ignoreCase = true))
    }

    @Test
    fun `a duplicate signup points at signing in instead`() {
        val message = friendlyMessage(422, "User already registered", null)
        assertTrue(message, message.contains("signing in", ignoreCase = true))
    }

    @Test
    fun `a missing table blames the schema rather than the user`() {
        val message = friendlyMessage(404, null, null)
        assertTrue(message, message.contains("schema.sql"))
    }

    @Test
    fun `an expired session asks for a new sign-in`() {
        assertEquals("Your session expired. Sign in again.", friendlyMessage(401, null, null))
    }

    @Test
    fun `an unrecognised server message is passed through rather than swallowed`() {
        val tooShort = "Password should be at least 6 characters"
        assertEquals(tooShort, friendlyMessage(422, tooShort, null))
        assertEquals("some new supabase error", friendlyMessage(400, "some new supabase error", null))
    }

    @Test
    fun `nothing useful from the server still produces a specific message`() {
        assertEquals("Request failed (HTTP 418).", friendlyMessage(418, null, null))
        assertEquals("Request failed (HTTP 418).", friendlyMessage(418, "   ", null))
    }
}
