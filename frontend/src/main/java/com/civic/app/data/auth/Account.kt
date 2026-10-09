package com.civic.app.data.auth

/**
 * A live Supabase session. [expiresAtMillis] is when the access token stops working; the refresh token is what
 * keeps the user signed in across app restarts.
 */
data class CloudSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtMillis: Long,
) {
    /** Refreshed a minute early, so a request never starts with a token that dies mid-flight. */
    fun isExpiring(now: Long = System.currentTimeMillis()): Boolean = now >= expiresAtMillis - 60_000
}

/**
 * Who the app is acting as.
 *
 * There are two kinds, and the difference is only where the identity lives:
 * - a **device profile** ([isCloud] false) — a name the user picked on this phone, no server involved. Reports
 *   stay on the device. This is what makes the app usable with no backend configured.
 * - a **cloud account** ([isCloud] true) — a real Supabase user; [id] is their `auth.users` UUID, reports sync.
 *
 * Both are real identities as far as the UI is concerned: reports and comments are attributed to [displayName].
 */
data class Account(
    val id: String,
    val username: String,
    val displayName: String,
    /** Local file path for a device profile, public URL for a cloud account. Null = use initials. */
    val avatar: String? = null,
    val bio: String? = null,
    val email: String? = null,
    val isCloud: Boolean = false,
) {
    /** Fallback avatar: up to two initials from the display name. */
    val initials: String
        get() = displayName.trim().split(' ', limit = 3)
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
}

/** What the app shows on launch. */
sealed interface AuthState {
    /** Reading the stored session; the UI shows a splash rather than flashing the wrong screen. */
    data object Loading : AuthState

    /** First launch (or after "remove profile"): the user has not told us who they are yet. */
    data object NeedsOnboarding : AuthState

    data class Active(val account: Account, val session: CloudSession? = null) : AuthState {
        val isCloud: Boolean get() = session != null
    }
}

/** The outcome of a sign-up: a session, or an account that still has to confirm its email address. */
sealed interface SignUpOutcome {
    data object SignedIn : SignUpOutcome
    data class NeedsEmailConfirmation(val email: String) : SignUpOutcome
}
